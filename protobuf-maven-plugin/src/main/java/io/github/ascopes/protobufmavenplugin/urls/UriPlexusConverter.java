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

import java.net.URI;
import java.net.URISyntaxException;
import org.codehaus.plexus.component.configurator.ComponentConfigurationException;
import org.codehaus.plexus.component.configurator.converters.basic.AbstractBasicConverter;


/**
 * Plexus/Sisu parameter converter for URIs.
 *
 * <p>We provide this to avoid using the URL and File APIs in the Mojo interface. URLs do an
 * immediate lookup for the URL scheme's appropriate URLStreamHandlerProvider upon construction, and
 * we have no control over how that works. We have no ability to inject custom URL handlers at this
 * point because the URL class is hardcoded to only consider the system classloader. Since Maven
 * uses ClassWorlds to run multiple classloaders for each plugin and component, we will not be
 * loaded as part of that default classloader. By deferring this operation to as late as possible
 * (i.e. in {@link UriResourceFetcher}), we can
 * ensure we provide the desired URL handler directly instead. This allows us to hook custom URL
 * handlers in via {@link java.util.ServiceLoader} dynamically, like we would be able to outside a
 * Maven plugin running in Plexus.
 *
 * <p>Newer versions of Plexus/Sisu provide this for us, so in the future, we can remove these
 * components (looks to be supported from Maven 3.9.x).
 *
 * @author Ashley Scopes
 * @since 3.1.3
 */
public final class UriPlexusConverter extends AbstractBasicConverter {

  @Override
  public boolean canConvert(Class<?> type) {
    return URI.class.equals(type);
  }

  @Override
  protected Object fromString(String str) throws ComponentConfigurationException {
    try {
      return new URI(str);
    } catch (URISyntaxException ex) {
      // GH-689: align with the same error format as Sisu provides in Maven 3.9.x for
      // forwards compatibility.
      throw new ComponentConfigurationException("Cannot convert '" + str + "' to URI", ex);
    }
  }
}
