/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.web.admin.monitor;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.external.statistics.CountStatistic;
import org.glassfish.external.statistics.annotations.Reset;
import org.glassfish.external.statistics.impl.CountStatisticImpl;
import org.glassfish.external.statistics.impl.StatisticImpl;
import org.glassfish.external.statistics.impl.TimeStatisticImpl;
import org.glassfish.external.probe.provider.annotations.*;
import org.glassfish.gmbal.AMXMetadata;
import org.glassfish.gmbal.Description;
import org.glassfish.gmbal.ManagedAttribute;
import org.glassfish.gmbal.ManagedObject;

/**
 * Provides the monitoring data at the Web container level
 *
 * @author Shing Wai Chan
 */
@AMXMetadata(type="servlet-instance-mon", group="monitoring")
@ManagedObject
@Description("Web Container Servlet Instance Statistics")
public class ServletInstanceStatsProvider {

    private static final Logger logger = HttpServiceStatsProviderBootstrap.logger;

    private static final String ERROR_COUNT_DESCRIPTION = "Number of error responses (that is, responses with a status code greater than or equal to 400)";

    private static final String MAX_TIME_DESCRIPTION = "Maximum response time";

    private static final String SERVICE_TIME_DESCRIPTION = "Aggregate response time";

    private static final String PROCESSING_TIME_DESCRIPTION = "Average response time";

    private static final String REQUEST_COUNT_DESCRIPTION = "Number of requests processed";

    private CountStatisticImpl errorCount = new CountStatisticImpl(
        "ErrorCount", StatisticImpl.UNIT_COUNT, ERROR_COUNT_DESCRIPTION);

    private CountStatisticImpl requestCount = new CountStatisticImpl(
        "RequestCount", StatisticImpl.UNIT_COUNT, REQUEST_COUNT_DESCRIPTION);

    private CountStatisticImpl maxTime = new CountStatisticImpl("MaxTime",
            StatisticImpl.UNIT_MILLISECOND, MAX_TIME_DESCRIPTION);

    private CountStatisticImpl processingTime = new CountStatisticImpl(
        "ProcessingTime", StatisticImpl.UNIT_MILLISECOND,
        PROCESSING_TIME_DESCRIPTION);

    private TimeStatisticImpl requestProcessTime = new TimeStatisticImpl(
        0L, 0L, 0L, 0L, "", "", "", System.currentTimeMillis(), -1L);

    private CountStatisticImpl serviceTime = new CountStatisticImpl(
        "ServiceTime", StatisticImpl.UNIT_MILLISECOND,
        SERVICE_TIME_DESCRIPTION);

    private String servletName;
    private String moduleName;
    private String vsName;
    private ServletStatsProvider servletStatsProvider;

    private ThreadLocal<TimeStatData> timeStatDataLocal = new ThreadLocal<TimeStatData> (){
        TimeStatData tsd;

        protected TimeStatData initialValue (){
            tsd = new TimeStatData ();
            return tsd;
        }
        public TimeStatData get (){
            if (tsd == null) {
                tsd = new TimeStatData();
            }
            return tsd;
        }
    };
    
    public ServletInstanceStatsProvider(String servletName,
            String moduleName, String vsName,
            ServletStatsProvider servletStatsProvider) {
        this.servletName = servletName;
        this.moduleName = moduleName;
        this.vsName = vsName;
        this.servletStatsProvider = servletStatsProvider;
    }

    public String getModuleName() {
        return moduleName;
    }
    
    public String getVSName() {
        return vsName;
    }

    @ManagedAttribute(id="errorcount")
    @Description(ERROR_COUNT_DESCRIPTION)
    public CountStatistic getErrorCount() {
        return errorCount;
    }

    @ManagedAttribute(id="maxtime")
    @Description(MAX_TIME_DESCRIPTION)
    public CountStatistic getMaximumTime() {
        maxTime.setCount(requestProcessTime.getMaxTime());
        return maxTime;
    }

    @ManagedAttribute(id="servicetime")
    @Description(SERVICE_TIME_DESCRIPTION)
    public CountStatistic getServiceTime() {
        serviceTime.setCount(requestProcessTime.getTotalTime());
        return serviceTime;
    }

    @ManagedAttribute(id="processingtime")
    @Description(PROCESSING_TIME_DESCRIPTION)
    public CountStatistic getProcessingTime() {
        long count = requestProcessTime.getCount();
        long processTime = 0L;
        if (count != 0) {
            processTime = requestProcessTime.getTotalTime()/count;
        }
        processingTime.setCount(processTime);
        return processingTime;
    }

    @ManagedAttribute(id="requestcount")
    @Description(REQUEST_COUNT_DESCRIPTION)
    public CountStatistic getCount() {
        requestCount.setCount(requestProcessTime.getCount());
        return requestCount;
    }

    @ProbeListener("glassfish:web:servlet:beforeServiceEvent")
    public void beforeServiceEvent(
                    @ProbeParam("servletName") String servletName,
                    @ProbeParam("appName") String appName,
                    @ProbeParam("hostName") String hostName) {
        if (isValidEvent(servletName, appName, hostName)) {
            timeStatDataLocal.get().setEntryTime(System.currentTimeMillis());
            if (logger.isLoggable(Level.FINEST)) {
                logger.finest("Servlet before service event received - " +
                        "servletName = " + servletName + 
                        ": appName = " + appName + ": hostName = " +
                        hostName);
            }
        }   
    }

    @ProbeListener("glassfish:web:servlet:afterServiceEvent")
    public void afterServiceEvent(
                    @ProbeParam("servletName") String servletName,
                    @ProbeParam("responseStatus") int responseStatus,
                    @ProbeParam("appName") String appName,
                    @ProbeParam("hostName") String hostName) {
        if (isValidEvent(servletName, appName, hostName)) {
            TimeStatData tsd = timeStatDataLocal.get();
            tsd.setExitTime(System.currentTimeMillis());
            long servletProcessingTime = tsd.getTotalTime();
            requestProcessTime.incrementCount(servletProcessingTime);
            servletStatsProvider.addServletProcessingTime(
                servletProcessingTime);

            if (responseStatus >= 400) {
                errorCount.increment();
            }

            if (logger.isLoggable(Level.FINEST)) {
                logger.finest("Servlet after service event received - " +
                        "servletName = " + servletName + 
                        ": appName = " + appName + ": hostName = " +
                        hostName);
            }
        }
    }

    @Reset
    public void reset() {
        this.requestProcessTime.reset();
        this.errorCount.reset();
        this.requestCount.reset();
        this.maxTime.reset();
        this.serviceTime.reset();
        this.processingTime.reset();
    }
    
    private boolean isValidEvent(String sName, String mName, String hostName) {
        return (moduleName.equals(mName) && vsName.equals(hostName)
                && servletName.equals(sName));
    }
}
