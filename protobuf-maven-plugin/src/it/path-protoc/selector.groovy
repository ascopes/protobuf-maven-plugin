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
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

String executablePath(String name) {
  String sep = Pattern.quote(System.getProperty("path.separator", ""))
  String[] pathExts = (System.getenv("PATHEXT") ?: "").split(sep, -1)
  String[] dirs = (System.getenv("PATH") ?: "").split(sep, -1)
  boolean windows = System.getProperty("os.name", "unknown")
      .toLowerCase()
      .startsWith("win")
  
  for (String dir : dirs) {
    for (String pathExt : pathExts) {
      Path expectedPath = Path.of(dir).resolve(name + pathExt)
      if (Files.isRegularFile(expectedPath) && (Files.isExecutable(expectedPath) || windows)) {
        println("Resolved ${name} to ${expectedPath} in system PATH")
        return expectedPath
      }
    }
  }

  println("Did not find ${name} in system PATH")
  return null;
}

return executablePath("protoc") != null
