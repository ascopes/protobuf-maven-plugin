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

Path baseProjectDir = basedir.toPath().toAbsolutePath()
Path protocPluginTargetDir = baseProjectDir.resolve("protoc-plugin")
    .resolve("target")
Path expectedGeneratedFile = baseProjectDir.resolve("some-project")
    .resolve("target")
    .resolve("generated-sources")
    .resolve("protobuf")
    .resolve("commandline-args.txt")

// Verify the JVM plugin produced the expected output file
assertThat(expectedGeneratedFile)
    .exists()
    .isRegularFile()
    .hasContent("foo\nbar\nbaz")

return true
