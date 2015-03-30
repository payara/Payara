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
import org.glassfish.external.statistics.StringStatistic;
import org.glassfish.external.statistics.annotations.Reset;
import org.glassfish.external.statistics.impl.CountStatisticImpl;
import org.glassfish.external.statistics.impl.StatisticImpl;
import org.glassfish.external.statistics.impl.StringStatisticImpl;
import org.glassfish.external.statistics.impl.TimeStatisticImpl;
import org.glassfish.external.probe.provider.annotations.*;
import org.glassfish.gmbal.Description;
import org.glassfish.gmbal.AMXMetadata;
import org.glassfish.gmbal.ManagedAttribute;
import org.glassfish.gmbal.ManagedObject;
import org.glassfish.grizzly.config.dom.NetworkConfig;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.hk2.api.PostConstruct;

/**
 * Provides the monitoring data at the Web container level
 *
 * @author Prashanth Abbagani
 * @author Amy Roh
 */
@AMXMetadata(type="request-mon", group="monitoring")
@ManagedObject
@Description( "Web Container HTTP Service Statistics" )
public class HttpServiceStatsProvider implements PostConstruct {

    private NetworkConfig networkConfig;

    private static final Logger logger = HttpServiceStatsProviderBootstrap.logger;

    private static final String ERROR_COUNT_DESCRIPTION =
            "Cumulative value of the error count, with error count representing the number of cases where the response code was greater than or equal to 400";
    private static final String MAX_TIME_DESCRIPTION =
            "Longest response time for a request; not a cumulative value, but the largest response time from among the response times";
    private static final String PROCESSING_TIME_DESCRIPTION =
            "Average request processing time";
    private static final String COUNT_BYTES_RECEIVED_DESCRIPTION =
            "The number of bytes received";
    private static final String COUNT_BYTES_TRANSMITTED_DESCRIPTION =
            "The number of bytes transmitted";
    private static final String COUNT_OPEN_CONNECTIONS_DESCRIPTION =
            "The number of open connections";
    private static final String COUNT_REQUESTS_DESCRIPTION =
            "The number of requests received";
    private static final String MAX_OPEN_CONNECTIONS_DESCRIPTION =
            "The maximum number of open connections";
    private static final String METHOD_DESCRIPTION =
            "The method of the last request serviced";
    private static final String URI_DESCRIPTION =
            "The URI of the last request serviced";
    private static final String COUNT_200_DESCRIPTION =
            "Number of responses with a status code equal to 200";
    private static final String COUNT_2xx_DESCRIPTION =
            "Number of responses with a status code in the 2xx range";
    private static final String COUNT_302_DESCRIPTION =
            "Number of responses with a status code equal to 302";
    private static final String COUNT_304_DESCRIPTION =
            "Number of responses with a status code equal to 304";
    private static final String COUNT_3xx_DESCRIPTION =
            "Number of responses with a status code in the 3xx range";
    private static final String COUNT_400_DESCRIPTION =
            "Number of responses with a status code equal to 400";
    private static final String COUNT_401_DESCRIPTION =
            "Number of responses with a status code equal to 401";
    private static final String COUNT_403_DESCRIPTION =
            "Number of responses with a status code equal to 403";
    private static final String COUNT_404_DESCRIPTION =
            "Number of responses with a status code equal to 404";
    private static final String COUNT_4xx_DESCRIPTION =
            "Number of responses with a status code in the 4xx range";
    private static final String COUNT_503_DESCRIPTION =
            "Number of responses with a status code equal to 503";
    private static final String COUNT_5xx_DESCRIPTION =
            "Number of responses with a status code in the 5xx range";
    private static final String COUNT_OTHER_DESCRIPTION =
            "Number of responses with a status code outside the 2xx, 3xx, 4xx, and 5xx range";

    private CountStatisticImpl errorCount = new CountStatisticImpl("ErrorCount",
            StatisticImpl.UNIT_COUNT, ERROR_COUNT_DESCRIPTION);
    private CountStatisticImpl maxTime = new CountStatisticImpl("MaxTime",
            StatisticImpl.UNIT_MILLISECOND, MAX_TIME_DESCRIPTION);
    private CountStatisticImpl processingTime = new CountStatisticImpl("ProcessingTime",
            StatisticImpl.UNIT_MILLISECOND, PROCESSING_TIME_DESCRIPTION);
    private CountStatisticImpl countBytesReceived = new CountStatisticImpl("CountBytesReceived",
            StatisticImpl.UNIT_COUNT, COUNT_BYTES_RECEIVED_DESCRIPTION);
    private CountStatisticImpl countBytesTransmitted = new CountStatisticImpl("CountBytesTransmitted",
            StatisticImpl.UNIT_COUNT, COUNT_BYTES_TRANSMITTED_DESCRIPTION);
    private CountStatisticImpl countOpenConnections = new CountStatisticImpl("CountOpenConnections",
            StatisticImpl.UNIT_COUNT, COUNT_OPEN_CONNECTIONS_DESCRIPTION);
    private CountStatisticImpl countRequests = new CountStatisticImpl("CountRequests",
            StatisticImpl.UNIT_COUNT, COUNT_REQUESTS_DESCRIPTION);
    private CountStatisticImpl maxOpenConnections = new CountStatisticImpl("MaxOpenConnections",
            StatisticImpl.UNIT_COUNT, MAX_OPEN_CONNECTIONS_DESCRIPTION);
    private StringStatisticImpl method = new StringStatisticImpl("Method",
            "String", METHOD_DESCRIPTION);
    private StringStatisticImpl uri = new StringStatisticImpl("Uri",
            "String", URI_DESCRIPTION);

