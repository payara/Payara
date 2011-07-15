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
import com.sun.appserv.management.config.HTTPAccessLogConfig;
import com.sun.appserv.management.config.HTTPServiceConfig;
import com.sun.appserv.management.config.VirtualServerConfig;
import com.sun.appserv.management.config.VirtualServerConfigKeys;
import com.sun.appserv.management.util.misc.MapUtil;

import java.util.HashMap;
import java.util.Map;

/**
 */
public final class VirtualServerConfigTest
        extends ConfigMgrTestBase {
    static final String HOSTS = "localhost";

    public VirtualServerConfigTest() {
    }

    protected Container
    getProgenyContainer() {
        return getHTTPService();
    }

    protected String
    getProgenyJ2EEType() {
        return XTypes.VIRTUAL_SERVER_CONFIG;
    }


    protected void
    removeProgeny(final String name) {
        getHTTPService().removeVirtualServerConfig(name);
    }

    protected final VirtualServerConfig
    create(String name) {
        return (VirtualServerConfig) createProgeny(name, null);
    }

    private Map<String, String>
    getOptional() {
        final Map<String, String> m = new HashMap<String, String>();
        m.put(VirtualServerConfigKeys.STATE_KEY, VirtualServerConfigKeys.STATE_DISABLED);
        m.put(VirtualServerConfigKeys.DOC_ROOT_PROPERTY_KEY, "/");
        m.put(VirtualServerConfigKeys.ACCESS_LOG_PROPERTY_KEY, "/");

        return m;
    }

    protected final AMXConfig
    createProgeny(
            final String name,
            final Map<String, String> extra) {
        final Map<String, String> allOptions = MapUtil.newMap(extra, getOptional());

        return (getHTTPService().createVirtualServerConfig(name, "localhost", allOptions));
    }

    protected final HTTPServiceConfig
    getHTTPService() {
        return getConfigConfig().getHTTPServiceConfig();
    }

    public void
    testCreateHTTPAccessLog()
            throws Exception {
        if (!checkNotOffline("testCreateRemove")) {
            return;
        }

        final String NAME = "VirtualServerConfigMgrTest-testCreateHTTPAccessLog";
        try {
            removeEx(NAME);
            final VirtualServerConfig newVS =
                    (VirtualServerConfig) createProgeny(NAME, null);
            assert newVS != null;
            //trace( "newVS.getState: " + newVS.getState() );
            // assert newVS.getState().equals("disabled");

            assert (newVS.getHTTPAccessLogConfig() == null);

            final HTTPAccessLogConfig accessLog =
                    newVS.createHTTPAccessLogConfig("false", "${com.sun.aas.instanceRoot}/logs/access", null);
            assert (accessLog != null);
            assert (Util.getObjectName(accessLog).equals(Util.getObjectName(newVS.getHTTPAccessLogConfig())));

            newVS.removeHTTPAccessLogConfig();
        }
        finally {
            remove(NAME);
        }
    }
}


