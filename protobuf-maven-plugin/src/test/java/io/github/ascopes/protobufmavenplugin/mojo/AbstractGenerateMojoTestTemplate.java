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
package io.github.ascopes.protobufmavenplugin.mojo;

import static io.github.ascopes.protobufmavenplugin.fixtures.RandomFixtures.someBasicString;
import static java.util.function.Predicate.not;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

import io.github.ascopes.protobufmavenplugin.dependencies.DependencyResolutionDepth;
import io.github.ascopes.protobufmavenplugin.dependencies.MavenDependencyBean;
import io.github.ascopes.protobufmavenplugin.digests.Digest;
import io.github.ascopes.protobufmavenplugin.fixtures.UsesSystemProperties;
import io.github.ascopes.protobufmavenplugin.generation.GenerationRequest;
import io.github.ascopes.protobufmavenplugin.generation.GenerationResult;
import io.github.ascopes.protobufmavenplugin.generation.Language;
import io.github.ascopes.protobufmavenplugin.generation.ProtobufBuildOrchestrator;
import io.github.ascopes.protobufmavenplugin.generation.SourceRootRegistrar;
import io.github.ascopes.protobufmavenplugin.plugins.BinaryMavenProtocPlugin;
import io.github.ascopes.protobufmavenplugin.plugins.BinaryMavenProtocPluginBean;
import io.github.ascopes.protobufmavenplugin.plugins.JvmMavenProtocPluginBean;
import io.github.ascopes.protobufmavenplugin.plugins.PathProtocPlugin;
import io.github.ascopes.protobufmavenplugin.plugins.PathProtocPluginBean;
import io.github.ascopes.protobufmavenplugin.plugins.ProtocPlugin;
import io.github.ascopes.protobufmavenplugin.plugins.UriProtocPlugin;
import io.github.ascopes.protobufmavenplugin.plugins.UriProtocPluginBean;
import io.github.ascopes.protobufmavenplugin.protoc.dists.BinaryMavenProtocDistribution;
import io.github.ascopes.protobufmavenplugin.utils.ResolutionException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.Spliterators.AbstractSpliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;
import org.apache.maven.model.Build;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.DynamicTest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestFactory;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.quality.Strictness;

@SuppressWarnings("UnnecessaryAssignment")
abstract class AbstractGenerateMojoTestTemplate<A extends AbstractGenerateMojo> {

  @TempDir
  Path tempDir;

  A mojo;

  @BeforeEach
  void setUp() throws Throwable {
    // Final vars to disable checkstyle complaints.
    final var mockBuild = mock(
        Build.class,
        withSettings().strictness(Strictness.LENIENT)
    );
    final var mavenProject = mock(
        MavenProject.class,
        withSettings().strictness(Strictness.LENIENT)
    );
    final var sourceCodeGenerator
        = sourceCodeGenerator(GenerationResult.PROTOC_SUCCEEDED);

    when(mockBuild.getDirectory())
        .thenReturn(tempDir.toString());
    when(mavenProject.getBuild())
        .thenReturn(mockBuild);
    when(mavenProject.getBasedir())
        .thenReturn(tempDir.toFile());

    mojo = newInstance();
    mojo.protocDistributionConverter = mock();
    mojo.sourceCodeGenerator = sourceCodeGenerator;
    mojo.mavenProject = mavenProject;
    mojo.protoc = mock(BinaryMavenProtocDistribution.class);
  }

  abstract A newInstance();

  abstract SourceRootRegistrar expectedSourceRootRegistrar();

  abstract Collection<Path> expectedDefaultSourceDirectories();

  abstract Path expectedDefaultOutputDirectory();

  abstract Set<String> expectedDefaultDependencyScopes();

  @DisplayName("the sourceRootRegistrar is the expected value")
  @Test
  void sourceRootRegistrarIsTheExpectedValue() {
    // Then
    assertThat(newInstance().sourceRootRegistrar())
        .isEqualTo(expectedSourceRootRegistrar());
  }

  @DisplayName("the default source directory is the expected path")
  @Test
  void defaultSourceDirectoriesIsTheExpectedPath() {
    // Given
    assertThat(mojo.defaultSourceDirectories())
        .isEqualTo(expectedDefaultSourceDirectories());
  }

  @DisplayName("the default output directory is the expected path")
  @Test
  void defaultOutputDirectoryIsTheExpectedPath() {
    // Then
    assertThat(mojo.defaultOutputDirectory())
        .isEqualTo(expectedDefaultOutputDirectory());
  }

  @DisplayName("nothing is run when the plugin is skipped")
  @Test
  void nothingIsRunWhenThePluginIsSkipped() throws Throwable {
    // Given
    mojo.skip = true;

    // When
    mojo.execute();

    // Then
    verifyNoInteractions(mojo.sourceCodeGenerator);
  }

  @DisplayName("no exception is raised when the sourceCodeGenerator is successful")
  @TestFactory
  Stream<DynamicTest> noExceptionRaisedWhenSourceCodeGeneratorReturnsSuccessful() {
    return Stream.of(GenerationResult.values())
        .filter(GenerationResult::isOk)
        .map(result -> DynamicTest.dynamicTest("for result " + result, () -> {
          // Given
          mojo.sourceCodeGenerator = sourceCodeGenerator(result);

          // Then
          assertThatNoException()
              .isThrownBy(mojo::execute);
        }));
  }

  @DisplayName("a MojoExecutionException is raised when the sourceCodeGenerator returns an error")
  @TestFactory
  Stream<DynamicTest> mojoExecutionExceptionRaisedWhenSourceCodeGeneratorReturnsError() {
    return Stream.of(GenerationResult.values())
        .filter(not(GenerationResult::isOk))
        .map(result -> DynamicTest.dynamicTest("for result " + result, () -> {
          // Given
          mojo.sourceCodeGenerator = sourceCodeGenerator(result);

          // Then
          assertThatException()
              .isThrownBy(mojo::execute)
              .isInstanceOf(MojoExecutionException.class)
              .withNoCause()
              .withMessage("Generation failed - %s", result);
        }));
  }

