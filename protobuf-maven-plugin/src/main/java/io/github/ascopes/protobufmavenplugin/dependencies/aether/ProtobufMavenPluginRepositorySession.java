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

import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.execution.scope.MojoExecutionScoped;
import org.eclipse.aether.AbstractForwardingRepositorySystemSession;
import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.collection.DependencyTraverser;
import org.eclipse.aether.resolution.ResolutionErrorPolicy;
import org.eclipse.aether.util.graph.traverser.AndDependencyTraverser;
import org.eclipse.aether.util.graph.traverser.FatArtifactTraverser;
import org.eclipse.aether.util.graph.traverser.StaticDependencyTraverser;
import org.eclipse.sisu.Description;

/**
 * Custom repository session for the Protobuf Maven Plugin which injects some special components to
 * deal with resolution of dependencies for edge cases.
 *
 * <p>For all other purposes, this delegates to the default implementation.
 *
 * @author Ashley Scopes
 * @since 2.0.3
 */
@Description("Injects additional functionality into the default Aether RepositorySession")
@MojoExecutionScoped
@Named
final class ProtobufMavenPluginRepositorySession extends AbstractForwardingRepositorySystemSession {

  private final RepositorySystemSession delegate;
  private final DependencyTraverser dependencyTraverser;
  private final ResolutionErrorPolicy resolutionErrorPolicy;

  @Inject
  ProtobufMavenPluginRepositorySession(MavenSession mavenSession) {
    delegate = mavenSession.getRepositorySession();

    dependencyTraverser = new AndDependencyTraverser(
        new WildcardAwareDependencyTraverser(),
        // Avoid OOME by not traversing things known to be fat archives of content.
        // Related to issues in GH-596 and GH-938.
        new FatArtifactTraverser(),
        // Always fall back to traversing JARs, etc.
        new StaticDependencyTraverser(true)
    );

    // As of 2.13.0, we do not want to cache invalid dependencies between builds. This gets a bit
    // confusing for users if it collides with logic that Maven itself is performing, so lets just
    // totally avoid it.
    resolutionErrorPolicy = new NoCacheResolutionErrorPolicy();
  }

  @Override
  protected RepositorySystemSession getSession() {
    return delegate;
  }

  @Override
  public DependencyTraverser getDependencyTraverser() {
    return dependencyTraverser;
  }

  @Override
  public ResolutionErrorPolicy getResolutionErrorPolicy() {
    return resolutionErrorPolicy;
  }
}
