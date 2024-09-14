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

import java.nio.file.Files
import java.nio.file.Path

import static org.assertj.core.api.Assertions.assertThat
import static org.assertj.core.api.Assertions.fail

Path baseDirectory = basedir.toPath().toAbsolutePath()
Path logFile = baseDirectory.resolve("build.log")
List<String> logLines = Files.readAllLines(logFile)

int indexOfMatch(List<String> logLines, String pattern) {
  for (int i = 0; i < logLines.size(); ++i) {
    if (logLines.get(i).matches(pattern)) {
      return i
    }
  }

  fail("No pattern %s found in log lines:%n%s", pattern, String.join("\n", logLines))
}

assertThat(indexOfMatch(logLines, ".*--plugin=.*?reactor-grpc.*"))
    .as("index of commandline argument for the reactor GRPC plugin")
    .isGreaterThan(indexOfMatch(logLines, ".*--plugin=.*?protoc-gen-grpc-java.*"))

return true
