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

import io.github.ascopes.protobufmavenplugin.dependencies.DependencyResolutionDepth;
import io.github.ascopes.protobufmavenplugin.dependencies.ImmutableMavenDependency;
import io.github.ascopes.protobufmavenplugin.dependencies.MavenDependencyPathResolver;
import io.github.ascopes.protobufmavenplugin.dependencies.PlatformClassifierFactory;
import io.github.ascopes.protobufmavenplugin.dependencies.ResolutionException;
import io.github.ascopes.protobufmavenplugin.dependencies.SystemPathBinaryResolver;
import io.github.ascopes.protobufmavenplugin.dependencies.UrlResourceFetcher;
import io.github.ascopes.protobufmavenplugin.utils.FileUtils;
import io.github.ascopes.protobufmavenplugin.utils.HostSystem;
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
  private final MavenDependencyPathResolver dependencyResolver;
  private final PlatformClassifierFactory platformClassifierFactory;
  private final SystemPathBinaryResolver systemPathResolver;
  private final UrlResourceFetcher urlResourceFetcher;

  @Inject
  public ProtocResolver(
      HostSystem hostSystem,
      MavenDependencyPathResolver dependencyResolver,
      PlatformClassifierFactory platformClassifierFactory,
      SystemPathBinaryResolver systemPathResolver,
      UrlResourceFetcher urlResourceFetcher
  ) {
    this.hostSystem = hostSystem;
    this.dependencyResolver = dependencyResolver;
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
      try {
        FileUtils.makeExecutable(path.get());
      } catch (IOException ex) {
        throw new ResolutionException("Failed to set executable bit on protoc binary", ex);
      }
    }

    return path;
  }

  private Optional<Path> resolveFromUrl(String url) throws ResolutionException {
    try {
      return urlResourceFetcher.fetchFileFromUrl(new URL(url), ".exe");
    } catch (IOException ex) {
      throw new ResolutionException(ex.getMessage(), ex);
    }
  }

  private Optional<Path> resolveFromMavenRepositories(String version) throws ResolutionException {
    if (hostSystem.isProbablyAndroidTermux()) {
      log.warn(
          "It looks like you are using Termux on Android. You may encounter issues "
              + "running the detected protoc binary from Maven central. If this is "
              + "an issue, install the protoc compiler manually from your package "
              + "manager (apt update && apt install protobuf), and then invoke "
              + "Maven with the -Dprotobuf.compiler.version=PATH flag."
      );
    }

    var artifact = ImmutableMavenDependency.builder()
        .groupId(GROUP_ID)
        .artifactId(ARTIFACT_ID)
        .version(version)
        .type("exe")
        .classifier(platformClassifierFactory.getClassifier(ARTIFACT_ID))
        .build();

    // First result is all we care about as it is the direct dependency.
    var path = dependencyResolver.resolveOne(artifact, DependencyResolutionDepth.DIRECT)
        .iterator()
        .next();

    return Optional.of(path);
  }
}
