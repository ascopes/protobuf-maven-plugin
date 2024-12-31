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

import io.github.ascopes.protobufmavenplugin.dependencies.DependencyResolutionDepth;
import io.github.ascopes.protobufmavenplugin.utils.FileUtils;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.eclipse.aether.artifact.DefaultArtifact;
import org.eclipse.aether.graph.Exclusion;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapter mapper for conversion between dependency and artifact types in various library formats.
 *
 * @author Ashley Scopes
 * @since 2.4.4
 */
final class AetherArtifactMapper {
  // If you are changing this file, and are using any types that are ambiguous between the
  // protobuf-maven-plugin, eclipse aether and maven core types, even if just visually, then
  // please keep their fully qualified names present for readability.

  private static final String DEFAULT_EXTENSION = "jar";
  private static final String DEFAULT_SCOPE = "compile";

  private static final Logger log = LoggerFactory.getLogger(AetherArtifactMapper.class);

  private final org.apache.maven.artifact.handler.ArtifactHandler artifactHandler;
  private final org.eclipse.aether.artifact.ArtifactTypeRegistry artifactTypeRegistry;

  AetherArtifactMapper(
      org.apache.maven.artifact.handler.ArtifactHandler artifactHandler,
      org.eclipse.aether.artifact.ArtifactTypeRegistry artifactTypeRegistry
  ) {
    this.artifactHandler = artifactHandler;
    this.artifactTypeRegistry = artifactTypeRegistry;
  }

  // Visible for testing only.
  org.apache.maven.artifact.handler.ArtifactHandler getArtifactHandler() {
    return artifactHandler;
  }

  org.eclipse.aether.artifact.ArtifactTypeRegistry getArtifactTypeRegistry() {
    return artifactTypeRegistry;
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
    return new org.eclipse.aether.artifact.DefaultArtifact(
        mavenArtifact.getGroupId(),
        mavenArtifact.getArtifactId(),
        classifierOrDefault(mavenArtifact.getClassifier()),
        extensionOrDefault(mavenArtifact.getType()),
        mavenArtifact.getVersion()
    );
  }

  org.eclipse.aether.graph.Dependency mapPmpArtifactToEclipseDependency(
      io.github.ascopes.protobufmavenplugin.dependencies.MavenArtifact mavenArtifact,
      io.github.ascopes.protobufmavenplugin.dependencies.DependencyResolutionDepth defaultDepth
  ) {
    var effectiveDependencyResolutionDepth = requireNonNullElse(
        mavenArtifact.getDependencyResolutionDepth(),
        defaultDepth
    );

    var exclusions = effectiveDependencyResolutionDepth == DependencyResolutionDepth.DIRECT
        ? List.of(WildcardAwareDependencyTraverser.WILDCARD_EXCLUSION)
        : List.<Exclusion>of();

    var artifact = mapPmpArtifactToEclipseArtifact(mavenArtifact);

    return new org.eclipse.aether.graph.Dependency(
        artifact,
        DEFAULT_SCOPE,
        false,
        exclusions
    );
  }

  org.eclipse.aether.graph.Dependency mapMavenDependencyToEclipseDependency(
      org.apache.maven.model.Dependency mavenDependency
  ) {
    var artifact = new DefaultArtifact(
        mavenDependency.getGroupId(),
        mavenDependency.getArtifactId(),
        classifierOrDefault(mavenDependency.getClassifier()),
        null,  // Inferred elsewhere.
        mavenDependency.getVersion(),
        extensionToEclipseArtifactType(mavenDependency.getType())
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

  private org.eclipse.aether.artifact.@Nullable ArtifactType extensionToEclipseArtifactType(
      @Nullable String extension
  ) {
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
}
