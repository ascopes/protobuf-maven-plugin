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
package io.github.ascopes.protobufmavenplugin.protoc.distributions;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("ProtocDistribution tests")
class ProtocDistributionTest {

  @DisplayName("fromString returns the expected value")
  @MethodSource("fromStringCases")
  @ParameterizedTest(name = "{argumentSetName}")
  void fromStringReturnsTheExpectedValue(String input, ProtocDistribution expected) {
    // When
    var actual = ProtocDistribution.fromString(input);

    // Then
    assertThat(actual).isEqualTo(expected);
  }

  static Stream<Arguments> fromStringCases() {
    return Stream.of(
        Arguments.argumentSet(
            "version number",
            "4.29.0",
            ImmutableBinaryMavenProtocDistribution.builder()
                .version("4.29.0")
                .build()
        ),
        Arguments.argumentSet(
            "\"PATH\"",
            "PATH",
            ImmutablePathProtocDistribution.builder()
                .name("protoc")
                .build()
        ),
        Arguments.argumentSet(
            "\"path\"",
            "path",
            ImmutablePathProtocDistribution.builder()
                .name("protoc")
                .build()
        ),
        Arguments.argumentSet(
            "file URI",
            "file:///home/me/.m2/repository/com/google/protobuf/protoc/protoc-4.29.0-x86_64.exe",
            ImmutableUriProtocDistribution.builder()
                .url(URI.create(
                    "file:///home/me/.m2/repository/com/google/protobuf/protoc/"
                        + "protoc-4.29.0-x86_64.exe"
                ))
                .build()
        ),
        Arguments.argumentSet(
            "http uri",
            "https://some-server.net/binaries/protoc/4.29.0/linux/amd64/protoc.exe",
            ImmutableUriProtocDistribution.builder()
                .url(URI.create(
                    "https://some-server.net/binaries/protoc/4.29.0/linux/amd64/protoc.exe"
                ))
                .build()
        )
    );
  }
}
