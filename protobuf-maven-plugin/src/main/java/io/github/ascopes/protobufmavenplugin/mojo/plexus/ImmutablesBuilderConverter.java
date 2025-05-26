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

import static java.util.function.Predicate.not;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ConfigurationListener;
import org.codehaus.plexus.component.configurator.converters.basic.AbstractBasicConverter;
import org.codehaus.plexus.component.configurator.converters.lookup.ConverterLookup;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;

/**
 * Converter that consumes an {@code immutables}-annotated interface, determining the
 * {@link org.immutables.value.Value.Immutable}-generated implementation type and constructs the
 * object value from that implementation's builder.
 *
 * <p>Somewhat similar conceptually to how
 * {@link org.codehaus.plexus.component.configurator.converters.composite.ObjectWithFieldsConverter}
 * works, but this works on immutable builders, making inferences about the implementation type at
 * runtime. The Plexus-provided converter can only handle "Java Bean" objects of the exact concrete
 * type that is provided in the enclosing class at compile time.
 *
 * <p>This implementation allows us to use {@link org.immutables.value.Value.Immutable}-annotated
 * interfaces in the Maven plugin parameters without hardcoding a reference to a mutable copy (which
 * would require us to generate two implentations for... one mutable and one immutable).
 *
 * @author Ashley Scopes
 * @since 3.3.0
 */
final class ImmutablesBuilderConverter extends AbstractBasicConverter {

  @Override
  public boolean canConvert(Class<?> iface) {
    try {
      getBuilderConstructor(getImmutablesClassFor(iface));
      return true;
    } catch (ReflectiveOperationException ex) {
      return false;
    }
  }

  @Override
  public Object fromConfiguration(
      ConverterLookup lookup,
      PlexusConfiguration configuration,
      Class<?> ifaceCls,
      Class<?> enclosingCls,
      ClassLoader loader,
      ExpressionEvaluator evaluator,
      ConfigurationListener listener
  ) throws ComponentConfigurationException {
    try {
      var builder = getBuilderConstructor(getImmutablesClassFor(ifaceCls)).invoke(null);
      var builderCls = builder.getClass();

      for (var childConfiguration : configuration.getChildren()) {
        var childName = fromXML(childConfiguration.getName());
        var descriptor = getAttributeDescriptor(ifaceCls, builderCls, childName);
        var childCls = getClassForImplementationHint(descriptor.type, childConfiguration, loader);

        var childValue = lookup.lookupConverterForType(childCls).fromConfiguration(
            lookup,
            childConfiguration,
            childCls,
            ifaceCls,
            loader,
            evaluator,
            listener
        );

        // No guarantee that we reuse the same object, we could create a whole new object, so
        // re-determine the type after each iteration.
        builder = descriptor.setter.invoke(builder, childValue);
        builderCls = builder.getClass();
      }

      // Cast ahead of time to catch any final issues.
      var builtObject = getBuilderBuildMethod(builderCls).invoke(builder);
      return ifaceCls.cast(builtObject);

    } catch (Exception ex) {
      throw new ComponentConfigurationException(
          "Failed to construct "
              + ifaceCls.getSimpleName() + " instance from attribute '"
              + configuration.getName() + "': "
              + ex.getMessage(),
          ex
      );
    }
  }

  private Class<?> getImmutablesClassFor(Class<?> ifaceCls) throws ClassNotFoundException {
    try {
      return Optional.ofNullable(ifaceCls.getClassLoader())
          .orElseGet(ClassLoader::getSystemClassLoader)
          .loadClass(ifaceCls.getPackageName() + ".Immutable" + ifaceCls.getSimpleName());
    } catch (ClassNotFoundException ex) {
      throw new ClassNotFoundException(
          "Unable to find 'immutables'-generated implementation for " + ifaceCls.getName(),
          ex
      );
    }
  }

  private Method getBuilderConstructor(Class<?> implCls) throws NoSuchMethodException {
    return Stream.of(implCls.getMethods())
        .filter(hasName("builder")
            .and(hasParameterCount(0))
            .and(isPublic())
            .and(isStatic()))
        .findFirst()
        .orElseThrow(noMethodFound("no .builder() method found in %s", implCls.getSimpleName()));
  }

  private Method getBuilderBuildMethod(Class<?> builderCls) throws NoSuchMethodException {
    return Stream.of(builderCls.getMethods())
        .filter(hasName("build")
            .and(hasParameterCount(0))
            .and(isPublic())
            .and(not(isStatic())))
        .findFirst()
        .orElseThrow(noMethodFound("no .build() method found in %s", builderCls.getSimpleName()));
  }

  private Setter getAttributeDescriptor(
      Class<?> ifaceCls,
      Class<?> builderCls,
      String name
  ) throws NoSuchMethodException {

    // Assumptions: if the attribute really is valid, we'll have a valid getter on the interface
    // class named get$$Name$$ or is$$Name$$, and we'll have a corresponding one-argument
    // setter named $$name$$, which consumes a valid type that can be assigned to the getter
    // return type. If none of these hold, we're likely touching other internals, so bail out.

    var noSuchMethodProvider = noMethodFound("no such attribute '%s'", name);

    var attribute = name.length() <= 1
        ? name.toUpperCase(Locale.ROOT)
        : name.substring(0, 1).toUpperCase(Locale.ROOT) + name.substring(1);

    var targetType = Stream.of(ifaceCls.getMethods())
        .filter(hasName("get" + attribute)
            .or(hasName("is" + attribute)))
        .filter(hasParameterCount(0)
            .and(isPublic())
            .and(not(isStatic())))
        .findFirst()
        .orElseThrow(noSuchMethodProvider)
        .getReturnType();

    var builderSetter = Stream.of(builderCls.getMethods())
        .filter(hasName(name)
            .and(hasParameterCount(1))
            .and(hasFirstArgumentAssignableFrom(targetType))
            .and(isPublic())
            .and(not(isStatic())))
        .findFirst()
        .orElseThrow(noSuchMethodProvider);

    return new Setter(targetType, builderSetter);
  }

  private static Supplier<NoSuchMethodException> noMethodFound(String message, Object... args) {
    return () -> new NoSuchMethodException(String.format(message, args));
  }

  private static Predicate<Method> hasName(String name) {
    return method -> name.equals(method.getName());
  }

  private static Predicate<Method> hasParameterCount(int count) {
    return method -> method.getParameterCount() == count;
  }

  private static Predicate<Method> hasFirstArgumentAssignableFrom(Class<?> type) {
    return method -> method.getParameterTypes()[0].isAssignableFrom(type);
  }

  private static Predicate<Method> isPublic() {
    return method -> Modifier.isPublic(method.getModifiers());
  }

  private static Predicate<Method> isStatic() {
    return method -> Modifier.isStatic(method.getModifiers());
  }

  private static class Setter {
    private final Class<?> type;
    private final Method setter;

    private Setter(Class<?> type, Method setter) {
      this.type = type;
      this.setter = setter;
    }
  }
}
