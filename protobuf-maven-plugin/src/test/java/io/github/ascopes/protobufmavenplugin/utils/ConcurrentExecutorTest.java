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

import static io.github.ascopes.protobufmavenplugin.fixtures.RandomFixtures.someInt;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.github.ascopes.protobufmavenplugin.fixtures.RandomFixtures;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;


/**
 * @author Ashley Scopes
 */
@DisplayName("ConcurrentExecutor tests")
class ConcurrentExecutorTest {

  ConcurrentExecutor executor;

  @BeforeEach
  void setUp() {
    executor = new ConcurrentExecutor();
  }

  @AfterEach
  void tearDown() throws InterruptedException {
    executor.destroy();
  }

  @Timeout(value = 10_000, unit = TimeUnit.MILLISECONDS)
  @DisplayName(".submit(Callable) calls the callable and returns the result")
  @Test
  void submitCallsCallableAndReturnsResult() throws Exception {
    // Given
    var expectedResult = someInt();
    Callable<Integer> callable = mock();
    when(callable.call()).thenReturn(expectedResult);

    // When
    var future = executor.submit(callable);

    // Then
    var actualResult = future.get();
    assertThat(actualResult).isEqualTo(expectedResult);

    verify(callable).call();
    verifyNoMoreInteractions(callable);
  }

  @Timeout(value = 10_000, unit = TimeUnit.MILLISECONDS)
  @DisplayName(".submit(Callable) calls the callable and raises any exception")
  @Test
  void submitCallsCallableAndRaisesAnyException() throws Exception {
    // Given
    var expectedException = new Exception("welp");
    Callable<Integer> callable = mock();
    when(callable.call()).thenThrow(expectedException);

    // When
    var future = executor.submit(callable);

    // Then
    assertThatExceptionOfType(ExecutionException.class)
        .isThrownBy(future::get)
        .havingCause()
        .isSameAs(expectedException);

    verify(callable).call();
    verifyNoMoreInteractions(callable);
  }

  // If we take more than 20 seconds on the 1000 instance case, we've probably run sequentially.
  @Timeout(value = 20_000, unit = TimeUnit.MILLISECONDS)
  @DisplayName(".submit(Callable) submits and executes the given tasks in parallel")
  @ValueSource(ints = {1, 2, 3, 5, 10, 100, 10_000})
  @ParameterizedTest(name = "for {0} task(s)")
  void submitExecutesTheGivenTasksInParallel(int taskCount) throws Exception {
    // When
    var tasks = new ArrayList<FutureTask<?>>();
    for (var i = 0; i < taskCount; ++i) {
      tasks.add(executor.submit(() -> {
        Thread.sleep(5);
        return null;
      }));
    }

    // Then
    for (var task : tasks) {
      task.get();
    }
  }

  @DisplayName(".awaiting() awaits all tasks and returns their results")
  @Timeout(value = 10_000, unit = TimeUnit.MILLISECONDS)
  @Test
  void awaitingAwaitsAllTasksAndReturnsTheirResults() {
    // Given
    var expectedResults = IntStream.generate(RandomFixtures::someInt)
        .limit(100)
        .boxed()
        .collect(Collectors.toList());

    List<FutureTask<Integer>> tasks = new ArrayList<>();
    for (var i = 0; i < expectedResults.size(); ++i) {
      // Local copy to prevent the lambda reading the mutable value from the closure.
      final int index = i;
      tasks.add(executor.submit(() -> {
        Thread.sleep(1_000);
        return expectedResults.get(index);
      }));
    }

    // When
    var actualResults = tasks.stream().collect(executor.awaiting());

    // Then
    assertThat(actualResults).containsExactlyInAnyOrderElementsOf(expectedResults);
  }

  @DisplayName(".awaiting() awaits all tasks and raises their exceptions")
  @SuppressWarnings("ResultOfMethodCallIgnored")
  @Timeout(value = 10_000, unit = TimeUnit.MILLISECONDS)
  @Test
  void awaitingAwaitsAllTasksAndRaisesTheirExceptions() {
    // Given
    List<Throwable> expectedExceptions = Stream.generate(Exception::new)
        .limit(100)
        .collect(Collectors.toList());

    List<FutureTask<Integer>> tasks = new ArrayList<>();
    for (var i = 0; i < expectedExceptions.size(); ++i) {
      // Local copy to prevent the lambda reading the mutable value from the closure.
      final int index = i;
      tasks.add(executor.submit(() -> {
        Thread.sleep(1_000);
        throw (Exception) expectedExceptions.get(index);
      }));
    }

    // Then
    assertThatExceptionOfType(MultipleFailuresException.class)
        .isThrownBy(() -> tasks.stream().collect(executor.awaiting()))
        .satisfies(
            ex -> assertThat(ex.getCause()).isInstanceOf(Exception.class),
            ex -> assertThat(ex.getSuppressed()).allSatisfy(
                suppressed -> assertThat(suppressed).isInstanceOf(Exception.class)
            ),
            ex -> assertThat(expectedExceptions).contains(ex.getCause()),
            ex -> assertThat(expectedExceptions).containsAll(List.of(ex.getSuppressed())),
            ex -> assertThat(expectedExceptions).hasSize(ex.getSuppressed().length + 1)
        );
  }
}
