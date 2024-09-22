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

package org.example.protocplugin;

import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public class ProtocPlugin {
  public static void main(String[] args) throws Throwable {
    var request = CodeGeneratorRequest.parseFrom(System.in);

    var jvmArgs = new StringBuilder();
    System.getProperties()
        .forEach((k, v) -> jvmArgs.append(k).append("=").append(v).append("\n"));

    var listingFile = CodeGeneratorResponse.File.newBuilder()
        .setName("jvm-args.txt")
        .setContent(jvmArgs.toString())
        .build();

    CodeGeneratorResponse.newBuilder()
        .addFile(listingFile)
        .build()
        .writeTo(System.out);
  }
}
