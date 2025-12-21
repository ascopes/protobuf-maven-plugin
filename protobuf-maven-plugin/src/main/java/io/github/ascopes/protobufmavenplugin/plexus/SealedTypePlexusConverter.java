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

import static java.util.function.Predicate.not;

import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;
import java.util.stream.Collectors;
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
 * non-sealed implementations of the sealed type that use the {@link KindHint} annotation, storing
 * them in a mapping.
 *
 * <p>If the user-provided kind matches a known kind from our indexing operation, we will use that
 * as the implementation class and look for a concrete converter from the ConverterLookup we have
 * been passed.
 *
 * <p>If no matching kind is found, we raise an error back to the user.
 *
 * <p>All indexed types are stored in a weak-referenced mapping internally, such that
 * garbage collection of their classworld releases the underlying data for garbage collection.
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

  private final Map<Class<?>, Map<String, Class<?>>> kindMappings;

  SealedTypePlexusConverter() {
    kindMappings = Collections.synchronizedMap(new WeakHashMap<>());
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
    var kind = Optional
        .ofNullable(configuration.getAttribute("kind"))
        .filter(not(String::isEmpty))
        .orElseThrow(() -> new ComponentConfigurationException(
            configuration,
            "Missing \"kind\" attribute. Valid kinds are: " + getValidKindsFor(type)
        ));

    var impl = Optional.of(getKindMappingFor(type))
        .map(mapping -> mapping.get(kind))
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
        .keySet()
        .stream()
        .map(kind -> "\"" + kind + "\"")
        .sorted()
        .collect(Collectors.joining(", "));
  }

  private Map<String, Class<?>> getKindMappingFor(Class<?> base) {
    return kindMappings.computeIfAbsent(base, SealedTypePlexusConverter::computeKindMappingFor);
  }

  private static Map<String, Class<?>> computeKindMappingFor(Class<?> base) {
    var mapping = new HashMap<String, Class<?>>();
    var queue = new ArrayDeque<Class<?>>();
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
          queue.push(permittedSubtype);
        }
      } else {
        var kind = next.getAnnotation(KindHint.class);

        if (kind != null) {
          log.trace(
              "Found concrete kind for base \"{}\": \"{}\" will map to \"{}\"",
              base.getName(),
              kind.kind(),
              kind.implementation().getName()
          );

          mapping.put(kind.kind(), kind.implementation());
        }
      }
    }

    return Collections.unmodifiableMap(mapping);
  }

}
