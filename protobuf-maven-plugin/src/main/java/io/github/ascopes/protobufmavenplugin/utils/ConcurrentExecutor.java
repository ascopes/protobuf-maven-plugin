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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import javax.annotation.PreDestroy;
import javax.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper component that allows scheduling IO-bound tasks within a thread pool.
 *
 * @author Ashley Scopes
 * @since 2.2.0
 */
@Named
public final class ConcurrentExecutor {

  private static final Logger log = LoggerFactory.getLogger(ConcurrentExecutor.class);
  private final ExecutorService executorService;

  public ConcurrentExecutor() {
    ExecutorService executorService;

    try {
      log.debug("Trying to create new Loom virtual thread pool");
      executorService = (ExecutorService) Executors.class
          .getMethod("newVirtualThreadPerTaskExecutor")
          .invoke(null);

      log.debug("Loom virtual thread pool creation was successful!");

    } catch (Exception ex) {
      var concurrency = Runtime.getRuntime().availableProcessors() * 8;
      log.debug(
          "Falling back to new work-stealing thread pool (concurrency={}, Loom is unavailable)",
          concurrency,
          ex
      );
      executorService = Executors.newWorkStealingPool(concurrency);
    }

    this.executorService = executorService;
  }

  /**
   * Destroy the internal thread pool.
   *
   * @throws InterruptedException if destruction timed out or the thread was interrupted.
   */
  @PreDestroy
  @SuppressWarnings({"ResultOfMethodCallIgnored", "unused"})
  public void destroy() throws InterruptedException {
    log.debug("Shutting down executor...");
    executorService.shutdown();
    log.debug("Awaiting executor termination...");

    // If this fails, then we can't do much about it. Force shutdown and hope threads don't
    // deadlock. Not going to bother adding complicated handling here as if we get stuck, we
    // likely have far bigger problems to deal with.
    executorService.awaitTermination(10, TimeUnit.SECONDS);
    var remaining = executorService.shutdownNow();
    log.debug("Shutdown ended, stubborn tasks that will be orphaned: {}", remaining);
  }

  public <R> FutureTask<R> submit(Callable<R> task) {
    var futureTask = new FutureTask<>(task);
    executorService.submit(futureTask);
    return futureTask;
  }

  /**
   * Return a reactive collector of all the results of a stream of scheduled tasks.
   *
   * @param <R> the task return type.
   * @return the collector.
   * @throws MultipleFailuresException if any of the results raised exceptions. All results are
   *                                   collected prior to this being raised.
   */
  public <R> Collector<FutureTask<R>, ?, List<R>> awaiting() {
    return Collectors.collectingAndThen(Collectors.toUnmodifiableList(), this::await);
  }

  private <R> List<R> await(List<FutureTask<R>> scheduledTasks) {
    try {
      var results = new ArrayList<R>();
      var exceptions = new ArrayList<Throwable>();

      for (var task : scheduledTasks) {
        try {
          results.add(task.get());
        } catch (ExecutionException ex) {
          exceptions.add(ex.getCause());
        } catch (InterruptedException ex) {
          Thread.currentThread().interrupt();
          return results;
        }
      }

      if (!exceptions.isEmpty()) {
        throw MultipleFailuresException.create(exceptions);
      }

      return Collections.unmodifiableList(results);

    } finally {
      // Interrupt anything that didn't complete if we get interrupted on the OS level.
      for (var task : scheduledTasks) {
        task.cancel(true);
      }
    }
  }
}
