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
package io.github.ascopes.protobufmavenplugin.utils;

/**
 * Helper for invoking checked functions without handling exceptions that
 * may be raised.
 *
 * <p>Use for cases where it is known that a checked exception cannot reasonably
 * be thrown.
 *
 * @author Ashley Scopes
 * @since 5.0.3
 */
public final class Unchecked {
  private Unchecked() {
    throw new UnsupportedOperationException("static-only class");
  }

  public static <R> R call(CheckedSupplier<R> supplier) {
    try {
      return supplier.get();
    } catch (Exception ex) {
      if (ex instanceof RuntimeException rte) {
        throw rte;
      }
      throw new IllegalStateException(
          "Checked exception raised unexpectedly, this is a bug. "
              + "Exception was: " + ex,
          ex
      );
    }
  }

  /**
   * Signature for a supplier that can throw a checked exception.
   */
  @FunctionalInterface
  public interface CheckedSupplier<R> {
    R get() throws Exception;
  }
}
