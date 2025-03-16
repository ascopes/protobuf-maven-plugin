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
package io.github.ascopes.protobufmavenplugin.utils;

import java.nio.file.AccessDeniedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Predicate;
import javax.inject.Inject;
import javax.inject.Named;
import org.apache.maven.SessionScoped;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A bean that allows discovering a binary on the system path, using OS-specific resolution
 * semantics.
 *
 * @author Ashley Scopes
 */
@Named
@SessionScoped
public final class SystemPathBinaryResolver {

  private static final Logger log = LoggerFactory.getLogger(SystemPathBinaryResolver.class);

  private final HostSystem hostSystem;

  @Inject
  public SystemPathBinaryResolver(HostSystem hostSystem) {
    this.hostSystem = hostSystem;
  }

  public Optional<Path> resolve(String name) throws ResolutionException {
    log.debug("Looking for executable matching name '{}' on PATH", name);
    var predicate = hostSystem.isProbablyWindows()
        ? isWindowsMatch(name)
        : isPosixMatch(name);

    try {
      for (var dir : hostSystem.getSystemPath()) {
        try (var files = Files.walk(dir, 1)) {
          var result = files.filter(predicate).findFirst();

          if (result.isPresent()) {
            log.debug("Result for lookup of '{}' on PATH was {}", name, result.get());
            return result;
          }

        } catch (AccessDeniedException ex) {
          log.debug("Ignoring directory '{}' as access is denied", dir, ex);
        }
      }
    } catch (Exception ex) {
      throw new ResolutionException("An exception occurred while scanning the system PATH", ex);
    }

    log.debug("No match found for '{}' on PATH", name);
    return Optional.empty();
  }

  private Predicate<Path> isWindowsMatch(String name) {
    return path -> {
      var matchesName = FileUtils.getFileNameWithoutExtension(path)
          .equalsIgnoreCase(name);
      var matchesExtension = FileUtils
          .getFileExtension(path)
          .filter(hostSystem.getSystemPathExtensions()::contains)
          .isPresent();

      log.trace(
          "Path '{}' matches name = {}, matches executable extension = {}",
          path,
          matchesName,
          matchesExtension
      );

      return matchesName && matchesExtension;
    };
  }

  private Predicate<Path> isPosixMatch(String name) {
    return path -> {
      var matchesName = path.getFileName().toString().equals(name);
      var matchesExecutableFlag = Files.isExecutable(path);

      log.trace(
          "Path '{}' matches name = {}, matches executable flag = {}",
          path,
          matchesName,
          matchesExecutableFlag
      );

      return matchesName && matchesExecutableFlag;
    };
  }
}
