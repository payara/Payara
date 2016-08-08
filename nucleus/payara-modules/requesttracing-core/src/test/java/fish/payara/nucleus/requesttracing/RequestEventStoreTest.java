/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.nucleus.requesttracing;

import fish.payara.nucleus.requesttracing.domain.EventType;
import fish.payara.nucleus.requesttracing.domain.RequestEvent;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author steve
 */
public class RequestEventStoreTest {
    
    private Map<Long,RequestTrace> tracesbyThreadId;
    private RequestEventStore eventStore;
    
    public RequestEventStoreTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
        tracesbyThreadId = Collections.synchronizedMap(new HashMap<Long,RequestTrace>());
        eventStore = new RequestEventStore();
    }
    
    @After
    public void tearDown() {
    }
    
    @Test
    public void testStoreEvent() {
        RequestEvent re = new RequestEvent(EventType.TRACE_START, "Start");
        eventStore.storeEvent(re);
        eventStore.storeEvent(new RequestEvent("Test"));
        eventStore.storeEvent(new RequestEvent(EventType.TRACE_END, "End"));
        assertEquals(3, eventStore.getTrace().getTrace().size());
    }
    
    @Test
    public void testFlushStore() {
        testStoreEvent();
        eventStore.flushStore();
        assertEquals(0, eventStore.getTrace().getTrace().size());
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
                RequestEvent re = new RequestEvent(EventType.TRACE_START, "Start"+threadID);
                eventStore.storeEvent(re);
                for (int i = 0; i < 100; i++) {
                    re = new RequestEvent("Event-"+i+"-"+threadID);
                    eventStore.storeEvent(re);
                }
                eventStore.storeEvent(new RequestEvent(EventType.TRACE_END, "End"+threadID));
                tracesbyThreadId.put(threadID, eventStore.getTrace());
            }
        });
        
        Thread thread2 = new Thread(new Runnable() {
            @Override
            public void run() {
                long threadID = Thread.currentThread().getId();
                RequestEvent re = new RequestEvent(EventType.TRACE_START, "Start"+threadID);
                eventStore.storeEvent(re);
                for (int i = 0; i < 200; i++) {
                    re = new RequestEvent("Event-"+i+"-"+threadID);
                    eventStore.storeEvent(re);
                }
                eventStore.storeEvent(new RequestEvent(EventType.TRACE_END, "End"+threadID));
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
        assertEquals(202, trace2.getTrace().size());
        assertEquals(102,trace1.getTrace().size());
        
        // ensure they are different classes
        assertNotSame(trace2, trace1);
        
        // assert last names are coorect
        assertEquals("End"+thread1.getId(), trace1.getTrace().getLast().getEventName());
        assertEquals("End"+thread2.getId(), trace2.getTrace().getLast().getEventName());
        
        //assert start names are correct
        assertEquals("Start"+thread1.getId(), trace1.getTrace().getFirst().getEventName());
        assertEquals("Start"+thread2.getId(), trace2.getTrace().getFirst().getEventName());
        
        // assert all conversation IDs are correct trace 1
        UUID convID = trace1.getTrace().getFirst().getConversationId();
        for (RequestEvent re : trace1.getTrace()) {
            assertEquals(convID,re.getConversationId());
        }
        
        // assert all conversation IDs are correct trace 2
        UUID convID2 = trace2.getTrace().getFirst().getConversationId();
        for (RequestEvent re : trace2.getTrace()) {
            assertEquals(convID2,re.getConversationId());
        }

    }

    /**
     * Test of getElapsedTime method, of class RequestEventStore.
     * @throws java.lang.InterruptedException
     */
    @Test
    public void testGetElapsedTime() throws InterruptedException {
        RequestEvent re = new RequestEvent(EventType.TRACE_START, "Start");
        eventStore.storeEvent(re);
        Thread.sleep(100);
        eventStore.storeEvent(new RequestEvent("Test"));
        eventStore.storeEvent(new RequestEvent(EventType.TRACE_END, "End"));
        assertEquals(3, eventStore.getTrace().getTrace().size());
        assertTrue(eventStore.getElapsedTime() >= 100);
    }
    
    
    @Test
    public void testFlushMultipleThreads() throws InterruptedException {
        
        // thread 1 builds up a trace
        Thread thread1 = new Thread(new Runnable() {
            @Override
            public void run(){
                long threadID = Thread.currentThread().getId();
                RequestEvent re = new RequestEvent(EventType.TRACE_START, "Start"+threadID);
                eventStore.storeEvent(re);
                for (int i = 0; i < 10; i++) {
                    re = new RequestEvent("Event-"+i+"-"+threadID);
                    try {
                        Thread.sleep(10);
                    } catch (InterruptedException ex) {
                        Logger.getLogger(RequestEventStoreTest.class.getName()).log(Level.SEVERE, null, ex);
                    }
                    eventStore.storeEvent(re);
                }
                eventStore.storeEvent(new RequestEvent(EventType.TRACE_END, "End"+threadID));
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
        assertEquals(12, trace1.getTrace().size());
        assertEquals(0, trace2.getTrace().size());
        
        // check trace 1 has an elapsed time in the correct range
        assertTrue(trace1.getElapsedTime() > 100);
        
    }


    
}
