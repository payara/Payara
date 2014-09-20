/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2010 Oracle and/or its affiliates. All rights reserved.
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

package test.admincli;

import org.testng.annotations.Test;
import org.testng.Assert;
import test.admincli.util.*;

//@Test(sequential = true)
public class ExistStatusTests {
    private boolean execReturn = false;
    private String ASADMIN = System.getProperty("ASADMIN");
    private String cmd, cmd1;
    @Test
    public void createJDBCPool() throws Exception {
//        System.out.println(ASADMIN);
        cmd = ASADMIN + " create-jdbc-connection-pool " +
            "--datasourceclassname=org.apache.derby.jdbc.ClientDataSource --property " +
            "DatabaseName=sun-appserv-samples:PortNumber=1527:serverName=localhost:Password=APP:User=APP QLJdbcPool";

        execReturn = RtExec.execute(cmd);
        Assert.assertEquals(execReturn, true, "Create jdbc connection pool failed ...");
    }

    @Test(dependsOnMethods = { "createJDBCPool" })
    public void pingJDBCPool() throws Exception {
//      extra ping of DerbyPool to create sun-appserv-samples DB.
        cmd = ASADMIN + " ping-connection-pool DerbyPool";
        RtExec.execute(cmd);
        cmd1 = ASADMIN + " ping-connection-pool QLJdbcPool";
        execReturn = RtExec.execute(cmd1);
        Assert.assertEquals(execReturn, true, "Ping jdbc connection pool failed ...");
    }

    @Test(dependsOnMethods = { "pingJDBCPool" })
    public void deleteJDBCPool() throws Exception {
        cmd = ASADMIN + " delete-jdbc-connection-pool QLJdbcPool";
        execReturn = RtExec.execute(cmd);
        Assert.assertEquals(execReturn, true, "Delete jdbc connection pool failed ...");
    }
}
