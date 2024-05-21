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

import io.github.ascopes.protobufmavenplugin.generation.SourceRootRegistrar;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;

@DisplayName("TestGenerateMojo tests")
class TestGenerateMojoTest extends AbstractGenerateMojoTestTemplate<TestGenerateMojo> {

  @Override
  TestGenerateMojo newInstance() {
    return new TestGenerateMojo();
  }

  @Override
  SourceRootRegistrar expectedSourceRootRegistrar() {
    return SourceRootRegistrar.TEST;
  }

  @Override
  Path expectedDefaultSourceDirectory() {
    return mojo.mavenProject.getBasedir().toPath()
        .resolve("src")
        .resolve("test")
        .resolve("protobuf");
  }

  @Override
  Path expectedDefaultOutputDirectory() {
    return Path.of(mojo.mavenProject.getBuild().getDirectory())
        .resolve("generated-test-sources")
        .resolve("protobuf");
  }
}
