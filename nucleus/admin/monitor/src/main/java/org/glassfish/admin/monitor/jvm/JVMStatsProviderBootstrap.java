/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.monitor.jvm;

import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.ArrayList;
import java.util.List;
import org.jvnet.hk2.annotations.Service;
import org.glassfish.api.monitoring.ContainerMonitoring;
import org.glassfish.external.probe.provider.PluginPoint;
import org.glassfish.external.probe.provider.StatsProviderManager;
import org.glassfish.hk2.api.PostConstruct;
import org.glassfish.hk2.runlevel.RunLevel;
import org.glassfish.internal.api.*;

/**
 *
 * @author PRASHANTH ABBAGANI
 */
@Service
@RunLevel(value=PostStartupRunLevel.VAL, mode=RunLevel.RUNLEVEL_MODE_NON_VALIDATING)
public class JVMStatsProviderBootstrap implements PostConstruct {

    private ServerRuntimeStatsProvider sRuntimeStatsProvider = new ServerRuntimeStatsProvider();
    private JVMClassLoadingStatsProvider clStatsProvider = new JVMClassLoadingStatsProvider();
    private JVMCompilationStatsProvider compileStatsProvider = new JVMCompilationStatsProvider();
    private JVMMemoryStatsProvider memoryStatsProvider = new JVMMemoryStatsProvider();
    private JVMOSStatsProvider osStatsProvider = new JVMOSStatsProvider();
    private JVMRuntimeStatsProvider runtimeStatsProvider = new JVMRuntimeStatsProvider();
    private JVMThreadSystemStatsProvider threadSysStatsProvider = new JVMThreadSystemStatsProvider();
    private List<JVMGCStatsProvider> jvmStatsProviderList = new ArrayList();
    private ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
    public static final String JVM = "jvm";

    public void postConstruct(){

        /* register with monitoring */
        StatsProviderManager.register("jvm", PluginPoint.SERVER, "", sRuntimeStatsProvider, ContainerMonitoring.LEVEL_LOW);
        StatsProviderManager.register("jvm", PluginPoint.SERVER, "jvm/class-loading-system", clStatsProvider, ContainerMonitoring.LEVEL_LOW);
        StatsProviderManager.register("jvm", PluginPoint.SERVER, "jvm/compilation-system", compileStatsProvider, ContainerMonitoring.LEVEL_LOW);
        for (GarbageCollectorMXBean gc : ManagementFactory.getGarbageCollectorMXBeans()) {
            JVMGCStatsProvider jvmStatsProvider = new JVMGCStatsProvider(gc.getName());
            jvmStatsProviderList.add(jvmStatsProvider);
            StatsProviderManager.register("jvm", PluginPoint.SERVER, "jvm/garbage-collectors/"+gc.getName(), jvmStatsProvider, ContainerMonitoring.LEVEL_LOW);
        }
        StatsProviderManager.register("jvm", PluginPoint.SERVER, "jvm/memory", memoryStatsProvider, ContainerMonitoring.LEVEL_LOW);
        StatsProviderManager.register("jvm", PluginPoint.SERVER, "jvm/operating-system", osStatsProvider, ContainerMonitoring.LEVEL_LOW);
        StatsProviderManager.register("jvm", PluginPoint.SERVER, "jvm/runtime", runtimeStatsProvider, ContainerMonitoring.LEVEL_LOW);
        StatsProviderManager.register("jvm", PluginPoint.SERVER, "jvm/thread-system", threadSysStatsProvider, ContainerMonitoring.LEVEL_LOW);
        for (ThreadInfo t : threadBean.getThreadInfo(threadBean.getAllThreadIds(), 5)) {
            if (t == null) continue; // See issue #12636
            JVMThreadInfoStatsProvider threadInfoStatsProvider = new JVMThreadInfoStatsProvider(t);
            StatsProviderManager.register("jvm", PluginPoint.SERVER, 
                    "jvm/thread-system/thread-"+t.getThreadId(), threadInfoStatsProvider, ContainerMonitoring.LEVEL_HIGH);
        }
    } 
}