  @DisplayName("a MojoFailureException is raised when the sourceCodeGenerator raises")
  @ValueSource(classes = {IOException.class, ResolutionException.class})
  @ParameterizedTest(name = "of type {0}")
  void mojoFailureExceptionRaisedWhenSourceCodeGeneratorRaises(
      Class<Throwable> causeType
  ) throws Throwable {
    // Given
    var message = "some message blah blah " + someBasicString();
    var exceptionCause = causeType.getConstructor(String.class).newInstance(message);
    mojo.sourceCodeGenerator = erroringSourceCodeGenerator(exceptionCause);

    // Then
    assertThatException()
        .isThrownBy(mojo::execute)
        .isInstanceOf(MojoFailureException.class)
        .withMessage("Generation aborted due to an unexpected error - %s", exceptionCause)
        .withCause(exceptionCause);
  }

  @DisplayName("when arguments is null, expect an empty list in the request")
  @NullAndEmptySource
  @ParameterizedTest(name = "when {0}")
  void whenArgumentsNullExpectEmptyListInRequest(
      @Nullable List<String> arguments
  ) throws Throwable {
    // Given
    mojo.arguments = arguments;

    // When
    mojo.execute();

    // Then
    var captor = ArgumentCaptor.forClass(GenerationRequest.class);
    verify(mojo.sourceCodeGenerator).generate(captor.capture());
    var actualRequest = captor.getValue();
    assertThat(actualRequest.getArguments()).isEmpty();
  }

  @DisplayName("when arguments are provided, expect the arguments in the request")
  @Test
  void whenArgumentsProvidedExpectArgumentsInRequest() throws Throwable {
    // Given
    List<String> arguments = List.of("foo", "bar");
    mojo.arguments = arguments;

    // When
    mojo.execute();

    // Then
    var captor = ArgumentCaptor.forClass(GenerationRequest.class);
    verify(mojo.sourceCodeGenerator).generate(captor.capture());
    var actualRequest = captor.getValue();
    assertThat(actualRequest.getArguments()).isEqualTo(arguments);
  }

  @DisplayName("cleanOutputDirectories is set on the request")
  @ValueSource(booleans = {true, false})
  @ParameterizedTest(name = "when {0}")
  void cleanOutputDirectoriesIsSetOnTheRequest(
      boolean cleanOutputDirectories
  ) throws Throwable {
    // Given
    mojo.cleanOutputDirectories = cleanOutputDirectories;

    // When
    mojo.execute();

    // Then
    var captor = ArgumentCaptor.forClass(GenerationRequest.class);
    verify(mojo.sourceCodeGenerator).generate(captor.capture());
    var actualRequest = captor.getValue();
    assertThat(actualRequest.isCleanOutputDirectories())
        .isEqualTo(cleanOutputDirectories);
  }

  @DisplayName("the dependencyResolutionDepth is set to the specified value")
  @EnumSource(DependencyResolutionDepth.class)
  @ParameterizedTest(name = "for {0}")
  void dependencyResolutionDepthIsSetToSpecifiedValue(
      DependencyResolutionDepth dependencyResolutionDepth
  ) throws Throwable {
    // Given
    mojo.dependencyResolutionDepth = dependencyResolutionDepth;

    // When
    mojo.execute();

    // Then
    var captor = ArgumentCaptor.forClass(GenerationRequest.class);
    verify(mojo.sourceCodeGenerator).generate(captor.capture());
    var actualRequest = captor.getValue();
    assertThat(actualRequest.getDependencyResolutionDepth()).isSameAs(dependencyResolutionDepth);
  }

  @DisplayName("defaultDependencyScopes() are used when no user provided scopes are present")
  @NullAndEmptySource
  @ParameterizedTest(name = "for dependencyScopes = \"{0}\"")
  void defaultDependencyScopesAreUsedWhenNoUserProvidedScopesArePresent(
      @Nullable Set<String> userProvidedDependencyScopes
  ) throws Throwable {
    // Given
    mojo.dependencyScopes = userProvidedDependencyScopes;

    // When
    mojo.execute();

    // Then
    var captor = ArgumentCaptor.forClass(GenerationRequest.class);
    verify(mojo.sourceCodeGenerator).generate(captor.capture());
    var actualRequest = captor.getValue();
    assertThat(actualRequest.getDependencyScopes())
        .containsExactlyInAnyOrderElementsOf(expectedDefaultDependencyScopes());
  }

  @DisplayName("user-provided dependencyScopes are used when provided")
  @MethodSource("io.github.ascopes.protobufmavenplugin.mojo.AbstractGenerateMojoTestTemplate"
      + "#scopeCombinations")
  @ParameterizedTest(name = "for {0}")
  void userProvidedDependencyScopesAreUsedWhenProvided(Set<String> combination) throws Throwable {
    // Given
    mojo.dependencyScopes = combination;

    // When
    mojo.execute();

    // Then
    var captor = ArgumentCaptor.forClass(GenerationRequest.class);
    verify(mojo.sourceCodeGenerator).generate(captor.capture());
    var actualRequest = captor.getValue();

    assertThat(actualRequest.getDependencyScopes())
        .containsExactlyInAnyOrderElementsOf(combination);
  }

  @DisplayName("the environmentVariables are set to the specified value")
  @Test
  void environmentVariablesAreSetToTheSpecifiedValue() throws Throwable {
    mojo.environmentVariables = Map.of("foo", "bar", "baz", "bork");

    // When
    mojo.execute();

    // Then
    var captor = ArgumentCaptor.forClass(GenerationRequest.class);
    verify(mojo.sourceCodeGenerator).generate(captor.capture());
    var actualRequest = captor.getValue();
    assertThat(actualRequest.getEnvironmentVariables())
        .isEqualTo(Map.of("foo", "bar", "baz", "bork"));
  }

