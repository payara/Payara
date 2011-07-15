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
import com.sun.appserv.management.config.JNDIResourceConfig;

import java.util.Map;

/**
 */
public final class JNDIResourceConfigTest
        extends ResourceConfigTestBase {
    private static final String JNDI_RESOURCE_JNDI_LOOKUP_NAME = "jndi/jndiTest";
    private static final String JNDI_RESOURCE_RES_TYPE = "javax.sql.DataSource";
    private static final String JNDI_RESOURCE_FACTORY_CLASS = "com.sun.jdo.spi.persistence.support.sqlstore.impl.PersistenceManagerFactoryImpl";
    private static final Map<String, String> OPTIONAL = null;

    public JNDIResourceConfigTest() {
        if (checkNotOffline("ensureDefaultInstance")) {
            ensureDefaultInstance(getDomainConfig());
        }
    }

    public static String
    getDefaultInstanceName() {
        return getDefaultInstanceName("JNDIResourceConfig");
    }

    public static JNDIResourceConfig
    ensureDefaultInstance(final DomainConfig dc) {
        JNDIResourceConfig result =
                dc.getResourcesConfig().getJNDIResourceConfigMap().get(getDefaultInstanceName());

        if (result == null) {
            result = createInstance(dc,
                                    getDefaultInstanceName(),
                                    JNDI_RESOURCE_JNDI_LOOKUP_NAME,
                                    JNDI_RESOURCE_RES_TYPE,
                                    JNDI_RESOURCE_FACTORY_CLASS,
                                    OPTIONAL);
        }

        return result;
    }

    public static JNDIResourceConfig
    createInstance(
            final DomainConfig dc,
            final String name,
            final String jndiLookupName,
            final String resType,
            final String factoryClass,
            final Map<String, String> optional) {
        return dc.getResourcesConfig().createJNDIResourceConfig(
                name, jndiLookupName, resType, factoryClass, optional);
    }


    protected Container
    getProgenyContainer() {
        return getDomainConfig();
    }

    protected String
    getProgenyJ2EEType() {
        return XTypes.JNDI_RESOURCE_CONFIG;
    }


    protected void
    removeProgeny(final String name) {
        final JNDIResourceConfig item =
                getDomainConfig().getResourcesConfig().getJNDIResourceConfigMap().get(name);

        getDomainConfig().getResourcesConfig().removeJNDIResourceConfig(name);
    }

    protected final AMXConfig
    createProgeny(
            final String name,
            final Map<String, String> options) {
        final JNDIResourceConfig config = getDomainConfig().getResourcesConfig().createJNDIResourceConfig(name,
                                                                                     JNDI_RESOURCE_JNDI_LOOKUP_NAME,
                                                                                     JNDI_RESOURCE_RES_TYPE,
                                                                                     JNDI_RESOURCE_FACTORY_CLASS,
                                                                                     options);

        addReference(config);

        return (config);
    }
}


