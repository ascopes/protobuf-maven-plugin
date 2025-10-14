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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpHeaders;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class HttpRequestExceptionTest {

  HttpResponse<InputStream> mockedHttpResponse;

  @BeforeEach
  void setUp() {
    mockedHttpResponse = mock();
  }

  @DisplayName("method fromHttpResponse extracts all expected values from HttpResponse")
  @Test
  void methodFromHttpResponseExtractsAllExpectedValuesFromHttpResponse() throws Exception {
    // Given
    var headers = HttpHeaders.of(Map.of(
        "Correlation-Id", List.of("123"),
        "Request-Id", List.of("456"),
        "WWW-Authenticate", List.of("auth"),
        "Proxy-Authenticate", List.of("proxy")
    ), (a, b) -> true);
    var inputStream = new ByteArrayInputStream("Error text".getBytes(StandardCharsets.UTF_8));

    // When
    when(mockedHttpResponse.statusCode())
        .thenReturn(500);
    when(mockedHttpResponse.uri())
        .thenReturn(new URI("http://whatever"));
    when(mockedHttpResponse.headers())
        .thenReturn(headers);
    when(mockedHttpResponse.body())
        .thenReturn(inputStream);
    var ex = HttpRequestException.fromHttpResponse(mockedHttpResponse);

    // Then
    assertThat(ex.getStatusCode()).isEqualTo(500);
    assertThat(ex.getCorrelationId()).isEqualTo("123");
    assertThat(ex.getRequestId()).isEqualTo("456");
    assertThat(ex.getWwwAuthenticate()).isEqualTo("auth");
    assertThat(ex.getProxyAuthenticate()).isEqualTo("proxy");
    assertThat(ex.getResponseBody()).isEqualTo("Error text");
    assertThat(ex).hasMessageContaining("HTTP 500 from http://whatever");
  }

  @DisplayName("method fromHttpResponse handles missing headers and null response body")
  @Test
  void methodFromHttpResponseHandlesMissingHeadersAndNullResponseBody() throws Exception {
    // Given
    var headers = HttpHeaders.of(Map.of(),
        (a, b) -> true);

    // When
    when(mockedHttpResponse.statusCode())
        .thenReturn(404);
    when(mockedHttpResponse.uri())
        .thenReturn(new URI("http://whatever"));
    when(mockedHttpResponse.headers())
        .thenReturn(headers);
    when(mockedHttpResponse.body())
        .thenReturn(null);
    var ex = HttpRequestException.fromHttpResponse(mockedHttpResponse);

    // Then
    assertThat(ex.getStatusCode()).isEqualTo(404);
    assertThat(ex.getCorrelationId()).isNull();
    assertThat(ex.getRequestId()).isNull();
    assertThat(ex.getWwwAuthenticate()).isNull();
    assertThat(ex.getProxyAuthenticate()).isNull();
    assertThat(ex.getResponseBody()).isNull();
    assertThat(ex).hasMessageContaining("HTTP 404 from http://whatever");
  }

  @DisplayName("method fromHttpResponse returns readable fallback for unreadable body")
  @Test
  void methodFromHttpResponseReturnsReadableFallbackForUnreadableBody() throws Exception {
    // Given
    var badStream = mock(InputStream.class);
    var headers = HttpHeaders.of(Map.of(),
        (a, b) -> true);

    // When
    when(badStream.readNBytes(anyInt()))
        .thenThrow(new IOException("unreadable"));
    when(mockedHttpResponse.statusCode())
        .thenReturn(500);
    when(mockedHttpResponse.uri())
        .thenReturn(new URI("http://whatever"));
    when(mockedHttpResponse.headers())
        .thenReturn(headers);
    when(mockedHttpResponse.body())
        .thenReturn(badStream);
    var ex = HttpRequestException.fromHttpResponse(mockedHttpResponse);

    // Then
    assertThat(ex.getResponseBody())
        .isEqualTo("<binary or unreadable response body>");
  }

  @DisplayName("method toString includes all required information")
  @Test
  void methodToStringIncludesAllRequiredInformation() {
    var ex = new HttpRequestException(
        "HTTP 400 from http://whatever",
        400,
        "123",
        "456",
        "auth",
        "proxy",
        "error-body"
    );
    assertThat(ex.toString())
        .contains("400", "123", "456", "auth", "proxy", "error-body");
  }

}
