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

import io.github.ascopes.protobufmavenplugin.plugins.ProjectPluginResolver;
import io.github.ascopes.protobufmavenplugin.plugins.ResolvedProtocPlugin;
import io.github.ascopes.protobufmavenplugin.protoc.ImmutableProtocInvocation;
import io.github.ascopes.protobufmavenplugin.protoc.ProtocExecutor;
import io.github.ascopes.protobufmavenplugin.protoc.ProtocInvocation;
import io.github.ascopes.protobufmavenplugin.protoc.ProtocResolver;
import io.github.ascopes.protobufmavenplugin.protoc.targets.ImmutableDescriptorFileProtocTarget;
import io.github.ascopes.protobufmavenplugin.protoc.targets.ImmutableLanguageProtocTarget;
import io.github.ascopes.protobufmavenplugin.protoc.targets.ImmutablePluginProtocTarget;
import io.github.ascopes.protobufmavenplugin.protoc.targets.ProtocTarget;
import io.github.ascopes.protobufmavenplugin.sources.FilesToCompile;
import io.github.ascopes.protobufmavenplugin.sources.ProjectInputListing;
import io.github.ascopes.protobufmavenplugin.sources.ProjectInputResolver;
import io.github.ascopes.protobufmavenplugin.sources.SourceListing;
import io.github.ascopes.protobufmavenplugin.sources.incremental.IncrementalCacheManager;
import io.github.ascopes.protobufmavenplugin.utils.FileUtils;
import io.github.ascopes.protobufmavenplugin.utils.ResolutionException;
import io.github.ascopes.protobufmavenplugin.utils.StringUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.scope.MojoExecutionScoped;
import org.eclipse.sisu.Description;
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
@Description("Orchestrator for the entire generation process, gluing all components together")
@MojoExecutionScoped
@Named
public final class ProtobufBuildOrchestrator {

  private static final Logger log = LoggerFactory.getLogger(ProtobufBuildOrchestrator.class);

  private final MavenSession mavenSession;
  private final ProtocResolver protocResolver;
  private final ProjectInputResolver projectInputResolver;
  private final ProjectPluginResolver projectPluginResolver;
  private final IncrementalCacheManager incrementalCacheManager;
  private final ProtocExecutor protocExecutor;

  @Inject
  public ProtobufBuildOrchestrator(
      MavenSession mavenSession,
      ProtocResolver protocResolver,
      ProjectInputResolver projectInputResolver,
      ProjectPluginResolver projectPluginResolver,
      IncrementalCacheManager incrementalCacheManager,
      ProtocExecutor protocExecutor
  ) {
    this.mavenSession = mavenSession;
    this.protocResolver = protocResolver;
    this.projectInputResolver = projectInputResolver;
    this.projectPluginResolver = projectPluginResolver;
    this.incrementalCacheManager = incrementalCacheManager;
    this.protocExecutor = protocExecutor;
  }

  public GenerationResult generate(
      GenerationRequest request
  ) throws ResolutionException, IOException {

    log.debug("The provided protobuf GenerationRequest is: {}", request);

    // GH-600: Short circuit and avoid expensive dependency resolution if
    // we can exit early.
    if (request.getSourceDirectories().isEmpty()
        && request.getSourceDependencies().isEmpty()
        && request.getSourceDescriptorPaths().isEmpty()
        && request.getSourceDescriptorDependencies().isEmpty()) {
      return handleMissingInputs(request);
    }

    final var protocPath = discoverProtocPath(request);
    final var resolvedPlugins = projectPluginResolver.resolveProjectPlugins(request);
    final var projectInputs = projectInputResolver.resolveProjectInputs(request);

    if (projectInputs.getCompilableProtoSources().isEmpty()
        && projectInputs.getCompilableDescriptorFiles().isEmpty()) {
      return handleMissingInputs(request);
    }

    if (resolvedPlugins.isEmpty()
        && request.getEnabledLanguages().isEmpty()
        && request.getOutputDescriptorFile() == null) {

      return handleMissingTargets(request);
    }

    createOutputDirectories(request);

    // GH-438: We now register the source roots before generating anything. This ensures we still
    // call Javac with the sources even if we incrementally compile with zero changes.
    registerSourceRoots(request);

    // Determine the sources we need to regenerate. This will be all the sources usually but
    // if incremental compilation is enabled then we will only output the files that have changed
    // unless we deem a full rebuild necessary.
    var compilableFiles = computeFilesToCompile(request, projectInputs);
    if (compilableFiles.isEmpty()) {
      // Nothing to compile. If we hit here, then we likely received inputs but were using
      // incremental compilation and nothing changed since the last build.
      incrementalCacheManager.updateIncrementalCache();
      return GenerationResult.NOTHING_TO_DO;
    }

    var importPaths = Stream
        .of(projectInputs.getCompilableProtoSources(), projectInputs.getDependencyProtoSources())
        .flatMap(Collection::stream)
        .map(SourceListing::getSourceRoot)
        .collect(Collectors.toUnmodifiableList());

    var invocation = createProtocInvocation(
        request,
        protocPath,
        resolvedPlugins,
        importPaths,
        compilableFiles
    );

    if (!protocExecutor.invoke(invocation)) {
      return GenerationResult.PROTOC_FAILED;
    }

    // Since we've succeeded in the codegen phase, we can replace the old incremental cache
    // with the new one.
    incrementalCacheManager.updateIncrementalCache();

    if (request.getOutputDescriptorFile() != null && request.isOutputDescriptorAttached()) {
      request.getOutputDescriptorAttachmentRegistrar().registerAttachedArtifact(
          mavenSession,
          request.getOutputDescriptorFile(),
          request.getOutputDescriptorAttachmentType(),
          request.getOutputDescriptorAttachmentClassifier()
      );
    }

    if (request.isEmbedSourcesInClassOutputs()) {
      embedSourcesInClassOutputs(
          request.getSourceRootRegistrar(),
          projectInputs.getCompilableProtoSources()
      );
    }

    return GenerationResult.PROTOC_SUCCEEDED;
  }