    private CountStatisticImpl count200 = new CountStatisticImpl("Count200",
            StatisticImpl.UNIT_COUNT, COUNT_200_DESCRIPTION);
    private CountStatisticImpl count2xx = new CountStatisticImpl("Count2xx",
            StatisticImpl.UNIT_COUNT, COUNT_2xx_DESCRIPTION);
    private CountStatisticImpl count302 = new CountStatisticImpl("Count302",
            StatisticImpl.UNIT_COUNT, COUNT_302_DESCRIPTION);
    private CountStatisticImpl count304 = new CountStatisticImpl("Count304",
            StatisticImpl.UNIT_COUNT, COUNT_304_DESCRIPTION);
    private CountStatisticImpl count3xx = new CountStatisticImpl("Count3xx",
            StatisticImpl.UNIT_COUNT, COUNT_3xx_DESCRIPTION);
    private CountStatisticImpl count400 = new CountStatisticImpl("Count400",
            StatisticImpl.UNIT_COUNT, COUNT_400_DESCRIPTION);
    private CountStatisticImpl count401 = new CountStatisticImpl("Count401",
            StatisticImpl.UNIT_COUNT, COUNT_401_DESCRIPTION);
    private CountStatisticImpl count403 = new CountStatisticImpl("Count403",
            StatisticImpl.UNIT_COUNT, COUNT_403_DESCRIPTION);
    private CountStatisticImpl count404 = new CountStatisticImpl("Count404",
            StatisticImpl.UNIT_COUNT, COUNT_404_DESCRIPTION);
    private CountStatisticImpl count4xx = new CountStatisticImpl("Count4xx",
            StatisticImpl.UNIT_COUNT, COUNT_4xx_DESCRIPTION);
    private CountStatisticImpl count503 = new CountStatisticImpl("Count503",
            StatisticImpl.UNIT_COUNT, COUNT_503_DESCRIPTION);
    private CountStatisticImpl count5xx = new CountStatisticImpl("Count5xx",
            StatisticImpl.UNIT_COUNT, COUNT_5xx_DESCRIPTION);
    private CountStatisticImpl countOther = new CountStatisticImpl("CountOther",
            StatisticImpl.UNIT_COUNT, COUNT_OTHER_DESCRIPTION);

    private TimeStatisticImpl requestProcessTime = new TimeStatisticImpl(0L, 0L, 0L, 0L,
            "", "", "", System.currentTimeMillis(), -1L);

    private String virtualServerName = null;
    private String [] networkListeners = null;

    private ThreadLocal<TimeStatData> individualData = new ThreadLocal<TimeStatData> (){

        TimeStatData tsd;

        protected TimeStatData initialValue (){
            tsd = new TimeStatData ();
            return tsd;
        }
        public TimeStatData get (){
            if (tsd == null)
                tsd = new TimeStatData();
            return tsd;
        }

    };

    public HttpServiceStatsProvider(String vsName, String listeners, NetworkConfig networkConfig) {
        this.virtualServerName = vsName;
        this.networkListeners = listeners == null ? new String[0] : listeners.split(",");
        this.networkConfig = networkConfig;
    }

    public void postConstruct() {
    }

    @ManagedAttribute(id="maxtime")
    @Description(MAX_TIME_DESCRIPTION)
    public CountStatistic getMaxTime() {
        maxTime.setCount(requestProcessTime.getMaxTime());
        return maxTime;
    }

    @ManagedAttribute(id="processingtime")
    @Description(PROCESSING_TIME_DESCRIPTION)
    public CountStatistic getTime() {
        processingTime.setCount(this.getProcessTime());
        return processingTime;
    }

    @ManagedAttribute(id="countbytesreceived")
    @Description(COUNT_BYTES_RECEIVED_DESCRIPTION)
    public CountStatistic getCountBytesReceived() {
        return countBytesReceived;
    }

