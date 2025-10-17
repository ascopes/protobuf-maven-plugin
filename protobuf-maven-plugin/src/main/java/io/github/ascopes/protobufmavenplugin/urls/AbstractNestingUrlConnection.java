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
    this.nestedUrl = requireNonNull(nestedUrl);

    // No output for you.
    setDoOutput(false);
  }

  public URL getNestedUrl() {
    return nestedUrl;
  }

  @Override
  public final void connect() throws NestedUrlException {
    if (connected) {
      return;
    }

    try {
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
      doConnect(nestedConnection);
      connected = true;
    } catch (IOException ex) {
      throw maybeWrapIoException(ex);
    }
  }

  @Override
  public final InputStream getInputStream() throws NestedUrlException {
    return nestedInputStream == null
        ? constructInputStream()
        : nestedInputStream;
  }

  private InputStream constructInputStream() throws NestedUrlException {
    requireNonNull(
        nestedConnection,
        () -> "internals are not connected to '" + nestedUrl + "', this is a bug!"
    );

    try {
      // Side effects - eww.
      return nestedInputStream = nestInputStream(nestedConnection.getInputStream());
    } catch (IOException ex) {
      var wrappedEx = maybeWrapIoException(ex);

      // Clean up, we're in a bad state and cannot continue, and we do not
      // want to abandon any resources.
      // Treat this as best effort only. If it fails due to our broken state,
      // then just ignore the resulting exception and continue anyway.
      try {
        nestedConnection.getInputStream().close();
      } catch (Throwable closureEx) {
        wrappedEx.addSuppressed(closureEx);
      }

      throw wrappedEx;
    }
  }

  private NestedUrlException maybeWrapIoException(Throwable ex) {
    if (ex instanceof NestedUrlException nestedEx) {
      // Continue to bubble upwards without adding more
      // irrelevant information to the mix.
      return nestedEx;
    }

    return new NestedUrlException(
        "Failed to transfer '"
            + nestedUrl
            + "' wrapped with handler for protocol '"
            + url.getProtocol()
            + "', an inner exception was raised: "
            + ex,
        ex
    );
  }

  // Can be overridden to add more logic in custom implementations if needed.
  void doConnect(URLConnection nestedConnection) throws IOException {
    nestedConnection.connect();
  }

  // Potentially wraps the input stream with another input stream, returning
  // an input stream to replace the input with.
  abstract InputStream nestInputStream(InputStream inputStream) throws IOException;

  /**
   * Internally raised exception that wraps another exception.
   *
   * <p>If we catch this, we just rethrow it without cascading
   * any further, as it means we have already reported the context
   * of the actual exception.
   *
   * <p>This avoids obnoxiously verbose exception chains if a highly
   * nested protocol fails to be handled correctly.
   */
  static final class NestedUrlException extends IOException {
    NestedUrlException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
