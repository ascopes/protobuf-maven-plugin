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

import groovy.transform.Field

import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

static final @Field String ALPHA = "abcdefghijklmnopqrstuvwxyz"
static final @Field String ALPHA_NUMERIC = ALPHA + "0123456789"
static final @Field String ALPHA_NUMERIC_UNDERSCORE = ALPHA_NUMERIC + "_"
static final @Field List<String> PROTO_TYPES = [
    "double", "float", "int32", "int64", "uint32", "uint64", "sint32", "sint64",
    "fixed32", "fixed64", "sfixed32", "sfixed64", "bool", "string", "bytes",
]

static final @Field Random rng = new Random()

int randomInt(int min, int max) {
  return min + rng.nextInt(max - min)
}

char randomChar(String dictionary) {
  return dictionary[rng.nextInt(dictionary.length())]
}

void addRandomMessageName(Appendable sb) {
  int length = randomInt(5, 10)
  sb.append(Character.toUpperCase(randomChar(ALPHA)))
  for (int i = 1; i < length; ++i) {
    sb.append(randomChar(ALPHA_NUMERIC))
  }
}

void addRandomIdentifier(Appendable sb) {
  int length = randomInt(5, 10)
  sb.append(randomChar(ALPHA))
  for (int i = 1; i < length; ++i) {
    sb.append(randomChar(ALPHA_NUMERIC_UNDERSCORE))
  }
}

void addRandomMessage(Appendable sb) {
  sb.append("\nmessage ")
  addRandomMessageName(sb)
  sb.append(" {\n")
  int fieldCount = randomInt(1, 5)
  for (int i = 1; i <= fieldCount; ++i) {
    sb.append("  ")
        .append(PROTO_TYPES[rng.nextInt(PROTO_TYPES.size())])
        .append(" ")
    addRandomIdentifier(sb)
    sb.append(" = ")
        .append(i.toString())
        .append(";\n")
  }
  sb.append("}\n")
}

void addRandomProtoFile(int index, Appendable sb) {
  // File header
  sb.append('syntax = "proto3";\n')
      .append('option java_multiple_files = true;\n')
      .append('option java_package = "')

  sb.append("javapackage_").append(index.toString()).append(".")
  int javaPackageLength = randomInt(1, 5)
  addRandomIdentifier(sb)
  for (int i = 1; i < javaPackageLength; ++i) {
    sb.append(".")
    addRandomIdentifier(sb)
  }

  sb.append('";\n\n')
    .append("package ")

  sb.append("protopackage_").append(index.toString()).append(".")
  int protoPackageLength = randomInt(1, 5)
  addRandomIdentifier(sb)
  for (int i = 1; i < protoPackageLength; ++i) {
    sb.append(".")
    addRandomIdentifier(sb)
  }
  sb.append(';\n')

  // File contents, one to two messages each.
  int messageCount = randomInt(1, 2)
  for (int i = 0; i < messageCount; ++i) {
    addRandomMessage(sb)
  }
}

String randomFileName(int index) {
  StringBuilder fileName = new StringBuilder()
  // Ensure the file names remain unique.
  fileName.append(Character.toUpperCase(randomChar(ALPHA)))
  int length = randomInt(1, 10)
  for (int i = 1; i < length; ++i) {
    fileName.append(randomChar(ALPHA_NUMERIC))
  }
  return fileName.append("_${index}.proto").toString()
}

@SuppressWarnings("GroovyAssignabilityCheck")
CompletableFuture<Void> generateRandomFile(int index, Path baseDir, ExecutorService executor) {
  CompletableFuture<Void> completableFuture = new CompletableFuture<>()
  executor.submit {
    try {
      Path path = baseDir.resolve(randomFileName(index))
      try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
        addRandomProtoFile(index, writer)
      }
      completableFuture.complete(null)
    } catch (Exception ex) {
      completableFuture.completeExceptionally(ex)
    }
  }
  return completableFuture
}

void generateRandomFiles(int count, ExecutorService executor) {
  println("Generating ${count} random proto files files for this test...")
  Path baseDir = basedir.toPath().resolve("src").resolve("main").resolve("protobuf")
  Files.createDirectories(baseDir)
  CompletableFuture[] futures = new CompletableFuture[count]
  for (int i = 0; i < count; ++i) {
    futures[i] = generateRandomFile(i, baseDir, executor)
  }
  CompletableFuture.allOf(futures).join()
}

ExecutorService executor = Executors.newFixedThreadPool(30)
try {
  generateRandomFiles(500, executor)
} finally {
  executor.shutdown()
}
