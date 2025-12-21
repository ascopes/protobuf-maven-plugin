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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;

/**
 * URL stream handler that delegates to an inner URL handler for a nested
 * URL, wrapping any input stream to add additional logic, such as decompression.
 *
 * @author Ashley Scopes
 * @since 3.10.1
 */
final class NestingUrlStreamHandlerFactory extends AbstractUrlStreamHandlerFactory {
  private final UrlFactory urlFactory;
  private final InputStreamDecorator<?> decorator;

  NestingUrlStreamHandlerFactory(
      UrlFactory urlFactory,
      InputStreamDecorator<?> decorator,
      String protocol,
      String... protocols
  ) {
    super(protocol, protocols);
    this.urlFactory = urlFactory;
    this.decorator = decorator;
  }

  @Override
  URLConnection createUrlConnection(URL url) throws IOException {
    var innerUrl = urlFactory.create(URI.create(url.getFile()));
    return new AbstractNestingUrlConnection(url, innerUrl) {
      @Override
      InputStream nestInputStream(InputStream inputStream) throws IOException {
        return decorator.decorate(inputStream);
      }
    };
  }
}
