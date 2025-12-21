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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.ascopes.protobufmavenplugin.urls.AbstractNestingUrlConnection.NestedUrlException;
import java.net.URISyntaxException;
import java.net.URL;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("HttpUrlStreamHandlerFactory tests")
class HttpUrlStreamHandlerFactoryTest {

  @DisplayName("method createUrlConnection successfully returns HttpClientUrlConnection")
  @ValueSource(strings = {"http", "https"})
  @ParameterizedTest(name = "for path \"{0}\"")
  void methodCreateUrlConnectionSuccessfullyReturnsHttpClientUrlConnection(String protocol)
      throws Exception {
    // Given
    var factory = new HttpUrlStreamHandlerFactory();
    var handler = factory.createURLStreamHandler(protocol);
    var url = new URL(null, protocol + "://whatever", handler);
    var factoryClient = factory.getClass().getDeclaredField("client");
    factoryClient.setAccessible(true);

    // When
    var connection = url.openConnection();
    var connectionClient = connection.getClass().getDeclaredField("client");
    connectionClient.setAccessible(true);

    // Then
    assertThat(connection).isExactlyInstanceOf(HttpClientUrlConnection.class);
    assertThat(connection.getURL()).isSameAs(url);
    assertThat(factoryClient.get(factory)).isSameAs(connectionClient.get(connection));
  }

  @DisplayName("method createUrlConnection fails with NestedUrlException because of invalid URL")
  @Test
  void methodCreateUrlConnectionFailsWithNestedUrlException() throws Exception {
    // Given
    var factory = new HttpUrlStreamHandlerFactory();
    var badUrl = new URL("http://%gg");

    // When / Then
    assertThatThrownBy(() -> factory.createUrlConnection(badUrl))
        .isInstanceOf(NestedUrlException.class)
        .hasMessageContaining("Failed to create connection for URL " + badUrl)
        .hasCauseInstanceOf(URISyntaxException.class);
  }
}
