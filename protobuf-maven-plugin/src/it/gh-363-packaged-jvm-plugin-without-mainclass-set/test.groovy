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
import java.util.stream.Stream

import static org.assertj.core.api.Assertions.assertThat

Path baseProjectDir = basedir.toPath().toAbsolutePath()
Path protocPluginTargetDir = baseProjectDir.resolve("protoc-plugin")
    .resolve("target")
Path expectedGeneratedFile = baseProjectDir.resolve("some-project")
    .resolve("target")
    .resolve("generated-sources")
    .resolve("protobuf")
    .resolve("file-listing.txt")


// Verify compilation succeeded and that a JAR was created for the plugin itself.
assertThat(protocPluginTargetDir).isDirectory()
try (Stream<Path> fileStream = Files.list(protocPluginTargetDir)) {
  assertThat(fileStream.filter { it.getFileName().toString().endsWith(".jar") })
      .withFailMessage { "Expected JARs to be created by the build in this reproduction" }
      .isNotEmpty()
}

// Verify the JVM plugin produced the expected output file
assertThat(expectedGeneratedFile)
    .exists()
    .isRegularFile()
    .hasContent("org/example/helloworld.proto")

return true
