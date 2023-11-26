/*
 * Copyright (C) 2023, Ashley Scopes.
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
package io.github.ascopes.protobufmavenplugin.resolve.protoc;

import io.github.ascopes.protobufmavenplugin.platform.HostEnvironment;
import io.github.ascopes.protobufmavenplugin.resolve.AbstractMavenCoordinateFactory;
import io.github.ascopes.protobufmavenplugin.resolve.ExecutableResolutionException;
import org.apache.maven.shared.transfer.artifact.ArtifactCoordinate;
import org.apache.maven.shared.transfer.artifact.DefaultArtifactCoordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Coordinate factory for determining the correct coordinate for {@code protoc} to use for the
 * current system.
 *
 * @author Ashley Scopes
 */
public final class MavenProtocCoordinateFactory extends AbstractMavenCoordinateFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(MavenProtocCoordinateFactory.class);

  private static final String GROUP_ID = "com.google.protobuf";
  private static final String ARTIFACT_ID = "protoc";
  private static final String EXTENSION = "exe";

  /**
   * Create the artifact coordinate for the current system.
   *
   * @param versionRange the version or version range of the artifact to use.
   * @return the artifact to resolve.
   * @throws ExecutableResolutionException if the system is not supported.
   */
  public ArtifactCoordinate create(String versionRange) throws ExecutableResolutionException {
    emitPlatformWarnings();

    var coordinate = new DefaultArtifactCoordinate();
    coordinate.setGroupId(GROUP_ID);
    coordinate.setArtifactId(ARTIFACT_ID);
    coordinate.setVersion(versionRange);
    coordinate.setClassifier(determineClassifier());
    coordinate.setExtension(EXTENSION);
    return coordinate;
  }

  @Override
  protected String name() {
    return ARTIFACT_ID;
  }

  private void emitPlatformWarnings() {
    if (HostEnvironment.workingDirectory().toString().startsWith("/data/data/com.termux")) {
      LOGGER.warn(
          "It appears you are running on Termux. You may have difficulties "
              + "invoking the 'protoc' executable from Maven Central. If this "
              + "is an issue, install protoc directly ('pkg in protoc'), and "
              + "invoke Maven with '-Dprotoc.version=PATH' instead."
      );
    }
  }
}
