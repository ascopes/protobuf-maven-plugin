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
package io.github.ascopes.protobufmavenplugin.mojo;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElseGet;
import static java.util.function.Predicate.not;

import io.github.ascopes.protobufmavenplugin.dependencies.DependencyResolutionDepth;
import io.github.ascopes.protobufmavenplugin.dependencies.MavenDependencyBean;
import io.github.ascopes.protobufmavenplugin.digests.Digest;
import io.github.ascopes.protobufmavenplugin.generation.GenerationResult;
import io.github.ascopes.protobufmavenplugin.generation.ImmutableGenerationRequest;
import io.github.ascopes.protobufmavenplugin.generation.Language;
import io.github.ascopes.protobufmavenplugin.generation.OutputDescriptorAttachmentRegistrar;
import io.github.ascopes.protobufmavenplugin.generation.ProtobufBuildOrchestrator;
import io.github.ascopes.protobufmavenplugin.generation.SourceRootRegistrar;
import io.github.ascopes.protobufmavenplugin.plugins.BinaryMavenProtocPluginBean;
import io.github.ascopes.protobufmavenplugin.plugins.JvmMavenProtocPluginBean;
import io.github.ascopes.protobufmavenplugin.plugins.PathProtocPluginBean;
import io.github.ascopes.protobufmavenplugin.plugins.ProtocPlugin;
import io.github.ascopes.protobufmavenplugin.plugins.UriProtocPluginBean;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import javax.inject.Inject;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.project.MavenProjectHelper;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Abstract base for a code generation Mojo that calls {@code protoc}.
 *
 * @author Ashley Scopes
 */
public abstract class AbstractGenerateMojo extends AbstractMojo {

  private static final String DEFAULT_FALSE = "false";
  private static final String DEFAULT_TRUE = "true";
  private static final String DEFAULT_TRANSITIVE = "TRANSITIVE";

  private static final String COMPILER_VERSION_PROPERTY = "protobuf.compiler.version";

  private static final Logger log = LoggerFactory.getLogger(AbstractGenerateMojo.class);

  public AbstractGenerateMojo() {
    // Nothing to do here.
  }

  /*
   * Dependencies to inject.
   */

  /**
   * The source code generator.
   */
  @Inject
  ProtobufBuildOrchestrator sourceCodeGenerator;

  /**
   * The active Maven project.
   */
  @Inject
  MavenProject mavenProject;

  /**
   * The active MavenProjectHelper.
   */
  @Inject
  MavenProjectHelper mavenProjectHelper;

  /**
   * Provide additional arguments to pass to the {@code protoc} executable.
   *
   * <p>Generally, users do not need to use this. It is useful, however, if their use-case is not
   * covered by other configuration parameters in this goal.
   *
   * <p>Configuring arguments that are covered by other parameters in this goal is undefined
   * behaviour and should be avoided.
   *
   * <p>Example:
   * <pre>{@code
   *   <arguments>
   *     <argument>--experimental_allow_proto3_optional</argument>
   *     <argument>--php_out=target/generated-sources/php</argument>
   *   </arguments>
   * }</pre>
   *
   * @since 3.8.0
   */
  @Parameter
  @Nullable List<String> arguments;

  /**
   * If {@code true}, all output directories will be cleared before {@code protoc}
   * is invoked.
   *
   * <p>Enable this to force a clean build on each invocation.
   *
   * <p>This is ignored if {@code incrementalCompilation} is enabled, since it would discard the
   * information needed to support incremental compilation. In this case, it will be considered to
   * be false regardless.
   *
   * @since 3.6.0
   */
  @Parameter(defaultValue = DEFAULT_FALSE)
  boolean cleanOutputDirectories;

  /**
   * How to resolve transitive dependencies.
   *
   * <p>Supported values:
   *
   * <ul>
   *   <li><code>TRANSITIVE</code> - resolve transitive dependencies.</li>
   *   <li><code>DIRECT</code> - only resolve direct dependencies that were explicitly
   *       specified.</li>
   * </ul>
   *
   * @since 1.2.0
   */
  @Parameter(defaultValue = DEFAULT_TRANSITIVE)
  DependencyResolutionDepth dependencyResolutionDepth;

  /**
   * The dependency scopes to resolve dependencies for.
   *
   * <p>If unspecified, this uses a sensible default, as documented in the goal
   * description.
   *
   * <p>Valid values include: {@code compile}, {@code test}, {@code provided}, {@code runtime},
   * {@code system}, and {@code import}.
   *
   * <p>Refer to the Maven documentation for scopes for more details on the implications of each.
   *
   * @since 2.4.0
   */
  @Parameter
  @Nullable Set<String> dependencyScopes;

