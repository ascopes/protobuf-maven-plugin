/*
 * Copyright (C) 2023, Ashley Scopes.
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
package io.github.ascopes.protobufmavenplugin.resolve;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.ascopes.protobufmavenplugin.fixtures.TestFileSystem;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

@DisplayName("ProtoFileResolver tests")
class ProtoFileResolverTest {
  TestFileSystem fs;

  @BeforeEach
  void setUp() {
    fs = TestFileSystem.linux();
  }

  @AfterEach
  void tearDown() {
    fs.close();
  }

  @DisplayName("Proto files are returned")
  @Test
  void protoFilesAreReturned() throws IOException {
    // Given
    var root1 = fs.givenDirectoryExists("foo");
    var root2 = fs.givenDirectoryExists("spam");
    var root3 = fs.givenDirectoryExists("stuff");
    var file1 = fs.givenFileExists(root1, "bar", "baz", "user.proto");
    var file2 = fs.givenFileExists(root1, "bar", "baz", "message.proto");
    var file3 = fs.givenFileExists(root1, "bar", "baz", "channel.PrOTo");
    fs.givenFileExists(root1, "bar", "baz", "something.i.dont.care.about");
    var file4 = fs.givenFileExists(root2, "deathray.proto");
    fs.givenFileExists(root2, "literally-nothing");
    fs.givenFileExists(root3, "proto.this-isnt-valid");
    fs.givenDirectoryExists(root3, ".proto");
    fs.givenFileExists(root3, "meh", ".proto", "whatever");
    var roots = List.of(root1, root2, root3);
    var resolver = new ProtoFileResolver();

    // When
    var resolved = resolver.findProtoFiles(roots);

    // Then
    assertThat(resolved).containsExactlyInAnyOrder(file1, file2, file3, file4);
  }
}
