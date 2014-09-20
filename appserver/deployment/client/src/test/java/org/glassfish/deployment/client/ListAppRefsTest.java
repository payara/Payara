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

import javax.enterprise.deploy.spi.Target;
import javax.enterprise.deploy.spi.TargetModuleID;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Tim
 */
public class ListAppRefsTest {

    public ListAppRefsTest() {
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
    public void testListAppRefsTest() {
        System.out.println("testListAppRefsTest");
        DeploymentFacility df = DeploymentFacilityFactory.getDeploymentFacility();
        ServerConnectionIdentifier sci = new ServerConnectionIdentifier();
        sci.setHostName("localhost");
        sci.setHostPort(4848); // 8080 for the REST client
        sci.setUserName("admin");
        sci.setPassword("");

        df.connect(sci);

        try {

            TargetModuleID[] results1 =
                    df._listAppRefs(new String[] {"cluster1"});
            System.out.println("TargetModuleIDs returned for default:");
            for (TargetModuleID tmid1 : results1) {
                System.out.println(tmid1.getTarget().getName() + ":" +
                        tmid1.getModuleID());
            }

            TargetModuleID[] resultsAll1 =
                    df._listAppRefs(new String[] {"cluster1"}, "all");
            System.out.println("TargetModuleIDs returned for all cluster1:");
            for (TargetModuleID tmidAll1 : resultsAll1) {
                System.out.println(tmidAll1.getTarget().getName() + ":" +
                        tmidAll1.getModuleID());
            }

            TargetModuleID[] resultsRunning1 =
                    df._listAppRefs(new String[] {"cluster1"}, "running");
            System.out.println("TargetModuleIDs returned for running cluster1:");
            for (TargetModuleID tmidRunning1 : resultsRunning1) {
               System.out.println(tmidRunning1.getTarget().getName() + ":" +
                        tmidRunning1.getModuleID());
            }

            TargetModuleID[] resultsNonRunning1 =
                    df._listAppRefs(new String[] {"cluster1"}, "non-running");
            System.out.println("TargetModuleIDs returned for nonrunning cluster1:");
            for (TargetModuleID tmidNonRunning1 : resultsNonRunning1) {
                System.out.println(tmidNonRunning1.getTarget().getName() + ":" +
                        tmidNonRunning1.getModuleID());
            }

            TargetModuleID[] results =
                    df._listAppRefs(new String[] {"server"});
            System.out.println("TargetModuleIDs returned for default:");
            for (TargetModuleID tmid : results) {
                System.out.println(tmid.getTarget().getName() + ":" +
                        tmid.getModuleID());
            }

            TargetModuleID[] resultsAll =
                    df._listAppRefs(new String[] {"server"}, "all");
            System.out.println("TargetModuleIDs returned for all:");
            for (TargetModuleID tmidAll : resultsAll) {
                System.out.println(tmidAll.getTarget().getName() + ":" +
                        tmidAll.getModuleID());
            }

            TargetModuleID[] resultsRunning =
                    df._listAppRefs(new String[] {"server"}, "running");
            System.out.println("TargetModuleIDs returned for running:");
            for (TargetModuleID tmidRunning : resultsRunning) {
                System.out.println(tmidRunning.getTarget().getName() + ":" +
                        tmidRunning.getModuleID());
            }

            TargetModuleID[] resultsNonRunning =
                    df._listAppRefs(new String[] {"server"}, "non-running");
            System.out.println("TargetModuleIDs returned for nonrunning:");
            for (TargetModuleID tmidNonRunning : resultsNonRunning) {
                System.out.println(tmidNonRunning.getTarget().getName() + ":" +
                        tmidNonRunning.getModuleID());
            }

            TargetModuleID[] resultsAllWithType =
                    df._listAppRefs(new String[] {"server"}, "all", "web");
            System.out.println("TargetModuleIDs returned for all web:");
            for (TargetModuleID tmidAllWithType : resultsAllWithType) {
                System.out.println(tmidAllWithType.getTarget().getName() + ":" +
                        tmidAllWithType.getModuleID());
            }

            TargetModuleID[] resultsRunningWithType =
                    df._listAppRefs(new String[] {"server"}, "running", "ear");
            System.out.println("TargetModuleIDs returned for running ear:");
            for (TargetModuleID tmidRunningWithType : resultsRunningWithType) {
                System.out.println(tmidRunningWithType.getTarget().getName() + ":" +
                        tmidRunningWithType.getModuleID());
            }

            TargetModuleID[] resultsNonRunningWithType =
                    df._listAppRefs(new String[] {"server"}, "non-running", "ear");
            System.out.println("TargetModuleIDs returned for nonrunning ear:");
            for (TargetModuleID tmidNonRunningWithType : resultsNonRunningWithType) {
                System.out.println(tmidNonRunningWithType.getTarget().getName() + ":" +
                        tmidNonRunningWithType.getModuleID());
            }



        } catch (Exception e) {
            fail("Failed due to exception " + e.getMessage());
        }

    }

}