  /**
   * Enable attaching all compiled protobuf sources to the output of this Maven project so that
   * they are included in any generated JAR.
   *
   * <p>If one is using dependencies as sources, then those will also be attached, and may have
   * license implications.
   *
   * <p>Prior to v4.0.0, this defaulted to {@code false}. As of v4.0.0, this defaults to
   * {@code true} to improve the semantics around creating importable libraries.
   *
   * @since 2.1.0
   */
  @Parameter(defaultValue = DEFAULT_TRUE)
  boolean embedSourcesInClassOutputs;

  /**
   * Additional environment variables to pass to the {@code protoc} subprocess.
   *
   * <p>This can be used to override some internal behaviours within {@code protoc} or any
   * associated {@code protoc} plugins during execution.
   *
   * <p>By default, any environment variables made visible to Maven will be passed to
   * {@code protoc}. Any environment variables specified here will be appended to the default
   * environment variables, overwriting any that have duplicate names.
   *
   * <p>This will not support overriding aspects like the system path, as those are resolved
   * statically prior to any invocation.
   *
   * @since 3.7.0
   */
  @Parameter
  @Nullable Map<String, String> environmentVariables;

  /**
   * Source paths to protobuf sources to exclude from compilation.
   *
   * <p>This can be used to limit what is compiled by {@code protoc}.
   *
   * <p>Each entry is treated as a glob pattern, and is applied to the path of each discovered
   * compilation candidate file, relative to the {@code sourceDirectory} or {@code sourceDependency}
   * that provides them.
   *
   * <p>See <a href="https://docs.oracle.com/en%2Fjava%2Fjavase%2F11%2Fdocs%2Fapi%2F%2F/java.base/java/nio/file/FileSystem.html#getPathMatcher(java.lang.String)">
   * {@code java.nio.file.FileSystem#getPathMatcher}</a> for full details of the supported syntax
   * within glob patterns.
   *
   * <p>If a file matches <strong>any</strong> of these patterns, it is automatically excluded.
   *
   * <p>If not provided, then the default is to not exclude anything.
   *
   * <p>For example, if one wishes to not compile files named {@code user.proto},
   * {@code message.proto}, or {@code service.proto}, they should use the following
   * configuration.
   *
   * <pre><code>&lt;excludes&gt;
   *   &lt;exclude&gt;**&#47;user.proto&lt;/exclude&gt;
   *   &lt;exclude&gt;**&#47;message.proto&lt;/exclude&gt;
   *   &lt;exclude&gt;**&#47;service.proto&lt;/exclude&gt;
   * &lt;/excludes&gt;
   * </code></pre>
   *
   * <p>Use {@code includes} if one wishes to instead include files for compilation.
   *
   * @since 2.2.0
   */
  @Parameter(property = "protobuf.compiler.excludes")
  @Nullable List<String> excludes;

  /**
   * Fail on missing sources.
   *
   * <p>If no sources are detected, it is usually a sign that this plui  is misconfigured, or that
   * one is including this plugin in a project that does not need it. For this reason, the plugin
   * defaults this setting to being enabled.
   *
   * @since 0.5.0
   */
  @Parameter(defaultValue = DEFAULT_TRUE)
  boolean failOnMissingSources;

  /**
   * Fail if no output languages and no plugins are enabled.
   *
   * <p>This defaults to {@code true}, but may be set to {@code false} if all plugins are optional
   * and no languages are enabled.
   *
   * <p>Users should prefer to {@code skip} the plugin if a known configuration has no targets.
   *
   * @since 2.0.0
   */
  @Parameter(defaultValue = DEFAULT_TRUE)
  boolean failOnMissingTargets;

  /**
   * Specify that any warnings emitted by {@code protoc} should be treated as errors and fail the
   * build.
   *
   * <p>Defaults to {@code false}.
   *
   * @since 0.0.1
   */
  @Parameter(defaultValue = DEFAULT_FALSE)
  boolean fatalWarnings;

  /**
   * Ignore the {@code <dependencies/>} blocks in the Maven project when discovering
   * {@code *.proto} files to add to the import paths.
   *
   * <p>Generally users will want to leave this enabled unless they have a very specific case where
   * they wish to take control of how dependency resolution works.
   *
   * @since 1.2.0
   */
  @Parameter(defaultValue = DEFAULT_FALSE)
  boolean ignoreProjectDependencies;

