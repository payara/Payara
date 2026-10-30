/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016-2021 Payara Foundation and/or its affiliates. All rights reserved.
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
import static org.junit.Assert.assertTrue;

import java.io.StringReader;

import jakarta.json.Json;
import jakarta.json.JsonReader;

import fish.payara.notification.requesttracing.RequestTrace;
import org.junit.Before;
import org.junit.Test;

import fish.payara.notification.requesttracing.EventType;
import fish.payara.notification.requesttracing.RequestTraceSpan;

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
        RequestTraceSpan re = new RequestTraceSpan("TestEvent");
        trace.addEvent(re);
        assertFalse(trace.isStarted());
        assertFalse(trace.getTraceSpans().contains(this));
        assertEquals(0,trace.getTraceSpans().size());
    }
    
    @Test
    public void testStarting() {
        assertFalse(trace.isStarted());
        RequestTraceSpan re = new RequestTraceSpan(EventType.TRACE_START,"StartEvent");
        trace.addEvent(re);
        assertTrue(trace.isStarted());
        assertEquals(1,trace.getTraceSpans().size());
        assertTrue(trace.getTraceSpans().contains(re));
    }
    
    @Test
    public void testEnding() throws InterruptedException {
        assertFalse(trace.isStarted());
        RequestTraceSpan re = new RequestTraceSpan(EventType.TRACE_START,"StartEvent");
        trace.addEvent(re);
        assertEquals(0, trace.getElapsedTime());
        Thread.sleep(10);
        trace.endTrace();
        // add one after trace end
        re = new RequestTraceSpan("TestEvent");
        trace.addEvent(re);
        assertTrue(trace.getElapsedTime() > 0);
        assertEquals("Trace should not add events after end.", 1, trace.getTraceSpans().size());
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
    public void testFullStream() throws InterruptedException {
        RequestTraceSpan re = new RequestTraceSpan(EventType.TRACE_START,"Start");
        trace.addEvent(re);
        for (int i = 0; i < 10000; i++) {
            re = new RequestTraceSpan("Event"+i);
            trace.addEvent(re);
        }
        Thread.sleep(10);
        trace.endTrace();
        assertTrue(trace.isStarted());
        assertEquals(10001, trace.getTraceSpans().size());
        assertTrue(trace.getElapsedTime() > 0);
    }
    
    @Test
    public void testConversationIDPropagation() {
        RequestTraceSpan start = new RequestTraceSpan(EventType.TRACE_START,"Start");
        trace.addEvent(start);
        RequestTraceSpan re = new RequestTraceSpan("Event");
        trace.addEvent(re);
        assertEquals(start.getTraceId(), re.getTraceId());
        trace.endTrace();
        assertEquals(start.getTraceId(), re.getTraceId());        
    }

    @Test
    public void testJSONParse() {
        RequestTraceSpan re = new RequestTraceSpan(EventType.TRACE_START,"Start");
        trace.addEvent(re);
        for (int i = 0; i < 1000; i++) {
            re = new RequestTraceSpan("Event"+i);
            trace.addEvent(re);
        }
        trace.endTrace();
        String jsonString = trace.toString();
        JsonReader jsonReader = Json.createReader(new StringReader(jsonString));
        jsonReader.readObject();
    }
    
    @Test
    public void testMultipleStartEvents() {
        RequestTraceSpan re = new RequestTraceSpan(EventType.TRACE_START,"Start");
        trace.addEvent(re);
        re = new RequestTraceSpan("Event1");
        trace.addEvent(re);
        trace.endTrace();
        assertEquals(2,trace.getTraceSpans().size());
        re = new RequestTraceSpan(EventType.TRACE_START,"Start2");
        trace.addEvent(re);
        assertEquals(1,trace.getTraceSpans().size());
        assertEquals("Start2", trace.getTraceSpans().getFirst().getEventName());
    }
    
}
