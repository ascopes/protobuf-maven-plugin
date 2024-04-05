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

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.DefaultProjectBuildingRequest;
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
      DependencyResolutionDepth dependencyResolutionDepth
  ) throws ResolutionException {
    if (dependencyResolutionDepth == DependencyResolutionDepth.DIRECT) {
      var artifacts = session.getCurrentProject().getDependencies()
          .stream()
          .map(MavenArtifact::fromDependency)
          .collect(Collectors.toList());

      var paths = new ArrayList<Path>();
      for (var artifact : artifacts) {
        paths.add(resolveArtifact(session, artifact));
      }
      return paths;
    }

    return session.getCurrentProject().getArtifacts()
        .stream()
        .map(Artifact::getFile)
        .map(File::toPath)
        .distinct()
        .collect(Collectors.toList());
  }

  public Collection<Path> resolveDependencyTreePaths(
      MavenSession session,
      DependencyResolutionDepth dependencyResolutionDepth,
      MavenArtifact artifact
  ) throws ResolutionException {
    log.debug("Resolving dependency '{}'", artifact);

    var allDependencyPaths = new ArrayList<Path>();
    var artifactPath = resolveArtifact(session, artifact);
    allDependencyPaths.add(artifactPath);

    if (dependencyResolutionDepth == DependencyResolutionDepth.DIRECT) {
      log.debug("Not resolving transitive dependencies of '{}'", artifact);
      return allDependencyPaths;
    }

    var request = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
    var coordinate = artifact.toDependableCoordinate();

    try {
      for (var next : dependencyResolver.resolveDependencies(request, coordinate, null)) {
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
