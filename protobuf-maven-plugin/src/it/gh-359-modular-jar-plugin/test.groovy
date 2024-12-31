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
import org.assertj.core.api.InstanceOfAssertFactories

import java.nio.file.Files
import java.nio.file.Path
import java.util.stream.Stream

import static org.assertj.core.api.Assertions.assertThat

Path baseProjectDir = basedir.toPath().toAbsolutePath()
Path protocPluginFrontendTargetDir = baseProjectDir.resolve("protoc-plugin-frontend")
    .resolve("target")
Path expectedGeneratedFile = baseProjectDir.resolve("some-project")
    .resolve("target")
    .resolve("generated-sources")
    .resolve("protobuf")
    .resolve("file-listing.txt")
Path expectedScriptsDirectory = baseProjectDir.resolve("some-project")
    .resolve("target")
    .resolve("protobuf-maven-plugin")
    .resolve("generate")
    .resolve("default")
    .resolve("plugins")
    .resolve("jvm")

// Verify compilation succeeded but that no JAR was created for the plugin itself.
assertThat(protocPluginFrontendTargetDir).isDirectory()
try (Stream<Path> fileStream = Files.list(protocPluginFrontendTargetDir)) {
  assertThat(fileStream.filter { it.getFileName().toString().endsWith(".jar") })
      .withFailMessage { "Expected a JARs to be created by the build in this reproduction" }
      .isNotEmpty()
}

// Verify the JVM plugin produced the expected output file
assertThat(expectedGeneratedFile)
    .exists()
    .isRegularFile()
    .hasContent("org/example/helloworld.proto")

// Verify we invoked the JVM with a module path.
assertThat(Files.list(expectedScriptsDirectory))
    .singleElement(InstanceOfAssertFactories.PATH)
    .isDirectory()
    .extracting({ dir -> dir.resolve("args.txt") }, InstanceOfAssertFactories.PATH)
    .isRegularFile()
    .content()
    .contains("--module-path")

return true
