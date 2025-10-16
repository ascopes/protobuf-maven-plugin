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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.catchThrowable;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.net.URI;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("UrlFactory tests")
@WireMockTest
class UrlFactoryTest {

  @TempDir
  Path tempDir;

  WireMock wireMockClient;
  String wireMockBaseUrl;

  UrlFactory factory;

  @BeforeEach
  void setUp(WireMockRuntimeInfo wireMock) {
    wireMockClient = wireMock.getWireMock();
    wireMockBaseUrl = wireMock.getHttpBaseUrl();

    factory = new UrlFactory();
    factory.init();
  }

  @DisplayName("creating a URL before init() is called results in an error")
  @Test
  void creatingUrlBeforeInitCalledResultsInError() throws Exception {
    // Given
    var uri = URI.create("https://google.com");

    // Then
    assertThatExceptionOfType(NullPointerException.class)
        .isThrownBy(() -> new UrlFactory().create(uri));
  }

  @DisplayName("Local file system protocol gets the expected result")
  @Test
  void localFileSystemProtocolGetsTheExpectedResult() throws Exception {
    // Given
    var someContent = "foo bar baz " + System.nanoTime();
    var someFile = Files.writeString(
        tempDir.resolve("some-file.txt"),
        someContent
    );

    // When
    var url = factory.create(someFile.toUri());
    var actualContent = readString(url);

    // Then
    assertThat(actualContent)
        .isEqualTo(someContent);
  }

  @DisplayName("Custom archiving protocols on local files extract the expected result")
  @ParameterizedTest(name = "for URI {0}:file:.../{1}")
  @CsvSource({
      "ear,       example.zip",
      "jar,       example.zip",
      "kar,       example.zip",
      "tar,       example.tar",
      "tar:bz,    example.tar.bz2",
      "tar:bz2,   example.tar.bz2",
      "tar:bzip,  example.tar.bz2",
      "tar:bzip2, example.tar.bz2",
      "tar:gz,    example.tar.gz",
      "tar:gzip,  example.tar.gz",
      "war,       example.zip",
      "zip,       example.zip",
  })
  void customArchivingProtocolsOnLocalFilesExtractTheExpectedResult(
      String protocol,
      String testFile
  ) throws Exception {
    // Given
    var archivePath = pathForTestFile(testFile);
    var baseUri = protocol + ":" + archivePath.toUri() + "!/";
    var fooTxtUri = URI.create(baseUri + "foo.txt");
    var borkTxtUri = URI.create(baseUri + "bar/baz/bork.txt");

    // When
    var fooTxtUrl = factory.create(fooTxtUri);
    var borkTxtUrl = factory.create(borkTxtUri);
    var fooTxtContent = readString(fooTxtUrl);
    var borkTxtContent = readString(borkTxtUrl);

    // Then
    assertSoftly(softly -> {
      softly.assertThat(fooTxtContent)
          .describedAs("%s content", fooTxtUri)
          .isEqualTo("this is called foo\n");
      softly.assertThat(borkTxtContent)
          .describedAs("%s content", borkTxtUri)
          .isEqualTo("this is a nested bork\n");
    });
  }

  @DisplayName("Custom nested archiving protocols on local files extract the expected result")
  @Test
  void customNestedArchivingProtocolsOnLocalFilesExtractTheExpectedResult() throws Exception {
    // Given
    var archivePath = pathForTestFile("nested-example.zip");
    var baseUri = "tar:gz:zip:" + archivePath.toUri() + "!/example.tar.gz!/";
    var fooTxtUri = URI.create(baseUri + "foo.txt");
    var borkTxtUri = URI.create(baseUri + "bar/baz/bork.txt");

    // When
    var fooTxtUrl = factory.create(fooTxtUri);
    var borkTxtUrl = factory.create(borkTxtUri);
    var fooTxtContent = readString(fooTxtUrl);
    var borkTxtContent = readString(borkTxtUrl);

    // Then
    assertSoftly(softly -> {
      softly.assertThat(fooTxtContent)
          .describedAs("%s content", fooTxtUri)
          .isEqualTo("this is called foo\n");
      softly.assertThat(borkTxtContent)
          .describedAs("%s content", borkTxtUri)
          .isEqualTo("this is a nested bork\n");
    });
  }

