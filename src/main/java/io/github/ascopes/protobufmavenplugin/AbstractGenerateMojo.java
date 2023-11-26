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

package io.github.ascopes.protobufmavenplugin;

import io.github.ascopes.protobufmavenplugin.generate.SourceGeneratorBuilder;
import io.github.ascopes.protobufmavenplugin.generate.SourceRootRegistrar;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolver;
import org.jspecify.annotations.Nullable;

/**
 * Base Mojo to generate protobuf sources.
 *
 * <p>Can be extended for each language that this plugin supports.
 *
 * @author Ashley Scopes
 * @since 0.0.1
 */
public abstract class AbstractGenerateMojo extends AbstractMojo {

  /**
   * The artifact resolver to use to resolve dependencies from Maven repositories.
   *
   * @since 0.0.1
   */
  @Component
  private @Nullable ArtifactResolver artifactResolver;

  /**
   * The Maven session that is in use.
   *
   * @since 0.0.1
   */
  @Parameter(defaultValue = "${session}", required = true, readonly = true)
  private MavenSession mavenSession;

  /**
   * The version of protoc to use.
   *
   * <p>This should correspond to the version of {@code protobuf-java} or similar that is in
   * use.
   *
   * <p>The value can be a static version, or a valid Maven version range (such as
   * "{@code [3.5.0,4.0.0)}"). It is recommended to use a static version to ensure your builds are
   * reproducible.
   *
   * <p>If set to "{@code PATH}", then {@code protoc} is resolved from the system path rather than
   * being downloaded. This is useful if you need to use an unsupported architecture/OS, or a
   * development version of {@code protoc}.
   *
   * @since 0.0.1
   */
  @Parameter(required = true, property = "protoc.version")
  private String protocVersion;

  /**
   * The version of the GRPC plugin to use.
   *
   * <p>This should correspond to the version of {@code grpc-stubs} or similar that is in
   * use.
   *
   * <p>The value can be a static version, or a valid Maven version range (such as
   * "{@code [1.58.0,2.0.0)}"). It is recommended to use a static version to ensure your builds are
   * reproducible.
   *
   * <p>If set to "{@code PATH}", then the codegen plugins are resolved from the system path
   * rather than being downloaded. This is useful if you need to use an unsupported architecture/OS,
   * or a development version of the plugins.
   *
   * <p>If you do not need GRPC support, leaving this value unspecified or explicitly null will
   * disable the GRPC feature.
   *
   * @since 0.0.1
   */
  @Parameter(property = "grpc-plugin.version")
  private @Nullable String grpcPluginVersion;

  /**
   * Override the source directories to compile from.
   *
   * <p>Leave unspecified or explicitly null/empty to use the defaults.
   *
   * @since 0.0.1
   */
  @Parameter
  private @Nullable Set<String> sourceDirectories;

  /**
   * Override the directory to output generated protobuf message sources to.
   *
   * <p>Leave unspecified or explicitly null to use the defaults.
   *
   * @since 0.0.1
   */
  @Parameter
  private @Nullable String protobufOutputDirectory;

  /**
   * Override the directory to output generated GRPC service sources to.
   *
   * <p>Leave unspecified or explicitly null to use the defaults.
   *
   * @since 0.0.1
   */
  @Parameter
  private @Nullable String grpcOutputDirectory;

  /**
   * Whether to treat {@code protoc} compiler warnings as errors.
   *
   * @since 0.0.1
   */
  @Parameter(defaultValue = "false")
  private boolean fatalWarnings;

  /**
   * Whether to also generate Kotlin API wrapper code around the generated Java code.
   *
   * @since 0.0.1
   */
  @Parameter(defaultValue = "false")
  private boolean generateKotlinWrappers;

  /**
   * Whether to only generate "lite" messages or not.
   *
   * <p>These are bare-bones sources that do not contain most of the metadata that regular
   * Protobuf sources contain, and are designed for low-latency/low-overhead scenarios.
   *
   * <p>See the protobuf documentation for the pros and cons of this.
   *
   * @since 0.0.1
   */
  @Parameter(defaultValue = "false")
  private boolean liteOnly;

  /**
   * Initialise this Mojo.
   */
  protected AbstractGenerateMojo() {
    // Expect all fields to be initialised later by Plexus.
  }

  /**
   * Execute this goal.
   *
   * @throws MojoExecutionException if a user/configuration error is encountered.
   * @throws MojoFailureException   if execution fails due to an internal error.
   */
  @Override
  public final void execute() throws MojoExecutionException, MojoFailureException {
    new SourceGeneratorBuilder()
        .artifactResolver(artifactResolver)
        .fatalWarnings(fatalWarnings)
        .generateKotlinWrappers(generateKotlinWrappers)
        .grpcOutputDirectory(getActualGrpcOutputDirectory())
        .grpcPluginVersion(grpcPluginVersion)
        .liteOnly(liteOnly)
        .mavenSession(mavenSession)
        .protobufOutputDirectory(getActualProtobufOutputDirectory())
        .protocVersion(protocVersion)
        .sourceDirectories(getActualSourceDirectories())
        .sourceRootRegistrar(getSourceRootRegistrar())
        .build()
        .generate();
  }

  /**
   * Get the default source directory to use if none are specified.
   *
   * @param baseDir the project base directory.
   * @return the default source directory.
   */
  protected abstract Path getDefaultSourceDirectory(Path baseDir);

  /**
   * Get the default protobuf output directory to use if none are specified.
   *
   * @param targetDir the project target directory.
   * @return the default protobuf output directory.
   */
  protected abstract Path getDefaultProtobufOutputDirectory(Path targetDir);

  /**
   * Get the default GRPC output directory to use if none are specified.
   *
   * @param targetDir the project target directory.
   * @return the default GRPC output directory.
   */
  protected abstract Path getDefaultGrpcOutputDirectory(Path targetDir);

  /**
   * Get the source root registrar to use.
   *
   * @return the source root registrar to use.
   */
  protected abstract SourceRootRegistrar getSourceRootRegistrar();

  private Set<Path> getActualSourceDirectories() {
    if (sourceDirectories == null || sourceDirectories.isEmpty()) {
      var baseDir = mavenSession.getCurrentProject().getBasedir().toPath();
      return Set.of(getDefaultSourceDirectory(baseDir));
    }

    return sourceDirectories
        .stream()
        .map(Path::of)
        .collect(Collectors.toSet());
  }

  private Path getActualProtobufOutputDirectory() {
    if (protobufOutputDirectory == null || protobufOutputDirectory.isBlank()) {
      var targetDir = Path.of(mavenSession.getCurrentProject().getBuild().getDirectory());
      return getDefaultProtobufOutputDirectory(targetDir);
    }

    return Path.of(protobufOutputDirectory);
  }

  private Path getActualGrpcOutputDirectory() {
    if (grpcOutputDirectory == null || grpcOutputDirectory.isBlank()) {
      var targetDir = Path.of(mavenSession.getCurrentProject().getBuild().getDirectory());
      return getDefaultGrpcOutputDirectory(targetDir);
    }

    return Path.of(grpcOutputDirectory);
  }
}
