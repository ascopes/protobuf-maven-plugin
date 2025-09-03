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

Path tempDir = Path.of(System.getProperty("java.io.tmpdir"))

// We use /tmp for this to work around Windows problems
// with https://github.com/ascopes/protobuf-maven-plugin/pull/786
Files.createDirectories(tempDir.resolve("pmp-gh-782-sanctioned-executable-paths"))

return true
