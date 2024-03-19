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

package io.github.ascopes.protobufmavenplugin.dependency;

import static java.util.Objects.requireNonNullElse;
import static java.util.Objects.requireNonNullElseGet;

import io.github.ascopes.protobufmavenplugin.platform.HostSystem;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.shared.transfer.artifact.ArtifactCoordinate;
import org.apache.maven.shared.transfer.artifact.DefaultArtifactCoordinate;
import org.jspecify.annotations.Nullable;

/**
 * Factory that can produce dependencies marked with platform-specific classifiers.
 *
 * @author Ashley Scopes
 */
@Named
public final class PlatformArtifactFactory {
  private final HostSystem hostSystem;

  @Inject
  public PlatformArtifactFactory(HostSystem hostSystem) {
    this.hostSystem = hostSystem;
  }

  public ArtifactCoordinate createArtifact(
      String groupId,
      String artifactId,
      String version,
      @Nullable String extension,
      @Nullable String classifier
  ) {
    var dependency = new DefaultArtifactCoordinate();
    dependency.setGroupId(groupId);
    dependency.setArtifactId(artifactId);
    dependency.setVersion(version);
    dependency.setExtension(requireNonNullElse(extension, "exe"));
    dependency.setClassifier(requireNonNullElseGet(
        classifier, 
        () -> getPlatformExecutableClassifier(artifactId)
    ));
    return dependency;
  }

  private String getPlatformExecutableClassifier(String artifactId) {
    var rawOs = hostSystem.getOperatingSystem();
    var rawArch = hostSystem.getCpuArchitecture();

    if (hostSystem.isProbablyLinux()) {
      switch (rawArch) {
        case "ppc64le":
        case "ppc64":
          return "linux-ppcle_64";

        case "s390x":
        case "zarch_64":
          return "linux-s390_64";

        case "aarch64":
          return "linux-aarch_64";

        case "amd64":
          return "linux-x86_64";

        default:
          // Fall-over
          break;
      }

    } else if (hostSystem.isProbablyMacOs()) {
      switch (rawArch) {
        case "aarch64":
          return "osx-aarch_64";

        case "amd64":
        case "x86_64":
          return "osx-x86_64";

        default:
          // Fall-over
          break;
      }

    } else if (hostSystem.isProbablyWindows()) {
      switch (rawArch) {
        case "amd64":
        case "x86_64":
          return "windows-x86_64";

        case "x86":
        case "x86_32":
          return "windows-x86_32";

        default:
          // Fall-over
          break;
      }
    }

    var message = String.format(
        "No '%s' binary is available for reported OS '%s' and CPU architecture '%s'",
        artifactId,
        rawOs,
        rawArch
    );
    throw new UnsupportedOperationException(message);
  }
}
