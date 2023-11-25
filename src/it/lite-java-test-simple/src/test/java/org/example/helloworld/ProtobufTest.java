/*
 * Copyright (C) 2023, Ashley Scopes.
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

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class ProtobufTest {

  @Test
  void generatedProtobufSourcesAreLiteMessages() throws Throwable {
    // When
    var superClasses = new ArrayList<String>();
    Class<?> superClass = GreetingRequest.class;

    do {
      superClasses.add(superClass.getName());
      superClass = superClass.getSuperclass();
    } while (superClass != null);

    // Then
    assertFalse(superClasses.contains("com.google.protobuf.GeneratedMessageV3"));
  }

  @Test
  void generatedProtobufSourcesAreValid() throws Throwable {
    // Given
    var expectedGreetingRequest = GreetingRequest
        .newBuilder()
        .setName("Ashley")
        .build();

    // When
    var baos = new ByteArrayOutputStream();
    expectedGreetingRequest.writeTo(baos);
    var actualGreetingRequest = GreetingRequest.parseFrom(baos.toByteArray());

    assertNotEquals(0, baos.toByteArray().length);

    // Then
    assertEquals(expectedGreetingRequest.getName(), actualGreetingRequest.getName());
  }
}
