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

import com.sun.appserv.management.base.Container;
import com.sun.appserv.management.base.XTypes;
import com.sun.appserv.management.config.AMXConfig;
import com.sun.appserv.management.config.DomainConfig;
import com.sun.appserv.management.config.JDBCConnectionPoolConfig;
import com.sun.appserv.management.config.JDBCResourceConfig;

import java.util.Map;

/**
 */
public final class JDBCResourceConfigTest
        extends ResourceConfigTestBase {
    private static final String JDBC_RESOURCE_POOL_NAME_BASE = "JDBCResourceConfigMgrTest.test-pool";
    private static final String JDBC_DATASOURCE_CLASSNAME = "com.pointbase.xa.xaDataSource";
    private static final Map<String, String> OPTIONAL = null;

    private JDBCConnectionPoolConfig mPool;

    public JDBCResourceConfigTest() {
        mPool = null;
        if (checkNotOffline("ensureDefaultInstance")) {
            ensureDefaultInstance(getDomainConfig());
        }
    }

    public static String
    getDefaultInstanceName() {
        return getDefaultInstanceName("JDBCResourceConfig");
    }

    public static JDBCResourceConfig
    ensureDefaultInstance(final DomainConfig domainConfig) {
        JDBCResourceConfig result =
                domainConfig.getResourcesConfig().getJDBCResourceConfigMap().get(getDefaultInstanceName());

        if (result == null) {
            final JDBCConnectionPoolConfig pool =
                    JDBCConnectionPoolConfigTest.ensureDefaultInstance(domainConfig);

            result = createInstance(domainConfig,
                                    getDefaultInstanceName(), pool.getName(), OPTIONAL);
        }

        return result;
    }

    public static JDBCResourceConfig
    createInstance(
            final DomainConfig domainConfig,
            final String name,
            final String datasourceClassname,
            final Map<String, String> optional) {
        return domainConfig.getResourcesConfig().createJDBCResourceConfig(
                name, datasourceClassname, optional);
    }


    protected Container
    getProgenyContainer() {
        return getDomainConfig();
    }

    protected String
    getProgenyJ2EEType() {
        return XTypes.JDBC_RESOURCE_CONFIG;
    }


    protected void
    removeProgeny(final String name) {
        getDomainConfig().getResourcesConfig().removeJDBCConnectionPoolConfig(name);
    }

    protected String
    getProgenyTestName() {
        return ("jdbc/JDBCResourceConfigMgrTest");
    }

    private JDBCConnectionPoolConfig
    createPool(final String name) {
        try {
            getDomainConfig().getResourcesConfig().removeJDBCConnectionPoolConfig(name);
        }
        catch (Exception e) {
        }

        final JDBCConnectionPoolConfig config =
                getDomainConfig().getResourcesConfig().createJDBCConnectionPoolConfig(name, JDBC_DATASOURCE_CLASSNAME, null);

        return (config);
    }

    protected final AMXConfig
    createProgeny(
            final String name,
            final Map<String, String> options) {
        mPool = createPool(name + "-temppool");

        final JDBCResourceConfig config =
                getDomainConfig().getResourcesConfig().createJDBCResourceConfig(name, mPool.getName(), options);
        assert (config != null);

        addReference(config);

        return (config);
    }

    protected final void
    remove(String name) {
        getDomainConfig().getResourcesConfig().removeJDBCResourceConfig(name);

        if (mPool != null) {
            getDomainConfig().getResourcesConfig().removeJDBCConnectionPoolConfig(mPool.getName());
            mPool = null;
        }
    }

}


