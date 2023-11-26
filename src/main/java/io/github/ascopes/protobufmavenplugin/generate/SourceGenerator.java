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

import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;

import io.github.ascopes.protobufmavenplugin.execute.ProtocArgumentBuilder;
import io.github.ascopes.protobufmavenplugin.execute.ProtocExecutionException;
import io.github.ascopes.protobufmavenplugin.execute.ProtocExecutor;
import io.github.ascopes.protobufmavenplugin.resolve.ExecutableResolutionException;
import io.github.ascopes.protobufmavenplugin.resolve.protoc.MavenProtocResolver;
import io.github.ascopes.protobufmavenplugin.resolve.protoc.PathProtocResolver;
import io.github.ascopes.protobufmavenplugin.resolve.source.ProtoSourceResolver;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolver;

/**
 * Main code generator.
 *
 * <p>This orchestrates all the other components and settings that are provided in order to
 * produce the final results.
 *
 * @author Ashley Scopes
 */
public final class SourceGenerator {

  private final ArtifactResolver artifactResolver;
  private final MavenSession mavenSession;
  private final String protocVersion;
  private final Set<Path> sourceDirectories;
  private final Path outputDirectory;
  private final boolean fatalWarnings;
  private final boolean generateKotlinWrappers;
  private final boolean liteOnly;
  private final SourceRootRegistrar sourceRootRegistrar;

  SourceGenerator(SourceGeneratorBuilder builder) {
    artifactResolver = requireNonNull(builder.artifactResolver);
    mavenSession = requireNonNull(builder.mavenSession);
    protocVersion = requireNonNull(builder.protocVersion);
    sourceDirectories = requireNonNull(builder.sourceDirectories);
    outputDirectory = requireNonNull(builder.outputDirectory);
    fatalWarnings = requireNonNull(builder.fatalWarnings);
    generateKotlinWrappers = requireNonNull(builder.generateKotlinWrappers);
    liteOnly = requireNonNull(builder.liteOnly);
    sourceRootRegistrar = requireNonNull(builder.sourceRootRegistrar);
  }

  /**
   * Generate the sources.
   *
   * @throws MojoExecutionException if a user error occurs.
   * @throws MojoFailureException if a plugin error occurs.
   */
  public void generate() throws MojoExecutionException, MojoFailureException {
    // Step 1. Resolve everything we need to compile correctly.
    var protocPath = resolveProtocPath();
    var sources = resolveProtoSources();

    // Step 2. Prepare the output environment.
    registerSourceOutputRoots();

    // Step 3. Compile.
    dumpProtocVersion(protocPath);
    compileProtobufSources(protocPath, sources);
  }

  Path resolveProtocPath() throws MojoFailureException {
    var resolver = protocVersion.trim().equalsIgnoreCase("PATH")
        ? new PathProtocResolver()
        : new MavenProtocResolver(protocVersion, artifactResolver, mavenSession);

    try {
      return resolver.resolve();
    } catch (ExecutableResolutionException ex) {
      throw failure("Failed to resolve protoc executable", ex);
    }
  }

  private List<Path> resolveProtoSources() throws MojoFailureException {
    try {
      return ProtoSourceResolver.resolve(sourceDirectories);
    } catch (IOException ex) {
      throw failure("Failed to resolve protobuf sources", ex);
    }
  }

  void registerSourceOutputRoots() throws MojoFailureException {
    // Create the root first if it does not yet exist.
    try {
      Files.createDirectories(outputDirectory);
      sourceRootRegistrar.register(mavenSession.getCurrentProject(), outputDirectory);
    } catch (IOException ex) {
      throw failure("Failed to register source output root", ex);
    }
  }

  void dumpProtocVersion(Path protocPath) throws MojoFailureException {
    try {
      var args = new ProtocArgumentBuilder(protocPath).version();
      ProtocExecutor.invoke(args);
    } catch (ProtocExecutionException ex) {
      throw failure("Failed to determine protoc version", ex);
    }
  }

  void compileProtobufSources(
      Path protocPath,
      Collection<Path> sources
  ) throws MojoExecutionException {
    var argBuilder = new ProtocArgumentBuilder(protocPath)
        .includeDirectories(sourceDirectories)
        .fatalWarnings(fatalWarnings)
        .outputDirectory("java", outputDirectory, liteOnly);

    if (generateKotlinWrappers) {
      argBuilder.outputDirectory("kotlin", outputDirectory, liteOnly);
    }

    try {
      ProtocExecutor.invoke(argBuilder.build(sources));
    } catch (ProtocExecutionException ex) {
      throw error("Compilation of protobuf sources failed", ex);
    }
  }

  @SuppressWarnings("SameParameterValue")
  private MojoExecutionException error(String shortMessage, Exception ex) {
    var newEx = new MojoExecutionException(this, shortMessage, getLongMessage(shortMessage, ex));
    newEx.initCause(ex);
    return newEx;
  }

  private MojoFailureException failure(String shortMessage, Exception ex) {
    var newEx = new MojoFailureException(this, shortMessage, getLongMessage(shortMessage, ex));
    newEx.initCause(ex);
    return newEx;
  }

  private String getLongMessage(String shortMessage, Exception ex) {
    return ofNullable(ex)
        .map(Exception::getMessage)
        .orElse(shortMessage);
  }
}
