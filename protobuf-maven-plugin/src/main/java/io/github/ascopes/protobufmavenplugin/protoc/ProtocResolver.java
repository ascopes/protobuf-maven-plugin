/*
 * Copyright (C) 2023 Ashley Scopes
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
import io.github.ascopes.protobufmavenplugin.protoc.distributions.BinaryMavenProtocDistribution;
import io.github.ascopes.protobufmavenplugin.protoc.distributions.ImmutableBinaryMavenProtocDistribution;
import io.github.ascopes.protobufmavenplugin.protoc.distributions.JvmMavenProtocDistribution;
import io.github.ascopes.protobufmavenplugin.protoc.distributions.PathProtocDistribution;
import io.github.ascopes.protobufmavenplugin.protoc.distributions.ProtocDistribution;
import io.github.ascopes.protobufmavenplugin.protoc.distributions.UriProtocDistribution;
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

/**
 * Resolver for the {@code protoc} executable.
 *
 * @author Ashley Scopes
 */
@Description("Finds or downloads the required version of protoc from various locations")
@MojoExecutionScoped
@Named
public final class ProtocResolver {

  private final MavenArtifactPathResolver artifactPathResolver;
  private final PlatformClassifierFactory platformClassifierFactory;
  private final JavaAppToExecutableFactory javaAppToExecutableFactory;
  private final SystemPathBinaryResolver systemPathResolver;
  private final UriResourceFetcher urlResourceFetcher;

  @Inject
  public ProtocResolver(
      MavenArtifactPathResolver artifactPathResolver,
      PlatformClassifierFactory platformClassifierFactory,
      JavaAppToExecutableFactory javaAppToExecutableFactory,
      SystemPathBinaryResolver systemPathResolver,
      UriResourceFetcher urlResourceFetcher
  ) {
    this.artifactPathResolver = artifactPathResolver;
    this.platformClassifierFactory = platformClassifierFactory;
    this.javaAppToExecutableFactory = javaAppToExecutableFactory;
    this.systemPathResolver = systemPathResolver;
    this.urlResourceFetcher = urlResourceFetcher;
  }

  public Optional<Path> resolve(
      ProtocDistribution distribution,
      @Nullable Digest digest
  ) throws ResolutionException {
    Optional<Path> path;

    if (distribution instanceof BinaryMavenProtocDistribution impl) {
      path = resolveBinaryMavenDistribution(impl);
    } else if (distribution instanceof JvmMavenProtocDistribution impl) {
      path = resolveJvmMavenProtocDistribution(impl);
    } else if (distribution instanceof PathProtocDistribution impl) {
      path = resolvePathDistribution(impl);
    } else if (distribution instanceof UriProtocDistribution impl) {
      path = resolveUriDistribution(impl);
    } else {
      throw new UnsupportedOperationException("unsupported distribution " + distribution);
    }

    if (path.isEmpty()) {
      return Optional.empty();
    }

    var resolvedPath = path.get();

    if (digest != null) {
      try (var is = new BufferedInputStream(Files.newInputStream(resolvedPath))) {
        digest.verify(is);
      } catch (IOException ex) {
        throw new ResolutionException(
            "Failed to compute digest of \"" + resolvedPath + "\": " + ex,
            ex
        );
      }
    }

    return path;
  }

  private Optional<Path> resolveBinaryMavenDistribution(
      BinaryMavenProtocDistribution distribution
  ) throws ResolutionException {
    if (distribution.getClassifier() == null) {
      var classifier = platformClassifierFactory.getClassifier(distribution.getArtifactId());
      distribution = ImmutableBinaryMavenProtocDistribution.builder()
          .from(distribution)
          .classifier(classifier)
          .build();
    }

    return Optional.of(artifactPathResolver.resolveArtifact(distribution));
  }

  private Optional<Path> resolveJvmMavenProtocDistribution(
      JvmMavenProtocDistribution distribution
  ) throws ResolutionException {
    var dependencies = artifactPathResolver
        .resolveDependencies(
            List.of(distribution),
            DependencyResolutionDepth.TRANSITIVE,
            Set.of("compile", "runtime", "system"),
            false
        )
        .stream()
        .toList();

    var javaApp = ImmutableJavaApp.builder()
        .dependencies(dependencies)
        .mainClass(distribution.getMainClass())
        .jvmArgs(distribution.getJvmArgs())
        .jvmConfigArgs(distribution.getJvmConfigArgs())
        .uniqueName("protoc")
        .build();

    return Optional.of(javaAppToExecutableFactory.toExecutable(javaApp));
  }

  private Optional<Path> resolveUriDistribution(
      UriProtocDistribution distribution
  ) throws ResolutionException {
    return urlResourceFetcher.fetchFileFromUri(distribution.getUrl(), ".exe", true);
  }

  private Optional<Path> resolvePathDistribution(
      PathProtocDistribution distribution
  ) throws ResolutionException {
    return systemPathResolver.resolve(distribution.getName());
  }
}
