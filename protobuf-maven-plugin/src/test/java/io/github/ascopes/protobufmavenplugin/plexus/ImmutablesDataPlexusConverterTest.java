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
package io.github.ascopes.protobufmavenplugin.plexus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.params.provider.Arguments.arguments;

import io.github.ascopes.protobufmavenplugin.plexus.testdata.ImmutableValidInnerModel;
import io.github.ascopes.protobufmavenplugin.plexus.testdata.ImmutableValidOuterModel;
import io.github.ascopes.protobufmavenplugin.plexus.testdata.SomeBrokenModel;
import io.github.ascopes.protobufmavenplugin.plexus.testdata.ValidInnerModel;
import io.github.ascopes.protobufmavenplugin.plexus.testdata.ValidOuterModel;
import java.io.StringReader;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import org.codehaus.plexus.component.configurator.converters.lookup.ConverterLookup;
import org.codehaus.plexus.component.configurator.converters.lookup.DefaultConverterLookup;
import org.codehaus.plexus.component.configurator.expression.DefaultExpressionEvaluator;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("ImmutablesDataPlexusConverter tests")
class ImmutablesDataPlexusConverterTest {

  ImmutablesDataPlexusConverter converter;
  ConverterLookup converterLookup;
  ExpressionEvaluator expressionEvaluator;

  @BeforeEach
  void setUp() {
    converter = new ImmutablesDataPlexusConverter();
    converterLookup = new DefaultConverterLookup();
    expressionEvaluator = new DefaultExpressionEvaluator();

    converterLookup.registerConverter(converter);

  }

  @DisplayName(".canConvert(Class) returns the expected values")
  @MethodSource("canConvertTestCases")
  @ParameterizedTest(name = "expect {1} when calling with {0}")
  void canConvertReturnsTheExpectedValue(Class<?> cls, boolean expectedResult) {
    // When
    var actualResult = converter.canConvert(cls);

    // Then
    assertThat(actualResult).isEqualTo(expectedResult);
  }

  @DisplayName(".fromConfiguration(...) returns the expected results for nested models")
  @Test
  void fromConfigurationReturnsTheExpectedResults() throws Exception {
    // Given
    var configuration = xml2PlexusConfiguration("""
        <something>
          <foo>this-is-foo</foo>
          <bar>123456</bar>
          <validInnerModels>
            <validInnerModel>
              <foo>this-is-also-foo</foo>
              <bar>98765</bar>
            </validInnerModel>
          </validInnerModels>
        </something>
        """.stripIndent());

    // When
    var result = converterLookup.lookupConverterForType(ValidOuterModel.class)
        .fromConfiguration(
            converterLookup,
            configuration,
            ValidOuterModel.class,
            null,
            getClass().getClassLoader(),
            expressionEvaluator
        );

    // Then
    assertThat(result)
        .isNotNull()
        .isEqualTo(ImmutableValidOuterModel.builder()
            .foo("this-is-foo")
            .bar(123456)
            .validInnerModels(Set.of(
                ImmutableValidInnerModel.builder()
                    .foo("this-is-also-foo")
                    .bar(98765)
                    .build()))
            .build());
  }

  @DisplayName(
      "datatype lookup raises an IllegalStateException if unexpected reflective errors occur"
  )
  @Test
  void datatypeLookupRaisesIllegalStateExceptionIfUnexpectedReflectiveErrorsOccur() {
    // Then
    assertThatExceptionOfType(IllegalStateException.class)
        .isThrownBy(() -> converter.canConvert(SomeBrokenModel.class))
        .withCauseExactlyInstanceOf(NoSuchMethodException.class);
  }

  static PlexusConfiguration xml2PlexusConfiguration(String lines) {
    try {
      var dom = Xpp3DomBuilder.build(new StringReader(lines));
      return new XmlPlexusConfiguration(dom);
    } catch (Exception ex) {
      throw new RuntimeException("Failed to parse XML... welp", ex);
    }
  }

  static Stream<Arguments> canConvertTestCases() {
    return Stream.of(
        arguments(void.class, false),
        arguments(boolean.class, false),
        arguments(byte.class, false),
        arguments(short.class, false),
        arguments(int.class, false),
        arguments(long.class, false),
        arguments(float.class, false),
        arguments(double.class, false),
        arguments(Void.class, false),
        arguments(Object.class, false),
        arguments(String.class, false),
        arguments(List.class, false),
        arguments(Set.class, false),
        arguments(Class.class, false),
        arguments(SomeJunkType.class, false),
        arguments(ValidInnerModel.class, true),
        arguments(ValidOuterModel.class, true)
    );
  }

  interface SomeJunkType {
  }
}