  @DisplayName("the environmentVariables are set to an empty map if unspecified")
  @Test
  void environmentVariablesAreSetToAnEmptyMapIfUnspecified() throws Throwable {
    mojo.environmentVariables = null;

    // When
    mojo.execute();

    // Then
    var captor = ArgumentCaptor.forClass(GenerationRequest.class);
    verify(mojo.sourceCodeGenerator).generate(captor.capture());
    var actualRequest = captor.getValue();
    assertThat(actualRequest.getEnvironmentVariables()).isEmpty();
  }

  @DisplayName("fatalWarnings is set to the specified value")
  @ValueSource(booleans = {true, false})
  @ParameterizedTest(name = "for {0}")
  void fatalWarningsIsSetToSpecifiedValue(boolean value) throws Throwable {
    mojo.fatalWarnings = value;

    // When
    mojo.execute();

    // Then
    var captor = ArgumentCaptor.forClass(GenerationRequest.class);
    verify(mojo.sourceCodeGenerator).generate(captor.capture());
    var actualRequest = captor.getValue();
    assertThat(actualRequest.isFatalWarnings()).isEqualTo(value);
  }

  @DisplayName("ignoreProjectDependencies is set to the specified value")
  @ValueSource(booleans = {true, false})
  @ParameterizedTest(name = "for {0}")
  void ignoreProjectDependenciesIsSetToSpecifiedValue(boolean value) throws Throwable {
    mojo.ignoreProjectDependencies = value;

    // When
    mojo.execute();

    // Then
    var captor = ArgumentCaptor.forClass(GenerationRequest.class);
    verify(mojo.sourceCodeGenerator).generate(captor.capture());
    var actualRequest = captor.getValue();
    assertThat(actualRequest.isIgnoreProjectDependencies()).isEqualTo(value);
  }

  @DisplayName("when importDependencies is null, expect an empty list in the request")
  @NullAndEmptySource
  @ParameterizedTest(name = "when {0}")
  void whenImportDependenciesNullExpectEmptyListInRequest(
      @Nullable List<MavenDependencyBean> dependencies
  ) throws Throwable {
    // Given
    mojo.importDependencies = dependencies;

    // When
    mojo.execute();

    // Then
    var captor = ArgumentCaptor.forClass(GenerationRequest.class);
    verify(mojo.sourceCodeGenerator).generate(captor.capture());
    var actualRequest = captor.getValue();
    assertThat(actualRequest.getImportDependencies()).isEmpty();
  }

  @DisplayName("when importDependencies is provided, expect the plugins in the request")
  @Test
  void whenImportDependenciesProvidedExpectPluginsInRequest() throws Throwable {
    // Given
    List<MavenDependencyBean> plugins = mock();
    mojo.importDependencies = plugins;

    // When
    mojo.execute();

    // Then
    var captor = ArgumentCaptor.forClass(GenerationRequest.class);
    verify(mojo.sourceCodeGenerator).generate(captor.capture());
    var actualRequest = captor.getValue();
    assertThat(actualRequest.getImportDependencies()).isSameAs(plugins);
  }

  @DisplayName("when importPaths is null, expect an empty list in the request")
  @NullAndEmptySource
  @ParameterizedTest(name = "when {0}")
  void whenImportPathsNullExpectEmptyListInRequest(
      @Nullable List<Path> importPaths
  ) throws Throwable {
    // Given
    mojo.importPaths = importPaths;

    // When
    mojo.execute();

    // Then
    var captor = ArgumentCaptor.forClass(GenerationRequest.class);
    verify(mojo.sourceCodeGenerator).generate(captor.capture());
    var actualRequest = captor.getValue();
    assertThat(actualRequest.getImportPaths()).isEmpty();
  }

  @DisplayName("when importPaths is provided, expect the paths in the request")
  @Test
  void whenImportPathsProvidedExpectPathsInRequest(
      @TempDir Path someTempDir
  ) throws Throwable {
    // Given
    var path1 = Files.createDirectories(someTempDir.resolve("foo").resolve("bar"));
    var path2 = Files.createDirectories(someTempDir.resolve("do").resolve("ray"));
    var path3 = Files.createDirectories(someTempDir.resolve("aaa").resolve("bbb"));

    mojo.importPaths = List.of(path1, path2, path3);

    // When
    mojo.execute();

    // Then
    var captor = ArgumentCaptor.forClass(GenerationRequest.class);
    verify(mojo.sourceCodeGenerator).generate(captor.capture());
    var actualRequest = captor.getValue();
    assertThat(actualRequest.getImportPaths()).containsExactly(path1, path2, path3);
  }

  @DisplayName("incrementalCompilation is set to the specified value")
  @ValueSource(booleans = {true, false})
  @ParameterizedTest(name = "for {0}")
  void incrementalCompilationIsSetToSpecifiedValue(boolean value) throws Throwable {
    mojo.incrementalCompilation = value;

    // When
    mojo.execute();

    // Then
    var captor = ArgumentCaptor.forClass(GenerationRequest.class);
    verify(mojo.sourceCodeGenerator).generate(captor.capture());
    var actualRequest = captor.getValue();
    assertThat(actualRequest.isIncrementalCompilationEnabled()).isEqualTo(value);
  }

  @DisplayName("when jvmMavenPlugins is null, expect an empty list in the request")
  @NullAndEmptySource
  @ParameterizedTest(name = "when {0}")
  @SuppressWarnings("removal")
  void whenJvmMavenPluginsNullExpectEmptyListInRequest(
      @Nullable List<JvmMavenProtocPluginBean> plugins
  ) throws Throwable {
    // Given
    mojo.jvmMavenPlugins = plugins;

    // When
    mojo.execute();

    // Then
    var captor = ArgumentCaptor.forClass(GenerationRequest.class);
    verify(mojo.sourceCodeGenerator).generate(captor.capture());
    var actualRequest = captor.getValue();
    assertThat(actualRequest.getProtocPlugins()).isEmpty();
  }

