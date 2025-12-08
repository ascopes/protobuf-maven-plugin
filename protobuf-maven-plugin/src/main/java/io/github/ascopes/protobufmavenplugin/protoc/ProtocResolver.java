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

import io.github.ascopes.protobufmavenplugin.dependencies.DependencyResolutionDepth;
import io.github.ascopes.protobufmavenplugin.dependencies.MavenArtifactPathResolver;
import io.github.ascopes.protobufmavenplugin.dependencies.PlatformClassifierFactory;
import io.github.ascopes.protobufmavenplugin.digests.Digest;
import io.github.ascopes.protobufmavenplugin.java.ImmutableJavaApp;
import io.github.ascopes.protobufmavenplugin.java.JavaAppToExecutableFactory;
import io.github.ascopes.protobufmavenplugin.protoc.dists.BinaryMavenProtocDistribution;
import io.github.ascopes.protobufmavenplugin.protoc.dists.BinaryMavenProtocDistributionBean;
import io.github.ascopes.protobufmavenplugin.protoc.dists.JvmMavenProtocDistribution;
import io.github.ascopes.protobufmavenplugin.protoc.dists.PathProtocDistribution;
import io.github.ascopes.protobufmavenplugin.protoc.dists.ProtocDistribution;
import io.github.ascopes.protobufmavenplugin.protoc.dists.UriProtocDistribution;
import io.github.ascopes.protobufmavenplugin.urls.UriResourceFetcher;
import io.github.ascopes.protobufmavenplugin.utils.ResolutionException;
import io.github.ascopes.protobufmavenplugin.utils.SystemPathBinaryResolver;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.execution.scope.MojoExecutionScoped;
import org.eclipse.sisu.Description;
import org.jspecify.annotations.Nullable;
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

  private static final Logger log = LoggerFactory.getLogger(ProtocResolver.class);

  private final MavenArtifactPathResolver artifactPathResolver;
  private final JavaAppToExecutableFactory javaAppToExecutableFactory;
  private final PlatformClassifierFactory platformClassifierFactory;
  private final SystemPathBinaryResolver systemPathResolver;
  private final UriResourceFetcher urlResourceFetcher;

  @Inject
  public ProtocResolver(
      MavenArtifactPathResolver artifactPathResolver,
      JavaAppToExecutableFactory javaAppToExecutableFactory,
      PlatformClassifierFactory platformClassifierFactory,
      SystemPathBinaryResolver systemPathResolver,
      UriResourceFetcher urlResourceFetcher
  ) {
    this.artifactPathResolver = artifactPathResolver;
    this.javaAppToExecutableFactory = javaAppToExecutableFactory;
    this.platformClassifierFactory = platformClassifierFactory;
    this.systemPathResolver = systemPathResolver;
    this.urlResourceFetcher = urlResourceFetcher;
  }

  public Path resolveProtoc(
      ProtocDistribution distribution,
      @Nullable Digest deprecatedGlobalDigest
  ) throws ResolutionException {
    if (distribution instanceof BinaryMavenProtocDistribution distributionImpl) {
      return resolveBinaryMavenProtoc(distributionImpl);
    } else if (distribution instanceof PathProtocDistribution distributionImpl) {
      return resolvePathProtoc(distributionImpl, deprecatedGlobalDigest);
    } else if (distribution instanceof UriProtocDistribution distributionImpl) {
      return resolveUriProtoc(distributionImpl, deprecatedGlobalDigest);
    } else if (distribution instanceof JvmMavenProtocDistribution distributionImpl) {
      return resolveJvmMavenProtoc(distributionImpl);
    } else {
      // Unreachable, but needed until we use a Java version with pattern matching
      // for types.
      throw new UnsupportedOperationException();
    }
  }

  private Path resolveBinaryMavenProtoc(
      BinaryMavenProtocDistribution distribution
  ) throws ResolutionException {
    if (distribution.getClassifier() == null) {
      var newDistribution = new BinaryMavenProtocDistributionBean()
          .from(distribution);
      newDistribution.setClassifier(platformClassifierFactory.getClassifier("protoc"));
      distribution = newDistribution;
    }

    log.info("Building using binary protoc distribution from Maven (\"{}\")", distribution);

    return artifactPathResolver.resolveExecutable(distribution);
  }

  private Path resolvePathProtoc(
      PathProtocDistribution distribution,
      @Nullable Digest deprecatedGlobalDigest
  ) throws ResolutionException {
    var path = systemPathResolver.resolve(distribution.getName())
        .orElseThrow(() -> new ResolutionException(
            "No protoc binary named '" + distribution.getName() + "' found on the system path"
        ));
    verifyDigest(path, distribution.getDigest(), deprecatedGlobalDigest);

    log.info("Building using binary protoc distribution from system path (\"{}\")", path);

    return path;
  }

  private Path resolveUriProtoc(
      UriProtocDistribution distribution,
      @Nullable Digest deprecatedGlobalDigest
  ) throws ResolutionException {
    var path = urlResourceFetcher
        .fetchFileFromUri(distribution.getUrl(), "exe", true)
        .orElseThrow(() -> new ResolutionException(
            "No protoc binary found at '" + distribution.getUrl() + "'"
        ));
    verifyDigest(path, distribution.getDigest(), deprecatedGlobalDigest);

    log.info("Building using binary protoc distribution from URL (\"{}\")", distribution.getUrl());

    return path;
  }

  private Path resolveJvmMavenProtoc(
      JvmMavenProtocDistribution distribution
  ) throws ResolutionException {
    log.info("Building using pure Java protoc distribution from Maven (\"{}\")", distribution);

    try {
      var dependencies = artifactPathResolver
          .resolveDependencies(
              List.of(distribution),
              DependencyResolutionDepth.TRANSITIVE,
              Set.of("compile", "runtime", "system"),
              false
          )
          .stream()
          .toList();

      var app = ImmutableJavaApp.builder()
          .addAllDependencies(dependencies)
          .jvmArgs(distribution.getJvmArgs())
          .jvmConfigArgs(distribution.getJvmConfigArgs())
          .mainClass(distribution.getMainClass())
          .uniqueName("protoc")
          .build();

      return javaAppToExecutableFactory.toExecutable(app);
    } catch (ResolutionException ex) {
      throw new ResolutionException("Failed to resolve protoc " + distribution + ": " + ex, ex);
    }
  }

  private void verifyDigest(
      Path path,
      @Nullable Digest distributionDigest,
      @Nullable Digest deprecatedGlobalDigest
  ) throws ResolutionException {
    // XXX: remove handling for digest parameter outside PathProtocDistribution object in v5.0.0
    var digest = Optional.ofNullable(distributionDigest)
        .orElse(deprecatedGlobalDigest);

    if (digest != null) {
      log.debug("Verifying digest of \"{}\" against \"{}\"", path, digest);
      try (var is = new BufferedInputStream(Files.newInputStream(path))) {
        digest.verify(is);
      } catch (IOException ex) {
        throw new ResolutionException(
            "Failed to compute digest of \"" + path + "\": " + ex,
            ex
        );
      }
    }
  }
}
