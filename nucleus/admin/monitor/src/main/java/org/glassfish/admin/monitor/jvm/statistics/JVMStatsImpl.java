/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.monitor.jvm.statistics;

import java.util.*;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.annotations.Service;

import org.glassfish.hk2.api.PerLookup;
import org.glassfish.api.ActionReport;
import org.glassfish.api.monitoring.ContainerMonitoring;
import org.glassfish.api.ActionReport.ExitCode;
import org.glassfish.admin.monitor.cli.MonitorContract;
import org.glassfish.flashlight.MonitoringRuntimeDataRegistry;
import org.glassfish.flashlight.datatree.TreeNode;
import java.lang.management.MemoryUsage;
import org.glassfish.flashlight.datatree.MethodInvoker;
import java.util.logging.Logger;
import java.util.logging.Level;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.config.serverbeans.MonitoringService;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.external.statistics.Statistic;
import org.glassfish.external.statistics.CountStatistic;

import javax.inject.Inject;
import javax.inject.Named;

/**
 *
 * For v3 Prelude, following stats will be available
 * server.jvm.committedHeapSize java.lang.management.MemoryUsage
 * init, used, committed, max
 *
 */
//public class JVMStatsImpl implements JVMStats, MonitorContract {
@Service
@PerLookup
public class JVMStatsImpl implements MonitorContract {

    @Inject
    private MonitoringRuntimeDataRegistry mrdr;


    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    @Optional
    MonitoringService monitoringService = null;

    private final LocalStringManagerImpl localStrings =
             new LocalStringManagerImpl(JVMStatsImpl.class);

    private final String name = "jvm";

    public String getName() {
        return name;
    }

    public ActionReport process(final ActionReport report, final String filter) {

        if (monitoringService != null) {
            String level = monitoringService.getMonitoringLevel("jvm");
            if ((level != null) && (level.equals(ContainerMonitoring.LEVEL_OFF))) {
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage(localStrings.getLocalString("level.off",
                                "Monitoring level for jvm is off"));
                return report;
            }
        }

        if (mrdr == null) {
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(localStrings.getLocalString("mrdr.null",
                            "MonitoringRuntimeDataRegistry is null"));
            return report;
        }

        TreeNode serverNode = mrdr.get("server");
        if (serverNode == null) {
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(localStrings.getLocalString("mrdr.null",
                            "MonitoringRuntimeDataRegistry server node is null"));
            return report;
        }

        if ((filter != null) && (filter.length() > 0)) {
            if ("heapmemory".equals(filter)) {
                return (heapMemory(report, serverNode));
            } else if ("nonheapmemory".equals(filter)) {
                return (nonHeapMemory(report, serverNode));
            }
        } else {
            return (v2JVM(report, serverNode));
        }

        return null;
    }

    private ActionReport heapMemory(final ActionReport report, TreeNode serverNode) {
        long init = getFirstTreeNodeAsLong(serverNode, "server.jvm.memory.initheapsize-count");
        long used = getFirstTreeNodeAsLong(serverNode, "server.jvm.memory.usedheapsize-count");
        long committed = getFirstTreeNodeAsLong(serverNode, "server.jvm.memory.committedheapsize-count");
        long max = getFirstTreeNodeAsLong(serverNode, "server.jvm.memory.maxheapsize-count");
        String displayFormat = "%1$-10s %2$-10s %3$-10s %4$-10s";
        report.setMessage(String.format(displayFormat, init, used, committed, max));
        report.setActionExitCode(ExitCode.SUCCESS);
        return report;
    }

    private ActionReport nonHeapMemory(final ActionReport report, TreeNode serverNode) {
        long init = getFirstTreeNodeAsLong(serverNode, "server.jvm.memory.initnonheapsize-count");
        long used = getFirstTreeNodeAsLong(serverNode, "server.jvm.memory.usednonheapsize-count");
        long committed = getFirstTreeNodeAsLong(serverNode, "server.jvm.memory.committednonheapsize-count");
        long max = getFirstTreeNodeAsLong(serverNode, "server.jvm.memory.maxnonheapsize-count");
        String displayFormat = "%1$-10s %2$-10s %3$-10s %4$-10s";
        report.setMessage(String.format(displayFormat, init, used, committed, max));
        report.setActionExitCode(ExitCode.SUCCESS);
        return report;
    }

    // @author bnevins
    private long getFirstTreeNodeAsLong(TreeNode parent, String name) {

        List<TreeNode> nodes = parent.getNodes(name);

        if(!nodes.isEmpty()) {
            TreeNode node = nodes.get(0);
            Object val = node.getValue();
            if(val != null) {
                try {
                    CountStatistic cs = (CountStatistic)val;
                    return cs.getCount();
                } catch (Exception e) {
                    //TODO: handle exception
                }
            }
        }

        return 0L;
    }

    // @author bnevins
    private ActionReport v2JVM(final ActionReport report, TreeNode serverNode) {
        long uptime = getFirstTreeNodeAsLong(serverNode,    "server.jvm.runtime.uptime-count");
        long min = getFirstTreeNodeAsLong(serverNode,       "server.jvm.memory.initnonheapsize-count");
        min += getFirstTreeNodeAsLong(serverNode,           "server.jvm.memory.initheapsize-count");
        long max = getFirstTreeNodeAsLong(serverNode,       "server.jvm.memory.maxheapsize-count");
        max += getFirstTreeNodeAsLong(serverNode,           "server.jvm.memory.maxnonheapsize-count");
        long count = getFirstTreeNodeAsLong(serverNode,     "server.jvm.memory.committedheapsize-count");
        count += getFirstTreeNodeAsLong(serverNode,         "server.jvm.memory.committednonheapsize-count");

        String displayFormat = "%1$-25s %2$-10s %3$-10s %4$-10s %5$-10s %6$-10s";
        report.setMessage(
            String.format(displayFormat, uptime, min, max, 0, 0, count));

        report.setActionExitCode(ExitCode.SUCCESS);
        return report;
    }

    public Statistic[] getStatistics() {
    	return null;
    }

    public String[] getStatisticNames() {
    	return null;
    }

    public Statistic getStatistic(String statisticName) {
    	return null;
    }
}
