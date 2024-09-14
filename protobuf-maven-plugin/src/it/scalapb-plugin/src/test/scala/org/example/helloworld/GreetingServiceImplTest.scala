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

package org.example.helloworld

import java.util.concurrent.TimeUnit
import io.grpc.ManagedChannelBuilder
import io.grpc.ServerBuilder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import scala.concurrent.ExecutionContext

class GreetingServiceImplTest:
  @Test
  def greetingServiceWorksAsExpected: Unit =
    // Given
    val server = ServerBuilder
        .forPort(10003)
        .addService(GreetingServiceGrpc.bindService(new GreetingServiceImpl, ExecutionContext.global))
        .build

    val channel = ManagedChannelBuilder
        .forAddress("localhost", 10003)
        .usePlaintext
        .build

    val stub = GreetingServiceGrpc.blockingStub(channel)

    try
      server.start

      // When
      var request = GreetingRequest(name = "Ashley")
      var response = stub.greet(request)

      // Then
      assertEquals("Hello, Ashley!", response.text)
    finally
      channel.shutdown
      channel.awaitTermination(10, TimeUnit.SECONDS)

      server.shutdown
      server.awaitTermination(10, TimeUnit.SECONDS)

end GreetingServiceImplTest
