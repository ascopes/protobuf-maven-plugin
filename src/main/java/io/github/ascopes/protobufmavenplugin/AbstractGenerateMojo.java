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
import java.util.List;
import java.util.Set;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
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
   * The default directory to look for protobuf sources in.
   */
  protected static final String MAIN_SOURCE = "${project.basedir}/src/main/protobuf";

  /**
   * The default directory to look for test protobuf sources in.
   */
  protected static final String TEST_SOURCE = "${project.basedir}/src/test/protobuf";

  /**
   * The default directory to output generated sources to.
   */
  protected static final String MAIN_OUTPUT = "${project.build.directory}/generated-sources";

  /**
   * The default directory to output generated test sources to.
   */
  protected static final String TEST_OUTPUT = "${project.build.directory}/generated-test-sources";

  // Injected components.
  @Component
  private @Nullable ArtifactResolver artifactResolver;

  // Injected parameters.
  private @Nullable MavenSession mavenSession;
  private @Nullable String version;
  private @Nullable Set<Path> sourceDirectories;
  private @Nullable Path outputDirectory;
  private @Nullable Boolean fatalWarnings;
  private @Nullable Boolean generateKotlinWrappers;
  private @Nullable Boolean liteOnly;

  /**
   * Initialise this Mojo.
   */
  protected AbstractGenerateMojo() {
    // Expect all fields to be initialised later by Plexus.
  }

  /**
   * Set the artifact resolver.
   *
   * @param artifactResolver the artifact resolver.
   */
  public final void setArtifactResolver(ArtifactResolver artifactResolver) {
    this.artifactResolver = artifactResolver;
  }

  /**
   * The Maven session that is in use.
   *
   * <p>This is passed in by Maven automatically, so can be ignored.
   *
   * @param mavenSession the Maven session.
   * @since 0.0.1
   */
  @Parameter(defaultValue = "${session}", required = true, readonly = true)
  public final void setMavenSession(MavenSession mavenSession) {
    this.mavenSession = mavenSession;
  }

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
   * @param version the version of {@code protoc} to use.
   * @since 0.0.1
   */
  @Parameter(required = true, property = "protoc.version")
  public final void setVersion(String version) {
    this.version = version;
  }

  /**
   * Whether to treat {@code protoc} compiler warnings as errors.
   *
   * @param fatalWarnings whether to treat warnings as errors or not.
   * @since 0.0.1
   */
  @Parameter(defaultValue = "false")
  public final void setFatalWarnings(boolean fatalWarnings) {
    this.fatalWarnings = fatalWarnings;
  }

  /**
   * Whether to also generate Kotlin API wrapper code around the generated Java code.
   *
   * @param generateKotlinWrappers whether to generate Kotlin wrappers or not.
   * @since 0.0.1
   */
  @Parameter(defaultValue = "false")
  public final void setGenerateKotlinWrappers(boolean generateKotlinWrappers) {
    this.generateKotlinWrappers = generateKotlinWrappers;
  }

  /**
   * Whether to only generate "lite" messages or not.
   *
   * <p>These are bare-bones sources that do not contain most of the metadata that regular
   * Protobuf sources contain, and are designed for low-latency/low-overhead scenarios.
   *
   * <p>See the protobuf documentation for the pros and cons of this.
   *
   * @param liteOnly whether to only generate "lite" messages or not.
   * @since 0.0.1
   */
  @Parameter(defaultValue = "false")
  public final void setLiteOnly(boolean liteOnly) {
    this.liteOnly = liteOnly;
  }

  /**
   * Execute this goal.
   *
   * @throws MojoExecutionException if a user/configuration error is encountered.
   * @throws MojoFailureException if execution fails due to an internal error.
   */
  @Override
  public final void execute() throws MojoExecutionException, MojoFailureException {
    requireNonNull(artifactResolver);
    requireNonNull(mavenSession);
    requireNonNull(version);
    requireNonNull(sourceDirectories);
    requireNonNull(outputDirectory);
    requireNonNull(fatalWarnings);
    requireNonNull(generateKotlinWrappers);
    requireNonNull(liteOnly);

    var protocPath = resolveProtocPath();
    var sources = resolveProtoSources();

    // Protoc will not create the output directory if it does not exist already.
    createOutputDirectory();
    registerSource(mavenSession.getCurrentProject(), outputDirectory);

    try {
      var protocExecutor = new ProtocExecutor();

      // Log the version first.
      var versionArgs = new ProtocArgumentBuilder(protocPath)
          .version();
      protocExecutor.invoke(versionArgs);

      // Perform the compilation next.
      var compilerArgsBuilder = new ProtocArgumentBuilder(protocPath)
          .fatalWarnings(fatalWarnings)
          .includeDirectories(sourceDirectories)
          .outputDirectory("java", outputDirectory, liteOnly);

      if (generateKotlinWrappers) {
        // Will emit stubs that wrap the generated Java code only.
        compilerArgsBuilder
            .outputDirectory("kotlin", outputDirectory, liteOnly);
      }

      var compilerArgs = compilerArgsBuilder.build(sources);

      protocExecutor.invoke(compilerArgs);
    } catch (ProtocExecutionException ex) {
      throw new MojoExecutionException("Failed to invoke protoc", ex);
    }
  }

  /**
   * Register the given output path with the project for compilation.
   *
   * @param project the Maven project.
   * @param path    the path to register.
   */
  protected abstract void registerSource(MavenProject project, Path path);

  /**
   * The root directories to look for protobuf sources in.
   *
   * @param sourceDirectories the source directories.
   */
  protected final void setSourceDirectoryPaths(Set<Path> sourceDirectories) {
    this.sourceDirectories = sourceDirectories;
  }

  /**
   * Set the output directory.
   *
   * <p>Implementations are expected to declare a parameter that is named "outputDirectory"
   * and passes the input to this method as a {@link Path}. This is done to allow overriding
   * the output directory per goal.
   *
   * @param outputDirectory the output directory.
   */
  protected final void setOutputDirectory(Path outputDirectory) {
    this.outputDirectory = outputDirectory;
  }

  private Path resolveProtocPath() throws MojoExecutionException, MojoFailureException {
    try {
      var resolver = version.trim().equalsIgnoreCase("PATH")
          ? new PathProtocResolver()
          : new MavenProtocResolver(version, artifactResolver, mavenSession);

      return resolver.resolve();

    } catch (ExecutableResolutionException ex) {
      throw error("Failed to resolve protoc executable", ex);
    } catch (Exception ex) {
      throw failure("Failed to resolve protoc executable", ex);
    }
  }

  private List<Path> resolveProtoSources() throws MojoFailureException {
    try {
      return new ProtoSourceResolver().collectSources(sourceDirectories);
    } catch (IOException ex) {
      throw failure("Failed to resolve proto sources", ex);
    }
  }

  private void createOutputDirectory() throws MojoFailureException {
    try {
      Files.createDirectories(outputDirectory);
    } catch (IOException ex) {
      throw failure("Failed to create output directory '" + outputDirectory + "'", ex);
    }
  }

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
