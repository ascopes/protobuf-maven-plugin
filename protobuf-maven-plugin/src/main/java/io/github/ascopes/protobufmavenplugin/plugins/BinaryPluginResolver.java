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
package io.github.ascopes.protobufmavenplugin.plugins;

import static java.util.Objects.requireNonNullElse;

import io.github.ascopes.protobufmavenplugin.dependencies.MavenArtifactPathResolver;
import io.github.ascopes.protobufmavenplugin.dependencies.PlatformClassifierFactory;
import io.github.ascopes.protobufmavenplugin.fs.FileUtils;
import io.github.ascopes.protobufmavenplugin.fs.UriResourceFetcher;
import io.github.ascopes.protobufmavenplugin.utils.Digests;
import io.github.ascopes.protobufmavenplugin.utils.ResolutionException;
import io.github.ascopes.protobufmavenplugin.utils.SystemPathBinaryResolver;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.execution.scope.MojoExecutionScoped;
import org.eclipse.sisu.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Protoc plugin resolver that resolves executable platform binaries.
 *
 * @author Ashley Scopes
 */
@Description("Resolves native binary protoc plugins from various remote and local locations")
@MojoExecutionScoped
@Named
final class BinaryPluginResolver {

  private static final Logger log = LoggerFactory.getLogger(BinaryPluginResolver.class);

  private final MavenArtifactPathResolver artifactPathResolver;
  private final PlatformClassifierFactory platformClassifierFactory;
  private final SystemPathBinaryResolver systemPathResolver;
  private final UriResourceFetcher urlResourceFetcher;

  @Inject
  BinaryPluginResolver(
      MavenArtifactPathResolver artifactPathResolver,
      PlatformClassifierFactory platformClassifierFactory,
      SystemPathBinaryResolver systemPathResolver,
      UriResourceFetcher urlResourceFetcher
  ) {
    this.artifactPathResolver = artifactPathResolver;
    this.platformClassifierFactory = platformClassifierFactory;
    this.systemPathResolver = systemPathResolver;
    this.urlResourceFetcher = urlResourceFetcher;
  }

  Collection<ResolvedProtocPlugin> resolveMavenPlugins(
      Collection<? extends MavenProtocPlugin> plugins,
      Path defaultOutputDirectory
  ) throws ResolutionException {
    return resolveAll(plugins, defaultOutputDirectory, this::resolveMavenPlugin);
  }

  Collection<ResolvedProtocPlugin> resolvePathPlugins(
      Collection<? extends PathProtocPlugin> plugins,
      Path defaultOutputDirectory
  ) throws ResolutionException {
    return resolveAll(plugins, defaultOutputDirectory, this::resolvePathPlugin);
  }

  Collection<ResolvedProtocPlugin> resolveUrlPlugins(
      Collection<? extends UriProtocPlugin> plugins,
      Path defaultOutputDirectory
  ) throws ResolutionException {
    return resolveAll(plugins, defaultOutputDirectory, this::resolveUrlPlugin);
  }

  private Optional<ResolvedProtocPlugin> resolveMavenPlugin(
      MavenProtocPlugin plugin,
      Path defaultOutputDirectory
  ) throws ResolutionException {
    var pluginBuilder = ImmutableMavenProtocPlugin.builder()
        .from(plugin);

    if (plugin.getClassifier() == null) {
      var classifier = platformClassifierFactory.getClassifier(plugin.getArtifactId());
      pluginBuilder.classifier(classifier);
    }

    if (plugin.getType() == null) {
      pluginBuilder.type("exe");
    }

    plugin = pluginBuilder.build();

    log.debug("Resolving Maven protoc plugin {}", plugin);

    var path = artifactPathResolver.resolveArtifact(plugin);
    makeExecutable(path);
    return Optional.of(createResolvedProtocPlugin(plugin, defaultOutputDirectory, path));
  }

  private Optional<ResolvedProtocPlugin> resolvePathPlugin(
      PathProtocPlugin plugin,
      Path defaultOutputDirectory
  ) throws ResolutionException {

    log.debug("Resolving Path protoc plugin {}", plugin);

    var maybePath = systemPathResolver.resolve(plugin.getName());

    if (maybePath.isEmpty() && plugin.isOptional()) {
      return Optional.empty();
    }

    var path = maybePath.orElseThrow(() -> new ResolutionException(
        "No plugin named " + plugin.getName() + " was found on the system path"
    ));

    return Optional.of(createResolvedProtocPlugin(plugin, defaultOutputDirectory, path));
  }

  private Optional<ResolvedProtocPlugin> resolveUrlPlugin(
      UriProtocPlugin plugin,
      Path defaultOutputDirectory
  ) throws ResolutionException {
    log.debug("Resolving URL protoc plugin {}", plugin);

    var maybePath = urlResourceFetcher.fetchFileFromUri(plugin.getUrl(), ".exe");

    if (maybePath.isEmpty() && plugin.isOptional()) {
      return Optional.empty();
    }

    var path = maybePath.orElseThrow(() -> new ResolutionException(
        "Plugin at " + plugin.getUrl() + " does not exist"
    ));

    makeExecutable(path);

    return Optional.of(createResolvedProtocPlugin(plugin, defaultOutputDirectory, path));
  }

  private ResolvedProtocPlugin createResolvedProtocPlugin(
      ProtocPlugin plugin,
      Path defaultOutputDirectory,
      Path path
  ) {
    return ImmutableResolvedProtocPlugin
        .builder()
        .id(Digests.sha1(path.toString()))
        .options(plugin.getOptions())
        .order(plugin.getOrder())
        .outputDirectory(requireNonNullElse(plugin.getOutputDirectory(), defaultOutputDirectory))
        .path(path)
        .build();
  }

  private <P extends ProtocPlugin> Collection<ResolvedProtocPlugin> resolveAll(
      Collection<? extends P> plugins,
      Path defaultOutputDirectory,
      Resolver<? super P> resolver
  ) throws ResolutionException {
    var resolvedPlugins = new ArrayList<ResolvedProtocPlugin>();
    for (var plugin : plugins) {
      if (plugin.isSkip()) {
        log.info("Skipping plugin {}", plugin);
        continue;
      }

      resolver.resolve(plugin, defaultOutputDirectory)
          .ifPresentOrElse(resolvedPlugins::add, skipUnresolvedPlugin(plugin));
    }
    return resolvedPlugins;
  }

  private Runnable skipUnresolvedPlugin(ProtocPlugin plugin) {
    return () -> log.info("Skipping unresolved missing plugin {}", plugin);
  }

  private void makeExecutable(Path path) throws ResolutionException {
    try {
      FileUtils.makeExecutable(path);
    } catch (IOException ex) {
      throw new ResolutionException("Failed to set executable bit on protoc plugin", ex);
    }
  }

  @FunctionalInterface
  private interface Resolver<P extends ProtocPlugin> {

    Optional<ResolvedProtocPlugin> resolve(
        P plugin,
        Path defaultOutputDirectory
    ) throws ResolutionException;
  }
}
