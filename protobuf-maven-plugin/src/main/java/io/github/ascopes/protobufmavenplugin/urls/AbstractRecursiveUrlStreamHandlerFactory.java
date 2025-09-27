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
import java.io.OutputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Base logic for making a recursively resolving URL stream handler which can produce
 * {@link java.net.URLConnection}s that serve content being filtered through some decorated
 * {@link InputStream}.
 *
 * @author Ashley Scopes
 * @since 3.10.0
 */
abstract class AbstractRecursiveUrlStreamHandlerFactory implements URLStreamHandlerFactory {

  private final List<String> protocols;
  private final boolean archive;
  private final UrlFactory urlFactory;
  private final DecoratingUrlStreamHandlerImpl streamHandler;

  AbstractRecursiveUrlStreamHandlerFactory(
      boolean archive,
      UrlFactory urlFactory,
      String... protocols
  ) {
    if (protocols.length == 0) {
      throw new IllegalStateException("At least one protocol is required");
    }

    this.protocols = List.of(protocols);
    this.archive = archive;
    this.urlFactory = urlFactory;
    streamHandler = new DecoratingUrlStreamHandlerImpl();
  }

  @Override
  public final @Nullable URLStreamHandler createURLStreamHandler(String protocol) {
    return protocols.contains(protocol)
        ? streamHandler
        : null;
  }

  protected abstract InputStream decorate(
      InputStream inputStream,
      @Nullable String file
  ) throws IOException;

  /**
   * Stream handler. Parses URLs and yields a URL connection.
   */
  private final class DecoratingUrlStreamHandlerImpl extends URLStreamHandler {

    @Override
    protected URLConnection openConnection(URL url) throws IOException {

      var rawInnerUri = url.getFile();
      String file = null;

      if (archive) {
        var pathIndex = rawInnerUri.lastIndexOf("!/");
        if (pathIndex == -1) {
          throw new IOException(
              "URI '" + url + "' was missing a nested path fragment (e.g. '"
                  + url.getProtocol() + ":http://some-website.com/some-file!/path/within/archive"
                  + "')"
          );
        }

        // +2 since prefix "!/" is 2 chars long; we don't want to include the first forwardslash.
        file = rawInnerUri.substring(pathIndex + 2);
        rawInnerUri = rawInnerUri.substring(0, pathIndex);
      }

      var innerUrl = urlFactory.create(URI.create(rawInnerUri));
      return new DecoratingUrlConnection(url, innerUrl, file);
    }
  }

  /**
   * Decorative URL connection that wraps another URL connection, applying
   * additional transformitive logic.
   */
  private final class DecoratingUrlConnection extends URLConnection {

    private final URL innerUrl;
    private final @Nullable String file;

    private @Nullable URLConnection connection;
    private @Nullable InputStream decoratedInputStream;

    DecoratingUrlConnection(URL url, URL innerUrl, @Nullable String file) {
      super(url);
      this.innerUrl = innerUrl;
      this.file = file;
      connection = null;
      decoratedInputStream = null;
    }

    @Override
    public void connect() throws IOException {
      if (connected) {
        return;
      }

      connection = innerUrl.openConnection();

      // Copy any settings across prior to connecting.
      connection.setAllowUserInteraction(getAllowUserInteraction());
      connection.setConnectTimeout(getConnectTimeout());
      connection.setDoInput(getDoInput());
      connection.setDoOutput(getDoOutput());
      connection.setIfModifiedSince(getIfModifiedSince());
      connection.setReadTimeout(getReadTimeout());
      connection.setUseCaches(getUseCaches());
      getRequestProperties()
          .forEach((key, values) -> values
              .forEach(value -> connection.addRequestProperty(key, value)));

      // Handshake.
      connection.connect();
      connected = true;
    }

    @Override
    public InputStream getInputStream() throws IOException {
      if (decoratedInputStream == null) {
        var connection = requireNonNull(this.connection, "not connected");

        try {
          decoratedInputStream = decorate(connection.getInputStream(), file);
        } catch (IOException ex) {
          // Clean up, we're in a bad state and cannot continue, and we do not
          // want to abandon any resources.
          connection.getInputStream().close();
          throw new IOException(
              "Failed to wrap input stream with protocol "
                  + protocols.iterator().next()
                  + " for URL \"" + url + "\": "
                  + ex,
              ex
          );
        }
      }
      return decoratedInputStream;
    }

    // TODO(ascopes): do we actually ever want to expose this?
    @Override
    public OutputStream getOutputStream() throws IOException {
      return requireNonNull(connection, "not connected").getOutputStream();
    }
  }
}
