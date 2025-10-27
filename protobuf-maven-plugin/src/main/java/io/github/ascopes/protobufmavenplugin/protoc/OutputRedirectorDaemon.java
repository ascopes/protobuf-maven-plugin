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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

/**
 * Helper that consumes a stream for a subprocess on a separate thread,
 * formatting and emitting the output to a logger asynchronously.
 *
 * @author Ashley Scopes
 * @since 2.10.4
 */
final class OutputRedirectorDaemon {
  private final String name;
  private final long pid;
  private final BufferedReader reader;
  private final Logger logger;
  private final Thread thread;

  OutputRedirectorDaemon(
      String name,
      long pid,
      InputStream inputStream,
      Logger logger
  ) {
    this.name = name;
    this.pid = pid;
    reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
    this.logger = logger;
    thread = new Thread(this::redirect);
    thread.setDaemon(true);
    thread.setName("Log redirector for pid=" + pid + " - " + name);
    thread.start();
  }

  void await() throws InterruptedException {
    thread.join();
  }

  private void redirect() {
    try {
      String line;
      while ((line = reader.readLine()) != null) {
        logger.log("[{} pid={}] {}", name, pid, line.stripTrailing());
      }
    } catch (IOException ex) {
      logger.log(
          "[{} pid={}] Internal error intercepting logs!",
          name,
          pid,
          ex
      );
    }
  }

  @FunctionalInterface
  interface Logger {
    void log(String format, Object ... args);
  }
}
