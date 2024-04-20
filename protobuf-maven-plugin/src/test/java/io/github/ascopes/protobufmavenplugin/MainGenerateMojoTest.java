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

import io.github.ascopes.protobufmavenplugin.generate.SourceRootRegistrar;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;

@DisplayName("MainGenerateMojo tests")
class MainGenerateMojoTest extends AbstractGenerateMojoTestTemplate<MainGenerateMojo> {

  @Override
  MainGenerateMojo newInstance() {
    return new MainGenerateMojo();
  }

  @Override
  SourceRootRegistrar expectedSourceRootRegistrar() {
    return SourceRootRegistrar.MAIN;
  }

  @Override
  Path expectedDefaultSourceDirectory() {
    return mojo.mavenProject.getBasedir().toPath()
        .resolve("src")
        .resolve("main")
        .resolve("protobuf");
  }

  @Override
  Path expectedDefaultOutputDirectory() {
    return Path.of(mojo.mavenProject.getBuild().getDirectory())
        .resolve("generated-sources")
        .resolve("protobuf");
  }
}
