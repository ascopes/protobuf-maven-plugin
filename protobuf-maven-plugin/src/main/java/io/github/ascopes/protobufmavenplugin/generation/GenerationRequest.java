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
package io.github.ascopes.protobufmavenplugin.generation;

import io.github.ascopes.protobufmavenplugin.dependencies.DependencyResolutionDepth;
import io.github.ascopes.protobufmavenplugin.dependencies.MavenArtifact;
import io.github.ascopes.protobufmavenplugin.plugins.MavenProtocPlugin;
import io.github.ascopes.protobufmavenplugin.plugins.PathProtocPlugin;
import io.github.ascopes.protobufmavenplugin.plugins.UrlProtocPlugin;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.immutables.value.Value.Immutable;
import org.jspecify.annotations.Nullable;

/**
 * Base for a generation request with all the details of what to do during generation.
 *
 * @author Ashley Scopes
 */
@Immutable
public interface GenerationRequest {

  /**
   * Binary {@code protoc} plugins that should be resolved from Maven
   * repositories.
   *
   * @return the collection of plugins.
   */
  Collection<? extends MavenProtocPlugin> getBinaryMavenPlugins();

  /**
   * Binary {@code protoc} plugins that should be resolved from the system
   * {@code $PATH}.
   *
   * @return the collection of plugins.
   */
  Collection<? extends PathProtocPlugin> getBinaryPathPlugins();

  /**
   * Binary {@code protoc} plugins that should be resolved from URLs.
   *
   * @return the collection of plugins.
   */
  Collection<? extends UrlProtocPlugin> getBinaryUrlPlugins();

  /**
   * The preference for how to resolve transitive dependencies by default.
   *
   * @return the dependency resolution depth preference.
   */
  DependencyResolutionDepth getDependencyResolutionDepth();

  /**
   * The dependency scopes to allow when searching the Maven project
   * dependency list for {@code *.proto} files.
   *
   * @return the set of scopes.
   */
  Set<String> getDependencyScopes();

  /**
   * The collection of supported langagues to enable in {@code protoc}.
   *
   * @return the languages.
   */
  Collection<Language> getEnabledLanguages();

  /**
   * The collection of {@code *.proto} path patterns to exclude from being
   * passed to {@code protoc}.
   *
   * @return the collection of glob patterns.
   */
  List<String> getExcludes();

  /**
   * Additional user-defined Maven dependencies to include in the {@code protoc}
   * import path.
   *
   * @return the collection of dependencies.
   */
  Collection<? extends MavenArtifact> getImportDependencies();

  /**
   * Additional user-defined paths relative to the project root to include in the
   * {@code protoc} import path.
   *
   * @return the collection of paths.
   */
  Collection<Path> getImportPaths();

  /**
   * The collection of {@code *.proto} path patterns to include to be passed to
   * {@code protoc}.
   *
   * @return the collection of glob patterns.
   */
  List<String> getIncludes();

  /**
   * Java executable projects that satisfy the {@code protoc} plugin interface
   * to wrap in bootstrapping scripts and pass to {@code protoc}.
   *
   * @return the collection of plugins.
   */
  Collection<? extends MavenProtocPlugin> getJvmMavenPlugins();

  /**
   * The path to write all output files to.
   *
   * @return the output path.
   */
  Path getOutputDirectory();

  /**
   * The output {@code protobin} descriptor file to create, or {@code null} if
   * no descriptor file should be created.
   *
   * @return the path to the descriptor file to output, or {@code null}.
   */
  @Nullable Path getOutputDescriptorFile();

  /**
   * Whether to attach output {@code protobin} descriptor file to Maven project.
   *
   * @return flag indicating if descriptor file should be attached.
   */
  boolean isOutputDescriptorAttached();

  /**
   * The Maven artifact type to use when attaching the {@code protobin} descriptor
   * file to the Maven project.
   *
   * @return the artifact type, or {@code null}.
   */
  @Nullable String getOutputDescriptorAttachmentType();

  /**
   * The Maven artifact classifier to use when attaching the {@code protobin}
   * descriptor file to the Maven project.
   *
   * @return the artifact classifier, or {@code null}.
   */
  @Nullable String getOutputDescriptorAttachmentClassifier();

  /**
   * The version of {@code protoc} to use.
   *
   * <p>This will be one of:
   *
   * <ul>
   *   <li>A Maven version string (such as {@code 4.29.3}), to indicate to
   *       pull from the Maven repositories;
   *   <li>The literal string {@code PATH}, to indicate to invoke the
   *       {@code protoc} binary on the system {@code $PATH};
   *   <li>A URL to a binary to execute.
   * </ul>
   *
   * @return the version indicator.
   */
  String getProtocVersion();

  /**
   * Additional user-defined Maven dependencies to include in the {@code protoc}
   * import path, and to compile.
   *
   * @return the collection of dependencies.
   */
  Collection<? extends MavenArtifact> getSourceDependencies();

  /**
   * Paths relative to the project root that contain {@code *.proto} sources to
   * compile.
   *
   * @return the source roots.
   */
  Collection<Path> getSourceRoots();

  /**
   * The registrar strategy to use to notify Maven of generated sources.
   *
   * @return the registrar strategy.
   */
  SourceRootRegistrar getSourceRootRegistrar();

  /**
   * The registrar strategy to use to notify Maven of attached artifacts.
   *
   * @return the registrar strategy.
   */
  OutputDescriptorAttachmentRegistrar getOutputDescriptorAttachmentRegistrar();

  /**
   * Whether to include input {@code proto} sources in the output class
   * directory.
   *
   * @return the boolean preference.
   */
  boolean isEmbedSourcesInClassOutputs();

  /**
   * Whether to treat invalid dependencies as a build error.
   *
   * @return the boolean preference.
   */
  boolean isFailOnInvalidDependencies();

  /**
   * Whether to treat non-existent source roots as a build error.
   *
   * @return the boolean preference.
   */
  boolean isFailOnMissingSources();

  /**
   * Whether to treat builds with no enabled languages, plugins, or descriptor
   * file outputs as a build error.
   *
   * @return the boolean preference.
   */
  boolean isFailOnMissingTargets();

  /**
   * Whether to treat {@code protoc} build warnings as errors.
   *
   * @return the boolean preference.
   */
  boolean isFatalWarnings();

  /**
   * Whether to skip discovering {@code *.proto} files to import from the Maven
   * project {@code <dependencies/>} block.
   *
   * @return the boolean preference.
   */
  boolean isIgnoreProjectDependencies();

  /**
   * Whether to enable incrementally compiling sources.
   *
   * @return the boolean preference.
   */
  boolean isIncrementalCompilationEnabled();

  /**
   * Whether to request the generation of "lite" sources.
   *
   * @return the boolean preference.
   */
  boolean isLiteEnabled();

  /**
   * Whether to include imports in the output {@code protobin} descriptor.
   *
   * @return the boolean preference.
   */
  boolean isOutputDescriptorIncludeImports();

  /**
   * Whether to include source information in the output {@code protobin}
   * descriptor.
   *
   * @return the boolean preference.
   */
  boolean isOutputDescriptorIncludeSourceInfo();

  /**
   * Whether to retain build option metadata in the output {@code protobin}
   * descriptor.
   *
   * @return the boolean preference.
   */
  boolean isOutputDescriptorRetainOptions();

  /**
   * Whether to mark generated sources as candidates for compilation with the
   * {@code maven-compiler-plugin} and similar plugins.
   *
   * @return the boolean preference.
   */
  boolean isRegisterAsCompilationRoot();
}
