/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.deployment.monitor;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.Map;
import java.util.HashMap;
import org.glassfish.external.statistics.CountStatistic;
import org.glassfish.external.statistics.RangeStatistic;
import org.glassfish.external.statistics.StringStatistic;
import org.glassfish.external.statistics.impl.CountStatisticImpl;
import org.glassfish.external.statistics.impl.RangeStatisticImpl;
import org.glassfish.external.statistics.impl.StatisticImpl;
import org.glassfish.external.statistics.impl.StringStatisticImpl;
import org.glassfish.external.probe.provider.annotations.*;
import org.glassfish.gmbal.AMXMetadata;
import org.glassfish.gmbal.Description;
import org.glassfish.gmbal.ManagedAttribute;
import org.glassfish.gmbal.ManagedObject;

/**
 * Provider statistics for deployment lifecycle
 */
@AMXMetadata(type="deployment-mon", group="monitoring")
@ManagedObject
@Description("Deployment Module Statistics")
public class DeploymentLifecycleStatsProvider {

    public static final Logger deplLogger = org.glassfish.deployment.common.DeploymentContextImpl.deplLogger;

    private static final String ACTIVE_APPLICATIONS_DEPLOYED_DESCRIPTION =
        "Number of applications deployed";

    private static final String TOTAL_APPLICATIONS_DEPLOYED_DESCRIPTION =
        "Total number of applications ever deployed";

    private static final String APPLICATIONS_INFORMATION_DESCRIPTION =
        "Information about deployed applications";

    private static final String MODULE_TYPE = "moduleType";
    private static final String LOADING_TIME = "loadingTime";

    private static final int COLUMN_LENGTH = 25;
    private static final String LINE_BREAK = "%%%EOL%%%";

    private RangeStatisticImpl activeApplicationsDeployedCount;
    private CountStatisticImpl totalApplicationsDeployedCount;

    private StringStatisticImpl appsInfoStat = new StringStatisticImpl(
        "ApplicationsInformation", "List", 
        APPLICATIONS_INFORMATION_DESCRIPTION);

    private Map<String, Map<String, String>> appsInfoMap = 
        new HashMap<String, Map<String, String>>(); 

    public DeploymentLifecycleStatsProvider() {
        long curTime = System.currentTimeMillis();
        activeApplicationsDeployedCount = new RangeStatisticImpl(
            0L, 0L, 0L, "ActiveApplicationsDeployed", StatisticImpl.UNIT_COUNT,
            ACTIVE_APPLICATIONS_DEPLOYED_DESCRIPTION, curTime, curTime); 
        totalApplicationsDeployedCount = new CountStatisticImpl(
            "TotalApplicationsDeployed", StatisticImpl.UNIT_COUNT,
            TOTAL_APPLICATIONS_DEPLOYED_DESCRIPTION);
    }

    @ManagedAttribute(id="activeapplicationsdeployedcount")
    @Description(ACTIVE_APPLICATIONS_DEPLOYED_DESCRIPTION)
    public RangeStatistic getActiveApplicationsDeployed() {
        return activeApplicationsDeployedCount;
    }

    @ManagedAttribute(id="totalapplicationsdeployedcount")
    @Description(TOTAL_APPLICATIONS_DEPLOYED_DESCRIPTION)
    public CountStatistic getTotalApplicationsDeployed() {
        return totalApplicationsDeployedCount;
    }

    @ManagedAttribute(id="applicationsinfo")
    @Description(APPLICATIONS_INFORMATION_DESCRIPTION)
    public StringStatistic getApplicationsInfo() {
        StringBuffer strBuf = new StringBuffer(1024);
        if (!appsInfoMap.isEmpty()) {
            // Set the headings for the tabular output
            int appNameLength = COLUMN_LENGTH;
            int moduleTypeLength = COLUMN_LENGTH;
            for (String appName : appsInfoMap.keySet()) {
                if (appName.length() > appNameLength) {
                    appNameLength = appName.length() + 1;
                }
                String moduleType = appsInfoMap.get(appName).get(MODULE_TYPE); 
                if (moduleType.length() > moduleTypeLength) {
                    moduleTypeLength = moduleType.length() + 1;
                }
            }

            strBuf.append(LINE_BREAK).append(LINE_BREAK);
            appendColumn(strBuf, "Application_Name", appNameLength);
	    appendColumn(strBuf, "Module_Type", moduleTypeLength);
            appendColumn(strBuf, "Loading_Time(ms)", COLUMN_LENGTH);
            strBuf.append(LINE_BREAK);

            for (String appName : appsInfoMap.keySet()) {
                appendColumn(strBuf, appName, appNameLength);
                Map<String, String> appInfoMap = appsInfoMap.get(appName);
                String moduleType = appInfoMap.get(MODULE_TYPE);
                String loadingTime = appInfoMap.get(LOADING_TIME);
		appendColumn(strBuf, moduleType, COLUMN_LENGTH);
                appendColumn(strBuf, loadingTime, COLUMN_LENGTH);
                strBuf.append(LINE_BREAK);
            }
        }
        appsInfoStat.setCurrent(strBuf.toString());
        return appsInfoStat.getStatistic();
    }

    @ProbeListener("glassfish:deployment:lifecycle:applicationDeployedEvent")
    public void applicationDeployedEvent(
                    @ProbeParam("appName") String appName,
                    @ProbeParam("appType") String appType,
                    @ProbeParam("loadTime") String loadTime) {
        if (deplLogger.isLoggable(Level.FINEST)) {
            deplLogger.finest("Application deployed event received - " +
                          "appName = " + appName +
                          ": appType = " + appType + 
                          ": loadTime = " + loadTime);
        }
        Map<String, String> appInfoMap = new HashMap<String, String>(); 
        appInfoMap.put(MODULE_TYPE, appType);
        appInfoMap.put(LOADING_TIME, loadTime);
        appsInfoMap.put(appName, appInfoMap);
        synchronized (activeApplicationsDeployedCount) {
            activeApplicationsDeployedCount.setCurrent(
                activeApplicationsDeployedCount.getCurrent() + 1);
        }
        totalApplicationsDeployedCount.increment();
    }
   
    @ProbeListener("glassfish:deployment:lifecycle:applicationUndeployedEvent")
    public void applicationUndeployedEvent(
                    @ProbeParam("appName") String appName,
                    @ProbeParam("appType") String appType) {
        if (deplLogger.isLoggable(Level.FINEST)) {
            deplLogger.finest("Application undeployed event received - " +
                          "appName = " + appName +
                          ": appType = " + appType);
        }
        appsInfoMap.remove(appName);
        synchronized (activeApplicationsDeployedCount) {
            activeApplicationsDeployedCount.setCurrent(
                activeApplicationsDeployedCount.getCurrent() - 1);
        }
    }

    private void appendColumn(StringBuffer buf, String text, int length) {
        buf.append(text);
        for (int i=text.length(); i<length; i++){
            buf.append(" ");
        }
    }
}
