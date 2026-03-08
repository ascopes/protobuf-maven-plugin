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
package io.github.ascopes.protobufmavenplugin.dependencies.aether;

import java.util.List;
import java.util.Set;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.jspecify.annotations.Nullable;

/**
 * Implementation of a subset of functionality within
 * {@link org.eclipse.aether.util.filter.ScopeDependencyFilter} that is easier to unit test.
 *
 * @author Ashley Scopes
 * @since 5.0.2
 */
final class InclusiveScopeDependencyFilter implements DependencyFilter {

  // Visible for testing only.
  private final Set<String> allowedScopes;

  InclusiveScopeDependencyFilter(Set<String> allowedScopes) {
    this.allowedScopes = allowedScopes;
  }

  @Override
  public boolean accept(DependencyNode node, List<DependencyNode> parents) {
    var dependency = node.getDependency();
    return dependency != null && allowedScopes.contains(dependency.getScope());
  }

  // For testing only.
  @Override
  public boolean equals(@Nullable Object obj) {
    return obj instanceof InclusiveScopeDependencyFilter self
        && self.allowedScopes.equals(allowedScopes);
  }

  @Override
  public int hashCode() {
    return allowedScopes.hashCode();
  }
}
