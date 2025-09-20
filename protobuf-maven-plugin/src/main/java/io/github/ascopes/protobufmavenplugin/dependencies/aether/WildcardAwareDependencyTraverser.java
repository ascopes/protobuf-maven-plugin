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

import io.github.ascopes.protobufmavenplugin.dependencies.DependencyResolutionDepth;
import org.eclipse.aether.collection.DependencyCollectionContext;
import org.eclipse.aether.collection.DependencyTraverser;
import org.eclipse.aether.graph.Dependency;
import org.eclipse.aether.graph.Exclusion;

/**
 * Dependency traverser that can detect a wildcard exclusion that is used to flag an artifact with a
 * {@link DependencyResolutionDepth#DIRECT} dependency resolution depth.
 *
 * <p>For all other purposes, this delegates to the default implementation.
 *
 * @author Ashley Scopes
 * @since 2.0.3
 */
final class WildcardAwareDependencyTraverser implements DependencyTraverser {

  static Exclusion WILDCARD_EXCLUSION = new Exclusion("*", "*", "*", "*");

  private final DependencyTraverser delegate;

  WildcardAwareDependencyTraverser(DependencyTraverser delegate) {
    this.delegate = delegate;
  }

  // Visible for testing.
  DependencyTraverser getDelegate() {
    return delegate;
  }

  @Override
  public boolean traverseDependency(Dependency dependency) {
    // If we internally have the special wildcard exclusion we define, then assume it is a
    // dependency with DependencyResolutionDepth.DIRECT, so don't traverse it any further.
    return !dependency.getExclusions().contains(WILDCARD_EXCLUSION)
        && delegate.traverseDependency(dependency);
  }

  @Override
  public WildcardAwareDependencyTraverser deriveChildTraverser(
      DependencyCollectionContext context
  ) {
    return new WildcardAwareDependencyTraverser(delegate.deriveChildTraverser(context));
  }
}
