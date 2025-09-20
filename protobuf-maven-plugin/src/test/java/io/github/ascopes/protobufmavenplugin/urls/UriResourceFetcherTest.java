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

import static java.util.Objects.requireNonNullElse;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.InstanceOfAssertFactories.PATH;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.matches;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import io.github.ascopes.protobufmavenplugin.digests.Digest;
import io.github.ascopes.protobufmavenplugin.fs.FileUtils;
import io.github.ascopes.protobufmavenplugin.fs.TemporarySpace;
import io.github.ascopes.protobufmavenplugin.utils.ResolutionException;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.apache.maven.execution.MavenSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Answers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mock.Strictness;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("UriResourceFetcher tests")
@ExtendWith(MockitoExtension.class)
class UriResourceFetcherTest {

  Path temporarySpaceDir;

  @Mock
  MavenSession session;

  @Mock(strictness = Strictness.LENIENT)
  TemporarySpace temporarySpace;

  @Mock
  UrlFactory urlFactory;

  @InjectMocks
  UriResourceFetcher uriResourceFetcher;

  @BeforeEach
  void setUp(@TempDir Path tempDir) throws IOException {
    temporarySpaceDir = tempDir.resolve("temporary-space");
    Files.createDirectories(temporarySpaceDir);

    when(temporarySpace.createTemporarySpace(any(String[].class)))
        .thenReturn(temporarySpaceDir);
  }

  @DisplayName("Local file protocols can be resolved if offline mode is enabled")
  @MethodSource("fileUris")
  @ParameterizedTest(name = "for URI {0}")
  void localFileProtocolsCanBeResolvedIfOfflineModeIsEnabled(
      URI uri,
      @TempDir Path tempDir
  ) throws Exception {
    // Given
    when(session.isOffline())
        .thenReturn(true);

    var dir = Files.createDirectories(tempDir.resolve("foo"));
    var file = Files.writeString(dir.resolve("bar.txt"), "blah blah");

    when(urlFactory.create(any()))
        .thenReturn(file.toUri().toURL());

    // When
    var result = uriResourceFetcher.fetchFileFromUri(uri, "foo", true);

    // Then
    assertThat(result)
        .get(PATH)
        .hasSameBinaryContentAs(file);

    verify(urlFactory).create(uri);
    verifyNoMoreInteractions(urlFactory);
  }

  @DisplayName("Remote protocols cannot be resolved if offline mode is enabled")
  @MethodSource("remoteUris")
  @ParameterizedTest(name = "for URI {0}")
  void remoteProtocolsCannotBeResolvedIfOfflineModeIsEnabled(URI uri) {
    // Given
    when(session.isOffline())
        .thenReturn(true);

    // Then
    assertThatExceptionOfType(ResolutionException.class)
        .isThrownBy(() -> uriResourceFetcher.fetchFileFromUri(uri, "foo", true))
        .withMessage(
            "Cannot resolve URI \"%s\". Only a limited number of URL protocols are supported "
                + "in offline mode.",
            uri
        );

    verifyNoInteractions(urlFactory);
  }

  @DisplayName("URIs are resolved and stored in local files")
  @MethodSource({"fileUris", "remoteUris"})
  @ParameterizedTest(name = "for URI {0}")
  void urisAreResolvedAndStoredInLocalFiles(URI uri) throws Exception {
    // Given
    var url = someUrlWithResolvedContent(uri, "foobar");
    when(urlFactory.create(any()))
        .thenReturn(url);

    Path expectedFile;
    if (uri.getPath() == null) {
      expectedFile = temporarySpaceDir
          .resolve(
              Digest.compute("SHA-1", url.toExternalForm()).toHexString()
                  + ".ext"
          );
    } else {
      expectedFile = temporarySpaceDir
          .resolve(
              uri.getPath().substring(uri.getPath().lastIndexOf("/") + 1)
                  + "-"
                  + Digest.compute("SHA-1", url.toExternalForm()).toHexString()
                  + ".ext"
          );
    }

    // When
    var result = uriResourceFetcher.fetchFileFromUri(uri, ".ext", false);

    // Then
    assertThat(result)
        .get(PATH)
        .isEqualTo(expectedFile)
        .hasBinaryContent("foobar".getBytes(StandardCharsets.UTF_8));

    verify(urlFactory).create(uri);
    verify(temporarySpace).createTemporarySpace("url", uri.getScheme());
  }

