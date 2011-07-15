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
import java.util.HashMap;

/**
 *
 */
public class ApplicationReferenceTest {

    public ApplicationReferenceTest() {
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
    public void testApplicationReferenceTest() {
        System.out.println("applicationReferenceTest");
        DeploymentFacility df = DeploymentFacilityFactory.getDeploymentFacility();
        ServerConnectionIdentifier sci = new ServerConnectionIdentifier();
        sci.setHostName("localhost");
        sci.setHostPort(4848); // 8080 for the REST client
        sci.setUserName("admin");
        sci.setPassword("");

        df.connect(sci);

        try {

            TargetModuleID[] results =
                    df.listAppRefs(new String[] {"cluster1"});
            System.out.println("TargetModuleIDs returned for cluster:");
            for (TargetModuleID tmid : results) {
                System.out.println(tmid.getTarget().getName() + ":" +
                        tmid.getModuleID());
            }

            TargetModuleID[] results1 =
                    df.listAppRefs(new String[] {"server"});
            System.out.println("TargetModuleIDs returned for server:");
            for (TargetModuleID tmid1 : results1) {
                System.out.println(tmid1.getTarget().getName() + ":" +
                        tmid1.getModuleID());
            }

            TargetModuleID[] results11 =
                    df.listAppRefs(new String[0]);
            System.out.println("TargetModuleIDs returned for server:");
            for (TargetModuleID tmid11 : results11) {
                System.out.println(tmid11.getTarget().getName() + ":" +
                        tmid11.getModuleID());
            }


            TargetModuleID[] results2 =
                    df.listAppRefs(new String[] {"cluster1", "server"});
            System.out.println("TargetModuleIDs returned for all:");
            for (TargetModuleID tmid2 : results2) {
                System.out.println(tmid2.getTarget().getName() + ":" +
                        tmid2.getModuleID());
            }

            Target[] targets = df.listTargets();
            Target[] clusterTarget = new Target[1];
            Target[] dasTarget = new Target[1];
            for (Target target : targets) {
                if (target.getName().equals("server")) {
                    dasTarget[0] = target; 
                } else if (target.getName().equals("cluster1")) {
                    clusterTarget[0] = target; 
                }
            }

            df.createAppRef(dasTarget, "webapps-simple", new HashMap());

            TargetModuleID[] results12 =
                    df.listAppRefs(new String[] {"server"});
            System.out.println("TargetModuleIDs returned for server2:");
            for (TargetModuleID tmid12 : results12) {
                System.out.println(tmid12.getTarget().getName() + ":" +
                        tmid12.getModuleID());
            }

            df.deleteAppRef(dasTarget, "webapps-simple", new HashMap());

            TargetModuleID[] results13 =
                    df.listAppRefs(new String[] {"server"});
            System.out.println("TargetModuleIDs returned for server3:");
            for (TargetModuleID tmid13 : results13) {
                System.out.println(tmid13.getTarget().getName() + ":" +
                        tmid13.getModuleID());
            }

            df.createAppRef(clusterTarget, "stateless-simple", new HashMap());

            TargetModuleID[] results22 =
                    df.listAppRefs(new String[] {"cluster1"});
            System.out.println("TargetModuleIDs returned for cluster2:");
            for (TargetModuleID tmid22 : results22) {
                System.out.println(tmid22.getTarget().getName() + ":" +
                        tmid22.getModuleID());
            }

            df.deleteAppRef(clusterTarget, "stateless-simple", new HashMap());

            TargetModuleID[] results23 =
                    df.listAppRefs(new String[] {"cluster1"});
            System.out.println("TargetModuleIDs returned for cluster3:");
            for (TargetModuleID tmid23 : results23) {
                System.out.println(tmid23.getTarget().getName() + ":" +
                        tmid23.getModuleID());
            }

            df.createAppRef(targets, "servletonly2", new HashMap());

            TargetModuleID[] results14 =
                    df.listAppRefs(new String[] {"server"});
            System.out.println("TargetModuleIDs returned for server4:");
            for (TargetModuleID tmid14 : results14) {
                System.out.println(tmid14.getTarget().getName() + ":" +
                        tmid14.getModuleID());
            }

            TargetModuleID[] results24 =
                    df.listAppRefs(new String[] {"cluster1"});
            System.out.println("TargetModuleIDs returned for cluster4:");
            for (TargetModuleID tmid24 : results24) {
                System.out.println(tmid24.getTarget().getName() + ":" +
                        tmid24.getModuleID());
            }

            df.deleteAppRef(targets, "servletonly2", new HashMap());

            TargetModuleID[] results15 =
                    df.listAppRefs(new String[] {"server"});
            System.out.println("TargetModuleIDs returned for server5:");
            for (TargetModuleID tmid15 : results15) {
                System.out.println(tmid15.getTarget().getName() + ":" +
                        tmid15.getModuleID());
            }

            TargetModuleID[] results25 =
                    df.listAppRefs(new String[] {"cluster1"});
            System.out.println("TargetModuleIDs returned for cluster5:");
            for (TargetModuleID tmid25 : results25) {
                System.out.println(tmid25.getTarget().getName() + ":" +
                        tmid25.getModuleID());
            }

        } catch (Exception e) {
            fail("Failed due to exception " + e.getMessage());
        }

    }

}
