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

import java.util.Set;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.jspecify.annotations.Nullable;


public abstract class AbstractGenerateMojo extends AbstractMojo {

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
   * Add additional locations to import code from. These can be file system paths or Maven
   * dependencies (in the format {@code mvn:groupId:artifactId:version[:classifier]}).
   *
   * <p>Maven dependencies should be packages as JARs.
   *
   * <p>Dependencies specified here will NOT be compiled by {@code protoc}, so will also need
   * to have a {@code compile}-scoped dependency available to the Java compiler via the usual means
   * (dependency blocks in your project).
   *
   * @since 0.1.0
   */
  @Parameter
  private @Nullable Set<String> additionalImports;

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

  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {

  }
}
