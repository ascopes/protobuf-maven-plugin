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
import static org.assertj.core.api.Assertions.assertThatException;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import io.github.ascopes.protobufmavenplugin.fixtures.RandomFixtures;
import io.github.ascopes.protobufmavenplugin.urls.AbstractNestingUrlConnection.NestedUrlException;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;

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
        values.forEach(value -> verify(innerConnection).addRequestProperty(key, value)));
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

  @DisplayName("The exception is reraised if .connect() catches a NestedUrlException")
  @Test
  void theExceptionIsReraisedIfConnectCatchesNestedUrlException() throws Exception {
    // Given
    var ex = new AbstractNestingUrlConnection.NestedUrlException(
        "bang!",
        new Exception("some cause")
    );
    var innerConnection = mock(URLConnection.class);
    var innerUrl = mock(URL.class);
    var url = URI.create("file://something").toURL();

    when(innerUrl.openConnection()).thenReturn(innerConnection);
    doThrow(ex).when(innerConnection).connect();
    var connection = new ReversingUrlConnection(url, innerUrl);

    // Then
    assertThatException()
        .isThrownBy(connection::connect)
        .isSameAs(ex);
  }

  @DisplayName("The exception is wrapped and reraised if .connect() catches an IOException")
  @Test
  void theExceptionIsWrappedAndReraisedIfConnectCatchesIoException() throws Exception {
    // Given
    var ex = new IOException("bang");
    var innerConnection = mock(URLConnection.class);
    var innerUrl = mock(URL.class);
    var url = URI.create("file://something").toURL();

    when(innerUrl.openConnection()).thenReturn(innerConnection);
    doThrow(ex).when(innerConnection).connect();
    var connection = new ReversingUrlConnection(url, innerUrl);

    // Then
    assertThatExceptionOfType(AbstractNestingUrlConnection.NestedUrlException.class)
        .isThrownBy(connection::connect)
        .withMessage(
            "Failed to transfer '%s' wrapped with handler for protocol '%s', "
                + "an inner exception was raised: %s",
            innerUrl,
            url.getProtocol(),
            ex
        )
        .withCause(ex);
  }

  @DisplayName(".getInputStream() returns the nested input stream")
  @Test
  void getInputStreamReturnsTheNestedInputStream() throws Exception {
    // Given
    var innerConnection = mock(URLConnection.class);
    var innerUrl = mock(URL.class);
    var url = URI.create("file://something").toURL();

    when(innerUrl.openConnection()).thenReturn(innerConnection);
    when(innerConnection.getInputStream())
        .thenReturn(new ByteArrayInputStream("Hello, World!".getBytes(StandardCharsets.UTF_8)));

    var connection = new ReversingUrlConnection(url, innerUrl);
    connection.connect();

    // When
    var baos = new ByteArrayOutputStream();
    try (var is = connection.getInputStream()) {
      is.transferTo(baos);
    }

    // Then
    assertThat(baos.toString(StandardCharsets.UTF_8))
        .isEqualTo("!dlroW ,olleH");
  }

  @DisplayName(".getInputStream() returns the same stream repeatedly")
  @Test
  void getInputStreamReturnsTheSameStreamRepeatedly() throws Exception {
    // Given
    var innerConnection = mock(URLConnection.class);
    var innerUrl = mock(URL.class);
    var url = URI.create("file://something").toURL();

    var data = "Hello, World!".getBytes(StandardCharsets.UTF_8);
    var innerInputStream = new ByteArrayInputStream(data);

    when(innerUrl.openConnection()).thenReturn(innerConnection);
    when(innerConnection.getInputStream())
        .thenReturn(innerInputStream);

    var connection = new ReversingUrlConnection(url, innerUrl);
    connection.connect();

    // Then
    assertThat(connection.getInputStream())
        .isSameAs(connection.getInputStream());
  }

  @DisplayName(".getInputStream() does not close the inner input stream if successful")
  @Test
  void getInputStreamDoesNotCloseInnerInputStreamIfSuccessful() throws Exception {
    // Given
    var innerConnection = mock(URLConnection.class);
    var innerUrl = mock(URL.class);
    var url = URI.create("file://something").toURL();

    var data = "Hello, World!".getBytes(StandardCharsets.UTF_8);
    var innerInputStream = spy(new ByteArrayInputStream(data));

    when(innerUrl.openConnection()).thenReturn(innerConnection);
    when(innerConnection.getInputStream()).thenReturn(innerInputStream);

    var connection = new ReversingUrlConnection(url, innerUrl);
    connection.connect();

    // Then
    assertThat(connection.getInputStream())
        .isSameAs(connection.getInputStream());

    // Never expect it to be closed if it is healthy. The wrapping stream will manage the
    // lifecycle for us.
    verify(innerInputStream, never()).close();
  }

  @DisplayName(".getInputStream() raises an exception if not connected")
  @Test
  void getInputStreamRaisesExceptionIfNotConnected() throws Exception {
    // Given
    var innerConnection = mock(URLConnection.class);
    var innerUrl = mock(URL.class);
    var url = URI.create("file://something").toURL();

    when(innerUrl.openConnection()).thenReturn(innerConnection);
    when(innerConnection.getInputStream())
        .thenReturn(new ByteArrayInputStream("Hello, World!".getBytes(StandardCharsets.UTF_8)));

    var connection = new ReversingUrlConnection(url, innerUrl);

    // Then
    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(connection::getInputStream)
        .withMessage("not connected");
  }

  @DisplayName("the exception is reraised if the nested input stream raises a NestedUrlException")
  @Test
  void theExceptionIsReraisedIfNestedInputStreamRaisesNestedUrlException() throws Exception {
    // Given
    var cause = new NestedUrlException("Bang!", new IOException("uh-oh"));

    var innerConnection = mock(URLConnection.class);
    var innerUrl = mock(URL.class);
    var innerInputStream = mock(InputStream.class, raise(cause));
    var url = URI.create("file://something").toURL();

    when(innerUrl.openConnection()).thenReturn(innerConnection);
    when(innerConnection.getInputStream()).thenReturn(innerInputStream);
    doNothing().when(innerInputStream).close();

    var connection = new ReversingUrlConnection(url, innerUrl);
    connection.connect();

    // Then
    assertThatExceptionOfType(NestedUrlException.class)
        .isThrownBy(connection::getInputStream)
        .isSameAs(cause);
  }

  @DisplayName("the exception is wrapped if the nested input stream raises an IOException")
  @Test
  void theExceptionIsWrappedIfNestedInputStreamRaisesIoException() throws Exception {
    // Given
    var cause = new IOException("uh-oh");
    var innerConnection = mock(URLConnection.class);
    var innerUrl = mock(URL.class);
    var innerInputStream = mock(InputStream.class, raise(cause));
    var url = URI.create("file://something").toURL();

    when(innerUrl.openConnection()).thenReturn(innerConnection);
    when(innerConnection.getInputStream()).thenReturn(innerInputStream);
    doNothing().when(innerInputStream).close();

    var connection = new ReversingUrlConnection(url, innerUrl);
    connection.connect();

    // Then
    assertThatExceptionOfType(NestedUrlException.class)
        .isThrownBy(connection::getInputStream)
        .withMessage(
            "Failed to transfer '%s' wrapped with handler for protocol '%s', "
                + "an inner exception was raised: %s",
            innerUrl,
            url.getProtocol(),
            cause
        )
        .withCause(cause);
  }

  @DisplayName("the inner input stream is closed if the nested input stream raises an IOException")
  @Test
  void theInnerInputStreamIsClosedIfNestedInputStreamRaisesAnIoException() throws Exception {
    // Given
    var cause = new IOException("uh-oh");
    var innerConnection = mock(URLConnection.class);
    var innerUrl = mock(URL.class);
    var innerInputStream = mock(InputStream.class, raise(cause));
    var url = URI.create("file://something").toURL();

    when(innerUrl.openConnection()).thenReturn(innerConnection);
    when(innerConnection.getInputStream()).thenReturn(innerInputStream);
    doNothing().when(innerInputStream).close();

    var connection = new ReversingUrlConnection(url, innerUrl);
    connection.connect();

    // When
    assertThatExceptionOfType(NestedUrlException.class)
        .isThrownBy(connection::getInputStream);

    // Then
    verify(innerInputStream).close();
  }

  @DisplayName("further exceptions closing the inner input stream are suppressed")
  @Test
  void furtherExceptionsClosingTheInnerInputStreamAreSuppressed() throws Exception {
    // Given
    var cause = new IOException("uh-oh");
    var closureException = new IOException("I can't close! Help me!");
    var innerConnection = mock(URLConnection.class);
    var innerUrl = mock(URL.class);
    var innerInputStream = mock(InputStream.class, raise(cause));
    var url = URI.create("file://something").toURL();

    when(innerUrl.openConnection()).thenReturn(innerConnection);
    when(innerConnection.getInputStream()).thenReturn(innerInputStream);
    doThrow(closureException).when(innerInputStream).close();

    var connection = new ReversingUrlConnection(url, innerUrl);
    connection.connect();

    // Then
    assertThatExceptionOfType(NestedUrlException.class)
        .isThrownBy(connection::getInputStream)
        .withCause(cause)
        .satisfies(ex -> assertThat(ex).hasSuppressedException(closureException));
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
      is.transferTo(baos);
      var data = baos.toByteArray();
      var reversedData = new byte[data.length];

      for (var i = 0; i < data.length; ++i) {
        reversedData[data.length - 1 - i] = data[i];
      }

      return new ByteArrayInputStream(reversedData);
    }
  }

  static <T> Answer<T> raise(Throwable ex) {
    return ctx -> {
      throw ex;
    };
  }
}
