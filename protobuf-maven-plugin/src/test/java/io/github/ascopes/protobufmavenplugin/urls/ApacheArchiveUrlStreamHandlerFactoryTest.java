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
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("ApacheArchiveUrlStreamHandlerFactory tests")
class ApacheArchiveUrlStreamHandlerFactoryTest {

  UrlFactory urlFactory;

  ApacheArchiveUrlStreamHandlerFactory urlStreamHandlerFactory;

  @BeforeEach
  void setUp() {
    urlFactory = mock();
    urlStreamHandlerFactory = new ApacheArchiveUrlStreamHandlerFactory(
        urlFactory,
        TarArchiveInputStream::new,
        "tar"
    );
  }

  @DisplayName("missing path fragments (xxx!/yyy) are invalid")
  @ValueSource(strings = {
      "tar:/",
      "tar:/foo",
      "tar:http://example.com",
      "tar:file:foo/bar/baz",
  })
  @ParameterizedTest(name = "for URL \"{0}\"")
  void missingPathFragmentsAreInvalid(String rawUrl) throws Exception {
    // Given
    var url = urlOf(rawUrl);

    // Then
    assertThatExceptionOfType(IOException.class)
        .isThrownBy(url::openConnection)
        .withMessage(
            "URI '%s' was missing a nested path fragment (e.g. "
                + "'tar:http://some-website.com/some-file!/path/within/archive')",
            rawUrl
        );
  }

  @DisplayName("FileNotFoundException is raised if no entry matching the given name is found")
  @ValueSource(strings = {
      "",
      "./.",
      "missing.txt",
      "./missing.txt"
  })
  @ParameterizedTest(name = "for requested TAR entry \"{0}\"")
  void fileNotFoundExceptionRaisedIfNoEntryMatchingGivenNameIsFound(
      String missingTarEntry
  ) throws Exception {
    // Given
    var safeTarEntry = missingTarEntry.replaceAll("^\\./", "");
    var tarFileUrl = urlForTestFile("example.tar");
    var url = urlOf("tar", tarFileUrl, missingTarEntry);
    when(urlFactory.create(any())).thenReturn(tarFileUrl);

    // When
    var conn = url.openConnection();
    conn.connect();

    // Then
    assertThatExceptionOfType(AbstractNestingUrlConnection.NestedUrlException.class)
        .isThrownBy(conn::getInputStream)
        .withMessage(
            "Failed to transfer '%s' wrapped with handler for protocol 'tar', an inner "
                + "exception was raised: %s: Could not find '%s' within %s",
            tarFileUrl,
            FileNotFoundException.class.getName(),
            safeTarEntry,
            TarArchiveInputStream.class.getSimpleName()
        )
        .havingCause()
        .isInstanceOf(FileNotFoundException.class)
        .withMessage(
            "Could not find '%s' within %s",
            safeTarEntry,
            TarArchiveInputStream.class.getSimpleName()
        );
  }

  @DisplayName("file contents are read successfully from the archive")
  @Test
  void fileContentsAreReadSuccessfullyFromTheArchive() throws Exception {
    // Given
    var tarFileUrl = urlForTestFile("example.tar");
    var url = urlOf("tar", tarFileUrl, "foo.txt");
    when(urlFactory.create(any())).thenReturn(tarFileUrl);

    // When
    var baos = new ByteArrayOutputStream();
    var conn = url.openConnection();
    conn.connect();
    try (var is = conn.getInputStream()) {
      is.transferTo(baos);
    }

    // Then
    assertThat(baos.toString(StandardCharsets.UTF_8))
        .as("contents extracted from %s", url)
        .isEqualTo("this is called foo\n");
  }

  URL urlOf(String scheme, URL inner, String path) throws Exception {
    return urlOf(scheme + ":" + inner + "!/" + path);
  }

  URL urlOf(String spec) throws Exception {
    return new URL(null, spec, urlStreamHandlerFactory.createURLStreamHandler("tar"));
  }

  URL urlForTestFile(String name) throws Exception {
    var path = Path.of("src", "test", "resources");
    for (var frag : getClass().getPackageName().split("\\.", -1)) {
      path = path.resolve(frag);
    }
    return path.resolve(name).toUri().toURL();
  }
}