  @DisplayName("liteOnly is set to the specified value")
  @ValueSource(booleans = {true, false})
  @ParameterizedTest(name = "for {0}")
  void liteOnlyIsSetToSpecifiedValue(boolean value) throws Throwable {
    mojo.liteOnly = value;

    // When
    mojo.execute();

    // Then
    var captor = ArgumentCaptor.forClass(GenerationRequest.class);
    verify(mojo.sourceCodeGenerator).generate(captor.capture());
    var actualRequest = captor.getValue();
    assertThat(actualRequest.isLiteEnabled()).isEqualTo(value);
  }

  @DisplayName("when outputDescriptorFile is provided, expect the provided file to be used")
  @Test
  void whenDescriptorFileProvidedExpectProvidedDirectoryToBeUsed(
      @TempDir Path tempDir
  ) throws Throwable {
    var expectedDescriptorFile = Files.createFile(tempDir.resolve("protobin.desc"));
    // Given
    mojo.outputDescriptorFile = expectedDescriptorFile;

    // When
    mojo.execute();

    // Then
    var captor = ArgumentCaptor.forClass(GenerationRequest.class);
    verify(mojo.sourceCodeGenerator).generate(captor.capture());
    var actualRequest = captor.getValue();
    assertThat(actualRequest.getOutputDescriptorFile())
        .isEqualTo(expectedDescriptorFile);
  }

  @DisplayName("when outputDescriptorFile is not provided, expect no file to be used")
  @Test
  void whenDescriptorFileNotProvidedExpectNoFileToBeUsed() throws Throwable {
    // Given
    mojo.outputDescriptorFile = null;

    // When
    mojo.execute();

    // Then
    var captor = ArgumentCaptor.forClass(GenerationRequest.class);
    verify(mojo.sourceCodeGenerator).generate(captor.capture());
    var actualRequest = captor.getValue();
    assertThat(actualRequest.getOutputDescriptorFile())
        .isNull();
  }

  @DisplayName("outputDescriptorIncludeImports is set to the specified value")
  @ValueSource(booleans = {true, false})
  @ParameterizedTest(name = "for {0}")
  void outputDescriptorIncludeImportsIsSetToSpecifiedValue(boolean value) throws Throwable {
    mojo.outputDescriptorIncludeImports = value;

    // When
    mojo.execute();

    // Then
    var captor = ArgumentCaptor.forClass(GenerationRequest.class);
    verify(mojo.sourceCodeGenerator).generate(captor.capture());
    var actualRequest = captor.getValue();
    assertThat(actualRequest.isOutputDescriptorIncludeImports()).isEqualTo(value);
  }

  @DisplayName("outputDescriptorIncludeSourceInfo is set to the specified value")
  @ValueSource(booleans = {true, false})
  @ParameterizedTest(name = "for {0}")
  void outputDescriptorIncludeSourceInfoIsSetToSpecifiedValue(boolean value) throws Throwable {
    mojo.outputDescriptorIncludeSourceInfo = value;

    // When
    mojo.execute();

    // Then
    var captor = ArgumentCaptor.forClass(GenerationRequest.class);
    verify(mojo.sourceCodeGenerator).generate(captor.capture());
    var actualRequest = captor.getValue();
    assertThat(actualRequest.isOutputDescriptorIncludeSourceInfo()).isEqualTo(value);
  }

  @DisplayName("outputDescriptorIncludeImports is set to the specified value")
  @ValueSource(booleans = {true, false})
  @ParameterizedTest(name = "for {0}")
  void outputDescriptorRetainOptionsIsSetToSpecifiedValue(boolean value) throws Throwable {
    mojo.outputDescriptorRetainOptions = value;

    // When
    mojo.execute();

    // Then
    var captor = ArgumentCaptor.forClass(GenerationRequest.class);
    verify(mojo.sourceCodeGenerator).generate(captor.capture());
    var actualRequest = captor.getValue();
    assertThat(actualRequest.isOutputDescriptorRetainOptions()).isEqualTo(value);
  }

  @DisplayName("outputDescriptorAttached is set to the specified value")
  @ValueSource(booleans = {true, false})
  @ParameterizedTest(name = "for {0}")
  void outputDescriptorAttachedIsSetToSpecifiedValue(boolean value) throws Throwable {
    mojo.outputDescriptorAttached = value;

    // When
    mojo.execute();

    // Then
    var captor = ArgumentCaptor.forClass(GenerationRequest.class);
    verify(mojo.sourceCodeGenerator).generate(captor.capture());
    var actualRequest = captor.getValue();
    assertThat(actualRequest.isOutputDescriptorAttached()).isEqualTo(value);
  }

  @DisplayName("outputDescriptorAttachmentType is set to the specified value")
  @Test
  void outputDescriptorAttachmentTypeIsSetToSpecifiedValue() throws Throwable {
    mojo.outputDescriptorAttachmentType = "mytype";

    // When
    mojo.execute();

    // Then
    var captor = ArgumentCaptor.forClass(GenerationRequest.class);
    verify(mojo.sourceCodeGenerator).generate(captor.capture());
    var actualRequest = captor.getValue();
    assertThat(actualRequest.getOutputDescriptorAttachmentType()).isEqualTo("mytype");
  }

  @DisplayName("outputDescriptorAttachmentClassifier is set to the specified value")
  @Test
  void outputDescriptorAttachmentClassifierIsSetToSpecifiedValue() throws Throwable {
    mojo.outputDescriptorAttachmentClassifier = "myclassifier";

    // When
    mojo.execute();

    // Then
    var captor = ArgumentCaptor.forClass(GenerationRequest.class);
    verify(mojo.sourceCodeGenerator).generate(captor.capture());
    var actualRequest = captor.getValue();
    assertThat(actualRequest.getOutputDescriptorAttachmentClassifier()).isEqualTo("myclassifier");
  }

