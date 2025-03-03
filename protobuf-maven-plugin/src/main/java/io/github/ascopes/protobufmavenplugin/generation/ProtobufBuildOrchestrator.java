/*
 * Copyright (C) 2023 - 2025, Ashley Scopes.
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

import static io.github.ascopes.protobufmavenplugin.sources.SourceListing.flattenSourceProtoFiles;

import io.github.ascopes.protobufmavenplugin.plugins.ProjectPluginResolver;
import io.github.ascopes.protobufmavenplugin.protoc.CommandLineExecutor;
import io.github.ascopes.protobufmavenplugin.protoc.ProtocArgumentFileBuilderBuilder;
import io.github.ascopes.protobufmavenplugin.protoc.ProtocResolver;
import io.github.ascopes.protobufmavenplugin.sources.ProjectInputListing;
import io.github.ascopes.protobufmavenplugin.sources.ProjectInputResolver;
import io.github.ascopes.protobufmavenplugin.sources.SourceListing;
import io.github.ascopes.protobufmavenplugin.sources.incremental.IncrementalCacheManager;
import io.github.ascopes.protobufmavenplugin.utils.FileUtils;
import io.github.ascopes.protobufmavenplugin.utils.ResolutionException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
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
  private final IncrementalCacheManager incrementalCacheManager;
  private final CommandLineExecutor commandLineExecutor;

  @Inject
  public ProtobufBuildOrchestrator(
      MavenSession mavenSession,
      ProtocResolver protocResolver,
      ProjectInputResolver projectInputResolver,
      ProjectPluginResolver projectPluginResolver,
      IncrementalCacheManager incrementalCacheManager,
      CommandLineExecutor commandLineExecutor
  ) {
    this.mavenSession = mavenSession;
    this.protocResolver = protocResolver;
    this.projectInputResolver = projectInputResolver;
    this.projectPluginResolver = projectPluginResolver;
    this.incrementalCacheManager = incrementalCacheManager;
    this.commandLineExecutor = commandLineExecutor;
  }

  public boolean generate(GenerationRequest request) throws ResolutionException, IOException {
    log.debug("The provided protobuf GenerationRequest is: {}", request);

    final var protocPath = discoverProtocPath(request);

    final var resolvedPlugins = projectPluginResolver.resolveProjectPlugins(request);
    final var projectInputs = projectInputResolver.resolveProjectInputs(request);

    if (projectInputs.getCompilableSources().isEmpty()) {
      if (request.isFailOnMissingSources()) {
        log.error("No protobuf sources found. If this is unexpected, check your "
            + "configuration and try again.");
        return false;
      } else {
        log.warn("No protobuf sources found.");
        return true;
      }
    }

    if (resolvedPlugins.isEmpty() && request.getEnabledLanguages().isEmpty()
        && request.getOutputDescriptorFile() == null) {
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

    // GH-438: We now register the source roots before generating anything. This ensures we still
    // call Javac with the sources even if we incrementally compile with zero changes.
    registerSourceRoots(request);

    // Determine the sources we need to regenerate. This will be all the sources usually but
    // if incremental compilation is enabled then we will only output the files that have changed
    // unless we deem a full rebuild necessary.
    var compilableSources = computeActualSourcesToCompile(request, projectInputs);
    if (compilableSources.isEmpty()) {
      // Nothing to compile. If we hit here, then we likely received inputs but were using
      // incremental compilation and nothing changed since the last build.
      incrementalCacheManager.updateIncrementalCache();
      return true;
    }

    var args = new ProtocArgumentFileBuilderBuilder()
        .addLanguages(
            request.getEnabledLanguages(),
            request.getOutputDirectory(),
            request.isLiteEnabled())
        .addImportPaths(projectInputs.getCompilableSources()
            .stream()
            .map(SourceListing::getSourceRoot)
            .collect(Collectors.toUnmodifiableList()))
        .addImportPaths(projectInputs.getDependencySources()
            .stream()
            .map(SourceListing::getSourceRoot)
            .collect(Collectors.toUnmodifiableList()))
        .addPlugins(resolvedPlugins, request.getOutputDirectory())
        .addSourcePaths(compilableSources)
        .setFatalWarnings(request.isFatalWarnings());

    if (request.getOutputDescriptorFile() != null) {
      args.setOutputDescriptorFile(
          request.getOutputDescriptorFile(),
          request.isOutputDescriptorIncludeImports(),
          request.isOutputDescriptorIncludeSourceInfo(),
          request.isOutputDescriptorRetainOptions()
      );

      if (request.isOutputDescriptorAttached()) {
        request.getOutputDescriptorAttachmentRegistrar().registerAttachedArtifact(
            mavenSession,
            request.getOutputDescriptorFile(),
            request.getOutputDescriptorAttachmentType(),
            request.getOutputDescriptorAttachmentClassifier()
        );
      }
    }

    if (!commandLineExecutor.execute(protocPath, args.build())) {
      return false;
    }

    // Since we've succeeded in the codegen phase, we can replace the old incremental cache
    // with the new one.
    incrementalCacheManager.updateIncrementalCache();

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
    if (request.isRegisterAsCompilationRoot()) {
      request.getSourceRootRegistrar().registerSourceRoot(
          mavenSession,
          request.getOutputDirectory()
      );
    }
  }

  private Collection<Path> computeActualSourcesToCompile(
      GenerationRequest request,
      ProjectInputListing projectInputs
  ) throws IOException {
    var totalSourceFileCount = projectInputs.getCompilableSources().stream()
        .mapToInt(sourcePath -> sourcePath.getSourceProtoFiles().size())
        .sum();

    var sourcesToCompile = shouldIncrementallyCompile(request)
        ? incrementalCacheManager.determineSourcesToCompile(projectInputs)
        : flattenSourceProtoFiles(projectInputs.getCompilableSources());

    if (sourcesToCompile.isEmpty()) {
      log.info(
          "Found {} source files, all are up-to-date, none will be regenerated this time",
          totalSourceFileCount
      );
      return List.of();
    }

    log.info(
        "Generating source code from {} (discovered {} within {})",
        pluralize(sourcesToCompile.size(), "source file"),
        pluralize(totalSourceFileCount, "source file"),
        pluralize(projectInputs.getCompilableSources().size(), "source path")
    );

    return sourcesToCompile;
  }

  private boolean shouldIncrementallyCompile(GenerationRequest request) {
    if (!request.isIncrementalCompilationEnabled()) {
      log.debug("Incremental compilation is disabled");
      return false;
    }

    if (request.getOutputDescriptorFile() != null) {
      // Protoc does not selectively update an existing descriptor with differentiated
      // changes. Using incremental compilation will result in behaviour that is
      // inconsistent, so do not allow it here.
      log.info("Incremental compilation is disabled since proto descriptor generation "
          + "has been requested.");
      return false;
    }

    log.debug("Will use incremental compilation");
    return true;
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

  private static String pluralize(int count, String name) {
    return count == 1
        ? "1 " + name
        : count + " " + name + "s";
  }
}
