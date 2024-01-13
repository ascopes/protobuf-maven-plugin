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

import io.github.ascopes.protobufmavenplugin.dependency.MavenDependencyPathResolver;
import io.github.ascopes.protobufmavenplugin.dependency.PluginResolver;
import io.github.ascopes.protobufmavenplugin.dependency.ProtocResolver;
import io.github.ascopes.protobufmavenplugin.dependency.ResolutionException;
import io.github.ascopes.protobufmavenplugin.dependency.ResolvedPlugin;
import io.github.ascopes.protobufmavenplugin.execute.ArgLineBuilder;
import io.github.ascopes.protobufmavenplugin.execute.CommandLineExecutor;
import io.github.ascopes.protobufmavenplugin.source.ProtoFileListing;
import io.github.ascopes.protobufmavenplugin.source.ProtoListingResolver;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Generator for source code.
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
  private final PluginResolver pluginResolver;
  private final ProtoListingResolver protoListingResolver;
  private final CommandLineExecutor commandLineExecutor;

  @Inject
  public SourceCodeGenerator(
      MavenDependencyPathResolver mavenDependencyPathResolver,
      ProtocResolver protocResolver,
      PluginResolver pluginResolver,
      ProtoListingResolver protoListingResolver,
      CommandLineExecutor commandLineExecutor
  ) {
    this.mavenDependencyPathResolver = mavenDependencyPathResolver;
    this.protocResolver = protocResolver;
    this.pluginResolver = pluginResolver;
    this.protoListingResolver = protoListingResolver;
    this.commandLineExecutor = commandLineExecutor;
  }

  public boolean generate(GenerationRequest request) throws ResolutionException, IOException {
    var protocPath = discoverProtocPath(request);
    var plugins = discoverPlugins(request);
    var importPaths = discoverImportPaths(request);
    var sourcePaths = discoverCompilableSources(request);

    if (sourcePaths.isEmpty()) {
      // We might want to add the ability to throw an error here in the future.
      // For now, let's just avoid doing additional work. This also prevents protobuf
      // failing because we provided no sources to it.
      log.info("No protobuf sources found; nothing to do!");
      return true;
    }
    
    createOutputDirectories(request);

    var argLineBuilder = new ArgLineBuilder(protocPath)
        .fatalWarnings(request.isFatalWarnings())
        .importPaths(importPaths
            .stream()
            .map(ProtoFileListing::getProtoFilesRoot)
            .collect(Collectors.toUnmodifiableSet()))
        .importPaths(request.getSourceRoots())
        .plugins(plugins, request.getOutputDirectory());

    if (request.isJavaEnabled()) {
      argLineBuilder.javaOut(request.getOutputDirectory(), request.isLiteEnabled());
    }

    if (request.isKotlinEnabled()) {
      argLineBuilder.kotlinOut(request.getOutputDirectory(), request.isLiteEnabled());
    }

    var sourceFiles = sourcePaths
        .stream()
        .map(ProtoFileListing::getProtoFiles)
        .flatMap(Collection::stream)
        .collect(Collectors.toUnmodifiableSet());

    return commandLineExecutor.execute(argLineBuilder.compile(sourceFiles));
  }

  private Path discoverProtocPath(GenerationRequest request) throws ResolutionException {
    return protocResolver.resolve(request.getMavenSession(), request.getProtocVersion());
  }

  private Collection<ResolvedPlugin> discoverPlugins(
      GenerationRequest request
  ) throws ResolutionException {
    return pluginResolver.resolveAll(request.getMavenSession(), request.getAdditionalPlugins());
  }

  private Collection<ProtoFileListing> discoverImportPaths(
      GenerationRequest request
  ) throws IOException, ResolutionException {
    var session = request.getMavenSession();

    log.debug("Finding importable protobuf sources from the classpath");
    var dependencyPaths = mavenDependencyPathResolver
        .resolveProjectDependencyPaths(session, request.getAllowedDependencyScopes());

    var inheritedDependencies = protoListingResolver
        .createProtoFileListings(dependencyPaths);

    // Always use all provided additional import paths, as we assume they are valid given the user
    // has explicitly included them in their configuration.
    var explicitDependencies = protoListingResolver
        .createProtoFileListings(request.getAdditionalImportPaths());

    var allDependencies = new ArrayList<>(inheritedDependencies);
    allDependencies.addAll(explicitDependencies);
    return allDependencies;
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
    var registrar = request.getSourceRootRegistrar();

    log.info("Creating {} and registering as a {} root", directory, registrar);

    Files.createDirectories(directory);
    registrar.registerSourceRoot(request.getMavenSession(), directory);
  }
}