    @ManagedAttribute(id="countbytestransmitted")
    @Description(COUNT_BYTES_TRANSMITTED_DESCRIPTION)
    public CountStatistic getCountBytesTransmitted() {
        return countBytesTransmitted;
    }

    @ManagedAttribute(id="countopenconnections")
    @Description(COUNT_OPEN_CONNECTIONS_DESCRIPTION)
    public CountStatistic getCountOpenConnections() {
        return countOpenConnections;
    }

    @ManagedAttribute(id="countrequests")
    @Description(COUNT_REQUESTS_DESCRIPTION)
    public CountStatistic getCountRequests() {
        return countRequests;
    }

    @ManagedAttribute(id="maxopenconnections")
    @Description(MAX_OPEN_CONNECTIONS_DESCRIPTION)
    public CountStatistic getMaxOpenConnections() {
        return maxOpenConnections;
    }

    @ManagedAttribute(id="method")
    @Description(METHOD_DESCRIPTION)
    public StringStatistic getMethod() {
        return method;
    }

    @ManagedAttribute(id="uri")
    @Description(URI_DESCRIPTION)
    public StringStatistic getUri() {
        return uri;
    }

    @ManagedAttribute(id="errorcount")
    @Description(ERROR_COUNT_DESCRIPTION)
    public CountStatistic getErrorCount() {
        return errorCount;
    }
    
    @ManagedAttribute(id="count200")
    @Description(COUNT_200_DESCRIPTION)
    public CountStatistic getCount200() {
        return count200;
    }
    
    @ManagedAttribute(id="count2xx")
    @Description(COUNT_2xx_DESCRIPTION)
    public CountStatistic getCount2xx() {
        return count2xx;
    }
    
    @ManagedAttribute(id="count302")
    @Description(COUNT_302_DESCRIPTION)
    public CountStatistic getCount302() {
        return count302;
    }
    
    @ManagedAttribute(id="count304")
    @Description(COUNT_304_DESCRIPTION)
    public CountStatistic getCount304() {
        return count304;
    }

    @ManagedAttribute(id="count3xx")
    @Description(COUNT_3xx_DESCRIPTION) 
    public CountStatistic getCount3xxt() {
        return count3xx;
    }

    @ManagedAttribute(id="count400")
    @Description(COUNT_400_DESCRIPTION)
    public CountStatistic getCount400() {
        return count400;
    }

    @ManagedAttribute(id="count401")
    @Description(COUNT_401_DESCRIPTION)
    public CountStatistic getCount401() {
        return count401;
    }

    @ManagedAttribute(id="count403")
    @Description(COUNT_403_DESCRIPTION)
    public CountStatistic getCount403() {
        return count403;
    }

    @ManagedAttribute(id="count404")
    @Description(COUNT_404_DESCRIPTION)
    public CountStatistic getCount404() {
        return count404;
    }

    @ManagedAttribute(id="count4xx")
    @Description(COUNT_4xx_DESCRIPTION)
    public CountStatistic getCount4xx() {
        return count4xx;
    }

    @ManagedAttribute(id="count503")
    @Description(COUNT_503_DESCRIPTION)
    public CountStatistic getCount503() {
        return count503;
    }

    @ManagedAttribute(id="count5xx")
    @Description(COUNT_5xx_DESCRIPTION)
    public CountStatistic getCount5xx() {
        return this.count5xx;
    }

    @ManagedAttribute(id="countother")
    @Description(COUNT_OTHER_DESCRIPTION)
    public CountStatistic getCountOther() {
        return this.countOther;
    }

    @ProbeListener("glassfish:web:http-service:dataReceivedEvent")
    public void dataReceivedEvent(
        @ProbeParam("size") int size) {
        countBytesReceived.increment(size);
    }

    @ProbeListener("glassfish:web:http-service:dataSentEvent")
    public void dataSentEvent(
        @ProbeParam("size") long size) {
        countBytesTransmitted.increment(size);
    }

    @ProbeListener("glassfish:web:http-service:requestStartEvent")
    public void requestStartEvent(
            @ProbeParam("appName") String appName,
            @ProbeParam("hostName") String hostName,
            @ProbeParam("serverName") String serverName,
            @ProbeParam("serverPort") int serverPort,
            @ProbeParam("contextPath") String contextPath,
            @ProbeParam("servletPath") String servletPath) {
        if ((hostName != null) && (hostName.equals(virtualServerName))) {
            individualData.get().setEntryTime(System.currentTimeMillis());
            countRequests.increment();
            if (logger.isLoggable(Level.FINEST)) {
                logger.finest(
                    "[TM]requestStartEvent received - virtual-server = " +
                    hostName + " : port = " + serverPort);
            }
        }
    }

