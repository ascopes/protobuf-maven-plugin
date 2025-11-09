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

import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.URLStreamHandlerFactory;
import java.util.List;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

/**
 * Abstract base for making a stateless URL stream handler factory.
 *
 * @author Ashley Scopes
 * @since 3.10.1
 */
abstract class AbstractUrlStreamHandlerFactory implements URLStreamHandlerFactory {
  private final List<String> protocols;
  private final UrlStreamHandlerImpl streamHandler;

  AbstractUrlStreamHandlerFactory(String protocol, String... protocols) {
    // Two args to enforce at least one protocol at any time.
    this.protocols = Stream
        .concat(Stream.of(protocol), Stream.of(protocols))
        .toList();
    streamHandler = new UrlStreamHandlerImpl();
  }

  @Override
  public final @Nullable URLStreamHandler createURLStreamHandler(String protocol) {
    return protocols.contains(protocol)
        ? streamHandler
        : null;
  }

  abstract URLConnection createUrlConnection(URL url) throws IOException;

  private final class UrlStreamHandlerImpl extends URLStreamHandler {
    @Override
    protected URLConnection openConnection(URL url) throws IOException {
      return createUrlConnection(url);
    }
  }
}
