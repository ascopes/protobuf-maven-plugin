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
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import java.io.StringReader;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
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

@DisplayName("SealedTypePlexusConverter tests")
class SealedTypePlexusConverterTest {

  SealedTypePlexusConverter converter;
  ConverterLookup converterLookup;
  ExpressionEvaluator expressionEvaluator;

  @BeforeEach
  void setUp() {
    converter = new SealedTypePlexusConverter();
    converterLookup = new DefaultConverterLookup();
    expressionEvaluator = new DefaultExpressionEvaluator();

    converterLookup.registerConverter(converter);
  }

  @DisplayName(".canConvert(Class<?>) returns true if passed sealed types")
  @MethodSource("canConvertTestCases")
  @ParameterizedTest(name = "for {argumentSetName}")
  void canConvertReturnsTrueIfPassedSealedType(Class<?> type, boolean expectedResult) {
    // When
    var result = converter.canConvert(type);

    // Then
    assertThat(result).isEqualTo(expectedResult);
  }

  @DisplayName(".fromConfiguration(...) returns the expected value")
  @Test
  void fromConfigurationReturnsTheExpectedValue() throws Exception {
    // Given
    var configuration = xml2PlexusConfiguration(
        """
        <database>
          <users>
            <user kind="person">
              <name>Bob</name>
            </user>
            <user kind="bot">
              <id>12345</id>
            </user>
            <user kind="admin">
              <name>Ashley</name>
              <employeeId>54321</employeeId>
            </user>
          </users>
        </database>
        """.stripIndent());

    // When
    var result = (Database) converterLookup.lookupConverterForType(Database.class)
        .fromConfiguration(
            converterLookup,
            configuration,
            Database.class,
            null,
            Database.class.getClassLoader(),
            expressionEvaluator,
            null
        );

    // Then
    assertThat(result)
        .as("result %s", result)
        .isNotNull();

    assertThat(result.users)
        .as("result.users %s", result.users)
        .isNotNull()
        .hasSize(3);

    assertThat(result.users.get(0))
        .as("result.users.0 %s", result.users.get(0))
        .isNotNull()
        .isExactlyInstanceOf(Person.class)
        .extracting(Person.class::cast)
        .satisfies(person -> assertThat(person.name)
            .as("person.name")
            .isEqualTo("Bob"));

    assertThat(result.users.get(1))
        .as("result.users.1 %s", result.users.get(1))
        .isNotNull()
        .isExactlyInstanceOf(Bot.class)
        .extracting(Bot.class::cast)
        .satisfies(bot -> assertThat(bot.id)
            .as("bot.id")
            .isEqualTo("12345"));

    assertThat(result.users.get(2))
        .as("result.users.2 %s", result.users.get(2))
        .isNotNull()
        .isExactlyInstanceOf(Owner.class)
        .extracting(Owner.class::cast)
        .satisfies(
            owner -> assertThat(owner.name)
                .as("owner.name")
                .isEqualTo("Ashley"),
            owner -> assertThat(owner.employeeId)
                .as("owner.employeeId")
                .isEqualTo("54321"));
  }

  @DisplayName(".fromConfiguration(...) raises an exception for missing kind attributes")
  @Test
  void fromConfigurationRaisesExceptionForMissingKindAttributes() throws Exception {
    // Given
    var configuration = xml2PlexusConfiguration(
        """
        <database>
          <users>
            <user>
              <name>Bob</name>
            </user>
            <user kind="bot">
              <id>12345</id>
            </user>
            <user kind="admin">
              <name>Ashley</name>
              <employeeId>54321</employeeId>
            </user>
          </users>
        </database>
        """.stripIndent());

    // Then
    assertThatExceptionOfType(ComponentConfigurationException.class)
        .isThrownBy(() -> converterLookup.lookupConverterForType(Database.class)
            .fromConfiguration(
                converterLookup,
                configuration,
                Database.class,
                null,
                Database.class.getClassLoader(),
                expressionEvaluator,
                null
            ))
        .withMessage("Missing \"kind\" attribute. Valid kinds are: "
            + "\"admin\", \"bot\", \"person\"")
        .extracting(ComponentConfigurationException::getFailedConfiguration)
        .as("exception#getFailedConfiguration()")
        .isEqualTo(configuration.getChild(0).getChild(0));
  }

