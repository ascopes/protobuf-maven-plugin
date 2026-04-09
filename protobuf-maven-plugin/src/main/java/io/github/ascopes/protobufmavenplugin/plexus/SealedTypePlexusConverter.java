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

import static java.util.Objects.requireNonNullElse;
import static java.util.function.Predicate.not;

import io.github.ascopes.protobufmavenplugin.utils.AnnotationProxy;
import io.github.ascopes.protobufmavenplugin.utils.StringUtils;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Named;
import javax.inject.Singleton;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ConfigurationListener;
import org.codehaus.plexus.component.configurator.converters.basic.AbstractBasicConverter;
import org.codehaus.plexus.component.configurator.converters.lookup.ConverterLookup;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.eclipse.sisu.Description;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Plexus converter for sealed types (as documented in "JEP-409 Sealed Classes").
 *
 * <p>If a given type is marked as being {@code sealed} and has at least one permitting
 * subclass or subinterface, then we deem it as being able to be converted.
 *
 * <p>Given a Plexus configuration and a known valid sealed class or interface, we will
 * first look for a "kind" attribute on the Plexus XML configuration that provides us an instruction
 * for which type to consume. If this attribute is not present, an exception is raised back to the
 * user instructing them to provide a valid attribute. If found, we will then recursively find all
 * implementations of the sealed type that use the {@link KindHint} annotation.
 *
 * <p>If the user-provided kind matches a known kind from our search operation, we will use that
 * as the implementation class and look for a concrete converter from the ConverterLookup we have
 * been passed.
 *
 * <p>If no matching kind is found, we raise an error back to the user.
 *
 * <p>If no kind is provided, and a {@link FromString} annotation is present on a method in the
 * base type, this method will be invoked to convert a Plexus string value into an instance of the
 * base type.
 *
 * <p>This does not cache any information, making it safe to life outside the scope of a single
 * Mojo execution, and free from the effects of crossing-over classloaders.
 *
 * <p>This is threadsafe.
 *
 * @author Ashley Scopes
 * @since 4.1.0
 */
@Description("Plexus converter that finds the most appropriate implementation of a sealed type")
@Named
@Singleton
final class SealedTypePlexusConverter extends AbstractBasicConverter {

  private static final Logger log = LoggerFactory.getLogger(SealedTypePlexusConverter.class);

  @Override
  public boolean canConvert(Class<?> type) {
    return Optional.of(type)
        .filter(Class::isSealed)
        .map(Class::getPermittedSubclasses)
        .map(List::of)
        .filter(not(List::isEmpty))
        .isPresent();
  }

  @Override
  public Object fromConfiguration(
      ConverterLookup lookup,
      PlexusConfiguration configuration,
      Class<?> type,
      @Nullable Class<?> enclosingType,
      @Nullable ClassLoader loader,
      ExpressionEvaluator evaluator,
      @Nullable ConfigurationListener listener
  ) throws ComponentConfigurationException {
    var kind = configuration.getAttribute("kind");

    if (configuration.getValue() != null) {
      if (kind == null) {
        return parseFromString(configuration, type, evaluator);
      }

      throw new ComponentConfigurationException(
          configuration,
          "Cannot set a string value with a kind attribute."
      );
    }

    // If we also merged in a value somewhere, discard it. Don't allow things like
    //
    // <protoc kind="binary-maven">
    //   1.2.3
    //   <version>4.5.6</version>
    // </protoc>
    //
    // This avoids spewing weird errors because we overrode a string value in the parent
    // POM with an object in the child POM. Plexus will default to merging these together
    // directly without discarding one or the other. Right now we cannot easily work out
    // which takes precedence to do this in a more intelligent way.
    configuration.setValue(null);

    return parseFromObject(kind, lookup, configuration, type, evaluator, listener);
  }

  private Object parseFromString(
      PlexusConfiguration configuration,
      Class<?> type,
      ExpressionEvaluator evaluator
  ) throws ComponentConfigurationException {
    var fromStringMethod = Stream.of(type.getDeclaredMethods())
        .filter(m -> AnnotationProxy.findAnnotation(FromString.class, m).isPresent())
        .peek(m -> log.debug("found @FromString method {} on {}", m, type.getName()))
        .findFirst()
        .orElseThrow(() -> missingKindAttribute(configuration, type));

    // Expand any interpolated values, then emit the string.
    var interpolatedValue = (String) fromExpression(configuration, evaluator, String.class);
    try {
      return fromStringMethod.invoke(null, interpolatedValue);
    } catch (ReflectiveOperationException ex) {
      throw new ComponentConfigurationException(
          configuration,
          "Failed to parse attribute string value: " + requireNonNullElse(ex.getCause(), ex),
          ex
      );
    }
  }

  private Object parseFromObject(
      @Nullable String kind,
      ConverterLookup lookup,
      PlexusConfiguration configuration,
      Class<?> type,
      ExpressionEvaluator evaluator,
      @Nullable ConfigurationListener listener
  ) throws ComponentConfigurationException {
    if (kind == null) {
      throw missingKindAttribute(configuration, type);
    }

    var implementation = findRequestedImplementation(type, kind)
        .orElseThrow(() -> new ComponentConfigurationException(
            configuration,
            "Invalid kind " + StringUtils.quoted(kind)
                + " specified. Valid kinds are: " + nameValidKinds(type)
                + "."
        ));

    return lookup.lookupConverterForType(implementation)
        .fromConfiguration(
            lookup,
            configuration,
            implementation,
            implementation.getEnclosingClass(),
            implementation.getClassLoader(),
            evaluator,
            listener
        );
  }

  private Optional<? extends Class<?>> findRequestedImplementation(Class<?> type, String kind) {
    return listKindedImplementations(type)
        .filter(kindPair -> kindPair.kind().equals(kind))
        .map(KindHint::implementation)
        .findFirst();
  }

  private String nameValidKinds(Class<?> type) {
    return listKindedImplementations(type)
        .map(KindHint::kind)
        .sorted()
        .map(StringUtils::quoted)
        .collect(Collectors.joining(", "));
  }

  private Stream<KindHint> listKindedImplementations(Class<?> type) {
    var thisKindHint = AnnotationProxy.findAnnotation(KindHint.class, type)
        .stream();

    var kindedSubtypes = Optional.ofNullable(type.getPermittedSubclasses())
        .stream()
        .flatMap(Stream::of)
        .flatMap(this::listKindedImplementations);

    return Stream.concat(thisKindHint, kindedSubtypes);
  }

  private ComponentConfigurationException missingKindAttribute(
      PlexusConfiguration configuration,
      Class<?> type
  ) {
    return new ComponentConfigurationException(
        configuration,
        "Missing required 'kind' annotation. Other values are not valid here. Valid kinds are "
            + nameValidKinds(type) + "."
    );
  }
}
