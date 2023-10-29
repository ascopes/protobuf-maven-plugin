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

package io.github.ascopes.protobufmavenplugin.fixture;

import io.github.ascopes.protobufmavenplugin.fixture.MockedSystemProperties.MockedSystemPropertiesExtension;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Properties;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.parallel.Isolated;

/**
 * Annotation that marks a test class as modifying system properties.
 *
 * <p>The system properties are emptied before each test and are added back
 * after each execution in a thread-safe way.
 *
 * @author Ashley Scopes
 */
@ExtendWith(MockedSystemPropertiesExtension.class)
@Isolated("Mocks system properties")
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface MockedSystemProperties {

  final class MockedSystemPropertiesExtension implements BeforeEachCallback, AfterEachCallback {

    private Properties initialProperties;

    @Override
    public void beforeEach(ExtensionContext extensionContext) {
      initialProperties = new Properties();
      // The copy-constructor does not actually copy anything, which will break other tests, so
      // we have to do it this way.
      initialProperties.putAll(System.getProperties());
      System.getProperties().clear();

      // Prevent issues with the JRE reading os.* and java.* properties which may mess up things
      // like Mocktio initialisation.
      initialProperties.stringPropertyNames()
          .stream()
          .filter(key -> key.matches("^(os\\.|java\\.|file\\.|path\\.|user\\.).+"))
          .forEach(key -> System.setProperty(key, initialProperties.getProperty(key)));
    }

    @Override
    public void afterEach(ExtensionContext extensionContext) {
      System.getProperties().clear();
      System.getProperties().putAll(initialProperties);
    }
  }
}
