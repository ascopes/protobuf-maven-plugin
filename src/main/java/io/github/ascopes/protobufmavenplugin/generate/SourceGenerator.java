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
import io.github.ascopes.protobufmavenplugin.resolve.Executable;
import io.github.ascopes.protobufmavenplugin.resolve.ExecutableResolutionException;
import io.github.ascopes.protobufmavenplugin.resolve.MavenArtifactResolver;
import io.github.ascopes.protobufmavenplugin.resolve.PathExecutableResolver;
import io.github.ascopes.protobufmavenplugin.resolve.ProtoSourceResolver;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolver;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main code generator.
 *
 * <p>This orchestrates all the other components and settings that are provided in order to
 * produce the final results.
 *
 * @author Ashley Scopes
 */
public final class SourceGenerator {

  private static final Logger LOGGER = LoggerFactory.getLogger(SourceGenerator.class);

  private final MavenSession mavenSession;
  private final String protocVersion;
  private final @Nullable String grpcPluginVersion;
  private final Set<Path> sourceDirectories;
  private final Path protobufOutputDirectory;
  private final Path grpcOutputDirectory;
  private final boolean fatalWarnings;
  private final boolean generateKotlinWrappers;
  private final boolean liteOnly;
  private final SourceRootRegistrar sourceRootRegistrar;

  // Internally managed components.
  private final MavenArtifactResolver mavenArtifactResolver;
  private final PathExecutableResolver pathExecutableResolver;

  SourceGenerator(SourceGeneratorBuilder builder) {
    var artifactResolver = requireNonNull(builder.artifactResolver);

    mavenSession = requireNonNull(builder.mavenSession);
    protocVersion = requireNonNull(builder.protocVersion);
    grpcPluginVersion = builder.grpcPluginVersion;
    sourceDirectories = requireNonNull(builder.sourceDirectories);
    protobufOutputDirectory = requireNonNull(builder.protobufOutputDirectory);
    grpcOutputDirectory = requireNonNull(builder.grpcOutputDirectory);
    fatalWarnings = requireNonNull(builder.fatalWarnings);
    generateKotlinWrappers = requireNonNull(builder.generateKotlinWrappers);
    liteOnly = requireNonNull(builder.liteOnly);
    sourceRootRegistrar = requireNonNull(builder.sourceRootRegistrar);

    mavenArtifactResolver = new MavenArtifactResolver(artifactResolver, mavenSession);
    pathExecutableResolver = new PathExecutableResolver();
  }

  /**
   * Generate the sources.
   *
   * @throws MojoExecutionException if a user error occurs.
   * @throws MojoFailureException   if a plugin error occurs.
   */
  public void generate() throws MojoExecutionException, MojoFailureException {
    LOGGER.debug("Beginning generation pass");

    // Step 1. Resolve everything we need to compile correctly.
    var protocPath = resolvePath(Executable.PROTOC, protocVersion);
    var pluginPaths = resolvePlugins();
    var sources = resolveProtoSources();

    // Step 2. Prepare the output environment.
    registerSourceOutputRoots();

    // Step 3. Log the protoc version being used for reference.
    dumpProtocVersion(protocPath);

    // Step 4. Perform the compilation.
    generateSources(protocPath, pluginPaths, sources);
  }

  Path resolvePath(Executable executable, String version) throws MojoFailureException {
    try {
      if (version.trim().equalsIgnoreCase("PATH")) {
        return pathExecutableResolver.resolve(executable);
      } else {
        return mavenArtifactResolver.resolve(executable, version);
      }
    } catch (ExecutableResolutionException ex) {
      throw failure("Failed to resolve protoc executable", ex);
    }
  }

  Collection<Plugin> resolvePlugins() throws MojoFailureException {
    if (grpcPluginVersion == null) {
      return List.of();
    }

    var resolvedPlugins = new ArrayList<Plugin>();
    resolvedPlugins.add(resolvePlugin(Executable.PROTOC_GEN_GRPC_JAVA, grpcPluginVersion));
    
    if (generateKotlinWrappers) {
      resolvedPlugins.add(resolvePlugin(Executable.PROTOC_GEN_GRPC_KOTLIN, grpcPluginVersion));
    }

    return resolvedPlugins;
  }

  private Plugin resolvePlugin(Executable executable, String version) throws MojoFailureException {
    var path = resolvePath(executable, version);
    return new Plugin(executable.getExecutableName(), path);
  }

  private List<Path> resolveProtoSources() throws MojoFailureException {
    try {
      return ProtoSourceResolver.resolve(sourceDirectories);
    } catch (IOException ex) {
      throw failure("Failed to resolve protobuf sources", ex);
    }
  }

  void registerSourceOutputRoots() throws MojoFailureException {
    try {
      // Create the root first if it does not yet exist.
      Files.createDirectories(protobufOutputDirectory);
      sourceRootRegistrar.register(mavenSession.getCurrentProject(), protobufOutputDirectory);
    } catch (IOException ex) {
      throw failure("Failed to register protobuf output root", ex);
    }

    if (grpcPluginVersion != null) {
      try {
        // Create the root first if it does not yet exist.
        Files.createDirectories(grpcOutputDirectory);
        sourceRootRegistrar.register(mavenSession.getCurrentProject(), grpcOutputDirectory);
      } catch (IOException ex) {
        throw failure("Failed to register GRPC output root", ex);
      }
    }
  }

  void dumpProtocVersion(Path protocPath) throws MojoFailureException {
    try {
      ProtocExecutor.invoke(new ProtocArgumentBuilder(protocPath).version());
    } catch (ProtocExecutionException ex) {
      throw failure("Failed to determine protoc version", ex);
    }
  }

  void generateSources(
      Path protocPath,
      Collection<Plugin> plugins,
      Collection<Path> sources
  ) throws MojoExecutionException {
    var argBuilder = new ProtocArgumentBuilder(protocPath)
        .includeDirectories(sourceDirectories)
        .fatalWarnings(fatalWarnings)
        .outputDirectory("java", protobufOutputDirectory, liteOnly);

    for (var plugin : plugins) {
      argBuilder.plugin(plugin.name, plugin.path);
    }

    if (grpcPluginVersion != null) {
      argBuilder.outputDirectory("grpc-java", grpcOutputDirectory, false);
    }

    if (generateKotlinWrappers) {
      argBuilder.outputDirectory("kotlin", protobufOutputDirectory, liteOnly);

      if (grpcPluginVersion != null) {
        argBuilder.outputDirectory("grpc-kotlin", grpcOutputDirectory, false);
      }
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

  private static final class Plugin {
    private final String name;
    private final Path path;

    private Plugin(String name, Path path) {
      this.name = name;
      this.path = path;
    }
  }
}
