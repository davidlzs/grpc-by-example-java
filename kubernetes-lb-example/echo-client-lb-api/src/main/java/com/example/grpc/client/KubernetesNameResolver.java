package com.example.grpc.client;

import io.fabric8.kubernetes.api.model.Endpoints;
import io.fabric8.kubernetes.client.KubernetesClient;
import io.fabric8.kubernetes.client.KubernetesClientBuilder;
import io.fabric8.kubernetes.client.Watcher;
import io.fabric8.kubernetes.client.WatcherException;
import io.grpc.Attributes;
import io.grpc.EquivalentAddressGroup;
import io.grpc.NameResolver;
import io.grpc.internal.SharedResourceHolder;

import javax.annotation.concurrent.GuardedBy;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Created by rayt on 6/22/17.
 */
public class KubernetesNameResolver extends NameResolver {
  private final String namespace;
  private final String name;
  private final int port;
  private final Args args;
  private final SharedResourceHolder.Resource<ScheduledExecutorService> timerServiceResource;
  private final SharedResourceHolder.Resource<Executor> sharedChannelExecutorResource;
  private final KubernetesClient kubernetesClient;
  private Listener listener;

  private volatile boolean refreshing = false;
  private volatile boolean watching = false;

  public KubernetesNameResolver(String namespace, String name, int port, NameResolver.Args args, SharedResourceHolder.Resource<ScheduledExecutorService> timerServiceResource, SharedResourceHolder.Resource<Executor> sharedChannelExecutorResource) {
    this.namespace = namespace;
    this.name = name;
    this.port = port;
    this.args = args;
    this.timerServiceResource = timerServiceResource;
    this.sharedChannelExecutorResource = sharedChannelExecutorResource;
    this.kubernetesClient = new KubernetesClientBuilder().build();
  }

  @Override
  public String getServiceAuthority() {
    return kubernetesClient.getMasterUrl().getAuthority();
  }

  @Override
  public void start(Listener listener) {
    this.listener = listener;
    refresh();
  }

  @Override
  public void shutdown() {
    kubernetesClient.close();
  }

  @Override
  @GuardedBy("this")
  public void refresh() {
    if (refreshing) return;
    try {
      refreshing = true;

      Endpoints endpoints = kubernetesClient.endpoints().inNamespace(namespace)
          .withName(name)
          .get();

      if (endpoints == null) {
        // Didn't find anything, retrying
        ScheduledExecutorService timerService = SharedResourceHolder.get(timerServiceResource);
        timerService.schedule(() -> {
          refresh();
        }, 30, TimeUnit.SECONDS);
        return;
      }

      update(endpoints);
      watch();
    } finally {
      refreshing = false;
    }
  }

  private void update(Endpoints endpoints) {
      List<EquivalentAddressGroup> servers = new ArrayList<>();
      if (endpoints.getSubsets() == null) return;
      endpoints.getSubsets().stream().forEach(subset -> {
        long matchingPorts = subset.getPorts().stream().filter(p -> {
          return p != null && p.getPort() == port;
        }).count();
        if (matchingPorts > 0) {
          subset.getAddresses().stream().map(address -> {
            return new EquivalentAddressGroup(new InetSocketAddress(address.getIp(), port));
          }).forEach(address -> {
            servers.add(address);
          });
        }
      });

      listener.onAddresses(servers, Attributes.EMPTY);
  }

  @GuardedBy("this")
  protected void watch() {
    if (watching) return;
    watching = true;

    kubernetesClient.endpoints().inNamespace(namespace)
        .withName(name)
        .watch(new Watcher<Endpoints>() {
          @Override
          public void eventReceived(Action action, Endpoints endpoints) {
            switch (action) {
              case MODIFIED:
              case ADDED:
                update(endpoints);
                return;
              case DELETED:
                listener.onAddresses(Collections.emptyList(), Attributes.EMPTY);
                return;
            }
          }

          @Override
          public void onClose(WatcherException e) {
            watching = false;
          }
        });
  }
}
