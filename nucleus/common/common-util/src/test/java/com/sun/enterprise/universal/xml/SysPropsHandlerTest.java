/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.universal.xml;

import com.sun.enterprise.universal.xml.SysPropsHandler.Type;
import java.util.Map;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Byron Nevins
 */
public class SysPropsHandlerTest {

    public SysPropsHandlerTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
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

    /**
     * Test of getCombinedSysProps method, of class SysPropsHandler.
     */
    @Test
    public void exercise() {
        System.out.println("exercise SysPropsHndler");
        SysPropsHandler instance = new SysPropsHandler();
        instance.add(Type.SERVER, "test", "from-server");
        instance.add(Type.CLUSTER, "test", "from-cluster");
        instance.add(Type.CONFIG, "test", "from-config");
        instance.add(Type.DOMAIN, "test", "from-domain");
        Map<String, String> map = instance.getCombinedSysProps();
        assertTrue(map.size() == 1);
        assertTrue(map.get("test").equals("from-server"));

        instance.add(Type.CLUSTER, "test2", "from-cluster");
        instance.add(Type.CONFIG, "test2", "from-config");
        instance.add(Type.DOMAIN, "test2", "from-domain");

        instance.add(Type.CONFIG, "test3", "from-config");
        instance.add(Type.DOMAIN, "test3", "from-domain");

        instance.add(Type.DOMAIN, "test4", "from-domain");

        map = instance.getCombinedSysProps();

        assertTrue(map.size() == 4);
        assertTrue(map.get("test").equals("from-server"));
        assertTrue(map.get("test2").equals("from-cluster"));
        assertTrue(map.get("test3").equals("from-config"));
        assertTrue(map.get("test4").equals("from-domain"));
    }
}