  /**
   * Specify additional dependencies to import protobuf sources from.
   *
   * <p>These will not be compiled into Java sources directly.
   *
   * <p>Objects support the following attributes:
   *
   * <ul>
   *   <li>{@code groupId} - the group ID - required</li>
   *   <li>{@code artifactId} - the artifact ID - required</li>
   *   <li>{@code version} - the version - required</li>
   *   <li>{@code type} - the artifact type - optional</li>
   *   <li>{@code classifier} - the artifact classifier - optional</li>
   *   <li>{@code dependencyResolutionDepth} - the dependency resolution depth to override
   *      the project settings with - optional</li>
   *   <li>{@code excludes} - a set of exclusions to apply to transitive dependencies</li>
   * </ul>
   *
   * <p>Exclusions are a set of objects, each with the following fields:
   *
   * <ul>
   *   <li>{@code groupId} - the group ID to exclude</li>
   *   <li>{@code artifactId} - the artifact ID to exclude</li>
   *   <li>{@code classifier} - optional - the classifier to exclude. If omitted, any classifiers
   *      are matched.</li>
   *   <li>{@code type} - optional - the type of the artifact to exclude. If omitted, any types
   *      are matched.</li>
   * </ul>
   *
   * @since 1.2.0
   */
  @Parameter
  @Nullable List<MavenDependencyBean> importDependencies;

  /**
   * Specify additional paths to import protobuf sources from on the local file system.
   *
   * <p>These will not be compiled into Java sources directly.
   *
   * <p>If users wish to depend on a JAR Maven artifact containing protobuf sources, then they
   * should add it as a dependency with the {@code provided} or {@code test} scope instead, or use
   * {@code importDependencies} rather than this parameter.
   *
   * <p>Import paths can also be specified as paths to ZIP or JAR archives on the local
   * file system. This plugin will extract any {@code *.proto} files, and pass them to
   * {@code protoc}.
   *
   * <p>If users wish to compile these proto sources rather than simply including them on the
   * import path, they should use the {@code sourceDirectories} parameter instead.
   *
   * @since 0.1.0
   */
  @Parameter
  @Nullable List<Path> importPaths;

  /**
   * Protobuf source paths to include in compilation.
   *
   * <p>This can be used to limit what is compiled by {@code protoc}.
   *
   * <p>Each entry is treated as a glob pattern, and is applied to the path of each discovered
   * compilation candidate file, relative to the {@code sourceDirectory} or {@code sourceDependency}
   * that provides them
   *
   * <p>See <a href="https://docs.oracle.com/en%2Fjava%2Fjavase%2F11%2Fdocs%2Fapi%2F%2F/java.base/java/nio/file/FileSystem.html#getPathMatcher(java.lang.String)">
   * {@code java.nio.file.FileSystem#getPathMatcher}</a> for full details of the supported syntax
   * within glob patterns.
   *
   * <p>If a file matches <strong>any</strong> of these patterns, it is automatically included.
   *
   * <p>If not provided, then the default is to allow any protobuf source file.
   *
   * <p>For example, if a user only wanted to compile files named {@code user.proto},
   * {@code message.proto}, or {@code service.proto}, then they would use the following
   * configuration.
   *
   * <pre><code>&lt;includes&gt;
   *   &lt;include&gt;**&#47;user.proto&lt;/include&gt;
   *   &lt;include&gt;**&#47;message.proto&lt;/include&gt;
   *   &lt;include&gt;**&#47;service.proto&lt;/include&gt;
   * &lt;/includes&gt;
   * </code></pre>
   *
   * <p>Use {@code excludes} to instead omit files from compilation.
   *
   * @since 2.2.0
   */
  @Parameter(property = "protobuf.compiler.includes")
  @Nullable List<String> includes;

  /**
   * Enable "incremental" compilation.
   *
   * <p>When enabled, this plugin will track changes to sources and importable protobuf
   * dependencies between builds, making a best-effort attempt to only rebuild files when
   * changes have been made since the last build.
   *
   * @since 2.7.0
   */
  @Parameter(defaultValue = DEFAULT_TRUE, property = "protobuf.compiler.incremental")
  boolean incrementalCompilation;

  /**
   * Enable generating Java sources from the protobuf sources.
   *
   * <p>Defaults to {@code true}, although some users may wish to disable this
   * if using an alternative plugin that covers generating the code for models instead.
   *
   * @since 0.1.1
   */
  @Parameter(defaultValue = DEFAULT_TRUE)
  boolean javaEnabled;

