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

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("ZipUrlStreamHandlerProvider tests")
class ZipUrlStreamHandlerProviderTest {

  @DisplayName("the provider returns null for unsupported protocols")
  @Test
  void providerReturnsNullForUnsupportedProtocols() {
    // Given
    var provider = new ZipUrlStreamHandlerProvider();

    // Then
    assertThat(provider.createURLStreamHandler("tar"))
        .isNull();
  }

  @DisplayName("the provider returns a handler for supported protocols")
  @Test
  void providerReturnsHandlerForSupportedProtocols() {
    // Given
    var provider = new ZipUrlStreamHandlerProvider();

    // Then
    assertThat(provider.createURLStreamHandler("zip"))
        .isNotNull();
  }

  @DisplayName("the 'zip' protocol can be used to read files from ZIP archives")
  @Test
  void zipProtocolCanBeUsedToReadFilesFromZips(@TempDir Path tempDir) throws IOException {
    // Given
    var zip = createZip(tempDir.resolve("test.zip"), Map.of(
        "foo/bar/baz.txt", "Hello, World!",
        "META-INF/versions/8/foo/bar/baz.txt", "This should be ignored"
    ));

    var uri = URI.create("zip:" + zip.toUri() + "!/foo/bar/baz.txt");

    // When
    var conn = uri.toURL().openConnection();
    conn.connect();
    var os = new ByteArrayOutputStream();
    try (var is = conn.getInputStream()) {
      is.transferTo(os);
    }

    // Then
    assertThat(os.toString())
        .isEqualTo("Hello, World!");
  }

  static Path createZip(Path target, Map<String, String> files) throws IOException {
    try (var zos = new ZipOutputStream(Files.newOutputStream(target))) {
      for (var file : files.entrySet()) {
        zos.putNextEntry(new ZipEntry(file.getKey()));
        var data = file.getValue().getBytes();
        zos.write(data, 0, data.length);
        zos.closeEntry();
      }
    }
    return target;
  }
}
