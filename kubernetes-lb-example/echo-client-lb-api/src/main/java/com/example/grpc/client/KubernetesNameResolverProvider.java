package com.example.grpc.client;

import com.google.common.base.Preconditions;
import io.grpc.NameResolver;
import io.grpc.NameResolverProvider;
import io.grpc.internal.GrpcUtil;

import java.net.URI;

/**
 * Created by rayt on 6/22/17.
 */

/**
 * Usage: kubernetes:///{namespace}/{service}/{port}
 * E.g.: kubernetes:///default/echo-server/8080
 */
public class KubernetesNameResolverProvider extends NameResolverProvider {
  public static final String SCHEME = "kubernetes";

  @Override
  protected boolean isAvailable() {
    return true;
  }

  @Override
  protected int priority() {
    return 5;
  }


  @Override
  public NameResolver newNameResolver(URI targetUri, NameResolver.Args args) {
    if (SCHEME.equals(targetUri.getScheme())) {
      String targetPath = Preconditions.checkNotNull(targetUri.getPath(), "targetPath");
      Preconditions.checkArgument(targetPath.startsWith("/"),
              "the path component (%s) of the target (%s) must start with '/'", targetPath, targetUri);

      String[] parts = targetPath.split("/");
      if (parts.length != 4) {
        throw new IllegalArgumentException("Must be formatted like kubernetes:///{namespace}/{service}/{port}");
      }

      try {
        int port = Integer.valueOf(parts[3]);
        return new KubernetesNameResolver(parts[1], parts[2], port, args, GrpcUtil.TIMER_SERVICE,
                GrpcUtil.SHARED_CHANNEL_EXECUTOR);
      } catch (NumberFormatException e) {
        throw new IllegalArgumentException("Unable to parse port number", e);
      }
    } else {
      return null;
    }
  }
  @Override
  public String getDefaultScheme() {
    return SCHEME;
  }
}
