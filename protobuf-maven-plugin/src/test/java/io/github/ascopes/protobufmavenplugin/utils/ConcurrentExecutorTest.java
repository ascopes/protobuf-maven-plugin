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
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
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
    executor.executorService.shutdownNow();
  }

  @Timeout(value = 20_000, unit = TimeUnit.MILLISECONDS)
  @DisplayName(".destroy() succeeds if the executor service is idle")
  @Test
  void destroySucceedsIfExecutorServiceIsIdle() throws Exception {
    // Given
    assertThat(executor.executorService.isTerminated())
        .as("executorService.isTerminated()")
        .isFalse();
    assertThat(executor.executorService.isShutdown())
        .as("executorService.isShutdown()")
        .isFalse();

    // When
    executor.destroy();

    // Then
    assertThat(executor.executorService.isTerminated())
        .as("executorService.isTerminated()")
        .isTrue();
    assertThat(executor.executorService.isShutdown())
        .as("executorService.isShutdown()")
        .isTrue();
  }

  @Timeout(value = 20_000, unit = TimeUnit.MILLISECONDS)
  @DisplayName(".destroy() succeeds if the executor service is already terminated")
  @Test
  void destroySucceedsIfExecutorServiceIsAlreadyTerminated() throws Exception {
    // Given
    executor.executorService.shutdownNow();

    // When
    executor.destroy();

    // Then
    assertThat(executor.executorService.isTerminated())
        .as("executorService.isTerminated()")
        .isTrue();
    assertThat(executor.executorService.isShutdown())
        .as("executorService.isShutdown()")
        .isTrue();
  }

  @Timeout(value = 20_000, unit = TimeUnit.MILLISECONDS)
  @DisplayName(".destroy() interrupts any interruptable running tasks")
  @Test
  void destroyInterruptsAnyInterruptableRunningTasks() throws Exception {
    // Given
    var task1 = new FutureTask<>(() -> sleepWait(10_000));
    var task2 = new FutureTask<>(() -> sleepWait(10_000));

    executor.executorService.submit(task1);
    executor.executorService.submit(task2);

    // Give tasks the chance to start.
    Thread.sleep(1_000);

    // When
    executor.destroy();

    // Then
    assertThatExceptionOfType(ExecutionException.class)
        .as("exception raised by task1 (%s)", task1)
        .isThrownBy(task1::get)
        .withCauseInstanceOf(InterruptedException.class);
    assertThatExceptionOfType(ExecutionException.class)
        .as("exception raised by task2 (%s)", task2)
        .isThrownBy(task2::get)
        .withCauseInstanceOf(InterruptedException.class);
  }

  @Timeout(value = 20_000, unit = TimeUnit.MILLISECONDS)
  @DisplayName(".destroy() abandons any uninterruptable running tasks")
  @Test
  void destroyAbandonsAnyUninterruptableRunningTasks() throws Exception {
    // Given
    var task1 = new FutureTask<>(() -> spinWait(10_000));
    var task2 = new FutureTask<>(() -> spinWait(10_000));

    executor.executorService.submit(task1);
    executor.executorService.submit(task2);

    // Give tasks the chance to start.
    Thread.sleep(1_000);

    // When
    executor.destroy();

    assertThat(task1)
        .as("task1 %s", task1)
        .isNotDone();
    assertThat(task2)
        .as("task1 %s", task2)
        .isNotDone();

    // Then
    assertThatExceptionOfType(TimeoutException.class)
        .as("exception raised waiting for task1 (%s)", task1)
        .isThrownBy(() -> task1.get(100, TimeUnit.MILLISECONDS));
    assertThatExceptionOfType(TimeoutException.class)
        .as("exception raised waiting for task2 (%s)", task2)
        .isThrownBy(() -> task2.get(100, TimeUnit.MILLISECONDS));
  }

  @Timeout(value = 20_000, unit = TimeUnit.MILLISECONDS)
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

  @Timeout(value = 20_000, unit = TimeUnit.MILLISECONDS)
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

  @Timeout(value = 20_000, unit = TimeUnit.MILLISECONDS)
  @DisplayName(".submit(Callable) calls the callable and handles interruption")
  @Test
  void submitCallsCallableAndHandlesInterruption() {
    // Given
    Callable<Void> callable = () -> {
      Thread.sleep(30_000);
      return null;
    };

    // When
    var future = executor.submit(callable);
    future.cancel(true);

    // Then
    assertThatExceptionOfType(CancellationException.class)
        .isThrownBy(future::get);
  }

  // If we take more than 20 seconds on the 1000 instance case, we've probably run sequentially.
  @Timeout(value = 20_000, unit = TimeUnit.MILLISECONDS)
  @DisplayName(".submit(Callable) submits and executes the given tasks in parallel")
  @ValueSource(ints = {1, 2, 3, 5, 10, 100, 1_000})
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
  @Timeout(value = 20_000, unit = TimeUnit.MILLISECONDS)
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
  @Timeout(value = 20_000, unit = TimeUnit.MILLISECONDS)
  @Test
  void awaitingAwaitsAllTasksAndRaisesTheirExceptions() {
    // Given
    List<Throwable> expectedExceptions = Stream.generate(Exception::new)
        .limit(50)
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

  @DisplayName(".awaiting() awaits all tasks and handles interruptions")
  @Timeout(value = 10_000, unit = TimeUnit.MILLISECONDS)
  @Test
  void awaitingAwaitsAllTasksAndHandlesInterruptions() {
    // Given
    List<FutureTask<Void>> tasks = new ArrayList<>();
    for (var i = 0; i < 1000; ++i) {
      // Local copy to prevent the lambda reading the mutable value from the closure.
      tasks.add(executor.submit(() -> {
        Thread.sleep(10_000);
        return null;
      }));
    }

    // When
    tasks.forEach(task -> task.cancel(true));

    // Then
    assertThatExceptionOfType(MultipleFailuresException.class)
        .isThrownBy(() -> tasks.stream().collect(executor.awaiting()))
        .satisfies(
            ex -> assertThat(ex.getCause()).isInstanceOf(CancellationException.class),
            ex -> assertThat(ex.getSuppressed())
                .hasSize(999)
                .allSatisfy(suppressed -> assertThat(suppressed)
                    .isInstanceOf(CancellationException.class))
        );
  }

  // Sleep-based waits can consume thread interrupts and can be cancelled,
  // representing some IO-bound work that cancels gracefully.
  @SuppressWarnings({"BusyWait", "SameParameterValue"})
  private static @Nullable Void sleepWait(int timeoutMs) throws InterruptedException {
    var deadline = System.nanoTime() + timeoutMs * 1_000_000L;
    do {
      try {
        var remaining = deadline - System.nanoTime();
        Thread.sleep(remaining / 1_000_000L);
      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        throw ex;
      }
    } while (deadline - System.nanoTime() > 0);
    return null;
  }

  // Spin-based waits should not perform IO, so should be un-cancellable and
  // uninterruptible, representing CPU bound work or a buggy/stubborn task.
  @SuppressWarnings("SameParameterValue")
  private static @Nullable Void spinWait(int timeoutMs) {
    var deadline = System.nanoTime() + timeoutMs * 1_000_000L;
    // Do not perform anything that can be interrupted.
    while (deadline - System.nanoTime() > 0) {
      // Keep checkstyle happy by having a body on this loop.
      continue;
    }
    return null;
  }
}
