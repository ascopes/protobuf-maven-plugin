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
// Only execute on x86 systems, aarch64 ports do not exist for all platforms
// we use in our CI pipeline for the old version of protoc we use for this
// test.
if (!System.getProperty("os.arch").equalsIgnoreCase("amd64")) {
  println("Skipping this test case as the system is not x86_64")
  return false
} else {
  return true
}
