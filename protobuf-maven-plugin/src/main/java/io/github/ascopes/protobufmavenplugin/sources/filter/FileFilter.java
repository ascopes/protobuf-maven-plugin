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
package io.github.ascopes.protobufmavenplugin.sources.filter;

import java.nio.file.Path;

/**
 * Filter for files.
 *
 * @author Ashley Scopes
 * @since 3.1.0
 */
@FunctionalInterface
public interface FileFilter {
  boolean matches(Path rootPath, Path filePath);

  default boolean matches(String relativeFilePath) {
    // Spoof the matcher into working by using the current working directory
    // to build a relative path from the path fragments.
    var spoofedPathRoot = Path.of("");
    var spoofedPath = spoofedPathRoot;

    for (var part : relativeFilePath.split("/", -1)) {
      spoofedPath = spoofedPath.resolve(part);
    }

    return matches(spoofedPathRoot, spoofedPath);
  }

  default FileFilter and(FileFilter other) {
    return (rootPath, filePath) -> matches(rootPath, filePath) && other.matches(rootPath, filePath);
  }
}
