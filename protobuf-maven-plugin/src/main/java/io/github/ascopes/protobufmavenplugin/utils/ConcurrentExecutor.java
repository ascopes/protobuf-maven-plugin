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
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.execution.scope.MojoExecutionScoped;
import org.eclipse.sisu.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper component that allows scheduling IO-bound tasks within a thread pool.
 *
 * @author Ashley Scopes
 * @since 2.2.0
 */
@Description("Manages an execution-wide thread pool for concurrent task execution")
@MojoExecutionScoped
@Named
public final class ConcurrentExecutor {

  private static int DEFAULT_MAXIMUM_CONCURRENCY = 80;
  private static int DEFAULT_MINIMUM_CONCURRENCY = 4;
  private static int DEFAULT_CONCURRENCY_MULTIPLIER = 8;
  private static final String CONCURRENCY_PROPERTY = "protobuf.executor.maxThreads";

  private static final Logger log = LoggerFactory.getLogger(ConcurrentExecutor.class);

  // Visible for testing only.
  final ExecutorService executorService;

  @Inject
  public ConcurrentExecutor() {
    // Prior to 2.13.0, we used unbounded thread pools, utilising virtual threads when
    // available. This was somewhat risky in hindsight as we could easily load a large
    // number of things into memory when analysing dependencies and then run
    // out of heap space to consume.
    //
    // As of 2.13.0, I have removed all of this and reverted to a basic work stealing pool
    // so that we have full control of the concurrency.
    //
    // Concurrency will be determined by a multiplier of the number of physical
    // CPU cores available, and is overridable via a system property if the user
    // wishes to take further control of this.

    var runtime = Runtime.getRuntime();
    var concurrency = determineConcurrency(runtime.availableProcessors());
    executorService = Executors.newWorkStealingPool(concurrency);
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

  // Visible for testing only.
  static int determineConcurrency(int cpuCount) {
    var defaultConcurrency = Math.min(
        Math.max(
            DEFAULT_CONCURRENCY_MULTIPLIER * cpuCount,
            DEFAULT_MINIMUM_CONCURRENCY
        ),
        DEFAULT_MAXIMUM_CONCURRENCY
    );

    var concurrency = Integer.getInteger(
        CONCURRENCY_PROPERTY,
        defaultConcurrency
    );

    if (concurrency < 1) {
      log.warn(
          "Concurrency has been overridden to an invalid value ({}). "
              + "This will be ignored and a concurrency of {} will be used instead.",
          concurrency,
          DEFAULT_MINIMUM_CONCURRENCY
      );
      concurrency = DEFAULT_MINIMUM_CONCURRENCY;
    }

    log.debug(
        "Effective concurrency is {}, default concurrency is {}. "
            + "Override this by passing -D{}=value",
        concurrency,
        defaultConcurrency,
        CONCURRENCY_PROPERTY
    );

    return concurrency;
  }
}
