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

import org.eclipse.aether.AbstractForwardingRepositorySystemSession;
import org.eclipse.aether.RepositoryCache;
import org.eclipse.aether.RepositorySystemSession;
import org.jspecify.annotations.Nullable;

/**
 * Custom repository session for the Protobuf Maven Plugin which injects some special components to
 * deal with resolution of dependencies for edge cases.
 *
 * <p>For all other purposes, this delegates to the default implementation.
 *
 * @author Ashley Scopes
 * @since 2.0.3
 */
final class ProtobufMavenPluginRepositorySession
    extends AbstractForwardingRepositorySystemSession {

  private final RepositorySystemSession delegate;

  ProtobufMavenPluginRepositorySession(RepositorySystemSession delegate) {
    this.delegate = delegate;
  }

  @Override
  protected RepositorySystemSession getSession() {
    return delegate;
  }

  @Nullable
  @Override
  public RepositoryCache getCache() {
    // GH-579: Temporarily disabled all caching to help debug issues with repository
    // resolution. This may be enabled in the future again.
    return null;
  }

  @Override
  public WildcardAwareDependencyTraverser getDependencyTraverser() {
    return new WildcardAwareDependencyTraverser(delegate.getDependencyTraverser());
  }
}
