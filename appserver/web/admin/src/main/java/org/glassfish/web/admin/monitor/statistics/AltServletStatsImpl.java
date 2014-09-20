/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.web.admin.monitor.statistics;

import org.glassfish.web.admin.monitor.HttpServiceStatsProviderBootstrap;
import org.jvnet.hk2.annotations.Service;

import org.glassfish.hk2.api.PerLookup;
import javax.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.ActionReport.ExitCode;
import org.glassfish.external.statistics.CountStatistic; 
import org.glassfish.external.statistics.RangeStatistic; 
import org.glassfish.admin.monitor.cli.MonitorContract;
import org.glassfish.flashlight.datatree.TreeNode;
import org.glassfish.flashlight.MonitoringRuntimeDataRegistry;
import java.util.List;
import java.util.ResourceBundle;

@Service
@PerLookup
public class AltServletStatsImpl implements MonitorContract {

    private static final ResourceBundle rb = HttpServiceStatsProviderBootstrap.rb;

    @Inject
    private MonitoringRuntimeDataRegistry mrdr;

    private final static String name = "servlet";

    private final static String displayFormat = "%1$-10s %2$-10s %3$-10s";

    public String getName() {
        return name;
    }

    public ActionReport process(final ActionReport report, final String filter) {

        if (mrdr == null) {
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(rb.getString(HTTPListenerStatsImpl.MRDR_NULL));
            return report;
        }

        TreeNode serverNode = mrdr.get("server");
        if (serverNode == null) {
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(rb.getString(HTTPListenerStatsImpl.MRDR_NULL));
            return report;
        }

        String [] patternArr = new String [] {"server.web.servlet.*"};
        
	long activeServletsLoadedCount = 0; 
	long maxServletsLoadedCount = 0; 
	long totalServletsLoadedCount = 0;

        for (String pattern : patternArr) {
            List<TreeNode> tnL = serverNode.getNodes(pattern);
            for (TreeNode tn : tnL) {
                if (tn.hasChildNodes()) {
                    continue;
                }
                if ("activeservletsloadedcount".equals(tn.getName())) { 
                    activeServletsLoadedCount = getRangeStatisticValue(tn.getValue());
                } else if ("maxservletsloadedcount".equals(tn.getName())) { 
                    maxServletsLoadedCount = getCountStatisticValue(tn.getValue());
                } else if ("totalservletsloadedcount".equals(tn.getName())) { 
                    totalServletsLoadedCount = getCountStatisticValue(tn.getValue());
                }
            }
        }

        report.setMessage(String.format(displayFormat, 
                activeServletsLoadedCount, maxServletsLoadedCount,
                totalServletsLoadedCount));

        report.setActionExitCode(ExitCode.SUCCESS);
        return report;
    }

    private long getCountStatisticValue(Object obj) {
        long l = 0L;
        if (obj == null) return l;
        if (obj instanceof CountStatistic) {
            return ((CountStatistic)obj).getCount();
        }
        return l;
    }

    private long getRangeStatisticValue(Object obj) {
        long l = 0L;
        if (obj == null) return l;
        if (obj instanceof RangeStatistic) {
            return ((RangeStatistic)obj).getCurrent();
        }
        return l;
    }
}
