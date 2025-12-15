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

    try (var files = Files.walk(baseDir())) {
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

  @DisplayName("all injectable beans are annotated with a description")
  @Test
  void allInjectableBeansAreAnnotatedWithDescription() throws IOException {
    try (var files = Files.walk(baseDir())) {
      assertSoftly(softly -> {
        files.filter(Files::isRegularFile)
            .filter(f -> f.toString().endsWith(".java"))
            .map(f -> toClass(baseDir(), f))
            .filter(cls -> cls.isAnnotationPresent(javax.inject.Named.class))
            .forEach(cls -> {
              var isOk = cls.isAnnotationPresent(org.eclipse.sisu.Description.class);
              softly.assertThat(isOk)
                  .withFailMessage(
                      "Expected %s to be annotated with org.eclipse.sisu.Description",
                      cls.getName()
                  )
                  .isTrue();
            });
      });
    }
  }

  @DisplayName("all injectable beans are annotated as a singleton")
  @Test
  void allInjectableBeansAreAnnotatedAsSingleton() throws IOException {
    try (var files = Files.walk(baseDir())) {
      assertSoftly(softly -> {
        files.filter(Files::isRegularFile)
            .filter(f -> f.toString().endsWith(".java"))
            .map(f -> toClass(baseDir(), f))
            .filter(cls -> cls.isAnnotationPresent(javax.inject.Named.class))
            .forEach(cls -> {
              var isOk = cls.isAnnotationPresent(javax.inject.Singleton.class)
                  ^ cls.isAnnotationPresent(
                      org.apache.maven.execution.scope.MojoExecutionScoped.class);
              softly.assertThat(isOk)
                  .withFailMessage(
                      "Expected %s to be annotated with either Singleton or MojoExecutionScoped",
                      cls.getName()
                  )
                  .isTrue();
            });
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

  private Class<?> toClass(Path baseDir, Path source) {
    var frags = baseDir.relativize(source).iterator();
    var clsName = new StringBuilder(frags.next().toString());
    while (frags.hasNext()) {
      clsName.append(".").append(frags.next());
    }

    try {
      return Class.forName(clsName.toString().replaceAll("\\.java$", ""));
    } catch (ClassNotFoundException ex) {
      throw new RuntimeException("Failed to load class", ex);
    }
  }

  private Path baseDir() {
    return Path.of(".")
        .resolve("src")
        .resolve("main")
        .resolve("java");
  }
}
