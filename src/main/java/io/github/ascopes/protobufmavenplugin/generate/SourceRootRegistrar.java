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
package io.github.ascopes.protobufmavenplugin.generate;

import java.nio.file.Path;
import java.util.function.BiConsumer;
import org.apache.maven.project.MavenProject;

/**
 * Registrars for generated source outputs.
 *
 * @author Ashley Scopes
 */
public enum SourceRootRegistrar {
  /**
   * Registrar for main source roots.
   */
  MAIN(MavenProject::addCompileSourceRoot),

  /**
   * Registrar for test source roots.
   */
  TEST(MavenProject::addTestCompileSourceRoot);

  private final BiConsumer<MavenProject, String> sourceRegistrar;

  SourceRootRegistrar(BiConsumer<MavenProject, String> sourceRegistrar) {
    this.sourceRegistrar = sourceRegistrar;
  }

  /**
   * Register a given source output directory to the Maven project for further compilation.
   *
   * @param project         the project to register with.
   * @param outputDirectory the output directory path.
   */
  public void register(MavenProject project, Path outputDirectory) {
    sourceRegistrar.accept(project, outputDirectory.toString());
  }
}
