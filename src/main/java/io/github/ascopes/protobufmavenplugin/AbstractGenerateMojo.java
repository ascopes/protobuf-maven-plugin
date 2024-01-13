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
package io.github.ascopes.protobufmavenplugin;

import static java.util.Objects.requireNonNullElse;

import io.github.ascopes.protobufmavenplugin.dependency.ResolutionException;
import io.github.ascopes.protobufmavenplugin.generate.ImmutableGenerationRequest;
import io.github.ascopes.protobufmavenplugin.generate.SourceCodeGenerator;
import io.github.ascopes.protobufmavenplugin.generate.SourceRootRegistrar;
import io.github.ascopes.protobufmavenplugin.system.FileUtils;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.shared.transfer.dependencies.DefaultDependableCoordinate;
import org.jspecify.annotations.Nullable;

/**
 * Abstract base for a code-generation MOJO.
 *
 * @author Ashley Scopes
 */
public abstract class AbstractGenerateMojo extends AbstractMojo {

  /**
   * The source code generator.
   */
  @Component
  private SourceCodeGenerator sourceCodeGenerator;

  /**
   * The active Maven session.
   */
  @Parameter(required = true, readonly = true, defaultValue = "${session}")
  private MavenSession session;

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
   * Override the source directories to compile from.
   *
   * <p>Leave unspecified or explicitly null/empty to use the defaults.
   *
   * @since 0.0.1
   */
  @Parameter
  private @Nullable Set<String> sourceDirectories;

  /**
   * Specify additional paths to import protobuf sources from on the local file system.
   *
   * <p>These will not be compiled into Java sources directly.
   *
   * <p>If you wish to depend on a JAR containing protobuf sources, add it as a dependency
   * with the {@code provided} scope instead.
   *
   * @since 0.1.0
   */
  @Parameter
  private @Nullable Set<String> additionalImportPaths;

  /**
   * Additional plugins to use with the protobuf compiler.
   *
   * <p>Each plugin must be specified with one of:
   *
   * <ul>
   *   <li>An {@code artifact} block that points to a Maven artifact.</li>
   *   <li>An {@code executableName} block that refers to an executable on the system path.</li>
   * </ul>
   *
   * <p>For example:
   * <code><pre>
   *   &lt;additionalArtifacts&gt;
   *     &lt;additionalArtifact&gt;
   *       &lt;executableName&gt;protoc-gen-grpc-java&lt;/executableName&gt;
   *     &lt;/additionalArtifact&gt;
   *     &lt;additionalArtifact&gt;
   *       &lt;artifact&gt;
   *         &lt;groupId&gt;com.salesforce.servicelibs&lt;/groupId&gt;
   *         &lt;artifactId&gt;reactor-grpc&lt;/artifactId&gt;
   *         &lt;version&gt;1.2.4&lt;/version&gt;
   *       &lt;/artifact&gt;
   *     &lt;/additionalArtifact&gt;
   *   &lt;/additionalArtifacts&gt;
   * </pre></code>
   *
   * @since 0.1.0
   */
  @Parameter
  private @Nullable Set<Plugin> binaryPlugins;

  /**
   * Additional <strong>pure-Java</strong> plugins to use with the protobuf compiler.
   *
   * <p>Unlike artifact-based plugins, these are pure Java JAR applications that abide by the
   * protoc compiler API, and will be executed separately.
   *
   * @since 0.2.0
   */
  @Parameter
  private @Nullable Set<DefaultDependableCoordinate> jvmPlugins;

  /**
   * Override the directory to output generated code to.
   *
   * <p>Leave unspecified or explicitly null to use the defaults.
   *
   * @since 0.1.0
   */
  @Parameter
  private @Nullable String outputDirectory;

  /**
   * Whether to treat {@code protoc} compiler warnings as errors.
   *
   * @since 0.0.1
   */
  @Parameter(defaultValue = "false")
  private boolean fatalWarnings;

  /**
   * Whether to generate default Java source code.
   *
   * <p>Defaults to true, although some users may wish to disable this if using
   * an alternative plugin instead.
   *
   * @since 0.1.1
   */
  @Parameter(defaultValue = "true")
  private boolean javaEnabled;

  /**
   * Whether to also generate Kotlin API wrapper code around the generated Java code.
   *
   * <p>Note that this requires {@code javaEnabled} to also be {@code true}, otherwise compilation may fail.
   *
   * @since 0.1.0
   */
  @Parameter(defaultValue = "false")
  private boolean kotlinEnabled;

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

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    var actualOutputDirectory = outputDirectory == null || outputDirectory.isBlank()
        ? defaultOutputDirectory(session)
        : Path.of(outputDirectory);

    var actualSourceDirectories = sourceDirectories == null
        ? List.of(defaultSourceDirectory(session))
        : parsePaths(sourceDirectories);

    var request = ImmutableGenerationRequest.builder()
        .addAllAdditionalImportPaths(parsePaths(additionalImportPaths))
        .addAllBinaryPlugins(requireNonNullElse(binaryPlugins, Set.of()))
        .addAllJvmPlugins(requireNonNullElse(jvmPlugins, Set.of()))
        .addAllAllowedDependencyScopes(allowedScopes())
        .addAllSourceRoots(actualSourceDirectories)
        .isFatalWarnings(fatalWarnings)
        .isJavaEnabled(javaEnabled)
        .isKotlinEnabled(kotlinEnabled)
        .isLiteEnabled(liteOnly)
        .mavenSession(session)
        .outputDirectory(actualOutputDirectory)
        .protocVersion(protocVersion)
        .sourceRootRegistrar(sourceRootRegistrar())
        .build();

    try {
      if (!sourceCodeGenerator.generate(request)) {
        throw new MojoExecutionException("Protoc invocation failed");
      }
    } catch (ResolutionException | IOException ex) {
      throw new MojoFailureException(this, ex.getMessage(), ex.getMessage());
    }
  }

  protected abstract SourceRootRegistrar sourceRootRegistrar();

  protected abstract Path defaultSourceDirectory(MavenSession session);

  protected abstract Path defaultOutputDirectory(MavenSession session);

  protected abstract Set<String> allowedScopes();

  private Collection<Path> parsePaths(@Nullable Collection<String> paths) {
    if (paths == null) {
      return List.of();
    }

    return paths
        .stream()
        .map(Path::of)
        .map(FileUtils::normalize)
        .collect(Collectors.toUnmodifiableList());
  }
}
