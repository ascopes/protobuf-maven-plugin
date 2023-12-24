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
package io.github.ascopes.protobufmavenplugin.dependency;

import static java.util.Objects.requireNonNullElse;

import org.apache.maven.shared.transfer.artifact.ArtifactCoordinate;
import org.apache.maven.shared.transfer.dependencies.DependableCoordinate;
import org.jspecify.annotations.Nullable;


/**
 * POJO that represents a Maven artifact or dependency.
 *
 * <p>This can be used interchangeably with artifact and dependency resolvers.
 *
 * @author Ashley Scopes
 */
public final class Dependency implements ArtifactCoordinate, DependableCoordinate {

  private static final String DEFAULT_EXTENSION = "jar";

  private final String groupId;
  private final String artifactId;
  private final String version;
  private final String type;
  private final @Nullable String classifier;

  public Dependency(
      String groupId,
      String artifactId,
      String version,
      @Nullable String type,
      @Nullable String classifier
  ) {
    this.groupId = groupId;
    this.artifactId = artifactId;
    this.version = version;
    this.type = requireNonNullElse(type, DEFAULT_EXTENSION);
    this.classifier = classifier;
  }

  public Dependency(ArtifactCoordinate coordinate) {
    this(
        coordinate.getGroupId(),
        coordinate.getArtifactId(),
        coordinate.getVersion(),
        coordinate.getExtension(),
        coordinate.getClassifier()
    );
  }

  public Dependency(DependableCoordinate coordinate) {
    this(
        coordinate.getGroupId(),
        coordinate.getArtifactId(),
        coordinate.getVersion(),
        coordinate.getType(),
        coordinate.getClassifier()
    );
  }

  @Override
  public String getGroupId() {
    return groupId;
  }

  @Override
  public String getArtifactId() {
    return artifactId;
  }

  @Override
  public String getVersion() {
    return version;
  }

  @Override
  public String getType() {
    return type;
  }

  @Override
  public String getExtension() {
    return type;
  }

  @Nullable
  @Override
  public String getClassifier() {
    return classifier;
  }

  @Override
  public String toString() {
    var string = "mvn:"
        + groupId + ":"
        + artifactId + ":"
        + version + ":"
        + type;

    if (classifier != null) {
      string += ":" + classifier;
    }

    return string;
  }
}
