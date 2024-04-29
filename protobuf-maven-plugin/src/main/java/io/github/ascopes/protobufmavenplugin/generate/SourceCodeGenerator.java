/*
 * Copyright (C) 2023 - 2024, Ashley Scopes.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.github.ascopes.protobufmavenplugin.generate;

import io.github.ascopes.protobufmavenplugin.ProtocPlugin;
import io.github.ascopes.protobufmavenplugin.dependency.MavenDependencyPathResolver;
import io.github.ascopes.protobufmavenplugin.dependency.MavenProjectDependencyPathResolver;
import io.github.ascopes.protobufmavenplugin.dependency.ResolutionException;
import io.github.ascopes.protobufmavenplugin.execute.ArgLineBuilder;
import io.github.ascopes.protobufmavenplugin.execute.CommandLineExecutor;
import io.github.ascopes.protobufmavenplugin.plugin.BinaryPluginResolver;
import io.github.ascopes.protobufmavenplugin.plugin.JvmPluginResolver;
import io.github.ascopes.protobufmavenplugin.plugin.ResolvedProtocPlugin;
import io.github.ascopes.protobufmavenplugin.protoc.ProtocResolver;
import io.github.ascopes.protobufmavenplugin.source.ProtoFileListing;
import io.github.ascopes.protobufmavenplugin.source.ProtoSourceResolver;
import io.github.ascopes.protobufmavenplugin.utils.FileUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.execution.MavenSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Orchestrates all moving parts in this plugin, collecting all relevant information and
 * dependencies to pass to an invocation of {@code protoc}.
 *
 * <p>Orchestrates all other components.
 *
 * @author Ashley Scopes
 */
@Named
public final class SourceCodeGenerator {

  private static final Logger log = LoggerFactory.getLogger(SourceCodeGenerator.class);

  private final MavenSession mavenSession;
  private final MavenDependencyPathResolver mavenDependencyPathResolver;
  private final MavenProjectDependencyPathResolver mavenProjectDependencyPathResolver;
  private final ProtocResolver protocResolver;
  private final BinaryPluginResolver binaryPluginResolver;
  private final JvmPluginResolver jvmPluginResolver;
  private final ProtoSourceResolver protoListingResolver;
  private final CommandLineExecutor commandLineExecutor;

  @Inject
  public SourceCodeGenerator(
      MavenSession mavenSession,
      MavenDependencyPathResolver mavenDependencyPathResolver,
      MavenProjectDependencyPathResolver mavenProjectDependencyPathResolver,
      ProtocResolver protocResolver,
      BinaryPluginResolver binaryPluginResolver,
      JvmPluginResolver jvmPluginResolver,
      ProtoSourceResolver protoListingResolver,
      CommandLineExecutor commandLineExecutor
  ) {
    this.mavenSession = mavenSession;
    this.mavenDependencyPathResolver = mavenDependencyPathResolver;
    this.mavenProjectDependencyPathResolver = mavenProjectDependencyPathResolver;
    this.protocResolver = protocResolver;
    this.binaryPluginResolver = binaryPluginResolver;
    this.jvmPluginResolver = jvmPluginResolver;
    this.protoListingResolver = protoListingResolver;
    this.commandLineExecutor = commandLineExecutor;
  }

  private boolean hasPlugins(GenerationRequest request) {
    return !request.getBinaryUrlPlugins().isEmpty()
        || !request.getBinaryMavenPlugins().isEmpty()
        || !request.getBinaryPathPlugins().isEmpty();
  }

  private boolean pluginsOptional(GenerationRequest request) {
    return request.getBinaryUrlPlugins().stream().allMatch(ProtocPlugin::isOptional)
        && request.getBinaryMavenPlugins().stream().allMatch(ProtocPlugin::isOptional)
        && request.getBinaryPathPlugins().stream().allMatch(ProtocPlugin::isOptional);
  }

  public boolean generate(GenerationRequest request) throws ResolutionException, IOException {
    final var protocPath = discoverProtocPath(request);

    final var resolvedPlugins = discoverPlugins(request);
    final var importPaths = discoverImportPaths(request);
    final var sourcePaths = discoverCompilableSources(request);

    if (sourcePaths.isEmpty()) {
      if (request.isFailOnMissingSources()) {
        log.error("No protobuf sources found. If this is unexpected, check your "
            + "configuration and try again.");
        return false;
      } else {
        log.warn("No protobuf sources found; nothing to do!");
        return true;
      }
    }

    if (resolvedPlugins.isEmpty() && hasPlugins(request) && pluginsOptional(request)) {
      log.info("No resolved plugins found and all are optional, nothing to do.");
      return true;
    }

    createOutputDirectories(request);

    var argLineBuilder = new ArgLineBuilder(protocPath)
        .fatalWarnings(request.isFatalWarnings())
        .importPaths(importPaths
            .stream()
            .map(ProtoFileListing::getProtoFilesRoot)
            .collect(Collectors.toCollection(LinkedHashSet::new)))
        .importPaths(request.getSourceRoots())
        .plugins(resolvedPlugins, request.getOutputDirectory());

    request.getEnabledLanguages()
        .forEach(language -> argLineBuilder.generateCodeFor(
            language,
            request.getOutputDirectory(),
            request.isLiteEnabled()
        ));

    var sourceFiles = sourcePaths
        .stream()
        .map(ProtoFileListing::getProtoFiles)
        .flatMap(Collection::stream)
        .collect(Collectors.toCollection(LinkedHashSet::new));

    if (!logProtocVersion(protocPath)) {
      log.error("Unable to execute protoc. Ensure the binary is compatible for this platform!");
      return false;
    }

    if (!commandLineExecutor.execute(argLineBuilder.compile(sourceFiles))) {
      return false;
    }

    registerSourceRoots(request);
    return true;
  }

