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

package org.glassfish.web.admin.monitor;

import org.glassfish.external.statistics.CountStatistic;
import org.glassfish.external.statistics.RangeStatistic;
import org.glassfish.external.statistics.impl.CountStatisticImpl;
import org.glassfish.external.statistics.impl.RangeStatisticImpl;
import org.glassfish.external.statistics.impl.StatisticImpl;
import org.glassfish.external.probe.provider.annotations.*;
import org.glassfish.gmbal.AMXMetadata;
import org.glassfish.gmbal.Description;
import org.glassfish.gmbal.ManagedAttribute;
import org.glassfish.gmbal.ManagedObject;

/**
 * Provides the monitoring data at the Web container level
 *
 * @author Prashanth Abbagani
 */
@AMXMetadata(type="jsp-mon", group="monitoring")
@ManagedObject
@Description("Web Container JSP Statistics")
public class JspStatsProvider {

    private static final String JSP_COUNT_DESCRIPTION =
        "Number of active JSP pages";

    private static final String TOTAL_JSP_COUNT_DESCRIPTION =
        "Total number of JSP pages ever loaded";

    private static final String JSP_RELOADED_COUNT_DESCRIPTION =
        "Total number of JSP pages that were reloaded";

    private static final String JSP_ERROR_COUNT_DESCRIPTION =
        "Total number of errors triggered by JSP page invocations";

    private String moduleName;
    private String vsName; 
    private RangeStatisticImpl jspCount;
    private CountStatisticImpl totalJspCount;
    private CountStatisticImpl jspReloadedCount;
    private CountStatisticImpl jspErrorCount;
    
    public JspStatsProvider(String moduleName, String vsName) {
        this.moduleName = moduleName;
        this.vsName = vsName;
        long curTime = System.currentTimeMillis();
        jspCount = new RangeStatisticImpl(
            0L, 0L, 0L, "JspCount", StatisticImpl.UNIT_COUNT,
            JSP_COUNT_DESCRIPTION, curTime, curTime);
        totalJspCount = new CountStatisticImpl(
            "TotalJspCount", StatisticImpl.UNIT_COUNT,
            TOTAL_JSP_COUNT_DESCRIPTION);
        jspReloadedCount = new CountStatisticImpl(
            "JspReloadedCount", StatisticImpl.UNIT_COUNT,
            JSP_RELOADED_COUNT_DESCRIPTION);
        jspErrorCount = new CountStatisticImpl(
            "JspErrorCount", StatisticImpl.UNIT_COUNT,
            JSP_ERROR_COUNT_DESCRIPTION);
    }

    @ManagedAttribute(id="jspcount")
    @Description(JSP_COUNT_DESCRIPTION)
    public RangeStatistic getJspCount() {
        return jspCount;
    }

    @ManagedAttribute(id="totaljspcount")
    @Description(TOTAL_JSP_COUNT_DESCRIPTION)
    public CountStatistic getTotalJspCount() {
        return totalJspCount;
    }

    @ManagedAttribute(id="jspreloadedcount")
    @Description(JSP_RELOADED_COUNT_DESCRIPTION)
    public CountStatistic getJspReloadedCount() {
        return jspReloadedCount;
    }

    @ManagedAttribute(id="jsperrorcount")
    @Description(JSP_ERROR_COUNT_DESCRIPTION)
    public CountStatistic getJspErrorCount() {
        return jspErrorCount;
    }
    
    @ProbeListener("glassfish:web:jsp:jspLoadedEvent")
    public void jspLoadedEvent(
            @ProbeParam("jspUri") String jspUri,
            @ProbeParam("appName") String appName,
            @ProbeParam("hostName") String hostName) {
        if (isValidEvent(appName, hostName)) {
            synchronized (jspCount) {
                jspCount.setCurrent(
                    jspCount.getCurrent() + 1);
            }
            totalJspCount.increment();
        }
    }

    @ProbeListener("glassfish:web:jsp:jspReloadedEvent")
    public void jspReloadedEvent(
            @ProbeParam("jspUri") String jspUri,
            @ProbeParam("appName") String appName,
            @ProbeParam("hostName") String hostName) {
        if (isValidEvent(appName, hostName)) {
            jspReloadedCount.increment();
        }
    }

    @ProbeListener("glassfish:web:jsp:jspDestroyedEvent")
    public void jspDestroyedEvent(
            @ProbeParam("jspUri") String jspUri,
            @ProbeParam("appName") String appName,
            @ProbeParam("hostName") String hostName) {
        if (isValidEvent(appName, hostName)) {
            synchronized (jspCount) {
                jspCount.setCurrent(
                    jspCount.getCurrent() - 1);
            }
        }
    }

    @ProbeListener("glassfish:web:jsp:jspErrorEvent")
    public void jspErrorEvent(
            @ProbeParam("jspUri") String jspUri,
            @ProbeParam("appName") String appName,
            @ProbeParam("hostName") String hostName) {
        if (isValidEvent(appName, hostName)) {
            jspErrorCount.increment();
        }
    }

    public String getModuleName() {
        return moduleName;
    }
    
    public String getVSName() {
        return vsName;
    }
    
    private boolean isValidEvent(String mName, String hostName) {
        //Temp fix, get the appname from the context root
        if ((moduleName == null) || (vsName == null)) {
            return true;
        }
        if ((moduleName.equals(mName)) && (vsName.equals(hostName))) {
            return true;
        }
        
        return false;
    }
}
