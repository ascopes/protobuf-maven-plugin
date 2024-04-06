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

import io.github.ascopes.protobufmavenplugin.DependencyResolutionDepth;
import io.github.ascopes.protobufmavenplugin.MavenArtifact;
import io.github.ascopes.protobufmavenplugin.dependency.MavenDependencyPathResolver;
import io.github.ascopes.protobufmavenplugin.dependency.PlatformClassifierFactory;
import io.github.ascopes.protobufmavenplugin.dependency.ResolutionException;
import io.github.ascopes.protobufmavenplugin.dependency.SystemPathBinaryResolver;
import io.github.ascopes.protobufmavenplugin.dependency.UrlResourceFetcher;
import io.github.ascopes.protobufmavenplugin.platform.FileUtils;
import io.github.ascopes.protobufmavenplugin.platform.HostSystem;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.execution.MavenSession;
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

  public Path resolve(
      MavenSession session,
      String version
  ) throws ResolutionException {
    if (version.equalsIgnoreCase("PATH")) {
      return systemPathResolver.resolve(EXECUTABLE_NAME)
          .orElseThrow(() -> new ResolutionException("No protoc executable was found"));
    }

    var path = version.contains(":")
        ? resolveFromUrl(version)
        : resolveFromMavenRepositories(session, version);

    try {
      FileUtils.makeExecutable(path);
    } catch (IOException ex) {
      throw new ResolutionException("Failed to set executable bit on protoc binary", ex);
    }

    return path;
  }

  private Path resolveFromUrl(String url) throws ResolutionException {
    try {
      return urlResourceFetcher.fetchFileFromUrl(new URL(url), ".exe");
    } catch (IOException ex) {
      throw new ResolutionException(ex.getMessage(), ex);
    }
  }

  private Path resolveFromMavenRepositories(
      MavenSession session,
      String version
  ) throws ResolutionException {
    if (hostSystem.isProbablyAndroidTermux()) {
      log.warn(
          "It looks like you are using Termux on Android. You may encounter issues "
              + "running the detected protoc binary from Maven central. If this is "
              + "an issue, install the protoc compiler manually from your package "
              + "manager (apt update && apt install protobuf), and then invoke "
              + "Maven with the -Dprotoc.version=PATH flag."
      );
    }

    var artifact = new MavenArtifact();
    artifact.setGroupId(GROUP_ID);
    artifact.setArtifactId(ARTIFACT_ID);
    artifact.setVersion(version);
    artifact.setType("exe");
    artifact.setClassifier(platformClassifierFactory.getClassifier(ARTIFACT_ID));

    return dependencyResolver.resolveOne(session, artifact, DependencyResolutionDepth.DIRECT)
        .iterator()
        .next();
  }
}
