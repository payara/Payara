/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.Select;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
/**
 * 
 * @author Jeremy Lv
 *
 */
public class MonitoringTest extends BaseSeleniumTestClass {

    private static final String MONITOR_LEVEL_OFF = "OFF";
    private static final String MONITOR_LEVEL_LOW = "LOW";
    private static final String MONITOR_LEVEL_HIGH = "HIGH";
    
    private static final String MONITOR_LEVEL_COL_ID = "col3";
    private static final String MONITOR_COMP_COL_ID = "col2";
    private static final String MONITOR_COMP_SELECT_ID = "col1";
    
    public static final String TARGET_SERVER_TYPE = "server";
    public static final String TARGET_STANDALONE_TYPE = "standalone";
    public static final String TARGET_CLUSTER_TYPE = "cluster";
    
    private static final String MONITORING_APPLICATIONS_COMPONENT_DROPDOWN_ID = "propertyForm:propertyContentPage:propertySheet:viewPropertySection:ApplicationProp:ComponentView_list";
    private static final String MONITORING_APPLICATIONS_APPLICATION_DROPDOWN_ID = "propertyForm:propertyContentPage:propertySheet:viewPropertySection:ApplicationProp:View_list";

    @Test
    public void testMonitoringServicePage() {
        gotoDasPage();
        setMonitorLevel("Web Container", MONITOR_LEVEL_LOW, false, "server", TARGET_SERVER_TYPE);
        setMonitorLevel(null, MONITOR_LEVEL_OFF, true, "server", TARGET_SERVER_TYPE);
    }

    @Test
    public void testMonitoringServerPage() {
        gotoDasPage();
        monitoringJvmStats("server", TARGET_SERVER_TYPE);
        monitoringWebContainerStats("server", TARGET_SERVER_TYPE);
        monitoringTransactionServiceStats("server", TARGET_SERVER_TYPE);
        
//        //This seems a bug to the glassfish v4 need to be resolved!
//        monitoringSecurityStats("server", TARGET_SERVER_TYPE);
        
        monitoringHttpServiceStats("server", TARGET_SERVER_TYPE);
    }

    @Test
    public void testMonitoringApplicationsPage() {
        gotoDasPage();
        ejbTimerMonitoring("server", TARGET_SERVER_TYPE);
        gotoDasPage();
        statefulAndStatelessBeanMonitoring("server", TARGET_SERVER_TYPE);
    }

    //Monitoring service related methods.
    private void goToMonitoringServicePage(String target, String targetType) {
        goToMonitoringApplicationsPage(target, targetType);
        while(!driver.findElement(By.linkText("Configure Monitoring")).isDisplayed()){
            sleep(500);
        }
//        waitForElementPresent("TtlTxt_sun4", "Application Monitoring");
        driver.findElement(By.linkText("Configure Monitoring")).click();
    }

    private void setMonitorLevel(String component, String monLevel, boolean isAll, String target, String targetType) {
        gotoDasPage();
        goToMonitoringServicePage(target, targetType);
        waitForElementPresent("TtlTxt_sun4", "Monitoring Service");
        if (isAll) {
            clickByIdAction("form1:basicTable:_tableActionsTop:_selectMultipleButton:_selectMultipleButton_image");
        } else {
            selectTableRowByValue("form1:basicTable", component, MONITOR_COMP_SELECT_ID, MONITOR_COMP_COL_ID);
        }
        waitForButtonEnabled("form1:basicTable:topActionsGroup1:button1");
        Select select = new Select(driver.findElement(By.id("form1:basicTable:topActionsGroup1:change_list")));
        select.selectByVisibleText(monLevel);
        clickByIdAction("form1:basicTable:topActionsGroup1:button1");
        waitforBtnDisable("form1:basicTable:topActionsGroup1:button1");
        clickAndWait("form1:title:topButtons:saveButton");
        assertTrue(isElementSaveSuccessful("label_sun4","New values successfully saved."));

        //Test whether the level has changed to monLevel or not.
        //clickAndWait("treeForm:tree:configurations:server-config:monitor:monitor_link", TRIGGER_MONITORING_SERVICE);
        gotoDasPage();
        goToMonitoringServicePage(target, targetType);
        Select select1;
        if (isAll) {
            select1 = new Select(driver.findElement(By.id("form1:basicTable:rowGroup1:0:" + MONITOR_LEVEL_COL_ID + ":level")));
        } else {
            String id = getTableRowByValue("form1:basicTable", component, MONITOR_COMP_COL_ID);
            select1 = new Select(driver.findElement(By.id(id + MONITOR_LEVEL_COL_ID + ":level")));
        }
        assertTrue(select1.getFirstSelectedOption().getAttribute("value").equals(monLevel));
    }

