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
import java.nio.file.Path

import static org.assertj.core.api.Assertions.assertThat

void assertGeneratedFiles(
    Path targetDir,
    List<String> expectedGeneratedFiles,
    List<String> expectedNotGeneratedFiles
) {
    Path generatedSourcesDir = targetDir.resolve("generated-sources/protobuf")
    Path classesDir = targetDir.resolve("classes")

    assertThat(generatedSourcesDir).isDirectory()
    assertThat(classesDir).isDirectory()

    expectedGeneratedFiles.forEach {
        assertThat(generatedSourcesDir.resolve("${it}.java"))
            .exists()
            .isNotEmptyFile()
        assertThat(classesDir.resolve("${it}.class"))
            .exists()
            .isNotEmptyFile()
    }

    expectedNotGeneratedFiles.forEach {
        assertThat(generatedSourcesDir.resolve("${it}.java"))
            .doesNotExist()
        assertThat(classesDir.resolve("${it}.class"))
            .doesNotExist()
    }
}

Path baseDirectory = basedir.toPath().toAbsolutePath()
Path consumerParentDir = baseDirectory.resolve("consumer-parent")

assertGeneratedFiles(
    consumerParentDir.resolve("consumer-includes").resolve("target"),
    [
        "org/example/helloworld/Helloworld",
        "org/example/test1/Test1Suite",
    ],
    [
        "org/example/test2/Test2Suite",
        "org/example/test3/Test3Suite",
    ]
)

assertGeneratedFiles(
    consumerParentDir.resolve("consumer-excludes").resolve("target"),
    [
        "org/example/test2/Test2Suite",
        "org/example/test3/Test3Suite",
    ],
    [
        "org/example/helloworld/Helloworld",
        "org/example/test1/Test1Suite",
    ]
)

assertGeneratedFiles(
    consumerParentDir.resolve("consumer-excludes-includes").resolve("target"),
    [
        "org/example/helloworld/Helloworld",
        "org/example/test2/Test2Suite",
    ],
    [
        "org/example/test1/Test1Suite",
        "org/example/test3/Test3Suite",
    ]
)
return true
