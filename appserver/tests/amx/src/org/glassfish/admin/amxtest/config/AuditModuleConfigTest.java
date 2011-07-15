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
* $Header: /cvs/glassfish/admin/mbeanapi-impl/tests/org.glassfish.admin.amxtest/config/AuditModuleConfigTest.java,v 1.6 2007/05/05 05:23:54 tcfujii Exp $
* $Revision: 1.6 $
* $Date: 2007/05/05 05:23:54 $
*/
package org.glassfish.admin.amxtest.config;

import com.sun.appserv.management.base.Container;
import com.sun.appserv.management.base.XTypes;
import com.sun.appserv.management.config.AMXConfig;
import com.sun.appserv.management.config.AuditModuleConfig;
import com.sun.appserv.management.config.SecurityServiceConfig;

import java.util.Map;


/**
 */
public final class AuditModuleConfigTest
        extends ConfigMgrTestBase {
    static final String CLASSNAME = "com.sun.enterprise.security.Audit";

    public AuditModuleConfigTest() {
        if (checkNotOffline("ensureDefaultInstance")) {
            ensureDefaultInstance(getConfigConfig().getSecurityServiceConfig());
        }
    }


    public static String
    getDefaultInstanceName() {
        return getDefaultInstanceName("AuditModuleConfig");
    }


    public static AuditModuleConfig
    ensureDefaultInstance(final SecurityServiceConfig securityServiceConfig) {
        AuditModuleConfig result =
                securityServiceConfig.getAuditModuleConfigMap().get(getDefaultInstanceName());

        if (result == null) {
            result = createInstance(securityServiceConfig,
                                    getDefaultInstanceName(), CLASSNAME, false, null);
        }

        return result;
    }

    public static AuditModuleConfig
    createInstance(
            final SecurityServiceConfig securityServiceConfig,
            final String name,
            final String classname,
            final boolean enabled,
            final Map<String, String> optional) {
        return securityServiceConfig.createAuditModuleConfig(
                name, CLASSNAME, enabled, null);
    }


    protected Container
    getProgenyContainer() {
        return getConfigConfig().getSecurityServiceConfig();
    }

    protected String
    getProgenyJ2EEType() {
        return XTypes.AUDIT_MODULE_CONFIG;
    }


    protected void
    removeProgeny(final String name) {
        getConfigConfig().getSecurityServiceConfig().removeAuditModuleConfig(name);
    }

    protected final AMXConfig
    createProgeny(
            final String name,
            final Map<String, String> options) {
        return getConfigConfig().getSecurityServiceConfig().createAuditModuleConfig(name, CLASSNAME, false, options);
    }
}


