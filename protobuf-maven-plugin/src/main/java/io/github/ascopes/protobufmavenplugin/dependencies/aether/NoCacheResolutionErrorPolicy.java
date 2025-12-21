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
package io.github.ascopes.protobufmavenplugin.dependencies.aether;

import org.eclipse.aether.RepositorySystemSession;
import org.eclipse.aether.artifact.Artifact;
import org.eclipse.aether.metadata.Metadata;
import org.eclipse.aether.resolution.ResolutionErrorPolicy;
import org.eclipse.aether.resolution.ResolutionErrorPolicyRequest;

/**
 * Policy that disables resolution error caching.
 *
 * <p>In Maven 3.9.9 or newer, we should use {@code SimpleResolutionErrorPolicy} elsewhere instead
 * of this. For now, this is implemented to enable Maven 3.8 compatibility.
 *
 * @author Ashley Scopes
 * @since 2.13.0
 */
final class NoCacheResolutionErrorPolicy implements ResolutionErrorPolicy {

  @Override
  public int getArtifactPolicy(
      RepositorySystemSession session,
      ResolutionErrorPolicyRequest<Artifact> request
  ) {
    return CACHE_DISABLED;
  }

  @Override
  public int getMetadataPolicy(
      RepositorySystemSession session,
      ResolutionErrorPolicyRequest<Metadata> request
  ) {
    return CACHE_DISABLED;
  }
}
