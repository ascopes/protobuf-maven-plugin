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

package io.github.ascopes.protobufmavenplugin.dependencies;

import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
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
  private final MavenSession mavenSession;
  private final RepositorySystem repositorySystem;
  private final ArtifactHandler artifactHandler;

  @Inject
  public MavenDependencyPathResolver(
      MavenSession mavenSession,
      RepositorySystem repositorySystem,
      ArtifactHandler artifactHandler
  ) {
    this.mavenSession = mavenSession;
    this.repositorySystem = repositorySystem;
    this.artifactHandler = artifactHandler;
  }

  public Collection<Path> resolveOne(
      MavenArtifact mavenArtifact,
      DependencyResolutionDepth dependencyResolutionDepth
  ) throws ResolutionException {
    return resolveAll(List.of(mavenArtifact), dependencyResolutionDepth);
  }

  public Collection<Path> resolveAll(
      Collection<? extends MavenArtifact> mavenArtifacts,
      DependencyResolutionDepth dependencyResolutionDepth
  ) throws ResolutionException {

    if (mavenArtifacts.isEmpty()) {
      log.debug("No artifacts provided, not resolving anything in this round...");
      return List.of();
    }

    var artifacts = dependencyResolutionDepth == DependencyResolutionDepth.DIRECT
        ? resolveDirect(mavenArtifacts)
        : resolveTransitive(mavenArtifacts);

    return artifacts
        .stream()
        .map(Artifact::getFile)
        .map(File::toPath)
        .collect(Collectors.toList());
  }

  private Collection<Artifact> resolveDirect(
      Collection<? extends MavenArtifact> mavenArtifacts
  ) throws ResolutionException {
    return resolveDirectWithBackReferences(mavenArtifacts).keySet();
  }

  private Map<Artifact, MavenArtifact> resolveDirectWithBackReferences(
      Collection<? extends MavenArtifact> mavenArtifacts
  ) throws ResolutionException {
    log.debug("Resolving direct artifacts {}", mavenArtifacts);

    var artifactRequests = new LinkedHashMap<ArtifactRequest, MavenArtifact>();

    for (var mavenArtifact : mavenArtifacts) {
      artifactRequests.put(getArtifactRequest(mavenArtifact), mavenArtifact);
    }

    try {
      return repositorySystem
          .resolveArtifacts(mavenSession.getRepositorySession(), artifactRequests.keySet())
          .stream()
          .collect(Collectors.toMap(
              ArtifactResult::getArtifact,
              result -> artifactRequests.get(result.getRequest())
          ));

    } catch (ArtifactResolutionException ex) {
      throw new ResolutionException("Failed to resolve one or more artifacts", ex);
    }
  }

  private ArtifactRequest getArtifactRequest(MavenArtifact mavenArtifact) {
    var artifact = new DefaultArtifact(
        mavenArtifact.getGroupId(),
        mavenArtifact.getArtifactId(),
        specifiedOrElse(mavenArtifact.getClassifier(), artifactHandler::getClassifier),
        specifiedOrElse(mavenArtifact.getType(), () -> "jar"),
        mavenArtifact.getVersion()
    );
    return new ArtifactRequest(artifact, remoteRepositories(), null);
  }

  private Collection<Artifact> resolveTransitive(
      Collection<? extends MavenArtifact> mavenArtifacts
  ) throws ResolutionException {
    var resolvedArtifacts = resolveDirectWithBackReferences(mavenArtifacts);

    var dependenciesToResolve = resolvedArtifacts.keySet()
        .stream()
        .filter(artifact -> resolvedArtifacts.get(artifact)
            .getDependencyResolutionDepth() != DependencyResolutionDepth.DIRECT)
        .map(this::createDependencyFromArtifact)
        .collect(Collectors.toList());

    log.debug("Resolving transitive dependencies for {}", dependenciesToResolve);

    var collectRequest = new CollectRequest(
        dependenciesToResolve,
        createDependencyManagementFor(dependenciesToResolve, resolvedArtifacts.keySet()),
        remoteRepositories()
    );

    var dependencyRequest = new DependencyRequest(collectRequest, null);

    try {
      // XXX: do I need to check the CollectResult exception list here as well? It isn't overly
      // clear to whether I care about this or whether it gets propagated in the
      // DependencyResolutionException anyway...
      var resolvedDependencies = repositorySystem
          .resolveDependencies(mavenSession.getRepositorySession(), dependencyRequest)
          .getArtifactResults()
          .stream()
          .map(ArtifactResult::getArtifact);

      // Concatenate with the initial artifacts and return distinct values only. That way we
      // still include dependencies that were defined with a custom dependencyResolutionScope
      // that overrides the global setting.
      return Stream.concat(resolvedArtifacts.keySet().stream(), resolvedDependencies)
          .distinct()
          .collect(Collectors.toList());

    } catch (DependencyResolutionException ex) {
      throw new ResolutionException("Failed to resolve one or more dependencies", ex);
    }
  }

  private Dependency createDependencyFromArtifact(Artifact artifact) {
    return new Dependency(artifact, null, false);
  }

  private List<Dependency> createDependencyManagementFor(
      List<Dependency> dependencies,
      Collection<Artifact> artifacts
  ) {
    // We include the artifacts as well to ensure they are included on the dependencyManagement
    // mapping, as this may impact the resolution of some edge cases where we both depend on an
    // artifact directly with DIRECT resolution scope and that artifact via a transitive dependency
    // elsewhere.
    return Stream
        .concat(
            artifacts.stream().map(this::createDependencyFromArtifact),
            dependencies.stream()
        )
        .collect(Collectors.toList());
  }

  private List<RemoteRepository> remoteRepositories() {
    return RepositoryUtils
        .toRepos(mavenSession.getProjectBuildingRequest().getRemoteRepositories());
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
