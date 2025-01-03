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
import io.github.ascopes.protobufmavenplugin.dependencies.MavenDependency;
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

  Collection<? extends MavenProtocPlugin> getBinaryMavenPlugins();

  Collection<? extends PathProtocPlugin> getBinaryPathPlugins();

  Collection<? extends UrlProtocPlugin> getBinaryUrlPlugins();

  DependencyResolutionDepth getDependencyResolutionDepth();

  Set<String> getDependencyScopes();

  Collection<Language> getEnabledLanguages();

  List<String> getExcludes();

  Collection<? extends MavenDependency> getImportDependencies();

  Collection<Path> getImportPaths();

  List<String> getIncludes();

  Collection<? extends MavenProtocPlugin> getJvmMavenPlugins();

  Path getOutputDirectory();

  @Nullable
  Path getOutputDescriptorFile();

  String getProtocVersion();

  Collection<? extends MavenDependency> getSourceDependencies();

  Collection<Path> getSourceRoots();

  SourceRootRegistrar getSourceRootRegistrar();

  boolean isEmbedSourcesInClassOutputs();

  boolean isFailOnInvalidDependencies();

  boolean isFailOnMissingSources();

  boolean isFailOnMissingTargets();

  boolean isFatalWarnings();

  boolean isIgnoreProjectDependencies();

  boolean isIncrementalCompilationEnabled();

  boolean isLiteEnabled();

  boolean isRegisterAsCompilationRoot();
}