  @DisplayName("URLConnections are configured with the expected attributes")
  @Test
  void urlConnectionsAreConfiguredWithTheExpectedAttributes() throws Exception {
    // Given
    var uri = URI.create("some://google.com/foo/bar/baz.txt");
    var url = someUrlWithResolvedContent(uri, "bazbork");
    when(urlFactory.create(any()))
        .thenReturn(url);

    // When
    uriResourceFetcher.fetchFileFromUri(uri, ".ext", false);

    // Then
    var conn = url.openConnection();
    verify(conn).addRequestProperty(
        eq("User-Agent"),
        matches(
            "^"
                + "protobuf-maven-plugin/.*? \\(io.github.ascopes\\) "
                + "Apache-Maven/.*? "
                + "Java/.*? \\(.*?, .*?, .*?\\)"
                + "$"
        )
    );
    verify(conn).setAllowUserInteraction(false);
    verify(conn).setConnectTimeout(30_000);
    verify(conn).setDoInput(true);
    verify(conn).setDoOutput(false);
    verify(conn).setReadTimeout(30_000);
    verify(conn).setUseCaches(false);
  }

  @DisplayName("Executable bits are set on the resulting file when instructed")
  @ValueSource(booleans = {true, false})
  @ParameterizedTest(name = "when setExecutable = {0}")
  void executableBitsAreSetOnTheResultingFileWhenInstructed(
      boolean setExecutable
  ) throws Exception {
    try (var fileUtilsMock = mockStatic(FileUtils.class, Answers.CALLS_REAL_METHODS)) {
      // Given
      var uri = URI.create("some://google.com/foo/bar/baz.txt");
      var url = someUrlWithResolvedContent(uri, "bazbork");
      when(urlFactory.create(any()))
          .thenReturn(url);

      Path expectedFile = temporarySpaceDir
          .resolve(
              uri.getPath().substring(uri.getPath().lastIndexOf("/") + 1)
                  + "-"
                  + Digest.compute("SHA-1", url.toExternalForm()).toHexString()
                  + ".ext"
          );

      // When
      uriResourceFetcher.fetchFileFromUri(uri, ".ext", setExecutable);

      // Then
      fileUtilsMock
          .verify(() -> FileUtils.makeExecutable(expectedFile), times(setExecutable ? 1 : 0));
    }
  }

  @DisplayName("URL resolution errors result in an exception being raised")
  @Test
  void urlResolutionErrorsResultInAnExceptionBeingRaised() throws Exception {
    // Given
    var uri = URI.create("some://google.com/foo/bar/baz.txt");
    var cause = new IOException("bang");
    when(urlFactory.create(any()))
        .thenThrow(cause);

    // Then
    assertThatExceptionOfType(ResolutionException.class)
        .isThrownBy(() -> uriResourceFetcher.fetchFileFromUri(uri, "something", false))
        .withMessage("URI \"%s\" is invalid: %s", uri, cause)
        .withCause(cause);
  }

  @DisplayName("URL connect failures result in an exception being raised")
  @Test
  void urlConnectFailuresResultInAnExceptionBeingRaised() throws Exception {
    // Given
    var uri = URI.create("some://google.com/foo/bar/baz.txt");
    var url = someUrlWithResolvedContent(uri, "bazbork");
    when(urlFactory.create(any()))
        .thenReturn(url);
    var cause = new IOException("bang");
    when(url.openConnection())
        .thenThrow(cause);

    Path expectedFile = temporarySpaceDir
        .resolve(
            uri.getPath().substring(uri.getPath().lastIndexOf("/") + 1)
                + "-"
                + Digest.compute("SHA-1", url.toExternalForm()).toHexString()
                + ".something"
        );

    // Then
    assertThatExceptionOfType(ResolutionException.class)
        .isThrownBy(() -> uriResourceFetcher.fetchFileFromUri(uri, ".something", false))
        .withMessage("Failed to transfer \"%s\" to \"%s\": %s", uri, expectedFile, cause)
        .withCause(cause);
  }

  @DisplayName("URL transfer failures result in an exception being raised")
  @Test
  void urlTransferFailuresResultInAnExceptionBeingRaised() throws Exception {
    // Given
    var uri = URI.create("some://google.com/foo/bar/baz.txt");
    var url = someUrlWithResolvedContent(uri, "bazbork");
    when(urlFactory.create(any()))
        .thenReturn(url);

    var cause = new IOException("bang");
    var badInputStream = new BadInputStream(cause);
    var conn = mock(URLConnection.class);
    when(conn.getInputStream())
        .thenReturn(badInputStream);
    when(url.openConnection())
        .thenReturn(conn);

    Path expectedFile = temporarySpaceDir
        .resolve(
            uri.getPath().substring(uri.getPath().lastIndexOf("/") + 1)
                + "-"
                + Digest.compute("SHA-1", url.toExternalForm()).toHexString()
                + ".something"
        );

    // Then
    assertThatExceptionOfType(ResolutionException.class)
        .isThrownBy(() -> uriResourceFetcher.fetchFileFromUri(uri, ".something", false))
        .withMessage("Failed to transfer \"%s\" to \"%s\": %s", uri, expectedFile, cause)
        .withCause(cause);
  }

