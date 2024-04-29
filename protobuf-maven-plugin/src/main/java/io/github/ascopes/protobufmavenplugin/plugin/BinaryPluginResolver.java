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

package io.github.ascopes.protobufmavenplugin.plugin;

import io.github.ascopes.protobufmavenplugin.DependencyResolutionDepth;
import io.github.ascopes.protobufmavenplugin.ImmutableMavenProtocPlugin;
import io.github.ascopes.protobufmavenplugin.MavenProtocPlugin;
import io.github.ascopes.protobufmavenplugin.PathProtocPlugin;
import io.github.ascopes.protobufmavenplugin.ProtocPlugin;
import io.github.ascopes.protobufmavenplugin.UrlProtocPlugin;
import io.github.ascopes.protobufmavenplugin.dependency.MavenDependencyPathResolver;
import io.github.ascopes.protobufmavenplugin.dependency.PlatformClassifierFactory;
import io.github.ascopes.protobufmavenplugin.dependency.ResolutionException;
import io.github.ascopes.protobufmavenplugin.dependency.SystemPathBinaryResolver;
import io.github.ascopes.protobufmavenplugin.dependency.UrlResourceFetcher;
import io.github.ascopes.protobufmavenplugin.utils.Digests;
import io.github.ascopes.protobufmavenplugin.utils.FileUtils;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;

/**
 * Protoc plugin resolver for native binaries on the system.
 *
 * @author Ashley Scopes
 */
@Named
public final class BinaryPluginResolver {

  private final MavenDependencyPathResolver dependencyResolver;
  private final PlatformClassifierFactory platformClassifierFactory;
  private final SystemPathBinaryResolver systemPathResolver;
  private final UrlResourceFetcher urlResourceFetcher;

  @Inject
  public BinaryPluginResolver(
      MavenDependencyPathResolver dependencyResolver,
      PlatformClassifierFactory platformClassifierFactory,
      SystemPathBinaryResolver systemPathResolver,
      UrlResourceFetcher urlResourceFetcher
  ) {
    this.dependencyResolver = dependencyResolver;
    this.platformClassifierFactory = platformClassifierFactory;
    this.systemPathResolver = systemPathResolver;
    this.urlResourceFetcher = urlResourceFetcher;
  }

  public Collection<ResolvedProtocPlugin> resolveMavenPlugins(
      Collection<? extends MavenProtocPlugin> plugins
  ) throws ResolutionException {
    return resolveAll(plugins, this::resolveMavenPlugin);
  }

  public Collection<ResolvedProtocPlugin> resolvePathPlugins(
      Collection<? extends PathProtocPlugin> plugins
  ) throws ResolutionException {
    return resolveAll(plugins, this::resolvePathPlugin);
  }

  public Collection<ResolvedProtocPlugin> resolveUrlPlugins(
      Collection<? extends UrlProtocPlugin> plugins
  ) throws ResolutionException {
    return resolveAll(plugins, this::resolveUrlPlugin);
  }

  private Optional<ResolvedProtocPlugin> resolveMavenPlugin(
      MavenProtocPlugin plugin
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

    // Only one dependency should ever be returned here.
    var path = dependencyResolver.resolveOne(plugin, DependencyResolutionDepth.DIRECT)
        .iterator()
        .next();

    if (plugin.isOptional() != null && plugin.isOptional() && !Files.exists(path)) {
      return Optional.empty();
    }

    makeExecutable(path);
    return Optional.of(createResolvedProtocPlugin(plugin, path));
  }

  private Optional<ResolvedProtocPlugin> resolvePathPlugin(
      PathProtocPlugin plugin
  ) throws ResolutionException {
    var path = systemPathResolver.resolve(plugin.getName());

    if (!path.isPresent()) {
      if (plugin.isOptional() != null && plugin.isOptional()) {
        return Optional.empty();
      }

      new ResolutionException(
          "No executable '" + plugin.getName() + "' was found on the system path");
    }

    return Optional.of(createResolvedProtocPlugin(plugin, path.get()));
  }

  private Optional<ResolvedProtocPlugin> resolveUrlPlugin(
      UrlProtocPlugin plugin
  ) throws ResolutionException {
    var path = urlResourceFetcher.fetchFileFromUrl(plugin.getUrl(), ".exe");

    if (plugin.isOptional() != null && plugin.isOptional() && !Files.exists(path)) {
      return Optional.empty();
    }

    makeExecutable(path);

    return Optional.of(createResolvedProtocPlugin(plugin, path));
  }

  private ResolvedProtocPlugin createResolvedProtocPlugin(ProtocPlugin plugin, Path path) {
    return ImmutableResolvedProtocPlugin
        .builder()
        .id(Digests.sha1(path.toString()))
        .path(path)
        .options(plugin.getOptions())
        .build();
  }

  private <A> Collection<ResolvedProtocPlugin> resolveAll(
      Collection<A> plugins,
      Resolver<A> resolver
  ) throws ResolutionException {
    var resolvedPlugins = new ArrayList<ResolvedProtocPlugin>();
    for (var plugin : plugins) {
      resolver.resolve(plugin).ifPresent(resolvedPlugins::add);
    }
    return resolvedPlugins;
  }

  private void makeExecutable(Path path) throws ResolutionException {
    try {
      FileUtils.makeExecutable(path);
    } catch (IOException ex) {
      throw new ResolutionException("Failed to set executable bit on protoc plugin", ex);
    }
  }

  @FunctionalInterface
  private interface Resolver<A> {

    Optional<ResolvedProtocPlugin> resolve(A arg) throws ResolutionException;
  }
}
