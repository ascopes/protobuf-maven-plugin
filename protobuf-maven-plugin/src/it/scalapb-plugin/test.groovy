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
import java.nio.file.Path

import static org.assertj.core.api.Assertions.assertThat

Path baseDirectory = basedir.toPath().toAbsolutePath()
Path generatedSourcesDir = baseDirectory.resolve("target/generated-sources/protobuf")
Path classesDir = baseDirectory.resolve("target/classes")

List<String> expectedGeneratedSources = [
  "org/example/helloworld/GreetingRequest.scala",
  "org/example/helloworld/GreetingResponse.scala",
  "org/example/helloworld/GreetingServiceGrpc.scala",
  "org/example/helloworld/HelloworldProto.scala",
]

List<String> expectedCompiledClasses = [
  "org/example/helloworld/HelloworldProto\$.class",
  "org/example/helloworld/GreetingServiceGrpc\$GreetingServiceBlockingClient.class",
  "org/example/helloworld/HelloworldProto.class",
  "org/example/helloworld/HelloworldProto.tasty",
  "org/example/helloworld/GreetingServiceGrpc\$GreetingServiceBlockingStub\$.class",
  "org/example/helloworld/GreetingRequest\$.class",
  "org/example/helloworld/GreetingResponse.tasty",
  "org/example/helloworld/GreetingResponse\$.class",
  "org/example/helloworld/GreetingServiceGrpc\$GreetingService\$.class",
  "org/example/helloworld/GreetingServiceImpl.tasty",
  "org/example/helloworld/GreetingServiceGrpc.class",
  "org/example/helloworld/GreetingServiceGrpc\$GreetingServiceStub.class",
  "org/example/helloworld/GreetingServiceGrpc\$.class",
  "org/example/helloworld/GreetingResponse\$GreetingResponseLens.class",
  "org/example/helloworld/GreetingServiceGrpc.tasty",
  "org/example/helloworld/GreetingResponse.class",
  "org/example/helloworld/GreetingRequest.class",
  "org/example/helloworld/GreetingServiceGrpc\$GreetingServiceBlockingStub.class",
  "org/example/helloworld/GreetingServiceGrpc\$GreetingServiceStub\$.class",
  "org/example/helloworld/GreetingServiceImpl.class",
  "org/example/helloworld/GreetingRequest\$GreetingRequestLens.class",
  "org/example/helloworld/GreetingRequest.tasty",
  "org/example/helloworld/GreetingServiceGrpc\$GreetingService.class",
]

assertThat(generatedSourcesDir).isDirectory()

assertThat(classesDir).isDirectory()

expectedGeneratedSources.forEach {
  assertThat(generatedSourcesDir.resolve(it))
      .exists()
      .isNotEmptyFile()
}

expectedCompiledClasses.forEach {
  assertThat(classesDir.resolve(it))
      .exists()
      .isNotEmptyFile()
}

return true