  /**
   * Generate Kotlin API wrapper code around the generated Java code.
   *
   * <p>This may require {@code javaEnabled} to also be {@code true}, otherwise compilation
   * may fail unless other sources are generated to replace the expected Java ones.
   *
   * @since 0.1.0
   */
  @Parameter(defaultValue = DEFAULT_FALSE)
  boolean kotlinEnabled;

  /**
   * Generate "lite" messages rather than full messages, where possible.
   *
   * <p>These are bare-bones sources that do not contain most of the metadata that regular
   * protobuf sources contain, and are designed for low-latency/low-overhead scenarios.
   *
   * <p>See the protobuf documentation for the pros and cons of this.
   *
   * @since 0.0.1
   */
  @Parameter(defaultValue = DEFAULT_FALSE)
  boolean liteOnly;

  /**
   * The path to write the protobuf descriptor file to.
   *
   * <p>Leave unspecified to disable. Writes a FileDescriptorSet (a protocol buffer,
   * defined by {@code descriptor.proto}) containing all the input files in
   * {@code outputDescriptorFile}.
   *
   * <p>If this is specified, then incremental compilation will always be disabled
   * to prevent issues with inconsistent build results.
   *
   * @see #outputDescriptorAttached
   * @since 2.9.0
   */
  @Parameter
  @Nullable Path outputDescriptorFile;

  /**
   * Attach the generated protobuf descriptor file as a Maven project artifact.
   *
   * <p>This is ignored if {@code outputDescriptorFile} is not provided.
   *
   * @see #outputDescriptorFile
   * @since 2.11.0
   */
  @Parameter(defaultValue = DEFAULT_FALSE)
  boolean outputDescriptorAttached;

  /**
   * The Maven artifact type for the file descriptor set descriptor when attached to the
   * Maven project.
   *
   * <p>This is ignored if {@code outputDescriptorAttached} is false, or if
   * {@code outputDescriptorFile} is not provided.
   *
   * @see #outputDescriptorAttached
   * @see #outputDescriptorFile
   * @since 2.11.0
   */
  @Parameter
  @Nullable String outputDescriptorAttachmentType;

  /**
   * The Maven artifact classifier for the file descriptor set descriptor when attached to
   * the Maven project.
   *
   * <p>This is ignored if {@code outputDescriptorAttached} is false.</p>
   *
   * @see #outputDescriptorFile
   * @since 2.11.0
   */
  @Parameter
  @Nullable String outputDescriptorAttachmentClassifier;

  /**
   * Include imports in generated file descriptor set descriptor files.
   *
   * <p>This is ignored if {@code outputDescriptorFile} is not provided.
   *
   * @see #outputDescriptorFile
   * @since 2.10.0
   */
  @Parameter(defaultValue = DEFAULT_FALSE)
  boolean outputDescriptorIncludeImports;

  /**
   * Include source information in generated file descriptor set descriptors.
   * files.
   *
   * <p>This is ignored if {@code outputDescriptorFile} is not provided.
   *
   * @see #outputDescriptorFile
   * @since 2.10.0
   */
  @Parameter(defaultValue = DEFAULT_FALSE)
  boolean outputDescriptorIncludeSourceInfo;

  /**
   * Retain option details in generated file descriptor set descriptors.
   *
   * <p>This is ignored if {@code outputDescriptorFile} is not provided.
   *
   * @see #outputDescriptorFile
   * @since 2.10.0
   */
  @Parameter(defaultValue = DEFAULT_FALSE)
  boolean outputDescriptorRetainOptions;

  /**
   * The directory to output generated code to.
   *
   * <p>Leave unspecified or explicitly null to use the default for the
   * goal. This defaults to the Maven generated sources directory within {@code target/}.
   *
   * @since 0.1.0
   */
  @Parameter
  @Nullable Path outputDirectory;

  /**
   * Protoc plugins to invoke as part of the build.
   *
   * <p>This is a list of zero or more plugin descriptors, which can have varying types. The
   * {@code kind} attribute must be set on each to allow determining which type of plugin is
   * being specified.
   *
   * <p>See <a href="https://ascopes.github.io/protobuf-maven-plugin/using-protoc-plugins.html">
   * "using protoc plugins"</a> for usage examples and documentation.
   *
   * @since 4.1.0
   */
  @Parameter
  @Nullable List<ProtocPlugin> plugins;

  /**
   * Optional digest to verify {@code protoc} against.
   *
   * <p>Generally, users should not need to provide this, as the Maven Central
   * {@code protoc} binaries will already be digest-verified as part of distribution.
   * Users may wish to specify this if using a {@code PATH}-based binary, or using a URL for
   * {@code protoc}.
   *
   * <p>This is a string in the format {@code sha512:1a2b3c...}, using any
   * message digest algorithm supported by the current JDK.
   *
   * @since 3.5.0
   */
  @Parameter(property = "protobuf.compiler.digest")
  @Nullable Digest protocDigest;

