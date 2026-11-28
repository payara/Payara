/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 */
package com.sun.enterprise.v3.services.impl;

import java.nio.charset.StandardCharsets;
import org.glassfish.grizzly.filterchain.FilterChainContext;
import org.glassfish.grizzly.http.HttpRequestPacket;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.memory.Buffers;
import org.glassfish.grizzly.memory.MemoryManager;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.ServiceLocatorFactory;
import org.glassfish.internal.grizzly.ContextMapper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ContainerMapperTest {

    private ServiceLocator habitat;
    private ContainerMapper mapper;
    private ContextMapper contextMapper;
    private Response response;
    private int status;
    private int handlerInvocations;

    @Before
    public void setUp() {
        habitat = ServiceLocatorFactory.getInstance().create("ContainerMapperTest");
        GrizzlyService service = new GrizzlyService() {
            @Override
            public ServiceLocator getHabitat() {
                return habitat;
            }
        };
        mapper = new ContainerMapper(service, null);
        contextMapper = new ContextMapper();
        mapper.setMapper(contextMapper);
        response = new Response() {
            @Override
            public void sendError(int sc) {
                status = sc;
            }

            @Override
            public void sendError(int sc, String message) {
                status = sc;
            }
        };
    }

    @After
    public void tearDown() {
        ServiceLocatorFactory.getInstance().destroy(habitat);
    }

    @Test
    public void invalidURIReturns400Test() throws Exception {
        for (String uri : new String[]{"/%%3", "/%a", "/%ZZ", "/%00"}) {
            for (boolean useBuffer : new boolean[]{false, true}) {
                status = 0;
                mapper.service(createRequest(uri, useBuffer), response);
                assertEquals(uri, 400, status);
            }
        }
    }

    @Test
    public void handlerFailureReturns500Test() throws Exception {
        for (RuntimeException failure : new RuntimeException[]{
                new IllegalArgumentException("handler argument"),
                new IllegalStateException("handler state")}) {
            contextMapper.setHttpHandler(new HttpHandler() {
                @Override
                public void service(Request request, Response response) {
                    handlerInvocations++;
                    throw failure;
                }
            });
            status = 0;
            mapper.service(createRequest("/ok", true), response);
            assertEquals(failure.getClass().getSimpleName(), 500, status);
        }
        assertEquals(2, handlerInvocations);
    }

    private Request createRequest(String uri, boolean useBuffer) {
        HttpRequestPacket packet = HttpRequestPacket.builder().uri(uri).build();
        byte[] rawUri = uri.getBytes(StandardCharsets.US_ASCII);
        if (useBuffer) {
            packet.getRequestURIRef().init(Buffers.wrap(MemoryManager.DEFAULT_MEMORY_MANAGER, rawUri), 0, rawUri.length);
        } else {
            packet.getRequestURIRef().init(rawUri, 0, rawUri.length);
        }
        Request request = new Request();
        request.initialize(packet, new FilterChainContext(), null);
        return request;
    }
}
