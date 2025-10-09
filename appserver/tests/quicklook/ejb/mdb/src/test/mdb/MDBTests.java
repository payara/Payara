/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2013 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2019-2022] Payara Foundation and/or affiliates

package test.mdb;

import java.io.File;
import org.testng.annotations.Test;
import org.testng.Assert;
import org.testng.annotations.Parameters;
import test.admincli.util.*;

@Test(sequential = true)
public class MDBTests {
    
    private static final String GLASSFISH_APPCLIENT_MAIN_CLASS_NAME =
            "org.glassfish.appclient.client.AppClientGroupFacade";
    private boolean execReturn;
    private final String GLASSFISH_HOME = System.getProperty("glassfish.home");
    private final String APPCLIENT = System.getProperty("APPCLIENT");
    private final String ASADMIN = System.getProperty("ASADMIN");
    private final String cwd = System.getProperty("BASEDIR") == null ? System.getProperty("user.dir") : System.getProperty("BASEDIR");
    private String cmd;
    private final String mdbApp = "ejb-ejb30-hello-mdbApp";

    @Parameters({ "BATCH_FILE1" })
    @Test
    public void createJMSRscTest(String batchFile1) throws Exception {
        Assert.assertEquals(
            RtExec.execute(
                "MDBTests.createJMSRscTest", 
                ASADMIN + " multimode --port 4848 --file " + cwd + File.separator + batchFile1), 
            true, 
            "Create JMS resource failed ...");
    }

    @Parameters({ "MDB_APP_DIR" })
    @Test(dependsOnMethods = { "createJMSRscTest" })
    public void deployJMSAppTest(String mdbAppDir) throws Exception {
        cmd = ASADMIN + " deploy --port 4848 --retrieve=" + cwd + File.separator + mdbAppDir + " " + cwd + File.separator + mdbAppDir
                + mdbApp + ".ear ";
        execReturn = RtExec.execute("MDBTests.deployJMSAppTest", cmd);
        
        Assert.assertEquals(execReturn, true, "Deploy the mdb app failed ... ");
    }

    @Parameters({ "MDB_APP_DIR" })
    @Test(dependsOnMethods = { "deployJMSAppTest" })
    public void runJMSAppTest(String mdbAppDir) throws Exception {
        String clientJar = cwd + File.separator + mdbAppDir + mdbApp + "Client.jar";
        String gfClientJar = GLASSFISH_HOME + File.separator + "lib" + File.separator + "gf-client.jar";
        cmd = APPCLIENT
                + " --add-opens=java.base/jdk.internal.loader=ALL-UNNAMED --add-exports=java.base/jdk.internal.ref=ALL-UNNAMED --add-opens=jdk.management/com.sun.management.internal=ALL-UNNAMED --add-opens=java.base/java.lang=ALL-UNNAMED --add-opens=java.base/java.io=ALL-UNNAMED --add-opens=java.management/javax.management.openmbean=ALL-UNNAMED --add-opens=java.management/javax.management=ALL-UNNAMED --add-opens=java.base/java.net=ALL-UNNAMED --add-opens=java.base/java.nio=ALL-UNNAMED --add-opens=java.base/java.util=ALL-UNNAMED --add-opens=java.base/sun.nio.ch=ALL-UNNAMED --add-opens=java.management/sun.management=ALL-UNNAMED --add-opens=java.base/sun.net.www.protocol.jrt=ALL-UNNAMED --add-opens=java.base/sun.net.www.protocol.jar=ALL-UNNAMED --add-opens=java.naming/javax.naming.spi=ALL-UNNAMED --add-opens=java.rmi/sun.rmi.transport=ALL-UNNAMED --add-opens=java.logging/java.util.logging=ALL-UNNAMED --add-opens=java.base/sun.net.www=ALL-UNNAMED --add-opens=java.base/sun.security.util=ALL-UNNAMED --add-opens=java.base/java.lang.invoke=ALL-UNNAMED --add-opens=java.desktop/java.beans=ALL-UNNAMED --add-exports=jdk.naming.dns/com.sun.jndi.dns=ALL-UNNAMED --add-exports=java.naming/com.sun.jndi.ldap=ALL-UNNAMED --add-opens=java.base/jdk.internal.misc=ALL-UNNAMED "
                + GLASSFISH_APPCLIENT_MAIN_CLASS_NAME
                + " -client " + clientJar
                + " -targetserver" + " localhost:3700"
                + " -name ejb-ejb30-hello-mdb-client"
                + " -cp " + gfClientJar + File.pathSeparator + clientJar;
        
        execReturn = RtExec.execute("MDBTests.runJMSAppTest", cmd);
        Assert.assertEquals(execReturn, true, "Run appclient against JMS APP failed ...");
    }

    @Test(dependsOnMethods = { "runJMSAppTest" })
    public void undeployJMSAppTest() throws Exception {
        cmd = ASADMIN + " undeploy --port 4848 " + mdbApp;
        execReturn = RtExec.execute("MDBTests.undeployJMSAppTest", cmd);
        
        Assert.assertEquals(execReturn, true, "UnDeploy the mdb app failed ... ");
    }

    @Parameters({ "BATCH_FILE2" })
    @Test(dependsOnMethods = { "undeployJMSAppTest" })
    public void deleteJMSRscTest(String batchFile2) throws Exception {
        cmd = ASADMIN + " multimode --port 4848 --file " + cwd + File.separator + batchFile2;
        execReturn = RtExec.execute("MDBTests.deleteJMSRscTest", cmd);
        
        Assert.assertEquals(execReturn, true, "Delete JMD Resource failed ...");
    }

}
