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
