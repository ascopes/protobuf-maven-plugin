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
import static org.junit.jupiter.params.provider.Arguments.argumentSet;

import io.github.ascopes.protobufmavenplugin.digests.Digest;
import io.github.ascopes.protobufmavenplugin.digests.DigestException;
import java.util.stream.Stream;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.converters.lookup.DefaultConverterLookup;
import org.codehaus.plexus.component.configurator.expression.DefaultExpressionEvaluator;
import org.codehaus.plexus.configuration.DefaultPlexusConfiguration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;


@DisplayName("DigestConverter test")
class DigestConverterTest {

  DigestConverter converter;

  @BeforeEach
  void setUp() {
    converter = new DigestConverter();
  }

  @DisplayName("only the expected types are convertible")
  @CsvSource({
      "io.github.ascopes.protobufmavenplugin.utils.Digest,  true",
      "                                      java.net.URI, false",
      "                                java.nio.file.Path, false",
      "                                  java.lang.Object, false",
      "                                 java.lang.Integer, false",
      "                                   java.lang.Class, false",
      "                                  java.lang.String, false",
      "                           java.lang.StringBuilder, false",
      "                                      java.io.File, false",
      "                                      java.net.URL, false",
  })
  @ParameterizedTest(name = "for {0}, expect {1}")
  void onlyTheExpectedTypesAreConvertible(Class<?> type, boolean expectedResult) {
    // Then
    assertThat(converter.canConvert(type))
        .isEqualTo(expectedResult);
  }

  @DisplayName("Digests are converted successfully")
  @MethodSource("validDigestCases")
  @ParameterizedTest(name = "for {argumentSetName}")
  void digestsAreConvertedSuccessfully(
      String input,
      Digest expectedDigest
  ) throws ComponentConfigurationException {
    // Given
    var converterLookup = new DefaultConverterLookup();
    var configuration = new DefaultPlexusConfiguration("digest", input);
    var evaluator = new DefaultExpressionEvaluator();

    // When
    var result = converter.fromConfiguration(
        converterLookup,
        configuration,
        Digest.class,
        null,
        getClass().getClassLoader(),
        evaluator
    );

    // Then
    assertThat(result)
        .isEqualTo(expectedDigest);
  }

  static Stream<Arguments> validDigestCases() {
    return Stream.of(
        argumentSet(
            "MD2 hash (uppercase label)",
            "MD2:44b1450953f99dc391c25193d49f36f9",
            Digest.compute("MD2", "my train is slow")
        ),
        argumentSet(
            "MD2 hash (lowercase label)",
            "md2:44b1450953f99dc391c25193d49f36f9",
            Digest.compute("MD2", "my train is slow")
        ),
        argumentSet(
            "MD5 hash (lowercase hex digest)",
            "MD5:ab1e0de47af5f82ddee58ab91a41dad8",
            Digest.compute("MD5", "i hate writing tests")
        ),
        argumentSet(
            "MD5 hash (uppercase hex digest)",
            "MD5:AB1E0DE47AF5F82DDEE58AB91A41DAD8",
            Digest.compute("MD5", "i hate writing tests")
        ),
        argumentSet(
            "SHA-1 hash (using label 'SHA-1')",
            "SHA-1:99cdcb688b3de8368af969e86548a848fbb343aa",
            Digest.compute("SHA-1", "without an ide")
        ),
        argumentSet(
            "SHA-1 hash (using label 'sha1')",
            "sha1:99cdcb688b3de8368af969e86548a848fbb343aa",
            Digest.compute("SHA-1", "without an ide")
        ),
        argumentSet(
            "SHA-1 hash (using label 'SHA1')",
            "SHA1:99cdcb688b3de8368af969e86548a848fbb343aa",
            Digest.compute("SHA-1", "without an ide")
        ),
        argumentSet(
            "SHA-1 hash (split by various whitespace characters)",
            "SHA  \r\n\t1:99c\r\ndcb688b3de8368af969e86548a8\t  \t48fbb343aa",
            Digest.compute("SHA-1", "without an ide")
        ),
        argumentSet(
            "SHA-224 hash (using label 'SHA-224')",
            "SHA-224:90f2352402b7da021b46b09bd6f636ff24fa6690935a75719758103f",
            Digest.compute("SHA-224", "test data")
        ),
        argumentSet(
            "SHA-224 hash (using label 'sha224')",
            "sha224:90f2352402b7da021b46b09bd6f636ff24fa6690935a75719758103f",
            Digest.compute("SHA-224", "test data")
        ),
        argumentSet(
            "SHA-256 hash (using label 'SHA-256')",
            "SHA-256:916f0027a575074ce72a331777c3478d6513f786a591bd892da1a577bf2335f9",
            Digest.compute("SHA-256", "test data")
        ),
        argumentSet(
            "SHA-256 hash (using label 'sha256')",
            "sha256:916f0027a575074ce72a331777c3478d6513f786a591bd892da1a577bf2335f9",
            Digest.compute("SHA-256", "test data")
        ),
        argumentSet(
            "SHA-384 hash (using label 'SHA-384')",
            "SHA-384:29901176dc824ac3fd22227677499f02e4e69477ccc501593cc3dc8c6bfef73a08dfdf4a8017"
                + "23c0479b74d6f1abc372",
            Digest.compute("SHA-384", "test data")
        ),
        argumentSet(
            "SHA-384 hash (using label 'sha384')",
            "sha384:29901176dc824ac3fd22227677499f02e4e69477ccc501593cc3dc8c6bfef73a08dfdf4a8017"
                + "23c0479b74d6f1abc372",
            Digest.compute("SHA-384", "test data")
        ),
        argumentSet(
            "SHA-512 hash (using label 'SHA-512')",
            "SHA-512:0e1e21ecf105ec853d24d728867ad70613c21663a4693074b2a3619c1bd39d66b588c33723"
                + "bb466c72424e80e3ca63c249078ab347bab9428500e7ee43059d0d",
            Digest.compute("SHA-512", "test data")
        ),
        argumentSet(
            "SHA-512 hash (using label 'sha512')",
            "sha512:0e1e21ecf105ec853d24d728867ad70613c21663a4693074b2a3619c1bd39d66b588c33723"
                + "bb466c72424e80e3ca63c249078ab347bab9428500e7ee43059d0d",
            Digest.compute("SHA-512", "test data")
        )
    );
  }

