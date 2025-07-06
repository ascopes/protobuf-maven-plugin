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
package io.github.ascopes.protobufmavenplugin.utils;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ConfigurationListener;
import org.codehaus.plexus.component.configurator.converters.basic.AbstractBasicConverter;
import org.codehaus.plexus.component.configurator.converters.lookup.ConverterLookup;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.immutables.data.Datatype;
import org.immutables.data.Datatype.Builder;
import org.immutables.data.Datatype.Feature;
import org.jspecify.annotations.Nullable;

/**
 * Converter for Plexus components that can build a generated "immutables" value type, based
 * on the interface it was derived from.
 *
 * <p>Note that all objects must be annotated with both {@link org.immutables.value.Value}
 * <strong>and</strong> {@link org.immutables.data.Data}, otherwise deserialization will fail at
 * runtime.
 *
 * @author Ashley Scopes
 * @since TBC
 */
public final class ImmutablesDataPlexusConverter extends AbstractBasicConverter {
  private final Map<Class<?>, Datatype<?>> knownDatatypes;

  public ImmutablesDataPlexusConverter() {
    knownDatatypes = new ConcurrentHashMap<>();
  }

  @Override
  public boolean canConvert(Class<?> cls) {
    try {
      datatypeFor(cls.getClassLoader(), cls);
      return true;
    } catch (NoSuchElementException ex) {
      return false;
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public Object fromConfiguration(
      ConverterLookup lookup,
      PlexusConfiguration configuration,
      Class<?> type,
      @Nullable Class<?> enclosingType,
      ClassLoader loader,
      ExpressionEvaluator evaluator,
      @Nullable ConfigurationListener listener
  ) throws ComponentConfigurationException {
    var datatype = datatypeFor(loader, type);

    var builder = (Builder<Object>) datatype.builder();
    for (var child : configuration.getChildren()) {
      try {
        var feature = (Feature<Object, Object>) datatype.feature(child.getName());
        var valueType = feature.type().getRawType();
        var converter = lookup.lookupConverterForType(valueType);
        var value = converter.fromConfiguration(
            lookup,
            child,
            valueType,
            null,
            loader,
            evaluator,
            listener
        );
        builder.set(feature, value);
      } catch (NoSuchElementException ex) {
        throw new ComponentConfigurationException(
            "No attribute " + child.getName() + " exists for " + type.getSimpleName(),
            ex
        );
      }
    }

    return builder.build();
  }

  private <T> Datatype<T> datatypeFor(
      ClassLoader classLoader,
      Class<T> cls
  ) {
    @SuppressWarnings("unchecked")
    var datatype = (Datatype<T>) knownDatatypes.computeIfAbsent(cls, ignored -> {
      var outerClsName = cls.getPackageName() + ".Datatypes_" + cls.getSimpleName();

      try {
        var outerCls = classLoader.loadClass(outerClsName);
        return (Datatype<?>) outerCls.getMethod("_" + cls.getSimpleName()).invoke(null);
      } catch (ClassNotFoundException ex) {
        var newEx = new NoSuchElementException("No datatype converter " + outerClsName);
        //noinspection UnnecessaryInitCause - only JDK15 and newer
        newEx.initCause(ex);
        throw newEx;

      } catch (ReflectiveOperationException ex) {
        throw new IllegalStateException("Failed to find datatype for " + cls.getName(), ex);
      }
    });

    return datatype;
  }
}
