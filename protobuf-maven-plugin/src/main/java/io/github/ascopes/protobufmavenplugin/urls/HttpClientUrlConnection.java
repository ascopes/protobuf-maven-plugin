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

import io.github.ascopes.protobufmavenplugin.utils.HttpRequestException;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;

/**
 * URL connection for HTTP and HTTPS requests
 * that wraps HttpClient
 *
 *
 * @author Ilja Kanstanczuk
 * @since 3.10.1
 */
public class HttpClientUrlConnection extends URLConnection {

  private final HttpClient client;
  private final HttpRequest request;
  private HttpResponse<byte[]> response;

  public HttpClientUrlConnection(URL url) throws URISyntaxException {
    super(url);
    String protocol = url.getProtocol();
    this.client = HttpClient
        .newBuilder()
        .followRedirects(Redirect.ALWAYS)
        .version(protocol.equalsIgnoreCase("http") ? Version.HTTP_1_1 : Version.HTTP_2)
        .build();
    this.request = HttpRequest
        .newBuilder()
        .uri(url.toURI())
        .GET()
        .build();
  }

  @Override
  public void connect() throws IOException {
    if (connected) {
      return;
    }
    try {
      response = client.send(request, BodyHandlers.ofByteArray());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("HTTP request interrupted for " + url, e);
    } catch (IOException e) {
      throw new IOException("Failed to fetch " + url, e);
    }
    this.connected = true;
    if (response != null && response.statusCode() == 404) {
      throw new FileNotFoundException(url.toString());
    }
    if (response != null && response.statusCode() >= 400) {
      throw HttpRequestException.fromHttpResponse(response);
    }
  }

  @Override
  public InputStream getInputStream() throws IOException {
    if (!connected) {
      connect();
    }
    return new ByteArrayInputStream(response.body());
  }
}
