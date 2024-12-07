/*
 * Copyright (C) 2023 - 2024, Ashley Scopes.
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

package io.github.ascopes.protobufmavenplugin.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.IOException;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("ArgumentFileBuilder tests")
class ArgumentFileBuilderTest {

  @DisplayName("Java arguments are converted to a string argument file in the expected format")
  @MethodSource("javaArgumentFileCases")
  @ParameterizedTest(name = "for argument list {0}")
  void javaArgumentsAreConvertedToStringArgumentFileInExpectedFormat(
      List<Object> givenArguments,
      String expectedResult
  ) throws IOException  {
    // Given
    var builder = new ArgumentFileBuilder();

    // When
    for (var argument : givenArguments) {
      builder.add(argument);
    }

    var actualResult = new StringBuilder();
    builder.writeToJavaArgumentFile(actualResult);

    // Then
    assertThat(actualResult).asString().isEqualTo(expectedResult);
  }

  @DisplayName("Protoc arguments are converted to a string argument file in the expected format")
  @MethodSource("protocArgumentFileCases")
  @ParameterizedTest(name = "for argument list {0}")
  void protocArgumentsAreConvertedToStringArgumentFileInExpectedFormat(
      List<Object> givenArguments,
      String expectedResult
  ) throws IOException  {
    // Given
    var builder = new ArgumentFileBuilder();

    // When
    for (var argument : givenArguments) {
      builder.add(argument);
    }

    var actualResult = new StringBuilder();
    builder.writeToProtocArgumentFile(actualResult);

    // Then
    assertThat(actualResult).asString().isEqualTo(expectedResult);
  }

  static Stream<Arguments> javaArgumentFileCases() {
    return Stream.of(
        // No arguments
        arguments(
            List.of(),
            ""
        ),
        // Single basic string
        arguments(
            List.of("-Xmx300m"),
            lines("-Xmx300m")
        ),
        // Complex basic string
        arguments(
            List.of("-Xms100m", "-Xmx1G", "-XX:+UseZGC", "-ea", "org.example.GreetMe", "Bob", "Jo"),
            lines("-Xms100m", "-Xmx1G", "-XX:+UseZGC", "-ea", "org.example.GreetMe", "Bob", "Jo")
        ),
        // Arguments containing special whitespace characters
        arguments(
            List.of("start", "foo foo", "bar\r\nbar", "thing", "baz\tbaz", "end"),
            lines("start", "\"foo foo\"", "\"bar\\r\\nbar\"", "thing", "\"baz\\tbaz\"", "end")
        ),
        // Escaping of escape sequences
        arguments(
            List.of("escaping-the-escape-sequence", "foo \\n bar"),
            lines("escaping-the-escape-sequence", "\"foo \\\\n bar\"")
        ),
        // Escaping of single quotes
        arguments(
            List.of("xxx", "who'se'n't does this?", "yyy"),
            lines("xxx", "\"who\\'se\\'n\\'t does this?\"", "yyy")
        ),
        // Escaping of double quotes
        arguments(
            List.of("xxx", "who\"se\"n\"t does this?", "yyy"),
            lines("xxx", "\"who\\\"se\\\"n\\\"t does this?\"", "yyy")
        ),
        // Arguments containing non-string characters
        arguments(
            List.of(69, 420, "Lol"),
            lines("69", "420", "Lol")
        )
    );
  }

  static Stream<Arguments> protocArgumentFileCases() {
    return Stream.of(
        // No arguments
        arguments(
            List.of(),
            ""
        ),
        // Single basic string
        arguments(
            List.of("-Xmx300m"),
            lines("-Xmx300m")
        ),
        // Complex basic string
        arguments(
            List.of("-Xms100m", "-Xmx1G", "-XX:+UseZGC", "-ea", "org.example.GreetMe", "Bob", "Jo"),
            lines("-Xms100m", "-Xmx1G", "-XX:+UseZGC", "-ea", "org.example.GreetMe", "Bob", "Jo")
        ),
        // Escaping of single quotes
        arguments(
            List.of("xxx", "who'se'n't does this?", "yyy"),
            lines("xxx", "who'se'n't does this?", "yyy")
        ),
        // Escaping of double quotes
        arguments(
            List.of("xxx", "who\"se\"n\"t does this?", "yyy"),
            lines("xxx", "who\"se\"n\"t does this?", "yyy")
        ),
        // Arguments containing non-string characters
        arguments(
            List.of(69, 420, "Lol"),
            lines("69", "420", "Lol")
        )
    );
  }

  static String lines(String... lines) {
    return String.join("\n", lines) + "\n";
  }
}
