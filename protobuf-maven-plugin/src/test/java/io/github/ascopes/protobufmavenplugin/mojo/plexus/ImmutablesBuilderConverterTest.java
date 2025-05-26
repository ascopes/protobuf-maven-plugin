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

import io.github.ascopes.protobufmavenplugin.mojo.plexus.immutablestypes.DoesNotHaveValidBuilderMethod1;
import io.github.ascopes.protobufmavenplugin.mojo.plexus.immutablestypes.DoesNotHaveValidBuilderMethod2;
import io.github.ascopes.protobufmavenplugin.mojo.plexus.immutablestypes.IllegallyNestedType;
import io.github.ascopes.protobufmavenplugin.mojo.plexus.immutablestypes.ImmutableValidModel;
import io.github.ascopes.protobufmavenplugin.mojo.plexus.immutablestypes.InvalidModel;
import io.github.ascopes.protobufmavenplugin.mojo.plexus.immutablestypes.InvalidModelBean;
import io.github.ascopes.protobufmavenplugin.mojo.plexus.immutablestypes.ValidConfigurationBean;
import io.github.ascopes.protobufmavenplugin.mojo.plexus.immutablestypes.ValidInnerModel;
import io.github.ascopes.protobufmavenplugin.mojo.plexus.immutablestypes.ValidModel;
import java.io.StringReader;
import java.util.List;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.converters.composite.ObjectWithFieldsConverter;
import org.codehaus.plexus.component.configurator.converters.lookup.DefaultConverterLookup;
import org.codehaus.plexus.component.configurator.expression.DefaultExpressionEvaluator;
import org.codehaus.plexus.configuration.xml.XmlPlexusConfiguration;
import org.codehaus.plexus.util.xml.Xpp3DomBuilder;
import org.immutables.value.Value.Modifiable;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

// This test must be public and have public attributes in specific places for Plexus integrations
// to work properly.
@DisplayName("ImmutablesBuilderConverter tests")
public class ImmutablesBuilderConverterTest {

  @DisplayName(".canConvert(Class) returns false for non-immutable model types")
  @ValueSource(classes = {
      int.class,
      Integer.class,
      Nullable.class,
      String.class,
      List.class,
      InvalidModel.class,
      Modifiable.class,
      InvalidModelBean.class,
      ImmutableValidModel.class,
      ImmutableValidModel.Builder.class,
      ImmutablesBuilderConverterTest.class,
      StringBuffer.class,
      DoesNotHaveValidBuilderMethod1.class,
      DoesNotHaveValidBuilderMethod2.class,
      IllegallyNestedType.class,
  })
  @ParameterizedTest(name = "expect {0} to not be compatible")
  void invalidModelTypesAreIncompatible(Class<?> type) {
    // Given
    var converter = new ImmutablesBuilderConverter();

    // Then
    assertThat(converter.canConvert(type)).isFalse();
  }

  @DisplayName(".canConvert(Class) returns true for compatible types")
  @ValueSource(classes = {
      ValidModel.class,
      ValidInnerModel.class,
  })
  @ParameterizedTest(name = "expect {0} to be compatible")
  void validModelTypesAreCompatible(Class<?> type) {
    // Given
    var converter = new ImmutablesBuilderConverter();

    // Then
    assertThat(converter.canConvert(type)).isTrue();
  }

