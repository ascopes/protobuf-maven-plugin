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

import io.github.ascopes.protobufmavenplugin.generation.OutputDescriptorAttachmentRegistrar;
import io.github.ascopes.protobufmavenplugin.generation.SourceRootRegistrar;
import java.nio.file.Path;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.ResolutionScope;

/**
 * Generate source code from protobuf files.
 *
 * <p>This treats generated code as being part of the main source set.
 * For test sources, use the {@code generate-test} goal instead.
 *
 * <p>Any project dependencies using the {@code compile}, {@code provided},
 * or {@code system} scopes will be made available to import from protobuf sources.
 *
 * <p>By default, sources will be read from {@code src/main/protobuf},
 * and generated sources will be written to {@code target/generated-sources/protobuf}.
 *
 * @author Ashley Scopes
 */
@Mojo(
    name = "generate",
    defaultPhase = LifecyclePhase.GENERATE_SOURCES,
    // We require resolving TEST scope here since the user can control the overall scope
    // we use for dependencies. The TEST scope is documented to cover all other scopes.
    requiresDependencyCollection = ResolutionScope.TEST,
    requiresDependencyResolution = ResolutionScope.TEST,
    requiresOnline = true,
    threadSafe = true
)
public final class MainGenerateMojo extends AbstractGenerateMojo {

  @Override
  SourceRootRegistrar sourceRootRegistrar() {
    return SourceRootRegistrar.MAIN;
  }

  @Override
  OutputDescriptorAttachmentRegistrar outputDescriptorAttachmentRegistrar() {
    return new OutputDescriptorAttachmentRegistrar(
        mavenProjectHelper, "protobin", null
    );
  }

  @Override
  Set<String> defaultDependencyScopes() {
    return Set.of("compile", "provided", "system");
  }

  @Override
  Collection<Path> defaultSourceDirectories() {
    var basePath = mavenProject.getBasedir().toPath()
        .resolve("src")
        .resolve("main")
        .resolve("protobuf");
    return List.of(basePath);
  }

  @Override
  Path defaultOutputDirectory() {
    return Path.of(mavenProject.getBuild().getDirectory())
        .resolve("generated-sources")
        .resolve("protobuf");
  }
}
