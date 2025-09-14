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
import static com.github.tomakehurst.wiremock.client.WireMock.anyRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.anyUrl;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import io.github.ascopes.protobufmavenplugin.digests.Digest;
import io.github.ascopes.protobufmavenplugin.fs.TemporarySpace;
import io.github.ascopes.protobufmavenplugin.utils.ResolutionException;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.apache.maven.execution.MavenSession;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@Disabled("TODO: rewrite these tests using the new URL resolution mechanism")
@DisplayName("UriResourceFetcher tests")
@ExtendWith(MockitoExtension.class)
@WireMockTest
class UriResourceFetcherTest {

  WireMock wireMockClient;
  String wireMockBaseUri;

  @TempDir
  Path tempDir;

  @Mock(strictness = Mock.Strictness.LENIENT)
  MavenSession mavenSession;

  @Mock
  TemporarySpace temporarySpace;

  @InjectMocks
  UriResourceFetcher uriResourceFetcher;

  @BeforeEach
  void setUp(WireMockRuntimeInfo wireMockInfo) {
    wireMockClient = wireMockInfo.getWireMock();
    wireMockBaseUri = wireMockInfo.getHttpBaseUrl();

    when(mavenSession.isOffline())
        .thenReturn(false);
  }

  @DisplayName("invalid URI protocols are not resolved")
  @Test
  void invalidUriProtocolsAreNotResolved() {
    // Given
    var uri = URI.create("foobar:file://path/to/it");

    // Then
    assertThatExceptionOfType(ResolutionException.class)
        .isThrownBy(() -> uriResourceFetcher.fetchFileFromUri(uri, ".blob", false))
        .withMessage(
            "URI \"%s\" is invalid: java.net.MalformedURLException: "
                + "unknown protocol: foobar",
            uri
        )
        .withCauseInstanceOf(MalformedURLException.class);
  }

  @DisplayName("file URIs are resolved when they exist")
  @ValueSource(booleans = {true, false})
  @ParameterizedTest(name = "when MavenSession.isOffline returns {0}")
  void fileUrisAreResolvedWhenTheyExist(boolean isOffline) throws Exception {
    // Given
    when(mavenSession.isOffline())
        .thenReturn(isOffline);

    var expectedOutputDir = tempDir.resolve("outputs");
    Files.createDirectories(expectedOutputDir);
    when(temporarySpace.createTemporarySpace(any(), any()))
        .thenReturn(expectedOutputDir);

    var file = Files.writeString(tempDir.resolve("nekomata.nya"), "blahblah");
    var uri = file.toUri();
    var digest = Digest.compute("SHA-1", uri.toURL().toExternalForm()).toHexString();
    var expectedFileName = "nekomata.nya-" + digest + ".thiren";

    // When
    var result = uriResourceFetcher.fetchFileFromUri(uri, ".thiren", false);

    // Then
    assertThat(result)
        .isPresent()
        .get(InstanceOfAssertFactories.PATH)
        .isNotEqualTo(file)
        .isEqualTo(expectedOutputDir.resolve(expectedFileName))
        .content()
        .isEqualTo(Files.readString(file));
  }

  @DisabledOnOs(
      disabledReason = "Windows does not support POSIX permission bits",
      value = OS.WINDOWS
  )
  @DisplayName("the executable bit is set on files when requested")
  @ValueSource(booleans = {true, false})
  @ParameterizedTest(name = "when setExecutable = {0}")
  void theExecutableBitIsSetOnFilesWhenRequested(boolean setExecutable) throws Exception {
    // Given
    var file = Files.createFile(tempDir.resolve("nekomata.nya"));

    var expectedOutputDir = tempDir.resolve("outputs");
    Files.createDirectories(expectedOutputDir);
    when(temporarySpace.createTemporarySpace(any(), any()))
        .thenReturn(expectedOutputDir);

    // When
    var result = uriResourceFetcher.fetchFileFromUri(file.toUri(), ".thiren", setExecutable);

    // Then
    var resultFile = result.orElseThrow();
    assertThat(resultFile)
        .exists();
    assertThat(Files.isExecutable(resultFile))
        .withFailMessage("expected %s to %sbe executable", resultFile, setExecutable ? "" : "not ")
        .isEqualTo(setExecutable);
  }

