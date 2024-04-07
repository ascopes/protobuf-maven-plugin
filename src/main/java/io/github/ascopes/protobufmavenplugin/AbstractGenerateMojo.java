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
import static java.util.Objects.requireNonNullElseGet;

import io.github.ascopes.protobufmavenplugin.dependency.ResolutionException;
import io.github.ascopes.protobufmavenplugin.generate.ImmutableGenerationRequest;
import io.github.ascopes.protobufmavenplugin.generate.SourceCodeGenerator;
import io.github.ascopes.protobufmavenplugin.generate.SourceRootRegistrar;
import io.github.ascopes.protobufmavenplugin.utils.FileUtils;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.jspecify.annotations.Nullable;

/**
 * Abstract base for a code generation Mojo that calls {@code protoc}.
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
   * <p>Note that specifying {@code -Dprotoc.version} in the {@code MAVEN_OPTS} or on the
   * command line overrides the version specified in the POM. This enables users to easily
   * override the version of {@code protoc} in use if their system is unable to support the
   * version specified in the POM. Termux users in particular will find
   * {@code -Dprotoc.version=PATH} to be useful, due to platform limitations with
   * {@code libpthread} that can result in {@code SIGSYS} (Bad System Call) being raised.
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
  private @Nullable List<File> sourceDirectories;

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
  @Parameter(defaultValue = "TRANSITIVE")
  private DependencyResolutionDepth dependencyResolutionDepth;

  /**
   * Specify additional paths to import protobuf sources from on the local file system.
   *
   * <p>These will not be compiled into Java sources directly.
   *
   * <p>If you wish to depend on a JAR containing protobuf sources, add it as a dependency
   * with the {@code provided} or {@code test} scope instead.
   *
   * <p>Prior to v1.2.0, this was called {@code additionalImportPaths}. This old name will
   * be maintained as a valid alias until v2.0.0.
   *
   * @since 0.1.0
   */
  @Parameter(alias = "additionalImportPaths")
  private @Nullable List<File> importPaths;

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
   * @since 0.3.0
   */
  @Parameter
  private @Nullable List<MavenArtifact> binaryMavenPlugins;

  /**
   * Binary plugins to use with the protobuf compiler, sourced from the system {@code PATH}.
   *
   * <p>For example:
   * <pre>{@code
   * <binaryPathPlugins>
   *   <binaryPathPlugin>protoc-gen-grpc-java</binaryPathPlugin>
   * </binaryPathPlugins>
   * }</pre>
   *
   * @since 0.3.0
   */
  @Parameter
  private @Nullable List<String> binaryPathPlugins;

  /**
   * Binary plugins to use with the protobuf compiler, specified as a valid URL.
   *
   * <p>This includes support for:
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
   *     <binaryUrlPlugin>ftp://myorganisation.org/protoc/plugins/myplugin.exe</binaryUrlPlugin>
   *   </binaryUrlPlugins>
   * }</pre>
   *
   * @since 0.4.0
   */
  @Parameter
  private @Nullable List<URL> binaryUrlPlugins;

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
   * @since 0.3.0
   */
  @Parameter
  private @Nullable List<MavenArtifact> jvmMavenPlugins;

  /**
   * Override the directory to output generated code to.
   *
   * <p>Leave unspecified or explicitly null to use the default for the
   * goal. This defaults to the Maven generated sources directory within {@code target/}.
   *
   * @since 0.1.0
   */
  @Parameter
  private @Nullable File outputDirectory;

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
  @Parameter(defaultValue = "true")
  private boolean failOnMissingSources;

  /**
   * Specify that any warnings emitted by {@code protoc} should be treated as errors and fail the
   * build.
   *
   * <p>Defaults to {@code false}.
   *
   * @since 0.0.1
   */
  @Parameter(defaultValue = "false")
  private boolean fatalWarnings;

  /**
   * Enable generating Java sources from the protobuf sources.
   *
   * <p>Defaults to {@code true}, although some users may wish to disable this
   * if using an alternative plugin that covers generating the code for models instead.
   *
   * @since 0.1.1
   */
  @Parameter(defaultValue = "true")
  private boolean javaEnabled;

  /**
   * Enable generating Kotlin API wrapper code around the generated Java code.
   *
   * <p>This may require {@code javaEnabled} to also be {@code true}, otherwise compilation
   * may fail unless other sources are generated to replace the expected Java ones.
   *
   * @since 0.1.0
   */
  @Parameter(defaultValue = "false")
  private boolean kotlinEnabled;

  /**
   * Enable generating Python sources from the protobuf sources.
   *
   * <p>If you enable this, you probably will also want to enable Python stubs
   * to enable generating {@code *.pyi} files for static type checkers.
   *
   * @since 1.1.0
   */
  @Parameter(defaultValue = "false")
  private boolean pythonEnabled;

  /**
   * Enable generating Python stubs ({@code *.pyi} files) for static typechecking from the protobuf
   * sources.
   *
   * <p>If you enable this, you probably will also want to enable Python itself
   * to get actual source code to accompany the stubs.
   *
   * @since 1.1.0
   */
  @Parameter(defaultValue = "false")
  private boolean pythonStubsEnabled;

  /**
   * Enable generating Ruby sources from the protobuf sources.
   *
   * @since 1.1.0
   */
  @Parameter(defaultValue = "false")
  private boolean rubyEnabled;

  /**
   * Enable generating C++ sources from the protobuf sources.
   *
   * @since 1.1.0
   */
  @Parameter(defaultValue = "false")
  private boolean cppEnabled;

  /**
   * Enable generating C# sources from the protobuf sources.
   *
   * @since 1.1.0
   */
  @Parameter(defaultValue = "false")
  private boolean csharpEnabled;

  /**
   * Enable generating Rust sources from the protobuf sources.
   *
   * @since 1.1.0
   */
  @Parameter(defaultValue = "false")
  private boolean rustEnabled;

  /**
   * Enable generating Objective-C sources from the protobuf sources.
   *
   * @since 1.1.0
   */
  @Parameter(defaultValue = "false")
  private boolean objcEnabled;

  /**
   * Enable generating PHP sources from the protobuf sources.
   *
   * @since 1.1.0
   */
  @Parameter(defaultValue = "false")
  private boolean phpEnabled;

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

  /**
   * Whether to register the output directories as compilation roots with Maven.
   *
   * <p>Generally, you want to do this, but there may be edge cases where you
   * wish to control this behaviour manually instead. In this case, set this parameter to be
   * {@code false}.
   *
   * @since 0.5.0
   */
  @Parameter(defaultValue = "true")
  private boolean registerAsCompilationRoot;

  /**
   * Execute the plugin and generate sources.
   *
   * @throws MojoExecutionException if execution fails.
   * @throws MojoFailureException   if an error occurs.
   */
  @Override
  public void execute() throws MojoExecutionException, MojoFailureException {
    try {
      validate();
    } catch (IllegalArgumentException ex) {
      throw new MojoExecutionException(ex.getMessage(), ex);
    }

    //noinspection DataFlowIssue
    var request = ImmutableGenerationRequest.builder()
        .binaryMavenPlugins(nonNullList(binaryMavenPlugins))
        .binaryPathPlugins(nonNullList(binaryPathPlugins))
        .binaryUrlPlugins(nonNullList(binaryUrlPlugins))
        .dependencyResolutionDepth(dependencyResolutionDepth)
        .jvmMavenPlugins(nonNullList(jvmMavenPlugins))
        .importPaths(nonNullList(importPaths)
            .stream()
            .map(File::toPath)
            .collect(Collectors.toList()))
        .isCppEnabled(cppEnabled)
        .isCsharpEnabled(csharpEnabled)
        .isFailOnMissingSources(failOnMissingSources)
        .isFatalWarnings(fatalWarnings)
        .isJavaEnabled(javaEnabled)
        .isKotlinEnabled(kotlinEnabled)
        .isLiteEnabled(liteOnly)
        .isObjcEnabled(objcEnabled)
        .isPhpEnabled(phpEnabled)
        .isPythonEnabled(pythonEnabled)
        .isPythonStubsEnabled(pythonStubsEnabled)
        .isRegisterAsCompilationRoot(registerAsCompilationRoot)
        .isRubyEnabled(rubyEnabled)
        .isRustEnabled(rustEnabled)
        .mavenSession(session)
        .outputDirectory(outputDirectory())
        .protocVersion(protocVersion())
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
        .orElseGet(() -> defaultOutputDirectory(session));
  }

  private Collection<Path> sourceDirectories() {
    if (sourceDirectories != null) {
      return sourceDirectories.stream().map(File::toPath).collect(Collectors.toList());
    } else {
      return List.of(defaultSourceDirectory(session));
    }
  }

  /**
   * Provides the source root registrar for this Mojo.
   *
   * <p>This specifies where to attach generated sources to in order for it
   * to be included as part of the compilation for main or test sources.
   *
   * @return the registrar to use.
   */
  protected abstract SourceRootRegistrar sourceRootRegistrar();

  /**
   * Provides the default source directory to read protobuf sources from.
   *
   * <p>This does not need to point to an existing directory, the plugin will
   * handle this automatically.
   *
   * @param session the Maven session.
   * @return the path to the directory.
   */
  protected abstract Path defaultSourceDirectory(MavenSession session);

  /**
   * Provides the default output directory to write generated code to.
   *
   * <p>This does not need to point to an existing directory, the plugin will
   * handle this automatically.
   *
   * @param session the Maven session.
   * @return the path to the directory.
   */
  protected abstract Path defaultOutputDirectory(MavenSession session);

  /**
   * Validate this Mojo's parameters.
   *
   * @throws IllegalArgumentException if any parameters are invalid.
   */
  protected void validate() {
    // TODO: move this logic into the protoc resolver class.
    if (protocVersion.equalsIgnoreCase("latest")) {
      throw new IllegalArgumentException(
          "Cannot use LATEST for the protoc version. "
              + "Google has not released linear versions in the past, meaning that "
              + "using LATEST will have unexpected behaviour."
      );
    }

    // Having .jar on the output directory makes protoc generate a JAR with a
    // Manifest. This will break our logic because generated sources will be
    // inaccessible for the compilation phase later. For now, just prevent this
    // edge case entirely.
    // TODO: move this logic into the source generator class.
    Optional.ofNullable(outputDirectory)
        .map(File::toPath)
        .flatMap(FileUtils::getFileExtension)
        .filter(".jar"::equalsIgnoreCase)
        .ifPresent(ext -> {
          throw new IllegalArgumentException(
              "The output directory cannot be a path with a JAR file extension");
        });
  }

  private String protocVersion() {
    // Give precedence to overriding the protoc version via the command line
    // in case the Maven binaries are incompatible with the current system.
    var overriddenVersion = System.getProperty("protoc.version");
    return requireNonNullElse(overriddenVersion, protocVersion);
  }

  private <T> List<T> nonNullList(@Nullable List<T> list) {
    return requireNonNullElseGet(list, List::of);
  }
}
