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

import java.nio.file.Path;
import org.apache.commons.lang3.SystemProperties;
import org.apache.commons.lang3.SystemUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.parallel.Isolated;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@DisplayName("PathProtocResolver tests")
@ExtendWith(MockitoExtension.class)
@Isolated("Modifies system environment variables")
class PathProtocResolverTest {

  static {
    // Works around a bug in Mockito that breaks classloading due to precedence with
    // lazily loading class references: https://github.com/mockito/mockito/issues/3156.
    SystemUtils.getEnvironmentVariable("foo", "bar");
  }

  @Mock(answer = Answers.RETURNS_SMART_NULLS)
  MockedStatic<SystemProperties> systemPropertiesMock;

  @Mock(answer = Answers.RETURNS_SMART_NULLS)
  MockedStatic<SystemUtils> systemUtilsMock;

  @TempDir
  Path tempDir;

  PathProtocResolver resolver;

  @BeforeEach
  void setUp() {
    resolver = new PathProtocResolver();
  }

  @Test
  void undefinedPathThrowsException() {
    // Given
    systemUtilsMock.when(() -> SystemUtils.getEnvironmentVariable("PATH", ""))
        .thenReturn("");

    assertThatThrownBy(resolver::resolveProtoc)
        .isInstanceOf(ProtocResolutionException.class)
        .hasNoCause()
        .hasMessage("No protoc binary was found in the $PATH");
  }


}
