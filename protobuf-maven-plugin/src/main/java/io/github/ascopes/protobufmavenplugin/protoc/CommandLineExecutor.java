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

    log.info("Invoking protoc");
    log.debug("Protoc binary is located at {}", protocPath);

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

  private boolean runProcess(ProcessBuilder procBuilder) throws InterruptedException, IOException {
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
}
