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
package io.github.ascopes.protobufmavenplugin.dependencies;

/**
 * Parameter to configure how to resolve transitive dependencies.
 *
 * @author Ashley Scopes
 * @since 1.2.0
 */
public enum DependencyResolutionDepth {

  /**
   * Only resolve direct dependencies that were explicitly included.
   * No transitive dependencies will be resolved.
   */
  DIRECT,

  /**
   * Resolve all transitive dependencies.
   */
  TRANSITIVE,
}
