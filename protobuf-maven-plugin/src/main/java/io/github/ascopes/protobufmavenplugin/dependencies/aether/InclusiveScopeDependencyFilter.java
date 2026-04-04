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

import io.github.ascopes.protobufmavenplugin.utils.VisibleForTestingOnly;
import java.util.List;
import java.util.Set;
import org.eclipse.aether.graph.DependencyFilter;
import org.eclipse.aether.graph.DependencyNode;
import org.eclipse.aether.util.filter.ScopeDependencyFilter;

/**
 * Implementation of a subset of functionality within {@link ScopeDependencyFilter} that is easier
 * to unit test.
 *
 * @author Ashley Scopes
 * @since 5.0.2
 */
@SuppressWarnings("ClassCanBeRecord")
final class InclusiveScopeDependencyFilter implements DependencyFilter {

  private final Set<String> allowedScopes;

  /**
   * Initialize this filter.
   *
   * @param allowedScopes the scopes to allow.
   */
  InclusiveScopeDependencyFilter(Set<String> allowedScopes) {
    this.allowedScopes = allowedScopes;
  }

  @VisibleForTestingOnly
  Set<String> getAllowedScopes() {
    return allowedScopes;
  }

  @Override
  public boolean accept(DependencyNode node, List<DependencyNode> parents) {
    var dependency = node.getDependency();
    return dependency != null && allowedScopes.contains(dependency.getScope());
  }
}
