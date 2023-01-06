/*
 * Copyright 2016 Google, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.grpc.client;

import com.example.grpc.GreetingServiceGrpc;
import com.example.grpc.HelloRequest;
import com.example.grpc.HelloResponse;
import com.example.grpc.Sentiment;
import io.grpc.Channel;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;

/**
 * Created by rayt on 5/16/16.
 */
public class MyGrpcClient {
  private final Channel channel;

  public MyGrpcClient(Channel channel) {
    this.channel = channel;
  }

  public static void main(String[] args) throws InterruptedException {
    ManagedChannel channel = ManagedChannelBuilder.forAddress("localhost", 8080)
        .usePlaintext()
        .build();

    MyGrpcClient client = new MyGrpcClient(channel);
    client.greet("Ray", 18, Sentiment.HAPPY);

    channel.shutdown();
  }

  public void greet(String name, int age, Sentiment mood) {
    GreetingServiceGrpc.GreetingServiceBlockingStub stub =
        GreetingServiceGrpc.newBlockingStub(channel);

    HelloRequest request = HelloRequest.newBuilder()
            .setName(name)
            .setAge(age)
            .setSentiment(mood)
            .build();
    HelloResponse response;
    try {
      response = stub.greeting(request);
    } catch (StatusRuntimeException e) {
      System.err.println("RPC failed: " + e.getStatus());
      return;
    }

    System.out.println(response);
  }
}
