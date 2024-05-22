/*
 * Copyright (C) 2023 - 2024, Ashley Scopes.
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

package io.github.ascopes.protobufmavenplugin.spi;

import com.google.auto.service.AutoService;
import java.io.IOException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.net.spi.URLStreamHandlerProvider;
import org.jspecify.annotations.Nullable;

/**
 * A URL stream handler that is used to register the {@code zip}
 * protocol.
 *
 * <p>In reality, it just delegates to the JAR handler internally, which is already
 * compatible with ZIP files. We provide this to create a more intuitive interface
 * for users than using the JAR protocol for non-JAR files.
 *
 * @author Ashley Scopes
 * @since 2.1.1
 */
@AutoService(URLStreamHandlerProvider.class)
public final class ZipUrlStreamHandlerProvider extends URLStreamHandlerProvider {

  // No state is stored, so we can just reuse a singleton instance.
  private static final URLStreamHandler instance = new URLStreamHandler() {
    @Override
    public URLConnection openConnection(URL url) throws IOException {
      // The handler for JAR files can deal with processing ZIP files for us.
      return new URL("jar", null, url.getFile()).openConnection();
    }
  };

  @Override
  public @Nullable URLStreamHandler createURLStreamHandler(String protocol) {
    return protocol.equalsIgnoreCase("zip")
        ? instance
        : null;
  }
}
