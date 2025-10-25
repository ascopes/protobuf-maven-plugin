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

import static java.util.Objects.requireNonNull;

import io.github.ascopes.protobufmavenplugin.utils.ResolutionException;
import io.github.ascopes.protobufmavenplugin.utils.StringUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.scope.MojoExecutionScoped;
import org.eclipse.aether.RepositorySystem;
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
import org.eclipse.aether.util.filter.ScopeDependencyFilter;
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
 * you are doing!
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
  private final ProtobufMavenPluginRepositorySession repositorySystemSession;
  private final List<RemoteRepository> remoteRepositories;

  @Inject
  AetherResolver(
      RepositorySystem repositorySystem,
      MavenSession mavenSession,
      ProtobufMavenPluginRepositorySession repositorySystemSession
  ) {
    this.repositorySystem = repositorySystem;
    this.repositorySystemSession = repositorySystemSession;

    // Prior to v2.12.0, we used the ProjectBuildingRequest on the MavenSession
    // and used RepositoryUtils.toRepos to create the repository list. GH-579
    // was raised to report that the <repositories> block in the POM was being
    // ignored. This appears to be due to the project building request only
    // looking at what is in ~/.m2/settings.xml. The current project remote
    // repositories seems to be what we need to use instead here.
    remoteRepositories = mavenSession.getCurrentProject().getRemoteProjectRepositories();
  }

  Artifact resolveRequiredArtifact(Artifact artifact) throws ResolutionException {
    log.info("Attempting to resolve artifact \"{}\"", artifact);

    var request = new ArtifactRequest();
    request.setArtifact(artifact);
    request.setRepositories(remoteRepositories);

    ArtifactResult result;

    try {
      result = repositorySystem.resolveArtifact(repositorySystemSession, request);
    } catch (ArtifactResolutionException ex) {
      log.debug("Discarding internal exception", ex);
      result = ex.getResult();
    }

    // Looks like we can get resolution exceptions even if things resolve correctly, as it appears
    // to raise for the local repository first.
    // If this happens, don't bother raising or reporting it as it is normal behaviour. If anything
    // else goes wrong, then we can panic about it.
    if (result.isMissing()) {
      throw mapExceptions(
          "Failed to resolve artifact \"" + artifact + "\"",
          result.getExceptions()
      );
    }

    reportWarnings(result.getExceptions());

    // This shouldn't happen, but I do not trust that Aether isn't hiding some wild edge cases
    // for me here.
    return requireNonNull(
        result.getArtifact(),
        () -> "No result was returned by Aether while resolving artifact \"" + artifact + "\""
    );
  }

  Collection<Artifact> resolveDependencies(
      List<Dependency> dependencies,
      Set<String> allowedDependencyScopes
  ) throws ResolutionException {
    var collectRequest = new CollectRequest();
    collectRequest.setDependencies(dependencies);
    collectRequest.setRepositories(remoteRepositories);

    var dependencyRequest = new DependencyRequest();
    dependencyRequest.setCollectRequest(collectRequest);
    var filter = new ScopeDependencyFilter(
        /* included: */ allowedDependencyScopes,
        /* excluded */ List.of()
    );
    dependencyRequest.setFilter(filter);

    log.debug(
        "Resolving {} - {}",
        StringUtils.pluralize(dependencies.size(), "dependency", "dependencies"),
        dependencies
    );

    DependencyResult dependencyResult;

    try {
      dependencyResult = repositorySystem
          .resolveDependencies(repositorySystemSession, dependencyRequest);
    } catch (DependencyResolutionException ex) {
      log.debug("Discarding internal exception", ex);
      dependencyResult = ex.getResult();
    }

    var artifacts = new ArrayList<Artifact>();
    var exceptions = new ArrayList<>(dependencyResult.getCollectExceptions());

    for (var artifactResult : dependencyResult.getArtifactResults()) {
      var artifact = artifactResult.getArtifact();
      if (artifactResult.isResolved() && artifact != null) {
        log.debug("Resolution of dependencies returned artifact \"{}\"", artifact);
        artifacts.add(artifact);
      }

      if (artifactResult.isMissing()) {
        log.debug("Artifact \"{}\" is missing!", artifact);
      }

      exceptions.addAll(artifactResult.getExceptions());
    }

    reportWarnings(exceptions);

    return Collections.unmodifiableList(artifacts);
  }

  private ResolutionException mapExceptions(String message, Collection<Exception> causes) {
    var causeIterator = causes.iterator();

    // Assumption: this is always possible. If it isn't, we screwed up somewhere.
    var cause = causeIterator.next();
    var exception = new ResolutionException(
        message
            + " - resolution failed with "
            + StringUtils.pluralize(causes.size(), "error: ", "errors - first was: ")
            + cause.getMessage(),
        cause
    );
    causeIterator.forEachRemaining(exception::addSuppressed);
    return exception;
  }

  private void reportWarnings(Iterable<? extends Exception> exceptions) {
    exceptions.forEach(exception -> {
      // We purposely log the warning class and message twice, since exception tracebacks are
      // hidden unless Maven was invoked with --errors.
      //noinspection LoggingPlaceholderCountMatchesArgumentCount
      log.debug(
          "Dependency resolution warning was reported. "
              + "This might be okay or it might be bad - {}: {}",
          exception.getClass().getName(),
          exception.getMessage(),
          exception
      );
    });
  }
}

