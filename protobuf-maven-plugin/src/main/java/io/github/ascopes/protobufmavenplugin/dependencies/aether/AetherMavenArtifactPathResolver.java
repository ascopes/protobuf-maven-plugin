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
import io.github.ascopes.protobufmavenplugin.dependencies.MavenArtifactPathResolver;
import io.github.ascopes.protobufmavenplugin.dependencies.ResolutionException;
import io.github.ascopes.protobufmavenplugin.utils.FileUtils;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;
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
import org.eclipse.aether.artifact.ArtifactType;
import org.eclipse.aether.artifact.ArtifactTypeRegistry;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Exclusion;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
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
final class AetherMavenArtifactPathResolver implements MavenArtifactPathResolver {

  private static final String DEFAULT_EXTENSION = "jar";
  private static final String DEFAULT_SCOPE = "compile";

  private static final Logger log = LoggerFactory.getLogger(AetherMavenArtifactPathResolver.class);

  private final MavenSession mavenSession;
  private final ArtifactHandler artifactHandler;
  private final ArtifactTypeRegistry artifactTypeRegistry;
  private final RepositorySystem repositorySystem;
  private final RepositorySystemSession repositorySession;
  private final List<RemoteRepository> remoteRepositories;

  @Inject
  AetherMavenArtifactPathResolver(
      MavenSession mavenSession,
      RepositorySystem repositorySystem,
      ArtifactHandler artifactHandler
  ) {
    repositorySession = new ProtobufMavenPluginRepositorySession(
        mavenSession.getRepositorySession()
    );

    this.mavenSession = mavenSession;
    this.repositorySystem = repositorySystem;
    artifactTypeRegistry = repositorySession.getArtifactTypeRegistry();
    this.artifactHandler = artifactHandler;

    // Convert the Maven ArtifactRepositories into Aether RemoteRepositories
    var artifactRepositories = mavenSession.getProjectBuildingRequest().getRemoteRepositories();
    remoteRepositories = RepositoryUtils.toRepos(artifactRepositories);

    log.debug("Injected artifact handler {}", artifactHandler);
    log.debug("Detected remote repositories as {}", remoteRepositories);
  }

  @Override
  public Path resolveArtifact(MavenArtifact artifact) throws ResolutionException {
    var repositorySession = mavenSession.getRepositorySession();
    var aetherArtifact = new DefaultArtifact(
        artifact.getGroupId(),
        artifact.getArtifactId(),
        classifierOrDefault(artifact.getClassifier()),
        extensionOrDefault(artifact.getType()),
        artifact.getVersion()
    );

    log.info("Resolving artifact {} from repositories", aetherArtifact);

    var artifactRequest = new ArtifactRequest(aetherArtifact, remoteRepositories, null);

    try {
      var resolvedArtifact = repositorySystem
          .resolveArtifact(repositorySession, artifactRequest)
          .getArtifact();
      return determinePath(resolvedArtifact);

    } catch (ArtifactResolutionException ex) {
      throw new ResolutionException(
          "Failed to resolve artifact " + aetherArtifact + " from repositories",
          ex
      );
    }
  }

  @Override
  public List<Path> resolveDependencies(
      Collection<? extends MavenArtifact> artifacts,
      DependencyResolutionDepth defaultDependencyResolutionDepth,
      Set<String> dependencyScopes,
      boolean includeProjectDependencies,
      boolean failOnInvalidDependencies
  ) throws ResolutionException {
    DependencyResult dependencyResult;

    try {
      var dependenciesToResolve = Stream
          .concat(
              includeProjectDependencies ? getProjectDependencies() : Stream.of(),
              artifacts.stream().map(createDependency(defaultDependencyResolutionDepth))
          )
          .collect(Collectors.toUnmodifiableList());

      log.debug(
          "Attempting to resolve the following dependencies in this pass: {}",
          dependenciesToResolve
      );

      var scopeFilter = new ScopeDependencyFilter(dependencyScopes);
      var collectRequest = new CollectRequest(dependenciesToResolve, null, remoteRepositories);
      var dependencyRequest = new DependencyRequest(collectRequest, scopeFilter);
      dependencyResult = repositorySystem.resolveDependencies(repositorySession, dependencyRequest);

    } catch (DependencyResolutionException ex) {
      // GH-299: if this exception is raised, we may still have some results we can use. If this is
      // the case then resolution only partially failed, so continue for now unless strict
      // resolution is enabled. We do not fail-fast here anymore (since 2.4.0) as any resolution
      // errors should be dealt with by the maven-compiler-plugin later on if needed.
      //
      // If we didn't get any result, then something more fatal has occurred, so raise.
      dependencyResult = ex.getResult();

      if (dependencyResult == null || failOnInvalidDependencies) {
        throw new ResolutionException("Failed to resolve dependencies from repositories", ex);
      }

      // Log the message as well here as we omit it by default if `--errors' is not passed to Maven.
      log.warn(
          "Error resolving one or more dependencies, dependencies may be missing during "
              + "protobuf compilation! {}", ex.getMessage(), ex
      );
    }

    return dependencyResult
        .getArtifactResults()
        .stream()
        .map(ArtifactResult::getArtifact)
        .map(this::determinePath)
        .collect(Collectors.toUnmodifiableList());
  }

