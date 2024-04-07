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
import io.github.ascopes.protobufmavenplugin.MavenArtifact;
import io.github.ascopes.protobufmavenplugin.dependency.MavenDependencyPathResolver;
import io.github.ascopes.protobufmavenplugin.dependency.PlatformClassifierFactory;
import io.github.ascopes.protobufmavenplugin.dependency.ResolutionException;
import io.github.ascopes.protobufmavenplugin.dependency.SystemPathBinaryResolver;
import io.github.ascopes.protobufmavenplugin.dependency.UrlResourceFetcher;
import io.github.ascopes.protobufmavenplugin.utils.Digests;
import io.github.ascopes.protobufmavenplugin.utils.FileUtils;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.execution.MavenSession;

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

  public Collection<ResolvedPlugin> resolveMavenPlugins(
      MavenSession session,
      Collection<MavenArtifact> plugins
  ) throws ResolutionException {
    return resolveAll(plugins, plugin -> resolveMavenPlugin(session, plugin));
  }

  public Collection<ResolvedPlugin> resolvePathPlugins(
      Collection<String> plugins
  ) throws ResolutionException {
    return resolveAll(plugins, this::resolvePathPlugin);
  }

  public Collection<ResolvedPlugin> resolveUrlPlugins(
      Collection<URL> plugins
  ) throws ResolutionException {
    return resolveAll(plugins, this::resolveUrlPlugin);
  }

  private ResolvedPlugin resolveMavenPlugin(
      MavenSession session,
      MavenArtifact plugin
  ) throws ResolutionException {
    var artifactId = plugin.getArtifactId().orElse(null);

    if (plugin.getClassifier().isEmpty()) {
      plugin.setClassifier(platformClassifierFactory.getClassifier(artifactId));
    }

    if (plugin.getType().isEmpty()) {
      plugin.setType("exe");
    }

    // Only one dependency should ever be returned here.
    var path = dependencyResolver.resolveOne(session, plugin, DependencyResolutionDepth.DIRECT)
        .iterator()
        .next();
    makeExecutable(path);
    return createResolvedPlugin(path);
  }

  private ResolvedPlugin resolvePathPlugin(String binaryName) throws ResolutionException {
    var path = systemPathResolver.resolve(binaryName)
        .orElseThrow(() -> new ResolutionException("No executable '"
            + binaryName + "' was found on the system path"));
    return createResolvedPlugin(path);
  }

  private ResolvedPlugin resolveUrlPlugin(URL url) throws ResolutionException {
    var path = urlResourceFetcher.fetchFileFromUrl(url, ".exe");
    makeExecutable(path);
    return createResolvedPlugin(path);
  }

  private ResolvedPlugin createResolvedPlugin(Path path) {
    return ImmutableResolvedPlugin
        .builder()
        .id(Digests.sha1(path.toString()))
        .path(path)
        .build();
  }

  private <A> Collection<ResolvedPlugin> resolveAll(
      Collection<A> plugins,
      Resolver<A> resolver
  ) throws ResolutionException {
    var resolvedPlugins = new ArrayList<ResolvedPlugin>();
    for (var plugin : plugins) {
      resolvedPlugins.add(resolver.resolve(plugin));
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

    ResolvedPlugin resolve(A arg) throws ResolutionException;
  }
}
