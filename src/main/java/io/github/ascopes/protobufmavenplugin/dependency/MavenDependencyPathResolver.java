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

import io.github.ascopes.protobufmavenplugin.system.FileUtils;
import java.nio.file.Path;
import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.project.DefaultProjectBuildingRequest;
import org.apache.maven.shared.artifact.filter.resolve.ScopeFilter;
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

  private final DependencyResolver dependencyResolver;

  @Inject
  public MavenDependencyPathResolver(DependencyResolver dependencyResolver) {
    this.dependencyResolver = dependencyResolver;
  }

  public Collection<Path> resolveDependencyPaths(
      MavenSession session,
      DependableCoordinate coordinate
  ) throws DependencyResolverException {
    log.info("Resolving dependency '{}'", coordinate);

    var request = new DefaultProjectBuildingRequest(session.getProjectBuildingRequest());
    var scopes = ScopeFilter.including("compile", "provided", "system");
    var result = dependencyResolver.resolveDependencies(request, coordinate, scopes);

    return StreamSupport.stream(result.spliterator(), false)
        .map(artifactResult -> artifactResult.getArtifact().getFile().toPath())
        .map(FileUtils::normalize)
        .collect(Collectors.toUnmodifiableList());
  }
}
