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
import com.sun.appserv.management.config.ConfigConfig;
import com.sun.appserv.management.config.ThreadPoolConfig;
import com.sun.appserv.management.config.ThreadPoolConfigKeys;
import com.sun.appserv.management.util.misc.MapUtil;

import java.util.HashMap;
import java.util.Map;

/**
 */
public final class ThreadPoolConfigTest
        extends ConfigMgrTestBase {
    static final Map<String, String> OPTIONAL = new HashMap<String, String>();

    static {
        OPTIONAL.put(ThreadPoolConfigKeys.MIN_THREAD_POOL_SIZE_KEY, "10");
        OPTIONAL.put(ThreadPoolConfigKeys.MAX_THREAD_POOL_SIZE_KEY, "100");
        OPTIONAL.put(ThreadPoolConfigKeys.IDLE_THREAD_TIMEOUT_IN_SECONDS_KEY, "120");
        OPTIONAL.put(ThreadPoolConfigKeys.NUM_WORK_QUEUES_KEY, "10");
    }

    public ThreadPoolConfigTest() {
        if (checkNotOffline("ensureDefaultInstance")) {
            ensureDefaultInstance(getConfigConfig());
        }
    }

    public static String
    getDefaultInstanceName() {
        return getDefaultInstanceName("ThreadPoolConfig");
    }

    public static ThreadPoolConfig
    ensureDefaultInstance(final ConfigConfig cc) {
        ThreadPoolConfig result = cc.getThreadPoolsConfig().getThreadPoolConfigMap().get(getDefaultInstanceName());

        if (result == null) {
            result = createInstance(cc, getDefaultInstanceName(), OPTIONAL);
        }

        return result;
    }

    public static ThreadPoolConfig
    createInstance(
            final ConfigConfig cc,
            final String name,
            Map<String, String> optional) {
        return cc.getThreadPoolsConfig().createThreadPoolConfig(name, optional);
    }

    protected Container
    getProgenyContainer() {
        return getConfigConfig();
    }

    protected String
    getProgenyJ2EEType() {
        return XTypes.THREAD_POOL_CONFIG;
    }


    protected void
    removeProgeny(final String name) {
        getConfigConfig().getThreadPoolsConfig().removeThreadPoolConfig(name);
    }

    protected final AMXConfig
    createProgeny(
            final String name,
            final Map<String, String> options) {
        final Map<String, String> allOptions = MapUtil.newMap(options, OPTIONAL);

        return getConfigConfig().getThreadPoolsConfig().createThreadPoolConfig(name, allOptions);
    }
}


