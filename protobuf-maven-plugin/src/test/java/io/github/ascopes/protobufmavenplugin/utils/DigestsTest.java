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

package io.github.ascopes.protobufmavenplugin.utils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockStatic;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Random;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;


/**
 * @author Ashley Scopes
 */
@DisplayName("Digests tests")
class DigestsTest {

  @DisplayName(
      ".sha1(String) returns the expected SHA-1 digest in an un-padded url-safe base64 string"
  )
  @CsvSource({
      "           foobarbaz, X1UT-IIv2-UUWvM7ZNjZcNz5XG4",
      "      /src/main/java, R4yHFYpaVvKy0Ckbl9gOpNRrw4I",
      "            ./protoc, NZCHAZ9FTJa3iMDRcYSq0_lc1kw",
      // Tests that the output is urlsafe. Test case was generated via bruteforce
      // across random ascii strings until a match containing both - and _ was
      // found.
      "EsWoyIWuIcpMltIOJJAv, zYwfI-X_k4pk__DriohLNCpAHbU",
  })
  @ParameterizedTest(name = ".sha1(\"{0}\") returns \"{1}\"")
  void sha1ReturnsExpectedSha1DigestInUnPaddedUrlSafeBase64String(String input, String expected) {
    // When
    var actual = Digests.sha1(input);
    // Then
    assertThat(actual).isEqualTo(expected);
  }

  @DisplayName(".sha1(String) raises an IllegalArgumentException if unsupported")
  @Test
  void sha1RaisesAnIllegalArgumentExceptionIfUnsupported() {
    try (var mockMessageDigestCls = mockStatic(MessageDigest.class)) {
      // Given
      mockMessageDigestCls.when(() -> MessageDigest.getInstance(any()))
          .thenThrow(new NoSuchAlgorithmException("that doesn't exist!"));

      // Then
      assertThatExceptionOfType(IllegalArgumentException.class)
          .isThrownBy(() -> Digests.sha1("foobar"));
    }
  }

  @DisplayName(".sha512ForStream(InputStream) returns the expected result")
  @Test
  void sha512ForStreamReturnsTheExpectedResult() throws Exception {
    // Given
    var data = randomBytes(8000);
    var expectedRawDigest = MessageDigest.getInstance("SHA-512").digest(data);
    var expectedDigest = Base64.getUrlEncoder().withoutPadding()
        .encodeToString(expectedRawDigest);

    // When
    var inputStream = new ByteArrayInputStream(data);
    var actualDigest = Digests.sha512ForStream(inputStream);

    // Then
    assertThat(actualDigest).isEqualTo(expectedDigest);
  }

  @DisplayName(".sha512ForStream(InputStream) raises an IllegalArgumentException if unsupported")
  @Test
  void sha512ForStreamRaisesAnIllegalArgumentExceptionIfUnsupported() throws Exception {
    try (var mockMessageDigestCls = mockStatic(MessageDigest.class)) {
      // Given
      mockMessageDigestCls.when(() -> MessageDigest.getInstance(any()))
          .thenThrow(new NoSuchAlgorithmException("that doesn't exist!"));
      var stream = new ByteArrayInputStream(randomBytes(128));

      // Then
      assertThatExceptionOfType(IllegalArgumentException.class)
          .isThrownBy(() -> Digests.sha512ForStream(stream));
    }
  }

  static byte[] randomBytes(int size) {
    var data = new byte[size];
    new Random().nextBytes(data);
    return data;
  }
}
