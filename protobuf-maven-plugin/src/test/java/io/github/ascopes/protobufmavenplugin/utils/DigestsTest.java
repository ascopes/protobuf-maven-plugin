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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;


/**
 * @author Ashley Scopes
 */
@DisplayName("Digests tests")
class DigestsTest {

  @DisplayName("sha1(String) returns the expected unpadded url-safe base64 string")
  @CsvSource({
      "           foobarbaz, X1UT-IIv2-UUWvM7ZNjZcNz5XG4",
      "      /src/main/java, R4yHFYpaVvKy0Ckbl9gOpNRrw4I",
      "            ./protoc, NZCHAZ9FTJa3iMDRcYSq0_lc1kw",
      // Tests that the output is urlsafe. Test case was generated via bruteforce
      // across random ascii strings until a match containing both - and _ was
      // found.
      "EsWoyIWuIcpMltIOJJAv, zYwfI-X_k4pk__DriohLNCpAHbU",
  })
  @ParameterizedTest(name = "sha1(\"{0}\") returns \"{1}\"")
  void sha1ReturnsExpectedUnpaddedUrlSafeBase64String(String input, String expected) {
    // When
    var actual = Digests.sha1(input);
    // Then
    assertThat(actual).isEqualTo(expected);
  }

  @DisplayName("sha1(String) raises an IllegalArgumentException on error")
  @Test
  void sha1RaisesIllegalArgumentExceptionOnError() {
    // Then
    assertThatThrownBy(() -> Digests.sha1(null))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
