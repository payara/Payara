/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2010 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.util.i18n.StringManager;
import java.lang.management.ClassLoadingMXBean;
import java.lang.management.CompilationMXBean;
import java.lang.management.ManagementFactory;
import javax.management.MBeanServerConnection;

/**
 */
class ClassReporter {

    private final MBeanServerConnection mbsc;
    private final StringManager sm = StringManager.getManager(ClassReporter.class);
    public ClassReporter(final MBeanServerConnection mbsc) {
        this.mbsc = mbsc;
    }
    public String getClassReport() throws RuntimeException {
        try {
            final StringBuilderNewLineAppender sb = new StringBuilderNewLineAppender(new StringBuilder());
            final ClassLoadingMXBean clmb = ManagementFactory.newPlatformMXBeanProxy(mbsc, 
                    ManagementFactory.CLASS_LOADING_MXBEAN_NAME, ClassLoadingMXBean.class);
            sb.append(sm.getString("classloading.info"));
            sb.append(sm.getString("classes.loaded", clmb.getLoadedClassCount()));
            sb.append(sm.getString("classes.total", clmb.getTotalLoadedClassCount()));
            sb.append(sm.getString("classes.unloaded", clmb.getUnloadedClassCount()));
            
            final CompilationMXBean cmb = ManagementFactory.newPlatformMXBeanProxy(mbsc, 
                    ManagementFactory.COMPILATION_MXBEAN_NAME, CompilationMXBean.class);
            sb.append(sm.getString("complilation.info"));
            sb.append(sm.getString("compilation.monitor.status", cmb.isCompilationTimeMonitoringSupported()));
            sb.append(sm.getString("jit.compilar.name", cmb.getName()));
            sb.append(sm.getString("compilation.time", JVMInformationCollector.millis2HoursMinutesSeconds(cmb.getTotalCompilationTime())));
            return ( sb.toString() );
        } catch(final Exception e) {
            throw new RuntimeException(e);
        }
    }
}