  @DisplayName("FileNotFoundExceptions raised during URL connect return an empty result")
  @Test
  void fileNotFoundExceptionsRaisedDuringUrlConnectReturnEmptyResult() throws Exception {
    // Given
    var uri = URI.create("some://google.com/foo/bar/baz.txt");
    var url = someUrlWithResolvedContent(uri, "bazbork");
    when(urlFactory.create(any()))
        .thenReturn(url);
    var cause = new FileNotFoundException("bang");
    when(url.openConnection())
        .thenThrow(cause);

    // When
    var result = uriResourceFetcher.fetchFileFromUri(uri, ".foo", false);

    // Then
    assertThat(result)
        .isEmpty();
  }

  @DisplayName("FileNotFoundExceptions raised during URL transfer return an empty result")
  @Test
  void fileNotFoundExceptionsRaisedDuringUrlTransferReturnEmptyResult() throws Exception {
    // Given
    var uri = URI.create("some://google.com/foo/bar/baz.txt");
    var url = someUrlWithResolvedContent(uri, "bazbork");
    when(urlFactory.create(any()))
        .thenReturn(url);

    var cause = new FileNotFoundException("bang");
    var badInputStream = new BadInputStream(cause);
    var conn = mock(URLConnection.class);
    when(conn.getInputStream())
        .thenReturn(badInputStream);
    when(url.openConnection())
        .thenReturn(conn);

    // When
    var result = uriResourceFetcher.fetchFileFromUri(uri, ".foo", false);

    // Then
    assertThat(result)
        .isEmpty();
  }

  static Stream<URI> fileUris() {
    return Stream
        .of(
            "file:foo/bar/baz.txt",
            "tar:gz:file:foo/bar/baz.tgz!/foo.txt",
            "tar:bz2:file:foo/bar/baz.tbz!/bar.txt",
            "zip:file:foo/bar.zip!/cat.png",
            "jar:file:foo/bar/baz/foo/bar.jar!/dog.tiff"
        )
        .map(URI::create);
  }

  static Stream<URI> remoteUris() {
    return Stream
        .of(
            "ftp://example.com/foo/bar.txt",
            "sftp://example.com/foo/bar.txt",
            "http://example.com/foo/bar.txt",
            "https://example.com/foo/bar.txt",
            "tar:gz:ftp://example.com/foo/bar.tgz!/foo.txt",
            "tar:bz2:sftp://example.com/foo/bar.tbz!/bar.txt",
            "zip:http://example.com/foo/bar.zip!/cat.png",
            "jar:https://example.com/foo/bar.jar!/dog.tiff"
        )
        .map(URI::create);
  }

  static URL someUrlWithResolvedContent(URI uri, String content) throws Exception {
    return someUrlWithResolvedContent(uri, content.getBytes(StandardCharsets.UTF_8));
  }

  static URL someUrlWithResolvedContent(URI uri, byte[] content) throws Exception {
    var connection = mock(URLConnection.class);
    lenient().when(connection.getInputStream())
        .thenReturn(new ByteArrayInputStream(content));

    var url = mock(URL.class);
    lenient().when(url.openConnection())
        .thenReturn(connection);
    lenient().when(url.toExternalForm())
        .thenReturn(uri.toString());
    // Quirk: URI.getPath() returns null when the corresponding URL would return empty string here.
    lenient().when(url.getPath())
        .thenReturn(requireNonNullElse(uri.getPath(), ""));
    lenient().when(url.getProtocol())
        .thenReturn(uri.getScheme());
    return url;
  }

  // Overridden as mocking an InputStream and wrapping it in a BufferedInputStream seems to
  // result in an infinite loop, memory leaks, and heap exhaustion on Zulu JDKs!
  static class BadInputStream extends InputStream {

    private final IOException ex;

    BadInputStream(IOException ex) {
      this.ex = ex;
    }

    @Override
    public int read() throws IOException {
      throw ex;
    }
  }
}
