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
package io.github.ascopes.protobufmavenplugin.dependencies.aether;

import io.github.ascopes.protobufmavenplugin.dependencies.DependencyResolutionDepth;
import io.github.ascopes.protobufmavenplugin.dependencies.MavenArtifact;
import io.github.ascopes.protobufmavenplugin.dependencies.MavenArtifactPathResolver;
import io.github.ascopes.protobufmavenplugin.utils.ResolutionException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.execution.MavenSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.graph.Dependency;

/**
 * Implementation of {@link MavenArtifactPathResolver} that integrates with the Eclipse Aether
 * artifact resolution backend provided by Eclipse for Apache Maven.
 *
 * @author Ashley Scopes
 * @since 2.4.4, many iterations of this existed in the past with different names.
 */
@Named
final class AetherMavenArtifactPathResolver implements MavenArtifactPathResolver {
  private final MavenSession mavenSession;
  private final AetherArtifactMapper aetherMapper;
  private final AetherResolver aetherResolver;

  @Inject
  AetherMavenArtifactPathResolver(
      MavenSession mavenSession,
      RepositorySystem repositorySystem,
      ArtifactHandler artifactHandler
  ) {
    var artifactRepositories = mavenSession.getProjectBuildingRequest().getRemoteRepositories();
    var remoteRepositories = RepositoryUtils.toRepos(artifactRepositories);

    this.mavenSession = mavenSession;

    var repositorySystemSession = new ProtobufMavenPluginRepositorySession(
        mavenSession.getRepositorySession());

    aetherMapper = new AetherArtifactMapper(
        artifactHandler, repositorySystemSession.getArtifactTypeRegistry());

    aetherResolver = new AetherResolver(
        repositorySystem, repositorySystemSession, remoteRepositories);
  }

  // Visible for testing only.
  MavenSession getMavenSession() {
    return mavenSession;
  }

  // Visible for testing only.
  AetherArtifactMapper getAetherMapper() {
    return aetherMapper;
  }

  // Visible for testing only.
  AetherResolver getAetherResolver() {
    return aetherResolver;
  }

  @Override
  public Path resolveArtifact(MavenArtifact mavenArtifact) throws ResolutionException {
    var unresolvedArtifact = aetherMapper.mapPmpArtifactToEclipseArtifact(mavenArtifact);
    var resolvedArtifact = aetherResolver.resolveArtifact(unresolvedArtifact);
    return aetherMapper.mapEclipseArtifactToPath(resolvedArtifact);
  }

  @Override
  public List<Path> resolveDependencies(
      Collection<? extends MavenArtifact> artifacts,
      DependencyResolutionDepth defaultDepth,
      Set<String> dependencyScopes,
      boolean includeProjectDependencies,
      boolean failOnInvalidDependencies
  ) throws ResolutionException {
    var unresolvedDependencies = new ArrayList<Dependency>();

    artifacts.stream()
        .map(artifact -> aetherMapper.mapPmpArtifactToEclipseDependency(artifact, defaultDepth))
        .forEach(unresolvedDependencies::add);

    if (includeProjectDependencies) {
      mavenSession.getCurrentProject().getDependencies()
          .stream()
          .map(aetherMapper::mapMavenDependencyToEclipseDependency)
          .forEach(unresolvedDependencies::add);
    }

    var resolvedArtifacts = aetherResolver.resolveDependenciesToArtifacts(
        unresolvedDependencies, dependencyScopes, failOnInvalidDependencies);

    return resolvedArtifacts.stream()
        .map(aetherMapper::mapEclipseArtifactToPath)
        .distinct()
        .collect(Collectors.toUnmodifiableList());
  }
}
