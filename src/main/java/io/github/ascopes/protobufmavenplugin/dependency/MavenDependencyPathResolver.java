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

import io.github.ascopes.protobufmavenplugin.DependencyResolutionDepth;
import io.github.ascopes.protobufmavenplugin.MavenArtifact;
import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.execution.MavenSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.jspecify.annotations.Nullable;
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
  private final RepositorySystem repositorySystem;
  private final ArtifactHandler artifactHandler;

  @Inject
  public MavenDependencyPathResolver(
      RepositorySystem repositorySystem,
      ArtifactHandler artifactHandler
  ) {
    this.repositorySystem = repositorySystem;
    this.artifactHandler = artifactHandler;
  }

  public Collection<Path> resolveOne(
      MavenSession session,
      MavenArtifact mavenArtifact,
      DependencyResolutionDepth dependencyResolutionDepth
  ) throws ResolutionException {
    return resolveAll(session, List.of(mavenArtifact), dependencyResolutionDepth);
  }

  public Collection<Path> resolveAll(
      MavenSession session,
      Collection<MavenArtifact> mavenArtifacts,
      DependencyResolutionDepth dependencyResolutionDepth
  ) throws ResolutionException {
    var artifacts = dependencyResolutionDepth == DependencyResolutionDepth.DIRECT
        ? resolveDirect(session, mavenArtifacts)
        : resolveTransitive(session, mavenArtifacts);

    return artifacts
        .stream()
        .map(Artifact::getFile)
        .map(File::toPath)
        .collect(Collectors.toList());
  }

  private List<Artifact> resolveDirect(
      MavenSession session,
      Collection<MavenArtifact> mavenArtifacts
  ) throws ResolutionException {
    log.debug("Resolving direct artifacts {}", mavenArtifacts);

    var artifactRequests = new ArrayList<ArtifactRequest>();

    for (var mavenArtifact : mavenArtifacts) {
      artifactRequests.add(getArtifactRequest(session, mavenArtifact));
    }

    try {
      return repositorySystem
          .resolveArtifacts(session.getRepositorySession(), artifactRequests)
          .stream()
          .map(ArtifactResult::getArtifact)
          .collect(Collectors.toList());

    } catch (ArtifactResolutionException ex) {
      throw new ResolutionException("Failed to resolve one or more artifacts", ex);
    }
  }

  private ArtifactRequest getArtifactRequest(MavenSession session, MavenArtifact mavenArtifact) {
    var artifact = new DefaultArtifact(
        mavenArtifact.getGroupId(),
        mavenArtifact.getArtifactId(),
        specifiedOrElse(mavenArtifact.getClassifier(), artifactHandler::getClassifier),
        specifiedOrElse(mavenArtifact.getType(), () -> "jar"),
        mavenArtifact.getVersion()
    );
    return new ArtifactRequest(artifact, remoteRepositories(session), null);
  }

  private List<Artifact> resolveTransitive(
      MavenSession session,
      Collection<MavenArtifact> mavenArtifacts
  ) throws ResolutionException {
    var resolvedArtifacts = resolveDirect(session, mavenArtifacts);
    var dependenciesToResolve = resolvedArtifacts.stream()
        .map(artifact -> new Dependency(artifact, null, false))
        .collect(Collectors.toList());

    log.debug("Resolving transitive dependencies for {}", dependenciesToResolve);

    var collectRequest = new CollectRequest(
        dependenciesToResolve,
        null,
        remoteRepositories(session)
    );

    var dependencyRequest = new DependencyRequest(collectRequest, null);

    try {
      // XXX: do I need to check the CollectResult exception list here as well? It isn't overly
      // clear to whether I care about this or whether it gets propagated in the
      // DependencyResolutionException anyway...
      return repositorySystem.resolveDependencies(session.getRepositorySession(), dependencyRequest)
          .getArtifactResults()
          .stream()
          .map(ArtifactResult::getArtifact)
          .collect(Collectors.toList());

    } catch (DependencyResolutionException ex) {
      throw new ResolutionException("Failed to resolve one or more dependencies", ex);
    }
  }

  private List<RemoteRepository> remoteRepositories(MavenSession session) {
    return RepositoryUtils.toRepos(session.getProjectBuildingRequest().getRemoteRepositories());
  }

  private @Nullable String specifiedOrElse(
      @Nullable String value,
      Supplier<@Nullable String> elseGet
  ) {
    return value == null || value.isBlank()
        ? elseGet.get()
        : value;
  }
}
