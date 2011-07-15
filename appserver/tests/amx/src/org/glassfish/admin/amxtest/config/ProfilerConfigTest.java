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

import com.sun.appserv.management.base.Util;
import com.sun.appserv.management.config.JavaConfig;
import com.sun.appserv.management.config.ProfilerConfig;
import com.sun.appserv.management.config.ProfilerConfigKeys;
import com.sun.appserv.management.util.jmx.JMXUtil;
import org.glassfish.admin.amxtest.AMXTestBase;

import java.util.HashMap;
import java.util.Map;

/**
 */
public final class ProfilerConfigTest
        extends AMXTestBase {
    private static final String NATIVE_LIBRARY_PATH = "a/b/c";
    private static final String CLASSPATH = "/foo/bar";

    private static Map<String, String>
    getOptional() {
        final Map<String, String> optional = new HashMap<String, String>();
        optional.put(ProfilerConfigKeys.NATIVE_LIBRARY_PATH_KEY, NATIVE_LIBRARY_PATH);
        optional.put(ProfilerConfigKeys.CLASSPATH_KEY, CLASSPATH);
        optional.put(ProfilerConfigKeys.ENABLED_KEY, "false");
        return optional;
    }

    public ProfilerConfigTest() {
        if (checkNotOffline("testIllegalCreate")) {
            ensureDefaultInstance(getConfigConfig().getJavaConfig());
        }
    }

    public static ProfilerConfig
    ensureDefaultInstance(final JavaConfig javaConfig) {
        ProfilerConfig prof = javaConfig.getProfilerConfig();
        if (prof == null) {
            final String NAME = "profiler";

            prof = javaConfig.createProfilerConfig(NAME, getOptional());
            assert prof != null;
        }

        return prof;
    }

    private void
    testGetters(final ProfilerConfig prof) {
        assert (prof.getClasspath() != null);
        prof.setClasspath(prof.getClasspath());

        assert (prof.getNativeLibraryPath() != null);
        prof.setNativeLibraryPath(prof.getNativeLibraryPath());

        assert (prof.getJVMOptions() != null);
        prof.setJVMOptions(prof.getJVMOptions());

        prof.setEnabled(prof.getEnabled());
    }

    public synchronized void
    testCreateRemoveProfiler()
            throws Exception {
        if (checkNotOffline("testIllegalCreate")) {
            ensureDefaultInstance(getConfigConfig().getJavaConfig());

            final JavaConfig javaConfig = getConfigConfig().getJavaConfig();

            javaConfig.removeProfilerConfig();
            assert javaConfig.getProfilerConfig() == null :
                    "Can't remove ProfilerConfig from " +
                            JMXUtil.toString(Util.getObjectName(javaConfig));

            ensureDefaultInstance(javaConfig);
            assert javaConfig.getProfilerConfig() != null;
            Util.getExtra(javaConfig.getProfilerConfig()).getMBeanInfo();

            testGetters(javaConfig.getProfilerConfig());

            javaConfig.removeProfilerConfig();
            ensureDefaultInstance(javaConfig);
            assert javaConfig.getProfilerConfig() != null;
            Util.getExtra(javaConfig.getProfilerConfig()).getMBeanInfo();
            testGetters(javaConfig.getProfilerConfig());
        }
    }
}


