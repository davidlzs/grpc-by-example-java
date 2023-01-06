package com.example.grpc;

import com.example.grpc.server.EchoServiceImpl;
import io.grpc.inprocess.InProcessChannelBuilder;
import io.grpc.inprocess.InProcessServerBuilder;
import io.grpc.testing.GrpcCleanupRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;

public class EchoServiceImplTest {
    /**
     * This rule manages automatic graceful shutdown for the registered servers and channels at the
     * end of test.
     */
    @Rule
    public final GrpcCleanupRule grpcCleanup = new GrpcCleanupRule();

    // Generate a unique in-process server name.
    private final String serverName = InProcessServerBuilder.generateName();

    /**
     * To test the server, make calls with a real stub using the in-process channel, and verify
     * behaviors or state changes from the client side.
     */

    @Before
    public void setUp() throws IOException {
        // Create a server, add service, start, and register for automatic graceful shutdown.
        grpcCleanup.register(InProcessServerBuilder
                .forName(serverName).directExecutor().addService(new EchoServiceImpl()).build().start());
    }

    @Test
    public void greeterImpl_replyMessage() {
        // setup
        EchoServiceGrpc.EchoServiceBlockingStub blockingStub = EchoServiceGrpc.newBlockingStub(
                // Create a client channel and register for automatic graceful shutdown.
                grpcCleanup.register(InProcessChannelBuilder.forName(serverName).directExecutor().build()));

        // execute
        EchoResponse response = blockingStub.echo(EchoRequest.newBuilder().setMessage("test name").build());

        // assert
        assertEquals("test name", response.getMessage());
    }
}