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

import io.github.ascopes.protobufmavenplugin.utils.ResolutionException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Interface for an object that provides resolution of Maven artifacts and dependencies,
 * emitting their paths.
 *
 * <p>This is abstracted such that the existing implementation can be easily changed in
 * the future.
 *
 * @author Ashley Scopes
 * @since 2.6.0
 */
public interface MavenArtifactPathResolver {

  /**
   * Resolve a single Maven artifact directly, and do not resolve any transitive dependencies.
   *
   * @param artifact the artifact to resolve.
   * @return the path to the resolved artifact.
   * @throws ResolutionException if resolution fails in the backend.
   */
  Path resolveArtifact(MavenArtifact artifact) throws ResolutionException;

  /**
   * Resolve all given dependencies based on their resolution depth semantics.
   *
   * @param artifacts                        the artifacts to resolve.
   * @param defaultDependencyResolutionDepth the project default dependency resolution depth.
   * @param dependencyScopes                 the allowed dependency scopes to resolve.
   * @param includeProjectDependencies       whether to also resolve project dependencies and return
   *                                         them in the result.
   * @param failOnInvalidDependencies        if {@code false}, resolution of invalid dependencies
   *                                         will result in errors being logged, but the build will
   *                                         not be halted.
   * @return the paths to each resolved artifact.
   * @throws ResolutionException if resolution failed in the backend.
   */
  List<Path> resolveDependencies(
      Collection<? extends MavenArtifact> artifacts,
      DependencyResolutionDepth defaultDependencyResolutionDepth,
      Set<String> dependencyScopes,
      boolean includeProjectDependencies,
      boolean failOnInvalidDependencies
  ) throws ResolutionException;
}
