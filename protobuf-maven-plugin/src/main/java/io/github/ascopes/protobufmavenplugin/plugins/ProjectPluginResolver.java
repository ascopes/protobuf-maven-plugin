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
package io.github.ascopes.protobufmavenplugin.plugins;

import io.github.ascopes.protobufmavenplugin.generation.GenerationRequest;
import io.github.ascopes.protobufmavenplugin.utils.ResolutionException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.execution.scope.MojoExecutionScoped;
import org.eclipse.sisu.Description;

/**
 * Resolver for plugins within a project that may be located in several
 * different places.
 *
 * @author Ashley Scopes
 * @since 2.7.0
 */
@Description("Resolves and packages protoc plugins from various remote and local locations")
@MojoExecutionScoped
@Named
public final class ProjectPluginResolver {
  private final BinaryPluginResolver binaryPluginResolver;
  private final JvmPluginResolver jvmPluginResolver;

  @Inject
  ProjectPluginResolver(
      BinaryPluginResolver binaryPluginResolver,
      JvmPluginResolver jvmPluginResolver
  ) {
    this.binaryPluginResolver = binaryPluginResolver;
    this.jvmPluginResolver = jvmPluginResolver;
  }

  public Collection<ResolvedProtocPlugin> resolveProjectPlugins(
      GenerationRequest request
  ) throws ResolutionException {
    // XXX: we could run this in parallel?

    var plugins = new ArrayList<ResolvedProtocPlugin>();
    plugins.addAll(binaryPluginResolver.resolveMavenPlugins(
        extract(request, BinaryMavenProtocPlugin.class),
        request.getOutputDirectory()
    ));
    plugins.addAll(binaryPluginResolver.resolvePathPlugins(
        extract(request, PathProtocPlugin.class),
        request.getOutputDirectory()
    ));
    plugins.addAll(binaryPluginResolver.resolveUrlPlugins(
        extract(request, UriProtocPlugin.class),
        request.getOutputDirectory()
    ));
    plugins.addAll(jvmPluginResolver.resolveMavenPlugins(
        extract(request, JvmMavenProtocPlugin.class),
        request.getOutputDirectory()
    ));
    plugins.trimToSize();
    return Collections.unmodifiableList(plugins);
  }

  private <T> Collection<T> extract(GenerationRequest request, Class<T> implementationType) {
    return request.getProtocPlugins()
        .stream()
        .filter(implementationType::isInstance)
        .map(implementationType::cast)
        .toList();
  }
}
