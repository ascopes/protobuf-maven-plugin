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
package io.github.ascopes.protobufmavenplugin.dependencies;

import org.immutables.value.Value.Modifiable;

/**
 * Marker to exclude a specific transitive dependency.
 *
 * <p>Holds an optional classifier and optional extension, in addition to the group ID and
 * artifact ID.
 *
 * @author Ashley Scopes
 * @since 2.12.0
 */
@Modifiable
public interface MavenExclusion {

  /**
   * Value used by Eclipse Aether internally to imply a match for
   * any value.
   */
  static String WILDCARD = "*";

  /**
   * Get the group ID.
   *
   * @return the group ID.
   */
  String getGroupId();

  /**
   * Get the artifact ID.
   *
   * @return the artifact ID.
   */
  String getArtifactId();

  /**
   * Get the artifact classifier.
   *
   * @return the classifier, or {@code *} by default which implies
   *     a match for any classifier.
   */
  default String getClassifier() {
    return WILDCARD;
  }

  /**
   * Get the artifact type.
   *
   * @return the type, or {@code *} by default which implies a match
   *     for any type.
   */
  default String getType() {
    return WILDCARD;
  }
}