  /**
   * Where to find {@code protoc} or which version to download.
   *
   * <p>This usually should correspond to the version of {@code protobuf-java} or similar that
   * is in use.
   *
   * <p>If set to "{@code PATH}", then {@code protoc} is resolved from the system path rather than
   * being downloaded. This is useful if users need to use an unsupported architecture/OS, or a
   * development version of {@code protoc}.
   *
   * <p>Users may also specify a URL. See the user guide for a list of supported protocols.
   *
   * <p>Note that specifying {@code -Dprotobuf.compiler.version} in the {@code MAVEN_OPTS} or on
   * the command line overrides the version specified in the POM. This enables users to easily
   * override the version of {@code protoc} in use if their system is unable to support the
   * version specified in the POM. Termux users in particular will find
   * {@code -Dprotobuf.compiler.version=PATH} to be useful, due to platform limitations with
   * {@code libpthread} that can result in {@code SIGSYS} (Bad System Call) being raised.
   *
   * <p>Path resolution on Linux, macOS, and other POSIX-like systems, resolution looks
   * for an executable binary matching the exact name in any directory in the {@code $PATH}
   * environment variable.
   *
   * <p>Path resolution on Windows, the case-insensitive {@code %PATH%} environment variable is
   * searched for an executable that matches the name, ignoring case and any file extension.
   * The file extension is expected to match any extension in the {@code %PATHEXT%} environment
   * variable.
   *
   * @since 0.0.1
   */
  @Parameter(
      alias = "protocVersion",
      required = true,
      property = COMPILER_VERSION_PROPERTY
  )
  String protoc;

  /**
   * Generate Python sources from the protobuf sources.
   *
   * <p>Users may also want to enable Python stubs to enable generating {@code *.pyi} files for
   * static type checkers.
   *
   * @since 1.1.0
   */
  @Parameter(defaultValue = DEFAULT_FALSE)
  boolean pythonEnabled;

  /**
   * Generate Python stubs ({@code *.pyi} files) for static typechecking from the protobuf
   * sources.
   *
   * <p>Users will also want to enable Python itself to get actual source code to accompany the
   * stubs.
   *
   * @since 1.1.0
   */
  @Parameter(defaultValue = DEFAULT_FALSE)
  boolean pythonStubsEnabled;

  /**
   * Register the output directories as compilation roots with Maven.
   *
   * <p>This allows {@code maven-compiler-plugin} to detect and compile generated code.
   *
   * <p>Generally, users want to do this, but there may be edge cases where one
   * wishes to control this behaviour manually instead. In this case, they should set this
   * parameter to be {@code false}.
   *
   * @since 0.5.0
   */
  @Parameter(defaultValue = DEFAULT_TRUE)
  boolean registerAsCompilationRoot;

  /**
   * Generate Ruby sources from the protobuf sources.
   *
   * @since 1.1.0
   */
  @Parameter(defaultValue = DEFAULT_FALSE)
  boolean rubyEnabled;

  /**
   * Corporate-sanctioned path to run native executables from.
   *
   * <p>Most users <strong>SHOULD NOT</strong> specify this.
   *
   * <p>If users operate in an overly locked-down corporate environment that disallows running
   * shell/batch scripts or native executables outside sanctioned locations on their local
   * file system, they can specify the path here either via this configuration parameter
   * or via a property such that any executables are first moved to a directory within this
   * location. This is designed to be able to be used within a Maven profile if desired.
   *
   * <p>When specified, any executables will be copied to this directory prior to invoking them.
   * These executables will be located in a nested sub-directory to allow this setting to be
   * shared across plugin invocations whilst retaining build reproducibility.
   *
   * @since 3.9.0
   */
  @Parameter(property = "protobuf.sanctioned-executable-path")
  @Nullable Path sanctionedExecutablePath;

  /**
   * Whether to skip the plugin execution entirely.
   *
   * @since 2.0.0
   */
  @Parameter(defaultValue = DEFAULT_FALSE, property = "protobuf.skip")
  boolean skip;

