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


import java.nio.file.Files
import java.nio.file.Path

import static org.assertj.core.api.Assertions.assertThat

static Path resolve(Path path, String... bits) {
  for (String bit : bits) {
    path = path.resolve(bit)
  }
  return path
}

Path baseDirectory = basedir.toPath().toAbsolutePath()
Path testDependencyTargetDir = resolve(baseDirectory, "test-dependency", "target")
Path testProjectTargetDir = resolve(baseDirectory, "test-project", "target")
Path mainProjectTargetDir = resolve(baseDirectory, "main-project", "target")

///////////////////////////////////////////
// `test-dependency' output expectations //
///////////////////////////////////////////

assertThat(testDependencyTargetDir).isDirectory()
assertThat(resolve(testDependencyTargetDir, "classes", "org", "example", "test", "TestSuite.class"))
    .isRegularFile()
assertThat(resolve(testDependencyTargetDir, "classes", "org", "example", "test", "test.proto"))
    .isRegularFile()
assertThat(Files.list(resolve(testDependencyTargetDir, "protobuf-maven-plugin", "generate", "default", "archives")))
    .withFailMessage { "Expected protobuf-java-* directory to be present" }
    .filteredOn { it.getFileName().toString().startsWith("protobuf-java-") }
    .hasSize(1)

////////////////////////////////////////
// `test-project' output expectations //
////////////////////////////////////////

assertThat(testProjectTargetDir).isDirectory()
assertThat(resolve(testProjectTargetDir, "test-classes", "org", "example", "compiler", "Compiler.class"))
    .isRegularFile()
assertThat(resolve(testProjectTargetDir, "test-classes", "org", "example", "compiler", "compiler.proto"))
    .isRegularFile()
assertThat(Files.list(resolve(testProjectTargetDir, "protobuf-maven-plugin", "generate-test", "default", "archives")))
    .withFailMessage { "Expected protobuf-java-* directory to be present" }
    .filteredOn { it.getFileName().toString().startsWith("protobuf-java-") }
    .hasSize(1)
// We should include test sources in the test goal execution.
assertThat(Files.list(resolve(testProjectTargetDir, "protobuf-maven-plugin", "generate-test", "default", "archives")))
    .withFailMessage { "Expected test-dependency-* directory to be present" }
    .filteredOn { it.getFileName().toString().startsWith("test-dependency-") }
    .hasSize(1)

////////////////////////////////////////
// `main-project' output expectations //
////////////////////////////////////////

assertThat(mainProjectTargetDir).isDirectory()
assertThat(resolve(mainProjectTargetDir,"classes", "org", "example", "compiler", "Compiler.class"))
    .isRegularFile()
assertThat(resolve(mainProjectTargetDir,"classes", "org", "example", "compiler", "compiler.proto"))
    .isRegularFile()
assertThat(Files.list(resolve(mainProjectTargetDir, "protobuf-maven-plugin", "generate", "default", "archives")))
    .withFailMessage { "Expected protobuf-java-* directory to be present" }
    .filteredOn { it.getFileName().toString().startsWith("protobuf-java-") }
    .hasSize(1)
// We should exclude test sources in the main goal execution.
assertThat(Files.list(resolve(mainProjectTargetDir, "protobuf-maven-plugin", "generate", "default", "archives")))
    .withFailMessage { "Expected test-dependency-* directory to not be present" }
    .filteredOn { it.getFileName().toString().startsWith("test-dependency-") }
    .isEmpty()

return true
