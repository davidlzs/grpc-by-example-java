package com.example.grpc.server;

import io.grpc.stub.StreamObserver;

import java.util.concurrent.atomic.AtomicInteger;

public class StreamObserverPool {
    private AtomicInteger amountOfActiveObserversCounter = new AtomicInteger(0);

    public int amountOfActiveObserversCounter() {
        return amountOfActiveObserversCounter.get();
    }

    public <T> StreamObserver<T> allocate(StreamObserver<T> delegate) {
        amountOfActiveObserversCounter.incrementAndGet();
        System.out.println("Creating new stream, active: " + amountOfActiveObserversCounter.get());
        return new StreamObserver<T> () {
            @Override
            public void onNext(T value) {
                delegate.onNext(value);
            }

            @Override
            public void onError(Throwable t) {
                delegate.onError(t);
                amountOfActiveObserversCounter.decrementAndGet();
                System.out.println("onError(), active: " + amountOfActiveObserversCounter.get());
            }

            @Override
            public void onCompleted() {
                delegate.onCompleted();
                amountOfActiveObserversCounter.decrementAndGet();
                System.out.println("onCompleted(), active: " + amountOfActiveObserversCounter.get());
            }
        };
    }
}