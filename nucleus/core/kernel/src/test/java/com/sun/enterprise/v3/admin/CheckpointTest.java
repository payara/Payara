/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
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
package com.sun.enterprise.v3.admin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URI;
import java.util.Iterator;
import java.util.Properties;

import org.glassfish.admin.payload.PayloadImpl;
import org.glassfish.admin.payload.PayloadImpl.Outbound;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.AdminCommandContextImpl;
import org.glassfish.api.admin.Payload;
import org.glassfish.api.admin.Payload.Inbound;
import org.glassfish.api.admin.Payload.Part;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.sun.enterprise.util.io.FileUtils;
import com.sun.enterprise.v3.common.PlainTextActionReporter;

/**
 * 
 * @author Andriy Zhdanov
 *
 */
public class CheckpointTest {
    CheckpointHelper checkpoint = new CheckpointHelper();
    File topDir;

    @Before
    public void setup() throws IOException {
        topDir = checkpoint.createTempDir("checkpoint", "");
    }

    @After
    public void tearDown() {
        FileUtils.whack(topDir);
    }

    @Test
    public void testSerialization() throws IOException, ClassNotFoundException {
        ActionReport report = new PlainTextActionReporter();

        Outbound outbound = Outbound.newInstance();
        Properties outboundProps = new Properties();
        outboundProps.setProperty("key", "value");
        outbound.addPart("text/plain", "test3", outboundProps, "Hello world!"); // text/plain with one part is saved as text

        ByteArrayInputStream inputStream = new ByteArrayInputStream("Hello world!".getBytes());
        Inbound inbound = PayloadImpl.Inbound.newInstance("text/plain", inputStream);
        Properties inboundProps = new Properties();
        inboundProps.setProperty("key", "value");

        AdminCommandContext context = new AdminCommandContextImpl(null /* logger */, report, inbound, outbound, null /* event broker */, "test_job_id");
        report.setFailureCause(new RuntimeException("Test"));
        File contextFile = File.createTempFile("context", "", topDir);
        checkpoint.saveAdminCommandContext(context, contextFile);
        
        Outbound restoredOutbound = Outbound.newInstance();
        AdminCommandContext restored = checkpoint.loadAdminCommandContext(contextFile, restoredOutbound);
        assertEquals("failureCause", "Test", restored.getActionReport().getFailureCause().getMessage());

        Payload.Outbound outboundTest = restored.getOutboundPayload();
        assertNotNull("outbound", outboundTest);
        
        Iterator<Part> parts = outboundTest.parts();
        assertTrue("has parts", parts.hasNext());
        Part part = parts.next();
        assertEquals("Content-type", "text/plain", part.getContentType());
        BufferedReader inputStreamReader = new BufferedReader(new InputStreamReader(part.getInputStream()));
        assertEquals("Content", "Hello world!", inputStreamReader.readLine());

        assertFalse("outbound has no more parts", parts.hasNext());

        Inbound inboundTest = restored.getInboundPayload();
        parts = inboundTest.parts();
        assertTrue("has parts", parts.hasNext());
        part = parts.next();
        assertEquals("Content-type", "text/plain", part.getContentType());
        inputStreamReader = new BufferedReader(new InputStreamReader(part.getInputStream()));
        assertEquals("Content", "Hello world!", inputStreamReader.readLine());

        assertFalse("inbound has no more parts", parts.hasNext());
    }

    @Test
    public void testSimpleOutbound() throws Exception {
        Outbound outbound = Outbound.newInstance();
        Properties props = new Properties();
        props.setProperty("key", "value");
        outbound.addPart("text/plain", "test3", props, "Hello world!"); // text/plain with one part is saved as text
        File outboundFile = File.createTempFile("outbound", "", topDir);
        checkpoint.saveOutbound(outbound, outboundFile);        

        Outbound outboundTest = Outbound.newInstance();
        checkpoint.loadOutbound(outboundTest, outboundFile);
        Iterator<Part> parts = outboundTest.parts();
        assertTrue("has parts", parts.hasNext());
        Part part = parts.next();
        assertEquals("Content-type", "text/plain", part.getContentType());
        BufferedReader inputStreamReader = new BufferedReader(new InputStreamReader(part.getInputStream()));
        assertEquals("Content", "Hello world!", inputStreamReader.readLine());

        assertFalse("has no more parts", parts.hasNext());
    }

