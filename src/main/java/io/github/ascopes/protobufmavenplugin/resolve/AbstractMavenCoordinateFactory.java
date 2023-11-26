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
package io.github.ascopes.protobufmavenplugin.resolve;

import io.github.ascopes.protobufmavenplugin.platform.HostEnvironment;
import org.apache.maven.shared.transfer.artifact.ArtifactCoordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Coordinate factory for determining the correct coordinate for the artifact to use for the current
 * system.
 *
 * @author Ashley Scopes
 */
public abstract class AbstractMavenCoordinateFactory {

  private static final Logger LOGGER = LoggerFactory.getLogger(
      AbstractMavenCoordinateFactory.class);

  /**
   * Create the artifact coordinate for the current system.
   *
   * @param versionRange the version or version range of the artifact to use.
   * @return the artifact to resolve.
   * @throws ExecutableResolutionException if the system is not supported.
   */
  public abstract ArtifactCoordinate create(String versionRange)
      throws ExecutableResolutionException;

  /**
   * Get a friendly name to use for logging.
   *
   * @return the friendly name.
   */
  protected abstract String name();

  /**
   * Determine the correct classifier for the current system.
   *
   * @return the classifier string.
   * @throws ExecutableResolutionException if the classifier could not be resolved.
   */
  protected final String determineClassifier() throws ExecutableResolutionException {
    String classifier;

    if (HostEnvironment.isWindows()) {
      classifier = "windows-" + determineArchitectureForWindows();
    } else if (HostEnvironment.isLinux()) {
      classifier = "linux-" + determineArchitectureForLinux();
    } else if (HostEnvironment.isMacOs()) {
      classifier = "osx-" + determineArchitectureForMacOs();
    } else {
      throw new ExecutableResolutionException(
          "No resolvable version of " + name() + " for the current OS found"
      );
    }

    LOGGER.debug("Will use {} as the {} artifact classifier", classifier, name());
    return classifier;
  }

  private String determineArchitectureForWindows() throws ExecutableResolutionException {
    var arch = HostEnvironment.cpuArchitecture();

    switch (arch) {
      case "amd64":
      case "x86_64":
        return "x86_64";

      case "x86":
      case "x86_32":
        return "x86_32";

      default:
        throw noResolvableProtocFor("Windows", arch);
    }
  }

  private String determineArchitectureForLinux() throws ExecutableResolutionException {
    var arch = HostEnvironment.cpuArchitecture();

    switch (arch) {
      // https://bugs.openjdk.org/browse/JDK-8073139
      case "ppc64le":
      case "ppc64":
        return "ppcle_64";

      case "s390":
      case "zarch_64":
        return "s390_64";

      case "aarch64":
        return "aarch_64";

      case "amd64":
        return "x86_64";

      default:
        throw noResolvableProtocFor("Linux", arch);
    }
  }

  private String determineArchitectureForMacOs() throws ExecutableResolutionException {
    var arch = HostEnvironment.cpuArchitecture();

    switch (arch) {
      case "aarch64":
        return "aarch_64";

      case "amd64":
      case "x86_64":
        return "x86_64";

      default:
        throw noResolvableProtocFor("Mac OS", arch);
    }
  }

  private ExecutableResolutionException noResolvableProtocFor(String os, String arch) {
    var message = "No resolvable " + name() + " version for " + os
        + " '" + arch + "' systems found";
    return new ExecutableResolutionException(message);
  }
}