  private Stream<org.eclipse.aether.graph.Dependency> getProjectDependencies() {
    return mavenSession.getCurrentProject().getDependencies()
        .stream()
        .map(this::createDependency);
  }

  private Artifact createArtifact(MavenArtifact artifact) {
    return new DefaultArtifact(
        artifact.getGroupId(),
        artifact.getArtifactId(),
        classifierOrDefault(artifact.getClassifier()),
        extensionOrDefault(artifact.getType()),  // MavenArtifact types <-> Aether extensions
        artifact.getVersion()
    );
  }

  private Function<MavenArtifact, org.eclipse.aether.graph.Dependency> createDependency(
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

      return new org.eclipse.aether.graph.Dependency(
          artifact, 
          DEFAULT_SCOPE,
          false,
          exclusions
      );
    };
  }

  private org.eclipse.aether.graph.Dependency createDependency(
      org.apache.maven.model.Dependency mavenDependency
  ) {
    var artifact = new DefaultArtifact(
        mavenDependency.getGroupId(),
        mavenDependency.getArtifactId(),
        classifierOrDefault(mavenDependency.getClassifier()),
        null,  // Inferred elsewhere.
        mavenDependency.getVersion(),
        extensionToType(mavenDependency.getType())  // MavenArtifact types <-> Aether extensions
    );

    var exclusions = mavenDependency.getExclusions()
        .stream()
        .map(mavenExclusion -> new Exclusion(
            mavenExclusion.getGroupId(),
            mavenExclusion.getArtifactId(),
            null,  // Any
            null   // Any
        ))
        .collect(Collectors.toUnmodifiableList());

    return new org.eclipse.aether.graph.Dependency(
        artifact,
        mavenDependency.getScope(),
        mavenDependency.isOptional(),
        exclusions
    );
  }

  private @Nullable String classifierOrDefault(@Nullable String classifier) {
    // .getClassifier can return null in this case to imply a default classifier to Aether.
    if (classifier == null) {
      classifier = artifactHandler.getClassifier();
    }
    return classifier;
  }

  private @Nullable ArtifactType extensionToType(@Nullable String extension) {
    extension = extensionOrDefault(extension);
    var type = artifactTypeRegistry.get(extension);

    if (type == null) {
      log.debug("Could not resolve extension {} to any known Aether artifact type", extension);
    } else {
      log.debug(
          "Resolved extension {} to Aether artifact type (classifier: {}, type: {}, id: {}, {})",
          extension,
          type.getClassifier(),
          type.getExtension(),
          type.getId(),
          type.getProperties()
      );
    }
    return type;
  }

  private String extensionOrDefault(@Nullable String extension) {
    // We have to provide a sensible default here if it isn't provided by the artifact
    // handler, otherwise we fall over in a heap because Maven implicitly infers this information
    // whereas Aether does not. For some reason, this is mandatory whereas classifiers can be
    // totally inferred if null.
    if (extension == null) {
      extension = requireNonNullElse(artifactHandler.getExtension(), DEFAULT_EXTENSION);
    }
    return extension;
  }

  private Path determinePath(Artifact artifact) {
    // TODO: when Maven moves to the v2.0.0 resolver API, replace
    //   this method with calls to Artifact.getPath() directly
    //   and delete this polyfill method.
    @SuppressWarnings("deprecation")
    var path = artifact.getFile().toPath();
    return FileUtils.normalize(path);
  }
}
