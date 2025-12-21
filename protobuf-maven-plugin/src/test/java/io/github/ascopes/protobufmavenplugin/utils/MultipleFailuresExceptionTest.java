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

import static io.github.ascopes.protobufmavenplugin.fixtures.RandomFixtures.someBasicString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @author Ashley Scopes
 */
@DisplayName("MultipleFailuresException tests")
class MultipleFailuresExceptionTest {

  @DisplayName("the exception has the expected message for a single exception")
  @Test
  void exceptionHasTheExpectedMessageForSingleException() {
    // Given
    var cause = new IllegalArgumentException(someBasicString());

    // When
    var exception = MultipleFailuresException.create(List.of(cause));

    // Then
    assertThat(exception)
        .hasMessage(
            "A failure occurred during a concurrent task: %s: %s",
            cause.getClass().getName(),
            cause.getMessage()
        );
  }

  @DisplayName("the exception has the expected message for multiple exceptions")
  @ValueSource(ints = {2, 5, 10})
  @ParameterizedTest(name = "for {0} exception(s)")
  void exceptionHasTheExpectedMessageForMultipleExceptions(int causeCount) {
    // Given
    var cause = new IllegalStateException(someBasicString());
    var suppressed = Stream.generate(() -> new Exception(someBasicString()))
        .limit(causeCount - 1);

    var causes = Stream.concat(Stream.of(cause), suppressed)
        .collect(Collectors.toList());

    // When
    var exception = MultipleFailuresException.create(causes);

    // Then
    assertThat(exception)
        .hasMessage(
            "%d failures occurred during a concurrent task. The first was: %s: %s",
            causeCount,
            cause.getClass().getName(),
            cause.getMessage()
        );
  }

  @DisplayName("the first exception is treated as the cause")
  @ValueSource(ints = {1, 5, 10})
  @ParameterizedTest(name = "for {0} exception(s)")
  void firstExceptionIsTreatedAsCause(int causeCount) {
    // Given
    var causes = Stream.generate(() -> new Exception(someBasicString()))
        .limit(causeCount)
        .collect(Collectors.toList());

    // When
    var exception = MultipleFailuresException.create(causes);

    // Then
    assertThat(exception).hasCause(causes.get(0));
  }

  @DisplayName("a single exception results in no suppressed exceptions")
  @Test
  void singleExceptionResultsInNoSuppressions() {
    // Given
    var cause = new Exception(someBasicString());

    // When
    var exception = MultipleFailuresException.create(List.of(cause));

    // Then
    assertThat(exception).hasNoSuppressedExceptions();
  }

  @DisplayName("the rest of the exceptions are suppressed")
  @ValueSource(ints = {2, 5, 10})
  @ParameterizedTest(name = "for {0} exception(s)")
  void restOfExceptionsAreSuppressed(int causeCount) {
    // Given
    var causes = Stream.generate(() -> new Exception(someBasicString()))
        .limit(causeCount)
        .collect(Collectors.toList());

    // When
    var exception = MultipleFailuresException.create(causes);

    // Then
    assertSoftly(softly -> {
      var iter = causes.iterator();

      softly.assertThat(exception.getSuppressed())
          .as("suppressed exceptions")
          .doesNotContain(iter.next());

      iter.forEachRemaining(cause -> softly.assertThat(exception).hasSuppressedException(cause));
    });
  }
}
