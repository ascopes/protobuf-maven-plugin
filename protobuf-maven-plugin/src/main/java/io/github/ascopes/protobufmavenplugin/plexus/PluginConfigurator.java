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

import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.SessionScoped;
import org.codehaus.plexus.component.configurator.BasicComponentConfigurator;
import org.codehaus.plexus.component.configurator.converters.ConfigurationConverter;
import org.eclipse.sisu.Description;

/**
 * Custom configurator for this Maven plugin which allows us to inject additional converter types
 * to handle parsing parameters, as well as other future bootstrapping concerns.
 *
 * <p>This is initialized per Mojo execution as some converter components will be associated
 * with classes in the current classloader realm. Doing this avoids the need for managing class
 * references via weak references to allow garbage collection of the current classloader.
 *
 * @author Ashley Scopes
 * @since 3.1.3
 */
@Description("Configures Plexus to work with this Maven Plugin")
@SessionScoped
@Named(PluginConfigurator.NAME)
public class PluginConfigurator extends BasicComponentConfigurator {

  public static final String NAME = "protobuf-maven-plugin-configurator";

  @Inject
  public PluginConfigurator(List<ConfigurationConverter> configurationConverters) {
    configurationConverters.forEach(converterLookup::registerConverter);
  }
}
