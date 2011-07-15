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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.glassfish.deployment.client;

import java.io.File;
import java.util.Properties;
import javax.enterprise.deploy.spi.Target;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author tjquinn
 */
public class TestDeploy {

    private static final String APP_NAME = "servletonly";

    public TestDeploy() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    /*
     * Note that the two tests below are here as examples of how to use the
     * DeploymentFacility.  In their current form they should not be used
     * as tests, because they would require the server to be up.
     */
    @Ignore 
    @Test
    public void testDeploy() {
        DeploymentFacility df = DeploymentFacilityFactory.getDeploymentFacility();
        ServerConnectionIdentifier sci = new ServerConnectionIdentifier();
        sci.setHostName("localhost");
        sci.setHostPort(4848); // 8080 for the REST client
        sci.setUserName("admin");
        sci.setPassword("");
        
        df.connect(sci);
        
        File archive = new File("/home/hzhang/deployment/apps/jsr88/servletonly-portable.war");
        File plan = new File("/home/hzhang/deployment/apps/jsr88/servletonly-deployplan.jar");
        DFDeploymentProperties options = new DFDeploymentProperties();
        options.setForce(true);
        options.setUpload(true);
        options.setName(APP_NAME);
        Properties props = new Properties();
        props.setProperty("keepSessions", "true");
        props.setProperty("foo", "bar");
        options.setProperties(props);

        try {
        Target[] targets = df.listTargets(); 
        DFProgressObject prog = df.deploy(
                targets /* ==> deploy to the default target */, 
                archive.toURI(), 
                plan.toURI(),
                options);
        DFDeploymentStatus ds = prog.waitFor();
        
        if (ds.getStatus() == DFDeploymentStatus.Status.FAILURE) {
            fail(ds.getAllStageMessages());
        }
        } catch (Exception e) {
          e.printStackTrace();
          fail(e.getMessage());
        }
        
    }
    
    @Ignore
    @Test
    public void testUndeploy() {
        DeploymentFacility df = DeploymentFacilityFactory.getDeploymentFacility();
        ServerConnectionIdentifier sci = new ServerConnectionIdentifier();
        sci.setHostName("localhost");
        sci.setHostPort(4848); 
        sci.setUserName("admin");
        sci.setPassword("");
        
        df.connect(sci);
        
        try{
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

/* test negative case 
        DFProgressObject prog = df.undeploy(
                clusterTarget, 
                APP_NAME);
*/

        DFProgressObject prog = df.undeploy(
                targets, 
                APP_NAME);

        DFDeploymentStatus ds = prog.waitFor();
        
        if (ds.getStatus() == DFDeploymentStatus.Status.FAILURE) {
            fail(ds.getAllStageMessages());
        }
        } catch (Exception e) {
            e.printStackTrace();
            fail(e.getMessage());
        }
        
    }
}
