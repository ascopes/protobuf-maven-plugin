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

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.shared.artifact.filter.resolve.ScopeFilter;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolver;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolverException;
import org.apache.maven.shared.transfer.dependencies.resolve.DependencyResolver;
import org.apache.maven.shared.transfer.dependencies.resolve.DependencyResolverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves Maven dependencies, and determines their location on the root file system.
 *
 * @author Ashley Scopes
 */
@Named
public final class MavenDependencyPathResolver {

  private static final Logger log = LoggerFactory.getLogger(MavenDependencyPathResolver.class);

  private final ArtifactResolver artifactResolver;
  private final DependencyResolver dependencyResolver;

  @Inject
  public MavenDependencyPathResolver(
      ArtifactResolver artifactResolver,
      DependencyResolver dependencyResolver
  ) {
    this.artifactResolver = artifactResolver;
    this.dependencyResolver = dependencyResolver;
  }

  public Collection<Path> resolveProjectDependencyPaths(
      MavenSession session,
      Set<String> allowedScopes,
      DependencyResolutionScope dependencyResolutionScope
  ) throws ResolutionException {
    var paths = new ArrayList<Path>();

    for (var dependency : session.getCurrentProject().getDependencies()) {
      var artifact = MavenArtifact.fromDependency(dependency);
      paths.addAll(resolveDependencyTreePaths(
          session,
          allowedScopes,
          dependencyResolutionScope,
          artifact
      ));
    }

    return paths;
  }

  public Collection<Path> resolveDependencyTreePaths(
      MavenSession session,
      Set<String> allowedScopes,
      DependencyResolutionScope dependencyResolutionScope,
      MavenArtifact artifact
  ) throws ResolutionException {
    log.debug("Resolving dependency '{}'", artifact);

    var allDependencyPaths = new ArrayList<Path>();
    var artifactPath = resolveArtifact(session, artifact);
    allDependencyPaths.add(artifactPath);

    if (dependencyResolutionScope == DependencyResolutionScope.DIRECT) {
      log.debug("Not resolving transitive dependencies of '{}'", artifact);
      return allDependencyPaths;
    }

    var request = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
    var scopes = ScopeFilter.including(allowedScopes);
    var coordinate = artifact.toDependableCoordinate();

    try {
      for (var next : dependencyResolver.resolveDependencies(request, coordinate, scopes)) {
        allDependencyPaths.add(next.getArtifact().getFile().toPath());
      }
    } catch (DependencyResolverException ex) {
      throw new ResolutionException("Failed to resolve dependencies of '" + artifact + "'", ex);
    }

    return allDependencyPaths;
  }

  public Path resolveArtifact(
      MavenSession session,
      MavenArtifact artifact
  ) throws ResolutionException {
    var request = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());

    try {
      var artifactCoordinate = artifact.toArtifactCoordinate();
      return artifactResolver.resolveArtifact(request, artifactCoordinate)
          .getArtifact()
          .getFile()
          .toPath();
    } catch (ArtifactResolverException ex) {
      throw new ResolutionException("Failed to resolve artifact '" + artifact + "'", ex);
    }
  }
}