  @DisplayName("nested file URIs are resolved when they exist")
  @CsvSource({
      "jar,  true",
      "zip,  true",
      "jar, false",
      "zip, false"
  })
  @ParameterizedTest(name = "for protocol {0}, when MavenSession.isOffline returns {1}")
  void nestedFileUrisAreResolvedWhenTheyExist(
      String protocol,
      boolean isOffline
  ) throws Exception {
    // Given
    when(mavenSession.isOffline())
        .thenReturn(isOffline);

    var expectedOutputDir = tempDir.resolve("outputs");
    Files.createDirectories(expectedOutputDir);
    when(temporarySpace.createTemporarySpace(any(), any()))
        .thenReturn(expectedOutputDir);

    var archiveFile = tempDir.resolve("inputs").resolve("archive." + protocol);
    Files.createDirectories(archiveFile.getParent());
    try (var outputStream = Files.newOutputStream(archiveFile)) {
      createJar(
          outputStream,
          Map.entry("some/file.txt", "some content here"),
          Map.entry("some/other/file.txt", "some more content here"),
          Map.entry("foo/bar.txt", "Hello, World!"),
          Map.entry("yet/another/file.bin", "blah blah blah")
      );
    }

    var uri = URI.create(protocol + ":" + archiveFile.toUri() + "!/foo/bar.txt");
    var digest = Digest.compute("SHA-1", uri.toURL().toExternalForm()).toHexString();
    var expectedFileName = "bar.txt-" + digest + ".log";

    // When
    var result = uriResourceFetcher.fetchFileFromUri(uri, ".log", false);

    // Then
    assertThat(result)
        .isPresent()
        .get(InstanceOfAssertFactories.PATH)
        .isEqualTo(expectedOutputDir.resolve(expectedFileName))
        .hasContent("Hello, World!");
  }

  @DisplayName("file URIs are not resolved when they do not exist")
  @Test
  void fileUrisAreNotResolvedWhenTheyDoNotExist() throws Exception {
    // Given
    var file = tempDir.resolve("nekomata.nya");

    var expectedOutputDir = tempDir.resolve("outputs");
    Files.createDirectories(expectedOutputDir);
    when(temporarySpace.createTemporarySpace(any(), any()))
        .thenReturn(expectedOutputDir);

    // When
    var result = uriResourceFetcher.fetchFileFromUri(file.toUri(), ".thiren", false);

    // Then
    assertThat(result).isNotPresent();
  }

  @DisplayName("nested file URIs are not resolved when they do not exist")
  @ValueSource(strings = {"jar", "zip"})
  @ParameterizedTest(name = "for protocol {0}")
  void nestedFileUrisAreNotResolvedWhenTheyDoNotExist(String protocol) throws Exception {
    // Given
    when(temporarySpace.createTemporarySpace(any(), any()))
        .thenReturn(tempDir);

    var expectedOutputDir = tempDir.resolve("outputs");
    Files.createDirectories(expectedOutputDir);
    when(temporarySpace.createTemporarySpace(any(), any()))
        .thenReturn(expectedOutputDir);

    var archiveFile = tempDir.resolve("inputs").resolve("archive." + protocol);
    Files.createDirectories(archiveFile.getParent());
    try (var outputStream = Files.newOutputStream(archiveFile)) {
      createJar(
          outputStream,
          Map.entry("some/file.txt", "some content here"),
          Map.entry("some/other/file.txt", "some more content here"),
          Map.entry("foo/bar.txt", "Hello, World!"),
          Map.entry("yet/another/file.bin", "blah blah blah")
      );
    }

    var uri = URI.create(protocol + ":" + archiveFile.toUri() + "!/missing-file.txt");

    // When
    var result = uriResourceFetcher.fetchFileFromUri(uri, ".log", false);

    // Then
    assertThat(result)
        .isEmpty();
  }

