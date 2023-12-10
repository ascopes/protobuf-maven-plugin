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
package io.github.ascopes.protobufmavenplugin.dependencies;

import io.github.ascopes.protobufmavenplugin.platform.HostEnvironment;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.util.HashSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Artifact resolver that can resolve an executable from Maven repositories.
 *
 * @author Ashley Scopes
 */
public final class MavenExecutableResolver {

  private static final Logger LOGGER = LoggerFactory.getLogger(MavenExecutableResolver.class);

  private final MavenArtifactResolver artifactResolver;

  /**
   * Initialise the resolver.
   *
   * @param artifactResolver  the artifact resolver to use.
   */
  public MavenExecutableResolver(MavenArtifactResolver artifactResolver) {
    this.artifactResolver = artifactResolver;
  }

  /**
   * Determine the path to the executable.
   *
   * @param executable the executable to resolve.
   * @param version the version of the executable.
   * @return the executable path.
   * @throws DependencyResolutionException if resolution fails for any reason.
   */
  public Path resolve(Executable executable, String version) throws DependencyResolutionException {
    var coordinate = executable.getMavenArtifactCoordinate(version);
    var path = artifactResolver.resolveArtifact(coordinate);

    LOGGER.info("Resolved {} to local path '{}'", coordinate, path);

    if (!HostEnvironment.isWindows()) {
      LOGGER.debug("Ensuring '{}' is marked as executable", path);

      try {
        // Copy so we can modify the set.
        var permissions = new HashSet<>(Files.getPosixFilePermissions(path));
        permissions.add(PosixFilePermission.OWNER_EXECUTE);
        Files.setPosixFilePermissions(path, permissions);
      } catch (IOException ex) {
        throw new DependencyResolutionException(
            "Setting executable bit for '" + path + "' failed", ex
        );
      }
    }

    return path;
  }
}
