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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.Select;

public class JavaMessageServiceTest extends BaseSeleniumTestClass {

    public static final String DEFAULT_JMS_HOST = "default_JMS_host";

    @Test
    public void testJmsService() {
        gotoDasPage();
        final String timeout = Integer.toString(generateRandomNumber(90));
        final String interval = Integer.toString(generateRandomNumber(10));
        final String attempts = Integer.toString(generateRandomNumber(10));

        clickAndWait("treeForm:tree:configurations:server-config:jmsConfiguration:jmsConfiguration_link");
        setFieldValue("propertyForm:propertyContentPage:propertySheet:propertSectionTextField:timeoutProp:Timeout", timeout);
        setFieldValue("propertyForm:propertyContentPage:propertySheet:propertSectionTextField:intervalProp:Interval", interval);
        setFieldValue("propertyForm:propertyContentPage:propertySheet:propertSectionTextField:attemptsProp:Attempts", attempts);
        Select select = new Select(driver.findElement(By.id("propertyForm:propertyContentPage:propertySheet:propertSectionTextField:behaviorProp:Behavior")));
        select.selectByVisibleText("priority");

        int count = addTableRow("propertyForm:propertyContentPage:basicTable", "propertyForm:propertyContentPage:basicTable:topActionsGroup1:addSharedTableButton");
        sleep(500);
        setFieldValue("propertyForm:propertyContentPage:basicTable:rowGroup1:0:col2:col1St", "property" + generateRandomString());
        sleep(500);
        setFieldValue("propertyForm:propertyContentPage:basicTable:rowGroup1:0:col3:col1St", "value");
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton");
        assertTrue(isElementSaveSuccessful("label_sun4","New values successfully saved."));
        
        gotoDasPage();
        clickAndWait("treeForm:tree:configurations:server-config:jmsConfiguration:jmsConfiguration_link");

        assertEquals(timeout, getValue("propertyForm:propertyContentPage:propertySheet:propertSectionTextField:timeoutProp:Timeout", "value"));
        assertEquals(interval, getValue("propertyForm:propertyContentPage:propertySheet:propertSectionTextField:intervalProp:Interval", "value"));
        assertEquals(attempts, getValue("propertyForm:propertyContentPage:propertySheet:propertSectionTextField:attemptsProp:Attempts", "value"));
        assertTableRowCount("propertyForm:propertyContentPage:basicTable", count);
        
        //delete the property used to test
        clickByIdAction("propertyForm:propertyContentPage:basicTable:_tableActionsTop:_selectMultipleButton:_selectMultipleButton_image");
        clickByIdAction("propertyForm:propertyContentPage:basicTable:topActionsGroup1:button1");
        waitforBtnDisable("propertyForm:propertyContentPage:basicTable:topActionsGroup1:button1");
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton");
        assertTrue(isElementSaveSuccessful("label_sun4","New values successfully saved."));
    }

    @Test
    public void testJmsHosts() {
        gotoDasPage();
        String hostText = "host" + generateRandomString();
        String host = "somemachine" + generateRandomNumber(1000);
        String port = Integer.toString(generateRandomNumber(32768));

        clickAndWait("treeForm:tree:configurations:server-config:jmsConfiguration:jmsHosts:jmsHosts_link");
        clickAndWait("propertyForm:configs:topActionsGroup1:newButton");
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:JmsHostTextProp:JmsHostText", hostText);
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:HostProp:Host", host);
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:PortProp:Port", port);
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:AdminUserProp:AdminUser", "admin");
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:newPasswordProp:NewPassword", "admin");
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:confirmPasswordProp:ConfirmPassword", "admin");
        clickAndWait("propertyForm:propertyContentPage:topButtons:newButton");
        
        String prefix = getTableRowByValue("propertyForm:configs", hostText, "colName");
        assertEquals(hostText, getText(prefix + "colName:link"));

        String clickId = prefix + "colName:link";
        clickByIdAction(clickId);
        assertEquals(host, getValue("propertyForm:propertySheet:propertSectionTextField:HostProp:Host", "value"));
        clickAndWait("propertyForm:propertyContentPage:topButtons:cancelButton");
        
        //delete related jms host
        String deleteRow = prefix + "col0:select";
        clickByIdAction(deleteRow);
        clickByIdAction("propertyForm:configs:topActionsGroup1:button1");
        closeAlertAndGetItsText();
        waitForAlertProcess("modalBody");
    }