  /**
   * Additional dependencies to compile, pulled from the Maven repository.
   *
   * <p>Note that this will resolve dependencies recursively unless
   * {@code dependencyResolutionDepth} is set to {@code DIRECT}.
   *
   * <p>For example:
   * <pre>{@code
   * <sourceDependencies>
   *   <sourceDependency>
   *     <groupId>com.mycompany</groupId>
   *     <artifactId>common-protos</artifactId>
   *     <version>1.2.4</version>
   *     <type>zip</type>
   *   </sourceDependency>
   * </sourceDependencies>
   * }</pre>
   *
   * <p>Objects support the following attributes:
   *
   * <ul>
   *   <li>{@code groupId} - the group ID - required</li>
   *   <li>{@code artifactId} - the artifact ID - required</li>
   *   <li>{@code version} - the version - required</li>
   *   <li>{@code type} - the artifact type - optional</li>
   *   <li>{@code classifier} - the artifact classifier - optional</li>
   *   <li>{@code dependencyResolutionDepth} - the dependency resolution depth to override
   *      the project settings with - optional</li>
   *   <li>{@code excludes} - a set of exclusions to apply to transitive dependencies</li>
   * </ul>
   *
   * <p>Exclusions are a set of objects, each with the following fields:
   *
   * <ul>
   *   <li>{@code groupId} - the group ID to exclude</li>
   *   <li>{@code artifactId} - the artifact ID to exclude</li>
   *   <li>{@code classifier} - optional - the classifier to exclude. If omitted, any classifiers
   *      are matched.</li>
   *   <li>{@code type} - optional - the type of the artifact to exclude. If omitted, any types
   *      are matched.</li>
   * </ul>
   *
   * @since 1.2.0
   */
  @Parameter
  @Nullable List<MavenDependencyBean> sourceDependencies;

  /**
   * Protobuf Descriptor files to compile.
   *
   * <p>For example:
   * <pre>{@code
   * <sourceDescriptorDependencies>
   *   <sourceDescriptorDependency>
   *     <groupId>com.mycompany</groupId>
   *     <artifactId>common-protos</artifactId>
   *     <version>1.2.4</version>
   *     <type>protobin</type>
   *   </sourceDescriptorDependency>
   * </sourceDescriptorDependencies>
   * }</pre>
   *
   * <p>Objects support the following attributes:
   *
   * <ul>
   *   <li>{@code groupId} - the group ID - required</li>
   *   <li>{@code artifactId} - the artifact ID - required</li>
   *   <li>{@code version} - the version - required</li>
   *   <li>{@code type} - the artifact type - optional</li>
   *   <li>{@code classifier} - the artifact classifier - optional</li>
   *   <li>{@code excludes} - a set of exclusions to apply to transitive dependencies</li>
   * </ul>
   *
   * <p>If users wish to use descriptor files from the local file system, use
   * the {@code sourceDescriptorPaths} parameter instead.
   *
   * @since 3.1.0
   */
  @Parameter
  @Nullable List<MavenDependencyBean> sourceDescriptorDependencies;

  /**
   * The source directories to compile protobuf sources from.
   *
   * <p>Leave unspecified or explicitly null/empty to use the defaults.
   *
   * <p><strong>Note that specifying custom directories will override the default
   * directories rather than adding to them.</strong>
   *
   * <p>Source directories can also be specified as paths to ZIP or JAR archives on the local
   * file system. This plugin will extract any {@code *.proto} files, and pass them to
   * {@code protoc}.
   *
   * <p>If users wish to compile sources from within a Maven artifact holding a JAR or ZIP, use the
   * {@code sourceDependencies} parameter instead.
   *
   * <p>If users wish to compile sources from descriptor files from the local file system, use
   * the {@code sourceDescriptorPaths} parameter instead.
   *
   * <p>If uzers wish to compile sources from within a Maven artifact holding a protobuf descriptor
   * file, use {@code sourceDescriptorDependencies} instead.
   *
   * @since 0.0.1
   */
  @Parameter(alias = "sourcePaths")
  @Nullable List<Path> sourceDirectories;

  /**
   * Descriptor files to compile from.
   *
   * @since 3.1.0
   */
  @Parameter
  @Nullable List<Path> sourceDescriptorPaths;

  /*
   * Deprecated parameters to remove in v5.
   */

  /**
   * Binary plugins to use with the protobuf compiler, sourced from a Maven repository.
   *
   * <p>See the
   * <a href="https://ascopes.github.io/protobuf-maven-plugin/using-protoc-plugins.html">"Using Protoc Plugins" page</a>
   * for more details on the parameters that this block can take.
   *
   * @since 0.3.0
   * @deprecated Users should now use {@link #plugins} instead. This option will be removed in v5.
   */
  @Deprecated(forRemoval = true)
  @Parameter
  @Nullable List<BinaryMavenProtocPluginBean> binaryMavenPlugins;

