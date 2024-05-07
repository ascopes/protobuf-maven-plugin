/*
 * Copyright (C) 2023 - 2024, Ashley Scopes.
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

package io.github.ascopes.protobufmavenplugin;

import java.net.URL;
import org.immutables.value.Value.Modifiable;


/**
 * Implementation independent descriptor for a protoc plugin that can
 * be resolved from a URL.
 *
 * <p>URL-based plugins can be marked as optional if they should be
 * skipped when the resource is unable to be resolved.
 *
 * @author Ashley Scopes
 * @since 2.0.0
 */
@Modifiable
public abstract class UrlProtocPlugin implements OptionalProtocPlugin {

  public abstract URL getUrl();
}
