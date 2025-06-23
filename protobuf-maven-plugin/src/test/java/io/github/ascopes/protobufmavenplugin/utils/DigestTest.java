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
package io.github.ascopes.protobufmavenplugin.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("Digest tests")
class DigestTest {
  @DisplayName(".equals returns true if two digests match")
  @MethodSource("equalsTestCases")
  @ParameterizedTest(name = "{argumentSetName}")
  void equalsReturnsTrueIfTwoDigestsMatch(
      Digest first,
      Object second,
      boolean expectEqual
  ) {
    // Then
    if (expectEqual) {
      assertThat(first).isEqualTo(second);
    } else {
      assertThat(first).isNotEqualTo(second);
    }
  }

  static Stream<Arguments> equalsTestCases() {
    var sameDigest = new Digest("foo", new byte[]{1, 2, 3, 4});

    return Stream.of(
        argumentSet(
            "not equal to null",
            new Digest("foo", new byte[]{1, 2, 3, 4}),
            null,
            false
        ),
        argumentSet(
            "not equal to different types of object",
            new Digest("foo", new byte[]{1, 2, 3, 4}),
            "some string",
            false
        ),
        argumentSet(
            "not equal if algorithms differ",
            new Digest("foo", new byte[]{1, 2, 3, 4}),
            new Digest("bar", new byte[]{1, 2, 3, 4}),
            false
        ),
        argumentSet(
            "not equal if buffers differ",
            new Digest("foo", new byte[]{1, 2, 3, 4}),
            new Digest("foo", new byte[]{5, 4, 3, 2}),
            false
        ),
        argumentSet(
            "equal if algorithms and buffers match",
            new Digest("foo", new byte[]{1, 2, 3, 4}),
            new Digest("foo", new byte[]{1, 2, 3, 4}),
            true
        ),
        argumentSet(
            "equal if the same instance of object",
            sameDigest,
            sameDigest,
            true
        )
    );
  }

  @DisplayName(".from(String, String) builds the expected digest object")
  @MethodSource("fromValidTestCases")
  @ParameterizedTest(name = "for {0} digest")
  void fromBuildsTheExpectedDigestObject(
      String algorithm,
      String hex,
      byte[] expectedDigest
  ) {
    // When
    var actualDigest = Digest.from(algorithm, hex);

    // Then
    assertThat(actualDigest.getDigest()).isEqualTo(expectedDigest);
  }

  static Stream<Arguments> fromValidTestCases() throws Throwable {
    return Stream.of(
        arguments(
            "MD2",
            "65d39bdcda41f0aeb232d7d90e58bb6c",
            getDigestOf("MD2", "hello, world!")
        ),
        arguments(
            "MD5",
            "3adbbad1791fbae3ec908894c4963870",
            getDigestOf("MD5", "hello, world!")
        ),
        arguments(
            "SHA-1",
            "1f09d30c707d53f3d16c530dd73d70a6ce7596a9",
            getDigestOf("SHA-1", "hello, world!")
        ),
        arguments(
            "SHA-224",
            "71abb44f43f76b938a35d06a541eb6c670210ccfd2baf2aa1627fee3",
            getDigestOf("SHA-224", "hello, world!")
        ),
        arguments(
            "SHA-256",
            "68e656b251e67e8358bef8483ab0d51c6619f3e7a1a9f0e75838d41ff368f728",
            getDigestOf("SHA-256", "hello, world!")
        ),
        arguments(
            "SHA-384",
            "6f9f238425eca2439ed4581ac1fdb45fc76379e7fba94bc0a7624fa3e7ab1ec3701b4bfcdda376ca7551"
                + "92e6f45f2a4e",
            getDigestOf("SHA-384", "hello, world!")
        ),
        arguments(
            "SHA-512",
            "6c2618358da07c830b88c5af8c3535080e8e603c88b891028a259ccdb9ac802d0fc0170c99d58affcf00"
                + "786ce188fc5d753e8c6628af2071c3270d50445c4b1c",
            getDigestOf("SHA-512", "hello, world!")
        )
    );
  }

  @DisplayName(".from(String, String) raises if the hex string is missing digits")
  @Test
  void fromRaisesIfTheHexStringIsMissingDigits() {
    // Then
    assertThatExceptionOfType(DigestException.class)
        // Has an odd number of characters, so cannot be parsed
        // as a set of bytes represented as hex digits, since we need
        // two digits per byte.
        .isThrownBy(() -> Digest.from("MD5", "0123456"))
        .withMessage("Invalid hex byte at position 7 in hexadecimal string '0123456'")
        .withCauseInstanceOf(IndexOutOfBoundsException.class);
  }

  @DisplayName(".from(String, String) raises if the hex string contains invalid digits")
  @Test
  void fromRaisesIfTheHexStringContainsInvalidHexDigits() {
    // Then
    assertThatExceptionOfType(DigestException.class)
        .isThrownBy(() -> Digest.from("MD5", "012345zz"))
        .withMessage("Invalid hex byte at position 7 in hexadecimal string '012345zz'")
        .withCauseInstanceOf(NumberFormatException.class);
  }

  @DisplayName(".from(String, String) raises if the algorithm does not exist")
  @Test
  void fromRaisesIfTheAlgorithmDoesNotExist() {
    // Then
    assertThatExceptionOfType(DigestException.class)
        .isThrownBy(() -> Digest.from("ashley", "0123"))
        .withMessage("Digest 'ashley' is not supported by this JVM")
        .withCauseInstanceOf(NoSuchAlgorithmException.class);
  }

  @DisplayName(".from(String, String) raises if the digest length is invalid for the algorithm")
  @Test
  void fromRaisesIfTheDigestLengthIsInvalidForTheAlgorithm() {
    // Then
    assertThatExceptionOfType(DigestException.class)
        .isThrownBy(() -> Digest.from("MD5", "deadbeef"))
        .withMessage(
            "Illegal length 4 for decoded digest 'deadbeef' with algorithm 'MD5', expected 16"
        )
        .withNoCause();
  }

  static byte[] getDigestOf(String algorithm, String data) throws Throwable {
    return MessageDigest.getInstance(algorithm).digest(data.getBytes());
  }
}
