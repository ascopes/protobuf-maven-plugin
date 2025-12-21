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
import java.util.function.Consumer
import static org.assertj.core.api.Assertions.assertThat

Path generatedSourcePackageDir = basedir.toPath()
    .resolve("bank-transaction-service")
    .resolve("target")
    .resolve("generated-sources")
    .resolve("protobuf")
    .resolve("com")
    .resolve("somebank")
    .resolve("models")

List<Path> expectedFiles = [
    generatedSourcePackageDir.resolve("Amount.java"),
    generatedSourcePackageDir.resolve("AmountAud.java"),
    generatedSourcePackageDir.resolve("AmountAudOrBuilder.java"),
    generatedSourcePackageDir.resolve("AmountEur.java"),
    generatedSourcePackageDir.resolve("AmountEurOrBuilder.java"),
    generatedSourcePackageDir.resolve("AmountGbp.java"),
    generatedSourcePackageDir.resolve("AmountGbpOrBuilder.java"),
    generatedSourcePackageDir.resolve("AmountJpy.java"),
    generatedSourcePackageDir.resolve("AmountJpyOrBuilder.java"),
    generatedSourcePackageDir.resolve("AmountOrBuilder.java"),
    generatedSourcePackageDir.resolve("AmountOuterClass.java"),
    generatedSourcePackageDir.resolve("AmountSign.java"),
    generatedSourcePackageDir.resolve("AmountUsd.java"),
    generatedSourcePackageDir.resolve("AmountUsdOrBuilder.java"),
    generatedSourcePackageDir.resolve("Payee.java"),
    generatedSourcePackageDir.resolve("PayeeOrBuilder.java"),
    generatedSourcePackageDir.resolve("PayeeOuterClass.java"),
    generatedSourcePackageDir.resolve("Transaction.java"),
    generatedSourcePackageDir.resolve("TransactionOrBuilder.java"),
    generatedSourcePackageDir.resolve("TransactionOuterClass.java"),
]

assertThat(generatedSourcePackageDir).isDirectory()
assertThat(expectedFiles).allSatisfy((Consumer) { assertThat(it).isRegularFile() })
return true

