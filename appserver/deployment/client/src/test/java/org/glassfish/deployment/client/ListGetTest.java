/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.deployment.client;

import javax.enterprise.deploy.shared.ModuleType;
import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.spi.TargetModuleID;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;
import java.util.List;

/**
 */
public class ListGetTest {

    public ListGetTest() {
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

    @Ignore
    @Test
    public void testListGetTest() {
        System.out.println("testListGetTest");
        DeploymentFacility df = DeploymentFacilityFactory.getDeploymentFacility();
        ServerConnectionIdentifier sci = new ServerConnectionIdentifier();
        sci.setHostName("localhost");
        sci.setHostPort(4848); // 8080 for the REST client
        sci.setUserName("admin");
        sci.setPassword("");

        df.connect(sci);

        try {
            Target[] results = df.listTargets();
            System.out.println("Targets returned:");
            for (Target tid : results) {
                System.out.println(tid.getName());
            }

            String contextRoot = df.getContextRoot("webapps-simple");
            System.out.println("Context root for webapps-simple:" + contextRoot);

            ModuleType type = df.getModuleType("webapps-simple");
            System.out.println("Module type for webapps-simple:" + type);

            ModuleType type2 = df.getModuleType("i18n-simple");
            System.out.println("Module type for i18n-simple:" + type2);

            ModuleType type3 = df.getModuleType("singleton");
            System.out.println("Module type for singleton:" + type3);

            ModuleType type4 = df.getModuleType("generic-ra");
            System.out.println("Module type for generic-ra:" + type4);

            ModuleType type5 = df.getModuleType("fooClient");
            System.out.println("Module type for fooClient:" + type5);

            ModuleType type6 = df.getModuleType("barEjb");
            System.out.println("Module type for barEjb:" + type6);

            List<String> subModuleResults = df.getSubModuleInfoForJ2EEApplication("singleton2");
            System.out.println("Sub modules returned:");
            for (String subModule : subModuleResults) {
                System.out.println(subModule);
            }

        } catch (Exception e) {
            fail("Failed due to exception " + e.getMessage());
        }

    }

}
