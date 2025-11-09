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
package io.github.ascopes.protobufmavenplugin;

import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Project structure tests")
class ProjectStructureTest {

  @DisplayName("Each package has a package-info.java")
  @Test
  void eachPackageHasPackageInfo() throws IOException {
    var baseDir = Path.of(".")
        .resolve("src")
        .resolve("main")
        .resolve("java");

    try (var files = Files.walk(baseDir)) {
      assertSoftly(softly -> {
        files.filter(Files::isDirectory)
            .filter(this::hasChildFiles)
            .forEach(pkg -> softly
                .assertThat(pkg.resolve("package-info.java"))
                .isRegularFile()
                .isNotEmptyFile());
      });
    }
  }

  private boolean hasChildFiles(Path baseDir) {
    try (var files = Files.walk(baseDir, 1)) {
      return files
          .anyMatch(Files::isRegularFile);
    } catch (IOException ex) {
      throw new UncheckedIOException(ex);
    }
  }
}
