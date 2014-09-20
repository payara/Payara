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

package org.glassfish.admin.amxtest.config;

import com.sun.appserv.management.base.AMX;
import com.sun.appserv.management.base.Util;
import com.sun.appserv.management.config.DomainConfig;
import org.glassfish.admin.amxtest.AMXTestBase;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;


/**
 */
public final class DomainConfigTest
        extends AMXTestBase {
    public DomainConfigTest() {
    }

    public void
    testGetDeployedItemProxies() {
        final DomainConfig mgr = getDomainConfig();

        final Set proxies = mgr.getContaineeSet();
        assert (proxies.size() != 0);

        final Iterator iter = proxies.iterator();
        while (iter.hasNext()) {
            final AMX proxy = Util.asAMX(iter.next());
        }
    }

    public void
    testGetDeployedItemProxiesByName() {
        final DomainConfig mgr = getDomainConfig();

        final Map<String, Map<String, AMX>> typeMap = mgr.getMultiContaineeMap(null);

        for (final String j2eeType : typeMap.keySet()) {
            final Map<String, AMX> proxyMap = typeMap.get(j2eeType);
            for (final String name : proxyMap.keySet()) {
                final AMX amx = Util.asAMX(proxyMap.get(name));

                final AMX proxy = mgr.getContainee(j2eeType, name);

                assert (Util.getObjectName(proxy).equals(Util.getObjectName(amx)));
                assert (proxy.getName().equals(name));
            }
        }
    }


    public void
    testGetAttributes() {
        final DomainConfig mgr = getDomainConfig();

        mgr.getApplicationRoot();
        mgr.getLocale();
        mgr.getLogRoot();
    }

    private <T extends AMX> void
    checkMap(final Map<String, T> m) {
        assert (m != null);
        assert (!m.keySet().contains(AMX.NO_NAME));
        assert (!m.keySet().contains(AMX.NULL_NAME));
    }


    public void
    testGetMaps() {
        final DomainConfig m = getDomainConfig();

        //checkMap(m.getServersConfig().getServerConfigMap());
        checkMap(m.getServersConfig().getStandaloneServerConfigMap());
        checkMap(m.getServersConfig().getClusteredServerConfigMap());
        checkMap(m.getLBConfigsConfig().getLBConfigMap());
        checkMap(m.getLoadBalancersConfig().getLoadBalancerConfigMap());
        checkMap(m.getNodeAgentsConfig().getNodeAgentConfigMap());
        checkMap(m.getConfigsConfig().getConfigConfigMap());
        checkMap(m.getClustersConfig().getClusterConfigMap());

        checkMap(m.getResourcesConfig().getPersistenceManagerFactoryResourceConfigMap());
        checkMap(m.getResourcesConfig().getJDBCResourceConfigMap());
        checkMap(m.getResourcesConfig().getJDBCConnectionPoolConfigMap());
        checkMap(m.getResourcesConfig().getConnectorResourceConfigMap());
        checkMap(m.getResourcesConfig().getConnectorConnectionPoolConfigMap());
        checkMap(m.getResourcesConfig().getAdminObjectResourceConfigMap());
        checkMap(m.getResourcesConfig().getResourceAdapterConfigMap());
        checkMap(m.getResourcesConfig().getMailResourceConfigMap());

        //checkMap(m.getApplicationsConfig().getJ2EEApplicationConfigMap());
        checkMap(m.getApplicationsConfig().getEJBModuleConfigMap());
        checkMap(m.getApplicationsConfig().getWebModuleConfigMap());
        checkMap(m.getApplicationsConfig().getRARModuleConfigMap());
        checkMap(m.getApplicationsConfig().getAppClientModuleConfigMap());
        checkMap(m.getApplicationsConfig().getLifecycleModuleConfigMap());
    }

    /*
         KEEP, not quite ready to test this yet.
         public void
     testCreateStandaloneServerConfig()
     {
         final ConfigSetup setup  = new ConfigSetup( getDomainRoot() );

         setup.removeTestServer();

         final StandaloneServerConfig server = setup.createTestServer();
         setup.removeTestServer();
     }
     */


    public void
    testCreateClusterConfig() {
        // to be done
    }
}



























