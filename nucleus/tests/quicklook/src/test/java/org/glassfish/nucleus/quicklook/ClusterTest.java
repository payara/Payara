/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.nucleus.quicklook;

import static org.glassfish.tests.utils.NucleusTestUtils.*;
import static org.testng.AssertJUnit.assertTrue;
import org.testng.annotations.Test;

/**
 *
 * @author Tom Mueller
 */
@Test
public class ClusterTest {
    
    final String tn = "QLCluster";
    final String port1 = "18080";
    final String port2 = "28080";
    final String cname = "eec1";
    final String i1name = "eein1-with-a-very-very-very-long-name";
    final String i2name = "eein2";
    String i1url = "http://localhost:" + port1;
    String i2url = "http://localhost:" + port2;

    public void createClusterTest() {
        // create a cluster and two instances
        assertTrue("create cluster", nadmin("create-cluster", cname));
    }
    
    @Test(dependsOnMethods = { "createClusterTest" })
    public void createInstancesTest() {           
        assertTrue("create instance1", nadmin("create-local-instance",
                "--cluster", cname, "--systemproperties",
                "HTTP_LISTENER_PORT=18080:HTTP_SSL_LISTENER_PORT=18181:IIOP_SSL_LISTENER_PORT=13800:" +
                "IIOP_LISTENER_PORT=13700:JMX_SYSTEM_CONNECTOR_PORT=17676:IIOP_SSL_MUTUALAUTH_PORT=13801:" +
                "JMS_PROVIDER_PORT=18686:ASADMIN_LISTENER_PORT=14848", i1name));
        assertTrue("create instance2", nadmin("create-local-instance",
                "--cluster", cname, "--systemproperties",
                "HTTP_LISTENER_PORT=28080:HTTP_SSL_LISTENER_PORT=28181:IIOP_SSL_LISTENER_PORT=23800:" +
                "IIOP_LISTENER_PORT=23700:JMX_SYSTEM_CONNECTOR_PORT=27676:IIOP_SSL_MUTUALAUTH_PORT=23801:" +
                "JMS_PROVIDER_PORT=28686:ASADMIN_LISTENER_PORT=24848", i2name));
    }
    
    @Test(dependsOnMethods = { "createInstancesTest" })
    public void startInstancesTest() {           
        // start the instances
        assertTrue("start instance1", nadmin("start-local-instance", i1name));
        assertTrue("start instance2", nadmin("start-local-instance", i2name));
    }
    
    @Test(dependsOnMethods = { "startInstancesTest" })
    public void checkClusterTest() {           
        // check that the instances are there
        assertTrue("list-instances", nadmin("list-instances"));
        assertTrue("getindex1", matchString("GlassFish Server", getURL(i1url)));
        assertTrue("getindex2", matchString("GlassFish Server", getURL(i2url)));
    }
    
    @Test(dependsOnMethods = { "checkClusterTest" })
    public void stopInstancesTest() {           
        // stop and delete the instances and cluster
        assertTrue("stop instance1", nadmin("stop-local-instance", "--kill", i1name));
        assertTrue("stop instance2", nadmin("stop-local-instance", "--kill", i2name));
    }
    
    @Test(dependsOnMethods = { "stopInstancesTest" })
    public void deleteInstancesTest() {
        assertTrue("delete instance1", nadmin("delete-local-instance", i1name));
        assertTrue("delete instance2", nadmin("delete-local-instance", i2name));
    }

    @Test(dependsOnMethods = { "deleteInstancesTest" })
    public void deleteClusterTest() {           
        assertTrue("delete cluster", nadmin("delete-cluster", cname));
    }
}