  @DisplayName(".fromConfiguration(...) successfully parses the immutable type")
  @Test
  void fromConfigurationSuccessfullyParsesTheImmutableType() throws Exception {
    // Given
    var converter = new ImmutablesBuilderConverter();
    var converterLookup = new DefaultConverterLookup();
    converterLookup.registerConverter(converter);

    var dom = Xpp3DomBuilder.build(new StringReader(String.join(
        "\n",
        "<configuration>",
        "  <validModel>",
        "    <a>a</a>",
        "    <someBoolean>true</someBoolean>",
        "    <someInteger>1234</someInteger>",
        "    <someString>hello, world!</someString>",
        "    <validInnerModel>",
        "      <someString>this is legit</someString>",
        "    </validInnerModel>",
        "    <validInnerModels>",
        "      <validInnerModel>",
        "        <someString>this is also legit</someString>",
        "      </validInnerModel>",
        "      <validInnerModel>",
        "        <someString>and so is this</someString>",
        "      </validInnerModel>",
        "    </validInnerModels>",
        "  </validModel>",
        "</configuration>"
    )));

    // When
    var myConfiguration = new ValidConfigurationBean();
    new ObjectWithFieldsConverter().processConfiguration(
        converterLookup,
        myConfiguration,
        getClass().getClassLoader(),
        new XmlPlexusConfiguration(dom),
        new DefaultExpressionEvaluator()
    );

    // Then
    assertThat(myConfiguration.isInitialized())
        .as("#isInitialized()")
        .isTrue();

    assertThat(myConfiguration.getValidModel())
        .isNotNull()
        .satisfies(
            vm -> assertThat(vm.getA())
                .as("getA")
                .isEqualTo("a"),
            vm -> assertThat(vm.isSomeBoolean())
                .as("isSomeBoolean")
                .isTrue(),
            vm -> assertThat(vm.getSomeInteger())
                .as("getSomeInteger")
                .isEqualTo(1234),
            vm -> assertThat(vm.getSomeString())
                .as("getSomeString")
                .isEqualTo("hello, world!"),
            vm -> assertThat(vm.getValidInnerModel())
                .as("getValidInnerModel")
                .isNotNull()
                .satisfies(
                    vim -> assertThat(vim.getSomeString())
                        .as("getSomeString")
                        .isEqualTo("this is legit")
                ),
            vm -> assertThat(vm.getValidInnerModels())
                .isNotNull()
                .isNotEmpty()
                .hasSize(2)
                .satisfiesOnlyOnce(
                    vim -> assertThat(vim.getSomeString())
                        .as("getSomeString")
                        .isEqualTo("this is also legit")
                )
                .satisfiesOnlyOnce(
                    vim -> assertThat(vim.getSomeString())
                        .as("getSomeString")
                        .isEqualTo("and so is this")
                )
        );
  }

  @DisplayName(".fromConfiguration(...) fails for invalid attributes")
  @Test
  void fromConfigurationFailsForInvalidModels() throws Exception {
    // Given
    var converter = new ImmutablesBuilderConverter();
    var converterLookup = new DefaultConverterLookup();
    converterLookup.registerConverter(converter);

    var dom = Xpp3DomBuilder.build(new StringReader(String.join(
        "\n",
        "<configuration>",
        "  <validModel>",
        "    <undefined>uh-oh</undefined>",
        "  </validModel>",
        "</configuration>"
    )));

    // When/then
    var myConfiguration = new ValidConfigurationBean();

    assertThatExceptionOfType(ComponentConfigurationException.class)
        .isThrownBy(() -> new ObjectWithFieldsConverter().processConfiguration(
            converterLookup,
            myConfiguration,
            getClass().getClassLoader(),
            new XmlPlexusConfiguration(dom),
            new DefaultExpressionEvaluator()
        ))
        .withMessage(
            "Failed to construct %s instance from attribute 'validModel': "
                + "no such attribute 'undefined'",
            ValidModel.class.getSimpleName(),
            ImmutableValidModel.Builder.class.getSimpleName()
        )
        .havingCause()
        .isInstanceOf(NoSuchMethodException.class);
  }

  @DisplayName(".fromConfiguration(...) fails for invalid builder method calls")
  @Test
  void fromConfigurationFailsForInvalidBuilderMethodCalls() throws Exception {
    // Given
    var converter = new ImmutablesBuilderConverter();
    var converterLookup = new DefaultConverterLookup();
    converterLookup.registerConverter(converter);

    var dom = Xpp3DomBuilder.build(new StringReader(String.join(
        "\n",
        "<configuration>",
        "  <validModel>",
        "    <!-- 'from' is a special internal builder method we don't want to access. -->",
        "    <from>this shouldn't be possible</from>",
        "  </validModel>",
        "</configuration>"
    )));

    // When/then
    var myConfiguration = new ValidConfigurationBean();

    assertThatExceptionOfType(ComponentConfigurationException.class)
        .isThrownBy(() -> new ObjectWithFieldsConverter().processConfiguration(
            converterLookup,
            myConfiguration,
            getClass().getClassLoader(),
            new XmlPlexusConfiguration(dom),
            new DefaultExpressionEvaluator()
        ))
        .withMessage(
            "Failed to construct %s instance from attribute 'validModel': "
                + "no such attribute 'from'",
            ValidModel.class.getSimpleName(),
            ImmutableValidModel.Builder.class.getSimpleName()
        )
        .havingCause()
        .isInstanceOf(NoSuchMethodException.class);
  }
}
