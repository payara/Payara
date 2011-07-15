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

package org.glassfish.web.admin.monitor.statistics;

import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.component.PerLookup;
import org.jvnet.hk2.annotations.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.ActionReport.ExitCode;
import org.glassfish.external.statistics.Statistic;
import org.glassfish.external.statistics.CountStatistic; 
import org.glassfish.external.statistics.RangeStatistic;
import org.glassfish.external.statistics.TimeStatistic;
import org.glassfish.external.statistics.Stats;
import org.glassfish.admin.monitor.cli.MonitorContract;
import org.glassfish.flashlight.datatree.TreeNode;
import org.glassfish.flashlight.MonitoringRuntimeDataRegistry;
import java.util.List;
import java.util.logging.Logger;
import java.util.logging.Level;
import com.sun.enterprise.util.LocalStringManagerImpl;

/** 
 * A Stats interface to represent the statistical data exposed by an
 * HTTP Listener. 
 *
 * For v3 Prelude, following stats will be available
 * errorCount, maxTime, processingTime, and requestCount
 *
 */
@Service
@Scoped(PerLookup.class)
public class HTTPListenerStatsImpl implements MonitorContract {

    @Inject
    private MonitoringRuntimeDataRegistry mrdr;

    @Inject
    private Logger logger;

    private static final LocalStringManagerImpl localStrings = 
        new LocalStringManagerImpl(HTTPListenerStatsImpl.class);

    private final String name = "httplistener";
    private final String displayFormat = "%1$-4s %2$-4s %3$-6.2f %4$-4s";

    public String getName() {
        return name;
    }

    public ActionReport process(final ActionReport report, final String filter) {

        if (logger.isLoggable(Level.FINEST)) {
            logger.finest("HTTPListenerStatsImpl: process ...");
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

        long errorCount = 0;
        long maxTime = 0;
        double processingTime = 0;
        long requestCount = 0;

        List<TreeNode> tnL = serverNode.getNodes("server.web.request.*");
        for (TreeNode tn : tnL) {
            if (tn.hasChildNodes()) {
                continue;
            }

            if ("errorcount".equals(tn.getName())) {
                errorCount = getCountStatisticValue(tn.getValue());
            } else if ("maxtime".equals(tn.getName())) {
                maxTime = getCountStatisticValue(tn.getValue());
            } else if ("processingtime".equals(tn.getName())) {
                processingTime = getCountStatisticValue(tn.getValue());
            } else if ("requestcount".equals(tn.getName())) {
                requestCount = getCountStatisticValue(tn.getValue());
            }
        }

        //report.setMessage(String.format(displayFormat, "ec", "mt", "pt", "rc"));
        report.setMessage(String.format(displayFormat, 
            errorCount, maxTime, processingTime, requestCount));
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

    /**
     * Cumulative value of the bytesReceived by each of the
     * RequestProcessors
     * @return CountStatistic
     */
    public CountStatistic getBytesReceived() {
        return null;
    }

    /**
     * Cumulative value of the bytesSent by each of the
     * RequestProcessors
     * @return CountStatistic
     */
    public CountStatistic getBytesSent() {
        return null;
    }
    
    /**
     * Cumulative value of the errorCount of each of the
     * RequestProcessors. The errorCount represents the number of
     * cases where the response code was >= 400
     * @return CountStatistic
     */
    public CountStatistic getErrorCount() {
        return null;
    }
    
    /**
     * @return CountStatistic
     */
    public CountStatistic getCount200() {
        return null;
    }
    public CountStatistic getCount2xx() {
        return null;
    }
    public CountStatistic getCount302() {
        return null;
    }
    public CountStatistic getCount304() {
        return null;
    }
    public CountStatistic getCount3xx() {
        return null;
    }
    public CountStatistic getCount400() {
        return null;
    }
    public CountStatistic getCount401() {
        return null;
    }
    public CountStatistic getCount403() {
        return null;
    }
    public CountStatistic getCount404() {
        return null;
    }
    public CountStatistic getCount4xx() {
        return null;
    }
    public CountStatistic getCount503() {
        return null;
    }
    public CountStatistic getCount5xx() {
        return null;
    }
    public CountStatistic getCountOther() {
        return null;
    }
    
    public CountStatistic getCountOpenConnections() {
        return null;
    }

    public CountStatistic getMaxOpenConnections() {
        return null;
    }
    
    
    /**
     * The longest response time for a request. This is not a
     * cumulative value, but is the maximum of the response times
     * for each of the RequestProcessors.
     * @return CountStatistic
     */
    public CountStatistic getMaxTime() {
        return null;
    }
    
    /**
     * Cumulative value of the processing times of each of the
     * RequestProcessors. The processing time of a RequestProcessor
     * is the average of request processing times over the request
     * count.
     * @return CountStatistic
     */
    public CountStatistic getProcessingTime() {
        return null;
    }
    
    /**
     * Cumulative number of the requests processed so far, 
     * by the RequestProcessors.
     * @return CountStatistic
     */
    public CountStatistic getRequestCount() {
        return null;
    }
    
    
    //ThreadPool statistics for the listener
    
    /**
     * The number of request processing threads currently in the
     * thread pool
     * @return CountStatistic
     */
    public CountStatistic getCurrentThreadCount() {
        return null;
    }
    
    /**
     * The number of request processing threads currently in the
     * thread pool, serving requests.
     * @return CountStatistic
     */
    public CountStatistic getCurrentThreadsBusy() {
        return null;
    }
    
    /**
     * The maximum number of request processing threads that are
     * created by the listener. It determines the maximum number of
     * simultaneous requests that can be handled
     * @return CountStatistic
     */
    public CountStatistic getMaxThreads() {
        return null;
    }
    
    /** 
     * The maximum number of unused request processing threads that will
     * be allowed to exist until the thread pool starts stopping the 
     * unnecessary threads.
     * @return CountStatistic
     */
    public CountStatistic getMaxSpareThreads() {
        return null;
    }

    /**
     * The number of request processing threads that will be created 
     * when this listener is first started.
     * @return CountStatistic
     */
    public CountStatistic getMinSpareThreads() {
        return null;
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
