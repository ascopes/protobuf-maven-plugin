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
package io.github.ascopes.protobufmavenplugin.protoc;

import io.github.ascopes.protobufmavenplugin.utils.ArgumentFileBuilder;
import io.github.ascopes.protobufmavenplugin.utils.TeeWriter;
import io.github.ascopes.protobufmavenplugin.utils.TemporarySpace;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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
  private final TemporarySpace temporarySpace;

  @Inject
  public CommandLineExecutor(TemporarySpace temporarySpace) {
    this.temporarySpace = temporarySpace;
  }

  public boolean execute(
      Path protocPath,
      ArgumentFileBuilder argumentFileBuilder
  ) throws IOException {
    var argumentFile = writeArgumentFile(argumentFileBuilder);

    log.info("Invoking protoc (enable debug logs for more details)");
    log.debug("Protoc binary is located at {}", protocPath);
    log.debug("Protoc argument file:\n{},", argumentFileBuilder);

    var procBuilder = new ProcessBuilder(protocPath.toString(), "@" + argumentFile);
    procBuilder.environment().putAll(System.getenv());

    try {
      return runProcess(procBuilder);
    } catch (InterruptedException ex) {
      Thread.currentThread().interrupt();
      var newEx = new InterruptedIOException("Execution was interrupted");
      newEx.initCause(ex);
      throw newEx;
    }
  }

  private Path writeArgumentFile(ArgumentFileBuilder argumentFileBuilder) throws IOException {
    var file = temporarySpace.createTemporarySpace("protoc").resolve("args.txt");
    log.debug("Writing to protoc argument file at {}", file);

    var writer = new TeeWriter(Files.newBufferedWriter(file, StandardCharsets.UTF_8));
    try (writer) {
      argumentFileBuilder.writeToProtocArgumentFile(writer);
    }

    log.debug("Written arguments were:\n{}", writer);
    return file;
  }

  private boolean runProcess(ProcessBuilder procBuilder) throws InterruptedException, IOException {
    final var startTimeNs = System.nanoTime();
    final var proc = procBuilder.start();

    final var stdoutThread = redirectOutput("stdout", proc.pid(), proc.getInputStream(), log::info);
    final var stderrThread = redirectOutput("stderr", proc.pid(), proc.getErrorStream(), log::warn);

    final var exitCode = proc.waitFor();
    final var elapsedTimeMs = (System.nanoTime() - startTimeNs) / 1_000_000L;

    // Ensure we've flushed the logs through before continuing.
    log.trace("Waiting for stdout and stderr threads to terminate...");
    stdoutThread.join();
    stderrThread.join();
    log.trace("Stdout and stderr threads terminated");

    if (exitCode == 0) {
      log.info(
          "protoc (pid {}) returned exit code 0 (success) after {}ms",
          proc.pid(),
          elapsedTimeMs
      );
      return true;

    } else {
      log.error(
          "protoc (pid {)} returned exit code {} (error) after {}ms",
          proc.pid(),
          exitCode,
          elapsedTimeMs
      );
      return false;
    }
  }

  private Thread redirectOutput(
      String streamName,
      long pid,
      InputStream stream,
      Consumer<String> logger
  ) {
    // We shouldn't need to flag to these threads to stop as they should immediately
    // terminate once the input streams are closed.
    var thread = new Thread(() -> {
      String line;
      try (var reader = new BufferedReader(new InputStreamReader(stream))) {
        while ((line = reader.readLine()) != null) {
          var entry = String.format(
              "[protoc (pid %d) - %s] %s",
              pid,
              streamName,
              line.stripTrailing()
          );
          logger.accept(entry);
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
