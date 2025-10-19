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
import io.github.ascopes.protobufmavenplugin.dependencies.MavenDependency;
import io.github.ascopes.protobufmavenplugin.digests.Digest;
import io.github.ascopes.protobufmavenplugin.generation.GenerationResult;
import io.github.ascopes.protobufmavenplugin.generation.ImmutableGenerationRequest;
import io.github.ascopes.protobufmavenplugin.generation.Language;
import io.github.ascopes.protobufmavenplugin.generation.OutputDescriptorAttachmentRegistrar;
import io.github.ascopes.protobufmavenplugin.generation.ProtobufBuildOrchestrator;
import io.github.ascopes.protobufmavenplugin.generation.SourceRootRegistrar;
import io.github.ascopes.protobufmavenplugin.plugins.MavenProtocPlugin;
import io.github.ascopes.protobufmavenplugin.plugins.PathProtocPlugin;
import io.github.ascopes.protobufmavenplugin.plugins.UriProtocPlugin;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
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
   * <p>Note that generally, you should not need to use this. It is useful, however, if your
   * use-case is not covered by other configuration parameters in this goal.
   *
   * <p>Configuring arguments that are covered by other parameters in this goal is undefined
   * behaviour and should be avoided.
   *
   * @since 3.8.0
   */
  @Parameter
  @Nullable List<String> arguments;

  /**
   * Binary plugins to use with the protobuf compiler, sourced from a Maven repository.
   *
   * <p>Binary plugins are {@code protoc} plugins that are regular executables, and thus can work
   * with {@code protoc} out of the box.
   *
   * <p>Plugin artifacts must be a <strong>native executable</strong>. By default, the OS and CPU
   * architecture is automatically generated and injected in the classifier if the classifier and
   * type are not provided explicitly.
   *
   * <p>For example:
   * <pre>{@code
   * <binaryMavenPlugins>
   *   <binaryMavenPlugin>
   *     <groupId>com.salesforce.servicelibs</groupId>
   *     <artifactId>reactor-grpc</artifactId>
   *     <version>1.2.4</version>
   *   </binaryMavenPlugin>
   * </binaryMavenPlugins>
   * }</pre>
   *
   * <p>If you have a Java-based plugin that does not distribute a native
   * executable, or are using a more obscure system architecture, then using a
   * {@code jvmMavenPlugin} may be more preferable.
   *
   * <p>Objects support the following attributes:
   *
   * <ul>
   *   <li>{@code groupId} - the group ID - required</li>
   *   <li>{@code artifactId} - the artifact ID - required</li>
   *   <li>{@code version} - the version - required</li>
   *   <li>{@code type} - the artifact type - optional</li>
   *   <li>{@code classifier} - the artifact classifier - optional</li>
   *   <li>{@code options} - a string of options to pass to the plugin
   *       - optional.</li>
   *   <li>{@code order} - an integer order to run the plugins in. Defaults
   *       to 0. Higher numbers run later than lower numbers. The built-in
   *       code generators in {@code protoc} and descriptor generation has
   *       an order of 0.</li>
   *   <li>{@code skip} - set to {@code true} to skip invoking this plugin -
   *       useful if you want to control whether the plugin runs via a
   *       property - optional.</li>
   *   <li>{@code outputDirectory} - where to write the generated outputs to.
   *       - if unspecified, then the {@link #outputDirectory} on the Maven
   *       plugin is used instead - optional.</li>
   *   <li>{@code registerAsCompilationRoot} - whether to register the output
   *       directory as a source directory for later compilation steps. If
   *       unspecified/null, then {@link #registerAsCompilationRoot} is used.
   *       If the {@code outputDirectory} is not overridden, this setting has
   *       no effect, and the project-wide setting is used. If explicitly
   *       specified, then the project setting is ignored in favour of this
   *       value instead.</li>
   * </ul>
   *
   * @since 0.3.0
   */
  @Parameter
  @Nullable List<MavenProtocPlugin> binaryMavenPlugins;

  /**
   * Binary plugins to use with the protobuf compiler, sourced from the system {@code PATH}.
   *
   * <p>Binary plugins are {@code protoc} plugins that are regular executables, and thus can work
   * with {@code protoc} out of the box.
   *
   * <p>For example:
   * <pre>{@code
   * <binaryPathPlugins>
   *   <binaryPathPlugin>
   *     <name>protoc-gen-grpc-java</name>
   *   </binaryPathPlugin>
   *   <binaryPathPlugin>
   *     <name>protoc-gen-something-else</name>
   *     <options>foo=bar,baz=bork</options>
   *   </binaryPathPlugin>
   * </binaryPathPlugins>
   * }</pre>
   *
   * <p>Objects support the following attributes:
   *
   * <ul>
   *   <li>{@code name} - the name of the binary to resolve.</li>
   *   <li>{@code options} - a string of options to pass to the plugin
   *       - optional.</li>
   *   <li>{@code order} - an integer order to run the plugins in. Defaults
   *       to 0. Higher numbers run later than lower numbers. The built-in
   *       code generators in {@code protoc} and descriptor generation has
   *       an order of 0.</li>
   *   <li>{@code skip} - set to {@code true} to skip invoking this plugin -
   *       useful if you want to control whether the plugin runs via a
   *       property - optional.</li>
   *   <li>{@code outputDirectory} - where to write the generated outputs to.
   *       - if unspecified, then the {@link #outputDirectory} on the Maven
   *       plugin is used instead - optional.</li>
   *   <li>{@code registerAsCompilationRoot} - whether to register the output
   *       directory as a source directory for later compilation steps. If
   *       unspecified/null, then {@link #registerAsCompilationRoot} is used.
   *       If the {@code outputDirectory} is not overridden, this setting has
   *       no effect, and the project-wide setting is used. If explicitly
   *       specified, then the project setting is ignored in favour of this
   *       value instead.</li>
   * </ul>
   *
   * <p>On Linux, macOS, and other POSIX-like systems, resolution looks for an executable
   * binary matching the exact name in any directory in the {@code $PATH} environment variable.
   *
   * <p>On Windows, the case-insensitive {@code %PATH%} environment variable is searched for an
   * executable that matches the name, ignoring case and any file extension. The file
   * extension is expected to match any extension in the {@code %PATHEXT%} environment variable.
   *
   * @since 2.0.0
   */
  @Parameter
  @Nullable List<PathProtocPlugin> binaryPathPlugins;

  /**
   * Binary plugins to use with the protobuf compiler, specified as a valid URL.
   *
   * <p>Binary plugins are {@code protoc} plugins that are regular executables, and thus can work
   * with {@code protoc} out of the box.
   *
   * <p>For example:
   * <pre>{@code
   * <binaryUrlPlugins>
   *   <!-- FTP resource -->
   *   <binaryUrlPlugin>
   *     <url>ftp://myorganisation.org/protoc/plugins/myplugin.exe</url>
   *   </binaryUrlPlugin>
   *
   *   <!-- HTTP resource with custom options-->
   *   <binaryUrlPlugin>
   *     <url>https://myorganisation.org/protoc/plugins/myplugin2.exe</url>
   *     <options>foo=bar,baz=bork</options>
   *   </binaryUrlPlugin>
   *
   *   <!-- HTTP resource that is a ZIP holding the binary we want. -->
   *   <binaryUrlPlugin>
   *     <url>zip:https://myorganisation.org/protoc/plugins/myplugin3.zip!/protoc-gen-something.exe</url>
   *   </binaryUrlPlugin>
   * </binaryUrlPlugins>
   * }</pre>
   *
   * <p>See the user guide for details on the supported protocols.
   *
   * <p>Objects support the following attributes:
   *
   * <ul>
   *   <li>{@code url} - the URL to resolve.</li>
   *   <li>{@code options} - a string of options to pass to the plugin
   *       - optional.</li>
   *   <li>{@code order} - an integer order to run the plugins in. Defaults
   *       to 0. Higher numbers run later than lower numbers. The built-in
   *       code generators in {@code protoc} and descriptor generation has
   *       an order of 0.</li>
   *   <li>{@code skip} - set to {@code true} to skip invoking this plugin -
   *       useful if you want to control whether the plugin runs via a
   *       property - optional.</li>
   *   <li>{@code outputDirectory} - where to write the generated outputs to.
   *       - if unspecified, then the {@link #outputDirectory} on the Maven
   *       plugin is used instead - optional.</li>
   *   <li>{@code registerAsCompilationRoot} - whether to register the output
   *       directory as a source directory for later compilation steps. If
   *       unspecified/null, then {@link #registerAsCompilationRoot} is used.
   *       If the {@code outputDirectory} is not overridden, this setting has
   *       no effect, and the project-wide setting is used. If explicitly
   *       specified, then the project setting is ignored in favour of this
   *       value instead.</li>
   *   <li>{@code digest} - an optional digest to verify the binary against.
   *       If specified, this is a string in the format {@code sha512:1a2b3c4d...},
   *       using any supported message digest provided by your JDK (e.g. {@code md5},
   *       {@code sha1}, {@code sha256}, {@code sha512}, etc.).</li>
   * </ul>
   *
   * @since 2.0.0
   */
  @Parameter
  @Nullable List<UriProtocPlugin> binaryUrlPlugins;

  /**
   * If {@code true}, all output directories will be cleared before {@code protoc}
   * is invoked.
   *
   * <p>Enable this to force a clean build on each invocation.
   *
   * <p>Note that this is ignored if {@code incrementalCompilation} is enabled.
   *
   * @since 3.6.0
   */
  @Parameter(defaultValue = DEFAULT_FALSE)
  boolean cleanOutputDirectories;

  /**
   * Enable generating C++ sources and headers from the protobuf sources.
   *
   * @deprecated will be removed in v4.0.0. Users wishing to generate C++ sources
   *     should use the {@code arguments} to specify {@code --cpp_out=path}.
   * @since 1.1.0
   */
  @Deprecated(since = "3.10.1", forRemoval = true)
  @Parameter(defaultValue = DEFAULT_FALSE)
  boolean cppEnabled;

  /**
   * Enable generating C# sources from the protobuf sources.
   *
   * @deprecated will be removed in v4.0.0. Users wishing to generate C# sources
   *     should use the {@code arguments} to specify {@code --csharp_out=path}.
   * @since 1.1.0
   */
  @Deprecated(since = "3.10.1", forRemoval = true)
  @Parameter(defaultValue = DEFAULT_FALSE)
  boolean csharpEnabled;

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
   * Enable attaching all compiled protobuf sources to the output of this
   * Maven project so that they are included in any generated JAR.
   *
   * <p>Note that if you are using dependencies as sources, then those will also
   * be attached, and may have license implications.
   *
   * @since 2.1.0
   */
  @Parameter(defaultValue = DEFAULT_FALSE)
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
   * <p>Note that this will not support overriding aspects like the system path, as those are
   * resolved statically prior to any invocation.
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
   * <p>For example, if you wanted to not compile files named {@code user.proto},
   * {@code message.proto}, or {@code service.proto}, you could use the following
   * configuration.
   *
   * <pre><code>&lt;excludes&gt;
   *   &lt;exclude&gt;**&#47;user.proto&lt;/exclude&gt;
   *   &lt;exclude&gt;**&#47;message.proto&lt;/exclude&gt;
   *   &lt;exclude&gt;**&#47;service.proto&lt;/exclude&gt;
   * &lt;/excludes&gt;
   * </code></pre>
   *
   * <p>Use {@code includes} if you wish to instead include files for compilation.
   *
   * @since 2.2.0
   */
  @Parameter(property = "protobuf.compiler.excludes")
  @Nullable List<String> excludes;

  /**
   * Fail the build if any invalid direct or transitive dependencies are encountered.
   *
   * <p>If {@code true}, the build will be aborted with an error if any invalid dependency is
   * encountered.
   *
   * <p>If {@code false}, then the build will report any invalid dependencies as errors in the logs,
   * before proceeding with the build. Any invalid dependencies will be discarded.
   *
   * <p>Prior to {@code v2.4.0}, any invalid dependencies would result in an error being raised
   * and the build being aborted. In {@code v2.4.0}, this has been relaxed.
   *
   * @deprecated will be removed in v4.0.0: invalid dependencies will be ignored.
   * @since 2.4.0
   */
  @Deprecated(since = "3.10.1", forRemoval = true)
  @Parameter(defaultValue = DEFAULT_FALSE)
  boolean failOnInvalidDependencies;

  /**
   * Fail on missing sources.
   *
   * <p>If no sources are detected, it is usually a sign that this plugin
   * is misconfigured, or that you are including this plugin in a project that does not need it. For
   * this reason, the plugin defaults this setting to being enabled. If you wish to not fail, you
   * can explicitly set this to {@code false} instead.
   *
   * @deprecated will be removed in v4.0.0: missing sources will always be an error.
   * @since 0.5.0
   */
  @Deprecated(since = "3.10.1", forRemoval = true)
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
   * <p>Generally you will want to leave this enabled unless you have a very specific case where
   * you wish to take control of how dependency resolution works.
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
  @Nullable List<MavenDependency> importDependencies;

  /**
   * Specify additional paths to import protobuf sources from on the local file system.
   *
   * <p>These will not be compiled into Java sources directly.
   *
   * <p>If you wish to depend on a JAR Maven artifact containing protobuf sources, add it as a
   * dependency with the {@code provided} or {@code test} scope instead, or use
   * {@code importDependencies} rather than this parameter.
   *
   * <p>Import paths can also be specified as paths to ZIP or JAR archives on the local
   * file system. This plugin will extract any {@code *.proto} files for you, and pass them to
   * {@code protoc}.
   *
   * <p>If you wish to also compile proto sources, use the {@code sourceDirectories} parameter
   * instead.
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
   * <p>For example, if you only wanted to compile files named {@code user.proto},
   * {@code message.proto}, or {@code service.proto}, you could use the following
   * configuration.
   *
   * <pre><code>&lt;includes&gt;
   *   &lt;include&gt;**&#47;user.proto&lt;/include&gt;
   *   &lt;include&gt;**&#47;message.proto&lt;/include&gt;
   *   &lt;include&gt;**&#47;service.proto&lt;/include&gt;
   * &lt;/includes&gt;
   * </code></pre>
   *
   * <p>Use {@code excludes} if you wish to instead omit files from compilation.
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
   * Additional <strong>pure-Java</strong> plugins to use with the protobuf compiler.
   *
   * <p>Unlike artifact-based plugins, these are pure Java JAR applications that abide by the
   * protoc compiler API, and will be provided to the compiler via generated scripts.
   *
   * <p>For example:
   * <pre>{@code
   * <jvmMavenPlugins>
   *   <jvmMavenPlugin>
   *     <groupId>com.salesforce.servicelibs</groupId>
   *     <artifactId>reactor-grpc</artifactId>
   *     <version>1.2.4</version>
   *   </jvmMavenPlugin>
   * </jvmMavenPlugins>
   * }</pre>
   *
   * <p>This mechanism allows plugin vendors to implement their plugins in
   * Java and just distribute platform-independent JAR instead.
   *
   * <p>Objects support the following attributes:
   *
   * <ul>
   *   <li>{@code groupId} - the group ID - required</li>
   *   <li>{@code artifactId} - the artifact ID - required</li>
   *   <li>{@code version} - the version - required</li>
   *   <li>{@code type} - the artifact type - optional</li>
   *   <li>{@code classifier} - the artifact classifier - optional</li>
   *   <li>{@code options} - a string of options to pass to the plugin. This
   *       uses the standard {@code protoc} interface for specifying options
   *       - optional.</li>
   *   <li>{@code order} - an integer order to run the plugins in. Defaults
   *       to 0. Higher numbers run later than lower numbers. The built-in
   *       code generators in {@code protoc} and descriptor generation has
   *       an order of 0.</li>
   *   <li>{@code skip} - set to {@code true} to skip invoking this plugin -
   *       useful if you want to control whether the plugin runs via a
   *       property - optional.</li>
   *   <li>{@code outputDirectory} - where to write the generated outputs to.
   *       - if unspecified, then the {@link #outputDirectory} on the Maven
   *       plugin is used instead - optional.</li>
   *   <li>{@code registerAsCompilationRoot} - whether to register the output
   *       directory as a source directory for later compilation steps. If
   *       unspecified/null, then {@link #registerAsCompilationRoot} is used.
   *       If the {@code outputDirectory} is not overridden, this setting has
   *       no effect, and the project-wide setting is used. If explicitly
   *       specified, then the project setting is ignored in favour of this
   *       value instead.</li>
   *   <li>{@code mainClass} - if the plugin is not an assembled JAR at the time
   *       the {@code protobuf-maven-plugin} is run, then you will need to provide
   *       the fully qualified class name of the plugin entrypoint here. This is
   *       usually only needed if you are creating the JVM plugin within the
   *       same project. If the plugin is an assembled JAR, then this option is
   *       optional, the {@code Main-Class} manifest entry will be used when
   *       present if this is not provided.</li>
   *   <li>{@code jvmArgs} - a list of commandline arguments to pass to the
   *       plugin process - optional.</li>
   *   <li>{@code jvmConfigArgs} - a list of commandline arguments to configure
   *       the JVM itself. This is used to control factors such as JIT compilation,
   *       JVM properties, heap size, etc. Users should leave this as the default
   *       value (which optimises for short-lived processes) unless they know
   *       exactly what they are doing - optional.
   * </ul>
   *
   * @since 0.3.0
   */
  @Parameter
  @Nullable List<MavenProtocPlugin> jvmMavenPlugins;

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
   * Generate Objective-C sources from the protobuf sources.
   *
   * @deprecated will be removed in v4.0.0. Users wishing to generate Objective-C sources
   *     should use the {@code arguments} to specify {@code --objc_out=path}.
   * @since 1.1.0
   */
  @Deprecated(since = "3.10.1", forRemoval = true)
  @Parameter(defaultValue = DEFAULT_FALSE)
  boolean objcEnabled;

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
   * Generate PHP sources from the protobuf sources.
   *
   * @deprecated will be removed in v4.0.0. Users wishing to generate PHP sources
   *     should use the {@code arguments} to specify {@code --php_out=path}.
   * @since 1.1.0
   */
  @Deprecated(since = "3.10.1", forRemoval = true)
  @Parameter(defaultValue = DEFAULT_FALSE)
  boolean phpEnabled;

  /**
   * Optional digest to verify {@code protoc} against.
   *
   * <p>Generally, you will not need to provide this, as the Maven Central
   * {@code protoc} binaries will already be digest-verified as part of distribution.
   * You may wish to specify this if you are using a {@code PATH}-based binary, or
   * using a URL for {@code protoc}.
   *
   * <p>This is a string in the format {@code sha512:1a2b3c...}, using any
   * message digest algorithm supported by your JDK.
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
   * being downloaded. This is useful if you need to use an unsupported architecture/OS, or a
   * development version of {@code protoc}.
   *
   * <p>You can also specify a URL. See the user guide for a list of supported protocols.
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
   * <strong>In v4.0.0, this will be renamed to {@code <protoc />}.</strong>
   *
   * @since 0.0.1
   */
  @Parameter(
      alias = "protoc",
      required = true,
      property = COMPILER_VERSION_PROPERTY
  )
  String protocVersion;

  /**
   * Generate Python sources from the protobuf sources.
   *
   * <p>If you enable this, you probably will also want to enable Python stubs
   * to enable generating {@code *.pyi} files for static type checkers.
   *
   * @since 1.1.0
   */
  @Parameter(defaultValue = DEFAULT_FALSE)
  boolean pythonEnabled;

  /**
   * Generate Python stubs ({@code *.pyi} files) for static typechecking from the protobuf
   * sources.
   *
   * <p>If you enable this, you probably will also want to enable Python itself
   * to get actual source code to accompany the stubs.
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
   * <p>Generally, you want to do this, but there may be edge cases where you
   * wish to control this behaviour manually instead. In this case, set this parameter to be
   * {@code false}.
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
   * Generate Rust sources from the protobuf sources.
   *
   * @deprecated will be removed in v4.0.0. Users wishing to generate Rust sources
   *     should use the {@code arguments} to specify {@code --rust_out=path}.
   * @since 1.1.0
   */
  @Deprecated(since = "3.10.1", forRemoval = true)
  @Parameter(defaultValue = DEFAULT_FALSE)
  boolean rustEnabled;

  /**
   * Corporate-sanctioned path to run native executables from.
   *
   * <p>Most users <strong>SHOULD NOT</strong> specify this.
   *
   * <p>If you operate in an overly locked-down corporate environment that disallows running
   * shell/batch scripts or native executables outside sanctioned locations on your local
   * file system, you can specify the path here either via this configuration parameter
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
  @Nullable List<MavenDependency> sourceDependencies;

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
   * <p>If you wish to use descriptor files from the local file system, use
   * the {@code sourceDescriptorPaths} parameter instead.
   *
   * @since 3.1.0
   */
  @Parameter
  @Nullable List<MavenDependency> sourceDescriptorDependencies;

  /**
   * The source directories to compile protobuf sources from.
   *
   * <p>Leave unspecified or explicitly null/empty to use the defaults.
   *
   * <p><strong>Note that specifying custom directories will override the default
   * directories rather than adding to them.</strong>
   *
   * <p>Source directories can also be specified as paths to ZIP or JAR archives on the local
   * file system. This plugin will extract any {@code *.proto} files for you, and pass them to
   * {@code protoc}.
   *
   * <p>If you wish to compile sources from within a Maven artifact holding a JAR or ZIP, use the
   * {@code sourceDependencies} parameter instead.
   *
   * <p>If you wish to compile sources from descriptor files from the local file system, use
   * the {@code sourceDescriptorPaths} parameter instead.
   *
   * <p>If you wish to compile sources from within a Maven artifact holding a protobuf descriptor
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
  @SuppressWarnings("removal")
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (Runtime.version().feature() < 17) {
      log.warn(
          "It looks like you are using a JDK older than Java 17. From v4.0.0 of this plugin, "
              + "you will need to run Maven with Java 17 or newer for the build to succeed. "
              + "Please consider updating to a newer JDK where possible."
      );
    }

    if (skip) {
      log.info("Execution of this plugin has been skipped in the configuration");
      return;
    }

    var enabledLanguages = Language.languageSet()
        .addIf(cppEnabled, Language.CPP)
        .addIf(csharpEnabled, Language.C_SHARP)
        .addIf(javaEnabled, Language.JAVA)
        .addIf(kotlinEnabled, Language.KOTLIN)
        .addIf(objcEnabled, Language.OBJECTIVE_C)
        .addIf(phpEnabled, Language.PHP)
        .addIf(pythonEnabled, Language.PYTHON)
        .addIf(pythonStubsEnabled, Language.PYI)
        .addIf(rubyEnabled, Language.RUBY)
        .addIf(rustEnabled, Language.RUST)
        .build();

    var request = ImmutableGenerationRequest.builder()
        .arguments(nonNullList(arguments))
        .binaryMavenPlugins(nonNullList(binaryMavenPlugins))
        .binaryPathPlugins(nonNullList(binaryPathPlugins))
        .binaryUrlPlugins(nonNullList(binaryUrlPlugins))
        .cleanOutputDirectories(cleanOutputDirectories)
        .dependencyResolutionDepth(dependencyResolutionDepth)
        .dependencyScopes(dependencyScopes())
        .embedSourcesInClassOutputs(embedSourcesInClassOutputs)
        .environmentVariables(nonNullMap(environmentVariables))
        .enabledLanguages(enabledLanguages)
        .excludes(nonNullList(excludes))
        .failOnInvalidDependencies(failOnInvalidDependencies)
        .failOnMissingSources(failOnMissingSources)
        .failOnMissingTargets(failOnMissingTargets)
        .fatalWarnings(fatalWarnings)
        .ignoreProjectDependencies(ignoreProjectDependencies)
        .importDependencies(nonNullList(importDependencies))
        .importPaths(determinePaths(importPaths, List::of))
        .includes(nonNullList(includes))
        .incrementalCompilationEnabled(incrementalCompilation)
        .jvmMavenPlugins(nonNullList(jvmMavenPlugins))
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
        .protocVersion(protocVersion())
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
      log.error("Generation failed due to an unexpected error - {}", ex.getMessage(), ex);
      throw new MojoFailureException(
          "Generation failed due to an unexpected error - " + ex,
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

  private String protocVersion() {
    // Give precedence to overriding the protobuf.compiler.version via the command line
    // in case the Maven binaries are incompatible with the current system.
    var overriddenVersion = System.getProperty(COMPILER_VERSION_PROPERTY);
    return overriddenVersion == null
        ? requireNonNull(protocVersion, "protocVersion has not been set")
        : overriddenVersion;
  }

  private static Collection<Path> determinePaths(
      @Nullable Collection<Path> inputPaths,
      Supplier<Collection<Path>> defaultIfMissing
  ) {
    var transformed = Optional.ofNullable(inputPaths)
        .filter(not(Collection::isEmpty))
        .stream()
        .flatMap(Collection::stream)
        .collect(Collectors.toUnmodifiableList());

    var finalValue = transformed.isEmpty()
        ? defaultIfMissing.get()
        : transformed;

    return finalValue.stream()
        .filter(path -> {
          if (Files.notExists(path)) {
            log.info("Ignoring non-existent path \"{}\"", path);
            return false;
          }
          return true;
        })
        .collect(Collectors.toUnmodifiableList());
  }

  private static <T> List<T> nonNullList(@Nullable List<T> list) {
    return requireNonNullElseGet(list, List::of);
  }

  private static <K, V> Map<K, V> nonNullMap(@Nullable Map<K, V> map) {
    return requireNonNullElseGet(map, Map::of);
  }
}
