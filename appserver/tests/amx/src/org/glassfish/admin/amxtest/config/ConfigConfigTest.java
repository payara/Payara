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

import com.sun.appserv.management.DomainRoot;
import com.sun.appserv.management.base.SystemInfo;
import com.sun.appserv.management.base.Util;
import com.sun.appserv.management.config.ConfigConfig;
import com.sun.appserv.management.config.DomainConfig;
import com.sun.appserv.management.util.misc.StringUtil;
import org.glassfish.admin.amxtest.AMXTestBase;
import org.glassfish.admin.amxtest.MultipleServerSupportRequired;

import java.util.Map;


/**
 */
public final class ConfigConfigTest
        extends AMXTestBase
        implements MultipleServerSupportRequired {
    public ConfigConfigTest() {
        if (checkNotOffline("ensureDefaultInstance")) {
            ensureDefaultInstance(getDomainRoot());
        }

    }

    public static ConfigConfig
    ensureDefaultInstance(final DomainRoot domainRoot) {
        ConfigConfig config = null;

        final DomainConfig domainConfig = domainRoot.getDomainConfig();
        final Map<String, ConfigConfig> existing = domainConfig.getConfigsConfig().getConfigConfigMap();

        if (supportsMultipleServers(domainRoot)) {
            config = existing.get(getDefaultInstanceName(domainRoot));
            if (config == null) {
                final ConfigSetup setup = new ConfigSetup(domainRoot);
                config = setup.createConfig(getDefaultInstanceName(domainRoot));
            }
        } else {
            config = existing.get(PE_CONFIG_NAME);
            assert (config != null) : "No config named " + StringUtil.quote(PE_CONFIG_NAME);
        }
        return config;
    }

    public static String
    getDefaultInstanceName(final DomainRoot domainRoot) {
        String name = null;

        if (domainRoot.getSystemInfo().supportsFeature(SystemInfo.MULTIPLE_SERVERS_FEATURE)) {
            name = getDefaultInstanceName("ConfigConfigTest");
        } else {
            name = PE_CONFIG_NAME;
        }
        return name;
    }


    private ConfigConfig
    create(final String name)
            throws Throwable {
        final ConfigSetup setup = new ConfigSetup(getDomainRoot());

        setup.removeConfig(name);

        final ConfigConfig config = setup.createConfig(name);
        assert (name.equals(config.getName()));

        // see that it responds to a request
        final Map<String, Object> attrs = Util.getExtra(config).getAllAttributes();
        //printVerbose( "Attributes for config " + config.getName() + ":" );
        //printVerbose( MapUtil.toString( attrs, NEWLINE ) );

        return config;
    }

    public void
    testCreateRemove()
            throws Throwable {
        if (!checkNotOffline("testCreateRemove")) {
            return;
        }

        final String NAME = "ConfigConfigTest.testCreateRemove";

        final Map<String, ConfigConfig> before = getDomainConfig().getConfigsConfig().getConfigConfigMap();

        final int NUM = 2;
        final ConfigConfig[] configs = new ConfigConfig[NUM];

        for (int i = 0; i < NUM; ++i) {
            configs[i] = create(NAME + i);
        }

        final ConfigSetup setup = new ConfigSetup(getDomainRoot());
        for (final ConfigConfig config : configs) {
            setup.removeConfig(config.getName());

            // verify that the config is gone
            try {
                Util.getExtra(config).getAllAttributes();
                fail("Config " + config.getName() + " should no longer exist");
            }
            catch (Exception e) {
                // good, we expected to be here
            }
        }

        final Map<String, ConfigConfig> after = getDomainConfig().getConfigsConfig().getConfigConfigMap();
        assert (before.keySet().equals(after.keySet()));
    }
}



























