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
package io.github.ascopes.protobufmavenplugin.fs;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.spi.URLStreamHandlerProvider;
import org.jspecify.annotations.Nullable;

/**
 * URL stream handler SPI implementation that enables the use of the
 * "zip" protocol within URLs. This allows users to dereference files within
 * ZIP archives.
 *
 * <p>Underneath, this simply delegates to the JDK's JAR provider, as
 * both formats work in the same way internally.
 *
 * @author Ashley Scopes
 * @since 3.2.p
 */
public final class ZipUrlStreamHandlerProvider extends URLStreamHandlerProvider {

  @Override
  public @Nullable URLStreamHandler createURLStreamHandler(String protocol) {
    if (!"zip".equals(protocol)) {
      return null;
    }

    return new URLStreamHandler() {
      @Override
      public URLConnection openConnection(URL url) throws IOException {
        // Replace zip with jar, as the JDK JAR handler can deal with this
        // for us rather than needing a brand new implementation.
        return URI.create("jar" + url.toExternalForm().substring(3))
            .toURL()
            .openConnection();
      }
    };
  }
}
