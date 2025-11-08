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
package io.github.ascopes.protobufmavenplugin.fs;

import java.io.File;
import java.nio.file.Path;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.ConfigurationListener;
import org.codehaus.plexus.component.configurator.converters.basic.FileConverter;
import org.codehaus.plexus.component.configurator.converters.lookup.ConverterLookup;
import org.codehaus.plexus.component.configurator.expression.ExpressionEvaluator;
import org.codehaus.plexus.configuration.PlexusConfiguration;
import org.jspecify.annotations.Nullable;


/**
 * Plexus/Sisu parameter converter for Path objects on the root file system.
 *
 * <p>We provide this to avoid using the URL and File APIs in the Mojo interface.
 *
 * <p>Newer versions of Plexus/Sisu provide this for us, so in the future, we can remove these
 * components (looks to be supported from Maven 3.9.8).
 *
 * @author Ashley Scopes
 * @since 3.1.3
 */
public final class PathPlexusConverter extends FileConverter {

  @Override
  public boolean canConvert(Class<?> type) {
    return Path.class.equals(type);
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
    // GH-689: we need to consider paths as relative to the Maven project directory rather
    // than relative to the current working directory. This is important when running nested
    // Maven modules that are expected to assume the submodule directory rather than the launch
    // site directory.
    //
    // For now, we do the same thing that Sisu is doing in Maven 3.9.x to retain backwards and
    // forwards compatibility. This handles any other niche cases we might miss as well.
    // See https://github.com/eclipse-sisu/sisu-project/blob/e86e5005ff03b57aab8c7eb7f54f41e85914e2dd/org.eclipse.sisu.plexus/src/main/java/org/codehaus/plexus/component/configurator/converters/basic/PathConverter.java#L33

    var result = super.fromConfiguration(
        lookup, configuration, type, enclosingType, loader, evaluator, listener
    );

    return result instanceof File file
        ? file.toPath()
        : result;
  }
}
