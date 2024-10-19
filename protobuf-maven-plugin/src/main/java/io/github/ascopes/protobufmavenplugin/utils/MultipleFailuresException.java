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

package io.github.ascopes.protobufmavenplugin.utils;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * Exception that gets raised when one or more concurrent tasks fail.
 *
 * @author Ashley Scopes
 * @since 2.2.0
 */
final class MultipleFailuresException extends RuntimeException {

  private MultipleFailuresException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Initialise this exception.
   *
   * @param exceptions the exceptions that were thrown. Must have at least one item.
   * @return the wrapper exception.
   * @throws NoSuchElementException if an empty list was provided.
   */
  static MultipleFailuresException create(List<? extends Throwable> exceptions) {
    var causeIterator = exceptions.iterator();
    var cause = causeIterator.next();
    var message = causeIterator.hasNext()
        ? exceptions.size() + " failures occurred during a concurrent task. The first was: "
        : "A failure occured during a concurrent task: ";
    message += cause.getClass().getName() + ": " + cause.getMessage();

    var ex = new MultipleFailuresException(message, cause);
    causeIterator.forEachRemaining(ex::addSuppressed);
    return ex;
  }
}
