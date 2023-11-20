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

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.ProcessBuilder.Redirect;
import java.time.Duration;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Helper that invokes a built {@code protoc} invocation.
 *
 * @author Ashley Scopes
 */
public final class ProtocExecutor {
  private static final Logger LOGGER = LoggerFactory.getLogger(ProtocExecutor.class);

  private final List<String> arguments;

  /**
   * Initialise this executor.
   *
   * @param argumments the command line arguments, starting with the executable.
   */
  ProtocExecutor(List<String> arguments) {
    this.arguments = arguments;
  }

  /**
   * Invoke {@code protoc}.
   *
   * @return the exit code.
   *
   * @throws ProtocExecutionException if the invocation fails or is interrupted.
   */
  public int invoke() throws ProtocExecutionException {
    try {
      var start = System.nanoTime();
      var proc = new ProcessBuilder()
          .command(arguments)
          .redirectErrorStream(true)
          .start();

      LOGGER.info(
          "Executing protoc (pid {}, arguments: {})",
          proc.pid(),
          String.join(" ", arguments)
      );

      try {
        var exitCode = proc.waitFor();
        var elapsedTime = (System.nanoTime() - start) / 1_000_000;

        LOGGER.info(
            "Protoc (pid {}) returned exit code {} after ~{}ms",
            proc.pid(),
            exitCode,
            elapsedTime
        );
        return exitCode;

      } catch (InterruptedException ex) {
        Thread.currentThread().interrupt();
        proc.destroy();
        throw new ProtocExecutionException("Execution was interrupted", ex);
      }

    } catch (IOException ex) {
      throw new ProtocExecutionException("Failed to invoke protoc process", ex);
    }
  }
}
