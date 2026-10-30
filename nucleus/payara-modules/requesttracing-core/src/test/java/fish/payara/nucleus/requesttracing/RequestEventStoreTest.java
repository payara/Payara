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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import fish.payara.notification.requesttracing.RequestTrace;
import org.junit.Before;
import org.junit.Test;

import fish.payara.notification.requesttracing.EventType;
import fish.payara.notification.requesttracing.RequestTraceSpan;

/**
 *
 * @author steve
 */
public class RequestEventStoreTest {
    
    private Map<Long,RequestTrace> tracesbyThreadId;
    private RequestTraceSpanStore eventStore;

    @Before
    public void setUp() {
        tracesbyThreadId = Collections.synchronizedMap(new HashMap<>());
        eventStore = new RequestTraceSpanStore();
    }

    @Test
    public void testStoreEvent() {
        RequestTraceSpan re = new RequestTraceSpan(EventType.TRACE_START, "Start");
        eventStore.storeEvent(re);
        eventStore.storeEvent(new RequestTraceSpan("Test"));
        eventStore.endTrace();
        assertEquals(2, eventStore.getTrace().getTraceSpans().size());
    }
    
    @Test
    public void testFlushStore() {
        testStoreEvent();
        eventStore.flushStore();
        assertEquals(0, eventStore.getTrace().getTraceSpans().size());
    }

    /**
     * Test of storeEvent method utilising multiple threads ensuring traces are separate
     */
    @Test
    public void testStoreEventMultipleThreads() throws InterruptedException {
        Thread thread1 = new Thread(new Runnable() {
            @Override
            public void run() {
                long threadID = Thread.currentThread().getId();
                RequestTraceSpan re = new RequestTraceSpan(EventType.TRACE_START, "Start"+threadID);
                eventStore.storeEvent(re);
                for (int i = 0; i < 100; i++) {
                    re = new RequestTraceSpan("Event-"+i+"-"+threadID);
                    eventStore.storeEvent(re);
                }
                eventStore.endTrace();
                tracesbyThreadId.put(threadID, eventStore.getTrace());
            }
        });
        
        Thread thread2 = new Thread(new Runnable() {
            @Override
            public void run() {
                long threadID = Thread.currentThread().getId();
                RequestTraceSpan re = new RequestTraceSpan(EventType.TRACE_START, "Start"+threadID);
                eventStore.storeEvent(re);
                for (int i = 0; i < 200; i++) {
                    re = new RequestTraceSpan("Event-"+i+"-"+threadID);
                    eventStore.storeEvent(re);
                }
                eventStore.endTrace();
                tracesbyThreadId.put(threadID, eventStore.getTrace());
            }
        });
        
        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();
        
        RequestTrace trace1 = tracesbyThreadId.get(thread1.getId());
        RequestTrace trace2 = tracesbyThreadId.get(thread2.getId());
        
        // make sure we have something of the correct length in each trace
        assertNotNull(trace2);
        assertNotNull(trace1);
        assertEquals(201, trace2.getTraceSpans().size());
        assertEquals(101,trace1.getTraceSpans().size());
        
        // ensure they are different classes
        assertNotSame(trace2, trace1);
        
        // assert last names are correct
        assertEquals("Event-99-"+thread1.getId(), trace1.getTraceSpans().getLast().getEventName());
        assertEquals("Event-199-"+thread2.getId(), trace2.getTraceSpans().getLast().getEventName());
        
        //assert start names are correct
        assertEquals("Start"+thread1.getId(), trace1.getTraceSpans().getFirst().getEventName());
        assertEquals("Start"+thread2.getId(), trace2.getTraceSpans().getFirst().getEventName());
        
        // assert all conversation IDs are correct trace 1
        UUID convID = trace1.getTraceSpans().getFirst().getTraceId();
        for (RequestTraceSpan re : trace1.getTraceSpans()) {
            assertEquals(convID,re.getTraceId());
        }
        
        // assert all conversation IDs are correct trace 2
        UUID convID2 = trace2.getTraceSpans().getFirst().getTraceId();
        for (RequestTraceSpan re : trace2.getTraceSpans()) {
            assertEquals(convID2,re.getTraceId());
        }

    }