    @Test
    public void testComplexOutbound() throws Exception {
        Outbound outbound = Outbound.newInstance();
        Properties props = new Properties();
        props.setProperty("key", "value");
        File sourceFile = File.createTempFile("source", "", topDir);
        populateFile(sourceFile, "Hello from file!");
        outbound.attachFile("application/octet-stream", new URI("test1"), "addText", sourceFile);
        outbound.addPart("application/xml", "test2", props, "Hello xml!");
        outbound.addPart("text/plain", "test3", props, "Hello world!"); // text/plain with one part is saved as text
        File outboundFile = File.createTempFile("outbound", "", topDir);
        checkpoint.saveOutbound(outbound, outboundFile);

        
        Outbound outboundTest = Outbound.newInstance();
        checkpoint.loadOutbound(outboundTest, outboundFile);
        Iterator<Part> parts = outboundTest.parts();
        assertTrue("has parts", parts.hasNext());
        Part part = parts.next();
        assertEquals("Content-type", "application/octet-stream", part.getContentType());
        BufferedReader inputStreamReader = new BufferedReader(new InputStreamReader(part.getInputStream()));
        assertEquals("Content", "Hello from file!", inputStreamReader.readLine());

        part = parts.next();
        assertEquals("Content-type", "application/xml", part.getContentType());
        inputStreamReader = new BufferedReader(new InputStreamReader(part.getInputStream()));
        assertEquals("Content", "Hello xml!", inputStreamReader.readLine());

        part = parts.next();
        assertEquals("Content-type", "text/plain", part.getContentType());
        inputStreamReader = new BufferedReader(new InputStreamReader(part.getInputStream()));
        assertEquals("Content", "Hello world!", inputStreamReader.readLine());

        assertFalse("has no more parts", parts.hasNext());

    }

    @Test
    public void testSimpleInbound() throws Exception {
        ByteArrayInputStream inputStream = new ByteArrayInputStream("Hello world!".getBytes());
        Inbound inbound = PayloadImpl.Inbound.newInstance("text/plain", inputStream);
        File inboundFile = File.createTempFile("inbound", "", topDir);
        checkpoint.saveInbound(inbound, inboundFile);

        Inbound inboundTest = checkpoint.loadInbound(inboundFile);
        Iterator<Part> parts = inboundTest.parts();
        assertTrue("has parts", parts.hasNext());
        Part part = parts.next();
        assertEquals("Content-type", "text/plain", part.getContentType());
        BufferedReader inputStreamReader = new BufferedReader(new InputStreamReader(part.getInputStream()));
        assertEquals("Content", "Hello world!", inputStreamReader.readLine());

        assertFalse("has no more parts", parts.hasNext());
    }

    @Test
    public void testComplexInbound() throws Exception {
        Outbound outbound = Outbound.newInstance();
        Properties props = new Properties();
        props.setProperty("key", "value");
        File sourceFile = File.createTempFile("source", "", topDir);
        populateFile(sourceFile, "Hello from file!");
        outbound.attachFile("application/octet-stream", new URI("test1"), "addText", sourceFile);
        outbound.addPart("application/xml", "test2", props, "Hello xml!");
        outbound.addPart("text/plain", "test3", props, "Hello world!"); // text/plain with one part is saved as text
        File outboundFile = File.createTempFile("outbound", "", topDir);
        checkpoint.saveOutbound(outbound, outboundFile);

        Inbound inbound = PayloadImpl.Inbound.newInstance("application/zip", new FileInputStream(outboundFile));
        File inboundFile = File.createTempFile("inbound", "", topDir);
        checkpoint.saveInbound(inbound, inboundFile);

        Inbound inboundTest = checkpoint.loadInbound(inboundFile);
        Iterator<Part> parts = inboundTest.parts();
        assertTrue("has parts", parts.hasNext());
        Part part = parts.next();
        assertEquals("Content-type", "application/octet-stream", part.getContentType());
        BufferedReader inputStreamReader = new BufferedReader(new InputStreamReader(part.getInputStream()));
        assertEquals("Content", "Hello from file!", inputStreamReader.readLine());

        assertTrue("has parts", parts.hasNext()); // prefetchNextEntry
        part = parts.next();
        assertEquals("Content-type", "application/xml", part.getContentType());
        inputStreamReader = new BufferedReader(new InputStreamReader(part.getInputStream()));
        assertEquals("Content", "Hello xml!", inputStreamReader.readLine());

        part = parts.next();
        assertEquals("Content-type", "text/plain", part.getContentType());
        inputStreamReader = new BufferedReader(new InputStreamReader(part.getInputStream()));
        assertEquals("Content", "Hello world!", inputStreamReader.readLine());

        assertFalse("has no more parts", parts.hasNext());
    }

    private File populateFile(final File f, final String content) throws FileNotFoundException {
        final PrintStream ps = new PrintStream(f);
        ps.println(content);
        ps.close();
        return f;
    }
}
