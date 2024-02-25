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

// Only execute this test if the user has protoc installed on their
// PATH and it is successfully executable.
try {
  println("Checking if protoc is on the system path...")
  int exitCode = "protoc --version".execute().waitFor()

  if (exitCode == 0) {
    println("Protoc is on the path and is executable, proceeding with the test.")
    return true
  } else {
    println("Skipping this test: protoc system binary exited with code ${exitCode}")
    return false
  }
} catch (IOException ex) {
  println("Skipping this test. ${ex.getClass().getName()}: ${ex.getMessage()}")
  return false
}
