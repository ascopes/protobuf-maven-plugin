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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper component that allows scheduling IO-bound tasks within a thread pool.
 *
 * @author Ashley Scopes
 * @since 2.2.0
 */
@Named
@Singleton  // Only one instance globally to avoid resource exhaustion
public final class ConcurrentExecutor {

  private static final Logger log = LoggerFactory.getLogger(ConcurrentExecutor.class);

  // Visible for testing only.
  final ExecutorService executorService;

  @Inject
  public ConcurrentExecutor() {
    executorService = newVirtualThreadExecutor()
        .orElseGet(ConcurrentExecutor::newPlatformThreadExecutor);
  }

  /**
   * Destroy the internal thread pool.
   */
  @PreDestroy
  @SuppressWarnings("unused")
  public void destroy() {
    log.debug("Shutting down executor...");
    var remainingTasks = executorService.shutdownNow();
    log.debug("Remaining tasks that will be orphaned: {}", remainingTasks);
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

  // Awaits each task, in the order it was scheduled. Any interrupt is caught and terminates
  // the entire batch.
  private <R> List<R> await(List<FutureTask<R>> scheduledTasks) {
    try {
      var results = new ArrayList<R>();
      var exceptions = new ArrayList<Throwable>();

      for (var task : scheduledTasks) {
        try {
          results.add(task.get());
        } catch (ExecutionException ex) {
          exceptions.add(ex.getCause());
        } catch (CancellationException | InterruptedException ex) {
          exceptions.add(ex);
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

  private static Optional<ExecutorService> newVirtualThreadExecutor() {
    try {
      log.debug("Trying to create new Loom virtual thread pool");
      var executorService = Executors.class
          .getMethod("newVirtualThreadPerTaskExecutor")
          .invoke(null);

      log.debug("Loom virtual thread pool creation was successful!");
      return Optional.of(executorService)
          .map(ExecutorService.class::cast);

    } catch (Exception ex) {
      log.debug(
          "Loom virtual thread pool is not available on this platform, continuing anyway...",
          ex
      );
      return Optional.empty();
    }
  }

  private static ExecutorService newPlatformThreadExecutor() {
    var cpuCores = Runtime.getRuntime().availableProcessors();
    var initialThreads = 8;
    var maxThreads = cpuCores * 8;
    var keepAliveSeconds = 15;
    var workQueue = new LinkedBlockingQueue<Runnable>();

    return new ThreadPoolExecutor(
        initialThreads,
        maxThreads,
        keepAliveSeconds,
        TimeUnit.SECONDS,
        workQueue
    );
  }
}
