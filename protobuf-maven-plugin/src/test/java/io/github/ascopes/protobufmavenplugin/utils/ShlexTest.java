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

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;


/**
 * @author Ashley Scopes
 */
@DisplayName("Shlex tests")
class ShlexTest {

  @DisplayName("quoteShellArgs(Iterable) returns the expected value")
  @MethodSource("quoteShellArgsTestCases")
  @ParameterizedTest(name = "quoteShellArgs({0}) returns <{1}>")
  void quoteShellArgsReturnsTheExpectedValue(List<String> input, String expected) {
    // When
    var actual = Shlex.quoteShellArgs(input);
    // Then
    assertThat(actual).isEqualTo(expected);
  }

  @DisplayName("quoteBatchArgs(Iterable) returns the expected value")
  @MethodSource("quoteBatchArgsTestCases")
  @ParameterizedTest(name = "quoteBatchArgs({0}) returns <{1}>")
  void quoteBatchArgsReturnsTheExpectedValue(List<String> input, String expected) {
    // When
    var actual = Shlex.quoteBatchArgs(input);
    // Then
    assertThat(actual).isEqualTo(expected);
  }

  static Stream<Arguments> quoteShellArgsTestCases() {
    return Stream.of(
        arguments(list(), ""),
        arguments(list("protoc"), "protoc"),
        arguments(list("/usr/bin/env", "protoc"), "/usr/bin/env protoc"),
        arguments(list("foo bar", "baz"), "'foo bar' baz"),
        arguments(list("foo bar", "baz", "bork qux"), "'foo bar' baz 'bork qux'"),
        arguments(list("foo\\bar", "baz"), "'foo\\\\bar' baz"),
        arguments(list("ABCDEFGHIJKLMNOPQRSTUVWXYZ"), "ABCDEFGHIJKLMNOPQRSTUVWXYZ"),
        arguments(list("abcdefghijklmnopqrstuvwxyz"), "abcdefghijklmnopqrstuvwxyz"),
        arguments(list("0123456789"), "0123456789"),
        arguments(list("-_"), "-_"),
        arguments(list("."), "."),
        arguments(list("/"), "/"),
        arguments(list("="), "="),
        arguments(list(","), "','"),
        arguments(list("<>"), "'<>'"),
        arguments(list("$"), "'$'"),
        arguments(list("&"), "'&'"),
        arguments(list("&&"), "'&&'"),
        arguments(list("||"), "'||'"),
        arguments(list(";"), "';'"),
        arguments(list("~"), "'~'"),
        arguments(list("*"), "'*'"),
        arguments(list("(){}[]"), "'(){}[]'"),
        arguments(list("✓"), "'✓'"),
        arguments(list("^"), "'^'"),
        arguments(list("\\"), "'\\\\'"),
        arguments(list("\""), "'\"'"),
        arguments(list("po'tato"), "'po'\"'\"'tato'"),
        arguments(list("'potato'"), "''\"'\"'potato'\"'\"''"),
        arguments(list("foo\nbar", "baz"), "'foo'$'\\n''bar' baz"),
        arguments(list("foo\rbar", "baz"), "'foo'$'\\r''bar' baz"),
        arguments(list("foo\tbar", "baz"), "'foo'$'\\t''bar' baz"),
        arguments(
            list("a".repeat(100), "b".repeat(100), "c".repeat(100)),
            String.join(
                " \\\n    ",
                "a".repeat(100),
                "b".repeat(100),
                "c".repeat(100)
            )
        )
    );
  }

  static Stream<Arguments> quoteBatchArgsTestCases() {
    return Stream.of(
        arguments(list(), ""),
        arguments(list("protoc"), "protoc"),
        arguments(list("/usr/bin/env", "protoc"), "/usr/bin/env protoc"),
        arguments(list("foo bar", "baz"), "\"foo bar\" baz"),
        arguments(list("foo bar", "baz", "bork qux"), "\"foo bar\" baz \"bork qux\""),
        arguments(list("foo\\bar", "baz"), "\"foo\\bar\" baz"),
        arguments(list("ABCDEFGHIJKLMNOPQRSTUVWXYZ"), "ABCDEFGHIJKLMNOPQRSTUVWXYZ"),
        arguments(list("abcdefghijklmnopqrstuvwxyz"), "abcdefghijklmnopqrstuvwxyz"),
        arguments(list("0123456789"), "0123456789"),
        arguments(list("-_"), "-_"),
        arguments(list("."), "."),
        arguments(list("/"), "/"),
        arguments(list("="), "="),
        arguments(list("\\"), "\"\\\""),
        arguments(list("\""), "\"\"\"\"\""),
        arguments(list("'"), "\"'\""),
        arguments(list(" "), "\" \""),
        arguments(list("\r"), "\"^\r\""),
        arguments(list("\t"), "\"^\t\""),
        arguments(list("^"), "\"^^\""),
        arguments(list("&"), "\"^&\""),
        arguments(list("<"), "\"^<\""),
        arguments(list(">"), "\"^>\""),
        arguments(list("|"), "\"^|\""),
        arguments(list("100% complete", "0% incomplete"), "\"100%% complete\" \"0%% incomplete\""),
        arguments(
            list("a".repeat(100), "b".repeat(100), "c".repeat(100)),
            String.join(
                " ^\r\n    ",
                "a".repeat(100),
                "b".repeat(100),
                "c".repeat(100)
            )
        )
    );
  }

  private static List<String> list(String... args) {
    return List.of(args);
  }
}
