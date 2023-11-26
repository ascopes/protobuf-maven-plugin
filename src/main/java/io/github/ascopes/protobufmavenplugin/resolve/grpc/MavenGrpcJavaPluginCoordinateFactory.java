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
package io.github.ascopes.protobufmavenplugin.resolve.grpc;

import io.github.ascopes.protobufmavenplugin.resolve.AbstractMavenCoordinateFactory;
import io.github.ascopes.protobufmavenplugin.resolve.ExecutableResolutionException;
import org.apache.maven.shared.transfer.artifact.ArtifactCoordinate;
import org.apache.maven.shared.transfer.artifact.DefaultArtifactCoordinate;

/**
 * Coordinate factory for determining the correct coordinate for the GRPC Java generator to download
 * from Maven central.
 *
 * @author Ashley Scopes
 */
public final class MavenGrpcJavaPluginCoordinateFactory extends AbstractMavenCoordinateFactory {

  private static final String GROUP_ID = "io.grpc";
  private static final String ARTIFACT_ID = "protoc-gen-grpc-java";
  private static final String EXTENSION = "exe";

  /**
   * Create the artifact coordinate for the current system.
   *
   * @param versionRange the version or version range of the artifact to use.
   * @return the artifact to resolve.
   * @throws ExecutableResolutionException if the system is not supported.
   */
  public ArtifactCoordinate create(String versionRange) throws ExecutableResolutionException {
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
}
