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
import java.util.stream.Collectors
import java.util.stream.Stream

import static org.assertj.core.api.Assertions.assertThat

Path baseDirectory = basedir.toPath().toAbsolutePath()
Path protosDirectory = baseDirectory
    .resolve("target")
    .resolve("protobuf-maven-plugin")
    .resolve("generate")
    .resolve("default")
    .resolve("archives")

assertThat(protosDirectory).isDirectory()

try (Stream<Path> listing = Files.list(protosDirectory)) {
  List<Path> directories = listing.collect(Collectors.toList())
  assertThat(directories).hasSize(2)
}

return true