  private GenerationResult handleMissingInputs(GenerationRequest request) {
    var message = "No protobuf sources found. If this is unexpected, check your "
        + "configuration and try again.";

    if (request.isFailOnMissingSources()) {
      log.error("{}", message);
      return GenerationResult.NO_SOURCES;
    }

    log.warn("{}", message);
    return GenerationResult.NOTHING_TO_DO;
  }

  private GenerationResult handleMissingTargets(GenerationRequest request) {
    var message = "No output languages or descriptors are enabled and no plugins were found. "
        + "If this is unexpected, check your configuration and try again.";

    if (request.isFailOnMissingTargets()) {
      log.error("{}", message);
      return GenerationResult.NO_TARGETS;
    }

    log.warn("{}", message);
    return GenerationResult.NOTHING_TO_DO;
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

    Optional.ofNullable(request.getOutputDescriptorFile())
        .map(p -> p.toAbsolutePath().getParent())
        .ifPresent(p -> {
          try {
            Files.createDirectories(p);
          } catch (IOException e) {
            throw new IllegalStateException(
                "Failed to create output directory for descriptor file '"
                    + p + "'", e);
          }
        });
  }

  private void registerSourceRoots(GenerationRequest request) {
    if (request.isRegisterAsCompilationRoot()) {
      request.getSourceRootRegistrar().registerSourceRoot(
          mavenSession,
          request.getOutputDirectory()
      );
    }
  }

  private FilesToCompile computeFilesToCompile(
      GenerationRequest request,
      ProjectInputListing projectInputs
  ) throws IOException {
    var totalSourceFileCount = projectInputs.getCompilableProtoSources().stream()
        .mapToInt(sourcePath -> sourcePath.getSourceFiles().size())
        .sum();
    var totalDescriptorFileCount = projectInputs.getCompilableDescriptorFiles().stream()
        .mapToInt(sourcePath -> sourcePath.getSourceFiles().size())
        .sum();

    var filesToCompile = shouldIncrementallyCompile(request)
        ? incrementalCacheManager.determineSourcesToCompile(projectInputs)
        : FilesToCompile.allOf(projectInputs);

    if (filesToCompile.isEmpty()) {
      log.info(
          "Found {} and {} to compile, but all are up-to-date so none will be built this time",
          StringUtils.pluralize(totalSourceFileCount, "proto source file"),
          StringUtils.pluralize(totalDescriptorFileCount, "descriptor file")
      );
      return FilesToCompile.empty();
    }

    log.info(
        "Generating source code from {} and {} (discovered a total of {} and {})",
        StringUtils.pluralize(filesToCompile.getProtoSources().size(), "proto source file"),
        StringUtils.pluralize(filesToCompile.getDescriptorFiles().size(), "descriptor file"),
        StringUtils.pluralize(totalSourceFileCount, "proto source file"),
        StringUtils.pluralize(totalDescriptorFileCount, "descriptor file")
    );

    return filesToCompile;
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

  private ProtocInvocation createProtocInvocation(
      GenerationRequest request,
      Path protocPath,
      Collection<ResolvedProtocPlugin> resolvedPlugins,
      Collection<Path> protoImportPaths,
      FilesToCompile filesToCompile
  ) {
    var targets = new TreeSet<ProtocTarget>();

    request.getEnabledLanguages()
        .stream()
        .map(language -> ImmutableLanguageProtocTarget.builder()
            .isLite(request.isLiteEnabled())
            .language(language)
            .outputPath(request.getOutputDirectory())
            .build())
        .forEach(targets::add);

    resolvedPlugins.stream()
        .map(plugin -> ImmutablePluginProtocTarget.builder()
            .plugin(plugin)
            .outputPath(request.getOutputDirectory())
            .build())
        .forEach(targets::add);

    if (request.getOutputDescriptorFile() != null) {
      var descriptorFileTarget = ImmutableDescriptorFileProtocTarget.builder()
          .isIncludeImports(request.isOutputDescriptorIncludeImports())
          .isIncludeSourceInfo(request.isOutputDescriptorIncludeSourceInfo())
          .isRetainOptions(request.isOutputDescriptorRetainOptions())
          .outputFile(request.getOutputDescriptorFile())
          .build();

      targets.add(descriptorFileTarget);
    }

    return ImmutableProtocInvocation.builder()
        .importPaths(protoImportPaths)
        .inputDescriptorFiles(filesToCompile.getDescriptorFiles())
        .isFatalWarnings(request.isFatalWarnings())
        .protocPath(protocPath)
        .sourcePaths(filesToCompile.getProtoSources())
        .targets(targets)
        .build();
  }
}
