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

import static java.util.function.Function.identity;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.scope.MojoExecutionScoped;
import org.apache.maven.model.DependencyManagement;
import org.apache.maven.project.MavenProject;
import org.eclipse.sisu.Description;
import org.jspecify.annotations.Nullable;

/**
 * Helper that determines the Maven dependency management in a way that Aether can understand, and
 * enables filling in "inferred" information on dependencies from any project dependency
 * management.
 *
 * @author Ashley Scopes
 * @since 2.13.0
 */
@Description("Provides dependencyManagement resolution for Aether dependencies")
@MojoExecutionScoped
@Named
final class AetherDependencyManagement {

  private final Map<String, org.eclipse.aether.artifact.Artifact> effectiveDependencyManagement;

  @Inject
  AetherDependencyManagement(MavenSession mavenSession, AetherArtifactMapper artifactMapper) {

    // Do this on initialization to avoid repeatedly computing the same thing on each dependency.
    // This logic may become expensive to perform for large projects if they have a large number of
    // managed dependencies (e.g. projects that inherit from spring-boot-starter-parent).
    //
    // These attributes may be null if excluded from the Maven model recursively.
    effectiveDependencyManagement = Optional.of(mavenSession.getCurrentProject())
        .map(MavenProject::getDependencyManagement)
        .map(DependencyManagement::getDependencies)
        .stream()
        .flatMap(Collection::stream)
        .map(artifactMapper::mapMavenDependencyToEclipseArtifact)
        .collect(deduplicateArtifacts());
  }

  org.eclipse.aether.graph.Dependency fillManagedAttributes(
      org.eclipse.aether.graph.Dependency dependency
  ) {
    var artifact = dependency.getArtifact();

    if (isSpecified(artifact.getVersion())) {
      // Nothing to override here.
      return dependency;
    }

    var key = getDependencyManagementKey(artifact);
    var managedArtifact = effectiveDependencyManagement.get(key);

    if (managedArtifact == null) {
      // Nothing that can override us here.
      return dependency;
    }

    return new org.eclipse.aether.graph.Dependency(
        managedArtifact,
        dependency.getScope(),
        dependency.getOptional(),
        dependency.getExclusions()
    );
  }

  static Collector<
      org.eclipse.aether.artifact.Artifact,
      ?,
      Map<String, org.eclipse.aether.artifact.Artifact>> deduplicateArtifacts() {
    return Collectors.collectingAndThen(
        Collectors.toMap(
            AetherDependencyManagement::getDependencyManagementKey,
            identity(),
            AetherDependencyManagement::newestArtifact,
            // Retain order. It matters!
            // Luckily, merge on a linked hash map retains the initial
            // order regardless of the item that is chosen.
            LinkedHashMap::new
        ),
        Collections::unmodifiableMap
    );
  }

  private static String getDependencyManagementKey(org.eclipse.aether.artifact.Artifact artifact) {
    // Inspired by the logic in Maven's Dependency class.

    var builder = new StringBuilder()
        .append(artifact.getGroupId())
        .append(":")
        .append(artifact.getArtifactId())
        .append(":")
        .append(artifact.getExtension());

    if (isSpecified(artifact.getClassifier())) {
      builder.append(":")
          .append(artifact.getClassifier());
    }

    return builder.toString();
  }

  private static org.eclipse.aether.artifact.Artifact newestArtifact(
      org.eclipse.aether.artifact.Artifact a,
      org.eclipse.aether.artifact.Artifact b
  ) {
    var versionA = parseVersion(a.getVersion());
    var versionB = parseVersion(b.getVersion());
    return versionA.compareTo(versionB) < 0 ? b : a;
  }

  private static boolean isSpecified(@Nullable String value) {
    return value != null && !value.isBlank();
  }

  private static ComparableVersion parseVersion(@Nullable String version) {
    if (!isSpecified(version)) {
      // Lowest possible priority version.
      version = "0.0.0-a0";
    }
    return new ComparableVersion(version);
  }
}
