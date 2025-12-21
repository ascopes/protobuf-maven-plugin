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
import java.util.function.Consumer
import java.util.stream.Collectors
import java.util.stream.Stream

import static org.assertj.core.api.Assertions.assertThat

// Type hint to work around method ambiguity in groovy.
@SuppressWarnings("all")
<T> Consumer<T> consumer(Closure consumer) { consumer }


Path baseDirectory = basedir.toPath().toAbsolutePath()
Path generatedSourcesDir = baseDirectory.resolve("target/generated-sources/protobuf")
Path classesDir = baseDirectory.resolve("target/classes")
Path tempDir = Path.of(System.getProperty("java.io.tmpdir"))
List<String> expectedGeneratedFiles = [
    "org/example/helloworld/Helloworld",
    "org/example/helloworld/GreetingRequest",
    "org/example/helloworld/GreetingRequestOrBuilder",
    "org/example/helloworld/GreetingServiceGrpc",
    "org/example/helloworld/ReactorGreetingServiceGrpc",
]

Path sanctionedExecutableDir = tempDir
    .resolve("pmp-gh-782-sanctioned-executable-paths")
    .resolve("gh-782-sanctioned-executable-paths")  // groupId
    .resolve("gh-782-sanctioned-executable-paths")  // artifactId
    .resolve("protobuf-maven-plugin")  // frag
    .resolve("generate")  // goal
    .resolve("default")  // goal ID

List<String> expectedSanctionedFilePatterns = [
    "plugin-[0-9]-invoke.(bat|sh)",
    "plugin-[0-9]-protoc-gen-grpc-java-.+\\.exe",
    "protoc-protoc-.+\\.exe",
]


assertThat(generatedSourcesDir).isDirectory()
assertThat(classesDir).isDirectory()

expectedGeneratedFiles.forEach {
  assertThat(generatedSourcesDir.resolve("${it}.java"))
      .exists()
      .isNotEmptyFile()
  assertThat(classesDir.resolve("${it}.class"))
      .exists()
      .isNotEmptyFile()
}

assertThat(sanctionedExecutableDir).isDirectory()

try (Stream<Path> list = Files.list(sanctionedExecutableDir)) {
  List<Path> files = list.collect(Collectors.toList());

  for (String fileNamePattern : expectedSanctionedFilePatterns) {
    assertThat(files).satisfiesOnlyOnce consumer {
      assertThat(it.getFileName().toString())
          .matches(fileNamePattern)
      assertThat(it)
          .isNotEmptyFile()
          .isRegularFile()
    }
  }
}
