/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package test.clustersetup;

import com.sun.appserv.test.AdminBaseDevTest;
import java.io.File;
import org.testng.annotations.Test;
import org.testng.Assert;

public class ClusterSetupTest extends AdminBaseDevTest {

    @Override
    protected String getTestDescription() {
        return "QL cluster test helloworld";
    }

    final String tn = "QLCluster";
    final String port1 = "18080";
    final String port2 = "28080";
    final String cname = "eec1";
    final String i1name = "eein1-with-a-very-very-very-long-name";
    final String i2name = "eein2";
    
    String i1url = "http://localhost:" + port1;
    String i2url = "http://localhost:" + port2;

    public boolean retStatus = false, ret1 = false, ret2 = false;

    @Test
    public void createClusterTest() throws Exception {
        
        // Try to see if there's an existing cluster config, if so tear it down
        try {
            deleteInstance();
            deleteCluster();
        } catch (Exception e) {
            // Ignore
        }
        
        // Create a cluster and two instances
        retStatus = report(tn + "create-cluster", asadmin("create-cluster", cname));
        Assert.assertEquals(retStatus, true, "Create Cluster failed ...");
    }

    @Test(dependsOnMethods = { "createClusterTest" })
    public void createInstanceTest() throws Exception {
        report(tn + "create-local-instance1",
                asadmin(
                    "create-local-instance", 
                    "--cluster", cname, 
                    "--node", "localhost-domain1", 
                    "--systemproperties", 
                        "HTTP_LISTENER_PORT=18080:HTTP_SSL_LISTENER_PORT=18181:IIOP_SSL_LISTENER_PORT=13800:"
                        + "IIOP_LISTENER_PORT=13700:JMX_SYSTEM_CONNECTOR_PORT=17676:IIOP_SSL_MUTUALAUTH_PORT=13801:"
                        + "JMS_PROVIDER_PORT=18686:ASADMIN_LISTENER_PORT=14848",
                    i1name));
        
        retStatus = report(tn + "create-local-instance2",
                asadmin(
                    "create-local-instance", 
                    "--cluster", cname, 
                    "--node", "localhost-domain1", 
                    "--systemproperties",
                        "HTTP_LISTENER_PORT=28080:HTTP_SSL_LISTENER_PORT=28181:IIOP_SSL_LISTENER_PORT=23800:"
                        + "IIOP_LISTENER_PORT=23700:JMX_SYSTEM_CONNECTOR_PORT=27676:IIOP_SSL_MUTUALAUTH_PORT=23801:"
                        + "JMS_PROVIDER_PORT=28686:ASADMIN_LISTENER_PORT=24848",
                   i2name));
        
        Assert.assertEquals(retStatus, true, "Create instance failed ...");
    }

    @Test(dependsOnMethods = { "createInstanceTest" })
    public void startInstanceTest() throws Exception {
        // Start the instances
        report(tn + "start-local-instance1", asadmin("start-local-instance", "--node", "localhost-domain1", i1name));
        report(tn + "start-local-instance2", asadmin("start-local-instance", "--node", "localhost-domain1", i2name));
        
        System.out.println("Waiting for 5 sec...");
        
        Thread.currentThread().sleep(5000);
        
        // Check that the instances are there
        report(tn + "list-instances", asadmin("list-instances"));
        report(tn + "getindex1", matchString("GlassFish Server", getURL(i1url)));
        retStatus = report(tn + "getindex2", matchString("Payara Server", getURL(i2url)));
        
        Assert.assertEquals(retStatus, true, "Start instance failed ...");
    }
    
    public void deleteInstance() throws Exception {
        AsadminReturn ar1 = asadminWithOutput("stop-local-instance", "--node", "localhost-domain1", "--kill", i1name);
        AsadminReturn ar2 = asadminWithOutput("stop-local-instance", "--node", "localhost-domain1", "--kill", i2name);
        AsadminReturn ar3 = asadminWithOutput("delete-local-instance", "--node", "localhost-domain1", i1name);
        AsadminReturn ar4 = asadminWithOutput("delete-local-instance", "--node", "localhost-domain1", i2name);

        report(tn + "stop-local-instance1", ar1.returnValue);
        report(tn + "stop-local-instance2", ar2.returnValue);
        report(tn + "delete-local-instance1", ar3.returnValue);
        report(tn + "delete-local-instance2", ar4.returnValue);
    }

    public void deleteCluster() throws Exception {
        AsadminReturn ar1 = asadminWithOutput("delete-cluster", cname);
        retStatus = report(tn + "delete-cluster", ar1.returnValue);
    }
}
