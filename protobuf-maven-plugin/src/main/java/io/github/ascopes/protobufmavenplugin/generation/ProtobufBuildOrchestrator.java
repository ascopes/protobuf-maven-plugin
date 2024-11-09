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

package io.github.ascopes.protobufmavenplugin.generation;

import io.github.ascopes.protobufmavenplugin.dependencies.MavenArtifactPathResolver;
import io.github.ascopes.protobufmavenplugin.dependencies.ResolutionException;
import io.github.ascopes.protobufmavenplugin.plugins.BinaryPluginResolver;
import io.github.ascopes.protobufmavenplugin.plugins.JvmPluginResolver;
import io.github.ascopes.protobufmavenplugin.plugins.ResolvedProtocPlugin;
import io.github.ascopes.protobufmavenplugin.protoc.ArgLineBuilder;
import io.github.ascopes.protobufmavenplugin.protoc.CommandLineExecutor;
import io.github.ascopes.protobufmavenplugin.protoc.ProtocResolver;
import io.github.ascopes.protobufmavenplugin.sources.SourceGlobFilter;
import io.github.ascopes.protobufmavenplugin.sources.SourceListing;
import io.github.ascopes.protobufmavenplugin.sources.SourceResolver;
import io.github.ascopes.protobufmavenplugin.utils.FileUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Comparator;
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
public final class ProtobufBuildOrchestrator {

  private static final Logger log = LoggerFactory.getLogger(ProtobufBuildOrchestrator.class);

  private final MavenSession mavenSession;
  private final MavenArtifactPathResolver artifactPathResolver;
  private final ProtocResolver protocResolver;
  private final BinaryPluginResolver binaryPluginResolver;
  private final JvmPluginResolver jvmPluginResolver;
  private final SourceResolver protoListingResolver;
  private final CommandLineExecutor commandLineExecutor;

  @Inject
  public ProtobufBuildOrchestrator(
      MavenSession mavenSession,
      MavenArtifactPathResolver artifactPathResolver,
      ProtocResolver protocResolver,
      BinaryPluginResolver binaryPluginResolver,
      JvmPluginResolver jvmPluginResolver,
      SourceResolver protoListingResolver,
      CommandLineExecutor commandLineExecutor
  ) {
    this.mavenSession = mavenSession;
    this.artifactPathResolver = artifactPathResolver;
    this.protocResolver = protocResolver;
    this.binaryPluginResolver = binaryPluginResolver;
    this.jvmPluginResolver = jvmPluginResolver;
    this.protoListingResolver = protoListingResolver;
    this.commandLineExecutor = commandLineExecutor;
  }

  public boolean generate(GenerationRequest request) throws ResolutionException, IOException {
    log.debug("Protobuf GenerationRequest is: {}", request);
    
    final var protocPath = discoverProtocPath(request);

    final var resolvedPlugins = discoverPlugins(request);
    final var sourcePaths = discoverCompilableSources(request);
    final var importPaths = discoverImportPaths(sourcePaths, request);

    if (sourcePaths.isEmpty()) {
      if (request.isFailOnMissingSources()) {
        log.error("No protobuf sources found. If this is unexpected, check your "
            + "configuration and try again.");
        return false;
      } else {
        log.warn("No protobuf sources were found. There is nothing to do!");
        return true;
      }
    }

    if (resolvedPlugins.isEmpty() && request.getEnabledLanguages().isEmpty()) {
      if (request.isFailOnMissingTargets()) {
        log.error("No languages are enabled and no plugins found, check your "
            + "configuration and try again.");
        return false;
      } else {
        log.warn("No languages are enabled and no plugins were found. There is nothing to do!");
        return true;
      }
    }

    createOutputDirectories(request);

    var argLineBuilder = new ArgLineBuilder(protocPath)
        .fatalWarnings(request.isFatalWarnings())
        .importPaths(
            importPaths.stream()
                .map(SourceListing::getProtoFilesRoot)
                .collect(Collectors.toCollection(LinkedHashSet::new))
        );

    request.getEnabledLanguages()
        .forEach(language -> argLineBuilder.generateCodeFor(
            language,
            request.getOutputDirectory(),
            request.isLiteEnabled()
        ));

    // GH-269: Add the plugins after the enabled languages to support generated code injection
    argLineBuilder.plugins(resolvedPlugins, request.getOutputDirectory());

    var sourceFiles = sourcePaths
        .stream()
        .map(SourceListing::getProtoFiles)
        .flatMap(Collection::stream)
        .collect(Collectors.toCollection(LinkedHashSet::new));

    var argLine = argLineBuilder.compile(sourceFiles);

    if (!commandLineExecutor.execute(argLine)) {
      return false;
    }

    registerSourceRoots(request);

    if (request.isEmbedSourcesInClassOutputs()) {
      embedSourcesInClassOutputs(request.getSourceRootRegistrar(), sourcePaths);
    }

    return true;
  }

