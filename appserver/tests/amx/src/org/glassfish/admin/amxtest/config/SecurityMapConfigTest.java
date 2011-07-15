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

import com.sun.appserv.management.config.BackendPrincipalConfig;
import com.sun.appserv.management.config.ConnectorConnectionPoolConfig;
import com.sun.appserv.management.config.DomainConfig;
import com.sun.appserv.management.config.ResourceAdapterConfig;
import com.sun.appserv.management.config.SecurityMapConfig;
import com.sun.appserv.management.util.misc.GSetUtil;
import org.glassfish.admin.amxtest.AMXTestBase;

import java.util.Set;


/**
 */
public final class SecurityMapConfigTest
        extends AMXTestBase {
    public SecurityMapConfigTest() {
        if (checkNotOffline("ensureDefaultInstance")) {
            ensureDefaultInstance(getDomainConfig());
        }
    }


    public static String
    getDefaultInstanceName() {
        return getDefaultInstanceName("SecurityMapConfig");
    }

    private static final String DEFAULT_BACKEND_PRINCIPAL = "SecurityMapConfigTest.default";
    private static final String DEFAULT_BACKEND_PASSWORD = "changeme";
    private static final String[] DEFAULT_PRINCIPALS =
            new String[]{"SecurityMapConfigTest.principal1"};
    private static final String[] DEFAULT_USERGROUPS = new String[0];

    public static SecurityMapConfig
    ensureDefaultInstance(final DomainConfig domainConfig) {
        final ConnectorConnectionPoolConfig ccpc =
                ConnectorConnectionPoolConfigTest.ensureDefaultInstance(domainConfig);

        SecurityMapConfig result =
                ccpc.getSecurityMapConfigMap().get(getDefaultInstanceName());
        if (result == null) {
            result = createInstance(ccpc,
                                    getDefaultInstanceName(),
                                    DEFAULT_BACKEND_PRINCIPAL,
                                    DEFAULT_BACKEND_PASSWORD,
                                    DEFAULT_PRINCIPALS,
                                    DEFAULT_USERGROUPS);
        }

        return result;
    }


    private void
    testGetters(final SecurityMapConfig smc) {
        final String[] principalNames = smc.getPrincipalNames();
        final String[] userGroupNames = smc.getUserGroupNames();

        assert (principalNames != null || userGroupNames != null) : "both principals and usergroups are null";

        final BackendPrincipalConfig bpc = smc.getBackendPrincipalConfig();
        assert (bpc != null);
        final String s = bpc.getUserName();
        bpc.setUserName(s);
        final String password = bpc.getPassword();
        bpc.setPassword(password);
    }


    public static SecurityMapConfig
    createInstance(
            final ConnectorConnectionPoolConfig ccpc,
            final String name,
            final String backendPrincipalUsername,
            final String backendPrincipalPassword,
            final String[] principals,
            final String[] userGroups) {
        final SecurityMapConfig smc =
                ccpc.createSecurityMapConfig(name,
                                             backendPrincipalUsername, backendPrincipalPassword,
                                             principals, userGroups);

        return smc;
    }

    private static final String CONNECTOR_DEF_NAME = "javax.resource.cci.ConnectionFactory";

    public void
    testCreateRemove() {
        if (!checkNotOffline("testDeleteLBConfig")) {
            return;
        }

        final String TEST_NAME = "SecurityMapConfigTest.testCreateRemove";
        final ResourceAdapterConfig rac = ResourceAdapterConfigTest.createInstance(
                getDomainConfig(), TEST_NAME);

        try {
            final ConnectorConnectionPoolConfig ccpc =
                    ConnectorConnectionPoolConfigTest.createInstance(getDomainConfig(),
                                                                     TEST_NAME,
                                                                     CONNECTOR_DEF_NAME,
                                                                     rac.getName(), null);

            try {
                final String smcName = "SecurityMapConfigTest.testCreateRemove";
                final String[] principals = new String[]{"SecurityMapConfigTest.testCreateRemove"};
                final String[] userGroups = new String[0];
                final SecurityMapConfig smc = createInstance(
                        ccpc,
                        smcName,
                        DEFAULT_BACKEND_PRINCIPAL,
                        DEFAULT_BACKEND_PASSWORD,
                        principals,
                        null);
                try {
                    assert (smcName.equals(smc.getName()));
                    assert (smc == ccpc.getSecurityMapConfigMap().get(smc.getName()));
                    testGetters(smc);

                    final Set<String> principalsBefore = GSetUtil.newSet(smc.getPrincipalNames());
                    final String PRINCIPAL1 = "testCreateRemove.test1";
                    smc.createPrincipal(PRINCIPAL1);

                    final Set<String> principalsAfter = GSetUtil.newSet(smc.getPrincipalNames());
                    assert (principalsAfter.contains(PRINCIPAL1));

                    smc.removePrincipal(PRINCIPAL1);
                    assert (principalsBefore.equals(GSetUtil.newSet(smc.getPrincipalNames())));

                }
                finally {
                    ccpc.removeSecurityMapConfig(smc.getName());
                }
            }
            finally {
                getDomainConfig().getResourcesConfig().removeConnectorConnectionPoolConfig(ccpc.getName());
            }
        }
        finally {
            getDomainConfig().getResourcesConfig().removeResourceAdapterConfig(rac.getName());
        }
    }

}



























