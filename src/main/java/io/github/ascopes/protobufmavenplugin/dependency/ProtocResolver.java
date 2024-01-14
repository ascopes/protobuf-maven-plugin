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
package io.github.ascopes.protobufmavenplugin.dependency;

import io.github.ascopes.protobufmavenplugin.system.FileUtils;
import java.io.IOException;
import java.nio.file.Path;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.execution.MavenSession;

/**
 * Resolver for the {@code protoc} executable.
 *
 * @author Ashley Scopes
 */
@Named
public final class ProtocResolver {
  private static final String EXECUTABLE_NAME = "protoc";
  private static final String GROUP_ID = "com.google.protobuf";
  private static final String ARTIFACT_ID = "protoc";

  private final MavenDependencyPathResolver mavenDependencyPathResolver;
  private final PlatformArtifactFactory platformArtifactFactory;
  private final SystemPathBinaryResolver systemPathResolver;

  @Inject
  public ProtocResolver(
      MavenDependencyPathResolver mavenDependencyPathResolver,
      PlatformArtifactFactory platformArtifactFactory,
      SystemPathBinaryResolver systemPathResolver
  ) {
    this.mavenDependencyPathResolver = mavenDependencyPathResolver;
    this.platformArtifactFactory = platformArtifactFactory;
    this.systemPathResolver = systemPathResolver;
  }

  public Path resolve(
      MavenSession session,
      String version
  ) throws ResolutionException {
    if (version.equalsIgnoreCase("PATH")) {
      return systemPathResolver.resolve(EXECUTABLE_NAME)
          .orElseThrow(() -> new ResolutionException("No protoc executable was found"));
    }

    var coordinate = platformArtifactFactory.createArtifact(
        GROUP_ID,
        ARTIFACT_ID,
        version,
        null,
        null
    );

    // We only care about the first dependency in this case.
    try {
      var path = mavenDependencyPathResolver.resolveArtifact(session, coordinate);
      FileUtils.makeExecutable(path);
      return path;
    } catch (IOException ex) {
      throw new ResolutionException("Failed to set executable bit on protoc binary", ex);
    }
  }
}