  @DisplayName(
      "when outputDirectory is not provided, expect the default output directory to be used"
  )
  @Test
  void whenOutputDirectoryNotProvidedExpectDefaultOutputDirectoryToBeUsed() throws Throwable {
    // Given
    mojo.outputDirectory = null;
    var expectedOutputDirectory = expectedDefaultOutputDirectory();

    // When
    mojo.execute();

    // Then
    var captor = ArgumentCaptor.forClass(GenerationRequest.class);
    verify(mojo.sourceCodeGenerator).generate(captor.capture());
    var actualRequest = captor.getValue();
    assertThat(actualRequest.getOutputDirectory()).isEqualTo(expectedOutputDirectory);
  }

  @DisplayName("when outputDirectory is provided, expect the provided directory to be used")
  @Test
  void whenOutputDirectoryProvidedExpectProvidedDirectoryToBeUsed(
      @TempDir Path expectedOutputDirectory
  ) throws Throwable {
    // Given
    mojo.outputDirectory = expectedOutputDirectory;

    // When
    mojo.execute();

    // Then
    var captor = ArgumentCaptor.forClass(GenerationRequest.class);
    verify(mojo.sourceCodeGenerator).generate(captor.capture());
    var actualRequest = captor.getValue();
    assertThat(actualRequest.getOutputDirectory()).isEqualTo(expectedOutputDirectory);
  }

  @DisplayName("the protocDigest is set in the request")
  @NullSource
  @ValueSource(strings = "non-null")
  @ParameterizedTest(name = "for {0} digest")
  @SuppressWarnings("removal")
  void protocDistributionDigestIsSetInTheRequest(@Nullable String digestValue) throws Throwable {
    // Given
    var digest = Optional.ofNullable(digestValue)
        .map(v -> Digest.compute("SHA-1", v))
        .orElse(null);
    mojo.protocDigest = digest;

    // When
    mojo.execute();

    // Then
    var captor = ArgumentCaptor.forClass(GenerationRequest.class);
    verify(mojo.sourceCodeGenerator).generate(captor.capture());
    var actualRequest = captor.getValue();
    assertThat(actualRequest.getProtocDigest()).isSameAs(digest);
  }

  @DisplayName("when plugins is null, expect an empty list in the request")
  @NullAndEmptySource
  @ParameterizedTest(name = "when {0}")
  void whenPluginsNullExpectEmptyListInRequest(
      @Nullable List<ProtocPlugin> plugins
  ) throws Throwable {
    // Given
    mojo.plugins = plugins;

    // When
    mojo.execute();

    // Then
    var captor = ArgumentCaptor.forClass(GenerationRequest.class);
    verify(mojo.sourceCodeGenerator).generate(captor.capture());
    var actualRequest = captor.getValue();
    assertThat(actualRequest.getProtocPlugins()).isEmpty();
  }

  @DisplayName("when plugins are provided, expect the plugins in the request")
  @Test
  void whenPluginsProvidedExpectPluginsInRequest() throws Throwable {
    // Given
    List<ProtocPlugin> plugins = List.of(
        mock(JvmMavenProtocPluginBean.class),
        mock(BinaryMavenProtocPlugin.class),
        mock(UriProtocPlugin.class),
        mock(PathProtocPlugin.class)
    );
    mojo.plugins = plugins;

    // When
    mojo.execute();

    // Then
    var captor = ArgumentCaptor.forClass(GenerationRequest.class);
    verify(mojo.sourceCodeGenerator).generate(captor.capture());
    var actualRequest = captor.getValue();
    assertThat(actualRequest.getProtocPlugins()).containsAll(plugins);
  }

  @DisplayName("when legacy plugins are provided, expect the plugins in the request")
  @Test
  @SuppressWarnings("removal")
  void whenLegacyPluginsProvidedExpectPluginsInRequest() throws Throwable {
    // Given
    List<BinaryMavenProtocPluginBean> binaryMavenPlugins = List.of(
        mock("<binaryMavenPlugin 1>"),
        mock("<binaryMavenPlugin 2>")
    );
    mojo.binaryMavenPlugins = binaryMavenPlugins;

    List<PathProtocPluginBean> binaryPathPlugins = List.of(
        mock("<binaryPathPlugin 1>"),
        mock("<binaryPathPlugin 2>")
    );
    mojo.binaryPathPlugins = binaryPathPlugins;

    List<UriProtocPluginBean> binaryUrlPlugins = List.of(
        mock("<binaryUrlPlugin 1>"),
        mock("<binaryUrlPlugin 2>")
    );
    mojo.binaryUrlPlugins = binaryUrlPlugins;

    List<JvmMavenProtocPluginBean> jvmMavenPlugins = List.of(
        mock("<jvmMavenPlugin 1>"),
        mock("<jvmMavenPlugin 2>")
    );
    mojo.jvmMavenPlugins = jvmMavenPlugins;

    List<ProtocPlugin> plugins = List.of(
        mock(JvmMavenProtocPluginBean.class, "<plugin 1>"),
        mock(BinaryMavenProtocPlugin.class, "<plugin 2>"),
        mock(UriProtocPlugin.class, "<plugin 3>"),
        mock(PathProtocPlugin.class, "<plugin 4>")
    );
    mojo.plugins = plugins;

    // When
    mojo.execute();

    // Then
    var captor = ArgumentCaptor.forClass(GenerationRequest.class);
    verify(mojo.sourceCodeGenerator).generate(captor.capture());
    var actualRequest = captor.getValue();

    assertSoftly(softly -> softly
        .assertThat(actualRequest.getProtocPlugins())
        .containsAll(binaryMavenPlugins)
        .containsAll(binaryPathPlugins)
        .containsAll(binaryUrlPlugins)
        .containsAll(jvmMavenPlugins)
        .containsAll(plugins));
  }

