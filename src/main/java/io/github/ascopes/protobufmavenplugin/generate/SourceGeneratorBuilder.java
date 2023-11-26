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
package io.github.ascopes.protobufmavenplugin.generate;

import io.github.ascopes.protobufmavenplugin.AbstractGenerateMojo;
import java.nio.file.Path;
import java.util.Set;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolver;
import org.jspecify.annotations.Nullable;

/**
 * Builder class for a source generator.
 *
 * <p>This is kept separate from the {@link AbstractGenerateMojo} to decouple the management of
 * Plexus container resources from the generation of source code. This adds somewhat significant
 * amounts of boilerplate but keeps the implementation easier to test without falling back to flaky
 * integration testing internally.
 *
 * @author Ashley Scopes
 */
public final class SourceGeneratorBuilder {

  @Nullable ArtifactResolver artifactResolver;
  @Nullable MavenSession mavenSession;
  @Nullable String protocVersion;
  @Nullable String grpcPluginVersion;
  @Nullable Set<Path> sourceDirectories;
  @Nullable Path protobufOutputDirectory;
  @Nullable Path grpcOutputDirectory;
  @Nullable Boolean fatalWarnings;
  @Nullable Boolean generateKotlinWrappers;
  @Nullable Boolean liteOnly;
  @Nullable SourceRootRegistrar sourceRootRegistrar;

  /**
   * Initialise this builder.
   */
  public SourceGeneratorBuilder() {
    // Nothing to do here.
  }

  /**
   * Set the artifact resolver.
   *
   * @param artifactResolver the artifact resolver.
   * @return this builder.
   */
  public SourceGeneratorBuilder artifactResolver(ArtifactResolver artifactResolver) {
    this.artifactResolver = artifactResolver;
    return this;
  }

  /**
   * Set the Maven session.
   *
   * @param mavenSession the Maven session.
   * @return this builder.
   */
  public SourceGeneratorBuilder mavenSession(MavenSession mavenSession) {
    this.mavenSession = mavenSession;
    return this;
  }

  /**
   * Set the {@code protoc} version.
   *
   * @param protocVersion the {@code protoc} version.
   * @return this builder.
   */
  public SourceGeneratorBuilder protocVersion(String protocVersion) {
    this.protocVersion = protocVersion;
    return this;
  }

  /**
   * Set the GRPC plugin version.
   *
   * @param grpcPluginVersion the GRPC plugin version, or {@code null} to disable.
   * @return this builder.
   */
  public SourceGeneratorBuilder grpcPluginVersion(@Nullable String grpcPluginVersion) {
    this.grpcPluginVersion = grpcPluginVersion;
    return this;
  }

  /**
   * Set the source directories to compile from.
   *
   * @param sourceDirectories the source directories to compile.
   * @return this builder.
   */
  public SourceGeneratorBuilder sourceDirectories(Set<Path> sourceDirectories) {
    this.sourceDirectories = sourceDirectories;
    return this;
  }

  /**
   * Set the directory to output generated protobuf message sources to.
   *
   * @param protobufOutputDirectory the directory to output generated protobuf message sources to.
   * @return this builder.
   */
  public SourceGeneratorBuilder protobufOutputDirectory(Path protobufOutputDirectory) {
    this.protobufOutputDirectory = protobufOutputDirectory;
    return this;
  }

  /**
   * Set the directory to output generated GRPC service sources to.
   *
   * @param grpcOutputDirectory the directory to output generated GRPC service sources to.
   * @return this builder.
   */
  public SourceGeneratorBuilder grpcOutputDirectory(Path grpcOutputDirectory) {
    this.grpcOutputDirectory = grpcOutputDirectory;
    return this;
  }

  /**
   * Set the fatal warning preference.
   *
   * @param fatalWarnings the fatal warning preference.
   * @return this builder.
   */
  public SourceGeneratorBuilder fatalWarnings(boolean fatalWarnings) {
    this.fatalWarnings = fatalWarnings;
    return this;
  }

  /**
   * Set the Kotlin wrapper generation preference.
   *
   * @param generateKotlinWrappers the Kotlin wrapper generation preference.
   * @return this builder.
   */
  public SourceGeneratorBuilder generateKotlinWrappers(boolean generateKotlinWrappers) {
    this.generateKotlinWrappers = generateKotlinWrappers;
    return this;
  }

  /**
   * Set the "lite"-only generation preference.
   *
   * @param liteOnly the "lite"-only generation preference.
   * @return this builder.
   */
  public SourceGeneratorBuilder liteOnly(boolean liteOnly) {
    this.liteOnly = liteOnly;
    return this;
  }

  /**
   * Set the source root registrar to use.
   *
   * @param sourceRootRegistrar the source root registrar.
   * @return this builder.
   */
  public SourceGeneratorBuilder sourceRootRegistrar(SourceRootRegistrar sourceRootRegistrar) {
    this.sourceRootRegistrar = sourceRootRegistrar;
    return this;
  }

  /**
   * Build the source generator.
   *
   * @return the source generator.
   */
  public SourceGenerator build() {
    return new SourceGenerator(this);
  }
}
