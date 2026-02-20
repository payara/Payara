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
// Portions Copyright 2026 Payara Foundation and/or its affiliates

package test.hellodeploymentgroup;

import com.sun.appserv.test.AdminBaseDevTest;
import java.io.File;
import org.testng.annotations.Test;
import org.testng.Assert;

public class DeploymentGroupHelloworldTest extends AdminBaseDevTest {

    @Override
    protected String getTestDescription() {
        return "QL deployment group test helloworld";
    }


        final String tn = "QLGroup";
        final String port1 = "18080";
        final String port2 = "28080";
        final String cname = "eec1";
        String i1url = "http://localhost:"+port1;
        String i2url = "http://localhost:"+port2;
        static String BASEDIR = System.getProperty("BASEDIR");
        
        public boolean retStatus = false, ret1 = false,ret2 = false;


    @Test
    public void deploymentGroupDeployTest() throws Exception{
        // deploy an web application to the group
        File webapp = new File(BASEDIR+"/dist/hellodeploymentgroup", "helloworld.war");
        retStatus = report(tn + "deploy", asadmin("deploy", "--target", cname, webapp.getAbsolutePath()));
        Assert.assertEquals(retStatus, true, "Deployment Group deployment failed ...");
    }

    @Test(dependsOnMethods = { "deploymentGroupDeployTest" })
    public void deploymentGroupHelloWorldTest() throws Exception{
        System.out.println("Wait extra 5 sec for GF to generate helloworld app.");
	Thread.currentThread().sleep(5000);
        report(tn + "getapp1", matchString("Hello", getURL(i1url + "/helloworld/hi.jsp")));
        String s1 = getURL(i2url + "/helloworld/hi.jsp");
//        System.out.println("output from instance 2:" + s1);
        retStatus = report(tn + "getapp2", matchString("Hello", s1));
        Assert.assertEquals(retStatus, true, "Accessing helloworld page failed ...");
    }

    @Test(dependsOnMethods = { "deploymentGroupHelloWorldTest" })
    public void deploymentGroupUnDeployTest() throws Exception{
        retStatus = report(tn + "undeploy", asadmin("undeploy", "--target", cname, "helloworld"));
        Assert.assertEquals(retStatus, true, "Deployment Group undeployment failed ...");
    }
}
