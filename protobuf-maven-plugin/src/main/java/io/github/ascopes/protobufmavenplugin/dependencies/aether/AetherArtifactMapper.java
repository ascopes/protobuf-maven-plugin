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

import static java.util.Objects.requireNonNullElse;
import static java.util.function.Predicate.not;

import io.github.ascopes.protobufmavenplugin.fs.FileUtils;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.execution.scope.MojoExecutionScoped;
import org.eclipse.sisu.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapter mapper for conversion between dependency and artifact types in various library formats.
 *
 * @author Ashley Scopes
 * @since 2.4.4
 */
@Description("Maps between various dependency and artifact types")
@MojoExecutionScoped
@Named
final class AetherArtifactMapper {
  // If you are changing this file, and are using any types that are ambiguous between the
  // protobuf-maven-plugin, eclipse aether and maven core types, even if just visually, then
  // please keep their fully qualified names present for readability.

  private static final String DEFAULT_EXTENSION = "jar";
  private static final String DEFAULT_SCOPE = "compile";

  private static final Logger log = LoggerFactory.getLogger(AetherArtifactMapper.class);

  private final org.eclipse.aether.artifact.ArtifactTypeRegistry artifactTypeRegistry;

  @Inject
  AetherArtifactMapper(
      ProtobufMavenPluginRepositorySession protobufMavenPluginRepositorySession
  ) {
    artifactTypeRegistry = protobufMavenPluginRepositorySession.getArtifactTypeRegistry();
  }

  Path mapEclipseArtifactToPath(org.eclipse.aether.artifact.Artifact eclipseArtifact) {
    // TODO(ascopes): when Maven moves to the v2.0.0 resolver API, replace
    //   this method with calls to Artifact.getPath() directly.
    @SuppressWarnings("deprecation")
    var file = eclipseArtifact.getFile();
    return FileUtils.normalize(file.toPath());
  }

  org.eclipse.aether.artifact.Artifact mapPmpArtifactToEclipseArtifact(
      io.github.ascopes.protobufmavenplugin.dependencies.MavenArtifact mavenArtifact
  ) {
    var extension = Objects.requireNonNullElse(mavenArtifact.getType(), DEFAULT_EXTENSION);

    var artifactType = Optional.ofNullable(artifactTypeRegistry.get(extension))
        .orElseGet(() -> new FallbackEclipseArtifactType(extension));

    log.debug(
        "Resolved extension \"{}\" to Aether artifact type (classifier: \"{}\", type: \"{}\", "
            + "id: \"{}\", properties={})",
        mavenArtifact.getType(),
        artifactType.getClassifier(),
        artifactType.getExtension(),
        artifactType.getId(),
        artifactType.getProperties()
    );

    var classifier = Optional.ofNullable(mavenArtifact.getClassifier())
        .filter(not(String::isBlank))
        .orElseGet(artifactType::getClassifier);

    return new org.eclipse.aether.artifact.DefaultArtifact(
        mavenArtifact.getGroupId(),
        mavenArtifact.getArtifactId(),
        classifier,
        artifactType.getExtension(),
        mavenArtifact.getVersion()
    );
  }

  org.eclipse.aether.graph.Dependency mapPmpArtifactToEclipseDependency(
      io.github.ascopes.protobufmavenplugin.dependencies.MavenArtifact mavenArtifact,
      io.github.ascopes.protobufmavenplugin.dependencies.DependencyResolutionDepth defaultDepth
  ) {
    var effectiveDependencyResolutionDepth = depth(mavenArtifact, defaultDepth);

    var isDirectDepth = io.github.ascopes.protobufmavenplugin.dependencies
        .DependencyResolutionDepth.DIRECT == effectiveDependencyResolutionDepth;

    var exclusions = isDirectDepth
        ? Set.of(WildcardAwareDependencyTraverser.WILDCARD_EXCLUSION)
        : mapPmpExclusionsToEclipseExclusions(pmpExclusions(mavenArtifact));

    return new org.eclipse.aether.graph.Dependency(
        mapPmpArtifactToEclipseArtifact(mavenArtifact),
        DEFAULT_SCOPE,
        false,
        exclusions
    );
  }

  org.eclipse.aether.artifact.Artifact mapMavenArtifactToEclipseArtifact(
      org.apache.maven.artifact.Artifact mavenArtifact
  ) {
    // maven-core recommended tool to perform these kind of conversions
    return org.apache.maven.RepositoryUtils.toArtifact(mavenArtifact);
  }

  org.eclipse.aether.artifact.Artifact mapMavenDependencyToEclipseArtifact(
      org.apache.maven.model.Dependency mavenDependency
  ) {
    // maven-core recommended tool to perform these kind of conversions
    return org.apache.maven.RepositoryUtils.toDependency(mavenDependency, artifactTypeRegistry)
        .getArtifact();
  }

  private Set<org.eclipse.aether.graph.Exclusion> mapPmpExclusionsToEclipseExclusions(
      Collection<? extends io.github.ascopes.protobufmavenplugin.dependencies.MavenExclusion>
          exclusions
  ) {
    return exclusions.stream()
        .map(exclusion -> new org.eclipse.aether.graph.Exclusion(
            exclusion.getGroupId(),
            exclusion.getArtifactId(),
            exclusion.getClassifier(),
            exclusion.getType()
        ))
        .collect(Collectors.toUnmodifiableSet());
  }

  private io.github.ascopes.protobufmavenplugin.dependencies.DependencyResolutionDepth depth(
      io.github.ascopes.protobufmavenplugin.dependencies.MavenArtifact mavenArtifact,
      io.github.ascopes.protobufmavenplugin.dependencies.DependencyResolutionDepth defaultDepth
  ) {
    return asPmpMavenDependency(mavenArtifact)
        .map(io.github.ascopes.protobufmavenplugin.dependencies.MavenDependency
            ::getDependencyResolutionDepth)
        .orElse(defaultDepth);
  }

  private Collection<
      ? extends io.github.ascopes.protobufmavenplugin.dependencies.MavenExclusion
        > pmpExclusions(
      io.github.ascopes.protobufmavenplugin.dependencies.MavenArtifact mavenArtifact
  ) {
    return asPmpMavenDependency(mavenArtifact)
        .map(io.github.ascopes.protobufmavenplugin.dependencies.MavenDependency
            ::getExclusions)
        .orElseGet(Set::of);
  }

  private Optional<
      io.github.ascopes.protobufmavenplugin.dependencies.MavenDependency
        > asPmpMavenDependency(
      io.github.ascopes.protobufmavenplugin.dependencies.MavenArtifact mavenArtifact
  ) {
    return Optional.of(mavenArtifact)
        .filter(io.github.ascopes.protobufmavenplugin.dependencies.MavenDependency.class
            ::isInstance)
        .map(io.github.ascopes.protobufmavenplugin.dependencies.MavenDependency.class
            ::cast);
  }
}
