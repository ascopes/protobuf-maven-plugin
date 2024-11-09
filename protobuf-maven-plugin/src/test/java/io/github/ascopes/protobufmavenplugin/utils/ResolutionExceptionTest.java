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

import static io.github.ascopes.protobufmavenplugin.fixtures.RandomFixtures.someText;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;


/**
 * @author Ashley Scopes
 */
@DisplayName("ResolutionException tests")
class ResolutionExceptionTest {

  @DisplayName("I can initialize the exception with a message")
  @Test
  void canInitializeExceptionWithMessage() {
    // Given
    var message = someText();

    // When
    var ex = new ResolutionException(message);

    // Then
    assertThat(ex)
        .hasMessage(message)
        .hasNoCause()
        .hasNoSuppressedExceptions();
  }

  @DisplayName("I can initialize the exception with a message and a cause")
  @Test
  void canInitializeExceptionWithMessageAndCause() {
    // Given
    var cause = new RuntimeException("Well, damn");
    var message = someText();

    // When
    var ex = new ResolutionException(message, cause);

    // Then
    assertThat(ex)
        .hasMessage(message)
        .hasCause(cause)
        .hasNoSuppressedExceptions();
  }
}
