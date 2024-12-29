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
package org.example.protocplugin.backend;

import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

public final class ProtocPluginBackend {

  public ProtocPluginBackend() {
    // Nothing to do, but needed to make -Xlint be happy.
  }

  public void processCodeGenerationRequest(InputStream in, OutputStream out) throws IOException {
    var request = CodeGeneratorRequest.parseFrom(in);

    var inputFileNameList = request.getFileToGenerateList()
        .asByteStringList()
        .stream()
        .map(buff -> buff.toString(StandardCharsets.UTF_8))
        .sorted()
        .collect(Collectors.joining("\n"));

    var listingFile = CodeGeneratorResponse.File.newBuilder()
        .setName("file-listing.txt")
        .setContent(inputFileNameList)
        .build();

    CodeGeneratorResponse.newBuilder()
        .addFile(listingFile)
        .build()
        .writeTo(out);
  }
}