  @DisplayName("when protoc is null, expect an exception to be raised")
  @Test
  @UsesSystemProperties
  @SuppressWarnings("NullAway")
  void whenProtocDistributionNullExpectExceptionToBeRaised() {
    // Given
    mojo.protoc = null;

    // Then
    assertThatException()
        .isThrownBy(mojo::execute)
        .isInstanceOf(NullPointerException.class)
        .withMessage("<protoc/> has not been set");
  }

  @DisplayName("when protobuf.compiler.version is set, expect that to be used")
  @Test
  @UsesSystemProperties
  void whenProtocDistributionVersionSetInSystemPropertiesExpectThatToBeUsed() throws Throwable {
    // Given
    var expectedDistribution = mock(BinaryMavenProtocDistribution.class);
    when(mojo.protocDistributionConverter.fromString(any()))
        .thenReturn(expectedDistribution);

    mojo.protoc = mock(BinaryMavenProtocDistribution.class);
    System.setProperty("protobuf.compiler.version", "4.5.6");

    // When
    mojo.execute();

    // Then
    var captor = ArgumentCaptor.forClass(GenerationRequest.class);
    verify(mojo.sourceCodeGenerator).generate(captor.capture());
    var actualRequest = captor.getValue();
    assertThat(actualRequest.getProtocDistribution())
        .isSameAs(expectedDistribution);

    verify(mojo.protocDistributionConverter).fromString("4.5.6");
    verifyNoMoreInteractions(mojo.protocDistributionConverter);
  }

  @DisplayName("when protobuf.compiler.version is not set, expect the parameter to be used")
  @Test
  @UsesSystemProperties
  void whenProtocDistributionVersionNotSetInSystemPropertiesExpectParameterToBeUsed()
      throws Throwable {

    // Given
    var protoc = mock(BinaryMavenProtocDistribution.class);
    mojo.protoc = protoc;

    // When
    mojo.execute();

    // Then
    var captor = ArgumentCaptor.forClass(GenerationRequest.class);
    verify(mojo.sourceCodeGenerator).generate(captor.capture());
    var actualRequest = captor.getValue();
    assertThat(actualRequest.getProtocDistribution())
        .isSameAs(protoc);
  }

  @DisplayName("registerAsCompilationRoot is set to the specified value")
  @ValueSource(booleans = {true, false})
  @ParameterizedTest(name = "for {0}")
  void registerAsCompilationRootIsSetToSpecifiedValue(boolean value) throws Throwable {
    mojo.registerAsCompilationRoot = value;

    // When
    mojo.execute();

    // Then
    var captor = ArgumentCaptor.forClass(GenerationRequest.class);
    verify(mojo.sourceCodeGenerator).generate(captor.capture());
    var actualRequest = captor.getValue();
    assertThat(actualRequest.isRegisterAsCompilationRoot()).isEqualTo(value);
  }

  @DisplayName("when sanctionedExecutablePath is provided, expect it to be set on the request")
  @Test
  void whenSanctionedExecutablePathIsProvidedExpectItToBeSetOnTheRequest(
      @TempDir Path tempDir
  ) throws Throwable {
    var expectedSanctionedExecutablePath = Files.createDirectories(tempDir.resolve("some-path"));
    // Given
    mojo.sanctionedExecutablePath = expectedSanctionedExecutablePath;

    // When
    mojo.execute();

    // Then
    var captor = ArgumentCaptor.forClass(GenerationRequest.class);
    verify(mojo.sourceCodeGenerator).generate(captor.capture());
    var actualRequest = captor.getValue();
    assertThat(actualRequest.getSanctionedExecutablePath())
        .isEqualTo(expectedSanctionedExecutablePath);
  }

  @DisplayName("when sanctionedExecutablePath is not provided, expect no path to be used")
  @Test
  void whenSanctionedExecutablePathNotProvidedExpectNoFileToBeUsed() throws Throwable {
    // Given
    mojo.sanctionedExecutablePath = null;

    // When
    mojo.execute();

    // Then
    var captor = ArgumentCaptor.forClass(GenerationRequest.class);
    verify(mojo.sourceCodeGenerator).generate(captor.capture());
    var actualRequest = captor.getValue();
    assertThat(actualRequest.getSanctionedExecutablePath())
        .isNull();
  }

  @DisplayName("when sourceDependencies is null, expect an empty list in the request")
  @NullAndEmptySource
  @ParameterizedTest(name = "when {0}")
  void whenSourceDependenciesNullExpectEmptyListInRequest(
      @Nullable List<MavenDependencyBean> dependencies
  ) throws Throwable {
    // Given
    mojo.sourceDependencies = dependencies;

    // When
    mojo.execute();

    // Then
    var captor = ArgumentCaptor.forClass(GenerationRequest.class);
    verify(mojo.sourceCodeGenerator).generate(captor.capture());
    var actualRequest = captor.getValue();
    assertThat(actualRequest.getSourceDependencies()).isEmpty();
  }

  @DisplayName("when sourceDependencies is provided, expect the dependencies in the request")
  @Test
  void whenSourceDependenciesProvidedExpectDependenciesInRequest() throws Throwable {
    // Given
    List<MavenDependencyBean> plugins = mock();
    mojo.sourceDependencies = plugins;

    // When
    mojo.execute();

    // Then
    var captor = ArgumentCaptor.forClass(GenerationRequest.class);
    verify(mojo.sourceCodeGenerator).generate(captor.capture());
    var actualRequest = captor.getValue();
    assertThat(actualRequest.getSourceDependencies()).isSameAs(plugins);
  }

