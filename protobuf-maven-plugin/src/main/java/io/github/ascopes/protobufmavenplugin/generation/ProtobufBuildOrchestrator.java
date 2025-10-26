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

import static java.util.function.Function.identity;

import io.github.ascopes.protobufmavenplugin.fs.FileUtils;
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
import io.github.ascopes.protobufmavenplugin.sources.DescriptorListing;
import io.github.ascopes.protobufmavenplugin.sources.FilesToCompile;
import io.github.ascopes.protobufmavenplugin.sources.ProjectInputListing;
import io.github.ascopes.protobufmavenplugin.sources.ProjectInputResolver;
import io.github.ascopes.protobufmavenplugin.sources.SourceListing;
import io.github.ascopes.protobufmavenplugin.sources.incremental.IncrementalCacheManager;
import io.github.ascopes.protobufmavenplugin.utils.ResolutionException;
import io.github.ascopes.protobufmavenplugin.utils.StringUtils;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
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

    log.debug("Processing generation request {}", request);

    // GH-600: Short circuit and avoid expensive dependency resolution if
    // we can exit early.
    if (request.getSourceDirectories().isEmpty()
        && request.getSourceDependencies().isEmpty()
        && request.getSourceDescriptorPaths().isEmpty()
        && request.getSourceDescriptorDependencies().isEmpty()) {
      return handleMissingInputs(request);
    }

    final var incrementalCompilation = shouldIncrementallyCompile(request);
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

    createOutputDirectories(request, resolvedPlugins, incrementalCompilation);

    // GH-438: We now register the source roots before generating anything. This ensures we still
    // call Javac with the sources even if we incrementally compile with zero changes.
    registerSourceRoots(request, resolvedPlugins);

    // Determine the sources we need to regenerate. This will be all the sources usually but
    // if incremental compilation is enabled then we will only output the files that have changed
    // unless we deem a full rebuild necessary.
    var compilableFiles = computeFilesToCompile(request, projectInputs, incrementalCompilation);
    if (compilableFiles.isEmpty()) {
      // Nothing to compile. If we hit here, then we likely received inputs but were using
      // incremental compilation and nothing changed since the last build.
      incrementalCacheManager.updateIncrementalCache();
      return GenerationResult.NOTHING_TO_DO;
    }

    var invocation = createProtocInvocation(
        request,
        protocPath,
        resolvedPlugins,
        projectInputs,
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
    log.error(
        "No protobuf sources found. If this is unexpected, check your configuration and try again. "
            + "If this is expected, run Maven with -Dprotobuf.skip to skip the plugin execution.");
    return GenerationResult.NO_SOURCES;
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
    return protocResolver.resolve(request.getProtocVersion(), request.getProtocDigest())
        .orElseThrow(() -> new ResolutionException("Protoc binary was not found"));
  }

  private void createOutputDirectories(
      GenerationRequest request,
      Collection<ResolvedProtocPlugin> resolvedProtocPlugins,
      boolean incrementalCompilation
  ) throws IOException {
    var outputDirectories = Stream
        .of(
            // Project output directory.
            Stream.of(request.getOutputDirectory()),
            // Output descriptor file location, if non-null.
            Optional.ofNullable(request.getOutputDescriptorFile())
                .map(p -> p.toAbsolutePath().getParent())
                .stream(),
            // Custom output directories for plugins, if overriding the project defaults.
            resolvedProtocPlugins.stream()
                .map(ResolvedProtocPlugin::getOutputDirectory)
                .filter(Objects::nonNull)
        )
        .flatMap(identity())
        .collect(Collectors.toUnmodifiableList());

    if (!incrementalCompilation && request.isCleanOutputDirectories()) {
      for (var outputDirectory : outputDirectories) {
        log.info("Deleting outputs from previous build in \"{}\"", outputDirectory);
        FileUtils.deleteTree(outputDirectory);
      }
    }

    for (var outputDirectory : outputDirectories) {
      log.debug("Creating output directory \"{}\"", outputDirectory);
      Files.createDirectories(outputDirectory);
    }
  }

  private void registerSourceRoots(
      GenerationRequest request,
      Collection<ResolvedProtocPlugin> resolvedProtocPlugins
  ) {
    var registrar = request.getSourceRootRegistrar();
    Stream
        .of(
            // Project output directory, if we allow registration of compilation roots.
            Stream.of(request.getOutputDirectory())
                .filter(dir -> request.isRegisterAsCompilationRoot()),

            // Custom output directories for plugins, if we explicitly allow them to be used as
            // compilation roots, or if we do not override the behaviour and the project default is
            // to use them as compilation roots anyway.
            resolvedProtocPlugins.stream()
                .filter(plugin -> plugin.getRegisterAsCompilationRoot()
                    .orElseGet(request::isRegisterAsCompilationRoot))
                .map(ResolvedProtocPlugin::getOutputDirectory)
                .filter(Objects::nonNull)
        )
        .flatMap(identity())
        .forEach(outputDirectory -> registrar.registerSourceRoot(mavenSession, outputDirectory));
  }

  // TODO: migrate this logic to a compilation strategy.
  private FilesToCompile computeFilesToCompile(
      GenerationRequest request,
      ProjectInputListing projectInputs,
      boolean incrementalCompilation
  ) throws IOException {
    var totalSourceFileCount = projectInputs.getCompilableProtoSources().stream()
        .mapToInt(sourcePath -> sourcePath.getSourceFiles().size())
        .sum();
    var totalDescriptorFileCount = projectInputs.getCompilableDescriptorFiles().stream()
        .mapToInt(sourcePath -> sourcePath.getSourceFiles().size())
        .sum();

    var filesToCompile = incrementalCompilation
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

  // TODO: migrate this logic to a compilation strategy
  private boolean shouldIncrementallyCompile(GenerationRequest request) {
    if (!request.isIncrementalCompilationEnabled()) {
      log.debug("Incremental compilation was disabled by the user");
      return false;
    }

    log.debug("Incremental compilation was enabled by the user");

    if (request.getOutputDescriptorFile() != null) {
      // Protoc does not selectively update an existing descriptor with differentiated
      // changes. Using incremental compilation will result in behaviour that is
      // inconsistent, so do not allow it here.
      log.warn("Incremental compilation will be disabled since descriptors will be generated");
      return false;
    }

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
            "Failed to embed \"" + listing.getSourceRoot() + "\" into the class outputs directory",
            ex
        );
      }
    }
  }

  private ProtocInvocation createProtocInvocation(
      GenerationRequest request,
      Path protocPath,
      Collection<ResolvedProtocPlugin> resolvedPlugins,
      ProjectInputListing projectInputs,
      FilesToCompile filesToCompile
  ) {
    var targets = new TreeSet<ProtocTarget>();

    request.getEnabledLanguages()
        .stream()
        .map(language -> ImmutableLanguageProtocTarget.builder()
            .language(language)
            .lite(request.isLiteEnabled())
            .outputPath(request.getOutputDirectory())
            .build())
        .forEach(targets::add);

    resolvedPlugins.stream()
        .map(plugin -> ImmutablePluginProtocTarget.builder()
            .plugin(plugin)
            .build())
        .forEach(targets::add);

    if (request.getOutputDescriptorFile() != null) {
      var descriptorFileTarget = ImmutableDescriptorFileProtocTarget.builder()
          .includeImports(request.isOutputDescriptorIncludeImports())
          .includeSourceInfo(request.isOutputDescriptorIncludeSourceInfo())
          .outputFile(request.getOutputDescriptorFile())
          .retainOptions(request.isOutputDescriptorRetainOptions())
          .build();

      targets.add(descriptorFileTarget);
    }

    var importPaths = Stream
        .of(projectInputs.getCompilableProtoSources(), projectInputs.getDependencyProtoSources())
        .flatMap(Collection::stream)
        .map(SourceListing::getSourceRoot)
        .collect(Collectors.toUnmodifiableList());

    var inputDescriptorFiles = projectInputs.getCompilableDescriptorFiles()
        .stream()
        .map(DescriptorListing::getDescriptorFilePath)
        .collect(Collectors.toUnmodifiableList());

    return ImmutableProtocInvocation.builder()
        .arguments(request.getArguments())
        .descriptorSourceFiles(filesToCompile.getDescriptorFiles())
        .environmentVariables(request.getEnvironmentVariables())
        .fatalWarnings(request.isFatalWarnings())
        .importPaths(importPaths)
        .inputDescriptorFiles(inputDescriptorFiles)
        .protocPath(protocPath)
        .sanctionedExecutablePath(request.getSanctionedExecutablePath())
        .sourcePaths(filesToCompile.getProtoSources())
        .targets(targets)
        .build();
  }
}
