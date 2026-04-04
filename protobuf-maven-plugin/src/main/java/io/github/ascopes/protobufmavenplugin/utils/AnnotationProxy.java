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

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Proxy;
import java.util.Optional;
import java.util.stream.Stream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Proxies annotations in a classloader-safe way in the event we are dealing
 * with two classloaders holding the same classes.
 *
 * <p>This allows retrieving annotations on an {@link AnnotatedElement}
 * safely even when the wrong classloader is used by Plexus.
 *
 * <p>Ideally, this should never be needed, but for reasons we cannot
 * consistently reproduce, certain components get shared between Mojo
 * executions, even when scoping per Mojo instance.
 *
 * <p>See <a href="https://github.com/ascopes/protobuf-maven-plugin/pull/974">
 * GH-974</a> for such an example.
 *
 * @author Ashley Scopes
 * @since 5.1.3
 */
public final class AnnotationProxy {
  private static final Logger log = LoggerFactory.getLogger(AnnotationProxy.class);

  private AnnotationProxy() {
    throw new UnsupportedOperationException("static-only class");
  }

  public static <A extends Annotation> Optional<? extends A> findAnnotation(
      Class<A> annotationCls,
      AnnotatedElement element
  ) {
    return Optional.ofNullable(element.getAnnotation(annotationCls))
        .or(() -> findAndProxyAnnotation(annotationCls, element));
  }

  private static <A extends Annotation> Optional<? extends A> findAndProxyAnnotation(
      Class<A> annotationCls,
      AnnotatedElement element
  ) {
    return Stream.of(element.getAnnotations())
        .filter(annotation -> annotation.annotationType().getName().equals(annotationCls.getName()))
        .findFirst()
        .map(annotation -> proxy(annotationCls, annotation));
  }

  private static <A extends Annotation> A proxy(
      Class<A> targetCls,
      Annotation annotation
  ) {
    var sourceCls = annotation.annotationType();

    log.debug(
        "Proxying {} from {} ({}) to {} ({}) to mitigate classloader mismatch",
        annotation,
        sourceCls,
        sourceCls.getClassLoader(),
        targetCls,
        targetCls.getClassLoader()
    );

    @SuppressWarnings("unchecked")
    var proxyAnnotation = (A) Unchecked.call(() -> Proxy.newProxyInstance(
        targetCls.getClassLoader(),
        new Class<?>[]{ targetCls },
        (self, method, args) -> sourceCls.getMethod(method.getName())
            .invoke(annotation, args)
    ));

    return proxyAnnotation;
  }
}