  private Path discoverProtocPath(GenerationRequest request) throws ResolutionException {
    return protocResolver.resolve(request.getProtocVersion())
        .orElseThrow(() -> new ResolutionException("Protoc binary was not found"));
  }

  private Collection<ResolvedProtocPlugin> discoverPlugins(
      GenerationRequest request
  ) throws IOException, ResolutionException {
    var plugins = concat(
        binaryPluginResolver
            .resolveMavenPlugins(request.getBinaryMavenPlugins()),
        binaryPluginResolver
            .resolvePathPlugins(request.getBinaryPathPlugins()),
        binaryPluginResolver
            .resolveUrlPlugins(request.getBinaryUrlPlugins()),
        jvmPluginResolver
            .resolveMavenPlugins(request.getJvmMavenPlugins())
    );

    // Sort by precedence then by initial order (sort is stable which guarantees this property).
    return plugins
        .stream()
        .sorted(Comparator.comparingInt(ResolvedProtocPlugin::getOrder))
        .collect(Collectors.toUnmodifiableList());
  }

  private Collection<SourceListing> discoverImportPaths(
      Collection<SourceListing> sourcePathListings,
      GenerationRequest request
  ) throws ResolutionException {
    var artifactPaths = artifactPathResolver.resolveDependencies(
        request.getImportDependencies(),
        request.getDependencyResolutionDepth(),
        request.getDependencyScopes(),
        !request.isIgnoreProjectDependencies(),
        request.isFailOnInvalidDependencies()
    );

    var filter = new SourceGlobFilter();

    var importPathListings = protoListingResolver.createProtoFileListings(
        concat(request.getImportPaths(), artifactPaths),
        filter
    );

    // Use the source paths here as well and use them first to give them precedence. This works
    // around GH-172 where we can end up with different versions on the import and source paths
    // depending on how dependency conflicts arise.
    return Stream.concat(sourcePathListings.stream(), importPathListings.stream())
        .distinct()
        .collect(Collectors.toUnmodifiableList());
  }

  private Collection<SourceListing> discoverCompilableSources(
      GenerationRequest request
  ) throws ResolutionException {
    log.debug("Discovering all compilable protobuf source files");

    var filter = new SourceGlobFilter(request.getIncludes(), request.getExcludes());

    var sourcePathsListings = protoListingResolver.createProtoFileListings(
        request.getSourceRoots(),
        filter
    );

    var sourceDependencies = artifactPathResolver.resolveDependencies(
        request.getSourceDependencies(),
        request.getDependencyResolutionDepth(),
        request.getDependencyScopes(),
        false,
        request.isFailOnInvalidDependencies()
    );

    var sourceDependencyListings = protoListingResolver.createProtoFileListings(
        sourceDependencies,
        filter
    );

    var sourcePaths = concat(sourcePathsListings, sourceDependencyListings);

    var sourceFileCount = sourcePaths.stream()
        .mapToInt(sourcePath -> sourcePath.getProtoFiles().size())
        .sum();
    
    log.info(
        "Generating source code for {} from {}",
        pluralize(sourceFileCount, "protobuf file"),
        pluralize(sourcePaths.size(), "source file tree")
    );

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
                  + "' cannot be a path with a JAR file extension, due to "
                  + "limitations with how protoc operates on file names."
          );
        });

    Files.createDirectories(directory);
  }

  private void registerSourceRoots(GenerationRequest request) {
    var directory = request.getOutputDirectory();

    if (request.isRegisterAsCompilationRoot()) {
      var registrar = request.getSourceRootRegistrar();
      log.debug(
          "Registering {} as {} compilation root in this Maven project.",
          directory,
          registrar
      );
      registrar.registerSourceRoot(mavenSession, directory);
    } else {
      log.debug("Not registering {} as a compilation root", directory);
    }
  }

  private void embedSourcesInClassOutputs(
      SourceRootRegistrar registrar,
      Collection<SourceListing> listings
  ) throws ResolutionException {
    for (var listing : listings) {
      try {
        registrar.embedListing(mavenSession, listing);
      } catch (IOException ex) {
        throw new ResolutionException(
            "Failed to embed " + listing.getProtoFilesRoot() 
                + " into the class outputs directory",
            ex
        );
      }
    }
  }

  @SafeVarargs
  @SuppressWarnings("varargs")
  private static <T> List<T> concat(Collection<? extends T>... collections) {
    return Stream.of(collections)
        .flatMap(Collection::stream)
        .collect(Collectors.toUnmodifiableList());
  }

  private static String pluralize(int count, String name) {
    return count == 1
        ? "1 " + name
        : count + " " + name + "s";
  }
}
