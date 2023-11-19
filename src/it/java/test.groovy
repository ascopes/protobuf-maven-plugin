/*
 * Copyright (C) 2023, Ashley Scopes.
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
def generatedSourcesPath = baseDirectory.resolve("target/generated-sources/protoc")
def classesPath = baseDirectory.resolve("target/classes")
def expectedGeneratedFiles = [
    "org/example/helloworld/Helloworld",
    "org/example/helloworld/GreetingRequest",
    "org/example/helloworld/GreetingRequestOrBuilder",
    "org/example/helloworld/GreetingResponse",
    "org/example/helloworld/GreetingResponseOrBuilder"
]

assertThat(generatedSourcesPath).isDirectory()

assertThat(classesPath).isDirectory()

expectedGeneratedFiles.forEach {
  assertThat(generatedSourcesPath.resolve("${it}.java"))
      .exists()
      .isNotEmptyFile()
  assertThat(classesPath.resolve("${it}.class"))
      .exists()
      .isNotEmptyFile()

}
