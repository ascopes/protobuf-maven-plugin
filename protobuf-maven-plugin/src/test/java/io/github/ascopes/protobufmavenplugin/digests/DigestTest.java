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
package io.github.ascopes.protobufmavenplugin.digests;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.jupiter.params.provider.Arguments.argumentSet;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.ByteArrayInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.stream.Stream;
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
    var sameDigest = new Digest("foo", bytes(1, 2, 3, 4));

    return Stream.of(
        argumentSet(
            "not equal to null",
            new Digest("foo", bytes(1, 2, 3, 4)),
            null,
            false
        ),
        argumentSet(
            "not equal to different types of object",
            new Digest("foo", bytes(1, 2, 3, 4)),
            "some string",
            false
        ),
        argumentSet(
            "not equal if algorithms differ",
            new Digest("foo", bytes(1, 2, 3, 4)),
            new Digest("bar", bytes(1, 2, 3, 4)),
            false
        ),
        argumentSet(
            "not equal if buffers differ",
            new Digest("foo", bytes(1, 2, 3, 4)),
            new Digest("foo", bytes(5, 4, 3, 2)),
            false
        ),
        argumentSet(
            "equal if algorithms and buffers match",
            new Digest("foo", bytes(1, 2, 3, 4)),
            new Digest("foo", bytes(1, 2, 3, 4)),
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

  @DisplayName(".hashCode() returns a unique value")
  @Test
  void hashCodeReturnsUniqueValue() {
    // Given
    var foo = new Digest("aaa", bytes(1, 2, 3));
    var bar = new Digest("bbb", bytes(1, 2, 3));
    var baz = new Digest("aaa", bytes(5, 4, 3));

    // Then
    assertSoftly(softly -> {
      softly.assertThat(foo).hasSameHashCodeAs(foo);
      softly.assertThat(bar).hasSameHashCodeAs(bar);
      softly.assertThat(baz).hasSameHashCodeAs(baz);
      softly.assertThat(foo).doesNotHaveSameHashCodeAs(bar);
      softly.assertThat(foo).doesNotHaveSameHashCodeAs(baz);
      softly.assertThat(bar).doesNotHaveSameHashCodeAs(baz);
    });
  }

  @DisplayName(".toString() returns the expexted value")
  @Test
  void toStringReturnsTheExpectedValue() {
    // Given
    var digest = new Digest("SHA-940", bytes(0xDE, 0xAD, 0xBE, 0xEF, 0x69, 0x42, 0x0, 0x1, 0x2));

    // Then
    assertThat(digest).hasToString("SHA-940:deadbeef6942000102");
  }

  @DisplayName(".toHexString() returns the expexted value")
  @Test
  void toHexStringReturnsTheExpectedValue() {
    // Given
    var digest = new Digest("SHA-940", bytes(0xDE, 0xAD, 0xBE, 0xEF, 0x69, 0x42, 0x0, 0x1, 0x2));

    // Then
    assertThat(digest.toHexString()).isEqualTo("deadbeef6942000102");
  }

  @DisplayName(".verify(InputStream) succeeds if the digest matches the content")
  @Test
  void verifySucceedsIfTheDigestMatchesTheContent() throws Throwable {
    // Given
    var stream = new ByteArrayInputStream("hello, world".getBytes());
    var digest = Digest.from(
        "SHA-256",
        "09ca7e4eaa6e8ae9c7d261167129184883644d07dfba7cbfbc4c8a2e08360d5b"
    );

    // Then
    assertThatNoException()
        .isThrownBy(() -> digest.verify(stream));
  }

  @DisplayName(".verify(InputStream) raises if the digest does not match the content")
  @Test
  void verifyRaisesIfTheDigestDoesNotMatchTheContent() throws Throwable {
    // Given
    var stream = new ByteArrayInputStream("goodbye, world".getBytes());
    var digest = Digest.from(
        "SHA-256",
        "09ca7e4eaa6e8ae9c7d261167129184883644d07dfba7cbfbc4c8a2e08360d5b"
    );

    // Then
    assertThatExceptionOfType(DigestException.class)
        .isThrownBy(() -> digest.verify(stream))
        .withMessage(
            "Actual digest"
                + " 'SHA-256:41d8dfe07cfa2111d75a6b318c4e35e2f6bab9daf0d5b0748acb162bb909fa04'"
                + " does not match expected digest"
                + " 'SHA-256:09ca7e4eaa6e8ae9c7d261167129184883644d07dfba7cbfbc4c8a2e08360d5b'"
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

  @DisplayName(".compute(String, String) computes the digest for the string")
  @Test
  void computeStringStringComputesTheDigestForTheString() {
    // Given
    var expectedDigest = new Digest(
        "MD5",
        bytes(
            0x43, 0xdf, 0x48, 0x0e, 0x18, 0xe4, 0x6f, 0xd8, 0x6c, 0x1b, 0xb8, 0x35,
            0x23, 0xe3, 0xfe, 0x8f
        )
    );

    // When
    var actualDigest = Digest.compute("MD5", "you are already dead");

    // Then
    assertThat(actualDigest).isEqualTo(expectedDigest);
  }

  @DisplayName(".compute(String, byte[]) computes the digest for the bytes")
  @Test
  void computeStringBytesComputesTheDigestForTheBytes() {
    // Given
    var expectedDigest = new Digest(
        "MD5",
        bytes(
            0x43, 0xdf, 0x48, 0x0e, 0x18, 0xe4, 0x6f, 0xd8, 0x6c, 0x1b, 0xb8, 0x35,
            0x23, 0xe3, 0xfe, 0x8f
        )
    );

    // When
    var actualDigest = Digest.compute(
        "MD5",
        bytes(
            'y', 'o', 'u', ' ', 'a', 'r', 'e', ' ', 'a', 'l', 'r', 'e', 'a', 'd', 'y',
            ' ', 'd', 'e', 'a', 'd'
        )
    );

    // Then
    assertThat(actualDigest).isEqualTo(expectedDigest);
  }


  @DisplayName(".compute(String, InputStream) computes the digest for the stream")
  @Test
  void computeStringInputStreamComputesTheDigestForTheStream() throws Throwable {
    // Given
    var expectedDigest = new Digest(
        "MD5",
        bytes(
            0x43, 0xdf, 0x48, 0x0e, 0x18, 0xe4, 0x6f, 0xd8, 0x6c, 0x1b, 0xb8, 0x35,
            0x23, 0xe3, 0xfe, 0x8f
        )
    );

    // When
    var actualDigest = Digest.compute(
        "MD5",
        new ByteArrayInputStream(bytes(
            'y', 'o', 'u', ' ', 'a', 'r', 'e', ' ', 'a', 'l', 'r', 'e', 'a', 'd', 'y',
            ' ', 'd', 'e', 'a', 'd'
        ))
    );

    // Then
    assertThat(actualDigest).isEqualTo(expectedDigest);
  }


  static byte[] getDigestOf(String algorithm, String data) throws Throwable {
    return MessageDigest.getInstance(algorithm).digest(data.getBytes());
  }

  static byte[] bytes(int... ints) {
    var bytes = new byte[ints.length];
    for (var i = 0; i < ints.length; ++i) {
      bytes[i] = (byte) ints[i];
    }
    return bytes;
  }
}
