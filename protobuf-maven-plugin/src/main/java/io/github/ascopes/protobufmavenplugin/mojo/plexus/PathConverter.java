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

import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.converters.basic.AbstractBasicConverter;


/**
 * Converter for Path objects on the root file system.
 *
 * <p>We provide this to avoid using the URL and File APIs in the Mojo interface.
 *
 * <p>Newer versions of Plexus/Sisu provide this for us, so in the future, we can remove these
 * components (looks to be supported from Maven 3.9.x).
 *
 * @author Ashley Scopes
 * @since 3.1.3
 */
final class PathConverter extends AbstractBasicConverter {

  @Override
  public boolean canConvert(Class<?> type) {
    return type.equals(Path.class);
  }

  @Override
  protected Object fromString(String str) throws ComponentConfigurationException {
    try {
      return Path.of(str);
    } catch (InvalidPathException ex) {
      throw new ComponentConfigurationException("Failed to parse path '" + str + "': " + ex, ex);
    }
  }
}
