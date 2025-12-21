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

import io.github.ascopes.protobufmavenplugin.dependencies.DependencyResolutionDepth;
import io.github.ascopes.protobufmavenplugin.dependencies.MavenArtifact;
import io.github.ascopes.protobufmavenplugin.dependencies.MavenArtifactPathResolver;
import io.github.ascopes.protobufmavenplugin.digests.Digest;
import io.github.ascopes.protobufmavenplugin.fs.FileUtils;
import io.github.ascopes.protobufmavenplugin.fs.TemporarySpace;
import io.github.ascopes.protobufmavenplugin.utils.ResolutionException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.scope.MojoExecutionScoped;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.sisu.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Implementation of {@link MavenArtifactPathResolver} that integrates with the Eclipse Aether
 * artifact resolution backend provided by Eclipse for Apache Maven.
 *
 * @author Ashley Scopes
 * @since 2.4.4, many iterations of this existed in the past with different names.
 */
@Description("Integrates with Eclipse Aether to resolve and download dependencies locally")
@MojoExecutionScoped
@Named
final class AetherMavenArtifactPathResolver implements MavenArtifactPathResolver {

  private static final Logger log = LoggerFactory.getLogger(AetherMavenArtifactPathResolver.class);

  private final MavenSession mavenSession;
  private final AetherArtifactMapper aetherArtifactMapper;
  private final AetherDependencyManagement aetherDependencyManagement;
  private final AetherResolver aetherResolver;
  private final TemporarySpace temporarySpace;

  @Inject
  AetherMavenArtifactPathResolver(
      MavenSession mavenSession,
      AetherArtifactMapper aetherArtifactMapper,
      AetherDependencyManagement aetherDependencyManagement,
      AetherResolver aetherResolver,
      TemporarySpace temporarySpace
  ) {
    this.mavenSession = mavenSession;
    this.aetherArtifactMapper = aetherArtifactMapper;
    this.aetherDependencyManagement = aetherDependencyManagement;
    this.aetherResolver = aetherResolver;
    this.temporarySpace = temporarySpace;
  }

  @Override
  public Path resolveExecutable(MavenArtifact artifact) throws ResolutionException {
    log.debug("Resolving artifact \"{}\"", artifact);
    var unresolvedArtifact = aetherArtifactMapper.mapPmpArtifactToEclipseArtifact(artifact);
    var resolvedArtifact = aetherResolver.resolveRequiredArtifact(unresolvedArtifact);
    var originalPath = aetherArtifactMapper.mapEclipseArtifactToPath(resolvedArtifact);

    // GH-792: make a copy and set that as executable rather than changing what is in the
    // repository, as this is racy between concurrent builds and deemed to be unsafe.
    var digestPart = Digest.compute("SHA-1", artifact.toString()).toHexString();
    var finalPath = temporarySpace.createTemporarySpace("artifacts")
        .resolve(artifact.getArtifactId() + "-" + digestPart + ".exe");

    try {
      log.debug("Copying \"{}\" to \"{}\" and making executable", originalPath, finalPath);
      Files.copy(originalPath, finalPath, StandardCopyOption.REPLACE_EXISTING);
      FileUtils.makeExecutable(finalPath);
    } catch (IOException ex) {
      throw new ResolutionException("Failed to process downloaded artifact " + artifact, ex);
    }

    return finalPath;
  }

  @Override
  public List<Path> resolveDependencies(
      Collection<? extends MavenArtifact> artifacts,
      DependencyResolutionDepth depth,
      Set<String> dependencyScopes,
      boolean includeProjectArtifacts
  ) throws ResolutionException {
    var unresolvedDependencies = new ArrayList<Dependency>();

    artifacts.stream()
        .peek(artifact -> log.debug("Resolving artifact \"{}\" as dependency", artifact))
        .map(artifact -> aetherArtifactMapper.mapPmpArtifactToEclipseDependency(artifact, depth))
        .map(aetherDependencyManagement::fillManagedAttributes)
        .forEach(unresolvedDependencies::add);

    var resolvedArtifacts = aetherResolver
        .resolveDependencies(unresolvedDependencies, dependencyScopes)
        .stream();

    if (includeProjectArtifacts) {
      // As of 2.13.0, we enforce that dependencies are resolved by Maven
      // first. This is less error-prone and a bit faster for regular builds
      // as Maven can cache this stuff however it wants to. This also seems to
      // help avoid GH-596 which can cause heap exhaustion from within Aether
      // for some reason.
      log.debug("Querying project dependencies from Maven model, as requested.");

      var projectArtifacts = mavenSession.getCurrentProject().getArtifacts()
          .stream()
          .filter(artifact -> dependencyScopes.contains(artifact.getScope()))
          .peek(artifact -> log.trace("Including project artifact \"{}\"", artifact))
          .map(aetherArtifactMapper::mapMavenArtifactToEclipseArtifact);

      resolvedArtifacts = Stream.concat(projectArtifacts, resolvedArtifacts);
    }

    return resolvedArtifacts
        .collect(AetherDependencyManagement.deduplicateArtifacts())
        .values()
        .stream()
        .map(aetherArtifactMapper::mapEclipseArtifactToPath)
        // Order matters here, so don't convert to an unordered container in the
        // future. We make assumptions on the order of this elsewhere.
        .toList();
  }
}
