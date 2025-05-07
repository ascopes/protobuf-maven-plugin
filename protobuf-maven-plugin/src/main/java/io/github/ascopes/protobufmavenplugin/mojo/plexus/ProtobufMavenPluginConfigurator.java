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

import javax.inject.Named;
import javax.inject.Singleton;
import org.codehaus.plexus.component.configurator.BasicComponentConfigurator;
import org.eclipse.sisu.Description;

/**
 * Custom configurator for this Maven plugin which allows us to inject additional converter types to
 * work around internal quirks regarding how Maven, Sisu, and Plexus operate under the hood.
 *
 * @author Ashley Scopes
 * @since 3.1.3
 */
@Description("Registers custom Maven parameter converters for Plexus")
@Named(ProtobufMavenPluginConfigurator.NAME)
@Singleton
public class ProtobufMavenPluginConfigurator extends BasicComponentConfigurator {

  public static final String NAME = "protobuf-maven-plugin-configurator";

  ProtobufMavenPluginConfigurator() {
    converterLookup.registerConverter(new PathConverter());
    converterLookup.registerConverter(new UriConverter());
  }
}
