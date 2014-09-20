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

package org.glassfish.admin.amxtest;

import com.sun.appserv.management.base.AMX;
import com.sun.appserv.management.config.ConfigConfig;
import com.sun.appserv.management.config.JavaConfig;
import com.sun.appserv.management.util.misc.GSetUtil;
import com.sun.appserv.management.base.AMXDebugSupportMBean;
import org.glassfish.admin.amx.util.AMXDebugStuff;

import java.util.Map;
import java.util.Set;


/**
 This test run prior to testing any AMX MBeans.
 */
public final class RunMeFirstTest
        extends AMXTestBase {
    public RunMeFirstTest() {
        initCoverageInfos();
    }

    private void
    initCoverageInfos() {
        final Set<AMX> all = getAllAMX();

        // set the AMX-DEBUG flags on
        final String AMX_DEBUG = "-DAMX-DEBUG.enabled=true";
        final String AMX_DEBUG2 = "-DAMX-DEBUG=true";

        // set AMX-DEBUG.enabled=true in all ConfigConfig JVM options
        final Map<String, ConfigConfig> configs = getDomainConfig().getConfigsConfig().getConfigConfigMap();
        for (final ConfigConfig config : configs.values()) {
            final JavaConfig jc = config.getJavaConfig();
            final String[] opt = jc.getJVMOptions();
            final Set<String> jvmOptions = GSetUtil.newStringSet(opt == null ? new String[0] : opt );

            if (!(jvmOptions.contains(AMX_DEBUG) || jvmOptions.contains(AMX_DEBUG2))) {
                jvmOptions.add(AMX_DEBUG);
                jc.setJVMOptions(GSetUtil.toStringArray(jvmOptions));

                // don't warn for default-config; it's not used by a running server
                if (!config.getName().equals("default-config")) {
                    warning("Enabled AMX-DEBUG for config " + config.getName() +
                            " (restart required)");
                }
            }
        }

        // setup default stuff
        final AMXDebugSupportMBean debug = getAMXDebugSupportMBean();
        debug.setAll(true);
        debug.setDefaultDebug(true);
        debug.getOutputIDs();

        for (final AMX amx : all) {
            final AMXDebugStuff debugStuff = getTestUtil().asAMXDebugStuff(amx);

            if (debugStuff == null) {
                continue;
            }

            try {
                debugStuff.enableAMXDebug(true);
            }
            catch (Throwable t) {
                warning("Couldn't enableAMXDebug() for " + amx.getJ2EEType());
            }

            try {
                debugStuff.enableCoverageInfo(true);
                debugStuff.clearCoverageInfo();
            }
            catch (Throwable t) {
                warning("Couldn't enableCoverageInfo for " + amx.getJ2EEType());
            }
        }
    }
}














