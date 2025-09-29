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

import static java.util.Objects.requireNonNull;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import org.jspecify.annotations.Nullable;

/**
 * Abstract base for a URL connection that wraps another URL connection
 * created from a nested URL.
 *
 * @author Ashley Scopes
 * @since 3.10.1
 */
abstract class AbstractNestingUrlConnection extends URLConnection {
  private final URL nestedUrl;

  private @Nullable URLConnection nestedConnection;
  private @Nullable InputStream nestedInputStream;

  AbstractNestingUrlConnection(URL url, URL nestedUrl) {
    super(url);
    this.nestedUrl = nestedUrl;

    // No output for you.
    setDoOutput(false);
  }

  @Override
  public void connect() throws IOException {
    if (connected) {
      return;
    }

    nestedConnection = nestedUrl.openConnection();

    // Copy any settings across prior to connecting.
    nestedConnection.setAllowUserInteraction(getAllowUserInteraction());
    nestedConnection.setConnectTimeout(getConnectTimeout());
    nestedConnection.setDoInput(getDoInput());
    nestedConnection.setIfModifiedSince(getIfModifiedSince());
    nestedConnection.setReadTimeout(getReadTimeout());
    nestedConnection.setUseCaches(getUseCaches());
    getRequestProperties()
        .forEach((key, values) -> values
            .forEach(value -> nestedConnection.addRequestProperty(key, value)));

    // Never bother with outputs, we never use them.
    nestedConnection.setDoOutput(false);

    // Handshake.
    nestedConnection.connect();
    connected = true;
  }

  @Override
  public InputStream getInputStream() throws IOException {
    if (nestedInputStream == null) {
      requireNonNull(nestedConnection, "not connected");

      try {
        nestedInputStream = nestInputStream(nestedConnection.getInputStream());
      } catch (IOException ex) {
        // Clean up, we're in a bad state and cannot continue, and we do not
        // want to abandon any resources.
        nestedConnection.getInputStream().close();
        throw new IOException(
            "Failed to wrap input stream with protocol "
                + url.getProtocol()
                + " for URL \"" + url + "\": "
                + ex,
            ex
        );
      }
    }

    return nestedInputStream;
  }

  abstract InputStream nestInputStream(InputStream inputStream) throws IOException;
}
