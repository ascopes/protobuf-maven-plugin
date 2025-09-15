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
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.List;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Base logic for making a recursively resolving URL stream handler which can produce
 * {@link java.net.URLConnection}s that serve content being filtered through some decorated
 * {@link InputStream}.
 *
 * @author Ashley Scopes
 * @since 3.10.0
 */
abstract class AbstractDecoratingUrlStreamHandlerFactory implements URLStreamHandlerFactory {

  private final Set<String> protocols;
  private final boolean archive;
  private final UrlFactory urlFactory;
  private final DecoratingUrlStreamHandlerImpl streamHandler;

  AbstractDecoratingUrlStreamHandlerFactory(
      boolean archive,
      UrlFactory urlFactory,
      String... protocols
  ) {
    if (protocols.length == 0) {
      throw new IllegalStateException("At least one protocol is required");
    }

    this.protocols = Set.copyOf(List.of(protocols));
    this.archive = archive;
    this.urlFactory = urlFactory;
    streamHandler = new DecoratingUrlStreamHandlerImpl();
  }

  @Override
  public final @Nullable URLStreamHandler createURLStreamHandler(String protocol) {
    if (protocols.contains(protocol)) {
      return streamHandler;
    }
    return null;
  }

  protected abstract InputStream decorateInputStream(
      InputStream inputStream,
      @Nullable String file
  ) throws IOException;

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

        // +2 since prefix is 2 chars long; we don't want to include the first forwardslash.
        file = rawInnerUri.substring(pathIndex + 2);
        rawInnerUri = rawInnerUri.substring(0, pathIndex);
      }

      var innerUri = URI.create(rawInnerUri);
      var innerUrl = urlFactory.create(innerUri);

      return new DecoratingUrlConnection(url, innerUrl, file);
    }
  }

  private final class DecoratingUrlConnection extends URLConnection {

    private final @Nullable String file;
    private final URL innerUrl;

    private @Nullable URLConnection innerUrlConnection;

    DecoratingUrlConnection(URL url, URL innerUrl, @Nullable String file) {
      super(url);
      this.innerUrl = innerUrl;
      this.file = file;
      innerUrlConnection = null;
    }

    @Override
    public void connect() throws IOException {
      if (innerUrlConnection == null) {
        innerUrlConnection = innerUrl.openConnection();
        innerUrlConnection.connect();
        connected = true;
      }
    }

    @Override
    public InputStream getInputStream() throws IOException {
      var innerUrlConnection = requireNonNull(this.innerUrlConnection);

      try {
        return decorateInputStream(innerUrlConnection.getInputStream(), file);
      } catch (IOException ex) {
        // Clean up, we're in a bad state and cannot continue.
        innerUrlConnection.getInputStream().close();
        throw ex;
      }
    }
  }

}
