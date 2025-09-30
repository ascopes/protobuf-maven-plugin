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
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.github.ascopes.protobufmavenplugin.fixtures.RandomFixtures;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AbstractNestingUrlConnection tests")
class AbstractNestingUrlConnectionTest {

  @DisplayName(".connect() opens the inner connection and configures it sensibly")
  @Test
  void connectOpensInnerConnectionAndConfiguresItSensibly() throws Exception {
    // Given
    final var innerConnection = mock(URLConnection.class);
    final var innerUrl = mock(URL.class);
    final var url = URI.create("file://something").toURL();

    when(innerUrl.openConnection()).thenReturn(innerConnection);
    final var connection = new ReversingUrlConnection(url, innerUrl);

    assertThat(connection.isConnected())
        .withFailMessage(".connected was set prematurely")
        .isFalse();

    final var allowUserInteraction = RandomFixtures.someBoolean();
    final var connectTimeout = RandomFixtures.somePositiveInt();
    final var doInput = RandomFixtures.someBoolean();
    final var doOutput = RandomFixtures.someBoolean();
    final var ifModifiedSince = RandomFixtures.somePositiveInt();
    final var readTimeout = RandomFixtures.somePositiveInt();
    final var useCaches = RandomFixtures.someBoolean();

    final var requestProperties = Stream
        .generate(() -> Map.entry(
            RandomFixtures.someBasicString(),
            List.of(
                RandomFixtures.someBasicString(),
                RandomFixtures.someBasicString(),
                RandomFixtures.someBasicString()
            )
        ))
        .limit(10)
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            Map.Entry::getValue
        ));

    // When
    connection.setAllowUserInteraction(allowUserInteraction);
    connection.setConnectTimeout(connectTimeout);
    connection.setDoInput(doInput);
    connection.setDoOutput(doOutput);
    connection.setIfModifiedSince(ifModifiedSince);
    connection.setReadTimeout(readTimeout);
    connection.setUseCaches(useCaches);
    requestProperties.forEach((key, values) ->
        values.forEach(value ->
            connection.addRequestProperty(key, value)));
    connection.connect();

    // Then
    final var inOrder = inOrder(innerUrl, innerConnection);
    inOrder.verify(innerUrl).openConnection();

    inOrder.verify(innerConnection).setAllowUserInteraction(allowUserInteraction);
    inOrder.verify(innerConnection).setConnectTimeout(connectTimeout);
    inOrder.verify(innerConnection).setDoInput(doInput);
    inOrder.verify(innerConnection).setIfModifiedSince(ifModifiedSince);
    inOrder.verify(innerConnection).setReadTimeout(readTimeout);
    inOrder.verify(innerConnection).setUseCaches(useCaches);
    requestProperties.forEach((key, values) ->
        values.forEach(value ->
            inOrder.verify(innerConnection).addRequestProperty(key, value)));
    // Never allowed here.
    inOrder.verify(innerConnection).setDoOutput(false);
    inOrder.verify(innerConnection).connect();

    inOrder.verifyNoMoreInteractions();

    assertThat(connection.isConnected())
        .withFailMessage(".connected was not set on the outer connection")
        .isTrue();
  }

  @DisplayName(".connect() does nothing if already connected")
  @Test
  void connectDoesNothingIfAlreadyConnected() throws Exception {
    // Given
    var innerConnection = mock(URLConnection.class);
    var innerUrl = mock(URL.class);
    var url = URI.create("file://something").toURL();

    when(innerUrl.openConnection()).thenReturn(innerConnection);
    var connection = new ReversingUrlConnection(url, innerUrl);
    connection.markConnected();

    // When
    connection.connect();

    // Then
    verifyNoInteractions(innerUrl, innerConnection);
  }

  static class ReversingUrlConnection extends AbstractNestingUrlConnection {
    ReversingUrlConnection(URL url, URL nestedUrl) {
      super(url, nestedUrl);
    }

    boolean isConnected() {
      return connected;
    }

    void markConnected() {
      connected = true;
    }

    @Override
    InputStream nestInputStream(InputStream is) throws IOException {
      var baos = new ByteArrayOutputStream();
      var data = baos.toByteArray();
      var reversedData = new byte[data.length];

      for (var i = 0; i < data.length; ++i) {
        reversedData[data.length - 1 - i] = data[i];
      }

      return new ByteArrayInputStream(reversedData);
    }
  }
}
