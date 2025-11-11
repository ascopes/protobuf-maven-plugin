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
package io.github.ascopes.protobufmavenplugin.fixtures;

import static java.util.Objects.requireNonNull;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Properties;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.parallel.Isolated;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Annotation for JUnit5 which allows a test to modify system properties by
 * temporarily replacing them with a new map.
 *
 * <p>System properties are restored at the end of each test.
 *
 * <p>All test instances are marked as isolated due to the modification of the
 * system properties global reference.
 *
 * @author Ashley Scopes
 */
@ExtendWith(UsesSystemProperties.UsesSystemPropertiesExtension.class)
@Inherited
@Isolated("modifies system properties singleton")
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface UsesSystemProperties {

  final class UsesSystemPropertiesExtension
      implements BeforeEachCallback, AfterEachCallback {

    private final Logger log = LoggerFactory.getLogger(UsesSystemProperties.class);
    private @Nullable Properties originalProperties;

    UsesSystemPropertiesExtension() {
      originalProperties = null;
    }

    @Override
    public void beforeEach(ExtensionContext extensionContext) {
      log.debug(
          "Replacing system properties with empty map for duration of test in {}",
          extensionContext.getTestMethod()
      );
      originalProperties = System.getProperties();
      System.setProperties(new Properties());
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) {
      System.setProperties(requireNonNull(originalProperties));
      log.debug(
          "Restored original system properties since completion of test in {}",
          extensionContext.getTestMethod()
      );
    }
  }
}