  private Path discoverProtocPath(GenerationRequest request) throws ResolutionException {
    return protocResolver.resolve(request.getProtocVersion());
  }

  private boolean logProtocVersion(Path protocPath) throws IOException {
    var args = new ArgLineBuilder(protocPath).version();
    return commandLineExecutor.execute(args);
  }

  private Collection<ResolvedProtocPlugin> discoverPlugins(
      GenerationRequest request
  ) throws IOException, ResolutionException {
    return concat(
        binaryPluginResolver
            .resolveMavenPlugins(request.getBinaryMavenPlugins()),
        binaryPluginResolver
            .resolvePathPlugins(request.getBinaryPathPlugins()),
        binaryPluginResolver
            .resolveUrlPlugins(request.getBinaryUrlPlugins()),
        jvmPluginResolver
            .resolveMavenPlugins(request.getJvmMavenPlugins())
    );
  }

  private Collection<ProtoFileListing> discoverImportPaths(
      GenerationRequest request
  ) throws IOException, ResolutionException {
    var importPaths = new ArrayList<ProtoFileListing>();

    if (!request.getImportDependencies().isEmpty()) {
      log.debug(
          "Finding importable protobuf sources from explicitly provided import dependencies ({})",
          request.getImportDependencies()
      );
      var importDependencies = mavenDependencyPathResolver.resolveAll(
          request.getImportDependencies(),
          request.getDependencyResolutionDepth()
      );
      importPaths.addAll(protoListingResolver.createProtoFileListings(importDependencies));
    }

    if (!request.getImportPaths().isEmpty()) {
      log.debug(
          "Finding importable protobuf sources from explicitly provided import paths ({})",
          request.getImportPaths()
      );
      importPaths.addAll(protoListingResolver.createProtoFileListings(request.getImportPaths()));
    }

    if (!request.getSourceDependencies().isEmpty()) {
      log.debug(
          "Finding importable protobuf sources from explicitly provided source dependencies ({})",
          request.getSourceDependencies()
      );
      var sourceDependencyPaths = mavenDependencyPathResolver.resolveAll(
          request.getSourceDependencies(),
          request.getDependencyResolutionDepth()
      );
      importPaths.addAll(protoListingResolver.createProtoFileListings(sourceDependencyPaths));
    }

    if (!request.isIgnoreProjectDependencies()) {
      log.debug("Finding importable protobuf sources from project dependencies");
      var projectDependencyPaths = mavenProjectDependencyPathResolver.resolveProjectDependencies(
          request.getDependencyResolutionDepth()
      );
      importPaths.addAll(protoListingResolver.createProtoFileListings(projectDependencyPaths));
    }

    return importPaths;
  }

  private Collection<ProtoFileListing> discoverCompilableSources(
      GenerationRequest request
  ) throws IOException, ResolutionException {
    log.debug("Discovering all compilable protobuf source files");
    var sourcePathsListings = protoListingResolver
        .createProtoFileListings(request.getSourceRoots());

    var sourceDependencies = mavenDependencyPathResolver.resolveAll(
        request.getSourceDependencies(),
        request.getDependencyResolutionDepth()
    );

    var sourceDependencyListings = protoListingResolver
        .createProtoFileListings(sourceDependencies);

    var sourcePaths = concat(sourcePathsListings, sourceDependencyListings);

    log.info("Will generate source code for {} protobuf file(s)", sourcePaths.size());
    return sourcePaths;
  }

  private void createOutputDirectories(GenerationRequest request) throws IOException {
    var directory = request.getOutputDirectory();
    log.debug("Creating {}", directory);

    // Having .jar on the output directory makes protoc generate a JAR with a
    // Manifest. This will break our logic because generated sources will be
    // inaccessible for the compilation phase later. For now, just prevent this
    // edge case entirely.
    FileUtils.getFileExtension(directory)
        .filter(".jar"::equalsIgnoreCase)
        .ifPresent(ext -> {
          throw new IllegalArgumentException(
              "The output directory '" + directory
                  + "' cannot be a path with a JAR file extension");
        });

    Files.createDirectories(directory);
  }

  private void registerSourceRoots(GenerationRequest request) {
    var directory = request.getOutputDirectory();

    if (request.isRegisterAsCompilationRoot()) {
      var registrar = request.getSourceRootRegistrar();
      log.debug("Registering {} as {} compilation root", directory, registrar);
      registrar.registerSourceRoot(mavenSession, directory);
    } else {
      log.debug("Not registering {} as a compilation root", directory);
    }
  }

  @SafeVarargs
  @SuppressWarnings("varargs")
  private static <T> List<T> concat(Collection<T>... collections) {
    return Stream.of(collections)
        .flatMap(Collection::stream)
        .collect(Collectors.toList());
  }
}
