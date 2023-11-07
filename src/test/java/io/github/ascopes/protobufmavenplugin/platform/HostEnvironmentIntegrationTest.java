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

package io.github.ascopes.protobufmavenplugin.platform;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assumptions.assumeThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

import java.io.File;
import java.nio.file.Path;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("HostEnvironment integration tests")
class HostEnvironmentIntegrationTest {

  @DisplayName(".systemPath() returns the expected system path")
  @Test
  void systemPathReturnsTheExpectedSystemPath() {
    // Given
    var paths = Stream.of(System.getenv("PATH"))
        .map(path -> path.split(Pattern.quote(File.pathSeparator)))
        .flatMap(Stream::of)
        .map(Path::of)
        .collect(Collectors.toUnmodifiableList());

    // Then
    assertThat(HostEnvironment.systemPath())
        .isEqualTo(paths);
  }

  @DisplayName(".systemPathExtensions() returns the expected system path extensions")
  @Test
  void systemPathExtensionsReturnsTheExpectedSystemPathExtensions() {
    assumeThat(HostEnvironment.isWindows())
        .isTrue();

    // Given
    var pathExtensions = Stream.of(System.getenv("PATHEXT"))
        .map(path -> path.split(Pattern.quote(File.pathSeparator)))
        .flatMap(Stream::of)
        .collect(Collectors.toList());

    // Then
    assertSoftly(softly -> {
      var actualPathExtensions = HostEnvironment.systemPathExtensions();

      for (var pathExtension : pathExtensions) {
        softly.assertThat(actualPathExtensions)
            .contains(pathExtension.toLowerCase(Locale.ROOT));
        softly.assertThat(actualPathExtensions)
            .contains(pathExtension.toUpperCase(Locale.ROOT));
        softly.assertThat(actualPathExtensions)
            .contains(spongebob(pathExtension));
      }
    });
  }

  String spongebob(String text) {
    var rand = new Random();
    var sb = new StringBuilder();

    for (var i = 0; i < text.length(); ++i) {
      var c = text.charAt(i);
      sb.append(rand.nextBoolean()
          ? Character.isUpperCase(c)
          : Character.toLowerCase(c));
    }

    return sb.toString();
  }

}
