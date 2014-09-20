/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.config.support;

import java.io.*;
import java.net.*;
import java.util.*;
import javax.xml.stream.XMLInputFactory;
import org.glassfish.config.support.DomainXmlPreParser.DomainXmlPreParserException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author bnevins
 */
public class DomainXmlPreParserTest {

    public DomainXmlPreParserTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        stock = loadURL("parser/stock.xml");
        i1 = loadURL("parser/i1.xml");
        i1i2 = loadURL("parser/i1i2.xml");
        c1i1 = loadURL("parser/c1i1.xml");
        c1i1c1i2 = loadURL("parser/c1i1c1i2.xml");
        noconfigfori1 = loadURL("parser/noconfigfori1.xml");
        System.setProperty("AS_DEBUG", "true");
    }

    private static URL loadURL(String name) {
        URL url = classLoader.getResource(name);
        assertNotNull(url);
        return url;
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    @Test(expected=DomainXmlPreParser.DomainXmlPreParserException.class)
    public void stockDomainHasNoInstance() throws DomainXmlPreParserException {
        System.out.println("stockDomainHasNoInstance");
        DomainXmlPreParser pp = new DomainXmlPreParser(stock, xif, "i1");
    }

    @Test
    public void domainWithi1() throws DomainXmlPreParserException {
        System.out.println("domainWithi1");
        DomainXmlPreParser pp = new DomainXmlPreParser(i1, xif, "i1");
        List<String> servers = pp.getServerNames();
        String clusterName = pp.getClusterName();
        String configName = pp.getConfigName();

        assertTrue(servers.size() == 1);
        assertTrue(servers.get(0).equals("i1"));
        assertNull(clusterName);
        assertEquals(configName, "i1-config");
    }

    @Test
    public void domainWithi1i2_i1() throws DomainXmlPreParserException {
        System.out.println("domainWithi1i2_i1");
        DomainXmlPreParser pp = new DomainXmlPreParser(i1i2, xif, "i1");
        List<String> servers = pp.getServerNames();
        String clusterName = pp.getClusterName();
        String configName = pp.getConfigName();

        assertTrue(servers.size() == 1);
        assertTrue(servers.contains("i1"));
        assertFalse(servers.contains("i2"));
        assertNull(clusterName);
        assertEquals(configName, "i1-config");
    }

    @Test
    public void domainWithi1i2_i2() throws DomainXmlPreParserException {
        System.out.println("domainWithi1i2_i2");
        DomainXmlPreParser pp = new DomainXmlPreParser(i1i2, xif, "i2");
        List<String> servers = pp.getServerNames();
        String clusterName = pp.getClusterName();
        String configName = pp.getConfigName();

        assertTrue(servers.size() == 1);
        assertTrue(servers.contains("i2"));
        assertFalse(servers.contains("i1"));
        assertNull(clusterName);
        assertEquals(configName, "i2-config");
    }

    @Test
    public void oneClusteredInstance() throws DomainXmlPreParserException {
        System.out.println("oneClusteredInstance");
        DomainXmlPreParser pp = new DomainXmlPreParser(c1i1, xif, "c1i1");
        List<String> servers = pp.getServerNames();
        String clusterName = pp.getClusterName();
        String configName = pp.getConfigName();

        assertTrue(servers.size() == 1);
        assertTrue(servers.contains("c1i1"));
        assertEquals(clusterName, "c1");
        assertEquals(configName, "c1-config");
    }

    @Test
    public void twoClusteredInstances() throws DomainXmlPreParserException {
        System.out.println("twoClusteredInstances");
        DomainXmlPreParser pp = new DomainXmlPreParser(c1i1c1i2, xif, "c1i1");
        List<String> servers = pp.getServerNames();
        String clusterName = pp.getClusterName();
        String configName = pp.getConfigName();

        assertTrue(servers.size() == 2);
        assertTrue(servers.contains("c1i1"));
        assertTrue(servers.contains("c1i2"));
        assertEquals(clusterName, "c1");
        assertEquals(configName, "c1-config");
    }

    @Test
    public void noConfigTest() {
        System.out.println("noConfigTest");
        try {
            DomainXmlPreParser pp = new DomainXmlPreParser(noconfigfori1, xif, "i1");
            fail("Expected an exception!!!");
        }
        catch(DomainXmlPreParserException e) {
            assertTrue(e.getMessage().startsWith("The config element, "));
            System.out.println(e);
        }
    }

    private static URL stock, i1, i1i2, c1i1, c1i1c1i2, noconfigfori1;
    private static ClassLoader classLoader = DomainXmlPreParserTest.class.getClassLoader();
    private static XMLInputFactory xif = XMLInputFactory.newInstance();
}
