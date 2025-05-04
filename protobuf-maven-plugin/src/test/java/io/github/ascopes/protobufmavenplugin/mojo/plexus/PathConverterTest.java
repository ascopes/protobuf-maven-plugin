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
package io.github.ascopes.protobufmavenplugin.mojo.plexus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockStatic;

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("PathConverter test")
class PathConverterTest {

  PathConverter converter;

  @BeforeEach
  void setUp() {
    converter = new PathConverter();
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

  @DisplayName("Paths can be parsed successfully")
  @Test
  void pathsCanBeParsedSuccessfully() throws ComponentConfigurationException {
    // Given
    var path = Path.of("foo", "bar", "baz.txt");

    // Then
    assertThat(converter.fromString(path.toString()))
        .isEqualTo(path);
  }

  @DisplayName("Invalid Paths raise an exception during parsing")
  @Test
  void invalidPathsRaiseAnExceptionDuringParsing() {
    // Mocked because we cannot create reproducible results across Windows and Unix
    try (var pathStatic = mockStatic(Path.class)) {
      // Given
      var expectedCause = new InvalidPathException("that is bad", "bad stuff found", 123);
      pathStatic.when(() -> Path.of(anyString()))
          .thenThrow(expectedCause);

      // Then
      assertThatExceptionOfType(ComponentConfigurationException.class)
          .isThrownBy(() -> converter.fromString("invalid-path"))
          .withMessage("Failed to parse path 'invalid-path': "
              + "java.nio.file.InvalidPathException: bad stuff found at index 123: that is bad")
          .havingCause()
          .isSameAs(expectedCause);

    }
  }
}
