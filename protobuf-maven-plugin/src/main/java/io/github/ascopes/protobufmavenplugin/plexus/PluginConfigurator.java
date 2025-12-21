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
import javax.inject.Singleton;
import org.codehaus.plexus.component.configurator.BasicComponentConfigurator;
import org.codehaus.plexus.component.configurator.converters.ConfigurationConverter;
import org.eclipse.sisu.Description;

/**
 * Custom configurator for this Maven plugin which allows us to inject additional converter types
 * to handle parsing parameters, as well as other future bootstrapping concerns.
 *
 * @author Ashley Scopes
 * @since 3.1.3
 */
@Description("Configures Plexus to work with this Maven Plugin")
@Named(PluginConfigurator.NAME)
@Singleton
public class PluginConfigurator extends BasicComponentConfigurator {

  public static final String NAME = "protobuf-maven-plugin-configurator";

  @Inject
  public PluginConfigurator(List<ConfigurationConverter> configurationConverters) {
    configurationConverters.forEach(converterLookup::registerConverter);
  }
}
