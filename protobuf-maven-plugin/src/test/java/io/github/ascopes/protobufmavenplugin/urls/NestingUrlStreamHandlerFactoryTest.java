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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("NestingUrlStreamHandlerFactory tests")
class NestingUrlStreamHandlerFactoryTest {

  @DisplayName(".createUrlConnection(URL) creates the expected URL connection")
  @Test
  void createUrlConnectionCreatesTheExpectedUrlConnection() throws Exception {
    // Given
    var urlFactory = mock(UrlFactory.class);
    var streamHandlerFactory = new NestingUrlStreamHandlerFactory(
        urlFactory,
        ReversingInputStream::new,
        "foo", "bar", "baz"
    );
    var nestedUrl = mock(URL.class);
    when(urlFactory.create(any())).thenReturn(nestedUrl);

    // When
    var streamHandler = streamHandlerFactory.createURLStreamHandler("bar");
    var inputUrl = new URL(null, "bar://do://ray", streamHandler);
    var connection = inputUrl.openConnection();

    // Then
    verify(urlFactory).create(URI.create(inputUrl.getFile()));
    verifyNoMoreInteractions(urlFactory);

    assertThat(connection)
        .isInstanceOf(AbstractNestingUrlConnection.class)
        .satisfies(conn -> assertThat(conn.getURL()).isSameAs(inputUrl))
        .satisfies(conn -> assertThat(((AbstractNestingUrlConnection) conn).getNestedUrl())
            .isSameAs(nestedUrl));
  }

  @DisplayName(".createUrlConnection(URL) creates the expected input stream")
  @Test
  void createUrlConnectionCreatesTheExpectedInputStream() throws Exception {
    // Given
    var urlFactory = mock(UrlFactory.class);
    var streamHandlerFactory = new NestingUrlStreamHandlerFactory(
        urlFactory,
        ReversingInputStream::new,
        "foo", "bar", "baz"
    );
    var nestedConnection = mock(URLConnection.class);
    var data = "Hello, World!".getBytes(StandardCharsets.UTF_8);
    var nestedInputStream = new ByteArrayInputStream(data);
    when(nestedConnection.getInputStream()).thenReturn(nestedInputStream);
    var nestedUrl = mock(URL.class);
    when(nestedUrl.openConnection()).thenReturn(nestedConnection);
    when(urlFactory.create(any())).thenReturn(nestedUrl);

    // When
    var streamHandler = streamHandlerFactory.createURLStreamHandler("bar");
    var inputUrl = new URL(null, "bar://do://ray", streamHandler);
    var connection = inputUrl.openConnection();
    connection.connect();
    var outputStream = new ByteArrayOutputStream();
    try (var inputStream = connection.getInputStream()) {
      inputStream.transferTo(outputStream);
    }

    // Then
    assertThat(outputStream.toString(StandardCharsets.UTF_8))
        .isEqualTo("!dlroW ,olleH");
  }

  static final class ReversingInputStream extends InputStream {
    private final InputStream inputStream;

    ReversingInputStream(InputStream inner) throws IOException {
      var baos = new ByteArrayOutputStream();
      inner.transferTo(baos);
      var data = baos.toByteArray();
      var reversedData = new byte[data.length];

      for (var i = 0; i < data.length; ++i) {
        reversedData[data.length - 1 - i] = data[i];
      }

      inputStream = new ByteArrayInputStream(reversedData);
    }

    @Override
    public int read() throws IOException {
      return inputStream.read();
    }
  }
}
