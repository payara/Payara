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
import com.sun.appserv.management.base.Util;
import com.sun.appserv.management.base.XTypes;
import com.sun.appserv.management.config.AMXConfig;
import com.sun.appserv.management.config.ConnectorConnectionPoolConfig;
import com.sun.appserv.management.config.ConnectorResourceConfig;
import com.sun.appserv.management.config.DomainConfig;
import com.sun.appserv.management.config.ResourceRefConfig;
import com.sun.appserv.management.util.misc.CollectionUtil;

import javax.management.ObjectName;
import java.util.Map;
import java.util.Set;

/**
 */
public final class ConnectorResourceConfigTest
        extends ResourceConfigTestBase {
    public ConnectorResourceConfigTest() {
        if (checkNotOffline("ensureDefaultInstance")) {
            ensureDefaultInstance(getDomainConfig());
        }
    }

    public static String
    getDefaultInstanceName() {
        return getDefaultInstanceName("ConnectorResourceConfig");
    }

    public static ConnectorResourceConfig
    ensureDefaultInstance(final DomainConfig domainConfig) {
        ConnectorResourceConfig result =
                domainConfig.getResourcesConfig().getConnectorResourceConfigMap().get(getDefaultInstanceName());

        final ConnectorConnectionPoolConfig connectorConnectionPool =
                ConnectorConnectionPoolConfigTest.ensureDefaultInstance(domainConfig);

        if (result == null) {
            result = createInstance(domainConfig,
                                    getDefaultInstanceName(),
                                    connectorConnectionPool.getName(), null);
        }

        return result;
    }

    public static ConnectorResourceConfig
    createInstance(
            final DomainConfig domainConfig,
            final String name,
            final String poolName,
            final Map<String, String> optional) {
        return domainConfig.getResourcesConfig().createConnectorResourceConfig(
                name, poolName, optional);
    }

    protected String
    getProgenyTestName() {
        return ("jndi/ConnectorResourceConfigTest");
    }

    protected Container
    getProgenyContainer() {
        return getDomainConfig();
    }

    protected String
    getProgenyJ2EEType() {
        return XTypes.CONNECTOR_RESOURCE_CONFIG;
    }


    protected void
    removeProgeny(final String name) {
        final Set<ResourceRefConfig> resourceRefs =
                getQueryMgr().queryJ2EETypeNameSet(XTypes.RESOURCE_REF_CONFIG, name);

        getDomainConfig().getResourcesConfig().removeConnectorResourceConfig(name);
    }


    protected final AMXConfig
    createProgeny(
            final String name,
            final Map<String, String> options) {
        final String poolName =
                ConnectorConnectionPoolConfigTest.ensureDefaultInstance(getDomainConfig()).getName();

        assert (getDomainConfig().getResourcesConfig().getConnectorResourceConfigMap().get(name) == null) :
                "A resource already exists with name: " + name;

        final Set<ResourceRefConfig> resourceRefs =
                getQueryMgr().queryJ2EETypeNameSet(XTypes.RESOURCE_REF_CONFIG, name);

        ConnectorResourceConfig config = null;

        final Set<ObjectName> resourceRefObjectNames = Util.toObjectNames(resourceRefs);
        if (resourceRefs.size() != 0) {
            assert (false);
            warning("A DANGLING resource ref already exists with name: " + name +
                    ", {" +
                    CollectionUtil.toString(resourceRefObjectNames) + "} (SKIPPING TEST)");
        } else {
            config = getDomainConfig().getResourcesConfig().createConnectorResourceConfig(name,
                                                                     poolName, options);

            final Set<ResourceRefConfig> refs =
                    getQueryMgr().queryJ2EETypeNameSet(XTypes.RESOURCE_REF_CONFIG, name);
            if (resourceRefs.size() != 0) {
                final ResourceRefConfig ref = refs.iterator().next();

                warning("A resource ref within " +
                        Util.getObjectName(ref.getContainer()) +
                        " was automatically created when creating the ConnectorResourceConfig ");
            }
        }

        addReference(config);

        return (config);
    }
}


