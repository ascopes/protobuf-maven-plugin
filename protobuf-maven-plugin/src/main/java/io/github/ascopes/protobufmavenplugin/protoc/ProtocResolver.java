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
package io.github.ascopes.protobufmavenplugin.protoc;

import io.github.ascopes.protobufmavenplugin.dependencies.ImmutableMavenDependency;
import io.github.ascopes.protobufmavenplugin.dependencies.MavenArtifactPathResolver;
import io.github.ascopes.protobufmavenplugin.dependencies.PlatformClassifierFactory;
import io.github.ascopes.protobufmavenplugin.fs.FileUtils;
import io.github.ascopes.protobufmavenplugin.fs.UriResourceFetcher;
import io.github.ascopes.protobufmavenplugin.utils.HostSystem;
import io.github.ascopes.protobufmavenplugin.utils.ResolutionException;
import io.github.ascopes.protobufmavenplugin.utils.SystemPathBinaryResolver;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Optional;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.execution.scope.MojoExecutionScoped;
import org.eclipse.sisu.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Resolver for the {@code protoc} executable.
 *
 * @author Ashley Scopes
 */
@Description("Finds or downloads the required version of protoc from various locations")
@MojoExecutionScoped
@Named
public final class ProtocResolver {

  private static final String EXECUTABLE_NAME = "protoc";
  private static final String GROUP_ID = "com.google.protobuf";
  private static final String ARTIFACT_ID = "protoc";
  private static final String TYPE = "exe";

  private static final Logger log = LoggerFactory.getLogger(ProtocResolver.class);

  private final HostSystem hostSystem;
  private final MavenArtifactPathResolver artifactPathResolver;
  private final PlatformClassifierFactory platformClassifierFactory;
  private final SystemPathBinaryResolver systemPathResolver;
  private final UriResourceFetcher urlResourceFetcher;

  @Inject
  public ProtocResolver(
      HostSystem hostSystem,
      MavenArtifactPathResolver artifactPathResolver,
      PlatformClassifierFactory platformClassifierFactory,
      SystemPathBinaryResolver systemPathResolver,
      UriResourceFetcher urlResourceFetcher
  ) {
    this.hostSystem = hostSystem;
    this.artifactPathResolver = artifactPathResolver;
    this.platformClassifierFactory = platformClassifierFactory;
    this.systemPathResolver = systemPathResolver;
    this.urlResourceFetcher = urlResourceFetcher;
  }

  public Optional<Path> resolve(String version) throws ResolutionException {
    if (version.equalsIgnoreCase("LATEST")) {
      log.warn(
          "You have set the protoc version to 'latest'. This will likely not behave as you "
              + "would expect, since Google have released incorrect version numbers of protoc "
              + "in the past. To remove this warning, please use a pinned version instead."
      );
    }

    if (version.equalsIgnoreCase("PATH")) {
      return systemPathResolver.resolve(EXECUTABLE_NAME);
    }

    // It is likely a URL, not a version string.
    var path = version.contains(":")
        ? resolveFromUri(version)
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

  private Optional<Path> resolveFromUri(String uriString) throws ResolutionException {
    try {
      var uri = new URI(uriString);
      return urlResourceFetcher.fetchFileFromUri(uri, ".exe");
    } catch (URISyntaxException ex) {
      throw new ResolutionException("Failed to parse URI '" + uriString + "'", ex);
    }
  }

  private Optional<Path> resolveFromMavenRepositories(String version) throws ResolutionException {
    if (hostSystem.isProbablyTermux()) {
      log.warn(
          "It looks like you are using Termux! If you are using an environment such as Termux, "
              + "then you may find that the Maven-distributed versions of protoc fail to run. "
              + "This is due to Android's kernel restricting the types of system calls that can "
              + "be made. You may wish to run 'pkg in protobuf' to install a modified version of "
              + "protoc, and reinvoke Maven with '-Dprotobuf.compiler.version=PATH' to force "
              + "this Maven plugin to use a compatible version. Also ensure you have the latest "
              + "JDK installed in this case. If you do not encounter any issues, then great! You "
              + "can safely ignore this warning."
      );
    }

    var artifact = ImmutableMavenDependency.builder()
        .groupId(GROUP_ID)
        .artifactId(ARTIFACT_ID)
        .version(version)
        .type(TYPE)
        .classifier(platformClassifierFactory.getClassifier(ARTIFACT_ID))
        .build();

    return Optional.of(artifactPathResolver.resolveArtifact(artifact));
  }
}
