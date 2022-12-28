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

package com.example.grpc.server;

import com.example.grpc.EchoRequest;
import com.example.grpc.EchoResponse;
import com.example.grpc.EchoServiceGrpc;
import io.grpc.Server;
import io.grpc.netty.shaded.io.grpc.netty.NettyServerBuilder;
import io.grpc.stub.StreamObserver;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.time.Duration;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;
import java.util.logging.Logger;

/**
 * Created by rayt on 5/16/16.
 */
public class EchoServer {
  private static CountDownLatch countDownLatch = new CountDownLatch(1);

  static public void main(String[] args) throws IOException, InterruptedException {

    EchoServiceImpl echoService = new EchoServiceImpl();
    Server server = NettyServerBuilder.forPort(8080)
//            .maxConnectionAge(5, TimeUnit.SECONDS)
//            .maxConnectionAgeGrace(3, TimeUnit.SECONDS)
        .addService(echoService).build();

    System.out.println("Starting server...");
    server.start();
    System.out.println("Server started!");
    server.awaitTermination();

/*
    Runtime.getRuntime().addShutdownHook(new Thread(() -> {
      server.shutdown();
      try {
        server.awaitTermination();
        waitFor(Duration.ofSeconds(30), (t) -> t.amountOfActiveObserversCounter() <= 0, echoService.streamObserverPool);
      } catch (InterruptedException e) {
      }
    }));

    countDownLatch.await();*/
  }

  private static void waitFor(Duration maxDuration, Predicate<StreamObserverPool> predicate, StreamObserverPool pool) {
    long endTime = System.currentTimeMillis() + maxDuration.toMillis();
    while (!predicate.test(pool) && System.currentTimeMillis() < endTime) {
      try {
        Thread.sleep(200);
      } catch (InterruptedException e) {
      }
    }

    countDownLatch.countDown();
    if(System.currentTimeMillis() >= endTime) {
      throw new IllegalStateException("Timeout :-(");
    } else {
      System.out.println(" all active streams are done!");
    }
  }
}

class EchoServiceImpl extends EchoServiceGrpc.EchoServiceImplBase {
  private static Logger LOGGER = Logger.getLogger(EchoServiceImpl.class.getName());

  public StreamObserverPool streamObserverPool = new StreamObserverPool();

  @Override
  public void echo(EchoRequest request, StreamObserver<EchoResponse> responseObserver) {

    StreamObserver<EchoResponse> wrappedResponseStreamObserver = streamObserverPool.allocate(responseObserver);

    try {
      String from = InetAddress.getLocalHost().getHostAddress();
      System.out.println("Received: " + request.getMessage());
      wrappedResponseStreamObserver.onNext(EchoResponse.newBuilder()
          .setFrom(from)
          .setMessage(request.getMessage())
          .build());
      wrappedResponseStreamObserver.onCompleted();
    } catch (UnknownHostException e) {
      wrappedResponseStreamObserver.onError(e);
    }
  }
}
