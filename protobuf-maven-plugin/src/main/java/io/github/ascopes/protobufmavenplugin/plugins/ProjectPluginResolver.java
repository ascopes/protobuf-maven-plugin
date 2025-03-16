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
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.execution.scope.MojoExecutionScoped;

/**
 * Resolver for plugins within a project.
 *
 * @author Ashley Scopes
 * @since 2.7.0
 */
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

  /**
   * Resolve all {@code protoc} plugins.
   *
   * <p>Note that duplicates are allowed.
   *
   * @param request the generation request.
   * @return the list of plugins.
   * @throws ResolutionException if resolution fails.
   */
  public List<ResolvedProtocPlugin> resolveProjectPlugins(
      GenerationRequest request
  ) throws ResolutionException {
    return Stream.<Collection<? extends ResolvedProtocPlugin>>builder()
        .add(binaryPluginResolver.resolveMavenPlugins(request.getBinaryMavenPlugins()))
        .add(binaryPluginResolver.resolvePathPlugins(request.getBinaryPathPlugins()))
        .add(binaryPluginResolver.resolveUrlPlugins(request.getBinaryUrlPlugins()))
        .add(jvmPluginResolver.resolveMavenPlugins(request.getJvmMavenPlugins()))
        .build()
        .flatMap(Collection::stream)
        // Sort by precedence then by initial order (sort is stable which guarantees this property).
        .sorted(Comparator.comparingInt(ResolvedProtocPlugin::getOrder))
        .collect(Collectors.toUnmodifiableList());
  }
}