  @DisplayName("when sourceDescriptorDependencies is null, expect an empty list in the request")
  @NullAndEmptySource
  @ParameterizedTest(name = "when {0}")
  void whenSourceDescriptorDependenciesNullExpectEmptyListInRequest(
      @Nullable List<MavenDependencyBean> dependencies
  ) throws Throwable {
    // Given
    mojo.sourceDescriptorDependencies = dependencies;

    // When
    mojo.execute();

    // Then
    var captor = ArgumentCaptor.forClass(GenerationRequest.class);
    verify(mojo.sourceCodeGenerator).generate(captor.capture());
    var actualRequest = captor.getValue();
    assertThat(actualRequest.getSourceDescriptorDependencies()).isEmpty();
  }

  @DisplayName(
      "when sourceDescriptorDependencies is provided, expect the dependencies in the request"
  )
  @Test
  void whenSourceDescriptorDependenciesProvidedExpectDependenciesInRequest() throws Throwable {
    // Given
    List<MavenDependencyBean> plugins = mock();
    mojo.sourceDescriptorDependencies = plugins;

    // When
    mojo.execute();

    // Then
    var captor = ArgumentCaptor.forClass(GenerationRequest.class);
    verify(mojo.sourceCodeGenerator).generate(captor.capture());
    var actualRequest = captor.getValue();
    assertThat(actualRequest.getSourceDescriptorDependencies()).isSameAs(plugins);
  }

  @DisplayName("when sourceDescriptorPaths is null, expect an empty collection in the request")
  @NullAndEmptySource
  @ParameterizedTest(name = "when {0}")
  void whenSourceDescriptorPathsNullExpectEmptyCollectionInRequest(
      @Nullable List<Path> sourceDescriptorPaths
  ) throws Throwable {
    // Given
    mojo.sourceDescriptorPaths = sourceDescriptorPaths;

    // When
    mojo.execute();

    // Then
    var captor = ArgumentCaptor.forClass(GenerationRequest.class);
    verify(mojo.sourceCodeGenerator).generate(captor.capture());
    var actualRequest = captor.getValue();
    assertThat(actualRequest.getSourceDescriptorPaths()).isEmpty();
  }

  @DisplayName("when sourceDescriptorPaths is provided, expect the paths in the request")
  @Test
  void whenSourceDescriptorPathsProvidedExpectPathsInRequest(
      @TempDir Path someTempDir
  ) throws Throwable {
    // Given
    var path1 = Files.createDirectories(someTempDir.resolve("foo").resolve("bar"));
    var path2 = Files.createDirectories(someTempDir.resolve("do").resolve("ray"));
    var path3 = Files.createDirectories(someTempDir.resolve("aaa").resolve("bbb"));

    mojo.sourceDescriptorPaths = List.of(path1, path2, path3);

    // When
    mojo.execute();

    // Then
    var captor = ArgumentCaptor.forClass(GenerationRequest.class);
    verify(mojo.sourceCodeGenerator).generate(captor.capture());
    var actualRequest = captor.getValue();
    assertThat(actualRequest.getSourceDescriptorPaths()).containsExactly(path1, path2, path3);
  }

  @DisplayName("missing sourceDescriptorPaths are not provided to the generator")
  @Test
  void missingSourceDescriptorPathsAreNotProvidedToTheGenerator(
      @TempDir Path someTempDir
  ) throws Throwable {
    // Given
    var dir1 = Files.createDirectories(someTempDir.resolve("foo").resolve("bar"));
    var dir2 = Files.createDirectories(someTempDir.resolve("aaa").resolve("bbb"));

    var path1 = Files.createFile(dir1.resolve("file1.binpb"));
    var path2 = someTempDir.resolve("do").resolve("ray");
    var path3 = Files.createFile(dir2.resolve("file2.binpb"));
    assertThat(path2).doesNotExist();

    mojo.sourceDescriptorPaths = List.of(path1, path2, path3);

    // When
    mojo.execute();

    // Then
    var captor = ArgumentCaptor.forClass(GenerationRequest.class);
    verify(mojo.sourceCodeGenerator).generate(captor.capture());
    var actualRequest = captor.getValue();
    assertThat(actualRequest.getSourceDescriptorPaths()).containsExactly(path1, path3);
  }

  @DisplayName("when sourceDirectories is null, expect the default path in the request")
  @NullAndEmptySource
  @ParameterizedTest(name = "when {0}")
  void whenSourceDirectoriesNullExpectDefaultValueInRequest(
      @Nullable List<Path> sourceDirectories
  ) throws Throwable {
    // Given
    mojo.sourceDirectories = sourceDirectories;
    for (var directory : expectedDefaultSourceDirectories()) {
      Files.createDirectories(directory);
    }

    // When
    mojo.execute();

    // Then
    var captor = ArgumentCaptor.forClass(GenerationRequest.class);
    verify(mojo.sourceCodeGenerator).generate(captor.capture());
    var actualRequest = captor.getValue();
    assertThat(actualRequest.getSourceDirectories()).containsExactlyElementsOf(
        expectedDefaultSourceDirectories());
  }

  @DisplayName("when sourceDirectories is provided, expect the paths in the request")
  @Test
  void whenSourceDirectoriesProvidedExpectPathsInRequest(
      @TempDir Path someTempDir
  ) throws Throwable {
    // Given
    var path1 = Files.createDirectories(someTempDir.resolve("foo").resolve("bar"));
    var path2 = Files.createDirectories(someTempDir.resolve("do").resolve("ray"));
    var path3 = Files.createDirectories(someTempDir.resolve("aaa").resolve("bbb"));

    mojo.sourceDirectories = List.of(path1, path2, path3);

    // When
    mojo.execute();

    // Then
    var captor = ArgumentCaptor.forClass(GenerationRequest.class);
    verify(mojo.sourceCodeGenerator).generate(captor.capture());
    var actualRequest = captor.getValue();
    assertThat(actualRequest.getSourceDirectories()).containsExactly(path1, path2, path3);
  }

