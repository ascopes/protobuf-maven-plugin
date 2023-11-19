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

import static java.util.Optional.ofNullable;

import io.github.ascopes.protobufmavenplugin.execute.ProtocExecutionException;
import io.github.ascopes.protobufmavenplugin.execute.ProtocExecutor;
import io.github.ascopes.protobufmavenplugin.execute.ProtocExecutorBuilder;
import io.github.ascopes.protobufmavenplugin.resolve.MavenProtocResolver;
import io.github.ascopes.protobufmavenplugin.resolve.PathProtocResolver;
import io.github.ascopes.protobufmavenplugin.resolve.ProtoSourceResolver;
import io.github.ascopes.protobufmavenplugin.resolve.ProtocResolutionException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.shared.transfer.artifact.resolve.ArtifactResolver;

/**
 * Base Mojo to generate protobuf sources.
 *
 * <p>Can be extended for each language that this plugin supports.
 *
 * @author Ashley Scopes
 */
public abstract class AbstractGenerateMojo extends AbstractMojo {

  // Injected components.
  @Component
  private ArtifactResolver artifactResolver;

  // Injected parameters.
  private MavenSession mavenSession;
  private String version;
  private Set<Path> sourceDirectories;
  private Path outputDirectory;
  private boolean fatalWarnings;
  private boolean reproducibleBuilds;

  /**
   * Set the artifact resolver.
   */
  public void setArtifactResolver(ArtifactResolver artifactResolver) {
    this.artifactResolver = artifactResolver;
  }

  /**
   * The Maven session that is in use.
   *
   * <p>This is passed in by Maven automatically, so can be ignored.
   */
  @Parameter(defaultValue = "${session}", required = true, readonly = true)
  public void setMavenSession(MavenSession mavenSession) {
    this.mavenSession = mavenSession;
  }

  /**
   * The version of protoc to use.
   *
   * <p>This can be a static version, or a valid Maven version range (such as
   * "{@code [3.5.0,4.0.0)}"). It is recommended to use a static version to ensure your builds are
   * reproducible.
   *
   * <p>If set to "{@code PATH}", then {@code protoc} is resolved from the system path rather than
   * being downloaded. This is useful if you need to use an unsupported architecture/OS, or a
   * development version of {@code protoc}.
   */
  @Parameter(required = true, property = "protoc.version")
  public void setVersion(String version) {
    this.version = version;
  }

  /**
   * The root directories to look for protobuf sources in.
   */
  @Parameter(defaultValue = "${project.basedir}/src/main/protobuf")
  public void setSourceDirectories(Set<String> sourceDirectories) {
    this.sourceDirectories = sourceDirectories
        .stream()
        .map(Path::of)
        .collect(Collectors.toUnmodifiableSet());
  }

  /**
   * The directory to output generated sources to.
   */
  @Parameter(defaultValue = "${project.build.directory}/generated-sources/protoc")
  public void setOutputDirectory(String outputDirectory) {
    this.outputDirectory = Path.of(outputDirectory);
  }

  /**
   * Whether to treat {@code protoc} compiler warnings as errors.
   */
  @Parameter(defaultValue = "false")
  public void setFatalWarnings(boolean fatalWarnings) {
    this.fatalWarnings = fatalWarnings;
  }

  /**
   * Whether to attempt to force builds to be reproducible.
   *
   * <p>When enabled, {@code protoc} may attempt to keep things like map ordering
   * consistent between builds as long as the same version of {@code protoc} is used each time.
   */
  @Parameter(defaultValue = "false")
  public void setReproducibleBuilds(boolean reproducibleBuilds) {
    this.reproducibleBuilds = reproducibleBuilds;
  }

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    var protocPath = resolveProtocPath();
    var sources = resolveProtoSources();

    // Protoc will not create the output directory if it does not exist already.
    createOutputDirectory();
    registerSource(mavenSession.getCurrentProject(), outputDirectory);

    var compilerExecutor = new ProtocExecutorBuilder(protocPath)
        .deterministicOutput(reproducibleBuilds)
        .fatalWarnings(fatalWarnings)
        .includeDirectories(sourceDirectories)
        .outputDirectory(getSourceOutputType(), outputDirectory)
        .buildCompilation(sources);

    run(compilerExecutor);
  }

  /**
   * The source output type to use with {@code protoc} (e.g. {@code java} for {@code --java_out}).
   *
   * @return the source output type.
   */
  protected abstract String getSourceOutputType();

  /**
   * Register the given output path with the project for compilation.
   *
   * @param project the Maven project.
   * @param path    the path to register.
   */
  protected abstract void registerSource(MavenProject project, Path path);

  private Path resolveProtocPath() throws MojoExecutionException, MojoFailureException {
    try {
      var resolver = version.trim().equalsIgnoreCase("PATH")
          ? new PathProtocResolver()
          : new MavenProtocResolver(version, artifactResolver, mavenSession);

      return resolver.resolveProtoc();

    } catch (ProtocResolutionException ex) {
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

  private void run(ProtocExecutor executor) throws MojoExecutionException, MojoFailureException {
    try {
      var exitCode = executor.invoke();
      if (exitCode != 0) {
        throw error("protoc returned a non-zero exit code (" + exitCode + ")", null);
      }
    } catch (ProtocExecutionException ex) {
      throw failure("Failed to execute protoc", ex);
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
