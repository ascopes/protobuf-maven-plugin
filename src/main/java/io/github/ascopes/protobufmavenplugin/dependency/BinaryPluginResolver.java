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
package io.github.ascopes.protobufmavenplugin.dependency;

import io.github.ascopes.protobufmavenplugin.platform.FileUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.UUID;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.shared.transfer.artifact.ArtifactCoordinate;

/**
 * Protoc plugin resolver for native binaries on the system.
 *
 * @author Ashley Scopes
 */
@Named
public final class BinaryPluginResolver {

  private final MavenDependencyPathResolver mavenDependencyPathResolver;
  private final PlatformArtifactFactory platformDependencyFactory;
  private final SystemPathBinaryResolver systemPathResolver;

  @Inject
  public BinaryPluginResolver(
      MavenDependencyPathResolver mavenDependencyPathResolver,
      PlatformArtifactFactory platformDependencyFactory,
      SystemPathBinaryResolver systemPathResolver
  ) {
    this.mavenDependencyPathResolver = mavenDependencyPathResolver;
    this.platformDependencyFactory = platformDependencyFactory;
    this.systemPathResolver = systemPathResolver;
  }

  public Collection<ResolvedPlugin> resolveMavenPlugins(
      MavenSession session,
      Collection<ArtifactCoordinate> plugins
  ) throws ResolutionException {
    var resolvedPlugins = new ArrayList<ResolvedPlugin>();

    for (var plugin : plugins) {
      resolvedPlugins.add(resolveMavenPlugin(session, plugin));
    }

    return resolvedPlugins;
  }

  private ResolvedPlugin resolveMavenPlugin(
      MavenSession session,
      ArtifactCoordinate plugin
  ) throws ResolutionException {
    var coordinate = enrich(plugin);
    try {
      var path = mavenDependencyPathResolver.resolveArtifact(session, coordinate);
      FileUtils.makeExecutable(path);
      return createResolvedPlugin(path);
    } catch (IOException ex) {
      throw new ResolutionException("Failed to set executable bit on protoc plugin", ex);
    }
  }

  public Collection<ResolvedPlugin> resolvePathPlugins(
      Collection<String> plugins
  ) throws ResolutionException {
    var resolvedPlugins = new ArrayList<ResolvedPlugin>();

    for (var plugin : plugins) {
      resolvedPlugins.add(resolvePathPlugin(plugin));
    }

    return resolvedPlugins;
  }

  private ResolvedPlugin resolvePathPlugin(String binaryName) throws ResolutionException {
    var path = systemPathResolver.resolve(binaryName)
        .orElseThrow(() -> new ResolutionException("No executable '"
            + binaryName + "' was found on the system path"));
    return createResolvedPlugin(path);
  }

  private ResolvedPlugin createResolvedPlugin(Path path) {
    return ImmutableResolvedPlugin
        .builder()
        .id(UUID.randomUUID().toString())
        .path(path)
        .build();
  }

  private ArtifactCoordinate enrich(ArtifactCoordinate coordinate) {
    // If the extension is null, then Maven treats this as a JAR by default, which is
    // annoying and the opposite of what we actually want to happen. If we pass a JAR
    // here, then explicitly swap it out with null as this is *never* what we want to
    // happen here.
    var extension = coordinate.getExtension().equals("jar")
        ? null
        : coordinate.getExtension();

    return platformDependencyFactory.createArtifact(
        coordinate.getGroupId(),
        coordinate.getArtifactId(),
        coordinate.getVersion(),
        extension,
        coordinate.getClassifier()
    );
  }
}
