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

package io.github.ascopes.protobufmavenplugin.mojo;

import static java.util.Objects.requireNonNull;
import static java.util.Objects.requireNonNullElseGet;
import static java.util.function.Predicate.not;

import io.github.ascopes.protobufmavenplugin.dependencies.DependencyResolutionDepth;
import io.github.ascopes.protobufmavenplugin.dependencies.MavenDependencyBean;
import io.github.ascopes.protobufmavenplugin.dependencies.ResolutionException;
import io.github.ascopes.protobufmavenplugin.generation.ImmutableGenerationRequest;
import io.github.ascopes.protobufmavenplugin.generation.Language;
import io.github.ascopes.protobufmavenplugin.generation.SourceCodeGenerator;
import io.github.ascopes.protobufmavenplugin.generation.SourceRootRegistrar;
import io.github.ascopes.protobufmavenplugin.plugins.MavenProtocPluginBean;
import io.github.ascopes.protobufmavenplugin.plugins.PathProtocPluginBean;
import io.github.ascopes.protobufmavenplugin.plugins.UrlProtocPluginBean;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
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
  private static final String PROTOBUF_COMPILER_VERSION = "protobuf.compiler.version";
  private static final String PROTOBUF_SKIP = "protobuf.skip";

  private final Logger log;

  public AbstractGenerateMojo() {
    // Use the implementation class to mark the logger.
    log = LoggerFactory.getLogger(getClass());
  }

  ///
  /// MOJO dependencies.
  ///

  /**
   * The source code generator.
   */
  @Component
  SourceCodeGenerator sourceCodeGenerator;

  /**
   * The active Maven project.
   */
  @Component
  MavenProject mavenProject;

  ///
  /// MOJO parameters.
  ///

  /**
   * Binary plugins to use with the protobuf compiler, sourced from a Maven repository.
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
   * {@code jvmMavenPlugin} may be more preferrable.
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
   *   <li>{@code skip} - set to {@code true} to skip invoking this plugin -
   *       useful if you want to control whether the plugin runs via a
   *       property - optional.</li>
   * </ul>
   *
   * @since 0.3.0
   */
  @Parameter
  @Nullable List<MavenProtocPluginBean> binaryMavenPlugins;

  /**
   * Binary plugins to use with the protobuf compiler, sourced from the system {@code PATH}.
   *
   * <p>For example:
   * <pre>{@code
   * <binaryPathPlugins>
   *   <binaryPathPlugin>
   *     <name>protoc-gen-grpc-java</name>
   *   </binaryPathPlugin>
   *   <binaryPathPlugin>
   *     <name>protoc-gen-something-else</name>
   *     <options>foo=bar</options>
   *   </binaryPathPlugin>
   * </binaryPathPlugins>
   * }</pre>
   *
   * <p>Prior to v2.0.0, this attribute was a list of strings.
   *
   * <p>Objects support the following attributes:
   *
   * <ul>
   *   <li>{@code name} - the name of the binary to resolve.</li>
   *   <li>{@code options} - a string of options to pass to the plugin
   *       - optional.</li>
   *   <li>{@code skip} - set to {@code true} to skip invoking this plugin -
   *       useful if you want to control whether the plugin runs via a
   *       property - optional.</li>
   * </ul>
   *
   * @since 2.0.0
   */
  @Parameter
  @Nullable List<PathProtocPluginBean> binaryPathPlugins;

  /**
   * Binary plugins to use with the protobuf compiler, specified as a valid URL.
   *
   * <p>This includes support for:
   *
   * <ul>
   *   <li>Local file system objects, specified using {@code file://path/to/file}</li>
   *   <li>HTTP resources, specified using {@code http://example.website/path/to/file}</li>
   *   <li>HTTPS resources, specified using {@code https://example.website/path/to/file}</li>
   *   <li>FTP resources, specified using {@code ftp://example.server/path/to/file}</li>
   * </ul>
   *
   * <p>For example:
   * <pre>{@code
   *   <binaryUrlPlugins>
   *     <binaryUrlPlugin>
   *       <url>ftp://myorganisation.org/protoc/plugins/myplugin.exe</url>
   *     </binaryUrlPlugin>
   *     <binaryUrlPlugin>
   *       <url>ftp://myorganisation.org/protoc/plugins/myplugin2.exe</url>
   *       <options>foo=bar</options>
   *     </binaryUrlPlugin>
   *   </binaryUrlPlugins>
   * }</pre>
   *
   * <p>Prior to v2.0.0, this attribute was a list of URLs.
   *
   * <p>Objects support the following attributes:
   *
   * <ul>
   *   <li>{@code url} - the URL to resolve.</li>
   *   <li>{@code options} - a string of options to pass to the plugin
   *       - optional.</li>
   *   <li>{@code skip} - set to {@code true} to skip invoking this plugin -
   *       useful if you want to control whether the plugin runs via a
   *       property - optional.</li>
   * </ul>
   *
   * @since 2.0.0
   */
  @Parameter
  @Nullable List<UrlProtocPluginBean> binaryUrlPlugins;

  /**
   * The scope to resolve dependencies with.
   *
   * <p>Supported values:
   *
   * <ul>
   *   <li><code>TRANSITIVE</code> - resolve all dependencies.</li>
   *   <li><code>DIRECT</code> - only resolve dependencies that were explicitly specified.</li>
   * </ul>
   *
   * @since 1.2.0
   */
  @Parameter(defaultValue = DEFAULT_TRANSITIVE)
  DependencyResolutionDepth dependencyResolutionDepth;

  /**
   * Set whether to attach all compiled protobuf sources to the output of this
   * Maven project so that they are included in any generated JAR.
   *
   * <p>Note that if you are using dependencies as sources, then those will also
   * be attached, and may have license implications. Therefore, this will default
   * to {@code false}.
   *
   * @since 2.1.0
   */
  @Parameter(defaultValue = DEFAULT_FALSE)
  boolean embedSourcesInClassOutputs;

  /**
   * Whether to fail on missing sources.
   *
   * <p>If no sources are detected, it is usually a sign that this plugin
   * is misconfigured, or that you are including this plugin in a project that does not need it. For
   * this reason, the plugin defaults this setting to being enabled. If you wish to not fail, you
   * can explicitly set this to false instead.
   *
   * @since 0.5.0
   */
  @Parameter(defaultValue = DEFAULT_TRUE)
  boolean failOnMissingSources;

  /**
   * Whether to fail if no output languages and no plugins are enabled.
   *
   * <p>This defaults to {@code true}, but may be set to {@code false} if all plugins are optional
   * and no languages are enabled.
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
   * Whether to ignore the {@code <dependencies/>} blocks in the Maven project when discovering
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
   * <p>If you wish to depend on a JAR containing protobuf sources, add it as a dependency
   * with the {@code provided} or {@code test} scope instead, or use {@code importDependencies}.
   *
   * @since 0.1.0
   */
  @Parameter
  @Nullable List<File> importPaths;

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
   *   <li>{@code options} - a string of options to pass to the plugin
   *       - optional.</li>
   *   <li>{@code skip} - set to {@code true} to skip invoking this plugin -
   *       useful if you want to control whether the plugin runs via a
   *       property - optional.</li>
   * </ul>
   *
   * @since 0.3.0
   */
  @Parameter
  @Nullable List<MavenProtocPluginBean> jvmMavenPlugins;

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
  @Parameter(defaultValue = DEFAULT_FALSE)
  boolean liteOnly;

  /**
   * Override the directory to output generated code to.
   *
   * <p>Leave unspecified or explicitly null to use the default for the
   * goal. This defaults to the Maven generated sources directory within {@code target/}.
   *
   * @since 0.1.0
   */
  @Parameter
  @Nullable File outputDirectory;

  /**
   * Specifies where to find {@code protoc} or which version to download.
   *
   * <p>This usually should correspond to the version of {@code protobuf-java} or similar that
   * is in use.
   *
   * <p>If set to "{@code PATH}", then {@code protoc} is resolved from the system path rather than
   * being downloaded. This is useful if you need to use an unsupported architecture/OS, or a
   * development version of {@code protoc}.
   *
   * <p>As of v0.4.0, you can also specify a URL that points to:
   *
   * <ul>
   *   <li>Local file system objects, specified using {@code file://path/to/file}</li>
   *   <li>HTTP resources, specified using {@code http://example.website/path/to/file}</li>
   *   <li>HTTPS resources, specified using {@code https://example.website/path/to/file}</li>
   *   <li>FTP resources, specified using {@code ftp://example.server/path/to/file}</li>
   * </ul>
   *
   * <p>Note that specifying {@code -Dprotobuf.compiler.version} in the {@code MAVEN_OPTS} or on
   * the command line overrides the version specified in the POM. This enables users to easily
   * override the version of {@code protoc} in use if their system is unable to support the
   * version specified in the POM. Termux users in particular will find
   * {@code -Dprotobuf.compiler.version=PATH} to be useful, due to platform limitations with
   * {@code libpthread} that can result in {@code SIGSYS} (Bad System Call) being raised.
   *
   * <p>Prior to v2.0.0, this parameter was named {@code protoc.version} when specified on the
   * commandline via JVM properties. This has been changed in v2.0.0 to
   * {@code protobuf.compiler.version} for consistency and to reduce naming collisions with
   * user-specified properties.
   *
   * @since 0.0.1
   */
  @Parameter(required = true, property = PROTOBUF_COMPILER_VERSION)
  String protocVersion;

  /**
   * Whether to register the output directories as compilation roots with Maven.
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
   * Whether to skip the plugin execution entirely.
   *
   * @since 2.0.0
   */
  @Parameter(defaultValue = DEFAULT_FALSE, property = PROTOBUF_SKIP)
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
   * </ul>
   *
   * @since 1.2.0
   */
  @Parameter
  @Nullable List<MavenDependencyBean> sourceDependencies;

  /**
   * Override the source directories to compile from.
   *
   * <p>Leave unspecified or explicitly null/empty to use the defaults.
   *
   * @since 0.0.1
   */
  @Parameter
  @Nullable List<File> sourceDirectories;

  ///
  /// Language enabling flags
  ///

  /**
   * Enable generating C++ sources from the protobuf sources.
   *
   * @since 1.1.0
   */
  @Parameter(defaultValue = DEFAULT_FALSE)
  boolean cppEnabled;

  /**
   * Enable generating C# sources from the protobuf sources.
   *
   * @since 1.1.0
   */
  @Parameter(defaultValue = DEFAULT_FALSE)
  boolean csharpEnabled;

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
   * Enable generating Kotlin API wrapper code around the generated Java code.
   *
   * <p>This may require {@code javaEnabled} to also be {@code true}, otherwise compilation
   * may fail unless other sources are generated to replace the expected Java ones.
   *
   * @since 0.1.0
   */
  @Parameter(defaultValue = DEFAULT_FALSE)
  boolean kotlinEnabled;

  /**
   * Enable generating Objective-C sources from the protobuf sources.
   *
   * @since 1.1.0
   */
  @Parameter(defaultValue = DEFAULT_FALSE)
  boolean objcEnabled;

  /**
   * Enable generating PHP sources from the protobuf sources.
   *
   * @since 1.1.0
   */
  @Parameter(defaultValue = DEFAULT_FALSE)
  boolean phpEnabled;

  /**
   * Enable generating Python sources from the protobuf sources.
   *
   * <p>If you enable this, you probably will also want to enable Python stubs
   * to enable generating {@code *.pyi} files for static type checkers.
   *
   * @since 1.1.0
   */
  @Parameter(defaultValue = DEFAULT_FALSE)
  boolean pythonEnabled;

  /**
   * Enable generating Python stubs ({@code *.pyi} files) for static typechecking from the protobuf
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
   * Enable generating Ruby sources from the protobuf sources.
   *
   * @since 1.1.0
   */
  @Parameter(defaultValue = DEFAULT_FALSE)
  boolean rubyEnabled;

  /**
   * Enable generating Rust sources from the protobuf sources.
   *
   * @since 1.1.0
   */
  @Parameter(defaultValue = DEFAULT_FALSE)
  boolean rustEnabled;

  ///
  /// Internal functionality
  ///

  /**
   * Provides the source root registrar for this Mojo.
   *
   * <p>This specifies where to attach generated sources to in order for it
   * to be included as part of the compilation for main or test sources.
   *
   * @return the registrar to use.
   */
  abstract SourceRootRegistrar sourceRootRegistrar();

  /**
   * Provides the default source directory to read protobuf sources from.
   *
   * <p>This does not need to point to an existing directory, the plugin will
   * handle this automatically.
   *
   * @return the path to the directory.
   */
  abstract Path defaultSourceDirectory();

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
   * Execute the plugin and generate sources.
   *
   * @throws MojoExecutionException if execution fails.
   * @throws MojoFailureException   if an error occurs.
   */
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    if (skip) {
      log.info("Plugin execution is skipped");
      return;
    }

    var enabledLanguages = Language.setBuilder()
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
        .binaryMavenPlugins(nonNullList(binaryMavenPlugins))
        .binaryPathPlugins(nonNullList(binaryPathPlugins))
        .binaryUrlPlugins(nonNullList(binaryUrlPlugins))
        .dependencyResolutionDepth(dependencyResolutionDepth)
        .enabledLanguages(enabledLanguages)
        .jvmMavenPlugins(nonNullList(jvmMavenPlugins))
        .importDependencies(nonNullList(importDependencies))
        .importPaths(importPaths())
        .isEmbedSourcesInClassOutputs(embedSourcesInClassOutputs)
        .isFailOnMissingSources(failOnMissingSources)
        .isFailOnMissingTargets(failOnMissingTargets)
        .isFatalWarnings(fatalWarnings)
        .isIgnoreProjectDependencies(ignoreProjectDependencies)
        .isLiteEnabled(liteOnly)
        .isRegisterAsCompilationRoot(registerAsCompilationRoot)
        .outputDirectory(outputDirectory())
        .protocVersion(protocVersion())
        .sourceDependencies(nonNullList(sourceDependencies))
        .sourceRootRegistrar(sourceRootRegistrar())
        .sourceRoots(sourceDirectories())
        .build();

    try {
      if (!sourceCodeGenerator.generate(request)) {
        throw new MojoExecutionException("Protoc invocation failed");
      }
    } catch (ResolutionException | IOException ex) {
      var mojoFailureException = new MojoFailureException(this, ex.getMessage(), ex.getMessage());
      mojoFailureException.initCause(ex);
      throw mojoFailureException;
    }
  }

  private Path outputDirectory() {
    return Optional.ofNullable(outputDirectory)
        .map(File::toPath)
        .orElseGet(this::defaultOutputDirectory);
  }

  private Collection<Path> sourceDirectories() {
    var transformedSourceDirectories = Optional.ofNullable(sourceDirectories)
        .filter(not(Collection::isEmpty))
        .stream()
        .flatMap(Collection::stream)
        .map(File::toPath)
        .collect(Collectors.toUnmodifiableList());

    var finalDirectories = transformedSourceDirectories.isEmpty()
        ? List.of(defaultSourceDirectory())
        : transformedSourceDirectories;

    return finalDirectories.stream()
        .filter(path -> {
          if (Files.notExists(path)) {
            log.warn("Ignoring source directory {} as it does not appear to exist", path);
            return false;
          }
          return true;
        })
        .collect(Collectors.toUnmodifiableList());
  }

  private Collection<Path> importPaths() {
    return nonNullList(importPaths).stream()
        .map(File::toPath)
        .collect(Collectors.toUnmodifiableList());
  }

  private String protocVersion() {
    // Give precedence to overriding the protobuf.compiler.version via the command line
    // in case the Maven binaries are incompatible with the current system.
    var overriddenVersion = System.getProperty(PROTOBUF_COMPILER_VERSION);
    return overriddenVersion == null
        ? requireNonNull(protocVersion, "protocVersion has not been set")
        : overriddenVersion;
  }

  private <T> List<T> nonNullList(@Nullable List<T> list) {
    return requireNonNullElseGet(list, List::of);
  }
}
