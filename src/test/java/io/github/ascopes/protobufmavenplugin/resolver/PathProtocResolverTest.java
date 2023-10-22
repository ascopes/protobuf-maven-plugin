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

package io.github.ascopes.protobufmavenplugin.resolver;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import io.github.ascopes.protobufmavenplugin.platform.HostEnvironment;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("PathProtocResolver tests")
@ExtendWith(MockitoExtension.class)
class PathProtocResolverTest {

  static {
    // Call one of the methods to ensure classloading has completed prior to mocking taking place.
    // See https://github.com/mockito/mockito/issues/3156.
    HostEnvironment.isLinux();
  }

  @Mock
  MockedStatic<HostEnvironment> platformMock;

  PathProtocResolver resolver;

  @BeforeEach
  void setUp() {
    resolver = new PathProtocResolver();
  }

  @DisplayName("An empty $PATH results in a resolution exception being raised")
  @Test
  void emptyPathThrowsResolutionException() {
    // Given
    platformMock.when(HostEnvironment::systemPath).thenReturn(List.of());

    // Then
    assertThatThrownBy(resolver::resolveProtoc)
        .isInstanceOf(ProtocResolutionException.class)
        .hasNoCause()
        .hasMessage("No protoc binary was found in the $PATH");
  }


}
