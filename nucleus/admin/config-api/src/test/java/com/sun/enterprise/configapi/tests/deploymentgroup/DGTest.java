/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Payara Foundation and/or its affiliates. All rights reserved.
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
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package com.sun.enterprise.configapi.tests.deploymentgroup;

import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.configapi.tests.ConfigApiTest;
import fish.payara.enterprise.config.serverbeans.DeploymentGroup;
import fish.payara.enterprise.config.serverbeans.DeploymentGroups;
import java.util.List;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.tests.utils.Utils;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Before;
import org.junit.Test;

/**
 *
 * @author Steve Millidge (Payara Foundation)
 */
public class DGTest extends ConfigApiTest {
    ServiceLocator habitat;

    public String getFileName() {
        return "DGDomain";
    }

    @Before
    public void setup() {
        habitat = Utils.instance.getHabitat(this);        
    }
    
    @Test
    public void getNamedDeploymentGroup() {
        Domain d = habitat.getService(Domain.class);
        DeploymentGroups dgs = d.getDeploymentGroups();
        assertNotNull("Deployment Groups should not be null", dgs);
        DeploymentGroup dg = dgs.getDeploymentGroup("dg1");
        assertNotNull("Deployment Group should not be null", dg);
        assertEquals("Deployment Group Name should be dg1","dg1",dg.getName());
    }
    
    @Test
    public void testThereAreTwo() {
        Domain d = habitat.getService(Domain.class);
        DeploymentGroups dgs = d.getDeploymentGroups();
        assertNotNull("Deployment Groups should not be null", dgs);
        List<DeploymentGroup> ldg = dgs.getDeploymentGroup();
        assertNotNull("Deployment Groups List should not be null", ldg);
        assertEquals("List should have 2 deployment groups", 2L, ldg.size());
    }
    
    @Test
    public void testGetServersFromDG () {
        Domain d = habitat.getService(Domain.class);
        DeploymentGroups dgs = d.getDeploymentGroups();
        assertNotNull("Deployment Groups should not be null", dgs);
        List<DeploymentGroup> ldg = dgs.getDeploymentGroup();
        assertNotNull("Deployment Groups List should not be null", ldg);
        assertEquals("List should have 2 deployment groups", 2L, ldg.size());
        
        // get first named group
        DeploymentGroup dg = dgs.getDeploymentGroup("dg1");
        assertNotNull("Deployment Group dg1 should not be null", dg);
        List<Server> servers = dg.getInstances();
        assertNotNull("Servers List for dg1 should not be null", servers);
        assertEquals("List should have 1 Server", 1L, servers.size());
        Server server = servers.get(0);
        assertNotNull("Server for dg1 should not be null", server);
        assertEquals("Server should be called server", "server", server.getName());

        // get second named group
        dg = dgs.getDeploymentGroup("dg2");
        assertNotNull("Deployment Group dg2 should not be null", dg);
        servers = dg.getInstances();
        assertNotNull("Servers List for dg2 should not be null", servers);
        assertEquals("List should have 2 Servers", 2L, servers.size());
        server = servers.get(0);
        assertNotNull("First Server for dg2 should not be null", server);
        assertEquals("First Server should be called server", "server", server.getName());
        server = servers.get(1);
        assertNotNull("Second Server for dg2 should not be null", server);
        assertEquals("Second Server should be called server2", "server2", server.getName());

        
    }
    
    @Test
    public void testGetNamedDeploymentGroup() {
        Domain d = habitat.getService(Domain.class);
        assertNotNull("dg1 should be found by name from the domain",d.getDeploymentGroupNamed("dg1"));
    }
    
    @Test
    public void testGetDeploymentGroupsForServer() {
        Domain d = habitat.getService(Domain.class);
        DeploymentGroup dg1 = d.getDeploymentGroupNamed("dg1");
        List<DeploymentGroup> dgs = d.getDeploymentGroupsForInstance("server");
        assertEquals("List should have 2 Deployment Groups", 2L, dgs.size());
    }


    @Test
    public void getDeploymentGroupFromServerTest() {
        Domain d = habitat.getService(Domain.class);
        Server server = d.getServerNamed("server2");
        assertTrue(server!=null);
        List<DeploymentGroup> dgs = server.getDeploymentGroup();
        assertEquals("List should have 1 element", 1L, dgs.size());
    }    
}
