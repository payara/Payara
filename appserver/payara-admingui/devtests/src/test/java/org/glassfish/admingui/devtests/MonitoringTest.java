/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admingui.devtests;

import org.junit.Test;

import java.io.File;
import java.net.MalformedURLException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MonitoringTest extends BaseSeleniumTestClass {

    private static final String TRIGGER_MONITORING_SERVICE = "Enable monitoring for a component or service by selecting either LOW or HIGH. Monitoring Service and Monitoring MBeans must both be enabled to use Administration Console monitoring features.";
    private static final String MONITOR_LEVEL_OFF = "OFF";
    private static final String MONITOR_LEVEL_LOW = "LOW";
    private static final String MONITOR_LEVEL_HIGH = "HIGH";
    
    private static final String MONITOR_LEVEL_COL_ID = "col3";
    private static final String MONITOR_COMP_COL_ID = "col2";
    private static final String MONITOR_COMP_SELECT_ID = "col1";
    
    public static final String TARGET_SERVER_TYPE = "server";
    public static final String TARGET_STANDALONE_TYPE = "standalone";
    public static final String TARGET_CLUSTER_TYPE = "cluster";
    
    private static final String TRIGGER_STANDALONE_PAGE = "Create and manage standalone instances for a node agent.";
    private static final String TRIGGER_STANDALONE_GENERAL_PAGE = "General Information";
    private static final String TRIGGER_MONITORING_SERVER_GENERAL = "General Information";
    private static final String TRIGGER_MONITORING_APPLICATIONS = "Application Monitoring";
    private static final String TRIGGER_MONITORING_SERVER = "Server Monitoring";
    private static final String TRIGGER_MONITORING_RESOURCES = "Resources Monitoring";
    private static final String MONITORING_APPLICATIONS_COMPONENT_DROPDOWN_ID = "propertyForm:propertyContentPage:propertySheet:viewPropertySection:ApplicationProp:ComponentView_list";
    private static final String MONITORING_APPLICATIONS_APPLICATION_DROPDOWN_ID = "propertyForm:propertyContentPage:propertySheet:viewPropertySection:ApplicationProp:View_list";

    @Test
    public void dummy() {
        // don't fail
    }

//    @Test
    public void testMonitoringServicePage() {
        setMonitorLevel("Web Container", MONITOR_LEVEL_LOW, false, "server", TARGET_SERVER_TYPE);
        setMonitorLevel(null, MONITOR_LEVEL_OFF, true, "server", TARGET_SERVER_TYPE);
    }

//    @Test
    public void testMonitoringServerPage() {
        monitoringJvmStats("server", TARGET_SERVER_TYPE);
        monitoringWebContainerStats("server", TARGET_SERVER_TYPE);
        monitoringTransactionServiceStats("server", TARGET_SERVER_TYPE);
        monitoringSecurityStats("server", TARGET_SERVER_TYPE);
        monitoringHttpServiceStats("server", TARGET_SERVER_TYPE);
    }

//    @Test
    public void testMonitoringApplicationsPage() {
        ejbTimerMonitoring("server", TARGET_SERVER_TYPE);
        statefulAndStatelessBeanMonitoring("server", TARGET_SERVER_TYPE);
    }

    //Monitoring service related methods.
    private void goToMonitoringServicePage(String target, String targetType) {
        goToMonitoringApplicationsPage(target, targetType);
        assertTrue(isElementPresent("link=Configure Monitoring"));
        clickAndWait("link=Configure Monitoring", TRIGGER_MONITORING_SERVICE);
    }

    private void setMonitorLevel(String component, String monLevel, boolean isAll, String target, String targetType) {
        goToMonitoringServicePage(target, targetType);
        waitForPageLoad("HTTP Service", 1000);
        if (isAll) {
            pressButton("form1:basicTable:_tableActionsTop:_selectMultipleButton:_selectMultipleButton_image");
        } else {
            selectTableRowByValue("form1:basicTable", component, MONITOR_COMP_SELECT_ID, MONITOR_COMP_COL_ID);
        }
        waitForButtonEnabled("form1:basicTable:topActionsGroup1:button1");
        selectDropdownOption("form1:basicTable:topActionsGroup1:change_list", monLevel);
        pressButton("form1:basicTable:topActionsGroup1:button1");
        waitForButtonDisabled("form1:basicTable:topActionsGroup1:button1");
        clickAndWait("form1:title:topButtons:saveButton", TRIGGER_NEW_VALUES_SAVED);

        //Test whether the level has changed to monLevel or not.
        //clickAndWait("treeForm:tree:configurations:server-config:monitor:monitor_link", TRIGGER_MONITORING_SERVICE);
        goToMonitoringServicePage(target, targetType);
        String level = null;
        if (isAll) {
            level = getSelectedLabel("form1:basicTable:rowGroup1:0:" + MONITOR_LEVEL_COL_ID + ":level");
        } else {
            String id = getTableRowByValue("form1:basicTable", component, MONITOR_COMP_COL_ID);
            level = getSelectedLabel(id + MONITOR_LEVEL_COL_ID + ":level");
        }
        assertEquals(monLevel, level);
    }

    //Monitoring->Server related methods.
    private void goToMonitoringServerPage(String target, String targetType) {
        if (targetType.equals(TARGET_SERVER_TYPE)) {
            clickAndWait("treeForm:tree:applicationServer:applicationServer_link", TRIGGER_MONITORING_SERVER_GENERAL);
            clickAndWait("propertyForm:serverInstTabs:monitoring", TRIGGER_MONITORING_APPLICATIONS);
            clickAndWait("propertyForm:serverInstTabs:monitoring:monitor_server", TRIGGER_MONITORING_SERVER);
        } else if (targetType.equals(TARGET_STANDALONE_TYPE)) {
            clickAndWait("treeForm:tree:standaloneTreeNode:standaloneTreeNode_link", TRIGGER_STANDALONE_PAGE);
            clickAndWait(getLinkIdByLinkText("propertyForm:instancesTable", target), TRIGGER_STANDALONE_GENERAL_PAGE);
            clickAndWait("propertyForm:standaloneInstanceTabs:monitoring", TRIGGER_MONITORING_APPLICATIONS);
            clickAndWait("propertyForm:standaloneInstanceTabs:monitoring:monitorServer", TRIGGER_MONITORING_SERVER);
        }
    }

    private void monitoringJvmStats(String target, String targetType) {
        final String threadInfoHeader = "Thread Info Statistics";
        final String jvmMemHeader = "JVM: Memory Statistics";
        final String jvmOsHeader = "JVM: Operating System Statistics";
        final String jvmClassLoadingHeader = "JVM: Class Loading Statistics";
        final String jvmCompilationHeader = "JVM: Compilation Statistics";
        final String jvmRuntimeHeader = "JVM: Runtime Statistics";
        final String jvmThreadHeader = "JVM: Thread System Statistics";

        final String threadInfoData = "DeadlockedThreads";
        final String jvmMemData = "UsedNonHeapSize";
        final String jvmOsData = "AvailableProcessors";
        final String jvmClassLoadingData = "UnLoadedClassCount";
        final String jvmCompilationData = "TotalCompilationTime";
        final String jvmRuntimeData = "SpecVersion";
        final String jvmThreadData = "TotalStartedThreadCount";

        final String dropDownId = "propertyForm:propertyContentPage:propertySheet:viewPropertySection:VsProp:View_list";
        final String gcCopyHeader = "Garbage Collectors Statistics : Copy";
        final String gcCopyData = "CollectionCount";
        final String gcMarkSweepHeader = "Garbage Collectors Statistics : MarkSweepCompact";
        final String gcMarkSweepData = "CollectionCount";
        final String threadHeader = "Thread Info Statistics";
        final String threadData = "ThreadState";

        setMonitorLevel("JVM", MONITOR_LEVEL_HIGH, false, target, targetType);
        goToMonitoringServerPage(target, targetType);

        verifyMonitoringStat("jvmThreadInfoStats", threadInfoData, threadInfoHeader);
        verifyMonitoringStat("jvmMemStats", jvmMemData, jvmMemHeader);
        verifyMonitoringStat("osStats", jvmOsData, jvmOsHeader);
        verifyMonitoringStat("clStats", jvmClassLoadingData, jvmClassLoadingHeader);
        verifyMonitoringStat("comStats", jvmCompilationData, jvmCompilationHeader);
        verifyMonitoringStat("runtimeStats", jvmRuntimeData, jvmRuntimeHeader);
        verifyMonitoringStat("jvmThreadStats", jvmThreadData, jvmThreadHeader);

        selectDropdownOption(dropDownId, "Copy");
        waitForPageLoad(gcCopyHeader, 10000);
        verifyMonitoringStat("gcStats", gcCopyData, gcCopyHeader);
        selectDropdownOption(dropDownId, "MarkSweepCompact");
        waitForPageLoad(gcMarkSweepHeader, 10000);
        verifyMonitoringStat("gcStats", gcMarkSweepData, gcMarkSweepHeader);
        selectDropdownOption(dropDownId, "thread-1");
        waitForPageLoad(threadHeader, 10000);
        verifyMonitoringStat("jvmThreadInfoStats", threadData, threadHeader);

        setMonitorLevel("JVM", MONITOR_LEVEL_OFF, false, target, targetType);
    }

    private void monitoringWebContainerStats(String target, String targetType) {
        final String webContainerServletHeader = "Web Container: Servlet Statistics";
        final String webContainerSessionHeader = "Web Container: Session Statistics";
        final String webContainerRequestHeader = "Web Container: Request Statistics";
        final String webContainerJspHeader = "Web Container: JSP Statistics";
        final String webContainerServletData = "ActiveServletsLoaded";
        final String webContainerSessionData = "SessionsTotal";
        final String webContainerRequestData = "RequestCount";
        final String webContainerJspData = "JspReloadedCount";

        setMonitorLevel("Web Container", MONITOR_LEVEL_HIGH, false, target, targetType);
        goToMonitoringServerPage(target, targetType);

        verifyMonitoringStat("servletsWeb", webContainerServletData, webContainerServletHeader);
        verifyMonitoringStat("sessionWeb", webContainerSessionData, webContainerSessionHeader);
        verifyMonitoringStat("requestWebStats", webContainerRequestData, webContainerRequestHeader);
        verifyMonitoringStat("jspWebStats", webContainerJspData, webContainerJspHeader);

        setMonitorLevel("Web Container", MONITOR_LEVEL_OFF, false, target, targetType);
    }

    private void monitoringTransactionServiceStats(String target, String targetType) {
        final String transactionServiceHeader = "Transaction Service Statistics";
        final String transactionServiceData = "RolledbackCount";

        setMonitorLevel("Transaction Service", MONITOR_LEVEL_HIGH, false, target, targetType);
        goToMonitoringServerPage(target, targetType);
        verifyMonitoringStat("txnServiceStats", transactionServiceData, transactionServiceHeader);
        setMonitorLevel("Transaction Service", MONITOR_LEVEL_OFF, false, target, targetType);
    }

    private void monitoringSecurityStats(String target, String targetType) {
        final String webAppSecurityDeploymentHeader = "Web Application Security Deployment Statistics";
        final String webAppSecurityDeploymentData = "WebSecurityManagerCount";

        setMonitorLevel("Security", MONITOR_LEVEL_HIGH, false, target, targetType);
        goToMonitoringServerPage(target, targetType);
        verifyMonitoringStat("webSecurity", webAppSecurityDeploymentData, webAppSecurityDeploymentHeader);
        setMonitorLevel("Security", MONITOR_LEVEL_OFF, false, target, targetType);
    }

    private void monitoringHttpServiceStats(String target, String targetType) {
        final String virtualStatsHeader = "Virtual Server Statistics : server";
        final String requestStatsHeader = "Request Statistics : server";
        final String virtualStatsData = "Hosts";
        final String requestStatsData = "RequestCount";

        final String fileCacheStatsHeader = "File Cache Statistics : admin-listener";
        final String keepAliveStatsHeader = "Keep Alive Statistics : admin-listener";
        final String connectionQueueStatsHeader = "Connection Queue Statistics : admin-listener";
        final String threadPoolStatsHeader = "Thread Pool Statistics : admin-listener";
        final String fileCacheStatsData = "OpenCacheEntriesCount";
        final String keepAliveStatsData = "CountFlushes";
        final String connectionQueueStatsData = "CountOverflows";
        final String threadPoolStatsData = "CoreThreads";

        String dropDownId = "propertyForm:propertyContentPage:propertySheet:viewPropertySection:VsProp:View_list";

        setMonitorLevel("HTTP Service", MONITOR_LEVEL_HIGH, false, target, targetType);
        goToMonitoringServerPage(target, targetType);

        selectDropdownOption(dropDownId, "server");
        waitForPageLoad(virtualStatsHeader, 10000);

        verifyMonitoringStat("virtualServerStats", virtualStatsData, virtualStatsHeader);
        verifyMonitoringStat("httpServiceStats", requestStatsData, requestStatsHeader);

        selectDropdownOption(dropDownId, "admin-listener");
        waitForPageLoad(fileCacheStatsHeader, 10000);
        verifyMonitoringStat("fileCacheStats", fileCacheStatsData, fileCacheStatsHeader);
        verifyMonitoringStat("keepAliveStats", keepAliveStatsData, keepAliveStatsHeader);
        verifyMonitoringStat("connectionQueueStats", connectionQueueStatsData, connectionQueueStatsHeader);
        verifyMonitoringStat("threadPoolStats", threadPoolStatsData, threadPoolStatsHeader);

        setMonitorLevel("HTTP Service", MONITOR_LEVEL_OFF, false, target, targetType);
    }

    private void verifyMonitoringStat(String stat, String statData, String statHeader) {
        assertTrue(isTextPresent(statHeader));
        clickAndWait("propertyForm:propertyContentPage:basicTable:" + stat + ":_groupHeader:_groupPanelToggleButton:_groupPanelToggleButton_image", statData);
        pressButton("propertyForm:propertyContentPage:basicTable:" + stat + ":_groupHeader:_groupPanelToggleButton:_groupPanelToggleButton_image");
    }

    private void goToMonitoringApplicationsPage(String target, String targetType) {
        if (targetType.equals(TARGET_SERVER_TYPE)) {
            clickAndWait("treeForm:tree:applicationServer:applicationServer_link", TRIGGER_MONITORING_SERVER_GENERAL);
            clickAndWait("propertyForm:serverInstTabs:monitoring", TRIGGER_MONITORING_APPLICATIONS);
        } else if (targetType.equals(TARGET_STANDALONE_TYPE)) {
            clickAndWait("treeForm:tree:standaloneTreeNode:standaloneTreeNode_link", TRIGGER_STANDALONE_PAGE);
            clickAndWait(getLinkIdByLinkText("propertyForm:instancesTable", target), TRIGGER_STANDALONE_GENERAL_PAGE);
            clickAndWait("propertyForm:standaloneInstanceTabs:monitoring", TRIGGER_MONITORING_APPLICATIONS);
        }
    }

    private void ejbTimerMonitoring(String target, String targetType) {
        final String statsHeader = "EJB Singleton Bean Statistics : ejb-timer-sessiontimer-ejb.jar/TimerSingleton";
        final String statDescription = "Number of times EJB remove method is called";
        final String appName = "ejb-timer-sessiontimerApp";

        deployApp("src/test/resources/ejb-timer-sessiontimerApp.ear", targetType, appName);
        setMonitorLevel("Web Container", MONITOR_LEVEL_HIGH, false, target, targetType);
        setMonitorLevel("EJB Container", MONITOR_LEVEL_HIGH, false, target, targetType);
        goToMonitoringApplicationsPage(target, targetType);

        selectDropdownOption(MONITORING_APPLICATIONS_COMPONENT_DROPDOWN_ID, "TimerSingleton");
        waitForPageLoad(statsHeader, 10000);
        assertTrue(isTextPresent(statsHeader));
        assertTrue(isTextPresent(statDescription));

        setMonitorLevel("Web Container", MONITOR_LEVEL_OFF, false, target, targetType);
        setMonitorLevel("EJB Container", MONITOR_LEVEL_OFF, false, target, targetType);
        undeployApp(appName, targetType);
    }

    public void appScopedResourcesMonitoring(String target, String targetType, String resName) {
        final String statsHeader = "Application Scoped Resource Statistics : JavaEEApp-war.war/resources/"+resName;
        final String statDescription = "Number of potential connection leaks";

        setMonitorLevel(null, MONITOR_LEVEL_HIGH, true, target, targetType);
        goToMonitoringApplicationsPage(target, targetType);

        selectDropdownOption(MONITORING_APPLICATIONS_APPLICATION_DROPDOWN_ID, "JavaEEApp-war.war");        
        selectDropdownOption(MONITORING_APPLICATIONS_COMPONENT_DROPDOWN_ID, resName);
        waitForPageLoad(statsHeader, 10000);
        assertTrue(isTextPresent(statsHeader));
        assertTrue(isTextPresent(statDescription));

        setMonitorLevel(null, MONITOR_LEVEL_OFF, true, target, targetType);
    }

    private void statefulAndStatelessBeanMonitoring(String target, String targetType) {
        final String statefulStatsHeader = "EJB Stateful Session Bean Statistics : ejb-ejb30-hello-session-ejb.jar/SfulEJB";
        final String statefulStatDescription = "Number of stateful session beans in MethodReady state";
        final String statelessStatsHeader = "EJB Stateless Session Bean Statistics : ejb-ejb30-hello-session-ejb.jar/SlessEJB";
        final String statelessStatDescription = "Number of stateless session beans in MethodReady state";
        String applicationName = "ejb-ejb30-hello-sessionApp";

        deployApp("src/test/resources/ejb-ejb30-hello-sessionApp.ear", targetType, applicationName);
        setMonitorLevel("Web Container", MONITOR_LEVEL_HIGH, false, target, targetType);
        setMonitorLevel("EJB Container", MONITOR_LEVEL_HIGH, false, target, targetType);
        goToMonitoringApplicationsPage(target, targetType);

        selectDropdownOption(MONITORING_APPLICATIONS_COMPONENT_DROPDOWN_ID, "SfulEJB");
        waitForPageLoad(statefulStatsHeader, 10000);
        assertTrue(isTextPresent(statefulStatsHeader));
        assertTrue(isTextPresent(statefulStatDescription));

        selectDropdownOption(MONITORING_APPLICATIONS_COMPONENT_DROPDOWN_ID, "SlessEJB");
        waitForPageLoad(statelessStatsHeader, 10000);
        assertTrue(isTextPresent(statelessStatsHeader));
        assertTrue(isTextPresent(statelessStatDescription));

        setMonitorLevel("Web Container", MONITOR_LEVEL_OFF, false, target, targetType);
        setMonitorLevel("EJB Container", MONITOR_LEVEL_OFF, false, target, targetType);
        undeployApp(applicationName, targetType);
    }

    private void deployApp(String appLocation, String target, String appName) {
        final String TRIGGER_APPLICATIONS = "Applications can be enterprise or web applications, or various kinds of modules.";

        clickAndWait("treeForm:tree:applications:applications_link", TRIGGER_APPLICATIONS);
        if (isTextPresent(appName)) {
            undeployApp(appName, target);
        }
        clickAndWaitForElement("propertyForm:deployTable:topActionsGroup1:deployButton", "form:sheet1:section1:prop1:fileupload");
        File war = new File(appLocation);
        try {
            selectFile("form:sheet1:section1:prop1:fileupload", war.toURL().toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }
        //waitForCondition("document.getElementById('form:war:psection:nameProp:appName').value == '" + appName + "'", 300000);
        assertEquals(appName, getFieldValue("form:war:psection:nameProp:appName"));
        clickAndWait("form:title:topButtons:uploadButton", TRIGGER_APPLICATIONS);
        assertTrue(isTextPresent(appName));
    }

    private void undeployApp(String applicationName, String target) {
        final String TRIGGER_APPLICATIONS = "Applications can be enterprise or web applications, or various kinds of modules.";
        clickAndWait("treeForm:tree:applications:applications_link", TRIGGER_APPLICATIONS);
        selectTableRowByValue("propertyForm:deployTable", applicationName);
        pressButton("propertyForm:deployTable:topActionsGroup1:button1");
        getConfirmation();
        waitForPageLoad(applicationName, true);
    }
}
