/*
 * Copyright (C) 2023 Ashley Scopes
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

import static java.util.function.Predicate.not;

import io.github.ascopes.protobufmavenplugin.utils.ResolutionException;
import io.github.ascopes.protobufmavenplugin.utils.StringUtils;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.execution.scope.MojoExecutionScoped;
import org.apache.maven.project.MavenProject;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.ArtifactResult;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.eclipse.sisu.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Integration layer with the Eclipse Aether resolver.
 *
 * <p>Handles pulling dependencies from remote Maven repositories and ensuring that they can
 * be accessed on the local system.
 *
 * <p>Warning: the code in this class is very fragile and changing it can easily result in the
 * introduction of regressions for users. If you need to alter it, be very sure that you know what
 * you are doing and that <strong>all</strong> possible error cases are handled in a remotely
 * sensible way to avoid bug reports due to ambiguous handling!
 *
 * @author Ashley Scopes
 * @since 2.4.4
 */
@Description("Wraps Eclipse Aether to provide dependency and artifact resolution capabilities")
@MojoExecutionScoped
@Named
final class AetherResolver {

  private static final Logger log = LoggerFactory.getLogger(AetherResolver.class);

  private final RepositorySystem repositorySystem;
  private final RepositorySystemSession repositorySystemSession;
  private final MavenProject mavenProject;

  @Inject
  AetherResolver(
      RepositorySystem repositorySystem,
      RepositorySystemSession repositorySystemSession,
      MavenProject mavenProject
  ) {
    this.repositorySystem = repositorySystem;
    this.repositorySystemSession = repositorySystemSession;
    this.mavenProject = mavenProject;
  }

  Artifact resolveArtifact(Artifact artifact) throws ResolutionException {
    log.debug("Resolving artifact \"{}\"", artifact);

    var request = new ArtifactRequest()
        .setArtifact(artifact)
        .setRepositories(computeRemoteRepositories());

    ArtifactResult artifactResult;
    Exception cause = null;

    try {
      artifactResult = repositorySystem.resolveArtifact(repositorySystemSession, request);
    } catch (ArtifactResolutionException ex) {
      log.debug("Handling resolution exception", ex);
      artifactResult = ex.getResult();
      cause = ex;
    }

    if (cause != null || !artifactResult.isResolved()) {
      var ex = new ResolutionException(
          "Failed to resolve artifact "
              + artifact
              + ". Check the coordinates and connection, and try again. Cause was: "
              + cause,
          cause
      );

      artifactResult.getExceptions().forEach(ex::addSuppressed);
      throw ex;
    }

    return Objects.requireNonNull(artifactResult.getArtifact());
  }

  Collection<Artifact> resolveDependencies(
      List<Dependency> dependencies,
      Set<String> allowedDependencyScopes
  ) throws ResolutionException {
    if (dependencies.isEmpty()) {
      return List.of();
    }

    log.debug(
        "Resolving {} with {} {} - {}",
        StringUtils.pluralize(dependencies.size(), "dependency", "dependencies"),
        StringUtils.pluralize(allowedDependencyScopes.size(), "scope", "scopes"),
        allowedDependencyScopes,
        dependencies
    );

    var dependencyRequest = new DependencyRequest()
        .setCollectRequest(new CollectRequest()
            .setDependencies(dependencies)
            .setRepositories(computeRemoteRepositories()))
        .setFilter(new InclusiveScopeDependencyFilter(allowedDependencyScopes));

    DependencyResult dependencyResult;
    Exception cause = null;

    try {
      dependencyResult = repositorySystem
          .resolveDependencies(repositorySystemSession, dependencyRequest);
    } catch (DependencyResolutionException ex) {
      log.debug("Handling resolution exception", ex);
      dependencyResult = ex.getResult();
      cause = ex;
    }

    // Handle the multiple cases where we might have failed.
    if (cause != null || !dependencyResult.getCollectExceptions().isEmpty()) {
      // TODO(ascopes): should we limit the number of things output here?
      var failedGavs = dependencyResult.getArtifactResults().stream()
          .filter(not(ArtifactResult::isResolved))
          .map(result -> Optional.ofNullable(result.getArtifact())
              .orElseGet(() -> result.getRequest().getArtifact()))
          .map(Artifact::toString)
          .collect(Collectors.joining(", "));

      String errorMessage;

      if (failedGavs.isEmpty()) {
        errorMessage = "Failed to resolve dependencies. Cause was: " + cause;
      } else {
        errorMessage = "Failed to resolve dependencies: " + failedGavs
            + ". Check the direct and transitive coordinates, and network connection, "
            + "then try again. Cause was: "
            + cause;
      }

      var ex = new ResolutionException(errorMessage, cause);
      dependencyResult.getCollectExceptions().forEach(ex::addSuppressed);
      dependencyResult.getArtifactResults().stream()
          .map(ArtifactResult::getExceptions)
          .flatMap(List::stream)
          .forEach(ex::addSuppressed);

      throw ex;
    }

    return dependencyResult.getArtifactResults()
        .stream()
        .map(ArtifactResult::getArtifact)
        .map(Objects::requireNonNull)
        .toList();
  }

  // Some historical context of why we do this in a very specific way:
  //
  // Prior to v2.12.0, we used the ProjectBuildingRequest on the MavenSession
  // and used RepositoryUtils.toRepos to create the repository list. GH-579
  // was raised to report that the <repositories> block in the POM was being
  // ignored. This appears to be due to the project building request only
  // looking at what is in ~/.m2/settings.xml. The current project remote
  // repositories seems to be what we need to use instead here.
  //
  // As of 5.0.2, we use .newResolutionRepositories to do this as it ensures
  // certain networking and authentication configurations are propagated
  // correctly without relying on Maven exposing the final configuration to
  // us immediately.
  private List<RemoteRepository> computeRemoteRepositories() {
    return repositorySystem.newResolutionRepositories(
        repositorySystemSession,
        mavenProject.getRemoteProjectRepositories()
    );
  }
}