    @ProbeListener("glassfish:web:http-service:requestEndEvent")
    public void requestEndEvent(
            @ProbeParam("appName") String appName,
            @ProbeParam("hostName") String hostName,
            @ProbeParam("serverName") String serverName,
            @ProbeParam("serverPort") int serverPort,
            @ProbeParam("contextPath") String contextPath,
            @ProbeParam("servletPath") String servletPath,
            @ProbeParam("statusCode") int statusCode,
            @ProbeParam("method") String method,
            @ProbeParam("uri") String uri) {
        if ((hostName != null) && (hostName.equals(virtualServerName))) {
            TimeStatData tsd = individualData.get();
            tsd.setExitTime(System.currentTimeMillis());
            requestProcessTime.incrementCount(tsd.getTotalTime());
            incrementStatsCounter(statusCode);
            if (logger.isLoggable(Level.FINEST)) {
                logger.finest(
                    "[TM]requestEndEvent received - virtual-server = " +
                    hostName + ": application = " +
                    contextPath + " : servlet = " +
                    servletPath + " :Response code = " +
                    statusCode + " :Response time = " +
                    tsd.getTotalTime());
            }
        }
        this.method.setCurrent(method);
        this.uri.setCurrent(uri);
    }

    // ---------------- Connection related listeners -----------
    @ProbeListener("glassfish:kernel:connection-queue:connectionAcceptedEvent")
    public void connectionAcceptedEvent(
            @ProbeParam("listenerName") String listenerName,
            @ProbeParam("connection") int connectionId,
            @ProbeParam("address") String address) {
        for (String listener : networkListeners) {
            if (listener.equals(listenerName)) {
                countOpenConnections.increment();
            }
            NetworkListener networkListener = networkConfig.getNetworkListener(listenerName);
            if (networkListener != null) {
                maxOpenConnections.setCount(
                        Integer.valueOf(networkListener.findProtocol().getHttp().getMaxConnections()));
            }
        }
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest(
                "[TM]connectionAcceptedEvent received - virtual-server = " + listenerName);
        }
    }

    @ProbeListener("glassfish:kernel:connection-queue:connectionClosedEvent")
    public void connectionClosedEvent(
            @ProbeParam("listenerName") String listenerName,
            @ProbeParam("connection") int connectionId) {
        for (String listener : networkListeners) {
            if (listener.equals(listenerName)) {
                countOpenConnections.decrement();
            }
        }
        if (logger.isLoggable(Level.FINEST)) {
            logger.finest(
                "[TM]connectionClosedEvent received - virtual-server = " + listenerName);
        }
    }

    
    public long getProcessTime() {
        long count = requestProcessTime.getCount();
        long processTime = 0L;
        if (count != 0) {
            processTime = requestProcessTime.getTotalTime()/count;
        }
        return processTime;
    }

    private void incrementStatsCounter(int statusCode) {
        switch (statusCode) {
            case 200:
                count200.increment();
                break;
            case 302:
                count302.increment();
                break;
            case 304:
                count304.increment();
                break;
            case 400:
                count400.increment();
                break;
            case 401:
                count401.increment();
                break;
            case 403:
                count403.increment();
                break;
            case 404:
                count404.increment();
                break;
            case 503:
                count503.increment();
                break;
            default:
                break;
        }

        if (200 <= statusCode && statusCode <=299) {
            count2xx.increment();
        } else if (300 <= statusCode && statusCode <=399) {
            count3xx.increment();
        } else if (400 <= statusCode && statusCode <=499) {
            count4xx.increment();
        } else if (500 <= statusCode && statusCode <=599) {
            count5xx.increment();
        } else {
            countOther.increment();
        }

        if (statusCode >= 400)
            errorCount.increment();
    }

    //Need to add this because requestProcessTime needs to be reset.
    //Since it is not exposed as public statistic the reset() would not get
    //called on its TimeStatisticImpl object so we need to use @Reset instead.
    //If @Reset is used then reset() won't be called on the statistic impl objects
    //so all the stats need to reset here as well.
    @Reset
    public void reset() {
        this.requestProcessTime.reset();
        this.count200.reset();
        this.count2xx.reset();
        this.count302.reset();
        this.count304.reset();
        this.count3xx.reset();
        this.count400.reset();
        this.count401.reset();
        this.count403.reset();
        this.count404.reset();
        this.count4xx.reset();
        this.count503.reset();
        this.count5xx.reset();
        this.countOther.reset();
        this.errorCount.reset();
        this.maxTime.reset();
        this.processingTime.reset();
        this.countBytesReceived.reset();
        this.countBytesTransmitted.reset();
        this.countOpenConnections.reset();
        this.countRequests.reset();
        this.maxOpenConnections.reset();
        this.method.reset();
        this.uri.reset();
    }
}
