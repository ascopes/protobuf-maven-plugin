/*
 * Copyright (C) 2023 Ashley Scopes
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

import io.github.ascopes.protobufmavenplugin.urls.AbstractNestingUrlConnection.NestedUrlException;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.http.HttpClient;
import java.net.http.HttpClient.Redirect;
import java.net.http.HttpClient.Version;

/**
 * URL stream handler factory for URLs that wrap HTTP and HTTPS connections
 *
 * @author Ilja Kanstanczuk
 * @since 3.10.2
 */
final class HttpUrlStreamHandlerFactory extends AbstractUrlStreamHandlerFactory {

  private final HttpClient client;

  HttpUrlStreamHandlerFactory() {
    super("http", "https");
    client = HttpClient
        .newBuilder()
        .followRedirects(Redirect.ALWAYS)
        .version(Version.HTTP_2)
        .build();
  }

  @Override
  URLConnection createUrlConnection(URL url) throws IOException {
    try {
      return new HttpClientUrlConnection(url, client);
    } catch (URISyntaxException uriException) {
      throw new NestedUrlException("Failed to create connection for URL " + url, uriException);
    }
  }
}
