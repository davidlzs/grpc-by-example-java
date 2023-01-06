package com.example.grpc.server;

import com.example.grpc.EchoRequest;
import com.example.grpc.EchoResponse;
import com.example.grpc.EchoServiceGrpc;
import io.grpc.stub.StreamObserver;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.logging.Logger;

public class EchoServiceImpl extends EchoServiceGrpc.EchoServiceImplBase {
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
