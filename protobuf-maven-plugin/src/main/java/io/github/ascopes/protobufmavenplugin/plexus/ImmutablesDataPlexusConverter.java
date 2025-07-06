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
package io.github.ascopes.protobufmavenplugin.plexus;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.WeakHashMap;
import javax.inject.Named;
import javax.inject.Singleton;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ConfigurationListener;
import org.codehaus.plexus.component.configurator.converters.ParameterizedConfigurationConverter;
import org.codehaus.plexus.component.configurator.converters.basic.AbstractBasicConverter;
import org.codehaus.plexus.component.configurator.converters.lookup.ConverterLookup;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.immutables.datatype.Datatype;
import org.jspecify.annotations.Nullable;

/**
 * Converter for Plexus components that can build a generated "immutables" value type, based
 * on the interface it was derived from.
 *
 * <p>Note that all objects must be annotated with both {@link org.immutables.value.Value}
 * <strong>and</strong> {@link org.immutables.datatype.Data}, otherwise deserialization will fail at
 * runtime.
 *
 * @author Ashley Scopes
 * @since TBC
 */
@Named
@Singleton
final class ImmutablesDataPlexusConverter extends AbstractBasicConverter {
  private final Map<Class<Object>, Datatype<Object>> knownDatatypes;

  ImmutablesDataPlexusConverter() {
    // Weak hashmap keys will be deregistered upon classloader destruction safely.
    knownDatatypes = Collections.synchronizedMap(new WeakHashMap<>());
  }

  @Override
  public boolean canConvert(Class<?> cls) {
    return datatypeFor(cls).isPresent();
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
    var datatype = datatypeFor(type)
        .orElseThrow(() -> new NoSuchElementException("No datatype converter " + type.getName()));

    var builder = datatype.builder();
    for (var child : configuration.getChildren()) {
      consumeChild(builder, child, lookup, datatype, loader, evaluator, listener);
    }

    return builder.build();
  }

  private void consumeChild(
      Datatype.Builder<Object> builder,
      PlexusConfiguration child,
      ConverterLookup lookup,
      Datatype<?> datatype,
      @Nullable ClassLoader loader,
      ExpressionEvaluator evaluator,
      @Nullable ConfigurationListener listener
  ) throws ComponentConfigurationException {
    try {
      @SuppressWarnings("unchecked")
      var feature = (Datatype.Feature<Object, Object>) datatype.feature(child.getName());
      var valueType = feature.type();
      var rawType = rawTypeOf(valueType);

      var converter = lookup.lookupConverterForType(rawType);

      Object value;

      if (converter instanceof ParameterizedConfigurationConverter parameterizedConverter) {
        var parameterizedType = (ParameterizedType) valueType;
        value = parameterizedConverter.fromConfiguration(
            lookup,
            child,
            rawType,
            parameterizedType.getActualTypeArguments(),
            rawType.getEnclosingClass(),
            loader,
            evaluator,
            listener
        );
      } else {
        value = converter.fromConfiguration(
            lookup,
            child,
            rawType,
            rawType.getEnclosingClass(),
            loader,
            evaluator,
            listener
        );
      }

      builder.set(feature, value);

    } catch (NoSuchElementException ex) {
      throw new ComponentConfigurationException(
          "No attribute " + child.getName() + " exists for " + datatype.name(),
          ex
      );
    }
  }

  private Optional<Datatype<Object>> datatypeFor(Class<?> cls) {
    if (cls.isPrimitive() || cls.getClassLoader() == null) {
      return Optional.empty();
    }

    // Horrible generic voodoo that probably is not safe, but the APIs have conflicting types
    // and the compiler is not smart enough to help us.
    @SuppressWarnings("unchecked")
    var castCls = (Class<Object>) cls;

    var datatype = knownDatatypes.computeIfAbsent(castCls, ignored -> {
      var loader = cls.getClassLoader();
      var outerClsName = cls.getPackageName() + ".Datatypes_" + cls.getSimpleName();

      try {
        var outerCls = loader.loadClass(outerClsName);
        var method = outerCls.getMethod("_" + cls.getSimpleName());

        @SuppressWarnings("unchecked")
        var result = (Datatype<Object>) method.invoke(null);

        return result;
      } catch (ClassNotFoundException ex) {
        return null;
      } catch (ReflectiveOperationException ex) {
        throw new IllegalStateException(
            "Failed to find datatype for " + cls.getName() + ": " + ex,
            ex
        );
      }
    });

    return Optional.ofNullable(datatype);
  }

  private static Class<?> rawTypeOf(Type type) {
    return type instanceof ParameterizedType parameterizedType
        ? rawTypeOf(parameterizedType.getRawType())
        // Assumption: this cannot ever be a wildcard or union type.
        : (Class<?>) type;
  }
}
