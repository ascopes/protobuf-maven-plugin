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
package io.github.ascopes.protobufmavenplugin.urls;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class HttpClientUrlConnectionTest {

  HttpClient mockHttpClient;

  @BeforeEach
  void setUp() {
    mockHttpClient = mock(HttpClient.class);
  }

  @DisplayName("constructor throws URISyntaxException for invalid URL")
  @Test
  void constructorThrowsUriSyntaxExceptionForInvalidUrl() throws Exception {
    // Given
    var badUrl = new URL("http://%gg");

    // When / Then
    assertThatThrownBy(() -> new HttpClientUrlConnection(badUrl, mockHttpClient))
        .isInstanceOf(URISyntaxException.class);
  }

  @DisplayName("method connect throws InterruptedIOException if thread is interrupted")
  @Test
  void methodConnectThrowsInterruptedIoExceptionIfThreadInterrupted() throws Exception {
    // Given
    var url = new URL("http://whatever");

    // When
    when(mockHttpClient.send(any(), any()))
        .thenThrow(new InterruptedException("interrupt"));
    var connection = new HttpClientUrlConnection(url, mockHttpClient);

    // Then
    assertThatThrownBy(connection::connect)
        .isInstanceOf(InterruptedIOException.class)
        .hasCauseInstanceOf(InterruptedException.class);
  }

  @DisplayName("method connect wraps IO failure in IOException")
  @Test
  void methodConnectWrapsIoFailureInIoException() throws Exception {
    // Given
    var url = new URL("http://whatever");

    // When
    when(mockHttpClient.send(any(), any()))
        .thenThrow(new IOException("network down"));
    var connection = new HttpClientUrlConnection(url, mockHttpClient);

    // Then
    assertThatThrownBy(connection::connect)
        .isInstanceOf(IOException.class)
        .hasMessageContaining("Failed to fetch");
  }

  @DisplayName("method getInputStream returns stream with response body after successful connect")
  @SuppressWarnings("unchecked")
  @Test
  void methodGetInputStreamReturnsStreamWithResponseBodyAfterSuccessfulConnect() throws Exception {
    // Given
    var url = new URL("http://whatever");
    var mockResponse = mock(HttpResponse.class);
    var body = new ByteArrayInputStream("OK".getBytes(StandardCharsets.UTF_8));

    // When
    when(mockResponse.statusCode())
        .thenReturn(200);
    when(mockResponse.body())
        .thenReturn(body);
    when(mockHttpClient.send(any(), any()))
        .thenReturn(mockResponse);
    var connection = new HttpClientUrlConnection(url, mockHttpClient);
    connection.connect();

    // Then
    assertThat(new String(connection.getInputStream().readAllBytes()))
        .isEqualTo("OK");
  }

}