  @DisplayName("HTTP URIs are resolved when they exist")
  @ValueSource(strings = {"bar.txt.bin", "foo/bar.txt.bin"})
  @ParameterizedTest(name = "for path {0}")
  void httpUrisAreResolvedWhenTheyExist(String requestedPath) throws Exception {
    // Given
    wireMockClient.register(get(urlEqualTo("/" + requestedPath))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "text/plain")
            .withBody("Hello, World!")));
    when(temporarySpace.createTemporarySpace(any(), any()))
        .thenReturn(tempDir);

    var uri = URI.create(wireMockBaseUri + "/" + requestedPath);
    var digest = Digest.compute("SHA-1", uri.toURL().toExternalForm()).toHexString();
    var expectedFileName = requestedPath.contains("/")
        ? requestedPath.substring(requestedPath.lastIndexOf("/") + 1)
        : requestedPath;

    // When
    var finalPath = uriResourceFetcher.fetchFileFromUri(uri, ".textfile", false);

    // Then
    wireMockClient.verifyThat(getRequestedFor(urlEqualTo("/" + requestedPath)));

    assertThat(finalPath)
        .isPresent()
        .get(InstanceOfAssertFactories.PATH)
        .isEqualTo(tempDir.resolve(expectedFileName + "-" + digest + ".textfile"))
        .hasContent("Hello, World!");
  }

  @DisplayName("nested HTTP URIs are resolved when they exist")
  @ValueSource(strings = {"jar", "zip"})
  @ParameterizedTest(name = "for protocol {0}")
  void nestedHttpUrisAreResolvedWhenTheyExist(String protocol) throws Exception {
    // Given
    var data = new ByteArrayOutputStream();
    createJar(
        data,
        Map.entry("some/file.txt", "some content here"),
        Map.entry("some/other/file.txt", "some more content here"),
        Map.entry("foo/bar/baz.txt", "Hello, World!"),
        Map.entry("yet/another/file.bin", "blah blah blah")
    );

    wireMockClient.register(get(urlEqualTo("/some/archive." + protocol))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "application/octet-stream")
            .withBody(data.toByteArray())));

    when(temporarySpace.createTemporarySpace(any(), any()))
        .thenReturn(tempDir);

    var uri = URI.create(protocol + ":" + wireMockBaseUri
        + "/some/archive." + protocol + "!/foo/bar/baz.txt");

    var digest = Digest.compute("SHA-1", uri.toURL().toExternalForm()).toHexString();

    // When
    var finalPath = uriResourceFetcher.fetchFileFromUri(uri, ".textfile", false);

    // Then
    wireMockClient.verifyThat(getRequestedFor(urlEqualTo("/some/archive." + protocol)));

    assertThat(finalPath)
        .isPresent()
        .get(InstanceOfAssertFactories.PATH)
        .isEqualTo(tempDir.resolve("baz.txt-" + digest + ".textfile"))
        .hasContent("Hello, World!");
  }

  @DisplayName("Pathless HTTP URIs are resolved when they exist")
  @Test
  void pathlessHttpUrisAreResolvedWhenTheyExist() throws Exception {
    // Given
    wireMockClient.register(get(urlEqualTo("/"))
        .willReturn(aResponse()
            .withStatus(200)
            .withHeader("Content-Type", "text/plain")
            .withBody("Hello, World!")));
    when(temporarySpace.createTemporarySpace(any(), any()))
        .thenReturn(tempDir);

    var uri = URI.create(wireMockBaseUri);
    var digest = Digest.compute("SHA-1", uri.toURL().toExternalForm()).toHexString();

    // When
    var finalPath = uriResourceFetcher.fetchFileFromUri(uri, ".textfile", false);

    // Then
    wireMockClient.verifyThat(getRequestedFor(urlEqualTo("/")));

    assertThat(finalPath)
        .isPresent()
        .get(InstanceOfAssertFactories.PATH)
        .isEqualTo(tempDir.resolve(digest + ".textfile"))
        .hasContent("Hello, World!");
  }

  @DisplayName("HTTP URIs are not resolved when they do not exist")
  @Test
  void httpUrisAreNotResolvedWhenTheyDoNotExist() throws Exception {
    // Given
    wireMockClient.register(get(urlEqualTo("/foo/bar.txt.bin"))
        .willReturn(aResponse().withStatus(404)));
    when(temporarySpace.createTemporarySpace(any(), any()))
        .thenReturn(tempDir);

    var uri = URI.create(wireMockBaseUri + "/foo/bar.txt.bin");

    // When
    var finalPath = uriResourceFetcher.fetchFileFromUri(uri, ".textfile", false);

    // Then
    wireMockClient.verifyThat(getRequestedFor(urlEqualTo("/foo/bar.txt.bin")));

    assertThat(finalPath)
        .isEmpty();
  }

  @DisplayName("HTTP URIs raise exceptions if transfer fails")
  @Test
  void httpUrisRaiseExceptionsIfTransferFails() {
    // Given
    wireMockClient.register(get(urlEqualTo("/foo/bar.txt.bin"))
        .willReturn(aResponse().withFault(Fault.MALFORMED_RESPONSE_CHUNK)));
    when(temporarySpace.createTemporarySpace(any(), any()))
        .thenReturn(tempDir);

    var uri = URI.create(wireMockBaseUri + "/foo/bar.txt.bin");

    // Then
    assertThatExceptionOfType(ResolutionException.class)
        .isThrownBy(() -> uriResourceFetcher.fetchFileFromUri(uri, ".textfile", false))
        .withCauseInstanceOf(IOException.class);

    wireMockClient.verifyThat(getRequestedFor(urlEqualTo("/foo/bar.txt.bin")));
  }

  @DisplayName("HTTP URIs raise exceptions if offline mode is enabled")
  @Test
  void httpUrisRaiseExceptionsIfOfflineModeIsEnabled() {
    // Given
    when(mavenSession.isOffline())
        .thenReturn(true);

    var uri = URI.create(wireMockBaseUri + "/foo/bar.txt.bin");
    // Then
    assertThatExceptionOfType(ResolutionException.class)
        .isThrownBy(() -> uriResourceFetcher.fetchFileFromUri(uri, ".textfile", false))
        .withMessage(
            "Cannot resolve URI \"%s\". Only a limited number of URL protocols "
                + "are supported in offline mode.",
            uri
        );

    wireMockClient.verifyThat(0, anyRequestedFor(anyUrl()));
  }

  @DisplayName("nested HTTP URIs raise exceptions if offline mode is enabled")
  @Test
  void nestedHttpUrisRaiseExceptionsIfOfflineModeIsEnabled() {
    // Given
    when(mavenSession.isOffline())
        .thenReturn(true);

    var uri = URI.create("jar:" + wireMockBaseUri + "/foo/bar.txt.bin!/foo.txt");
    // Then
    assertThatExceptionOfType(ResolutionException.class)
        .isThrownBy(() -> uriResourceFetcher.fetchFileFromUri(uri, ".textfile", false))
        .withMessage(
            "Cannot resolve URI \"%s\". Only a limited number of URL protocols "
                + "are supported in offline mode.",
            uri
        );

    wireMockClient.verifyThat(0, anyRequestedFor(anyUrl()));
  }

  @SafeVarargs
  @SuppressWarnings("vararg")
  static void createJar(
      OutputStream outputStream,
      Map.Entry<String, String>... files
  ) throws IOException {
    try (var zipOutputStream = new ZipOutputStream(outputStream)) {
      for (var file : files) {
        zipOutputStream.putNextEntry(new ZipEntry(file.getKey()));
        zipOutputStream.write(file.getValue().getBytes());
        zipOutputStream.closeEntry();
      }
    }
  }
}
