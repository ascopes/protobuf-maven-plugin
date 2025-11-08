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
package io.github.ascopes.protobufmavenplugin.generation;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.EnumSource.Mode;

@DisplayName("GenerationResult tests")
class GenerationResultTest {

  @DisplayName("uccessful results are marked as being 'OK'")
  @EnumSource(
      value = GenerationResult.class,
      mode = Mode.INCLUDE,
      names = {"PROTOC_SUCCEEDED", "NOTHING_TO_DO"}
  )
  @ParameterizedTest(name = "for \"{0}\"")
  void successfulResultsAreMarkedAsBeingOk(GenerationResult result) {
    // Then
    assertThat(result.isOk()).isTrue();
  }

  @DisplayName("un-successful results are marked as being not 'OK'")
  @EnumSource(
      value = GenerationResult.class,
      mode = Mode.EXCLUDE,
      names = {"PROTOC_SUCCEEDED", "NOTHING_TO_DO"}
  )
  @ParameterizedTest(name = "for \"{0}\"")
  void unsuccessfulResultsAreMarkedAsBeingNotOk(GenerationResult result) {
    // Then
    assertThat(result.isOk()).isFalse();
  }
}
