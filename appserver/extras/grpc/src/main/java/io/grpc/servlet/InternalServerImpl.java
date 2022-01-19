/*
 * Copyright 2018-2022 The gRPC Authors
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

import java.net.SocketAddress;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import io.grpc.InternalChannelz.SocketStats;
import io.grpc.InternalInstrumented;
import io.grpc.internal.InternalServer;
import io.grpc.internal.ServerListener;

import javax.annotation.Nullable;

final class InternalServerImpl implements InternalServer {

    protected ServerListener serverListener;

    protected InternalServerImpl() {
    }

    @Override
    public void start(ServerListener listener) {
        serverListener = listener;
    }

    @Override
    public void shutdown() {
        if (serverListener != null) {
            serverListener.serverShutdown();
        }
    }

    @Override
    public SocketAddress getListenSocketAddress() {
        return new SocketAddress() {
            private static final long serialVersionUID = 1L;

            @Override
            public String toString() {
                return "ServletServer";
            }
        };
    }

    @Override
    public InternalInstrumented<SocketStats> getListenSocketStats() {
        // sockets are managed by the servlet container, grpc is ignorant of that
        return null;
    }

    @Nullable
    @Override
    public List<InternalInstrumented<SocketStats>> getListenSocketStatsList() {
        return null;
    }

    @Override
    public List<? extends SocketAddress> getListenSocketAddresses() {
        return Collections.unmodifiableList(Arrays.asList(this.getListenSocketAddress()));
    }
}