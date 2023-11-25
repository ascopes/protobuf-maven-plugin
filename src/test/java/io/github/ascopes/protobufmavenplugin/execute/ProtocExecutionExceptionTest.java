/*
 * Copyright (C) 2023, Ashley Scopes.
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
package io.github.ascopes.protobufmavenplugin.execute;

import static io.github.ascopes.protobufmavenplugin.fixture.RandomData.someString;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ProtocExecutionException tests")
class ProtocExecutionExceptionTest {

  @DisplayName("ProtocExecutionException can be constructed with a message and cause parameter")
  @Test
  void canBeConstructedWithMessageAndCauseParameter() {
    // Given
    var message = someString();
    var cause = new Throwable(someString());

    // When
    var ex = new ProtocExecutionException(message, cause);

    // Then
    assertThat(ex)
        .hasMessage(message)
        .hasCause(cause)
        .hasNoSuppressedExceptions();
  }
}
