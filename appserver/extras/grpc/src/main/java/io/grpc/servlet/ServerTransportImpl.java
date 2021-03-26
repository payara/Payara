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

import java.util.concurrent.ScheduledExecutorService;

import static io.grpc.servlet.Preconditions.checkNotNull;
import com.google.common.util.concurrent.ListenableFuture;

import io.grpc.InternalChannelz.SocketStats;
import io.grpc.InternalLogId;
import io.grpc.Status;
import io.grpc.internal.GrpcUtil;
import io.grpc.internal.ServerTransport;
import io.grpc.internal.SharedResourceHolder;

final class ServerTransportImpl implements ServerTransport {

    private final InternalLogId logId = InternalLogId.allocate(ServerTransportImpl.class, null);
    private final ScheduledExecutorService scheduler;
    private final boolean usingCustomScheduler;

    public ServerTransportImpl(ScheduledExecutorService scheduler, boolean usingCustomScheduler) {
        this.scheduler = checkNotNull(scheduler, "scheduler");
        this.usingCustomScheduler = usingCustomScheduler;
    }

    @Override
    public void shutdown() {
        if (!usingCustomScheduler) {
            SharedResourceHolder.release(GrpcUtil.TIMER_SERVICE, scheduler);
        }
    }

    @Override
    public void shutdownNow(Status reason) {
        shutdown();
    }

    @Override
    public ScheduledExecutorService getScheduledExecutorService() {
        return scheduler;
    }

    @Override
    public ListenableFuture<SocketStats> getStats() {
        // does not support instrumentation
        return null;
    }

    @Override
    public InternalLogId getLogId() {
        return logId;
    }
}