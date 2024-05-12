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

package io.github.ascopes.protobufmavenplugin.dependencies.aether;

import static java.util.Objects.requireNonNullElse;

import io.github.ascopes.protobufmavenplugin.dependencies.DependencyResolutionDepth;
import io.github.ascopes.protobufmavenplugin.dependencies.MavenArtifact;
import io.github.ascopes.protobufmavenplugin.dependencies.ResolutionException;
import java.io.File;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.RepositoryUtils;
import org.apache.maven.artifact.handler.ArtifactHandler;
import org.apache.maven.execution.MavenSession;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.Exclusion;
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
 * Component that can resolve artifacts and dependencies from the Eclipse Aether dependency
 * resolution backend.
 *
 * @author Ashley Scopes
 * @since 2.0.3
 */
@Named
public class AetherMavenArtifactPathResolver {

  private static final Logger log = LoggerFactory.getLogger(AetherMavenArtifactPathResolver.class);

  private final MavenSession mavenSession;
  private final ArtifactHandler artifactHandler;
  private final RepositorySystem repositorySystem;
  private final RepositorySystemSession repositorySession;
  private final List<RemoteRepository> remoteRepositories;

  @Inject
  public AetherMavenArtifactPathResolver(
      MavenSession mavenSession,
      RepositorySystem repositorySystem,
      ArtifactHandler artifactHandler
  ) {
    this.mavenSession = mavenSession;
    this.repositorySystem = repositorySystem;
    this.artifactHandler = artifactHandler;

    var defaultRepositorySession = mavenSession.getRepositorySession();
    repositorySession = new ProtobufMavenPluginRepositorySession(defaultRepositorySession);
    remoteRepositories = RepositoryUtils
        .toRepos(mavenSession.getProjectBuildingRequest().getRemoteRepositories());

    log.debug("Injected artifact handler {}", artifactHandler);
    log.debug("Detected remote repositories as {}", remoteRepositories);
  }

  /**
   * Resolve a single Maven artifact directly, and do not resolve any transitive dependencies.
   *
   * @param artifact the artifact to resolve.
   * @return the path to the resolved artifact.
   * @throws ResolutionException if resolution fails in the backend.
   */
  public Path resolveArtifact(MavenArtifact artifact) throws ResolutionException {
    var repositorySession = mavenSession.getRepositorySession();
    var aetherArtifact = new DefaultArtifact(
        artifact.getGroupId(),
        artifact.getArtifactId(),
        Optional.ofNullable(artifact.getClassifier()).orElseGet(artifactHandler::getClassifier),
        Optional.ofNullable(artifact.getType()).orElseGet(artifactHandler::getExtension),
        artifact.getVersion()
    );

    log.info("Resolving artifact {}", aetherArtifact);

    var artifactRequest = new ArtifactRequest(aetherArtifact, remoteRepositories, null);

    try {
      return repositorySystem.resolveArtifact(repositorySession, artifactRequest)
          .getArtifact()
          .getFile()
          .toPath();

    } catch (ArtifactResolutionException ex) {
      throw new ResolutionException("Failed to resolve " + aetherArtifact, ex);
    }
  }

  /**
   * Resolve all given dependencies based on their resolution depth semantics.
   *
   * @param artifacts                        the artifacts to resolve.
   * @param defaultDependencyResolutionDepth the project default dependency resolution depth.
   * @param includeProjectDependencies       whether to also resolve project dependencies and return
   *                                         them in the result.
   * @return the paths to each resolved artifact.
   * @throws ResolutionException if resolution failed in the backend.
   */
  public Collection<Path> resolveDependencies(
      Collection<? extends MavenArtifact> artifacts,
      DependencyResolutionDepth defaultDependencyResolutionDepth,
      boolean includeProjectDependencies
  ) throws ResolutionException {
    try {
      var dependenciesToResolve = Stream
          .concat(
              includeProjectDependencies ? getProjectDependencies() : Stream.of(),
              artifacts.stream().map(createDependency(defaultDependencyResolutionDepth))
          )
          .collect(Collectors.toList());

      log.debug(
          "Attempting to resolve the following dependencies in this pass: {}",
          dependenciesToResolve
      );

      var collectRequest = new CollectRequest(dependenciesToResolve, null, remoteRepositories);
      var dependencyRequest = new DependencyRequest(collectRequest, null);

      return repositorySystem.resolveDependencies(repositorySession, dependencyRequest)
          .getArtifactResults()
          .stream()
          .map(ArtifactResult::getArtifact)
          .map(Artifact::getFile)
          .map(File::toPath)
          .collect(Collectors.toList());

    } catch (DependencyResolutionException ex) {
      throw new ResolutionException("Failed to resolve dependencies", ex);
    }
  }

  private Stream<Dependency> getProjectDependencies() {
    return mavenSession.getCurrentProject().getDependencies()
        .stream()
        .map(this::createDependency);
  }

  private Artifact createArtifact(MavenArtifact artifact) {
    return new DefaultArtifact(
        artifact.getGroupId(),
        artifact.getArtifactId(),
        effectiveClassifier(artifact.getClassifier()),
        effectiveExtension(artifact.getType()),
        artifact.getVersion()
    );
  }

  private Function<MavenArtifact, Dependency> createDependency(
      DependencyResolutionDepth defaultDependencyResolutionDepth
  ) {
    return mavenArtifact -> {
      var effectiveDependencyResolutionDepth = requireNonNullElse(
          mavenArtifact.getDependencyResolutionDepth(),
          defaultDependencyResolutionDepth
      );

      var exclusions = effectiveDependencyResolutionDepth == DependencyResolutionDepth.DIRECT
          ? List.of(WildcardAwareDependencyTraverser.WILDCARD_EXCLUSION)
          : List.<Exclusion>of();

      var artifact = createArtifact(mavenArtifact);

      return new Dependency(artifact, "compile", false, exclusions);
    };
  }

  private Dependency createDependency(
      org.apache.maven.model.Dependency mavenDependency
  ) {
    var artifact = new DefaultArtifact(
        mavenDependency.getGroupId(),
        mavenDependency.getArtifactId(),
        effectiveClassifier(mavenDependency.getClassifier()),
        effectiveExtension(mavenDependency.getType()),
        mavenDependency.getVersion()
    );

    var exclusions = mavenDependency.getExclusions()
        .stream()
        .map(mavenExclusion -> new Exclusion(
            mavenExclusion.getGroupId(),
            mavenExclusion.getArtifactId(),
            null,
            null
        ))
        .collect(Collectors.toList());

    return new Dependency(
        artifact,
        mavenDependency.getScope(),
        mavenDependency.isOptional(),
        exclusions
    );
  }

  @Nullable
  private String effectiveClassifier(@Nullable String classifier) {
    return Optional.ofNullable(classifier).orElseGet(artifactHandler::getClassifier);
  }

  private String effectiveExtension(@Nullable String extension) {
    // We have to provide a sensible default here if it isn't provided by the artifact
    // handler, otherwise we fall over in a heap. Not entirely sure why this doesn't matter
    // for classifiers...
    return Optional.ofNullable(extension)
        .or(() -> Optional.ofNullable(artifactHandler.getExtension()))
        .orElse("jar");
  }
}
