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
import java.nio.file.Path

import static org.assertj.core.api.Assertions.assertThat

Path baseDirectory = basedir.toPath().toAbsolutePath()
Path protoGeneratedSourcesDir = baseDirectory.resolve("target/generated-sources/proto")
Path grpcGeneratedSourcesDir = baseDirectory.resolve("target/generated-sources/grpc")
Path reactorGeneratedSourcesDir = baseDirectory.resolve("target/generated-sources/reactor")
Path classesDir = baseDirectory.resolve("target/classes")

List<String> expectedProtoGeneratedFiles = [
    "org/example/helloworld/Helloworld",
    "org/example/helloworld/GreetingRequest",
    "org/example/helloworld/GreetingRequestOrBuilder",
]

List<String> expectedGrpcGeneratedFiles = [
    "org/example/helloworld/GreetingServiceGrpc",
]

List<String> expectedReactorGeneratedFiles = [
    "org/example/helloworld/ReactorGreetingServiceGrpc",
]

assertThat(protoGeneratedSourcesDir).isDirectory()

assertThat(classesDir).isDirectory()

expectedProtoGeneratedFiles.forEach {
  assertThat(protoGeneratedSourcesDir.resolve("${it}.java"))
      .exists()
      .isNotEmptyFile()
  assertThat(classesDir.resolve("${it}.class"))
      .exists()
      .isNotEmptyFile()
}

expectedGrpcGeneratedFiles.forEach {
  assertThat(grpcGeneratedSourcesDir.resolve("${it}.java"))
      .exists()
      .isNotEmptyFile()
  assertThat(classesDir.resolve("${it}.class"))
      .exists()
      .isNotEmptyFile()
}

expectedReactorGeneratedFiles.forEach {
  assertThat(reactorGeneratedSourcesDir.resolve("${it}.java"))
      .exists()
      .isNotEmptyFile()
  assertThat(classesDir.resolve("${it}.class"))
      .exists()
      .isNotEmptyFile()
}
