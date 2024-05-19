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

package org.example.helloworld;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.grpc.ManagedChannelBuilder;
import io.grpc.ServerBuilder;
import org.junit.jupiter.api.Test;

class GreetingServiceImplTest {
  @Test
  void greetingServiceWorksAsExpected() throws Throwable {
    // Given
    var service = new GreetingServiceImpl();
    var server = ServerBuilder.forPort(8080).addService(service).build();
    var channel = ManagedChannelBuilder.forAddress("localhost", 8080).usePlaintext().build();
    var stub = GreetingServiceGrpc.newBlockingStub(channel);

    try {
      server.start();

      // When
      var request = GreetingRequest.newBuilder().setName("Ashley").build();
      var response = stub.greet(request);

      // Then
      assertEquals("Hello, Ashley!", response.getText());
    } finally {
      server.shutdown();
      server.awaitTermination();
    }
  }
}
