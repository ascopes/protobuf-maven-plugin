/*
 * Copyright (C) 2023 - 2024, Ashley Scopes.
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

package io.github.ascopes.protobufmavenplugin.spi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

@DisplayName("ZipUrlStreamHandlerProvider tests")
class ZipUrlStreamHandlerProviderTest {

  @TempDir
  Path tempDir;

  @DisplayName("The handler provider is returned when the URL scheme is 'zip'")
  @ParameterizedTest(name = "(scheme = {0})")
  @ValueSource(strings = {"zip", "ZIP", "zIp"})
  void theHandlerProviderIsReturnedWhenTheUrlSchemeIsZip(String scheme) {
    // Given
    var handlerProvider = new ZipUrlStreamHandlerProvider();

    // When
    var handler = handlerProvider.createURLStreamHandler(scheme);

    // Then
    assertThat(handler).isNotNull();
  }

  @DisplayName("The handler provider is not returned when the URL scheme is not 'zip'")
  @ParameterizedTest(name = "(scheme = {0})")
  @ValueSource(strings = {"jar", "gz", "tar", "wheel", "zippo", "gzip"})
  void theHandlerProviderIsNotReturnedWhenTheUrlSchemeIsNotZip(String scheme) {
    // Given
    var handlerProvider = new ZipUrlStreamHandlerProvider();

    // When
    var handler = handlerProvider.createURLStreamHandler(scheme);

    // Then
    assertThat(handler).isNull();
  }

  @DisplayName("ZIP contents can be read correctly via the registered SPI")
  @Test
  void zipContentsCanBeReadCorrectlyViaRegisteredSpi() throws IOException {
    // Given
    var expectedBazContent = UUID.randomUUID().toString();
    var expectedBorkContent = UUID.randomUUID().toString();
    createFile(tempDir.resolve("zip/foo/bar/Baz.txt"), expectedBazContent);
    createFile(tempDir.resolve("zip/foo/bar/Bork.txt"), expectedBorkContent);

    var zipName = UUID.randomUUID() + ".zip";
    var zipFile = createZip(zipName, tempDir.resolve("zip"));

    // When
    var bazUrl = new URL("zip:file://" + zipFile.toString() + "!/foo/bar/Baz.txt");
    var borkUrl = new URL("zip:file://" + zipFile.toString() + "!/foo/bar/Bork.txt");

    var actualBazContent = readContent(bazUrl);
    var actualBorkContent = readContent(borkUrl);

    // Then
    assertSoftly(softly -> {
      softly.assertThat(actualBazContent)
          .as("content of %s", bazUrl)
          .isEqualTo(expectedBazContent);
      softly.assertThat(actualBorkContent)
          .as("content of %s", borkUrl)
          .isEqualTo(expectedBorkContent);
    });
  }

  private Path createFile(Path path, String data) throws IOException {
    Files.createDirectories(path.getParent());
    return Files.writeString(path, data).toAbsolutePath();
  }

  // Create a zip file named <name> containing the contents of
  // <baseDir> recursively.
  private Path createZip(String name, Path baseDir) throws IOException {
    var zipPath = tempDir.resolve(name);

    try (
        var zip = new ZipOutputStream(Files.newOutputStream(zipPath));
        var files = Files.walk(baseDir).filter(Files::isRegularFile)
    ) {
      var iter = files.iterator();

      while (iter.hasNext()) {
        var nextFile = iter.next();
        var nextFileName = baseDir.relativize(nextFile).toString();

        zip.putNextEntry(new ZipEntry(nextFileName));
        var nextFileData = Files.readAllBytes(nextFile);
        zip.write(nextFileData, 0, nextFileData.length);
        zip.closeEntry();
      }
    }

    return zipPath.toAbsolutePath();
  }

  private String readContent(URL url) throws IOException {
    var conn = url.openConnection();
    conn.connect();
    try (
        var is = conn.getInputStream();
        var baos = new ByteArrayOutputStream()
    ) {
      is.transferTo(baos);
      return baos.toString();
    }
  }
}
