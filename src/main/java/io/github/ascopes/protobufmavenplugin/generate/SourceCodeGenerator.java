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

import io.github.ascopes.protobufmavenplugin.dependency.BinaryPluginResolver;
import io.github.ascopes.protobufmavenplugin.dependency.DependencyResolutionDepth;
import io.github.ascopes.protobufmavenplugin.dependency.JvmPluginResolver;
import io.github.ascopes.protobufmavenplugin.dependency.MavenDependencyPathResolver;
import io.github.ascopes.protobufmavenplugin.dependency.ProtocResolver;
import io.github.ascopes.protobufmavenplugin.dependency.ResolutionException;
import io.github.ascopes.protobufmavenplugin.dependency.ResolvedPlugin;
import io.github.ascopes.protobufmavenplugin.execute.ArgLineBuilder;
import io.github.ascopes.protobufmavenplugin.execute.CommandLineExecutor;
import io.github.ascopes.protobufmavenplugin.source.ProtoFileListing;
import io.github.ascopes.protobufmavenplugin.source.ProtoSourceResolver;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Named;
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

  private final MavenDependencyPathResolver mavenDependencyPathResolver;
  private final ProtocResolver protocResolver;
  private final BinaryPluginResolver binaryPluginResolver;
  private final JvmPluginResolver jvmPluginResolver;
  private final ProtoSourceResolver protoListingResolver;
  private final CommandLineExecutor commandLineExecutor;

  @Inject
  public SourceCodeGenerator(
      MavenDependencyPathResolver mavenDependencyPathResolver,
      ProtocResolver protocResolver,
      BinaryPluginResolver binaryPluginResolver,
      JvmPluginResolver jvmPluginResolver,
      ProtoSourceResolver protoListingResolver,
      CommandLineExecutor commandLineExecutor
  ) {
    this.mavenDependencyPathResolver = mavenDependencyPathResolver;
    this.protocResolver = protocResolver;
    this.binaryPluginResolver = binaryPluginResolver;
    this.jvmPluginResolver = jvmPluginResolver;
    this.protoListingResolver = protoListingResolver;
    this.commandLineExecutor = commandLineExecutor;
  }

  public boolean generate(GenerationRequest request) throws ResolutionException, IOException {
    var protocPath = discoverProtocPath(request);

    var plugins = discoverPlugins(request);
    var importPaths = discoverImportPaths(request);
    var sourcePaths = discoverCompilableSources(request);

    if (sourcePaths.isEmpty()) {
      if (request.isFailOnMissingSources()) {
        log.error("No protobuf sources found. If this is unexpected, check your "
            + "configuration and try again.");
        return false;
      } else {
        log.info("No protobuf sources found; nothing to do!");
        return true;
      }
    }

    createOutputDirectories(request);

    var argLineBuilder = new ArgLineBuilder(protocPath)
        .fatalWarnings(request.isFatalWarnings())
        .importPaths(importPaths
            .stream()
            .map(ProtoFileListing::getProtoFilesRoot)
            .collect(Collectors.toCollection(LinkedHashSet::new)))
        .importPaths(request.getSourceRoots())
        .plugins(plugins, request.getOutputDirectory());

    addOptionalOutput(request, GenerationRequest::isCppEnabled, argLineBuilder::cppOut);
    addOptionalOutput(request, GenerationRequest::isCsharpEnabled, argLineBuilder::csharpOut);
    addOptionalOutput(request, GenerationRequest::isKotlinEnabled, argLineBuilder::kotlinOut);
    addOptionalOutput(request, GenerationRequest::isJavaEnabled, argLineBuilder::javaOut);
    addOptionalOutput(request, GenerationRequest::isObjcEnabled, argLineBuilder::objcOut);
    addOptionalOutput(request, GenerationRequest::isPhpEnabled, argLineBuilder::phpOut);
    addOptionalOutput(request, GenerationRequest::isPythonStubsEnabled, argLineBuilder::pyiOut);
    addOptionalOutput(request, GenerationRequest::isPythonEnabled, argLineBuilder::pythonOut);
    addOptionalOutput(request, GenerationRequest::isRubyEnabled, argLineBuilder::rubyOut);
    addOptionalOutput(request, GenerationRequest::isRustEnabled, argLineBuilder::rustOut);

    var sourceFiles = sourcePaths
        .stream()
        .map(ProtoFileListing::getProtoFiles)
        .flatMap(Collection::stream)
        .collect(Collectors.toCollection(LinkedHashSet::new));

    if (!logProtocVersion(protocPath)) {
      log.error("Unable to execute protoc. Ensure the binary is compatible for this platform!");
      return false;
    }

    return commandLineExecutor.execute(argLineBuilder.compile(sourceFiles));
  }

  private Path discoverProtocPath(GenerationRequest request) throws ResolutionException {
    return protocResolver.resolve(request.getMavenSession(), request.getProtocVersion());
  }

  private void addOptionalOutput(
      GenerationRequest request,
      Predicate<GenerationRequest> check,
      BiConsumer<Path, Boolean> consumer
  ) {
    if (check.test(request)) {
      consumer.accept(request.getOutputDirectory(), request.isLiteEnabled());
    }
  }

  private boolean logProtocVersion(Path protocPath) throws IOException {
    var args = new ArgLineBuilder(protocPath).version();
    return commandLineExecutor.execute(args);
  }

  private Collection<ResolvedPlugin> discoverPlugins(
      GenerationRequest request
  ) throws IOException, ResolutionException {
    return concat(
        binaryPluginResolver
            .resolveMavenPlugins(request.getMavenSession(), request.getBinaryMavenPlugins()),
        binaryPluginResolver
            .resolvePathPlugins(request.getBinaryPathPlugins()),
        binaryPluginResolver
            .resolveUrlPlugins(request.getBinaryUrlPlugins()),
        jvmPluginResolver
            .resolveMavenPlugins(request.getMavenSession(), request.getJvmMavenPlugins())
    );
  }

  private Collection<ProtoFileListing> discoverImportPaths(
      GenerationRequest request
  ) throws IOException, ResolutionException {
    var session = request.getMavenSession();

    log.debug(
        "Finding importable protobuf sources from the classpath ({})",
        request.getDependencyResolutionDepth()
    );

    var dependencyPaths = mavenDependencyPathResolver.resolveProjectDependencyPaths(
        session,
        request.getAllowedDependencyScopes(),
        request.getDependencyResolutionDepth()
    );

    var inheritedDependencies = protoListingResolver
        .createProtoFileListings(dependencyPaths);

    // Always use all provided additional import paths, as we assume they are valid given the user
    // has explicitly included them in their configuration.
    var explicitDependencies = protoListingResolver
        .createProtoFileListings(request.getImportPaths());

    return concat(inheritedDependencies, explicitDependencies);
  }

  private Collection<ProtoFileListing> discoverCompilableSources(
      GenerationRequest request
  ) throws IOException {
    log.debug("Discovering all compilable protobuf source files");
    var sources = protoListingResolver.createProtoFileListings(request.getSourceRoots());
    log.info("Will generate source code for {} protobuf file(s)", sources.size());
    return sources;
  }

  private void createOutputDirectories(GenerationRequest request) throws IOException {
    var directory = request.getOutputDirectory();
    log.debug("Creating {}", directory);
    Files.createDirectories(directory);

    if (request.isRegisterAsCompilationRoot()) {
      var registrar = request.getSourceRootRegistrar();
      log.debug("Registering {} as {} compilation root", directory, registrar);
      registrar.registerSourceRoot(request.getMavenSession(), directory);
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
