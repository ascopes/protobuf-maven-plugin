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

package io.github.ascopes.protobufmavenplugin.execute;

import java.util.OptionalInt;

/**
 * Exception that is raised if execution of {@code protoc} fails.
 *
 * @author Ashley Scopes
 */
public final class ProtocExecutionException extends Exception {

  /**
   * The exit code, or {@code null} if no exit code exists in the given context.
   */
  private final Integer exitCode;

  /**
   * Initialise the exception.
   *
   * @param message the exception message.
   * @param cause   the cause of the exception.
   */
  public ProtocExecutionException(String message, Throwable cause) {
    super(message, cause);
    exitCode = null;
  }

  /**
   * Initialise the exception.
   *
   * @param exitCode the exit code that the process invocation returned.
   */
  public ProtocExecutionException(int exitCode) {
    super("Protoc execution returned an exit code of " + exitCode);
    this.exitCode = exitCode;
  }

  /**
   * Get the exit code if one was provided.
   *
   * @return the exit code or an empty optional if not set.
   */
  public OptionalInt getExitCode() {
    return exitCode == null ? OptionalInt.empty() : OptionalInt.of(exitCode);
  }
}
