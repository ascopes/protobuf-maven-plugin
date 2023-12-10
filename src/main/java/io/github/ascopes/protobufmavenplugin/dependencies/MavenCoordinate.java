/*
 * Copyright (C) 2023, Ashley Scopes.
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
package io.github.ascopes.protobufmavenplugin.dependencies;

import java.util.Objects;
import java.util.regex.Pattern;
import org.apache.maven.shared.transfer.artifact.ArtifactCoordinate;
import org.apache.maven.shared.transfer.dependencies.DependableCoordinate;
import org.jspecify.annotations.Nullable;

/**
 * Maven coordinate that is compatible with artifact and dependency resolution.
 *
 * @author Ashley Scopes
 */
public final class MavenCoordinate implements ArtifactCoordinate, DependableCoordinate {

  private static final Pattern COORDINATE_PATTERN = Pattern.compile(
      "mvn:(?<groupId>[^:]+)"
          + ":(?<artifactId>[^:]+)"
          + ":(?<version>[^:]+)"
          + "(?::(?<type>.*))?"
          + "(?::(?<classifier>.*))?"
  );

  private final String groupId;
  private final String artifactId;
  private final String version;
  private final String type;
  private final @Nullable String classifier;

  /**
   * Initialise this coordinate.
   *
   * @param groupId the group ID.
   * @param artifactId the artifact ID.
   * @param version the version.
   * @param type the artifact type, or {@code null} to use the default.
   * @param classifier the classifier, or {@code null} if not applicable.
   */
  public MavenCoordinate(
      String groupId,
      String artifactId,
      String version,
      @Nullable String type,
      @Nullable String classifier
  ) {
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = version;
    this.type = Objects.requireNonNullElse(type, "jar");
    this.classifier = classifier;
  }

  /**
   * Get the group ID.
   *
   * @return the group ID.
   */
  @Override
  public String getGroupId() {
    return groupId;
  }

  /**
   * Get the artifact ID.
   *
   * @return the artifact ID.
   */
  @Override
  public String getArtifactId() {
    return artifactId;
  }

  /**
   * Get the version.
   *
   * @return the version.
   */
  @Override
  public String getVersion() {
    return version;
  }

  /**
   * Get the artifact type.
   *
   * @return the artifact type.
   */
  @Override
  public String getType() {
    return type;
  }

  /**
   * Get the artifact extension.
   *
   * @return the artifact extension.
   */
  @Override
  public String getExtension() {
    return type;
  }

  /**
   * Get the classifier.
   *
   * @return the classifier, or {@code null} if not specified.
   */
  @Nullable
  @Override
  public String getClassifier() {
    return classifier;
  }

  /**
   * {@inheritDoc}
   *
   * @return the string representation.
   */
  @Override
  public String toString() {
    var sb = new StringBuilder();
    sb.append("mvn:")
        .append(groupId)
        .append(":")
        .append(artifactId)
        .append(":")
        .append(version)
        .append(":")
        .append(type);

    if (classifier != null) {
      sb.append(":").append(classifier);
    }

    return sb.toString();
  }

  /**
   * Parse a Maven classifier ID into an artifact coordinate.
   *
   * @param id the ID to parse.
   * @return the coordinate.
   * @throws DependencyResolutionException if the coordinate could not be parsed due to a syntax
   *                                       error.
   */
  public static MavenCoordinate parse(String id) throws DependencyResolutionException {
    var matcher = COORDINATE_PATTERN.matcher(id);
    if (!matcher.matches()) {
      throw new DependencyResolutionException("Failed to parse artifact coordinate '" + id + "'");
    }

    return new MavenCoordinate(
        matcher.group("groupId"),
        matcher.group("artifactId"),
        matcher.group("version"),
        matcher.group("type"),
        matcher.group("classifier")
    );
  }
}
