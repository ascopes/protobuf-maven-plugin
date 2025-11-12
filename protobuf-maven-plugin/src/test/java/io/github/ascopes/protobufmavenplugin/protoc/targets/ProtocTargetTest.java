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
package io.github.ascopes.protobufmavenplugin.protoc.targets;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ProtocTarget tests")
class ProtocTargetTest {

  @DisplayName("the default order is 0")
  @Test
  void defaultOrderIsZero() {
    // Given
    var plugin = new ProtocTargetImpl();

    // Then
    assertThat(plugin.getOrder()).isZero();
  }

  @DisplayName("targets with lower orders come before targets with higher orders")
  @Test
  void targetsWithLowerOrdersComeBeforeTargetsWithHigherOrders() {
    // Then
    assertThat(new OrderedProtocTargetImpl(100, "aaaa"))
        .isLessThan(new OrderedProtocTargetImpl(200, "aaaa"));
    assertThat(new OrderedProtocTargetImpl(-200, "aaaa"))
        .isLessThan(new OrderedProtocTargetImpl(-100, "aaaa"));
  }

  @DisplayName("targets with equal orders are compared by their .toString")
  @Test
  void targetsWithEqualOrdersAreComparedByTheirToString() {
    // Then
    assertThat(new OrderedProtocTargetImpl(100, "aaaa"))
        .isLessThan(new OrderedProtocTargetImpl(100, "bbbb"));
    assertThat(new OrderedProtocTargetImpl(100, "bbbb"))
        .isGreaterThan(new OrderedProtocTargetImpl(100, "aaaa"));
    assertThat(new OrderedProtocTargetImpl(100, "aaaa"))
        .isEqualByComparingTo(new OrderedProtocTargetImpl(100, "aaaa"));
  }

  private static final class ProtocTargetImpl implements ProtocTarget {
    // Nothing here.
  }

  private static final class OrderedProtocTargetImpl implements ProtocTarget {
    private final int order;
    private final String toString;

    private OrderedProtocTargetImpl(int order, String toString) {
      this.order = order;
      this.toString = toString;
    }

    @Override
    public int getOrder() {
      return order;
    }

    @Override
    public String toString() {
      return toString;
    }
  }
}
