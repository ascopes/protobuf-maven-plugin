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
package io.github.ascopes.protobufmavenplugin.dependency;

import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.shared.artifact.filter.resolve.ScopeFilter;
import org.apache.maven.shared.transfer.artifact.ArtifactCoordinate;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolver;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolverException;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResult;
import org.apache.maven.shared.transfer.dependencies.DependableCoordinate;
import org.apache.maven.shared.transfer.dependencies.resolve.DependencyResolver;
import org.apache.maven.shared.transfer.dependencies.resolve.DependencyResolverException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolves paths to Maven dependency objects on the current file system.
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

  public Path resolveArtifactPath(
      MavenSession session,
      ArtifactCoordinate artifactCoordinate
  ) throws ArtifactResolverException {
    var actualArtifactCoordinate = new Dependency(artifactCoordinate);
    log.info("Resolving artifact {} from Maven repositories", actualArtifactCoordinate);

    var request = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
    var result = artifactResolver.resolveArtifact(request, artifactCoordinate);
    return pathOf(result);
  }

  public Collection<Path> resolveDependencyPaths(
      MavenSession session,
      DependableCoordinate dependableCoordinate
  ) throws ArtifactResolverException, DependencyResolverException {
    var artifactCoordinate = new Dependency(dependableCoordinate);
    var artifactPath = resolveArtifactPath(session, artifactCoordinate);

    log.info("Resolving dependencies of {} from Maven repositories", dependableCoordinate);

    var request = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
    var scopeFilter = new ScopeFilter(List.of("compile", "provided", "system"), null);

    var result = dependencyResolver.resolveDependencies(request, artifactCoordinate, scopeFilter);
    var dependencyPaths = StreamSupport.stream(result.spliterator(), false)
        .map(this::pathOf);

    return Stream.concat(Stream.of(artifactPath), dependencyPaths)
        .collect(Collectors.toUnmodifiableList());
  }

  private Path pathOf(ArtifactResult artifactResult) {
    return artifactResult.getArtifact().getFile().toPath();
  }
}