  /**
   * Binary plugins to use with the protobuf compiler, sourced from the system {@code PATH}.
   *
   * <p>Binary plugins are {@code protoc} plugins that are regular executables, and thus can work
   * with {@code protoc} out of the box.
   *
   * <p>See the
   * <a href="https://ascopes.github.io/protobuf-maven-plugin/using-protoc-plugins.html">"Using Protoc Plugins" page</a>
   * for more details on the parameters that this block can take.
   *
   * @since 2.0.0
   * @deprecated Users should now use {@link #plugins} instead. This option will be removed in v5.
   */
  @Deprecated(forRemoval = true)
  @Parameter
  @Nullable List<PathProtocPluginBean> binaryPathPlugins;

  /**
   * Binary plugins to use with the protobuf compiler, specified as a valid URL.
   *
   * <p>Binary plugins are {@code protoc} plugins that are regular executables, and thus can work
   * with {@code protoc} out of the box.
   *
   * <p>See the
   * <a href="https://ascopes.github.io/protobuf-maven-plugin/using-protoc-plugins.html">"Using Protoc Plugins" page</a>
   * for more details on the parameters that this block can take.
   *
   * @since 2.0.0
   * @deprecated Users should now use {@link #plugins} instead. This option will be removed in v5.
   */
  @Deprecated(forRemoval = true)
  @Parameter
  @Nullable List<UriProtocPluginBean> binaryUrlPlugins;

  /**
   * Additional <strong>pure-Java</strong> plugins to use with the protobuf compiler.
   *
   * <p>Unlike artifact-based plugins, these are pure Java JAR applications that abide by the
   * protoc compiler API, and will be provided to the compiler via generated scripts.
   *
   * <p>See the
   * <a href="https://ascopes.github.io/protobuf-maven-plugin/using-protoc-plugins.html">"Using Protoc Plugins" page</a>
   * for more details on the parameters that this block can take.
   *
   * @since 0.3.0
   * @deprecated Users should now use {@link #plugins} instead. This option will be removed in v5.
   */
  @Deprecated(forRemoval = true)
  @Parameter
  @Nullable List<JvmMavenProtocPluginBean> jvmMavenPlugins;

  /*
   * Implementation-specific details.
   */

  /**
   * Provides the default source directory to read protobuf sources from.
   *
   * <p>This does not need to point to an existing directory, the plugin will
   * handle this automatically.
   *
   * @return the path to the directory.
   */
  abstract Collection<Path> defaultSourceDirectories();

  /**
   * Provides the default output directory to write generated code to.
   *
   * <p>This does not need to point to an existing directory, the plugin will
   * handle this automatically.
   *
   * @return the path to the directory.
   */
  abstract Path defaultOutputDirectory();

  /**
   * The default dependency scopes used for resolution.
   *
   * @return the set of dependency scopes used for resolution.
   */
  abstract Set<String> defaultDependencyScopes();

  /**
   * Provides the registrar for output descriptor files to attach them to the Maven project
   * as additional artifacts.
   *
   * @return the registrar to use.
   */
  abstract OutputDescriptorAttachmentRegistrar outputDescriptorAttachmentRegistrar();

  /**
   * Provides the source root registrar for this Mojo.
   *
   * <p>This specifies where to attach generated sources to in order for it
   * to be included as part of the compilation for main or test sources.
   *
   * @return the registrar to use.
   */
  abstract SourceRootRegistrar sourceRootRegistrar();

  /*
   * Core implementation.
   */

  /**
   * Execute the plugin and generate sources.
   *
   * @throws MojoExecutionException if execution fails.
   * @throws MojoFailureException   if an error occurs.
   */
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (skip) {
      log.info("User has requested to skip execution of this goal.");
      return;
    }

    var enabledLanguages = Language.setBuilder()
        .addIf(javaEnabled, Language.JAVA)
        .addIf(kotlinEnabled, Language.KOTLIN)
        .addIf(pythonEnabled, Language.PYTHON)
        .addIf(pythonStubsEnabled, Language.PYTHON_STUBS)
        .addIf(rubyEnabled, Language.RUBY)
        .build();

