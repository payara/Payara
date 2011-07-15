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
* $Header: /cvs/glassfish/admin/mbeanapi-impl/tests/org.glassfish.admin.amxtest/config/JMXConnectorConfigTest.java,v 1.7 2007/05/05 05:23:54 tcfujii Exp $
* $Revision: 1.7 $
* $Date: 2007/05/05 05:23:54 $
*/
package org.glassfish.admin.amxtest.config;

import com.sun.appserv.management.base.Container;
import com.sun.appserv.management.base.XTypes;
import com.sun.appserv.management.config.AMXConfig;
import com.sun.appserv.management.config.AdminServiceConfig;
import com.sun.appserv.management.config.AuthRealmConfig;
import com.sun.appserv.management.config.JMXConnectorConfig;
import com.sun.appserv.management.config.JMXConnectorConfigKeys;
import com.sun.appserv.management.config.PropertiesAccess;
import com.sun.appserv.management.config.SSLConfig;
import com.sun.appserv.management.config.SecurityServiceConfig;
import com.sun.appserv.management.util.misc.MapUtil;

import java.util.HashMap;
import java.util.Map;

/**
 */
public final class JMXConnectorConfigTest
        extends ConfigMgrTestBase {
    static final String ADDRESS = "0.0.0.0";
    static final String TEST_REALM_CLASS = "com.test.DUMMY";
    static final String DEFAULT_PORT = "17377";

    static final Map<String, String> OPTIONAL = new HashMap<String, String>();

    static {
        OPTIONAL.put(PropertiesAccess.PROPERTY_PREFIX + "xyz", "abc");
        OPTIONAL.put(JMXConnectorConfigKeys.SECURITY_ENABLED_KEY, "false");
    }

    public JMXConnectorConfigTest() {
        if (checkNotOffline("ensureDefaultInstance")) {
            ensureDefaultInstance(getConfigConfig().getAdminServiceConfig());
        }
    }

    public static String
    getDefaultInstanceName() {
        return getDefaultInstanceName("JMXConnectorConfig");
    }

    public static JMXConnectorConfig
    ensureDefaultInstance(final AdminServiceConfig adminServiceConfig) {
        JMXConnectorConfig result =
                adminServiceConfig.getJMXConnectorConfigMap().get(getDefaultInstanceName());

        if (result == null) {
            final SecurityServiceConfig securityServiceConfig =
                    getConfigConfig(adminServiceConfig).getSecurityServiceConfig();

            final AuthRealmConfig defaultAuthRealm =
                    AuthRealmConfigTest.ensureDefaultInstance(securityServiceConfig);

            result = createInstance(getDefaultInstanceName(),
                                    ADDRESS, DEFAULT_PORT, defaultAuthRealm, OPTIONAL);
        }

        return result;
    }

    public static JMXConnectorConfig
    createInstance(
            final String name,
            final String address,
            final String port,
            final AuthRealmConfig authRealm,
            final Map<String, String> optional) {
        final AdminServiceConfig adminServiceConfig =
                getConfigConfig(authRealm).getAdminServiceConfig();

        return adminServiceConfig.createJMXConnectorConfig(name,
                                                           address, port, authRealm.getName(), optional);
    }


    protected Container
    getProgenyContainer() {
        return getAdminServiceConfig();
    }

    protected String
    getProgenyJ2EEType() {
        return XTypes.JMX_CONNECTOR_CONFIG;
    }

    final SecurityServiceConfig
    getSecurityServiceConfig() {
        return getConfigConfig().getSecurityServiceConfig();
    }

    final AuthRealmConfig
    createAuthRealmConfig(final String name) {
        removeAuthRealmConfig(name);

        return getSecurityServiceConfig().createAuthRealmConfig(
                name, TEST_REALM_CLASS, null);
    }

    private String
    createAuthRealmName(final String progenyName) {
        return progenyName + ".TestRealm";
    }

    final void
    removeAuthRealmConfig(final String name) {
        try {
            getSecurityServiceConfig().removeAuthRealmConfig(name);
        }
        catch (Exception e) {
        }
    }

    protected void
    removeProgeny(final String name) {
        try {
            getAdminServiceConfig().removeJMXConnectorConfig(name);
        }
        finally {
            try {
                removeAuthRealmConfig(createAuthRealmName(name));
            }
            catch (Exception e) {
            }
        }
    }

    protected final AMXConfig
    createProgeny(
            final String name,
            final Map<String, String> options) {
        final Map<String, String> allOptions = MapUtil.newMap(options, OPTIONAL);

        final int port = (name.hashCode() % 16000) + 33111;

        final String authRealmName = createAuthRealmName(name);
        final AuthRealmConfig authRealmConfig = createAuthRealmConfig(authRealmName);

        try {
            return getAdminServiceConfig().createJMXConnectorConfig(name,
                                                                    ADDRESS, "" + port, authRealmName, allOptions);
        }
        catch (Exception e) {
            removeAuthRealmConfig(authRealmName);
            throw new RuntimeException(e);
        }
    }

    final AdminServiceConfig
    getAdminServiceConfig() {
        return (getConfigConfig().getAdminServiceConfig());
    }

    public void
    testCreateSSL()
            throws Exception {
        if (!checkNotOffline("testCreateSSL")) {
            return;
        }

        final String NAME = "JMXConnectorConfigTest-testCreateSSL";
        try {
            removeEx(NAME);
            final JMXConnectorConfig newConfig =
                    (JMXConnectorConfig) createProgeny(NAME, null);

            final Map<String, JMXConnectorConfig> jmxConnectors =
                    getAdminServiceConfig().getJMXConnectorConfigMap();

            final JMXConnectorConfig jmxConnector = (JMXConnectorConfig)
                    jmxConnectors.get(NAME);
            assert jmxConnector != null;
            assert jmxConnector == newConfig;

            final String CERT_NICKNAME = NAME + "Cert";

            final SSLConfig ssl = jmxConnector.createSSLConfig(CERT_NICKNAME, null);
            assert ssl != null;
            assert ssl.getCertNickname().equals(CERT_NICKNAME);

            jmxConnector.removeSSLConfig();
        }
        finally {
            remove(NAME);
        }
    }
}


