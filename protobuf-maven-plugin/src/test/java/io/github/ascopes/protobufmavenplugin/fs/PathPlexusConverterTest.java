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
package io.github.ascopes.protobufmavenplugin.fs;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.nio.file.Path;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.converters.lookup.DefaultConverterLookup;
import org.codehaus.plexus.component.configurator.expression.DefaultExpressionEvaluator;
import org.codehaus.plexus.configuration.DefaultPlexusConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("PathPlexusConverter tests")
class PathPlexusConverterTest {

  PathPlexusConverter converter;

  @BeforeEach
  void setUp() {
    converter = new PathPlexusConverter();
  }

  @DisplayName("only the expected types are convertible")
  @CsvSource({
      "     java.nio.file.Path,  true",
      "           java.net.URI, false",
      "       java.lang.Object, false",
      "      java.lang.Integer, false",
      "        java.lang.Class, false",
      "       java.lang.String, false",
      "java.lang.StringBuilder, false",
      "           java.io.File, false",
      "           java.net.URL, false",
  })
  @ParameterizedTest(name = "for {0}, expect {1}")
  void onlyTheExpectedTypesAreConvertible(Class<?> type, boolean expectedResult) {
    // Then
    assertThat(converter.canConvert(type))
        .isEqualTo(expectedResult);
  }

  @DisplayName("Relative paths can be parsed successfully")
  @Test
  void relativePathsCanBeParsedSuccessfully(
      @TempDir Path baseDir
  ) throws ComponentConfigurationException {
    // Given
    var expectedAbsolutePath = baseDir.resolve("foo").resolve("bar").resolve("baz.txt")
        .toAbsolutePath();
    var expectedPath = baseDir
        .relativize(expectedAbsolutePath);
    var converterLookup = new DefaultConverterLookup();
    var configuration = new DefaultPlexusConfiguration("path", expectedPath.toString());
    var evaluator = new SomeDirectoryRelativeExpressionEvaluator(baseDir);

    // When
    var result = converter.fromConfiguration(
        converterLookup,
        configuration,
        Path.class,
        null,
        getClass().getClassLoader(),
        evaluator,
        null
    );

    // Then
    assertThat(result).isInstanceOf(Path.class);
    var resultPath = (Path) result;

    assertThat(resultPath.toAbsolutePath())
        .hasToString("%s", expectedAbsolutePath);
  }

  @DisplayName("Absolute paths can be parsed successfully")
  @Test
  void absolutePathsCanBeParsedSuccessfully(
      @TempDir Path baseDir
  ) throws ComponentConfigurationException {
    // Given
    var expectedPath = baseDir.resolve("foo").resolve("bar").resolve("baz.txt")
        .toAbsolutePath();
    var converterLookup = new DefaultConverterLookup();
    var configuration = new DefaultPlexusConfiguration("path", expectedPath.toString());
    var evaluator = new SomeDirectoryRelativeExpressionEvaluator(baseDir);

    // When
    var result = converter.fromConfiguration(
        converterLookup,
        configuration,
        Path.class,
        null,
        getClass().getClassLoader(),
        evaluator,
        null
    );

    // Then
    assertThat(result).isInstanceOf(Path.class);
    var resultPath = (Path) result;

    assertThat(resultPath.toAbsolutePath())
        .hasToString("%s", expectedPath.toAbsolutePath());
  }

  @DisplayName("Null values are returned directly")
  @Test
  void nullValuesAreReturnedDirectly(
      @TempDir Path baseDir
  ) throws ComponentConfigurationException {
    // Given
    var converterLookup = new DefaultConverterLookup();
    var configuration = new DefaultPlexusConfiguration("path", null);
    var evaluator = new SomeDirectoryRelativeExpressionEvaluator(baseDir);

    // When
    var result = converter.fromConfiguration(
        converterLookup,
        configuration,
        Path.class,
        null,
        getClass().getClassLoader(),
        evaluator,
        null
    );

    // Then
    assertThat(result).isNull();
  }

  // Roughly equivalent to what Maven does, for the sake of this test.
  static final class SomeDirectoryRelativeExpressionEvaluator extends DefaultExpressionEvaluator {

    private final Path baseDir;

    SomeDirectoryRelativeExpressionEvaluator(Path baseDir) {
      this.baseDir = baseDir.toAbsolutePath();
    }

    @Override
    public File alignToBaseDirectory(File file) {
      if (file.isAbsolute()) {
        return file;
      }

      var path = file.toPath();
      var fullPath = baseDir;
      for (var part : path) {
        fullPath = fullPath.resolve(part);
      }
      return fullPath.toFile();
    }
  }
}