    //Monitoring->Server related methods.
    private void goToMonitoringServerPage(String target, String targetType) {
        if (targetType.equals(TARGET_SERVER_TYPE)) {
            clickAndWait("treeForm:tree:applicationServer:applicationServer_link");
            clickAndWait("propertyForm:serverInstTabs:monitoring");
            waitForElementPresent("TtlTxt_sun4", "Application Monitoring");
            clickAndWait("propertyForm:serverInstTabs:monitoring:monitor_server");
            waitForElementPresent("TtlTxt_sun4", "Server Monitoring");
        } else if (targetType.equals(TARGET_STANDALONE_TYPE)) {
            clickAndWait("treeForm:tree:standaloneTreeNode:standaloneTreeNode_link");
            String prefix = getTableRowByValue("propertyForm:instancesTable", target, "col1");
            String clickId = prefix + "col1:link";
            clickByIdAction(clickId);
            clickAndWait("propertyForm:standaloneInstanceTabs:monitoring");
            waitForElementPresent("TtlTxt_sun4", "Application Monitoring");
            clickAndWait("propertyForm:standaloneInstanceTabs:monitoring:monitorServer");
            waitForElementPresent("TtlTxt_sun4", "Server Monitoring");
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
        final String jvmRuntimeData = "VmName";
        final String jvmThreadData = "DeadlockedThreads";

        final String dropDownId = "propertyForm:propertyContentPage:propertySheet:viewPropertySection:VsProp:View_list";
        final String gcCopyHeader = "Garbage Collectors Statistics : Copy";
        final String gcCopyData = "CollectionCount";
        final String gcMarkSweepHeader = "Garbage Collectors Statistics : MarkSweepCompact";
        final String gcMarkSweepData = "CollectionCount";
        final String threadHeader = "Thread Info Statistics";
        final String threadData = "ThreadState";

        setMonitorLevel("Jvm", MONITOR_LEVEL_HIGH, false, target, targetType);
        goToMonitoringServerPage(target, targetType);

        verifyMonitoringStat("jvmThreadInfoStats", threadInfoData, threadInfoHeader);
        verifyMonitoringClickStat("jvmMemStats", jvmMemData, jvmMemHeader);
        verifyMonitoringClickStat("osStats", jvmOsData, jvmOsHeader);
        verifyMonitoringClickStat("clStats", jvmClassLoadingData, jvmClassLoadingHeader);
        verifyMonitoringClickStat("comStats", jvmCompilationData, jvmCompilationHeader);
        verifyMonitoringClickStat("runtimeStats", jvmRuntimeData, jvmRuntimeHeader);
        verifyMonitoringClickStat("jvmThreadStats", jvmThreadData, jvmThreadHeader);

        Select select = new Select(driver.findElement(By.id(dropDownId)));
        select.selectByVisibleText("PS Scavenge");
        verifyMonitoringStat("gcStats", gcCopyData, gcCopyHeader);
        sleep(1000);
        Select select1 = new Select(driver.findElement(By.id(dropDownId)));
        select1.selectByVisibleText("PS MarkSweep");
        verifyMonitoringStat("gcStats", gcMarkSweepData, gcMarkSweepHeader);
        sleep(1000);
        Select select2 = new Select(driver.findElement(By.id(dropDownId)));
        select2.selectByVisibleText("thread-1");
        verifyMonitoringStat("jvmThreadInfoStats", threadData, threadHeader);

        setMonitorLevel("Jvm", MONITOR_LEVEL_OFF, false, target, targetType);
    }

    private void monitoringWebContainerStats(String target, String targetType) {
        final String webContainerServletHeader = "Web Container: Servlet Statistics";
        final String webContainerSessionHeader = "Web Container: Session Statistics";
        final String webContainerRequestHeader = "Web Container: Request Statistics";
        final String webContainerJspHeader = "Web Container: JSP Statistics";
        final String webContainerServletData = "ActiveServletsLoaded";
        final String webContainerSessionData = "SessionsTotal";
        final String webContainerRequestData = "ProcessingTime";
        final String webContainerJspData = "JspCount";

        setMonitorLevel("Web Container", MONITOR_LEVEL_HIGH, false, target, targetType);
        goToMonitoringServerPage(target, targetType);

        verifyMonitoringClickStat("servletsWeb", webContainerServletData, webContainerServletHeader);
        verifyMonitoringClickStat("sessionWeb", webContainerSessionData, webContainerSessionHeader);
        verifyMonitoringClickStat("requestWebStats", webContainerRequestData, webContainerRequestHeader);
        verifyMonitoringClickStat("jspWebStats", webContainerJspData, webContainerJspHeader);

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

//    //This seems a bug to the glassfish v4 need to be resolved!
//    private void monitoringSecurityStats(String target, String targetType) {
//        final String webAppSecurityDeploymentHeader = "Web Application Security Deployment Statistics";
//        final String webAppSecurityDeploymentData = "WebSecurityManagerCount";
//
////        setMonitorLevel("Security", MONITOR_LEVEL_HIGH, false, target, targetType);
//        goToMonitoringServerPage(target, targetType);
//        verifyMonitoringClickStat("webSecurity", webAppSecurityDeploymentData, webAppSecurityDeploymentHeader);
////        setMonitorLevel("Security", MONITOR_LEVEL_OFF, false, target, targetType);
//    }

    private void monitoringHttpServiceStats(String target, String targetType) {
        final String virtualStatsHeader = "Virtual Server Statistics : server";
        final String requestStatsHeader = "Request Statistics : server";
        final String virtualStatsData = "Id";
        final String requestStatsData = "MaxOpenConnections";

        final String fileCacheStatsHeader = "File Cache Statistics : admin-listener";
        final String keepAliveStatsHeader = "Keep Alive Statistics : admin-listener";
        final String connectionQueueStatsHeader = "Connection Queue Statistics : admin-listener";
        final String threadPoolStatsHeader = "Thread Pool Statistics : admin-listener";
        final String fileCacheStatsData = "ContentMissesCount";
        final String keepAliveStatsData = "CountTimeouts";
        final String connectionQueueStatsData = "CountQueued15MinutesAverage";
        final String threadPoolStatsData = "CoreThreads";

        String dropDownId = "propertyForm:propertyContentPage:propertySheet:viewPropertySection:VsProp:View_list";

        setMonitorLevel("Http Service", MONITOR_LEVEL_HIGH, false, target, targetType);
        goToMonitoringServerPage(target, targetType);

        Select select = new Select(driver.findElement(By.id(dropDownId)));
        select.selectByVisibleText("server");

        verifyMonitoringStat("virtualServerStats", virtualStatsData, virtualStatsHeader);
        verifyMonitoringStat("httpServiceStats", requestStatsData, requestStatsHeader);

        Select select1 = new Select(driver.findElement(By.id(dropDownId)));
        select1.selectByVisibleText("admin-listener");
        verifyMonitoringStat("fileCacheStats", fileCacheStatsData, fileCacheStatsHeader);
        verifyMonitoringClickStat("keepAliveStats", keepAliveStatsData, keepAliveStatsHeader);
        verifyMonitoringClickStat("connectionQueueStats", connectionQueueStatsData, connectionQueueStatsHeader);
        verifyMonitoringClickStat("threadPoolStats", threadPoolStatsData, threadPoolStatsHeader);

        setMonitorLevel("Http Service", MONITOR_LEVEL_OFF, false, target, targetType);
    }

    private void verifyMonitoringStat(String stat, String statData, String statHeader) {
        assertEquals(statData, driver.findElement(By.id("propertyForm:propertyContentPage:basicTable:" + stat+ ":0:col2")).getText());
        clickAndWait("propertyForm:propertyContentPage:basicTable:" + stat + ":_groupHeader:_groupPanelToggleButton:_groupPanelToggleButton_image");
        clickByIdAction("propertyForm:propertyContentPage:basicTable:" + stat + ":_groupHeader:_groupPanelToggleButton:_groupPanelToggleButton_image");
    }

    private void verifyMonitoringClickStat(String stat, String statData, String statHeader) {
        clickAndWait("propertyForm:propertyContentPage:basicTable:" + stat + ":_groupHeader:_groupPanelToggleButton:_groupPanelToggleButton_image");
        assertEquals(statData, driver.findElement(By.id("propertyForm:propertyContentPage:basicTable:" + stat+ ":0:col2")).getText());
        clickByIdAction("propertyForm:propertyContentPage:basicTable:" + stat + ":_groupHeader:_groupPanelToggleButton:_groupPanelToggleButton_image");
    }
    
    private void goToMonitoringApplicationsPage(String target, String targetType) {
        if (targetType.equals(TARGET_SERVER_TYPE)) {
            clickAndWait("treeForm:tree:applicationServer:applicationServer_link");
            clickAndWait("propertyForm:serverInstTabs:monitoring");
        } else if (targetType.equals(TARGET_STANDALONE_TYPE)) {
            clickAndWait("treeForm:tree:standaloneTreeNode:standaloneTreeNode_link");
            String prefix = getTableRowByValue("propertyForm:instancesTable", target, "col1");
            String clickId = prefix + "col1:link";
            clickByIdAction(clickId);
            clickAndWait("propertyForm:standaloneInstanceTabs:monitoring");
        }
    }

    private void ejbTimerMonitoring(String target, String targetType) {
        final String statDescription = "Number of times EJB remove method is called";
        final String appName = "ejb-timer-sessiontimerApp";

        deployApp("src/test/resources/ejb-timer-sessiontimerApp.ear", targetType, appName);
        setMonitorLevel("Web Container", MONITOR_LEVEL_HIGH, false, target, targetType);
        setMonitorLevel("Ejb Container", MONITOR_LEVEL_HIGH, false, target, targetType);
        goToMonitoringApplicationsPage(target, targetType);

        Select select1 = new Select(driver.findElement(By.id("propertyForm:propertyContentPage:propertySheet:viewPropertySection:ApplicationProp:View_list")));
        select1.selectByVisibleText("ejb-timer-sessiontimer-ejb.jar");
        sleep(5000);
        Select select = new Select(driver.findElement(By.id(MONITORING_APPLICATIONS_COMPONENT_DROPDOWN_ID)));
        select.selectByVisibleText("TimerSingleton");
        assertEquals(statDescription, driver.findElement(By.id("propertyForm:propertyContentPage:appsTable:singletonBeanAppStats:0:col7")).getText());

        setMonitorLevel("Web Container", MONITOR_LEVEL_OFF, false, target, targetType);
        setMonitorLevel("Ejb Container", MONITOR_LEVEL_OFF, false, target, targetType);
        undeployApp(appName, targetType);
    }

    public void appScopedResourcesMonitoring(String target, String targetType, String resName) {
        final String statDescription = "Number of potential connection leaks";

        setMonitorLevel(null, MONITOR_LEVEL_HIGH, true, target, targetType);
        gotoDasPage();
        goToMonitoringApplicationsPage(target, targetType);

        Select select = new Select(driver.findElement(By.id(MONITORING_APPLICATIONS_APPLICATION_DROPDOWN_ID)));
        select.selectByVisibleText("JavaEEApp-war.war");
        Select select1 = new Select(driver.findElement(By.id(MONITORING_APPLICATIONS_COMPONENT_DROPDOWN_ID)));
        select1.selectByVisibleText(resName);
        assertEquals(statDescription, driver.findElement(By.id("propertyForm:propertyContentPage:appsTable:appScopedResStats:0:col7")).getText());

        setMonitorLevel(null, MONITOR_LEVEL_OFF, true, target, targetType);
    }

    private void statefulAndStatelessBeanMonitoring(String target, String targetType) {
        final String statefulStatDescription = "Number of times EJB remove method is called";
        final String statelessStatDescription = "Provides a count value reflecting the number of passivations for a StatefulSessionBean from the bean cache that succeeded";
        String applicationName = "ejb-ejb30-hello-sessionApp";

        deployApp("src/test/resources/ejb-ejb30-hello-sessionApp.ear", targetType, applicationName);
        setMonitorLevel("Web Container", MONITOR_LEVEL_HIGH, false, target, targetType);
        setMonitorLevel("Ejb Container", MONITOR_LEVEL_HIGH, false, target, targetType);
        goToMonitoringApplicationsPage(target, targetType);

        Select select2 = new Select(driver.findElement(By.id("propertyForm:propertyContentPage:propertySheet:viewPropertySection:ApplicationProp:View_list")));
        select2.selectByVisibleText("ejb-ejb30-hello-session-ejb.jar");
        sleep(5000);
        Select select = new Select(driver.findElement(By.id(MONITORING_APPLICATIONS_COMPONENT_DROPDOWN_ID)));
        select.selectByVisibleText("SfulEJB");
        assertEquals(statefulStatDescription, driver.findElement(By.id("propertyForm:propertyContentPage:appsTable:sfullStats:0:col7")).getText());

        Select select1 = new Select(driver.findElement(By.id(MONITORING_APPLICATIONS_COMPONENT_DROPDOWN_ID)));
        select1.selectByVisibleText("bean-cache");
        assertEquals(statelessStatDescription, driver.findElement(By.id("propertyForm:propertyContentPage:appsTable:ejbCacheStats:0:col7")).getText());

        setMonitorLevel("Web Container", MONITOR_LEVEL_OFF, false, target, targetType);
        setMonitorLevel("Ejb Container", MONITOR_LEVEL_OFF, false, target, targetType);
        undeployApp(applicationName, targetType);
    }

    private void deployApp(String appLocation, String target, String appName) {
        clickAndWait("treeForm:tree:applications:applications_link");
        int initCount = getTableRowCount("propertyForm:deployTable");
        if(initCount != 0){
            clickByIdAction("propertyForm:deployTable:_tableActionsTop:_selectMultipleButton:_selectMultipleButton_image");
            clickByIdAction("propertyForm:deployTable:topActionsGroup1:button1");
            closeAlertAndGetItsText();
            waitForAlertProcess("modalBody");
            waitforBtnDisable("propertyForm:deployTable:topActionsGroup1:button1");
        }
        
        //start to deploy applications
        driver.get(baseUrl + "common/applications/uploadFrame.jsf");
        driver.findElement(By.id("form:sheet1:section1:prop1:uploadRdBtn:uploadRdBtn_label"));
        File war = new File(appLocation);
        driver.findElement(By.id("form:sheet1:section1:prop1:fileupload")).sendKeys(war.getAbsoluteFile().toString());
        //waitForCondition("document.getElementById('form:war:psection:nameProp:appName').value == '" + appName + "'", 300000);
        assertEquals(appName, getValue("form:war:psection:nameProp:appName", "value"));
        clickAndWait("form:title:topButtons:uploadButton");
        
        gotoDasPage();
        clickAndWait("treeForm:tree:applications:applications_link");
        String prefix = getTableRowByValue("propertyForm:deployTable", appName, "col1");
        assertEquals(appName, getText(prefix + "col1:link"));
    }

    private void undeployApp(String applicationName, String target) {
        clickAndWait("treeForm:tree:applications:applications_link");
        String prefix = getTableRowByValue("propertyForm:deployTable", applicationName, "col1");
        String selectId = prefix + "col0:select";
        clickByIdAction(selectId);
        clickAndWait("propertyForm:deployTable:topActionsGroup1:button1");
        closeAlertAndGetItsText();
        waitForAlertProcess("modalBody");
        waitforBtnDisable("propertyForm:deployTable:topActionsGroup1:button1");
    }
}
