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

package io.github.ascopes.protobufmavenplugin.protoc;

import io.github.ascopes.protobufmavenplugin.dependencies.ImmutableMavenDependency;
import io.github.ascopes.protobufmavenplugin.dependencies.MavenArtifactPathResolver;
import io.github.ascopes.protobufmavenplugin.dependencies.PlatformClassifierFactory;
import io.github.ascopes.protobufmavenplugin.dependencies.ResolutionException;
import io.github.ascopes.protobufmavenplugin.dependencies.UrlResourceFetcher;
import io.github.ascopes.protobufmavenplugin.utils.FileUtils;
import io.github.ascopes.protobufmavenplugin.utils.HostSystem;
import io.github.ascopes.protobufmavenplugin.utils.SystemPathBinaryResolver;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

  private static final Logger log = LoggerFactory.getLogger(ProtocResolver.class);

  private final HostSystem hostSystem;
  private final MavenArtifactPathResolver artifactPathResolver;
  private final PlatformClassifierFactory platformClassifierFactory;
  private final SystemPathBinaryResolver systemPathResolver;
  private final UrlResourceFetcher urlResourceFetcher;

  @Inject
  public ProtocResolver(
      HostSystem hostSystem,
      MavenArtifactPathResolver artifactPathResolver,
      PlatformClassifierFactory platformClassifierFactory,
      SystemPathBinaryResolver systemPathResolver,
      UrlResourceFetcher urlResourceFetcher
  ) {
    this.hostSystem = hostSystem;
    this.artifactPathResolver = artifactPathResolver;
    this.platformClassifierFactory = platformClassifierFactory;
    this.systemPathResolver = systemPathResolver;
    this.urlResourceFetcher = urlResourceFetcher;
  }

  public Optional<Path> resolve(String version) throws ResolutionException {
    if (version.equalsIgnoreCase("latest")) {
      throw new IllegalArgumentException(
          "Cannot use LATEST for the protobuf.compiler.version. "
              + "Google has not released linear versions in the past, meaning that "
              + "using LATEST will have unexpected behaviour."
      );
    }

    if (version.equalsIgnoreCase("PATH")) {
      return systemPathResolver.resolve(EXECUTABLE_NAME);
    }

    var path = version.contains(":")
        ? resolveFromUrl(version)
        : resolveFromMavenRepositories(version);

    if (path.isPresent()) {
      var resolvedPath = path.get();
  
      try {
        FileUtils.makeExecutable(resolvedPath);

      } catch (IOException ex) {
        throw new ResolutionException(
            "Failed to set executable bit on protoc binary at " + resolvedPath
                + ": " + ex.getMessage(),
            ex
        );
      }
    }

    return path;
  }

  private Optional<Path> resolveFromUrl(String url) throws ResolutionException {
    try {
      return urlResourceFetcher.fetchFileFromUrl(new URL(url), ".exe");

    } catch (IOException ex) {
      throw new ResolutionException(
          "Failed to fetch resource from URL " + url + ": " + ex.getMessage(),
          ex
      );
    }
  }

  private Optional<Path> resolveFromMavenRepositories(String version) throws ResolutionException {
    if (hostSystem.isProbablyAndroid()) {
      log.warn(
          "It looks like you are using Android! Android is known to be missing "
              + "system calls for Linux that the official protoc binaries rely on "
              + "to work correctly. If you encounter issues, run Maven again with the "
              + "-Dprotobuf.compiler.version=PATH flag to rerun with a custom "
              + "user-provided version of protoc. If nothing fails, then you can "
              + "safely ignore this message!"
      );
    }

    var artifact = ImmutableMavenDependency.builder()
        .groupId(GROUP_ID)
        .artifactId(ARTIFACT_ID)
        .version(version)
        .type("exe")
        .classifier(platformClassifierFactory.getClassifier(ARTIFACT_ID))
        .build();

    return Optional.of(artifactPathResolver.resolveArtifact(artifact));
  }
}