  @DisplayName(".fromConfiguration(...) raises an exception for unknown kind attribute values")
  @Test
  void fromConfigurationRaisesExceptionForUnknownKindAttributeValues() throws Exception {
    // Given
    var configuration = xml2PlexusConfiguration(
        """
        <database>
          <users>
            <user kind="smurf">
              <name>Bob</name>
            </user>
            <user kind="bot">
              <id>12345</id>
            </user>
            <user kind="admin">
              <name>Ashley</name>
              <employeeId>54321</employeeId>
            </user>
          </users>
        </database>
        """.stripIndent());

    // Then
    assertThatExceptionOfType(ComponentConfigurationException.class)
        .isThrownBy(() -> converterLookup.lookupConverterForType(Database.class)
            .fromConfiguration(
                converterLookup,
                configuration,
                Database.class,
                null,
                Database.class.getClassLoader(),
                expressionEvaluator,
                null
            ))
        .withMessage("Invalid kind \"smurf\" specified. Valid kinds are: "
            + "\"admin\", \"bot\", \"person\"")
        .extracting(ComponentConfigurationException::getFailedConfiguration)
        .as("exception#getFailedConfiguration()")
        .isEqualTo(configuration.getChild(0).getChild(0));
  }

  /*
   * canConvert test data
   */

  static Stream<Arguments> canConvertTestCases() {
    return Stream.of(
        argumentSet("sealed interface", SealedInterface.class, true),
        argumentSet("sealed class", SealedClass.class, true),
        argumentSet("non-sealed class", NonSealedClass.class, false),
        argumentSet("regular interface", RegularInterface.class, false),
        argumentSet("regular class", RegularClass.class, false),
        argumentSet("record", SomeRecord.class, false),
        argumentSet("enum", SomeEnum.class, false),
        argumentSet("annotation", SomeAnnotation.class, false),
        argumentSet("builtin type", String.class, false),
        argumentSet("primitive", int.class, false)
    );
  }

  sealed interface SealedInterface permits NonSealedClass {}

  abstract static sealed class SealedClass permits NonSealedClass {}

  abstract static non-sealed class NonSealedClass extends SealedClass implements SealedInterface {}

  interface RegularInterface {}

  static class RegularClass {}

  record SomeRecord() {}

  enum SomeEnum {
    FOO, BAR
  }

  @interface SomeAnnotation {}

  /*
   * fromConfiguration test data
   */

  public static class Database {
    List<User> users = List.of();

    public Database() {}

    public List<User> getUsers() {
      return users;
    }

    public void setUsers(List<User> users) {
      this.users = users;
    }
  }

  public sealed interface User permits Person, Bot, Admin, InvalidLeafType {
    String getName();
  }

  @KindHint(kind = "person", implementation = Person.class)
  public static final class Person implements User {
    String name = "";

    @Override
    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }
  }

  @KindHint(kind = "bot", implementation = Bot.class)
  public static final class Bot implements User {
    String id = "";

    @Override
    public String getName() {
      return "bot::@" + id;
    }

    public void setId(String id) {
      this.id = id;
    }
  }

  @KindHint(kind = "admin", implementation = Owner.class)
  public abstract static non-sealed class Admin implements User {
    String name = "";
    String employeeId = "";

    @Override
    public String getName() {
      return name;
    }

    public void setName(String name) {
      this.name = name;
    }

    public String getEmployeeId() {
      return employeeId;
    }

    public void setEmployeeId(String employeeId) {
      this.employeeId = employeeId;
    }
  }

  public static final class Owner extends Admin {}

  // Missing annotation
  public static final class InvalidLeafType implements User {
    @Override
    public String getName() {
      return "";
    }
  }

  /*
   * Other helpers
   */
  static PlexusConfiguration xml2PlexusConfiguration(String lines) {
    try {
      var dom = Xpp3DomBuilder.build(new StringReader(lines));
      return new XmlPlexusConfiguration(dom);
    } catch (Exception ex) {
      throw new RuntimeException("Failed to parse XML... welp", ex);
    }
  }
}
