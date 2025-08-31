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

Path testDependencyGenerateDefaultArchivesDir = testDependencyTargetDir
    .resolve("protobuf-maven-plugin")
    // SHA-256 of "\0".join("generate", "default", "archives")
    .resolve("0a499ddd05c090f574ae588c7326a72fa6b7799765ebd89a7a9450830353312b")

assertThat(testDependencyTargetDir).isDirectory()
assertThat(resolve(testDependencyTargetDir, "classes", "org", "example", "test", "TestSuite.class"))
    .isRegularFile()
assertThat(resolve(testDependencyTargetDir, "classes", "org", "example", "test", "test.proto"))
    .isRegularFile()
assertThat(Files.list(testDependencyGenerateDefaultArchivesDir))
    .withFailMessage { "Expected protobuf-java-* directory to be present" }
    .filteredOn { it.getFileName().toString().startsWith("protobuf-java-") }
    .hasSize(1)

////////////////////////////////////////
// `test-project' output expectations //
////////////////////////////////////////

Path testProjectGenerateTestDefaultArchivesDir = testProjectTargetDir
    .resolve("protobuf-maven-plugin")
    // SHA-256 of "\0".join("generate-test", "default", "archives")
    .resolve("0adb9f47535fa69ba04c54f2533a170de99e89478c2513bb0dcc0b6ea5ba3ee5")

assertThat(testProjectTargetDir).isDirectory()
assertThat(resolve(testProjectTargetDir, "test-classes", "org", "example", "compiler", "Compiler.class"))
    .isRegularFile()
assertThat(resolve(testProjectTargetDir, "test-classes", "org", "example", "compiler", "compiler.proto"))
    .isRegularFile()
assertThat(Files.list(testProjectGenerateTestDefaultArchivesDir))
    .withFailMessage { "Expected protobuf-java-* directory to be present" }
    .filteredOn { it.getFileName().toString().startsWith("protobuf-java-") }
    .hasSize(1)
// We should include test sources in the test goal execution.
assertThat(Files.list(testProjectGenerateTestDefaultArchivesDir))
    .withFailMessage { "Expected test-dependency-* directory to be present" }
    .filteredOn { it.getFileName().toString().startsWith("test-dependency-") }
    .hasSize(1)

////////////////////////////////////////
// `main-project' output expectations //
////////////////////////////////////////

Path mainProjectGenerateDefaultArchivesDir = mainProjectTargetDir
    .resolve("protobuf-maven-plugin")
    // SHA-256 of "\0".join("generate", "default", "archives")
    .resolve("0a499ddd05c090f574ae588c7326a72fa6b7799765ebd89a7a9450830353312b")


assertThat(mainProjectTargetDir).isDirectory()
assertThat(resolve(mainProjectTargetDir,"classes", "org", "example", "compiler", "Compiler.class"))
    .isRegularFile()
assertThat(resolve(mainProjectTargetDir,"classes", "org", "example", "compiler", "compiler.proto"))
    .isRegularFile()
assertThat(Files.list(mainProjectGenerateDefaultArchivesDir))
    .withFailMessage { "Expected protobuf-java-* directory to be present" }
    .filteredOn { it.getFileName().toString().startsWith("protobuf-java-") }
    .hasSize(1)
// We should exclude test sources in the main goal execution.
assertThat(Files.list(mainProjectGenerateDefaultArchivesDir))
    .withFailMessage { "Expected test-dependency-* directory to not be present" }
    .filteredOn { it.getFileName().toString().startsWith("test-dependency-") }
    .isEmpty()

return true
