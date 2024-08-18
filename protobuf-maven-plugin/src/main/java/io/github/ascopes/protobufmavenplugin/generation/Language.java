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

package io.github.ascopes.protobufmavenplugin.generation;

import java.util.EnumSet;

/**
 * Supported generated source languages.
 *
 * @author Ashley Scopes
 * @since 1.2.0
 */
public enum Language {
  CPP("cpp"),
  C_SHARP("csharp"),
  JAVA("java"),
  KOTLIN("kotlin"),
  OBJECTIVE_C("objc"),
  PHP("php"),
  PYTHON("python"),
  PYI("pyi"),
  RUBY("ruby"),
  RUST("rust");

  private final String flagName;

  Language(String flagName) {
    this.flagName = flagName;
  }

  /**
   * Get the flag name.
   *
   * @return the name of the flag to pass to {@code protoc}.
   */
  public String getFlagName() {
    return flagName;
  }

  public static LanguageSetBuilder languageSet() {
    return new LanguageSetBuilder();
  }

  /**
   * Builder for a set of enabled languages.
   *
   * @author Ashley Scopes
   * @since 1.2.0
   */
  public static final class LanguageSetBuilder {

    private final EnumSet<Language> values;

    private LanguageSetBuilder() {
      values = EnumSet.noneOf(Language.class);
    }

    public LanguageSetBuilder addIf(boolean condition, Language language) {
      return condition ? add(language) : this;
    }

    public LanguageSetBuilder add(Language language) {
      values.add(language);
      return this;
    }

    public EnumSet<Language> build() {
      return values;
    }
  }
}
