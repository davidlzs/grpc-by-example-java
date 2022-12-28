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

import com.example.grpc.EchoRequest;
import com.example.grpc.EchoResponse;
import com.example.grpc.EchoServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.NameResolver;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * This is a simple client that depends on external load balancing, either via a proxy,
 * or a L4/L7 load balancer.
 */
public class SimpleEchoClient {
  private static int THREADS = 4;
  private static Random RANDOM = new Random();

  public static void main(String[] args) throws UnknownHostException {
    String host = System.getenv().getOrDefault("ECHO_SERVICE_HOST", "localhost");
    String port = System.getenv().getOrDefault("ECHO_SERVICE_PORT", "8080");
    final ManagedChannel channel =
//            ManagedChannelBuilder.forTarget(host + ":" + port)
//		.usePlaintext()
//        .build();

      ManagedChannelBuilder.forTarget(host + ":" + port)
              .defaultLoadBalancingPolicy("round_robin")
//              .keepAliveTime(2, TimeUnit.SECONDS)
//              .keepAliveTimeout(1, TimeUnit.SECONDS)
              .usePlaintext()
              .build();

    final String self = InetAddress.getLocalHost().getHostName();

    ExecutorService executorService = Executors.newFixedThreadPool(THREADS);
    for (int i = 0; i < THREADS; i++) {
      EchoServiceGrpc.EchoServiceBlockingStub stub = EchoServiceGrpc.newBlockingStub(channel);
        executorService.submit(() -> {
        while (true) {
        	try {
              EchoResponse response = stub.echo(EchoRequest.newBuilder()
                      .setMessage(self + ": " + Thread.currentThread().getName())
                      .build());
              System.out.println(response.getFrom() + " echoed");

              Thread.sleep(RANDOM.nextInt(700));
            } catch (Exception e) {
        		e.printStackTrace();
            }
        }
      });
    }
  }
}
