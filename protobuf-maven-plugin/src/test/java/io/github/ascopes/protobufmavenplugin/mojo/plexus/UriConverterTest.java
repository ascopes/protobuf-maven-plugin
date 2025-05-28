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
package io.github.ascopes.protobufmavenplugin.mojo.plexus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import io.github.ascopes.protobufmavenplugin.mojo.plexus.PathConverterTest.SomeDirectoryRelativeExpressionEvaluator;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.converters.lookup.DefaultConverterLookup;
import org.codehaus.plexus.component.configurator.expression.DefaultExpressionEvaluator;
import org.codehaus.plexus.configuration.DefaultPlexusConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@DisplayName("UriConverter test")
class UriConverterTest {

  UriConverter converter;

  @BeforeEach
  void setUp() {
    converter = new UriConverter();
  }

  @DisplayName("only the expected types are convertible")
  @CsvSource({
      "           java.net.URI,  true",
      "     java.nio.file.Path, false",
      "       java.lang.Object, false",
      "      java.lang.Integer, false",
      "        java.lang.Class, false",
      "       java.lang.String, false",
      "java.lang.StringBuilder, false",
      "           java.io.File, false",
      "           java.net.URL, false",
  })
  @ParameterizedTest(name = "for {0}, expect {1}")
  void onlyTheExpectedTypesAreConvertible(Class<?> type, boolean expectedResult) {
    // Then
    assertThat(converter.canConvert(type))
        .isEqualTo(expectedResult);
  }

  @DisplayName("URIs can be parsed successfully")
  @Test
  void urisCanBeParsedSuccessfully() throws ComponentConfigurationException {
    // Given
    var uri = URI.create("https://google.com");
    var converterLookup = new DefaultConverterLookup();
    var configuration = new DefaultPlexusConfiguration("uri", uri.toString());
    var evaluator = new DefaultExpressionEvaluator();

    // When
    var result = converter.fromConfiguration(
        converterLookup,
        configuration,
        URI.class,
        null,
        getClass().getClassLoader(),
        evaluator
    );

    // Then
    assertThat(converter.fromString(uri.toString()))
        .isEqualTo(uri);
  }

  @DisplayName("Invalid URIs raise an exception during parsing")
  @Test
  void invalidUrisRaiseAnExceptionDuringParsing() {
    // Given
    var converterLookup = new DefaultConverterLookup();
    var configuration = new DefaultPlexusConfiguration("uri", "foo\\bar");
    var evaluator = new DefaultExpressionEvaluator();

    // Then
    assertThatExceptionOfType(ComponentConfigurationException.class)
        .isThrownBy(() -> converter.fromConfiguration(
            converterLookup,
            configuration,
            URI.class,
            null,
            getClass().getClassLoader(),
            evaluator,
            null
        ))
        // Ignore the message here, it varies based on the platform the test is running.
        // Windows produces a different issue since backslashes have a special meaning there.
        .havingCause()
        .isInstanceOf(URISyntaxException.class);
  }

  @DisplayName("Null values are returned directly")
  @Test
  void nullValuesAreReturnedDirectly() throws ComponentConfigurationException {
    // Given
    var converterLookup = new DefaultConverterLookup();
    var configuration = new DefaultPlexusConfiguration("uri", null);
    var evaluator = new DefaultExpressionEvaluator();

    // When
    var result = converter.fromConfiguration(
        converterLookup,
        configuration,
        URI.class,
        null,
        getClass().getClassLoader(),
        evaluator
    );

    // Then
    assertThat(result).isNull();
  }
}