  @DisplayName("unparsable strings raise an exception")
  @Test
  void unparsableStringsRaiseAnException() {
    // Given
    var converterLookup = new DefaultConverterLookup();
    var configuration = new DefaultPlexusConfiguration("digest", "dodgy input");
    var evaluator = new DefaultExpressionEvaluator();

    // Then
    assertThatExceptionOfType(ComponentConfigurationException.class)
        .isThrownBy(() -> converter.fromConfiguration(
            converterLookup,
            configuration,
            Digest.class,
            null,
            getClass().getClassLoader(),
            evaluator
        ))
        .withMessage(
            "Failed to parse digest 'dodgyinput'. Ensure that the digest is in a format such as "
                + "'sha512:1a2b3c4d', where the digest is a hexadecimal-encoded string."
        );
  }

  @DisplayName("invalid digests raise an exception")
  @Test
  void invalidDigestsRaiseAnException() {
    // Given
    var converterLookup = new DefaultConverterLookup();
    var configuration = new DefaultPlexusConfiguration("digest", "sha-69420:1a2b3c4d5e");
    var evaluator = new DefaultExpressionEvaluator();

    // Then
    assertThatExceptionOfType(ComponentConfigurationException.class)
        .isThrownBy(() -> converter.fromConfiguration(
            converterLookup,
            configuration,
            Digest.class,
            null,
            getClass().getClassLoader(),
            evaluator
        ))
        .withMessage(
            "Failed to parse digest 'sha-69420:1a2b3c4d5e': "
                + "%s: Digest 'SHA-69420' is not supported by this JVM",
            DigestException.class.getName()
        )
        .withCauseInstanceOf(DigestException.class);
  }


  @DisplayName("null values are returned directly")
  @Test
  void nullValuesAreReturnedDirectly() throws ComponentConfigurationException {
    // Given
    var converterLookup = new DefaultConverterLookup();
    var configuration = new DefaultPlexusConfiguration("digest", null);
    var evaluator = new DefaultExpressionEvaluator();

    // When
    var result = converter.fromConfiguration(
        converterLookup,
        configuration,
        Digest.class,
        null,
        getClass().getClassLoader(),
        evaluator
    );

    // Then
    assertThat(result).isNull();
  }
}
