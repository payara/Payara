/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.v3.admin.commands;

import com.sun.enterprise.util.StringUtils;
import com.sun.enterprise.util.i18n.StringManager;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.TreeMap;
import javax.management.MBeanServerConnection;


class SummaryReporter {

    private final MBeanServerConnection mbsc;
    private final StringManager sm = StringManager.getManager(SummaryReporter.class);
    private final static String secretProperty = "module.core.status";

    public SummaryReporter(final MBeanServerConnection mbsc) {
        this.mbsc = mbsc;
    }
    public String getSummaryReport() throws RuntimeException {
        try {
            final StringBuilderNewLineAppender sb = new StringBuilderNewLineAppender(new StringBuilder());
            final OperatingSystemMXBean os = ManagementFactory.newPlatformMXBeanProxy(mbsc,
                    ManagementFactory.OPERATING_SYSTEM_MXBEAN_NAME, OperatingSystemMXBean.class);
            sb.append(getOSInfo(os));
            final RuntimeMXBean rt = ManagementFactory.newPlatformMXBeanProxy(mbsc,
                    ManagementFactory.RUNTIME_MXBEAN_NAME, RuntimeMXBean.class);
            sb.append(getVMInfo(rt));
            return ( sb.toString(secretProperty) );
        } catch(final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String getOSInfo(final OperatingSystemMXBean os) {
        final StringBuilderNewLineAppender sb = new StringBuilderNewLineAppender(new StringBuilder());
        sb.append(sm.getString("os.info"));
        sb.append(sm.getString("os.name", os.getName()));
        sb.append(sm.getString("os.arch", os.getArch(), os.getVersion()));
        sb.append(sm.getString("os.nproc", os.getAvailableProcessors()));
        sb.append(sm.getString("os.load", getSystemLoad(os)));
        return ( sb.toString() );
    }
    private String getVMInfo(final RuntimeMXBean rt) {
        final StringBuilderNewLineAppender sb = new StringBuilderNewLineAppender(new StringBuilder());
        sb.append(sm.getString("rt.info", rt.getName()));
        sb.append(sm.getString("rt.bcp", rt.getBootClassPath()));
        sb.append(sm.getString("rt.cp", rt.getClassPath()));
        sb.append(sm.getString("rt.libpath", rt.getLibraryPath()));
        sb.append(sm.getString("rt.nvv", rt.getVmName(), rt.getVmVendor(), rt.getVmVersion()));
        sb.append(getProperties(rt));
        return ( sb.toString() );
    }
    private String getProperties(final RuntimeMXBean rt) {
        final StringBuilderNewLineAppender sb = new StringBuilderNewLineAppender(new StringBuilder());
        final Map<String, String> unsorted = rt.getSystemProperties();
        // I decided to sort this for better readability -- 27 Feb 2006
        final TreeMap<String, String> props = new TreeMap<String, String>(unsorted);
        sb.append(sm.getString("rt.sysprops"));
        for (Map.Entry<String, String> entry : props.entrySet()) {
            sb.append(entry.getKey()).append(" = ").append(filterForbidden(entry.getKey(), entry.getValue()));
        }
        return ( sb.toString() );
    }

    private String getSystemLoad(OperatingSystemMXBean os) {
        //available only on 1.6
        String info = ThreadMonitor.NA;
        try {
            String METHOD = "getSystemLoadAverage";
            Method m = os.getClass().getMethod(METHOD, (Class[]) null);
            if (m != null) {
                Object ret = m.invoke(os, (Object[])null);
                return ( ret.toString() );
            }
        } catch(Exception e) {

        }
        return ( info );
    }

    private String filterForbidden(String key, String value) {
        if(StringUtils.ok(key) && key.startsWith("javax.net.ssl.") && key.indexOf("password") >= 0)
            return "********";
        else
            return value;
    }
}