    @Test
    public void testJmsHostInNonServerConfig() {
        String hostText = "host" + generateRandomString();
        String instanceName = "in" + generateRandomString();
        final String LINK_HOSTS = "treeForm:tree:configurations:" + instanceName + "-config:jmsConfiguration:jmsHosts:jmsHosts_link";

        StandaloneTest sat = new StandaloneTest();
        sat.createStandAloneInstance(instanceName);
        sat.startInstance(instanceName);

        // Create new JMS Host for the standalone instance's config
        clickAndWait(LINK_HOSTS);
        clickAndWait("propertyForm:configs:topActionsGroup1:newButton");
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:JmsHostTextProp:JmsHostText", hostText);
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:HostProp:Host", "localhost");
        clickAndWait("propertyForm:propertyContentPage:topButtons:newButton");

        // Delete the default host for the SA instance
        gotoDasPage();
        clickAndWait(LINK_HOSTS);
        String prefix = getTableRowByValue("propertyForm:configs", DEFAULT_JMS_HOST, "colName");
        String deleteRow = prefix + "col0:select";
        clickByIdAction(deleteRow);
        clickByIdAction("propertyForm:configs:topActionsGroup1:button1");
        closeAlertAndGetItsText();
        waitForAlertProcess("modalBody");

        // Verify that the DAS still has the default JMS Host
        gotoDasPage();
        clickAndWait("treeForm:tree:configurations:server-config:jmsConfiguration:jmsHosts:jmsHosts_link");
        assertEquals(DEFAULT_JMS_HOST, getText(prefix + "colName:link"));

        // Delete SA config's new host
        gotoDasPage();
        clickAndWait(LINK_HOSTS);
        String prefix1 = getTableRowByValue("propertyForm:configs", hostText, "colName");
        String deleteRow1 = prefix1 + "col0:select";
        clickByIdAction(deleteRow1);
        clickByIdAction("propertyForm:configs:topActionsGroup1:button1");
        closeAlertAndGetItsText();
        waitForAlertProcess("modalBody");

        sat.deleteAllStandaloneInstances();
    }

//    //This tests need to be rewrite and retested after the issue has been resolved
//    @Test
//    public void testJmsPhysicalDestinations() {
//        gotoDasPage();
//        final String name = "dest" + generateRandomString();
//        final String maxUnconsumed = Integer.toString(generateRandomNumber(100));
//        final String maxMessageSize = Integer.toString(generateRandomNumber(100));
//        final String maxTotalMemory = Integer.toString(generateRandomNumber(100));
//        final String maxProducers = Integer.toString(generateRandomNumber(500));
//        final String consumerFlowLimit = Integer.toString(generateRandomNumber(5000));
//
//        clickAndWait("treeForm:tree:applicationServer:applicationServer_link");
//        clickAndWait("propertyForm:serverInstTabs:jmsPhysDest");
//        clickAndWait("propertyForm:configs:topActionsGroup1:newButton");
//
//        setFieldValue("jmsPhysDestForm:propertySheet:propertSectionTextField:NameTextProp:NameText", name);
//        setFieldValue("jmsPhysDestForm:propertySheet:propertSectionTextField:maxNumMsgsProp:maxNumMsgs", maxUnconsumed);
//        setFieldValue("jmsPhysDestForm:propertySheet:propertSectionTextField:maxBytesPerMsgProp:maxBytesPerMsg", maxMessageSize);
//        setFieldValue("jmsPhysDestForm:propertySheet:propertSectionTextField:maxTotalMsgBytesProp:maxTotalMsgBytes", maxTotalMemory);
//        Select select = new Select(driver.findElement(By.id("jmsPhysDestForm:propertySheet:propertSectionTextField:typeProp:type")));
//        select.selectByVisibleText("javax.jms.Queue");
//        setFieldValue("jmsPhysDestForm:propertySheet:propertSectionTextField:maxNumProducersProp:maxNumProducers", maxProducers);
//        setFieldValue("jmsPhysDestForm:propertySheet:propertSectionTextField:consumerFlowLimitProp:consumerFlowLimit", consumerFlowLimit);
//        Select select1 = new Select(driver.findElement(By.id("jmsPhysDestForm:propertySheet:propertSectionTextField:useDmqProp:useDmq")));
//        select1.selectByVisibleText("false");
//        Select select2 = new Select(driver.findElement(By.id("jmsPhysDestForm:propertySheet:propertSectionTextField:validateSchemaProp:validateXMLSchemaEnabled")));
//        select2.selectByVisibleText("true");
//        clickAndWait("jmsPhysDestForm:propertyContentPage:topButtons:newButton");
//
//        String prefix = getTableRowByValue("propertyForm:configs", name, "col1");
//        assertEquals(name, getText(prefix + "col1:nameCol"));
//
//        String clickId = prefix + "col1:nameCol";
//        clickByIdAction(clickId);
//
//        assertEquals(maxUnconsumed, getValue("jmsPhysDestForm:propertySheet:propertSectionTextField:maxNumMsgsProp:maxNumMsgs", "value"));
//        assertEquals(maxMessageSize, getValue("jmsPhysDestForm:propertySheet:propertSectionTextField:maxBytesPerMsgProp:maxBytesPerMsg", "value"));
//        assertEquals(maxTotalMemory, getValue("jmsPhysDestForm:propertySheet:propertSectionTextField:maxTotalMsgBytesProp:maxTotalMsgBytes", "value"));
//
//        assertEquals(consumerFlowLimit, getValue("jmsPhysDestForm:propertySheet:propertSectionTextField:consumerFlowLimitProp:consumerFlowLimit", "value"));
//        assertEquals("true", getValue("jmsPhysDestForm:propertySheet:propertSectionTextField:validateSchemaProp:validateXMLSchemaEnabled", "value"));
//        clickAndWait("jmsPhysDestForm:propertyContentPage:topButtons:cancelButton");
//
//        String selectId = prefix + "col0:select";
//        clickByIdAction(selectId);;
//        clickAndWait("propertyForm:configs:topActionsGroup1:flushButton");
//        
//        gotoDasPage();
//        clickAndWait("treeForm:tree:applicationServer:applicationServer_link");
//        clickAndWait("propertyForm:serverInstTabs:jmsPhysDest");
//        deleteRow("propertyForm:configs:topActionsGroup1:deleteButton", "propertyForm:configs", name);
//    }

    @Test
    public void testMasterBroker() {
        ClusterTest ct = new ClusterTest();
        try {
            final String FIELD_MASTER_BROKER = "propertyForm:propertyContentPage:propertySheet:propertSectionTextField:maseterBrokerProp:MasterBroker";

            String clusterName = "clusterName" + generateRandomString();
            ct.deleteAllCluster();
            final String instance1 = clusterName + generateRandomString();
            final String instance2 = clusterName + generateRandomString();
            ct.createCluster(clusterName, instance1, instance2);
            final String ELEMENT_JMS_LINK = "treeForm:tree:configurations:" + clusterName + "-config:jmsConfiguration:jmsConfiguration_link";

            clickAndWait(ELEMENT_JMS_LINK);
            Select select = new Select(driver.findElement(By.id(FIELD_MASTER_BROKER)));
            select.selectByVisibleText(instance2);

            clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton");
            clickAndWait(ELEMENT_JMS_LINK);

            assertEquals(instance2, getValue(FIELD_MASTER_BROKER, "value"));
        } finally {
            ct.deleteAllCluster();
        }
    }
}
