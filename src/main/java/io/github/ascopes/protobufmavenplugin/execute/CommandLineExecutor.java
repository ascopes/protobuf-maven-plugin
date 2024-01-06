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
package io.github.ascopes.protobufmavenplugin.execute;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executor for commands.
 *
 * @author Ashley Scopes
 */
@Named
public final class CommandLineExecutor {

  private static final Logger log = LoggerFactory.getLogger(CommandLineExecutor.class);

  @Inject
  public CommandLineExecutor() {
    // Static-only class.
  }

  public boolean execute(List<String> args) throws IOException {
    log.info("Calling protoc with the following command line: {}", args);

    var procBuilder = new ProcessBuilder(args);
    procBuilder.environment().putAll(System.getenv());
    procBuilder.inheritIO();

    try {
      var startTimeNs = System.nanoTime();
      var proc = procBuilder.start();
      var exitCode = proc.waitFor();
      var elapsedTimeMs = ms(System.nanoTime() - startTimeNs);

      if (exitCode == 0) {
        log.info("Protoc completed after {}ms", elapsedTimeMs);
        return true;
      } else {
        log.error("Protoc returned exit code {} after {}ms", exitCode, elapsedTime);
        return false;
      }

    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      var newEx = new InterruptedIOException("Compilation was interrupted");
      newEx.initCause(ex);
      throw newEx;
    }
  }

  private static long ms(long ns) {
    return ns / 1_000_000L;
  }
}
