/*
 * Copyright (C) 2023 Ashley Scopes
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
package io.github.ascopes.protobufmavenplugin.fixtures;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import io.github.ascopes.protobufmavenplugin.utils.HostSystem;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.mockito.quality.Strictness;

/**
 * Fixtures for configuring host system mocks consistently.
 *
 * @author Ashley Scopes
 */
public final class HostSystemFixtures {

  public static HostSystem hostSystem() {
    return mock(withSettings().strictness(Strictness.LENIENT));
  }

  public static HostSystemMockConfigurer arch(String arch) {
    return namedConfigurer(arch, hs -> when(hs.getCpuArchitecture()).thenReturn(arch));
  }

  public static HostSystemMockConfigurer linux() {
    return namedConfigurer("Linux", hs -> {
      when(hs.isProbablyLinux()).thenReturn(true);
      when(hs.isProbablyMacOs()).thenReturn(false);
      when(hs.isProbablyWindows()).thenReturn(false);
    });
  }

  public static HostSystemMockConfigurer macOs() {
    return namedConfigurer("Mac OS", hs -> {
      when(hs.isProbablyLinux()).thenReturn(false);
      when(hs.isProbablyMacOs()).thenReturn(true);
      when(hs.isProbablyWindows()).thenReturn(false);
    });
  }

  public static HostSystemMockConfigurer windows() {
    return namedConfigurer("Windows", hs -> {
      when(hs.isProbablyLinux()).thenReturn(false);
      when(hs.isProbablyMacOs()).thenReturn(false);
      when(hs.isProbablyWindows()).thenReturn(true);
    });
  }

  public static HostSystemMockConfigurer otherOs() {
    return namedConfigurer("an unknown OS", hs -> {
      when(hs.isProbablyLinux()).thenReturn(false);
      when(hs.isProbablyMacOs()).thenReturn(false);
      when(hs.isProbablyWindows()).thenReturn(false);
    });
  }

  public static HostSystemMockConfigurer path(Path... directories) {
    var directoriesList = List.of(directories);

    return namedConfigurer(
        "Path containing " + Arrays.deepToString(directories),
        hs -> when(hs.getSystemPath()).thenReturn(directoriesList)
    );
  }

  public static HostSystemMockConfigurer pathExtensions(String... extensions) {
    var extensionSet = Stream.of(extensions)
        .collect(Collectors.collectingAndThen(
            Collectors.toCollection(() -> new TreeSet<>(String::compareToIgnoreCase)),
            Collections::unmodifiableNavigableSet
        ));

    return namedConfigurer(
        "Path extensions set to " + Arrays.deepToString(extensions),
        hs -> when(hs.getSystemPathExtensions()).thenReturn(extensionSet)
    );
  }


  @FunctionalInterface
  public interface HostSystemMockConfigurer {

    void configure(HostSystem mock);

    default HostSystemMockConfigurer and(HostSystemMockConfigurer second) {
      return namedConfigurer(this + " and " + second, hs -> {
        configure(hs);
        second.configure(hs);
      });
    }
  }

  private static HostSystemMockConfigurer namedConfigurer(
      String description,
      HostSystemMockConfigurer configurer
  ) {
    return new HostSystemMockConfigurer() {
      @Override
      public void configure(HostSystem mock) {
        configurer.configure(mock);
      }

      @Override
      public String toString() {
        return description;
      }
    };
  }

  private HostSystemFixtures() {
    // Static-only class.
  }
}
