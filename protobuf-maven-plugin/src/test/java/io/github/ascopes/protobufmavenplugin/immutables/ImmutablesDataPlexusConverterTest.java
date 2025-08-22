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
package io.github.ascopes.protobufmavenplugin.immutables;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("ImmutablesDataPlexusConverter tests")
class ImmutablesDataPlexusConverterTest {

  ImmutablesDataPlexusConverter converter;

  @BeforeEach
  void setUp() {
    converter = new ImmutablesDataPlexusConverter();
  }

  @DisplayName(".canConvert(Class) returns the expected values")
  @MethodSource("canConvertTestCases")
  @ParameterizedTest(name = "expect {1} when calling with {0}")
  void canConvertReturnsTheExpectedValue(Class<?> cls, boolean expectedResult) {
    // When
    var actualResult = converter.canConvert(cls);

    // Then
    assertThat(actualResult).isEqualTo(expectedResult);
  }

  static Stream<Arguments> canConvertTestCases() {
    return Stream.of(
        arguments(void.class, false),
        arguments(boolean.class, false),
        arguments(byte.class, false),
        arguments(short.class, false),
        arguments(int.class, false),
        arguments(long.class, false),
        arguments(float.class, false),
        arguments(double.class, false),
        arguments(Void.class, false),
        arguments(Object.class, false),
        arguments(String.class, false),
        arguments(List.class, false),
        arguments(List.class, false),
        arguments(ValidModel.class, true)
    );
  }

}
