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

import org.jspecify.annotations.Nullable;


/**
 * Base interface for a parameter that references a deployed Maven artifact
 * somewhere.
 *
 * <p>Implementation interfaces should extend this type rather than using it
 * directly.
 *
 * @author Ashley Scopes
 * @since 1.2.0
 */
public interface MavenArtifact {

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
   * Get the artifact version.
   *
   * <p>May be {@code null} if we expect to discover the version from dependency management.
   *
   * @return the version.
   */
  @Nullable String getVersion();

  /**
   * Get the artifact type.
   *
   * <p>May be {@code null} if the default should be used.
   *
   * @return the artifact type.
   */
  @Nullable String getType();

  /**
   * Get the artifact classifier.
   *
   * <p>May be {@code null} if no classifier is set.
   *
   * @return the artifact classifier.
   */
  @Nullable String getClassifier();
}
