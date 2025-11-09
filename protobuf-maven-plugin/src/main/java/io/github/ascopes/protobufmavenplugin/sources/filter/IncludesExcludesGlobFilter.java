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

import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.List;
import java.util.function.Predicate;

/**
 * File filter that handles inclusion and exclusion glob lists.
 *
 * @author Ashley Scopes
 * @since 3.1.0
 */
public final class IncludesExcludesGlobFilter implements FileFilter {

  private final List<PathMatcher> includes;
  private final List<PathMatcher> excludes;

  public IncludesExcludesGlobFilter(List<String> includes, List<String> excludes) {
    this.includes = compile(includes);
    this.excludes = compile(excludes);
  }

  @Override
  public boolean matches(Path rootPath, Path filePath) {
    var relativePath = rootPath.relativize(filePath);

    if (excludes.stream().anyMatch(path(relativePath))) {
      // File was explicitly excluded.
      return false;
    }

    // File was explicitly included when inclusions were present, or no inclusions were present
    // so we allow all files anyway.
    return includes.isEmpty() || includes.stream().anyMatch(path(relativePath));
  }

  private static List<PathMatcher> compile(List<String> globs) {
    return globs.stream()
        .map("glob:"::concat)
        .map(FileSystems.getDefault()::getPathMatcher)
        .toList();
  }

  private static Predicate<PathMatcher> path(Path path) {
    return pathMatcher -> pathMatcher.matches(path);
  }
}
