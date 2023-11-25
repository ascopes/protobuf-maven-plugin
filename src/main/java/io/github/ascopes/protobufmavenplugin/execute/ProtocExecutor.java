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

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executor for commands.
 *
 * @author Ashley Scopes
 */
public final class ProtocExecutor {

  private static final Logger LOGGER = LoggerFactory.getLogger(ProtocExecutor.class);

  /**
   * Initialise the executor.
   */
  public ProtocExecutor() {
  }

  /**
   * Invoke the process with the given arguments.
   *
   * <p>All process output streams will be piped to the class logger.
   *
   * @param arguments the arguments to invoke the process with.
   * @throws ProtocExecutionException if the execution fails.
   */
  public void invoke(List<String> arguments) throws ProtocExecutionException {
    try {
      LOGGER.info("Invoking {}", arguments);

      var start = System.nanoTime();
      long elapsed;

      var proc = new ProcessBuilder(arguments)
          .redirectErrorStream(true)
          .start();

      var loggingFuture = streamOutputAsLogs(proc.getInputStream());

      try {
        var exitCode = proc.waitFor();
        elapsed = System.nanoTime() - start;

        if (exitCode != 0) {
          // Dump all output in case it has some form of error message.
          throw new ProtocExecutionException(proc.waitFor());
        }
      } finally {
        loggingFuture.join();
        proc.destroy();
      }

      LOGGER.info("Protoc completed after {}ms", elapsed / 1_000_000L);
    } catch (IOException ex) {
      LOGGER.debug("Execution failed due to an IO exception", ex);
      throw ioExceptionCallingProcess(ex);
    } catch (InterruptedException ex) {
      LOGGER.debug("Execution interrupted", ex);
      throw processInterrupted(ex);
    }
  }

  private ProtocExecutionException ioExceptionCallingProcess(IOException ex) {
    return new ProtocExecutionException("An IO exception occurred while calling protoc", ex);
  }

  private ProtocExecutionException processInterrupted(InterruptedException ex) {
    Thread.currentThread().interrupt();
    return new ProtocExecutionException("Protoc execution was interrupted", ex);
  }

  private CompletableFuture<?> streamOutputAsLogs(InputStream inputStream) {
    return CompletableFuture.runAsync(() -> {
      try (var reader = new BufferedReader(new InputStreamReader(inputStream, UTF_8))) {
        String line;

        while ((line = reader.readLine()) != null) {
          LOGGER.info(">>> {}", line);
        }
      } catch (Throwable ex) {
        // Nothing else we can do. This probably can never happen?
        // Don't throw anything back to the caller as we want to ensure the process is destroyed
        // safely after joining this future.
        LOGGER.error("Critical error reading output of subprocess", ex);
      }
    });
  }
}
