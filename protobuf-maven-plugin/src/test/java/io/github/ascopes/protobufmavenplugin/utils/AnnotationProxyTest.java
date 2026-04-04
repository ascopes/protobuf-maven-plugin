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
package io.github.ascopes.protobufmavenplugin.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.ByteArrayOutputStream;
import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("AnnotationProxy tests")
class AnnotationProxyTest {

  @DisplayName("nothing is returned if the annotation is not present")
  @Test
  void nothingIsReturnedIfTheAnnotationIsNotPresent() {
    // When
    var annotation = AnnotationProxy
        .findAnnotation(SomeAnnotation.class, SomeUnannotatedClass.class);

    // Then
    assertThat(annotation)
        .isEmpty();
  }

  @DisplayName("annotations are returned as-is if using the same classloader")
  @Test
  void annotationsReturnedAsIsIfUsingSameClassloader() {
    // When
    var annotation = AnnotationProxy
        .findAnnotation(SomeAnnotation.class, SomeAnnotatedClass.class);

    // Then
    assertThat(annotation)
        .get()
        .satisfies(
            a -> assertThat(a.foo()).isEqualTo("baz"),
            a -> assertThat(a.bar()).isEqualTo("bork")
        );
  }

  @DisplayName("annotations are proxied between classloaders")
  @SuppressWarnings("unchecked")
  @Test
  void annotationsProxiedBetweenClassloaders() throws Exception {
    // Given
    var classLoader1 = loadTestClassesInNewClassLoader();
    var classLoader2 = loadTestClassesInNewClassLoader();

    var classLoader1AnnotatedClass = classLoader1.loadClass(SomeAnnotatedClass.class.getName());
    var classLoader1Annotation = (Class<Annotation>) classLoader1
        .loadClass(SomeAnnotation.class.getName());
    var classLoader2AnnotatedClass = classLoader2.loadClass(SomeAnnotatedClass.class.getName());
    var classLoader2Annotation = (Class<Annotation>) classLoader2
        .loadClass(SomeAnnotation.class.getName());

    assertThat(classLoader1AnnotatedClass)
        .isNotEqualTo(classLoader2AnnotatedClass)
        .isNotEqualTo(SomeAnnotatedClass.class);
    assertThat(classLoader1Annotation)
        .isNotEqualTo(classLoader2Annotation)
        .isNotEqualTo(SomeAnnotation.class);

    // When
    var annotation = AnnotationProxy
        .findAnnotation(classLoader1Annotation, classLoader2AnnotatedClass);

    // Then
    assertThat(annotation)
        .get()
        .satisfies(
            a -> assertThat(classLoader1Annotation.getMethod("foo").invoke(a)).isEqualTo("baz"),
            a -> assertThat(classLoader1Annotation.getMethod("bar").invoke(a)).isEqualTo("bork")
        );
  }

  //
  // Test data
  //

  public static class SomeUnannotatedClass {
  }

  @SomeAnnotation(foo = "baz", bar = "bork")
  public static class SomeAnnotatedClass {
  }

  @Retention(RetentionPolicy.RUNTIME)
  public @interface SomeAnnotation {
    String foo();
    String bar();
  }

  static ClassLoader loadTestClassesInNewClassLoader() {
    return new ClassLoader(ClassLoader.getSystemClassLoader()) {
      /* init */ {
        Stream.of(SomeUnannotatedClass.class, SomeAnnotation.class, SomeAnnotatedClass.class)
            .forEach(cls -> {
              var data = getBytes(cls);
              defineClass(cls.getName(), data, 0, data.length);
            });
      }
    };
  }

  static byte[] getBytes(Class<?> cls) {
    return Unchecked.call(() -> {
      var path = cls.getName().replace(".", "/") + ".class";
      try (var is = cls.getClassLoader().getResourceAsStream(path)) {
        assertThat(is)
            .as("class at %s", path)
            .isNotNull();
        var baos = new ByteArrayOutputStream();
        is.transferTo(baos);
        return baos.toByteArray();
      }
    });
  }
}
