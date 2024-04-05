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

package io.github.ascopes.protobufmavenplugin.generate;

import io.github.ascopes.protobufmavenplugin.dependency.DependencyResolutionDepth;
import io.github.ascopes.protobufmavenplugin.dependency.MavenArtifact;
import java.net.URL;
import java.nio.file.Path;
import java.util.Collection;
import org.apache.maven.execution.MavenSession;
import org.immutables.value.Value.Immutable;

/**
 * Base for a generation request with all the details of what to do during generation.
 *
 * @author Ashley Scopes
 */
@Immutable
public interface GenerationRequest {

  Collection<MavenArtifact> getBinaryMavenPlugins();

  Collection<String> getBinaryPathPlugins();

  Collection<URL> getBinaryUrlPlugins();

  DependencyResolutionDepth getDependencyResolutionDepth();

  Collection<Path> getImportPaths();

  Collection<MavenArtifact> getJvmMavenPlugins();

  MavenSession getMavenSession();

  Path getOutputDirectory();

  String getProtocVersion();

  Collection<Path> getSourceRoots();

  SourceRootRegistrar getSourceRootRegistrar();

  boolean isCppEnabled();

  boolean isCsharpEnabled();

  boolean isFailOnMissingSources();

  boolean isFatalWarnings();

  boolean isJavaEnabled();

  boolean isKotlinEnabled();

  boolean isLiteEnabled();

  boolean isObjcEnabled();

  boolean isPhpEnabled();

  boolean isPythonEnabled();

  boolean isPythonStubsEnabled();

  boolean isRegisterAsCompilationRoot();

  boolean isRubyEnabled();

  boolean isRustEnabled();
}
