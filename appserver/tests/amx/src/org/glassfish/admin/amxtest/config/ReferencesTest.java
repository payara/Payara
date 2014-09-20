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

/*
 * $Header: /cvs/glassfish/admin/mbeanapi-impl/tests/org.glassfish.admin.amxtest/config/ReferencesTest.java,v 1.8 2007/05/05 05:23:55 tcfujii Exp $
 * $Revision: 1.8 $
 * $Date: 2007/05/05 05:23:55 $
 */
package org.glassfish.admin.amxtest.config;

import com.sun.appserv.management.client.AppserverConnectionSource;
import com.sun.appserv.management.config.ConfigConfig;
import com.sun.appserv.management.config.DomainConfig;
import com.sun.appserv.management.config.MailResourceConfig;
import com.sun.appserv.management.config.ResourceRefConfig;
import com.sun.appserv.management.config.StandaloneServerConfig;
import org.glassfish.admin.amxtest.AMXTestBase;

import java.util.Map;


/**
 */
public final class ReferencesTest
        extends AMXTestBase {
    public ReferencesTest() {
    }


    private static final String MAIL_RESOURCE_NAME = "test/mail1";

    MailResourceConfig
    createMailResourceConfig() {
        final MailResourceConfig mr =
                getDomainConfig().getResourcesConfig().createMailResourceConfig(
                        MAIL_RESOURCE_NAME,
                        "localhost",
                        "mailuser@domain.com",
                        "mailfrom@domain.com",
                        null);
        return mr;
    }

    MailResourceConfig
    ensureMailResourceConfig() {
        final Map<String, MailResourceConfig> mails =
                getDomainConfig().getResourcesConfig().getMailResourceConfigMap();

        MailResourceConfig mr = mails.get(MAIL_RESOURCE_NAME);
        if (mr == null) {
            mr = createMailResourceConfig();
        }
        return mr;
    }

    void
    removeMailResourceConfig() {
        final Map<String, MailResourceConfig> mails =
                getDomainConfig().getResourcesConfig().getMailResourceConfigMap();

        MailResourceConfig mr = mails.get(MAIL_RESOURCE_NAME);
        if (mr != null) {
            getDomainConfig().getResourcesConfig().removeMailResourceConfig(mr.getName());
        }
    }

    public void
    testCreateAndRemoveResourceRefStandaloneServer()
            throws Exception {
        if (!checkNotOffline("testCreateRemove")) {
            return;
        }

        final Map<String, AppserverConnectionSource> nodeAgents =
                getNodeAgents();

        if (nodeAgents.keySet().size() == 0) {
            warning(
                    "testCreateAndRemoveResourceRefStandaloneServer: no Node Agents--SKIPPING TEST");
            return;
        }
        final String nodeAgentName = nodeAgents.keySet().iterator().next();

        final ConfigSetup setup = new ConfigSetup(getDomainRoot());

        ensureMailResourceConfig();

        final String serverName = "testCreateAndRemoveResourceRefStandaloneServer";
        final String configName = serverName + "-config";

        final ConfigConfig config = setup.createConfig(configName);
        try {
            final int basePort = 34770;

            final StandaloneServerConfig server =
                    setup.createServer(serverName, basePort, nodeAgentName, configName);
            try {
                final ResourceRefConfig ref =
                        server.createResourceRefConfig(MAIL_RESOURCE_NAME);
                assert (ref != null);

                server.removeResourceRefConfig(ref.getName());
            }
            catch (Exception e) {
                setup.removeServer(serverName);
                throw e;
            }
            finally {
                removeMailResourceConfig();
            }
        }
        catch (Exception e) {
            setup.removeConfig(configName);
            throw e;
        }
    }

    public void
    testCreateRefToMissingTarget() {
        if (!checkNotOffline("testCreateRemove")) {
            return;
        }

        final String MISSING_NAME = "NonExistent";

        final DomainConfig domainConfig = getDomainRoot().getDomainConfig();
        final StandaloneServerConfig serverConfig =
                domainConfig.getServersConfig().getStandaloneServerConfigMap().get("server");

        try {
            serverConfig.createResourceRefConfig(MISSING_NAME);
            assert false;

            serverConfig.createDeployedItemRefConfig(MISSING_NAME);
            assert false;
        }
        catch (Exception e) {
            // good
        }

    }

    /*
    public void testCreateAndRemoveResourceRefCluster()
    {
        Map clusterMap<String,ClusterConfig> =
            getDomainConfig().getClusterConfigMap();

        ClusterConfig cluster = clusterMap.get("testCluster");

        cluster.createResourceRefConfig("jdbc/PointBase");
        
        cluster.removeResourceRefConfig("jdbc/PointBase");
    }
    */

    /*



   public void testCreateAndRemoveDeployedItemRef()  {
       trace("\n..testCreateAndRemoveDeployedItemRef");

       StandaloneServerConfigMgr proxy =
       getDomainConfig().getStandaloneServerConfigMgr();

       ClusterConfigMgr c =
           getDomainConfig().getClusterConfigMgr();

       Map serverMap =
           getDomainConfig().getStandaloneServerConfigMap();

       Map clusterMap =
           getDomainConfig().getClusterConfigMap();

       trace("I got the names of the servers!!!!!" + serverMap);
       StandaloneServerConfig server =
       (StandaloneServerConfig)serverMap.get("testServer");

       trace("I got the names of the clusters!!!!!" + clusterMap);
       ClusterConfig cluster =
           (ClusterConfig)clusterMap.get("testCluster");

       DeployedItemRefConfigMgr deploy =
       server.getDeployedItemRefConfigMgr();


       DeployedItemRefConfigMgr deploy2 =
           cluster.getDeployedItemRefConfigMgr();

       trace("I got the deploy item ref mgr proxy from server! " + deploy);

       trace("I got the deploy item ref mgr proxy from cluster! " + deploy2);

       trace("New deployedItemRef: " +
           deploy.create("jndiTree", new java.util.HashMap()));

       trace("New deployedItemRef: " +
           deploy2.create("jndiTree", new java.util.HashMap()));

       try {
           trace("Sleeping.....");
           Thread.sleep(2000);
       } catch (Exception e) {
           e.printStackTrace();
       }

       // remove("testDeployedItemRefServer");
       // c.remove("testDeployedItemRefCluster");

       deploy.remove("jndiTree");
       deploy2.remove("jndiTree");
   }


   public void testCreateAndRemoveServerRef()
            {
       trace("\n..testCreateAndRemoveServerRef");

       Map map =
           getDomainConfig().getClusterConfigMap();
       Set names = map.keySet();
       trace("I got the names of the clusters!!!!!" + map);
       ClusterConfig proxy =
           (ClusterConfig)map.get("testCluster");

       ServerRefConfigMgr serverRefMgr =
           getServerRefConfigMgr();

       trace("I got the server ref proxy! " + serverRefMgr);

       trace("New server reference: " +
           serverRefMgr.create("testServerRef"));

       try {
           trace("Sleeping.....");
           Thread.sleep(2000);
       } catch (Exception e) {
           e.printStackTrace();
       }

       trace("Removing the server reference!!");
       serverRefMgr.remove("testServerRef");

   }


   public void testCreateAndRemoveStandaloneServer()
            {
       trace("\n..testCreateAndRemoveServer");

       ConfigConfigMgr configMgr =
           getDomainConfig().getConfigConfigMgr();

       trace("I got the config proxy! " + configMgr);
       trace("New config: " +
           configMgr.create("testConfig", new java.util.HashMap()));
       StandaloneServerConfigMgr proxy =
           getDomainConfig().getStandaloneServerConfigMgr();

       trace("I got the standalone server proxy! " + proxy);

       assert(proxy != null) : "The StandaloneServerConfigMgr is NULL!!";

       trace("New standAloneServer: " +
           create("testStandaloneServerWithNewSig", "test-agent",
               "testConfig", null));

       try {
           trace("Sleeping.....");
           Thread.sleep(2000);
       } catch (Exception e) {
           e.printStackTrace();
       }

       trace("Removing the standalone server!!");

       remove("testStandaloneServerWithNewSig");

      // configMgr.remove("testConfig");
   }


   public void testCreateAndRemoveClusteredServer()
           throws IOException {
       trace("\n..testCreateAndRemoveClusteredServer");

       ClusteredServerConfigMgr proxy =
           getDomainConfig().getClusteredServerConfigMgr();

       trace("I got the clustered server proxy! " + proxy);


       trace("New cluster: " +
           create("testClusteredServerWithNewSig", null, null, ));

       try {
           trace("Sleeping.....");
           Thread.sleep(2000);
       } catch (Exception e) {
           e.printStackTrace();
       }

       trace("Removing the slustered server!!");
       remove("testClusteredServer");

   }
    */


}