    var request = ImmutableGenerationRequest.builder()
        .arguments(nonNullList(arguments))
        .cleanOutputDirectories(cleanOutputDirectories)
        .dependencyResolutionDepth(dependencyResolutionDepth)
        .dependencyScopes(dependencyScopes())
        .embedSourcesInClassOutputs(embedSourcesInClassOutputs)
        .environmentVariables(nonNullMap(environmentVariables))
        .enabledLanguages(enabledLanguages)
        .excludes(nonNullList(excludes))
        .failOnMissingSources(failOnMissingSources)
        .failOnMissingTargets(failOnMissingTargets)
        .fatalWarnings(fatalWarnings)
        .ignoreProjectDependencies(ignoreProjectDependencies)
        .importDependencies(nonNullList(importDependencies))
        .importPaths(determinePaths(importPaths, List::of))
        .includes(nonNullList(includes))
        .incrementalCompilationEnabled(incrementalCompilation)
        .liteEnabled(liteOnly)
        .outputDescriptorAttached(outputDescriptorAttached)
        .outputDescriptorAttachmentClassifier(outputDescriptorAttachmentClassifier)
        .outputDescriptorAttachmentRegistrar(outputDescriptorAttachmentRegistrar())
        .outputDescriptorAttachmentType(outputDescriptorAttachmentType)
        .outputDescriptorFile(outputDescriptorFile)
        .outputDescriptorIncludeImports(outputDescriptorIncludeImports)
        .outputDescriptorIncludeSourceInfo(outputDescriptorIncludeSourceInfo)
        .outputDescriptorRetainOptions(outputDescriptorRetainOptions)
        .outputDirectory(outputDirectory())
        .protocDigest(protocDigest)
        .protocPlugins(protocPlugins())
        .protocVersion(protoc())
        .registerAsCompilationRoot(registerAsCompilationRoot)
        .sanctionedExecutablePath(sanctionedExecutablePath)
        .sourceDependencies(nonNullList(sourceDependencies))
        .sourceDescriptorDependencies(nonNullList(sourceDescriptorDependencies))
        .sourceDescriptorPaths(determinePaths(sourceDescriptorPaths, List::of))
        .sourceDirectories(determinePaths(sourceDirectories, this::defaultSourceDirectories))
        .sourceRootRegistrar(sourceRootRegistrar())
        .build();

    GenerationResult result;

    try {
      result = sourceCodeGenerator.generate(request);
    } catch (Exception ex) {
      // Log the message separately so that it appears even if the user did not pass --errors
      // to Maven.
      log.error("Generation aborted due to an unexpected error - {}", ex, ex);
      throw new MojoFailureException(
          "Generation aborted due to an unexpected error - " + ex,
          ex
      );
    }

    if (!result.isOk()) {
      log.error("Generation failed - {}", result);
      throw new MojoExecutionException("Generation failed - " + result);
    }

    log.info("{}", result);
  }

  private Set<String> dependencyScopes() {
    return Optional.ofNullable(dependencyScopes)
        .filter(not(Set::isEmpty))
        .orElseGet(this::defaultDependencyScopes);
  }

  private Path outputDirectory() {
    return Optional.ofNullable(outputDirectory)
        .orElseGet(this::defaultOutputDirectory);
  }

  private String protoc() {
    // Give precedence to overriding the protobuf.compiler.version via the command line
    // in case the Maven binaries are incompatible with the current system.
    var overriddenVersion = System.getProperty(COMPILER_VERSION_PROPERTY);
    return overriddenVersion == null
        ? requireNonNull(protoc, "<protoc/> has not been set")
        : overriddenVersion;
  }

  @Deprecated(forRemoval = true)
  private Collection<ProtocPlugin> protocPlugins() {
    var allPlugins = new ArrayList<ProtocPlugin>();
    allPlugins.addAll(nonNullList(plugins));
    allPlugins.addAll(nonNullList(binaryMavenPlugins));
    allPlugins.addAll(nonNullList(binaryPathPlugins));
    allPlugins.addAll(nonNullList(binaryUrlPlugins));
    allPlugins.addAll(nonNullList(jvmMavenPlugins));
    return Collections.unmodifiableList(allPlugins);
  }

  private Collection<Path> determinePaths(
      @Nullable Collection<Path> inputPaths,
      Supplier<Collection<Path>> defaultIfMissing
  ) {
    var transformed = Optional.ofNullable(inputPaths)
        .filter(not(Collection::isEmpty))
        .stream()
        .flatMap(Collection::stream)
        .toList();

    var finalValue = transformed.isEmpty()
        ? defaultIfMissing.get()
        : transformed;

    return finalValue.stream()
        .filter(Files::exists)
        .toList();
  }

  private <L> List<L> nonNullList(@Nullable List<L> list) {
    return requireNonNullElseGet(list, List::of);
  }

  private <K, V> Map<K, V> nonNullMap(@Nullable Map<K, V> map) {
    return requireNonNullElseGet(map, Map::of);
  }
}
