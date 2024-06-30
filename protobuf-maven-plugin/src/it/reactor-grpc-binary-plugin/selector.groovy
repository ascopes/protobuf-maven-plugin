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

if (!["x86_64", "amd64", "aarch64"].contains(System.getProperty("os.arch"))) {
  println("Skipping this test case as the system is not AMD64 or AARCH64")
  return false
} else if ("uname -o".execute().text =~ /Android.*/) {
  println("Skipping test as it uses incompatible binaries for Android")
  return false
} else {
  return true
}
