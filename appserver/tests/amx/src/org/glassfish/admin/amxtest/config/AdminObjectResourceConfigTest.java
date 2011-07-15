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
import com.sun.appserv.management.config.AdminObjectResourceConfig;
import com.sun.appserv.management.config.DomainConfig;

import java.util.Map;

/**
 */
public final class AdminObjectResourceConfigTest
        extends ResourceConfigTestBase {
    private static final String ADM_OBJ_RES_TYPE = "user";
    private static final String ADM_OBJ_RES_ADAPTER = "cciblackbox-tx";


    public AdminObjectResourceConfigTest() {
        if (checkNotOffline("ensureDefaultInstance")) {
            ensureDefaultInstance(getDomainConfig());
        }
    }

    public static String
    getDefaultInstanceName() {
        return getDefaultInstanceName("AdminObjectResourceConfig");
    }

    public static AdminObjectResourceConfig
    ensureDefaultInstance(final DomainConfig domainConfig) {
        AdminObjectResourceConfig result =
                domainConfig.getResourcesConfig().getAdminObjectResourceConfigMap().get(getDefaultInstanceName());

        if (result == null) {
            result = createInstance(
                    domainConfig,
                    getDefaultInstanceName(),
                    ADM_OBJ_RES_TYPE,
                    ADM_OBJ_RES_ADAPTER,
                    null);
        }

        return result;
    }

    public static AdminObjectResourceConfig
    createInstance(
            final DomainConfig domainConfig,
            final String name,
            final String resType,
            final String resAdapter,
            final Map<String, String> optional) {
        return domainConfig.getResourcesConfig().createAdminObjectResourceConfig(name,
                                                            resType, resAdapter, optional);
    }


    protected Container
    getProgenyContainer() {
        return getDomainConfig();
    }

    protected String
    getProgenyJ2EEType() {
        return XTypes.ADMIN_OBJECT_RESOURCE_CONFIG;
    }


    protected void
    removeProgeny(final String name) {
        getDomainConfig().getResourcesConfig().removeAdminObjectResourceConfig(name);
    }

    protected String
    getProgenyTestName() {
        return ("jndi/AdminObjectResourceConfigMgrTest");
    }

    protected final AMXConfig
    createProgeny(
            final String name,
            final Map<String, String> options) {
        final AdminObjectResourceConfig config = getDomainConfig().getResourcesConfig().createAdminObjectResourceConfig(
                name,
                ADM_OBJ_RES_TYPE,
                ADM_OBJ_RES_ADAPTER,
                options);

        addReference(config);

        return (config);
    }
}


