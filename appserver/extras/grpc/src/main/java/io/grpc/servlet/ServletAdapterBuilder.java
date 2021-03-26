/*
 * Copyright 2018 The gRPC Authors
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

package io.grpc.servlet;

import static io.grpc.internal.GrpcUtil.DEFAULT_MAX_MESSAGE_SIZE;
import static io.grpc.servlet.Preconditions.checkNotNull;
import static io.grpc.servlet.Preconditions.checkState;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;

import javax.annotation.concurrent.NotThreadSafe;

import io.grpc.BindableService;
import io.grpc.CompressorRegistry;
import io.grpc.DecompressorRegistry;
import io.grpc.ExperimentalApi;
import io.grpc.HandlerRegistry;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.ServerServiceDefinition;
import io.grpc.ServerStreamTracer;
import io.grpc.ServerStreamTracer.Factory;
import io.grpc.internal.AbstractServerImplBuilder;
import io.grpc.internal.GrpcUtil;
import io.grpc.internal.InternalServer;
import io.grpc.internal.ServerTransportListener;
import io.grpc.internal.SharedResourceHolder;

/**
 * Builder to build a gRPC server that can run as a servlet. This is for advanced custom settings.
 * Normally, users should consider extending the out-of-box {@link GrpcServlet} directly instead.
 *
 * <p>The API is experimental. The authors would like to know more about the real usecases. Users
 * are welcome to provide feedback by commenting on
 * <a href=https://github.com/grpc/grpc-java/issues/5066>the tracking issue</a>.
 */
@ExperimentalApi("https://github.com/grpc/grpc-java/issues/5066")
@NotThreadSafe
public final class ServletAdapterBuilder extends ServerBuilder<ServletAdapterBuilder> {

  @SuppressWarnings("rawtypes")
  private AbstractServerImplBuilder delegate = new AbstractServerImplBuilder() {
    @SuppressWarnings("unchecked")
    protected List<? extends InternalServer> buildTransportServers(List streamTracerFactories) {
        return ServletAdapterBuilder.this.buildTransportServers(streamTracerFactories);
    }
    @Override
    public AbstractServerImplBuilder<?> useTransportSecurity(File certChain, File privateKey) {
      throw new UnsupportedOperationException("TLS should be configured by the servlet container");
    }
  };

  List<? extends ServerStreamTracer.Factory> streamTracerFactories;
  int maxInboundMessageSize = DEFAULT_MAX_MESSAGE_SIZE;

  private ScheduledExecutorService scheduler;
  private boolean internalCaller;
  private boolean usingCustomScheduler;
  private InternalServerImpl internalServer;

  /**
   * Builds a gRPC server that can run as a servlet.
   *
   * <p>The returned server will not be started or bound to a port.
   *
   * <p>Users should not call this method directly. Instead users should call
   * {@link #buildServletAdapter()} which internally will call {@code build()} and {@code start()}
   * appropriately.
   *
   * @throws IllegalStateException if this method is called by users directly
   */
  @Override
  public Server build() {
    checkState(internalCaller, "build() method should not be called directly by an application");
    return delegate.build();
  }

  /**
   * Creates a {@link ServletAdapter}.
   */
  public ServletAdapter buildServletAdapter() {
    return new ServletAdapter(buildAndStart(), streamTracerFactories, maxInboundMessageSize);
  }

  private ServerTransportListener buildAndStart() {
    try {
      internalCaller = true;
      build().start();
    } catch (IOException e) {
      // actually this should never happen
      throw new RuntimeException(e);
    } finally {
      internalCaller = false;
    }

    if (!usingCustomScheduler) {
      scheduler = SharedResourceHolder.get(GrpcUtil.TIMER_SERVICE);
    }

    // Create only one "transport" for all requests because it has no knowledge of which request is
    // associated with which client socket. This "transport" does not do socket connection, the
    // container does.
    ServerTransportImpl serverTransport =
        new ServerTransportImpl(scheduler, usingCustomScheduler);
    return internalServer.serverListener.transportCreated(serverTransport);
  }

  protected List<? extends InternalServer> buildTransportServers(
      List<? extends Factory> streamTracerFactories) {
    checkNotNull(streamTracerFactories, "streamTracerFactories");
    this.streamTracerFactories = streamTracerFactories;
    internalServer = new InternalServerImpl();
    return Arrays.asList(internalServer);
  }

  /**
   * Provides a custom scheduled executor service to the server builder.
   *
   * @return this
   */
  public ServletAdapterBuilder scheduledExecutorService(ScheduledExecutorService scheduler) {
    this.scheduler = checkNotNull(scheduler, "scheduler");
    usingCustomScheduler = true;
    return this;
  }

  /**
   * Throws {@code UnsupportedOperationException}. TLS should be configured by the servlet
   * container.
   */
  @Override
  public ServletAdapterBuilder useTransportSecurity(File certChain, File privateKey) {
    delegate.useTransportSecurity(certChain, privateKey);
    return this;
  }

  @Override
  public ServletAdapterBuilder maxInboundMessageSize(int bytes) {
    delegate.maxInboundMessageSize(bytes);
    maxInboundMessageSize = bytes;
    return this;
  }

  @Override
  public ServletAdapterBuilder directExecutor() {
    delegate.directExecutor();
    return this;
  }

  @Override
  public ServletAdapterBuilder executor(Executor executor) {
    delegate.executor(executor);
    return this;
  }

  @Override
  public ServletAdapterBuilder addService(ServerServiceDefinition service) {
    delegate.addService(service);
    return this;
  }

  @Override
  public ServletAdapterBuilder addService(BindableService bindableService) {
    delegate.addService(bindableService);
    return this;
  }

  @Override
  public ServletAdapterBuilder fallbackHandlerRegistry(HandlerRegistry registry) {
    delegate.fallbackHandlerRegistry(registry);
    return this;
  }

  @Override
  public ServletAdapterBuilder decompressorRegistry(DecompressorRegistry registry) {
    delegate.decompressorRegistry(registry);
    return this;
  }

  @Override
  public ServletAdapterBuilder compressorRegistry(CompressorRegistry registry) {
    delegate.compressorRegistry(registry);
    return this;
  }

}
