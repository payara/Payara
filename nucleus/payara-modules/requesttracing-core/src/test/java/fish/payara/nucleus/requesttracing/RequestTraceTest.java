/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016 Payara Foundation and/or its affiliates. All rights reserved.
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

import fish.payara.nucleus.requesttracing.domain.EventType;
import fish.payara.nucleus.requesttracing.domain.RequestEvent;
import java.io.StringReader;
import javax.json.Json;
import javax.json.JsonReader;
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
    
    @Before
    public void setUp() {
        trace = new RequestTrace();
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
        assertTrue((900 < trace.getElapsedTime() && trace.getElapsedTime() < 1500));
    }
    
    @Test
    public void testFullStream() throws InterruptedException {
        RequestEvent re = new RequestEvent(EventType.TRACE_START,"Start");
        trace.addEvent(re);
        for (int i = 0; i < 10000; i++) {
            re = new RequestEvent("Event"+i);
            trace.addEvent(re);
        }
        Thread.currentThread().sleep(1000);
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