  @DisplayName("missing sourceDirectories are not provided to the generator")
  @Test
  void missingSourceDirectoriesAreNotProvidedToTheGenerator(
      @TempDir Path someTempDir
  ) throws Throwable {
    // Given
    var path1 = Files.createDirectories(someTempDir.resolve("foo").resolve("bar"));
    var path2 = someTempDir.resolve("do").resolve("ray");
    var path3 = Files.createDirectories(someTempDir.resolve("aaa").resolve("bbb"));
    assertThat(path2).doesNotExist();

    mojo.sourceDirectories = List.of(path1, path2, path3);

    // When
    mojo.execute();

    // Then
    var captor = ArgumentCaptor.forClass(GenerationRequest.class);
    verify(mojo.sourceCodeGenerator).generate(captor.capture());
    var actualRequest = captor.getValue();
    assertThat(actualRequest.getSourceDirectories()).containsExactly(path1, path3);
  }

  @DisplayName("languages are enabled and disabled as expected")
  @MethodSource("languageEnablingCases")
  @ParameterizedTest(name = "when {0} is true, then expect {2} to be enabled")
  void languagesAreEnabledAndDisabledAsExpected(
      String description,
      Consumer<A> languageConfigurer,
      Set<Language> expectedEnabledLanguages
  ) throws Throwable {
    // Given
    languageConfigurer.accept(mojo);

    // When
    mojo.execute();

    // Then
    var captor = ArgumentCaptor.forClass(GenerationRequest.class);
    verify(mojo.sourceCodeGenerator).generate(captor.capture());
    var actualRequest = captor.getValue();

    assertThat(actualRequest.getEnabledLanguages())
        .withFailMessage("expected languages were not enabled when setting %s", description)
        .containsExactlyInAnyOrderElementsOf(expectedEnabledLanguages);
  }

  static Stream<Arguments> languageEnablingCases() {
    return Stream.of(
        // Base cases
        arguments(
            "nothing",
            consumer(),
            EnumSet.noneOf(Language.class)
        ),
        arguments(
            "Java",
            consumer(a -> a.javaEnabled = true),
            EnumSet.of(Language.JAVA)
        ),
        arguments(
            "Kotlin",
            consumer(a -> a.kotlinEnabled = true),
            EnumSet.of(Language.KOTLIN)
        ),
        arguments(
            "Python",
            consumer(a -> a.pythonEnabled = true),
            EnumSet.of(Language.PYTHON)
        ),
        arguments(
            "Python stubs",
            consumer(a -> a.pythonStubsEnabled = true),
            EnumSet.of(Language.PYTHON_STUBS)
        ),
        arguments(
            "Ruby",
            consumer(a -> a.rubyEnabled = true),
            EnumSet.of(Language.RUBY)
        ),
        // Combined cases
        arguments(
            "Java, Kotlin",
            consumer(a -> {
              a.javaEnabled = true;
              a.kotlinEnabled = true;
            }),
            EnumSet.of(Language.JAVA, Language.KOTLIN)
        ),
        arguments(
            "Python, PYI",
            consumer(a -> {
              a.pythonEnabled = true;
              a.pythonStubsEnabled = true;
            }),
            EnumSet.of(Language.PYTHON, Language.PYTHON_STUBS)
        ),
        arguments(
            "everything",
            consumer(a -> {
              a.javaEnabled = true;
              a.kotlinEnabled = true;
              a.pythonEnabled = true;
              a.pythonStubsEnabled = true;
              a.rubyEnabled = true;
            }),
            EnumSet.allOf(Language.class)
        )
    );
  }

  static Consumer<AbstractGenerateMojo> consumer() {
    return a -> {
      // Do nothing.
    };
  }

  static Consumer<AbstractGenerateMojo> consumer(Consumer<AbstractGenerateMojo> consumer) {
    return consumer;
  }

  static ProtobufBuildOrchestrator sourceCodeGenerator(GenerationResult result) throws Throwable {
    var sourceCodeGenerator = mock(
        ProtobufBuildOrchestrator.class,
        withSettings().strictness(Strictness.LENIENT)
    );
    when(sourceCodeGenerator.generate(any()))
        .thenReturn(result);
    return sourceCodeGenerator;
  }

  static ProtobufBuildOrchestrator erroringSourceCodeGenerator(Throwable cause) throws Throwable {
    var sourceCodeGenerator = mock(
        ProtobufBuildOrchestrator.class,
        withSettings().strictness(Strictness.LENIENT)
    );
    when(sourceCodeGenerator.generate(any()))
        .thenThrow(cause);
    return sourceCodeGenerator;
  }

  @SuppressWarnings("unused")  // Used by JUnit, IntelliJ bug is marking as unused.
  static Stream<Set<String>> scopeCombinations() {
    return combinations("compile", "runtime", "test", "provided", "system");
  }

  @SafeVarargs
  @SuppressWarnings("vararg")
  static <T> Stream<Set<T>> combinations(T... items) {
    if (items.length >= Integer.SIZE) {
      throw new IllegalArgumentException("Too many items!");
    }

    var spliterator = new AbstractSpliterator<Set<T>>(items.length, AbstractSpliterator.SIZED) {
      private final int maxBitField = (1 << items.length) - 1;
      private int bitfield = 0x0;

      @Override
      public boolean tryAdvance(Consumer<? super Set<T>> action) {
        if (bitfield >= maxBitField) {
          return false;
        }

        bitfield += 1;
        var combination = new HashSet<T>();
        for (var bit = 0; bit < items.length; ++bit) {
          if ((bitfield & (1 << bit)) > 0) {
            combination.add(items[bit]);
          }
        }
        action.accept(combination);
        return true;
      }
    };
    return StreamSupport.stream(spliterator, false);
  }
}

