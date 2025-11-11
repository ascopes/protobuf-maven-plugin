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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.withSettings;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;


@DisplayName("OutputRedirectorDaemon tests")
class OutputRedirectorDaemonTest {

  @DisplayName("logs are emitted to the logger linewise")
  @Test
  @Timeout(10)
  void logsAreEmittedToTheLoggerLinewise() throws Exception {
    var inputStream = new ByteArrayInputStream(
        "Foo bar baz\nDo ray me\nEggs spam".getBytes(StandardCharsets.UTF_8)
    );
    OutputRedirectorDaemon.Logger logger = mock();
    new OutputRedirectorDaemon(
        "foobar",
        1243L,
        inputStream,
        logger
    );

    // When
    Thread.sleep(500);

    // Then
    verify(logger).log("[{} pid={}] {}", "foobar", 1243L, "Foo bar baz");
    verify(logger).log("[{} pid={}] {}", "foobar", 1243L, "Do ray me");
    verify(logger).log("[{} pid={}] {}", "foobar", 1243L, "Eggs spam");
    verifyNoMoreInteractions(logger);
  }

  @DisplayName("logging errors are handled and reported once")
  @Test
  @Timeout(10)
  void loggingErrorsAreHandledAndReportedOnce() throws Exception {
    var ex = new IOException("yikes");
    InputStream inputStream = mock(withSettings().defaultAnswer(ctx -> {
      throw ex;
    }));
    OutputRedirectorDaemon.Logger logger = mock();
    new OutputRedirectorDaemon(
        "foobar",
        1243L,
        inputStream,
        logger
    );

    // When
    Thread.sleep(500);

    // Then
    verify(logger).log("[{} pid={}] Internal error intercepting logs!", "foobar", 1243L, ex);
    verifyNoMoreInteractions(logger);
  }

  @DisplayName(".await() waits for the logger to be exhausted")
  @Test
  @Timeout(10)
  void awaitWaitsForTheLoggerToBeExhausted() throws Exception {
    var inputStream = new ByteArrayInputStream(
        "Foo bar baz\nDo ray me\nEggs spam".getBytes(StandardCharsets.UTF_8)
    );
    OutputRedirectorDaemon.Logger logger = mock();
    var daemon = new OutputRedirectorDaemon(
        "foobar",
        1243L,
        inputStream,
        logger
    );

    // When
    daemon.await();

    // Then
    assertThat(inputStream.read()).isEqualTo(-1);
  }
}
