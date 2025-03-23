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

import static java.util.function.Function.identity;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import org.apache.maven.artifact.versioning.ComparableVersion;
import org.apache.maven.execution.MavenSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.graph.Dependency;
import org.jspecify.annotations.Nullable;

/**
 * Helper that determines the Maven dependency management in a way that Aether can understand, and
 * enables filling in "inferred" information on dependencies from any project dependency
 * management.
 *
 * @author Ashley Scopes
 * @since 2.13.0
 */
final class AetherDependencyManagement {

  private final Map<String, Artifact> effectiveDependencyManagement;

  AetherDependencyManagement(MavenSession mavenSession, AetherArtifactMapper artifactMapper) {

    // Do this on initialization to avoid repeatedly computing the same thing on each dependency.
    // This logic may become expensive to perform for large projects if they have a large number of
    // managed dependencies (e.g. projects that inherit from spring-boot-starter-parent).
    effectiveDependencyManagement = mavenSession.getCurrentProject()
        .getDependencyManagement()
        .getDependencies()
        .stream()
        .map(artifactMapper::mapMavenDependencyToEclipseArtifact)
        .collect(deduplicateArtifacts());
  }

  Dependency fillManagedAttributes(Dependency dependency) {
    var artifact = dependency.getArtifact();

    if (!isUnspecified(artifact.getVersion())) {
      // Nothing to override here.
      return dependency;
    }

    var key = getDependencyManagementKey(artifact);
    var managedArtifact = effectiveDependencyManagement.get(key);

    if (managedArtifact == null) {
      // Nothing that can override us here.
      return dependency;
    }

    return new Dependency(
        managedArtifact,
        dependency.getScope(),
        dependency.getOptional(),
        dependency.getExclusions()
    );
  }

  public static Collector<Artifact, ?, Map<String, Artifact>> deduplicateArtifacts() {
    return Collectors.collectingAndThen(
        Collectors.toMap(
            AetherDependencyManagement::getDependencyManagementKey,
            identity(),
            AetherDependencyManagement::newestArtifact,
            // Retain order. It matters!
            LinkedHashMap::new
        ),
        Collections::unmodifiableMap
    );
  }

  private static String getDependencyManagementKey(Artifact artifact) {
    // Inspired by the logic in Maven's Dependency class.

    var builder = new StringBuilder()
        .append(artifact.getGroupId())
        .append(":")
        .append(artifact.getArtifactId())
        .append(":")
        .append(artifact.getExtension());

    if (!isUnspecified(artifact.getClassifier())) {
      builder.append(":")
          .append(artifact.getClassifier());
    }

    return builder.toString();
  }

  private static Artifact newestArtifact(Artifact a, Artifact b) {
    var versionA = parseVersion(a.getVersion());
    var versionB = parseVersion(b.getVersion());
    return versionA.compareTo(versionB) < 0 ? b : a;
  }

  private static boolean isUnspecified(@Nullable String value) {
    return value == null || value.isBlank();
  }

  private static ComparableVersion parseVersion(@Nullable String version) {
    if (isUnspecified(version)) {
      // Lowest possible priority version.
      version = "0.0.0-a0";
    }
    return new ComparableVersion(version);
  }
}