  @DisplayName("HTTP protocol locates the expected resource correctly")
  @ValueSource(strings = {"", "foo.txt", "bar/baz/bork.txt"})
  @ParameterizedTest(name = "for path \"{0}\"")
  void httpProtocolLocatesTheExpectedResourceCorrectly(String path) throws Exception {
    // Given
    wireMockClient.register(get(urlEqualTo("/" + path))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "text/plain")
            .withBody("Hello, World!")));
    var uri = URI.create(wireMockBaseUrl + "/" + path);

    // When
    var url = factory.create(uri);
    var actualContent = readString(url);

    // Then
    assertThat(actualContent)
        .isEqualTo("Hello, World!");
    wireMockClient
        .verifyThat(getRequestedFor(urlEqualTo("/" + path)));
  }

  @DisplayName("HTTP protocol gets response with status code 404 and throws FileNotFoundException")
  @ValueSource(strings = {"", "foo.txt", "bar/baz/bork.txt"})
  @ParameterizedTest(name = "for path \"{0}\"")
  void httpProtocolGetsResponseWithStatusCode404AndThrowsFileNotFoundException(
      String path
  ) throws Exception {
    // Given
    wireMockClient.register(get(urlEqualTo("/" + path))
        .willReturn(aResponse()
            .withStatus(404)));
    var uri = URI.create(wireMockBaseUrl + "/" + path);

    // When
    var url = factory.create(uri);
    Throwable thrown = catchThrowable(() -> readString(url));

    // Then
    assertThat(thrown).isInstanceOf(FileNotFoundException.class);
    wireMockClient
        .verifyThat(getRequestedFor(urlEqualTo("/" + path)));
  }

  @DisplayName("HTTP protocol gets response with status code 500 and throws HttpRequestException")
  @ValueSource(strings = {"", "foo.txt", "bar/baz/bork.txt"})
  @ParameterizedTest(name = "for path \"{0}\"")
  void httpProtocolGetsResponseWithStatusCode500AndThrowsHttpRequestException(
      String path
  ) throws Exception {
    // Given
    wireMockClient.register(get(urlEqualTo("/" + path))
        .willReturn(aResponse()
            .withStatus(500)
            .withHeader("Correlation-Id", "correlationId")
            .withHeader("Request-Id", "requestId")
            .withHeader("WWW-Authenticate", "auth")
            .withHeader("Proxy-Authenticate", "proxy")
            .withBody("failure")));
    var uri = URI.create(wireMockBaseUrl + "/" + path);

    // When
    var url = factory.create(uri);
    Throwable thrown = catchThrowable(() -> readString(url));

    // Then
    assertThat(thrown).isInstanceOf(HttpRequestException.class);
    var ex = (HttpRequestException) thrown;
    assertSoftly(softly -> {
      softly.assertThat(ex.getStatusCode())
          .isEqualTo(500);
      softly.assertThat(ex.getCorrelationId())
          .isEqualTo("correlationId");
      softly.assertThat(ex.getRequestId())
          .isEqualTo("requestId");
      softly.assertThat(ex.getWwwAuthenticate())
          .isEqualTo("auth");
      softly.assertThat(ex.getProxyAuthenticate())
          .isEqualTo("proxy");
      softly.assertThat(ex.getResponseBody())
          .isEqualTo("failure");
    });
    wireMockClient
        .verifyThat(getRequestedFor(urlEqualTo("/" + path)));
  }

  @DisplayName("HTTP protocol follows 302 redirects automatically")
  @ValueSource(strings = {"foo.txt", "bar/baz/bork.txt"})
  @ParameterizedTest(name = "for path \"{0}\"")
  void httpProtocolFollows302RedirectsAutomatically(String path) throws Exception {
    // Given
    var redirectedPath = "/redirected/" + path;
    var redirectedBody = "Redirected content for " + path;
    wireMockClient.register(get(urlEqualTo("/" + path))
        .willReturn(aResponse()
            .withStatus(302)
            .withHeader("Location", wireMockBaseUrl + redirectedPath)));
    wireMockClient.register(get(urlEqualTo(redirectedPath))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "text/plain")
            .withBody(redirectedBody)));
    var uri = URI.create(wireMockBaseUrl + "/" + path);

    // When
    var url = factory.create(uri);
    var actualContent = readString(url);

    // Then
    assertThat(actualContent)
        .isEqualTo(redirectedBody);
    wireMockClient.verifyThat(getRequestedFor(urlEqualTo("/" + path)));
    wireMockClient.verifyThat(getRequestedFor(urlEqualTo(redirectedPath)));
  }

  static Path pathForTestFile(String name) {
    var path = Path.of("src", "test", "resources");
    for (var frag : UrlFactoryTest.class.getPackageName().split("\\.")) {
      path = path.resolve(frag);
    }
    return path.resolve(name);
  }

  static String readString(URL url) throws Exception {
    var conn = url.openConnection();
    conn.setDoOutput(false);
    conn.setUseCaches(false);
    conn.connect();

    try (var is = conn.getInputStream()) {
      var baos = new ByteArrayOutputStream();
      is.transferTo(baos);
      return baos.toString(StandardCharsets.UTF_8);
    }
  }
}
