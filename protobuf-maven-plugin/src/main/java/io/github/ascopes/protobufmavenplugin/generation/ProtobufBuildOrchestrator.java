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

import io.github.ascopes.protobufmavenplugin.plugins.ProjectPluginResolver;
import io.github.ascopes.protobufmavenplugin.protoc.ArgLineBuilder;
import io.github.ascopes.protobufmavenplugin.protoc.CommandLineExecutor;
import io.github.ascopes.protobufmavenplugin.protoc.ProtocResolver;
import io.github.ascopes.protobufmavenplugin.sources.ProjectInputResolver;
import io.github.ascopes.protobufmavenplugin.sources.SourceListing;
import io.github.ascopes.protobufmavenplugin.utils.FileUtils;
import io.github.ascopes.protobufmavenplugin.utils.ResolutionException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
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
  private final ProtocResolver protocResolver;
  private final ProjectInputResolver projectInputResolver;
  private final ProjectPluginResolver projectPluginResolver;
  private final CommandLineExecutor commandLineExecutor;

  @Inject
  public ProtobufBuildOrchestrator(
      MavenSession mavenSession,
      ProtocResolver protocResolver,
      ProjectInputResolver projectInputResolver,
      ProjectPluginResolver projectPluginResolver,
      CommandLineExecutor commandLineExecutor
  ) {
    this.mavenSession = mavenSession;
    this.protocResolver = protocResolver;
    this.projectInputResolver = projectInputResolver;
    this.projectPluginResolver = projectPluginResolver;
    this.commandLineExecutor = commandLineExecutor;
  }

  public boolean generate(GenerationRequest request) throws ResolutionException, IOException {
    log.debug("Protobuf GenerationRequest is: {}", request);

    final var protocPath = discoverProtocPath(request);

    final var resolvedPlugins = projectPluginResolver.resolveProjectPlugins(request);
    final var projectInputs = projectInputResolver.resolveProjectInputs(request);

    if (projectInputs.getCompilableSources().isEmpty()) {
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
        .importPaths(projectInputs.getCompilableSources())
        .importPaths(projectInputs.getDependencySources());

    request.getEnabledLanguages()
        .forEach(language -> argLineBuilder.generateCodeFor(
            language,
            request.getOutputDirectory(),
            request.isLiteEnabled()
        ));

    // GH-269: Add the plugins after the enabled languages to support generated code injection
    argLineBuilder.plugins(resolvedPlugins, request.getOutputDirectory());

    var argLine = argLineBuilder.compile(projectInputs.getCompilableSources());

    if (!commandLineExecutor.execute(argLine)) {
      return false;
    }

    registerSourceRoots(request);

    if (request.isEmbedSourcesInClassOutputs()) {
      embedSourcesInClassOutputs(
          request.getSourceRootRegistrar(),
          projectInputs.getCompilableSources()
      );
    }

    return true;
  }

  private Path discoverProtocPath(GenerationRequest request) throws ResolutionException {
    return protocResolver.resolve(request.getProtocVersion())
        .orElseThrow(() -> new ResolutionException("Protoc binary was not found"));
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
            "Failed to embed " + listing.getSourceRoot() + " into the class outputs directory",
            ex
        );
      }
    }
  }
}
