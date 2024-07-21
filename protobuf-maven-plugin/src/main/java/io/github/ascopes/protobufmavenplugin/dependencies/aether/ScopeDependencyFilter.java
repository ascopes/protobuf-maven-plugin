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

package io.github.ascopes.protobufmavenplugin.dependencies.aether;

import java.util.List;
import java.util.Set;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;

/**
 * Slimmed-down implementation of {@link org.eclipse.aether.util.filter.ScopeDependencyFilter}.
 *
 * <p>This is provided as Maven 3.8.x does not include the Eclipse Aether ScopeDependencyFilter
 * at runtime, whereas Maven 3.9.x and later does.
 *
 * <p>TODO: delete this for Maven 4.x and use the Aether implementation instead.
 *
 * @author Ashley Scopes
 * @since 2.4.0
 */
final class ScopeDependencyFilter implements DependencyFilter {
  private final Set<String> includedScopes;

  /**
   * Initialise this filter.
   *
   * @param includedScopes the scopes to allow.
   */
  ScopeDependencyFilter(Set<String> includedScopes) {
    this.includedScopes = includedScopes;
  }

  @Override
  public boolean accept(DependencyNode node, List<DependencyNode> parents) {
    var dependency = node.getDependency();
    return dependency == null || includedScopes.contains(dependency.getScope());
  }
}
