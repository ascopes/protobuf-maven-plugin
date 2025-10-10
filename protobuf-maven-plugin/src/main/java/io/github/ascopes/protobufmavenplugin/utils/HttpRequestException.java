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

import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

/**
 * Exception raised if HTTP request fails
 *
 * @author Ilja Kanstanczuk
 * @since 3.10.1
 */
public final class HttpRequestException extends RuntimeException {

  private final int statusCode;
  private final String correlationId;
  private final String requestId;
  private final String wwwAuthenticate;
  private final String proxyAuthenticate;
  private final String responseBody;

  public HttpRequestException(
      String message,
      int statusCode,
      String correlationId,
      String requestId,
      String wwwAuthenticate,
      String proxyAuthenticate,
      String responseBody) {
    super(message);
    this.statusCode = statusCode;
    this.correlationId = correlationId;
    this.requestId = requestId;
    this.wwwAuthenticate = wwwAuthenticate;
    this.proxyAuthenticate = proxyAuthenticate;
    this.responseBody = responseBody;
  }

  public static HttpRequestException fromHttpResponse(HttpResponse<byte[]> response) {
    String body = asText(response.body(), 500);
    String correlationId = extractHeader(response, "Correlation-Id", "X-Correlation-Id");
    String requestId = extractHeader(response, "Request-Id", "X-Request-Id");

    return new HttpRequestException(
        "HTTP " + response.statusCode() + " from " + response.uri(),
        response.statusCode(),
        correlationId,
        requestId,
        response.headers().firstValue("WWW-Authenticate").orElse(null),
        response.headers().firstValue("Proxy-Authenticate").orElse(null),
        body
    );
  }

  private static String asText(byte[] body, int maxLength) {
    if (body == null || body.length == 0) {
      return null;
    }

    try {
      String text = new String(body, StandardCharsets.UTF_8);
      if (text.length() > maxLength) {
        return text.substring(0, maxLength) + "... [truncated]";
      }
      return text;
    } catch (Exception e) {
      return "<binary or unreadable body: " + body.length + " bytes>";
    }
  }

  private static String extractHeader(
      HttpResponse<byte[]> response,
      String... names) {
    for (String name : names) {
      var val = response.headers().firstValue(name);
      if (val.isPresent()) {
        return val.get();
      }
    }
    return null;
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
