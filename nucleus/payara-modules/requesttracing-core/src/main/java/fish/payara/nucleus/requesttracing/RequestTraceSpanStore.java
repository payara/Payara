/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2018 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package fish.payara.nucleus.requesttracing;

import fish.payara.notification.requesttracing.RequestTrace;
import fish.payara.notification.requesttracing.EventType;
import fish.payara.notification.requesttracing.RequestTraceSpan;
import fish.payara.nucleus.requesttracing.store.IterableThreadLocal;

import org.jvnet.hk2.annotations.Service;

import javax.inject.Singleton;

import java.util.Map.Entry;
import java.util.UUID;

/**
 * @author mertcaliskan
 *
 * Stores all request events  in a thread local that are being traced.
 * The start of the trace is marked with {@link EventType#TRACE_START} and the end of the trace is marked with {@link EventType#TRACE_END}.
 * All the request events placed in between are marked with a conversation id, which is the id of request event with type {@link EventType#TRACE_START}.
 * @since 4.1.1.163
 */
@Service
@Singleton
public class RequestTraceSpanStore {

    private IterableThreadLocal<RequestTrace> spanStore = new IterableThreadLocal<>(RequestTrace::new);

    public Iterable<Entry<Thread, RequestTrace>> getTraces() {
        return spanStore;
    }

    /**
     * Adds a new event to the request trace
     * @param payaraSpan 
     */
    void storeEvent(RequestTraceSpan payaraSpan) {
        RequestTrace currentTrace = spanStore.get();
        currentTrace.addEvent(payaraSpan);
    }

    void storeEvent(RequestTraceSpan payaraSpan, long timestampMillis) {
        RequestTrace currentTrace = spanStore.get();
        currentTrace.addEvent(payaraSpan, timestampMillis);
    }

    void endTrace() {
        RequestTrace currentTrace = spanStore.get();
        currentTrace.endTrace();
    }

    /**
     * Returns how long the request trace took
     * @return 0 if the trace has not finished
     */
    long getElapsedTime() {
        return spanStore.get().getElapsedTime();
    }

    /**
     * Clears the stored request trace and
     */
    void flushStore() {
        spanStore.set(new RequestTrace());
    }

    /**
     * Returns the full request trace as a string
     * @return A JSON-style representation of the request trace
     */
    String getTraceAsString() {
        return spanStore.get().toString();
    }

    // test methods
    /**
     * Returns the full trace
     * @return 
     * @since 4.1.2.173
     */
    public RequestTrace getTrace() {
        return spanStore.get();
    }

    /**
     * Gets the unique identifier for the request trace
     * @return 
     */
    UUID getTraceID() {
        RequestTrace trace = spanStore.get();
        return trace.getTraceId();
    }

    void setTraceId(UUID newID) {
        RequestTrace trace = spanStore.get();
        trace.setTraceId(newID);
    }

    /**
     * Returns true if a request trace has been started and has not been completed
     * @return 
     */
    boolean isTraceInProgress() {
        return (spanStore.get().isStarted() && !spanStore.get().isCompleted());
    }
}