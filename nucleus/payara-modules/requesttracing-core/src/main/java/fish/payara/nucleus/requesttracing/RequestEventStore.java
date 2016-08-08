/*
 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 Copyright (c) 2016 C2B2 Consulting Limited. All rights reserved.
 The contents of this file are subject to the terms of the Common Development
 and Distribution License("CDDL") (collectively, the "License").  You
 may not use this file except in compliance with the License.  You can
 obtain a copy of the License at
 https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 or packager/legal/LICENSE.txt.  See the License for the specific
 language governing permissions and limitations under the License.
 When distributing the software, include this License Header Notice in each
 file and include the License file at packager/legal/LICENSE.txt.
 */
package fish.payara.nucleus.requesttracing;

import fish.payara.nucleus.requesttracing.domain.EventType;
import fish.payara.nucleus.requesttracing.domain.RequestEvent;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Singleton;
import java.util.LinkedList;
import java.util.List;

/**
 * @author mertcaliskan
 *
 * Stores all request events that are being traced down by {@link fish.payara.nucleus.requesttracing.interceptor.PayaraServletContainerInterceptor} in linked list placed in a thread local.
 * The start of the trace is marked with {@link EventType#TRACE_START} and the end of the trace is marked with {@link EventType#TRACE_END}.
 * All the request events placed in between are marked with a conversation id, which is the id of request event with type {@link EventType#TRACE_START}.
 */
@Service
@Singleton
public class RequestEventStore {

    private ThreadLocal<RequestTrace> eventStore = new ThreadLocal<RequestTrace>() {
        @Override
        protected RequestTrace initialValue() {
            return new RequestTrace();
        }      
    };

    void storeEvent(RequestEvent requestEvent) {       
        RequestTrace currentTrace = eventStore.get();
        currentTrace.addEvent(requestEvent);
    }

    long getElapsedTime() {
        return eventStore.get().getElapsedTime();
    }

    void flushStore() {
        eventStore.set(new RequestTrace());
    }

    String getTraceAsString() {
        return eventStore.get().toString();
    }
    
    // test methods
    RequestTrace getTrace() {
        return eventStore.get();
    }
}