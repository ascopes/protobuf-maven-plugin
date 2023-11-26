package io.github.ascopes.protobufmavenplugin.resolve.protoc;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("PathProtocResolver tests")
class PathProtocResolverTest {

  PathProtocResolver resolver;

  @BeforeEach
  void setUp() {
    resolver = new PathProtocResolver();
  }

  @DisplayName("The expected binary name is used")
  @Test
  void expectedBinaryNameIsUsed() {
    // Then
    assertThat(resolver.binaryName()).isEqualTo("protoc");
  }
}
