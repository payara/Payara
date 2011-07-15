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
import com.sun.appserv.management.config.CustomResourceConfig;
import com.sun.appserv.management.config.DomainConfig;

import java.util.HashMap;
import java.util.Map;

/**
 */
public final class CustomResourceConfigTest
        extends ResourceConfigTestBase {
    static final Map<String, String> OPTIONAL = new HashMap<String, String>();

    // doesn't exist, just give a syntactically valid name
    static private final String RES_TYPE = "CustomResourceConfigTest.Dummy";
    static private final String FACTORY_CLASS =
            "org.glassfish.admin.amxtest.config.CustomResourceConfigTestDummy";

    public CustomResourceConfigTest() {
        if (checkNotOffline("ensureDefaultInstance")) {
            ensureDefaultInstance(getDomainConfig());
        }
    }

    public static String
    getDefaultInstanceName() {
        return getDefaultInstanceName("CustomResourceConfig");
    }


    public static CustomResourceConfig
    ensureDefaultInstance(final DomainConfig domainConfig) {
        CustomResourceConfig result =
                domainConfig.getResourcesConfig().getCustomResourceConfigMap().get(getDefaultInstanceName());

        if (result == null) {
            result = createInstance(domainConfig,
                                    getDefaultInstanceName(), RES_TYPE, FACTORY_CLASS, null);
        }

        return result;
    }

    public static CustomResourceConfig
    createInstance(
            final DomainConfig domainConfig,
            final String name,
            final String resType,
            final String factoryClass,
            final Map<String, String> optional) {
        final CustomResourceConfig config =
                domainConfig.getResourcesConfig().createCustomResourceConfig(name, resType, factoryClass, optional);

        return config;
    }


    protected Container
    getProgenyContainer() {
        return getDomainConfig();
    }

    protected String
    getProgenyJ2EEType() {
        return XTypes.CUSTOM_RESOURCE_CONFIG;
    }


    protected void
    removeProgeny(final String name) {
        getDomainConfig().getResourcesConfig().removeCustomResourceConfig(name);
    }

    protected String
    getProgenyTestName() {
        return ("CustomResourceConfigTest");
    }

    protected final AMXConfig
    createProgeny(
            final String name,
            final Map<String, String> options) {
        final CustomResourceConfig config =
                createInstance(getDomainConfig(), name, RES_TYPE, FACTORY_CLASS, options);

        addReference(config);

        return config;
    }
}


