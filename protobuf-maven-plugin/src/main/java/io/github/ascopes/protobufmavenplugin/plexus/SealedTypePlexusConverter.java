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

import java.lang.reflect.Method;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.inject.Named;
import org.apache.maven.execution.scope.MojoExecutionScoped;
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
 * non-sealed implementations of the sealed type that use the {@link KindHint} annotation, storing
 * them in a mapping.
 *
 * <p>If the user-provided kind matches a known kind from our indexing operation, we will use that
 * as the implementation class and look for a concrete converter from the ConverterLookup we have
 * been passed.
 *
 * <p>If no matching kind is found, we raise an error back to the user.
 *
 * <p>If no kind is provided, and a {@link FromString} annotation is present on a method in the
 * base type, this method will be invoked to convert a Plexus string value into an instance of the
 * base type.
 *
 * <p>This is threadsafe.
 *
 * @author Ashley Scopes
 * @since 4.1.0
 */
@Description("Plexus converter that finds the most appropriate implementation of a sealed type")
@MojoExecutionScoped
@Named
final class SealedTypePlexusConverter extends AbstractBasicConverter {

  private static final Logger log = LoggerFactory.getLogger(SealedTypePlexusConverter.class);

  private final Map<Class<?>, KindMapping<?>> kindMappings;

  SealedTypePlexusConverter() {
    kindMappings = new HashMap<>();
  }

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
    var kindMapping = getKindMappingFor(type);

    var kind = Optional
        .ofNullable(configuration.getAttribute("kind"))
        .filter(not(String::isEmpty))
        .orElse(null);

    // getChildCount is 0 if we have no nested attributes. Otherwise, "getValue" could return the
    // XML structure directly.
    if (configuration.getChildCount() == 0) {
      return fromString(configuration, evaluator, kind, kindMapping.fromStringHandle());
    } else {
      // If we also merged in a value somewhere, discard it. Don't allow things like
      //
      // <protoc kind="binary-maven">
      //   1.2.3
      //   <version>4.5.6</version>
      // </protoc>
      //
      // as this just produces total nonsense errors that make zero sense.
      //
      // This can occur if the parent sets one format (e.g. a raw string value) but then a child
      // tries to set an object instead. Plexus messes up the formatting and defaults to treating
      // an object as a string rather than an object with attributes by default. This causes a
      // failure as injecting a string value directly into an object instance leads to it trying
      // to call a non-existent `public static T set(String value)`. Remember Maven will MERGE
      // parent and child POM configurations recursively by default, not override them.
      //
      // This was 30 minutes of my life I may never get back...
      configuration.setValue(null);

      return fromAttributes(lookup, configuration, type, evaluator, listener, kind, kindMapping);
    }
  }

  private Object fromString(
      PlexusConfiguration configuration,
      ExpressionEvaluator evaluator,
      @Nullable String kind,
      @Nullable FromStringHandle<?> fromStringHandle
  ) throws ComponentConfigurationException {
    if (kind != null) {
      throw new ComponentConfigurationException(
          configuration,
          "Cannot set a string value with a kind attribute"
      );
    }

    if (fromStringHandle == null) {
      throw new ComponentConfigurationException(
          configuration,
          "Cannot set a string value on this type of attribute"
      );
    }

    // Expand any interpolated values, then emit the string.
    var interpolatedValue = (String) fromExpression(configuration, evaluator, String.class);
    return fromStringHandle.call(configuration, interpolatedValue);
  }

  private Object fromAttributes(
      ConverterLookup lookup,
      PlexusConfiguration configuration,
      Class<?> type,
      ExpressionEvaluator evaluator,
      @Nullable ConfigurationListener listener,
      @Nullable String kind,
      KindMapping<?> kindMapping
  ) throws ComponentConfigurationException {
    if (kind == null) {
      throw new ComponentConfigurationException(
          configuration,
          "Missing \"kind\" attribute. Valid kinds are: " + getValidKindsFor(type)
      );
    }

    var impl = Optional.ofNullable(kindMapping.kinds().get(kind))
        .orElseThrow(() -> new ComponentConfigurationException(
            configuration,
            "Invalid kind \"" + kind + "\" specified. Valid kinds are: " + getValidKindsFor(type)
        ));

    return lookup.lookupConverterForType(impl).fromConfiguration(
        lookup,
        configuration,
        impl,
        impl.getEnclosingClass(),
        impl.getClassLoader(),
        evaluator,
        listener
    );
  }

  private String getValidKindsFor(Class<?> base) {
    return getKindMappingFor(base)
        .kinds()
        .keySet()
        .stream()
        .map(kind -> "\"" + kind + "\"")
        .sorted()
        .collect(Collectors.joining(", "));
  }

  private synchronized KindMapping<?> getKindMappingFor(Class<?> base) {
    return kindMappings.computeIfAbsent(base, SealedTypePlexusConverter::computeKindMappingFor);
  }

  private static <T> KindMapping<T> computeKindMappingFor(Class<T> base) {
    return new KindMapping<>(
        base,
        discoverKindsFor(base),
        discoverFromStringFor(base).orElse(null)
    );
  }

  private static <T> Map<String, Class<? extends T>> discoverKindsFor(Class<T> base) {
    var mapping = new HashMap<String, Class<? extends T>>();
    var queue = new ArrayDeque<Class<? extends T>>();
    queue.push(base);

    while (!queue.isEmpty()) {
      var next = queue.removeFirst();

      if (next.isSealed()) {
        log.trace(
            "Found sealed type \"{}\" (base is \"{}\"), adding children to queue",
            base.getName(),
            next.getName()
        );

        for (var permittedSubtype : next.getPermittedSubclasses()) {
          queue.push(permittedSubtype.asSubclass(base));
        }
      } else {
        var kind = next.getAnnotation(KindHint.class);

        if (kind != null) {
          log.trace(
              "Found concrete kind for base \"{}\": \"{}\" will map to \"{}\"",
              base.getName(),
              kind.value(),
              next.getName()
          );

          mapping.put(kind.value(), next);
        }
      }
    }

    return Collections.unmodifiableMap(mapping);
  }

  private static <T> Optional<FromStringHandle<T>> discoverFromStringFor(Class<T> base) {
    return Stream.of(base.getDeclaredMethods())
        .filter(m -> m.isAnnotationPresent(FromString.class))
        .map(m -> handleFromMethod(base, m))
        .peek(m -> log.debug("found @FromString method {} on {}", m, base.getName()))
        .findFirst();
  }

  static <T> FromStringHandle<T> handleFromMethod(Class<T> base, Method method) {
    return (configuration, value) -> {
      try {
        return base.cast(method.invoke(null, value));
      } catch (ReflectiveOperationException ex) {
        throw new ComponentConfigurationException(
            configuration,
            "Failed to parse attribute string value: " + requireNonNullElse(ex.getCause(), ex),
            ex
        );
      }
    };
  }

  private record KindMapping<T>(
      Class<T> base,
      Map<String, Class<? extends T>> kinds,
      @Nullable FromStringHandle<T> fromStringHandle
  ) {

  }

  @FunctionalInterface
  private interface FromStringHandle<T> {

    T call(PlexusConfiguration configuration, String value) throws ComponentConfigurationException;
  }
}
