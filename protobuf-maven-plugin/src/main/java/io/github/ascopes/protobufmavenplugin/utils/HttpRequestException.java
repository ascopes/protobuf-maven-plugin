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

import java.io.IOException;
import java.io.InputStream;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import org.jspecify.annotations.Nullable;

/**
 * Exception raised if HTTP request fails
 *
 * @author Ilja Kanstanczuk
 * @since 3.10.2
 */
public final class HttpRequestException extends IOException {

  private final int statusCode;
  private final @Nullable String correlationId;
  private final @Nullable String requestId;
  private final @Nullable String wwwAuthenticate;
  private final @Nullable String proxyAuthenticate;
  private final @Nullable String responseBody;

  public HttpRequestException(
      String message,
      int statusCode,
      @Nullable String correlationId,
      @Nullable String requestId,
      @Nullable String wwwAuthenticate,
      @Nullable String proxyAuthenticate,
      @Nullable String responseBody
  ) {
    super(message);
    this.statusCode = statusCode;
    this.correlationId = correlationId;
    this.requestId = requestId;
    this.wwwAuthenticate = wwwAuthenticate;
    this.proxyAuthenticate = proxyAuthenticate;
    this.responseBody = responseBody;
  }

  public static HttpRequestException fromHttpResponse(HttpResponse<InputStream> response) {
    var body = asText(response.body(), 500);
    var correlationId = extractHeader(response, "Correlation-Id", "X-Correlation-Id");
    var requestId = extractHeader(response, "Request-Id", "X-Request-Id");

    return new HttpRequestException(
        "HTTP " + response.statusCode() + " from " + response.uri(),
        response.statusCode(),
        correlationId,
        requestId,
        extractHeader(response, "WWW-Authenticate"),
        extractHeader(response, "Proxy-Authenticate"),
        body
    );
  }

  private static String asText(InputStream stream, int maxLength) {
    if (stream == null) {
      return null;
    }
    try (stream) {
      var body = stream.readNBytes(maxLength + 1);
      var text = new String(body, StandardCharsets.UTF_8);
      if (text.length() > maxLength) {
        return text.substring(0, maxLength) + "... [truncated]";
      }
      return text;
    } catch (Exception e) {
      return "<binary or unreadable response body>";
    }
  }

  private static @Nullable String extractHeader(
      HttpResponse<InputStream> response,
      String... names
  ) {
    for (var name : names) {
      var val = response.headers().firstValue(name);
      if (val.isPresent()) {
        return val.get();
      }
    }
    return null;
  }

  public int getStatusCode() {
    return statusCode;
  }

  public @Nullable String getCorrelationId() {
    return correlationId;
  }

  public @Nullable String getRequestId() {
    return requestId;
  }

  public @Nullable String getWwwAuthenticate() {
    return wwwAuthenticate;
  }

  public @Nullable String getProxyAuthenticate() {
    return proxyAuthenticate;
  }

  public @Nullable String getResponseBody() {
    return responseBody;
  }

  @Override
  public String toString() {
    return "HttpClientUrlConnectionException{"
        + "statusCode=" + statusCode
        + ", correlationId='" + correlationId + '\''
        + ", requestId='" + requestId + '\''
        + ", wwwAuthenticate='" + wwwAuthenticate + '\''
        + ", proxyAuthenticate='" + proxyAuthenticate + '\''
        + ", responseBody='" + responseBody + '\''
        + '}';
  }
}
