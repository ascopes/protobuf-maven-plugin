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
package io.github.ascopes.protobufmavenplugin.urls;

import java.io.IOException;
import java.io.InputStream;
import org.jspecify.annotations.Nullable;

/**
 * Standard type for wrapping variants of any generic {@link InputStream} into
 * {@link java.net.URLConnection}s, effectively transforming the data format.
 *
 * @author Ashley Scopes
 * @since 3.10.0
 */
final class TransformingUrlStreamHandlerFactory
    extends AbstractDecoratingUrlStreamHandlerFactory {

  private final InputStreamDecorator<InputStream> decorator;

  TransformingUrlStreamHandlerFactory(
      UrlFactory urlFactory,
      InputStreamDecorator<InputStream> decorator,
      String... protocols
  ) {
    super(false, urlFactory, protocols);
    this.decorator = decorator;
  }

  @Override
  protected InputStream decorateInputStream(
      InputStream inputStream,
      @Nullable String file
  ) throws IOException {
    return decorator.decorate(inputStream);
  }
}
