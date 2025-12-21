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
package io.github.ascopes.protobufmavenplugin.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

import java.io.IOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("TeeWriter tests")
class TeeWriterTest {

  @DisplayName("the expected content is written to the file")
  @Test
  void expectedContentWrittenToFile(@TempDir Path tempDir) throws IOException {
    // Given
    var text = Stream.generate(UUID::randomUUID)
        .map(UUID::toString)
        .limit(10)
        .collect(Collectors.joining("\n"));
    var file = tempDir.resolve(UUID.randomUUID() + ".txt");

    // When
    try (var writer = new TeeWriter(Files.newBufferedWriter(file, StandardCharsets.UTF_8))) {
      writer.append(text);
    }

    // Then
    assertThat(file).hasContent(text);
  }

  @DisplayName("the expected content is written to the buffer")
  @Test
  void expectedContentWrittenToBuffer(@TempDir Path tempDir) throws IOException {
    // Given
    var text = Stream.generate(UUID::randomUUID)
        .map(UUID::toString)
        .limit(10)
        .collect(Collectors.joining("\n"));
    var file = tempDir.resolve(UUID.randomUUID().toString() + ".txt");

    // When
    var writer = new TeeWriter(Files.newBufferedWriter(file, StandardCharsets.UTF_8));
    try (writer) {
      writer.append(text);
    }

    // Then
    assertThat(writer).asString().isEqualTo(text);
  }

  @DisplayName(".close() closes the underlying writer")
  @Test
  void closeClosesWriter() throws IOException {
    // Given
    var innerWriter = mock(Writer.class);
    var writer = new TeeWriter(innerWriter);

    // When
    writer.close();

    // Then
    verify(innerWriter).close();
    verifyNoMoreInteractions(innerWriter);
  }

  @DisplayName(".flush() flushes the underlying writer")
  @Test
  void flushFlushesWriter() throws IOException {
    // Given
    var innerWriter = mock(Writer.class);
    var writer = new TeeWriter(innerWriter);

    // When
    writer.flush();

    // Then
    verify(innerWriter).flush();
    verifyNoMoreInteractions(innerWriter);
  }
}
