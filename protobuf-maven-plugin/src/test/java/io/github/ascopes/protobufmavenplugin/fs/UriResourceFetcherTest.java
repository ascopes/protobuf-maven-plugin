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

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static java.util.Objects.requireNonNullElse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.ascopes.protobufmavenplugin.utils.Digests;
import io.github.ascopes.protobufmavenplugin.utils.ResolutionException;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.maven.Maven;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("UriResourceFetcher tests")
@ExtendWith(MockitoExtension.class)
@WireMockTest
class UriResourceFetcherTest {

  WireMock wireMockClient;
  String wireMockBaseUri;

  @Mock
  TemporarySpace temporarySpace;

  @InjectMocks
  UriResourceFetcher uriResourceFetcher;

  @BeforeEach
  void setUp(WireMockRuntimeInfo wireMockInfo) {
    wireMockClient = wireMockInfo.getWireMock();
    wireMockBaseUri = wireMockInfo.getHttpBaseUrl();
  }

  @DisplayName("file URIs are resolved when they exist")
  @Test
  void fileUrisAreResolvedWhenTheyExist(@TempDir Path tempDir) throws Exception {
    // Given
    var file = Files.createFile(tempDir.resolve("nekomata.nya"));

    // When
    var result = uriResourceFetcher.fetchFileFromUri(file.toUri(), ".thiren");

    // Then
    assertThat(result)
        .isPresent()
        .get()
        .isEqualTo(file);
  }

  @DisplayName("file URIs are resolved when they do not exist")
  @Test
  void fileUrisAreResolvedWhenTheyDoNotExist(@TempDir Path tempDir) throws Exception {
    // Given
    var file = tempDir.resolve("nekomata.nya");

    // When
    var result = uriResourceFetcher.fetchFileFromUri(file.toUri(), ".thiren");

    // Then
    assertThat(result)
        .isEmpty();
  }

  @DisplayName("file URIs with bad characters result in an exception being raised")
  @Test
  void fileUrisWithBadCharactersResultInAnExceptionBeingRaised() throws Exception {
    // Given
    var uri = URI.create("file://bob@xxxxxx@Xx@X@X.localhost.net/59339785423");

    // Then
    assertThatExceptionOfType(ResolutionException.class)
        .isThrownBy(() -> uriResourceFetcher.fetchFileFromUri(uri, "boop"))
        // This varies between Linux and Windows....
        .withMessageMatching("Failed to discover file at "
            + "'file://bob@xxxxxx@Xx@X@X\\.localhost\\.net/59339785423': "
            + "java.lang.IllegalArgumentException: .*")
        .withCauseInstanceOf(IllegalArgumentException.class);
  }

  @DisplayName("other URIs are resolved when they exist")
  @ValueSource(strings = {"bar.txt.bin", "foo/bar.txt.bin"})
  @ParameterizedTest(name = "for path {0}")
  void otherUrisAreResolvedWhenTheyExist(
      String requestedPath,
      @TempDir Path tempDir
  ) throws Exception {
    // Given
    wireMockClient.register(get(urlEqualTo("/" + requestedPath))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "text/plain")
            .withBody("Hello, World!")));
    when(temporarySpace.createTemporarySpace(any(), any()))
        .thenReturn(tempDir);

    var uri = URI.create(wireMockBaseUri + "/" + requestedPath);
    var digest = Digests.sha1(uri.toASCIIString());
    var expectedFileName = requestedPath.contains("/")
        ? requestedPath.substring(requestedPath.lastIndexOf("/") + 1)
        : requestedPath;

    // When
    var finalPath = uriResourceFetcher.fetchFileFromUri(uri, ".textfile");

    // Then
    assertThat(finalPath)
        .isPresent()
        .get(InstanceOfAssertFactories.PATH)
        .isEqualTo(tempDir.resolve(expectedFileName + "-" + digest + ".textfile"))
        .hasContent("Hello, World!");

    wireMockClient.verifyThat(getRequestedFor(urlEqualTo("/" + requestedPath))
        .withHeader("User-Agent", equalTo(expectedUserAgent())));
  }

  @DisplayName("other pathless URIs are resolved when they exist")
  @Test
  void otherPathlessUrisAreResolvedWhenTheyExist(
      @TempDir Path tempDir
  ) throws Exception {
    // Given
    wireMockClient.register(get(urlEqualTo("/"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "text/plain")
            .withBody("Hello, World!")));
    when(temporarySpace.createTemporarySpace(any(), any()))
        .thenReturn(tempDir);

    var uri = URI.create(wireMockBaseUri);
    var digest = Digests.sha1(uri.toASCIIString());

    // When
    var finalPath = uriResourceFetcher.fetchFileFromUri(uri, ".textfile");

    // Then
    assertThat(finalPath)
        .isPresent()
        .get(InstanceOfAssertFactories.PATH)
        .isEqualTo(tempDir.resolve(digest + ".textfile"))
        .hasContent("Hello, World!");

    wireMockClient.verifyThat(getRequestedFor(urlEqualTo("/"))
        .withHeader("User-Agent", equalTo(expectedUserAgent())));
  }

  @DisplayName("other URIs are not resolved when they do not exist")
  @Test
  void otherUrisAreNotResolvedWhenTheyDoNotExist(@TempDir Path tempDir) throws Exception {
    // Given
    wireMockClient.register(get(urlEqualTo("/foo/bar.txt.bin"))
        .willReturn(aResponse().withStatus(404)));
    when(temporarySpace.createTemporarySpace(any(), any()))
        .thenReturn(tempDir);

    var uri = URI.create(wireMockBaseUri + "/foo/bar.txt.bin");

    // When
    var finalPath = uriResourceFetcher.fetchFileFromUri(uri, ".textfile");

    // Then
    assertThat(finalPath)
        .isEmpty();

    wireMockClient.verifyThat(getRequestedFor(urlEqualTo("/foo/bar.txt.bin"))
        .withHeader("User-Agent", equalTo(expectedUserAgent())));
  }

  @DisplayName("other URIs raise exceptions if transfer fails")
  @Test
  void otherUrisRaiseExceptionsIfTransferFails(@TempDir Path tempDir) throws Exception {
    // Given
    wireMockClient.register(get(urlEqualTo("/foo/bar.txt.bin"))
        .willReturn(aResponse().withFault(Fault.MALFORMED_RESPONSE_CHUNK)));
    when(temporarySpace.createTemporarySpace(any(), any()))
        .thenReturn(tempDir);

    var uri = URI.create(wireMockBaseUri + "/foo/bar.txt.bin");

    // Then
    assertThatExceptionOfType(ResolutionException.class)
        .isThrownBy(() -> uriResourceFetcher.fetchFileFromUri(uri, ".textfile"))
        .withCauseInstanceOf(IOException.class);

    wireMockClient.verifyThat(getRequestedFor(urlEqualTo("/foo/bar.txt.bin"))
        .withHeader("User-Agent", equalTo(expectedUserAgent())));
  }

  static String expectedUserAgent() {
    return String.format(
        "io.github.ascopes.protobuf-maven-plugin/%s org.apache.maven/%s (Java %s)",
        requireNonNullElse(
            // May not be set if we are running within an IDE.
            UriResourceFetcher.class.getPackage().getImplementationVersion(),
            "SNAPSHOT"
        ),
        Maven.class.getPackage().getImplementationVersion(),
        Runtime.version().toString()
    );
  }
}
