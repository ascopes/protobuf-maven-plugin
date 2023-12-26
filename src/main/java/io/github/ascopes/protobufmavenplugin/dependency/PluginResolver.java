/*
 * Copyright (C) 2023, Ashley Scopes.
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
package io.github.ascopes.protobufmavenplugin.dependency;

import io.github.ascopes.protobufmavenplugin.system.PathResolver;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.shared.transfer.dependencies.DependableCoordinate;
import org.apache.maven.shared.transfer.dependencies.resolve.DependencyResolverException;

/**
 * Resolver for plugins.
 *
 * @author Ashley Scopes
 */
@Named
public final class PluginResolver {

  private final MavenDependencyPathResolver dependencyPathResolver;
  private final PlatformDependencyFactory platformDependencyFactory;
  private final PathResolver systemPathResolver;

  @Inject
  public PluginResolver(
      MavenDependencyPathResolver dependencyPathResolver,
      PlatformDependencyFactory platformDependencyFactory,
      PathResolver systemPathResolver
  ) {
    this.dependencyPathResolver = dependencyPathResolver;
    this.platformDependencyFactory = platformDependencyFactory;
    this.systemPathResolver = systemPathResolver;
  }

  public ResolvedPlugin resolve(
      MavenSession session,
      PluginBean pluginBean
  ) throws IOException, DependencyResolverException {
    var path = resolvePath(session, pluginBean);
    return ImmutableResolvedPlugin
        .builder()
        .bean(pluginBean)
        .path(path)
        .build();
  }

  private Path resolvePath(
      MavenSession session,
      PluginBean pluginBean
  ) throws IOException, DependencyResolverException {
    if (pluginBean.getDependableCoordinate().isPresent()) {
      var coordinate = enrich(pluginBean.getDependableCoordinate().get());

      // We only care about the first dependency in this case.
      return dependencyPathResolver.resolveDependencyPaths(session, coordinate)
          .iterator()
          .next();
    }

    if (pluginBean.getExecutableName().isPresent()) {
      var executableName = pluginBean.getExecutableName().get();
      return systemPathResolver.resolve(executableName)
          .orElseThrow(() -> new FileNotFoundException("No executable '"
              + executableName + "' was found on the system path"));
    }

    throw new IllegalArgumentException(
        "No dependency or executable name was provided for protoc plugin with ID '"
            + pluginBean.getId() + "'");
  }

  private DependableCoordinate enrich(DependableCoordinate coordinate) {
    if (coordinate.getClassifier() == null) {
      return platformDependencyFactory.createPlatformExecutable(
          coordinate.getGroupId(),
          coordinate.getArtifactId(),
          coordinate.getVersion(),
          coordinate.getType()
      );
    }

    return coordinate;
  }
}
