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

package io.github.ascopes.protobufmavenplugin.protoc;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.util.List;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Named;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Executor for {@code protoc} commands.
 *
 * @author Ashley Scopes
 */
@Named
public final class CommandLineExecutor {

  private static final Logger log = LoggerFactory.getLogger(CommandLineExecutor.class);

  @Inject
  public CommandLineExecutor() {
    // Nothing to do.
  }

  public boolean execute(List<String> args) throws IOException {
    log.info("Invoking protoc");
    log.debug("Protoc invocation will occur with the following arguments: {}", args);

    var procBuilder = new ProcessBuilder(args);
    procBuilder.environment().putAll(System.getenv());

    try {
      return run(procBuilder);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      var newEx = new InterruptedIOException("Execution was interrupted");
      newEx.initCause(ex);
      throw newEx;
    }
  }

  private boolean run(ProcessBuilder procBuilder) throws InterruptedException, IOException {
    var startTimeNs = System.nanoTime();
    var proc = procBuilder.start();

    var stdoutThread = redirectOutput(proc.getInputStream(), log::info);
    var stderrThread = redirectOutput(proc.getErrorStream(), log::warn);

    var exitCode = proc.waitFor();
    var elapsedTimeMs = (System.nanoTime() - startTimeNs) / 1_000_000L;

    // Ensure we've flushed the logs through before continuing.
    stdoutThread.join();
    stderrThread.join();

    if (exitCode == 0) {
      log.info("protoc returned exit code 0 (success) after {}ms", elapsedTimeMs);
      return true;
    } else {
      log.error("protoc returned exit code {} (error) after {}ms", exitCode, elapsedTimeMs);
      return false;
    }
  }

  private Thread redirectOutput(InputStream stream, Consumer<String> logger) {
    // We shouldn't need to flag to these threads to stop as they should immediately
    // terminate once the input streams are closed.
    var thread = new Thread(() -> {
      String line;
      try (var reader = new BufferedReader(new InputStreamReader(stream))) {
        while ((line = reader.readLine()) != null) {
          logger.accept(line.stripTrailing());
        }
      } catch (IOException ex) {
        log.error("Stream error, output will be discarded", ex);
      }
    });

    thread.setDaemon(true);
    thread.setName("protoc output redirector thread for " + stream);
    thread.start();
    return thread;
  }
}
