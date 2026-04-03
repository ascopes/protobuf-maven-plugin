/*
 * Copyright (C) 2023 Ashley Scopes
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
import static org.assertj.core.api.Assertions.assertThatException;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.IOException;
import java.io.UncheckedIOException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("Unchecked tests")
class UncheckedTest {

  @DisplayName("return values are returned correctly")
  @ValueSource(strings = {"foo", "bar", "baz"})
  @ParameterizedTest(name = "when expecting \"{0}\"")
  void returnValuesAreReturnedCorrectly(String value) {
    // When
    var result = Unchecked.call(() -> value);

    // Then
    assertThat(result).isEqualTo(value);
  }

  @DisplayName("unchecked exceptions get raised back as expected")
  @Test
  void uncheckedExceptionsGetRaisedBackAsExpected() {
    // Given
    var ex = new UncheckedIOException("Uh-oh", new IOException("welp"));

    // Then
    assertThatException()
        .isThrownBy(() -> Unchecked.call(() -> {
          throw ex;
        }))
        .isSameAs(ex);
  }

  @DisplayName("checked exceptions get raised back as expected")
  @Test
  void checkedExceptionsGetRaisedBackAsExpected() {
    // Given
    var ex = new IOException("welp");

    // Then
    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> Unchecked.call(() -> {
          throw ex;
        }))
        .withMessage(
            "Checked exception raised unexpectedly, this is a bug. Exception was: %s",
            ex
        )
        .havingCause()
        .isSameAs(ex);
  }
}
