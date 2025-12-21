/*
 * Copyright (C) 2023 Ashley Scopes
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

Path baseDirectory = basedir.toPath().toAbsolutePath()
Path runtimeDependencyTargetDir = baseDirectory
    .resolve("runtime-dependency")
    .resolve("target")
Path projectTargetDir = baseDirectory
    .resolve("project")
    .resolve("target")

/////////////////////////////////////////////////////////
// `runtime-dependency' output expectations //
/////////////////////////////////////////////////////////

Path runtimeDependenciesRuntimeClass = runtimeDependencyTargetDir
    .resolve("classes")
    .resolve("org")
    .resolve("example")
    .resolve("runtime")
    .resolve("Runtime.class")
Path runtimeDependenciesRuntimeProto = runtimeDependencyTargetDir
    .resolve("classes")
    .resolve("org")
    .resolve("example")
    .resolve("runtime")
    .resolve("runtime.proto")

assertThat(runtimeDependencyTargetDir).isDirectory()
assertThat(runtimeDependenciesRuntimeClass)
    .isRegularFile()
assertThat(runtimeDependenciesRuntimeProto)
    .isRegularFile()

// Compile dependencies are included in the archives directory.
Path runtimeDependenciesArchivesDir = runtimeDependencyTargetDir
    .resolve("protobuf-maven-plugin")
    .resolve("generate")
    .resolve("default")
    .resolve("archives")

assertThat(Files.list(runtimeDependenciesArchivesDir))
    .withFailMessage { "Expected protobuf-java-* directory to be present" }
    .filteredOn { it.getFileName().toString().startsWith("protobuf-java-") }
    .hasSize(1)

///////////////////////////////////
// `project' output expectations //
///////////////////////////////////

Path projectTargetCompilerClass = projectTargetDir
    .resolve("classes")
    .resolve("org")
    .resolve("example")
    .resolve("compiler")
    .resolve("Compiler.class")
Path projectTargetCompilerProto = projectTargetDir
    .resolve("classes")
    .resolve("org")
    .resolve("example")
    .resolve("compiler")
    .resolve("compiler.proto")

assertThat(projectTargetDir).isDirectory()
assertThat(projectTargetCompilerClass)
    .isRegularFile()
assertThat(projectTargetCompilerProto)
    .isRegularFile()

// Compile dependencies are included in the archives directory.
Path projectTargetDirArchives = projectTargetDir
    .resolve("protobuf-maven-plugin")
    .resolve("generate")
    .resolve("default")
    .resolve("archives")

assertThat(Files.list(projectTargetDirArchives))
    .filteredOn { it.getFileName().toString().startsWith("protobuf-java-") }
    .isNotEmpty()

// Transitive runtime dependencies are not included in the archives directory.
assertThat(Files.list(projectTargetDirArchives))
    .withFailMessage {
      "Expected runtime-dependency-* directory to not be present, this means " +
          "direct runtime dependencies are being included erroneously!"
    }
    .filteredOn { it.getFileName().toString().startsWith("runtime-dependency-") }
    .isEmpty()

return true