    /**
     * Test of getElapsedTime method, of class RequestEventStore.
     * @throws java.lang.InterruptedException
     */
    @Test
    public void testGetElapsedTime() throws InterruptedException {
        RequestTraceSpan re = new RequestTraceSpan(EventType.TRACE_START, "Start");
        eventStore.storeEvent(re);
        Thread.sleep(100);
        eventStore.storeEvent(new RequestTraceSpan("Test"));
        eventStore.endTrace();
        assertEquals(2, eventStore.getTrace().getTraceSpans().size());
        assertTrue(eventStore.getElapsedTime() >= 90);
    }
    
    
    @Test
    public void testFlushMultipleThreads() throws InterruptedException {
        
        // thread 1 builds up a trace
        Thread thread1 = new Thread(new Runnable() {
            @Override
            public void run(){
                long threadID = Thread.currentThread().getId();
                RequestTraceSpan re = new RequestTraceSpan(EventType.TRACE_START, "Start"+threadID);
                eventStore.storeEvent(re);
                for (int i = 0; i < 10; i++) {
                    re = new RequestTraceSpan("Event-"+i+"-"+threadID);
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(RequestEventStoreTest.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    eventStore.storeEvent(re);
                }
                eventStore.endTrace();
                tracesbyThreadId.put(threadID, eventStore.getTrace());
            }
        });
        
        // thread 2 continually flushes the store
        Thread thread2 = new Thread(new Runnable() {
            @Override
            public void run() {
                long threadID = Thread.currentThread().getId();
                for (int i = 0; i < 10; i++) {
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(RequestEventStoreTest.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    eventStore.flushStore();
                }                
                tracesbyThreadId.put(threadID, eventStore.getTrace());
            }
        });
        
        thread1.start();
        thread2.start();
        thread1.join();
        thread2.join();
        
        RequestTrace trace1 = tracesbyThreadId.get(thread1.getId());
        RequestTrace trace2 = tracesbyThreadId.get(thread2.getId());
        
        // check trace sizes 
        assertEquals(11, trace1.getTraceSpans().size());
        assertEquals(0, trace2.getTraceSpans().size());
        
        // check trace 1 has an elapsed time in the correct range
        assertTrue(trace1.getElapsedTime() >= 90);
        
    }
    
    @Test
    public void testResettingConversationID() {
        RequestTraceSpan re = new RequestTraceSpan(EventType.TRACE_START, "Start");
        eventStore.storeEvent(re);
        for (int i = 0; i < 10; i++) {
            re = new RequestTraceSpan("Event-"+i);
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                Logger.getLogger(RequestEventStoreTest.class.getName()).log(Level.SEVERE, null, ex);
            }
            eventStore.storeEvent(re);
        }
//        eventStore.storeEvent(new RequesTraceSpan(EventType.TRACE_END, "End"));
        eventStore.endTrace();
        
        UUID oldID = eventStore.getTrace().getTraceSpans().getFirst().getTraceId();
        UUID newID = UUID.randomUUID();
        eventStore.setTraceId(newID);
        for (RequestTraceSpan event : eventStore.getTrace().getTraceSpans()) {
            assertEquals(newID, event.getTraceId());
        }
        
    }
    
    @Test
    public void isInProgress() {
        assertFalse(eventStore.isTraceInProgress());
        RequestTraceSpan re = new RequestTraceSpan(EventType.TRACE_START, "Start");
        eventStore.storeEvent(re);
        for (int i = 0; i < 10; i++) {
            re = new RequestTraceSpan("Event-"+i);
            try {
                Thread.sleep(10);
            } catch (InterruptedException ex) {
                Logger.getLogger(RequestEventStoreTest.class.getName()).log(Level.SEVERE, null, ex);
            }
            eventStore.storeEvent(re);
        }
        assertTrue(eventStore.isTraceInProgress());
//        eventStore.storeEvent(new RequesTraceSpan(EventType.TRACE_END, "End"));
        eventStore.endTrace();
        assertFalse(eventStore.isTraceInProgress());        
    }


    
}
