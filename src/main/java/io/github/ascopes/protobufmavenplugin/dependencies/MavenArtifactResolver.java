/*
 * Copyright (C) 2023, Ashley Scopes.
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
package io.github.ascopes.protobufmavenplugin.dependencies;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.shared.artifact.filter.resolve.ScopeFilter;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolver;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolverException;
import org.apache.maven.shared.transfer.dependencies.resolve.DependencyResolver;
import org.apache.maven.shared.transfer.dependencies.resolve.DependencyResolverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class MavenArtifactResolver {

  private static final Logger LOGGER = LoggerFactory.getLogger(MavenExecutableResolver.class);

  private final ArtifactResolver artifactResolver;
  private final DependencyResolver dependencyResolver;
  private final MavenSession mavenSession;

  /**
   * Initialise the resolver.
   *
   * @param artifactResolver the artifact resolver to use.
   * @param mavenSession     the Maven session to use.
   */
  public MavenArtifactResolver(
      ArtifactResolver artifactResolver,
      DependencyResolver dependencyResolver,
      MavenSession mavenSession
  ) {
    this.artifactResolver = artifactResolver;
    this.dependencyResolver = dependencyResolver;
    this.mavenSession = mavenSession;
  }

  /**
   * Determine the path to the artifact.
   *
   * @param coordinate the artifact coordinate.
   * @return the path to the artifact.
   * @throws DependencyResolutionException if resolution fails for any reason.
   */
  public Path resolveArtifact(
      MavenCoordinate coordinate
  ) throws DependencyResolutionException {
    try {
      LOGGER.info("Resolving {} from Maven repositories", coordinate);

      var request = new DefaultProjectBuildingRequest(mavenSession.getProjectBuildingRequest());
      var result = artifactResolver.resolveArtifact(request, coordinate);
      return result.getArtifact().getFile().toPath();

    } catch (ArtifactResolverException ex) {
      throw new DependencyResolutionException(
          "Failed to resolve " + coordinate + " from Maven repositories",
          ex
      );
    }
  }

  /**
   * Resolve the given dependency coordinate recursively, fetching all dependencies as well.
   *
   * @param coordinate the coordinate to pull.
   * @param scopes     the scopes of dependencies to allow (e.g. {@code "compile"},
   *                   {@code "provided"}).
   * @return the set of paths to all discovered dependencies.
   * @throws DependencyResolutionException if resolution fails.
   */
  public Set<Path> resolveDependencies(
      MavenCoordinate coordinate,
      Collection<String> scopes
  ) throws DependencyResolutionException {
    try {
      var results = new HashSet<Path>();
      results.add(resolveArtifact(coordinate));

      LOGGER.info("Resolving dependencies of {} from Maven repositories", coordinate);

      var request = new DefaultProjectBuildingRequest(mavenSession.getProjectBuildingRequest());
      var scopeFilter = new ScopeFilter(scopes, null);

      for (var result : dependencyResolver.resolveDependencies(request, coordinate, scopeFilter)) {
        var path = result.getArtifact().getFile().toPath();
        LOGGER.trace("Resolved '{}' to '{}'", result.getArtifact(), path);
        results.add(path);
      }

      return Collections.unmodifiableSet(results);
    } catch (DependencyResolverException ex) {
      throw new DependencyResolutionException(
          "Failed to resolve " + coordinate + " and dependencies from Maven repositories",
          ex
      );
    }
  }
}
