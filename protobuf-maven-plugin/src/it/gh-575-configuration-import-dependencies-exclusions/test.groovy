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

import groovy.lang.Closure

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
Path dependencyDirectory = baseDirectory
    .resolve("channels")
    .resolve("target")
    .resolve("protobuf-maven-plugin")
    .resolve("generate")
    .resolve("default")
    .resolve("archives")

assertThat(dependencyDirectory).isDirectory()

try (Stream<Path> listing = Files.list(dependencyDirectory)) {
  List<String> directories = listing
      .map { it.fileName.toString() }
      .collect(Collectors.toList())
  assertThat(directories).satisfiesOnlyOnce consumer { assertThat(it).startsWith("avatars-") }
  assertThat(directories).satisfiesOnlyOnce consumer { assertThat(it).startsWith("users-") }
  assertThat(directories).noneSatisfy consumer { assertThat(it).startsWith("metadata-") }
}

return true
