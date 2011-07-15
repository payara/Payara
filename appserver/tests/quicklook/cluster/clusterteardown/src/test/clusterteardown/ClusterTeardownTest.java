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

package test.clusterteardown;

import com.sun.appserv.test.AdminBaseDevTest;
import org.testng.annotations.Test;
import org.testng.Assert;

public class ClusterTeardownTest extends AdminBaseDevTest {

    @Override
    protected String getTestDescription() {
        return "QL Cluster TEARDOWN Test";
    }
        final String tn = "QLCluster";
        final String cname = "eec1";
        final String i1name = "eein1-with-a-very-very-very-long-name";
        final String i2name = "eein2";
        
        public boolean retStatus;

        // Byron Nevins Nov 4,2010 -- Add plenty of output if there are problems.
        // previously deleteInstanceTest would never say boo no matter what happened...
    @Test
    public void deleteInstanceTest() throws Exception{
        AsadminReturn ar1 = asadminWithOutput("stop-local-instance", "--kill", i1name);
        AsadminReturn ar2 = asadminWithOutput("stop-local-instance", "--kill", i2name);
        AsadminReturn ar3 = asadminWithOutput("delete-local-instance", i1name);
        AsadminReturn ar4 = asadminWithOutput("delete-local-instance", i2name);

        report(tn + "stop-local-instance1", ar1.returnValue);
        report(tn + "stop-local-instance2", ar2.returnValue);
        report(tn + "delete-local-instance1", ar3.returnValue);
        report(tn + "delete-local-instance2", ar4.returnValue);

        Assert.assertTrue(ar1.returnValue, "Error stopping instance " + i1name + ": " + ar1.outAndErr);
        Assert.assertTrue(ar2.returnValue, "Error stopping instance " + i2name + ": " + ar2.outAndErr);
        Assert.assertTrue(ar3.returnValue, "Error deleting instance " + i1name + ": " + ar3.outAndErr);
        Assert.assertTrue(ar4.returnValue, "Error deleting instance " + i2name + ": " + ar4.outAndErr);
    }
        
    @Test(dependsOnMethods = { "deleteInstanceTest" })
    public void deleteClusterTest() throws Exception{
        AsadminReturn ar1 = asadminWithOutput("delete-cluster", cname);
        retStatus = report(tn + "delete-cluster", ar1.returnValue);

        Assert.assertTrue(retStatus, "Error deleting cluster " + cname + ": " + ar1.outAndErr);
    }
}
