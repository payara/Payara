/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.nucleus.requesttracing;

import fish.payara.nucleus.requesttracing.domain.EventType;
import fish.payara.nucleus.requesttracing.domain.RequestEvent;
import java.io.StringReader;
import javax.json.Json;
import javax.json.JsonReader;
import jdk.nashorn.internal.runtime.JSONFunctions;
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
public class RequestTraceTest {
    
    private RequestTrace trace;
    
    public RequestTraceTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }
    
    @Before
    public void setUp() {
        trace = new RequestTrace();
    }
    
    @After
    public void tearDown() {
    }

    /**
     * Test of addEvent method, of class RequestTrace.
     */
    @Test
    public void testAddEventWithoutStarting() {
        trace = new RequestTrace();
        RequestEvent re = new RequestEvent("TestEvent");
        trace.addEvent(re);
        assertFalse(trace.isStarted());
        assertFalse(trace.getTrace().contains(this));
        assertEquals(0,trace.getTrace().size());
    }
    
    @Test
    public void testStarting() {
        assertFalse(trace.isStarted());
        RequestEvent re = new RequestEvent(EventType.TRACE_START,"StartEvent");
        trace.addEvent(re);
        assertTrue(trace.isStarted());
        assertEquals(1,trace.getTrace().size());
        assertTrue(trace.getTrace().contains(re));
    }
    
    @Test
    public void testEnding() throws InterruptedException {
        assertFalse(trace.isStarted());
        RequestEvent re = new RequestEvent(EventType.TRACE_START,"StartEvent");
        trace.addEvent(re);
        assertEquals(0, trace.getElapsedTime());
        Thread.currentThread().sleep(100);
        re = new RequestEvent(EventType.TRACE_END,"EndTrace");
        trace.addEvent(re);
        // add one after trace end
        re = new RequestEvent("TestEvent");
        trace.addEvent(re);
        assertTrue(trace.getElapsedTime() > 0);
        assertEquals("Trace should not add events after end", 2, trace.getTrace().size());
    }

    /**
     * Test of getElapsedTime method, of class RequestTrace.
     */
    @Test
    public void testGetElapsedTimeInitial() {
        trace = new RequestTrace();
        assertEquals(0,trace.getElapsedTime());
    }
    
    @Test
    public void testElapsedTime() throws InterruptedException {
        trace = new RequestTrace();
        trace.addEvent(new RequestEvent(EventType.TRACE_START,"Start"));
        Thread.currentThread().sleep(1000);
        trace.addEvent(new RequestEvent(EventType.TRACE_END,"Finish"));
        assertTrue((900 < trace.getElapsedTime() && trace.getElapsedTime() < 1100));
    }
    
    @Test
    public void testFullStream() {
        RequestEvent re = new RequestEvent(EventType.TRACE_START,"Start");
        trace.addEvent(re);
        for (int i = 0; i < 10000; i++) {
            re = new RequestEvent("Event"+i);
            trace.addEvent(re);
        }
        re = new RequestEvent(EventType.TRACE_END,"TraceEnd");
        trace.addEvent(re);
        assertTrue(trace.isStarted());
        assertEquals(10002, trace.getTrace().size());
        assertTrue(trace.getElapsedTime() > 0);
    }
    
    @Test
    public void testConversationIDPropagation() {
        RequestEvent start = new RequestEvent(EventType.TRACE_START,"Start");
        trace.addEvent(start);
        RequestEvent re = new RequestEvent("Event");
        trace.addEvent(re);
        assertEquals(start.getConversationId(), re.getConversationId());
        re = new RequestEvent(EventType.TRACE_END,"End");
        trace.addEvent(re);
        assertEquals(start.getConversationId(), re.getConversationId());        
    }

    @Test
    public void testJSONParse() {
        RequestEvent re = new RequestEvent(EventType.TRACE_START,"Start");
        trace.addEvent(re);
        for (int i = 0; i < 10000; i++) {
            re = new RequestEvent("Event"+i);
            trace.addEvent(re);
        }
        re = new RequestEvent(EventType.TRACE_END,"TraceEnd");
        trace.addEvent(re);
        String jsonString = trace.toString();
        JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
        jsonReader.readObject();
        
    }
    
    @Test
    public void testMultipleStartEvents() {
        RequestEvent re = new RequestEvent(EventType.TRACE_START,"Start");
        trace.addEvent(re);
        for (int i = 0; i < 100; i++) {
            re = new RequestEvent("Event"+i);
            trace.addEvent(re);
        }
        re = new RequestEvent(EventType.TRACE_END,"TraceEnd");
        trace.addEvent(re);
        assertEquals(102,trace.getTrace().size());
        re = new RequestEvent(EventType.TRACE_START,"Start2");
        trace.addEvent(re);
        assertEquals(1,trace.getTrace().size());
        assertEquals("Start2", trace.getTrace().getFirst().getEventName());
    }
    
}
