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

package io.github.ascopes.protobufmavenplugin.dependency;

import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.maven.model.Dependency;
import org.apache.maven.shared.transfer.artifact.ArtifactCoordinate;
import org.apache.maven.shared.transfer.artifact.DefaultArtifactCoordinate;
import org.apache.maven.shared.transfer.dependencies.DefaultDependableCoordinate;
import org.apache.maven.shared.transfer.dependencies.DependableCoordinate;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Implementation independent deacriptor for an artifact or dependency that
 * can be used in a Maven Plugin parameter.
 *
 * @author Ashley Scopes
 * @since 1.2.0
 */
public final class MavenArtifact {
  private static final Logger log = LoggerFactory.getLogger(MavenArtifact.class);

  private @Nullable String groupId;
  private @Nullable String artifactId;
  private @Nullable String version;
  private @Nullable String classifier;
  private @Nullable String type;

  public Optional<String> getGroupId() {
    return Optional.ofNullable(groupId);
  }

  public void setGroupId(@Nullable String groupId) {
    this.groupId = groupId;
  }

  public Optional<String> getArtifactId() {
    return Optional.ofNullable(artifactId);
  }

  public void setArtifactId(@Nullable String artifactId) {
    this.artifactId = artifactId;
  }

  public Optional<String> getVersion() {
    return Optional.ofNullable(version);
  }

  public void setVersion(@Nullable String version) {
    this.version = version;
  }

  public Optional<String> getClassifier() {
    return Optional.ofNullable(classifier);
  }

  public void setClassifier(@Nullable String classifier) {
    this.classifier = classifier;
  }

  public Optional<String> getType() {
    return Optional.ofNullable(type);
  }

  public void setType(@Nullable String type) {
    this.type = type;
  }

  // Alias to enable compatibility with Dependency objects. This avoids a breaking
  // change in v1.x.
  // This should be totally removed in v2.0 to avoid ambiguity.
  @Deprecated(forRemoval = true, since = "1.2.0")
  public void setExtension(@Nullable String extension) {
    log.warn("MavenArtifact.extension is deprecated for removal in v2.0.0. "
        + "Please use MavenArtifact.type instead for future compatibility.");
    this.type = extension;
  }

  @Override
  public boolean equals(@Nullable Object other) {
    if (!(other instanceof MavenArtifact)) {
      return false;
    }

    var that = (MavenArtifact) other;

    return Objects.equals(groupId, that.groupId)
        && Objects.equals(artifactId, that.artifactId)
        && Objects.equals(version, that.version)
        && Objects.equals(classifier, that.classifier)
        && Objects.equals(type, that.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(groupId, artifactId, version, classifier, type);
  }

  @Override
  public String toString() {
    return Stream.of(groupId, artifactId, version, classifier, type)
        .map(attr -> Objects.requireNonNullElse(attr, ""))
        .collect(Collectors.joining(":"));
  }

  public ArtifactCoordinate toArtifactCoordinate() {
    var coordinate = new DefaultArtifactCoordinate();
    coordinate.setGroupId(groupId);
    coordinate.setArtifactId(artifactId);
    coordinate.setVersion(version);
    coordinate.setClassifier(classifier);
    coordinate.setExtension(type);
    return coordinate;
  }

  public DependableCoordinate toDependableCoordinate() {
    var coordinate = new DefaultDependableCoordinate();
    coordinate.setGroupId(groupId);
    coordinate.setArtifactId(artifactId);
    coordinate.setVersion(version);
    coordinate.setClassifier(classifier);
    coordinate.setType(type);
    return coordinate;
  }

  public static MavenArtifact fromDependency(Dependency dependency) {
    var mavenArtifact = new MavenArtifact();
    mavenArtifact.setGroupId(dependency.getGroupId());
    mavenArtifact.setArtifactId(dependency.getArtifactId());
    mavenArtifact.setVersion(dependency.getVersion());
    mavenArtifact.setClassifier(dependency.getClassifier());
    mavenArtifact.setType(dependency.getType());
    return mavenArtifact;
  }
}
