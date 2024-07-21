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
Path transitiveRuntimeDependencyTargetDir = resolve(baseDirectory,"transitive-runtime-dependency", "target")
Path projectTargetDir = resolve(baseDirectory, "project", "target")

/////////////////////////////////////////////////////////
// `transitive-runtime-dependency' output expectations //
/////////////////////////////////////////////////////////

assertThat(transitiveRuntimeDependencyTargetDir).isDirectory()
assertThat(resolve(transitiveRuntimeDependencyTargetDir, "classes", "org", "example", "runtime", "Runtime.class"))
    .isRegularFile()
assertThat(resolve(transitiveRuntimeDependencyTargetDir, "classes", "org", "example", "runtime", "runtime.proto"))
    .isRegularFile()

// Compile dependencies are included in the archives directory.
assertThat(Files.list(resolve(transitiveRuntimeDependencyTargetDir, "protobuf-maven-plugin", "archives")))
    .withFailMessage { "Expected protobuf-java-* directory to be present" }
    .filteredOn { it.getFileName().toString().startsWith("protobuf-java-") }
    .hasSize(1)

///////////////////////////////////
// `project' output expectations //
///////////////////////////////////

assertThat(projectTargetDir).isDirectory()
assertThat(resolve(projectTargetDir, "classes", "org", "example", "compiler", "Compiler.class"))
    .isRegularFile()
assertThat(resolve(projectTargetDir, "classes", "org", "example", "compiler", "compiler.proto"))
    .isRegularFile()

// Compile dependencies are included in the archives directory.
assertThat(Files.list(resolve(projectTargetDir, "protobuf-maven-plugin", "archives")))
    .filteredOn { it.getFileName().toString().startsWith("protobuf-java-") }
    .isNotEmpty()

// Transitive runtime dependencies are not included in the archives directory.
assertThat(Files.list(resolve(projectTargetDir, "protobuf-maven-plugin", "archives")))
    .withFailMessage {
      "Expected transitive-runtime-dependency-* directory to not be present, this means " +
          "transitive runtime dependencies are being included erroneously!"
    }
    .filteredOn { it.getFileName().toString().startsWith("transitive-runtime-dependency-") }
    .isEmpty()

return true
