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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import org.eclipse.aether.RepositorySystem;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.collection.CollectRequest;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.repository.RemoteRepository;
import org.eclipse.aether.resolution.ArtifactRequest;
import org.eclipse.aether.resolution.ArtifactResolutionException;
import org.eclipse.aether.resolution.DependencyRequest;
import org.eclipse.aether.resolution.DependencyResolutionException;
import org.eclipse.aether.resolution.DependencyResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Integration layer with the Eclipse Aether resolver.
 *
 * @author Ashley Scopes
 * @since 2.4.4
 */
final class AetherResolver {

  private static final Logger log = LoggerFactory.getLogger(AetherResolver.class);

  private final RepositorySystem repositorySystem;
  private final ProtobufMavenPluginRepositorySession repositorySystemSession;
  private final List<RemoteRepository> remoteRepositories;

  AetherResolver(
      RepositorySystem repositorySystem,
      ProtobufMavenPluginRepositorySession repositorySystemSession,
      List<RemoteRepository> remoteRepositories
  ) {
    this.repositorySystem = repositorySystem;
    this.repositorySystemSession = repositorySystemSession;
    this.remoteRepositories = remoteRepositories;
  }

  // Visible for testing only.
  RepositorySystem getRepositorySystem() {
    return repositorySystem;
  }

  // Visible for testing only.
  ProtobufMavenPluginRepositorySession getRepositorySystemSession() {
    return repositorySystemSession;
  }

  // Visible for testing only.
  List<RemoteRepository> getRemoteRepositories() {
    return Collections.unmodifiableList(remoteRepositories);
  }

  Artifact resolveArtifact(Artifact unresolvedArtifact) throws ResolutionException {
    log.info("Resolving artifact {} from repositories", unresolvedArtifact);
    var request = new ArtifactRequest(unresolvedArtifact, remoteRepositories, null);

    try {
      var response = repositorySystem.resolveArtifact(repositorySystemSession, request);
      if (response.isResolved()) {
        // Almost certain this shouldn't happen, but knowing my luck, some weird edge case will
        // exist with this.
        logWarnings("resolving artifact " + unresolvedArtifact, response.getExceptions());
        return requireNonNull(response.getArtifact(), "No artifact was returned! Panic!");
      }

      throw resolutionException(
          "failed to resolve artifact " + unresolvedArtifact, response.getExceptions());
    } catch (ArtifactResolutionException ex) {
      throw new ResolutionException("Failed to resolve artifact " + unresolvedArtifact, ex);
    }
  }

  // Include project dependencies via flag in callee, don't do it here, it is a mess.
  Collection<Artifact> resolveDependenciesToArtifacts(
      List<Dependency> unresolvedDependencies,
      Set<String> dependencyScopes,
      boolean failOnInvalidDependencies
  ) throws ResolutionException {
    var scopeFilter = new ScopeDependencyFilter(dependencyScopes);
    var collectRequest = new CollectRequest(unresolvedDependencies, null, remoteRepositories);
    var request = new DependencyRequest(collectRequest, scopeFilter);

    log.debug(
        "Attempting to resolve the following dependencies in this pass: {}",
        unresolvedDependencies
    );

    var response = resolveDependencies(request, failOnInvalidDependencies);
    return extractArtifactsFromResolvedDependencies(response);
  }

  private DependencyResult resolveDependencies(
      DependencyRequest request,
      boolean failOnInvalidDependencies
  ) throws ResolutionException {
    DependencyResult response;

    // Part 1: resolve the dependencies directly.
    try {
      response = repositorySystem.resolveDependencies(repositorySystemSession, request);
    } catch (DependencyResolutionException ex) {
      // GH-299: if this exception is raised, we may still have some results we can use. If this is
      // the case then resolution only partially failed, so continue for now unless strict
      // resolution is enabled. We do not fail-fast here anymore (since 2.4.0) as any resolution
      // errors should be dealt with by the maven-compiler-plugin later on if needed.
      //
      // If we didn't get any result, then something more fatal has occurred, so raise.
      response = ex.getResult();

      if (response == null || failOnInvalidDependencies) {
        throw new ResolutionException("Failed to resolve dependencies from repositories", ex);
      }

      // Log the message as well here as we omit it by default if `--errors' is not passed to Maven.
      log.warn(
          "Error resolving one or more dependencies, dependencies may be missing during "
              + "protobuf compilation! {}", ex.getMessage(), ex
      );
    }

    return response;
  }

  private List<Artifact> extractArtifactsFromResolvedDependencies(
      DependencyResult response
  ) throws ResolutionException {
    var artifacts = new ArrayList<Artifact>();
    var exceptions = new ArrayList<Exception>();
    var failedAtLeastOnce = false;

    for (var artifactResponse : response.getArtifactResults()) {
      exceptions.addAll(artifactResponse.getExceptions());
      var artifact = artifactResponse.getArtifact();
      if (artifact == null) {
        failedAtLeastOnce = true;
      } else {
        artifacts.add(artifact);
      }
    }

    if (failedAtLeastOnce) {
      throw resolutionException("Failed to resolve artifacts for dependencies", exceptions);
    }

    return artifacts;
  }

  private ResolutionException resolutionException(String errorMessage, Iterable<Exception> causes) {
    var ex = new ResolutionException(errorMessage);
    var iterator = causes.iterator();
    if (iterator.hasNext()) {
      ex.initCause(iterator.next());
      iterator.forEachRemaining(ex::addSuppressed);
    }
    return ex;
  }

  private void logWarnings(String descriptionOfAction, Iterable<Exception> exceptions) {
    var iterator = exceptions.iterator();

    if (iterator.hasNext()) {
      iterator.forEachRemaining(ex -> log.debug(
          "Encountered a non-fatal resolution error while {}", descriptionOfAction, ex));
    }
  }
}
