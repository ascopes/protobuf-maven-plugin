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
import java.nio.file.Path

import static org.assertj.core.api.Assertions.assertThat

Path localRepositoryDir = localRepositoryPath.toPath().toAbsolutePath()

def expectedGeneratedFiles = [
    "gh-565-attach-descriptor-file/attach-defaults/1.0-SNAPSHOT/attach-defaults-1.0-SNAPSHOT.protobin",
    "gh-565-attach-descriptor-file/attach-defaults/1.0-SNAPSHOT/attach-defaults-1.0-SNAPSHOT-test.test-protobin",
    "gh-565-attach-descriptor-file/attach-custom-classifier/1.0-SNAPSHOT/attach-custom-classifier-1.0-SNAPSHOT-myclassifier.protobin",
    "gh-565-attach-descriptor-file/attach-custom-type/1.0-SNAPSHOT/attach-custom-type-1.0-SNAPSHOT.myprotobin",
]

// Verify that protobin descriptors were attached properly
expectedGeneratedFiles.forEach {
  assertThat(localRepositoryDir.resolve("${it}"))
      .exists()
      .isRegularFile()
}

return true
