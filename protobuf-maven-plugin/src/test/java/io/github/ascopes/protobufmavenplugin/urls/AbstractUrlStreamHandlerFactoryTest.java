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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.net.URL;
import java.net.URLConnection;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("AbstractUrlStreamHandlerFactory tests")
class AbstractUrlStreamHandlerFactoryTest {

  @DisplayName(".createURLStreamHandler(String) returns null if passed an unsupported protocol")
  @Test
  void createUrlStreamHandlerReturnsNullIfPassedUnsupportedProtocol() {
    // Given
    var factory = new SomeUrlStreamHandlerFactory(mock(), "foo", "bar", "baz");

    // Then
    assertThat(factory.createURLStreamHandler("bork")).isNull();
  }

  @DisplayName(".createURLStreamHandler(String) creates a delegating URL stream handler")
  @ValueSource(strings = {"foo", "bar", "baz"})
  @ParameterizedTest(name = "for supported protocol {0}")
  void createUrlStreamHandlerCreatesDelegatingUrlStreamHandler(String protocol) {
    // Given
    var factory = new SomeUrlStreamHandlerFactory(mock(), "foo", "bar", "baz");

    // When
    var actualStreamHandler = factory.createURLStreamHandler(protocol);

    // Then
    assertThat(actualStreamHandler).isNotNull();
  }

  @DisplayName("the result of .createURLStreamHandler(String) can open connections expectedly")
  @Test
  void resultOfCreateUrlStreamHandlerCanOpenConnectionsExpectedly() throws Exception {
    // Given
    var expectedUrlConnection = mock(URLConnection.class);
    var factory = new SomeUrlStreamHandlerFactory(expectedUrlConnection, "foo", "bar", "baz");
    var actualStreamHandler = factory.createURLStreamHandler("foo");
    var url = new URL(null, "foo://whatever", actualStreamHandler);

    // When
    var actualConnection = url.openConnection();

    // Then
    assertThat(actualConnection).isSameAs(expectedUrlConnection);
    assertThat(factory.createUrlConnectionUrl).isSameAs(url);
  }

  static final class SomeUrlStreamHandlerFactory extends AbstractUrlStreamHandlerFactory {
    private final URLConnection mockUrlConnection;
    private @Nullable URL createUrlConnectionUrl;

    SomeUrlStreamHandlerFactory(
        URLConnection mockUrlConnection,
        String protocol,
        String... protocols
    ) {
      super(protocol, protocols);
      this.mockUrlConnection = mockUrlConnection;
    }

    @Override
    URLConnection createUrlConnection(URL url) {
      createUrlConnectionUrl = url;
      return mockUrlConnection;
    }
  }
}
