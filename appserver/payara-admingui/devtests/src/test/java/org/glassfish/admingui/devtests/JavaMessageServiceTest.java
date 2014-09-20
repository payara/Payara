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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class JavaMessageServiceTest extends BaseSeleniumTestClass {

    public static final String DEFAULT_JMS_HOST = "default_JMS_host";
    private static final String TRIGGER_GENERAL_INFORMATION = "i18n.instance.GeneralTitle";
    private static final String TRIGGER_JMS_SERVICE = "i18njms.jms.PageHelp";
    private static final String TRIGGER_JMS_HOSTS = "i18njms.jmsHosts.ListPageHelp";
    private static final String TRIGGER_NEW_JMS_HOST = "i18njms.newJmsHost.NewJmsHost";
    private static final String TRIGGER_JMS_PHYSICAL_DESTINATIONS = "i18njms.jmsPhysDestinations.pageHelp";
    private static final String TRIGGER_NEW_JMS_PHYSICAL_DESTINATION = "i18njms.jmsPhysDestinations.newPageTitle";
    private static final String TRIGGER_EDIT_JMS_PHYSICAL_DESTINATION = "i18njms.jmsPhysDestinations.editPageTitle";
    private static final String TRIGGER_FLUSH = "i18njms.jmsPhysDestinations.purged";

    @Test
    public void testJmsService() {
        final String timeout = Integer.toString(generateRandomNumber(90));
        final String interval = Integer.toString(generateRandomNumber(10));
        final String attempts = Integer.toString(generateRandomNumber(10));

        clickAndWait("treeForm:tree:configurations:server-config:jmsConfiguration:jmsConfiguration_link", TRIGGER_JMS_SERVICE);
        setFieldValue("propertyForm:propertyContentPage:propertySheet:propertSectionTextField:timeoutProp:Timeout", timeout);
        setFieldValue("propertyForm:propertyContentPage:propertySheet:propertSectionTextField:intervalProp:Interval", interval);
        setFieldValue("propertyForm:propertyContentPage:propertySheet:propertSectionTextField:attemptsProp:Attempts", attempts);
        selectDropdownOption("propertyForm:propertyContentPage:propertySheet:propertSectionTextField:behaviorProp:Behavior", "priority");

        int count = addTableRow("propertyForm:propertyContentPage:basicTable", "propertyForm:propertyContentPage:basicTable:topActionsGroup1:addSharedTableButton");
        setFieldValue("propertyForm:propertyContentPage:basicTable:rowGroup1:0:col2:col1St", "property" + generateRandomString());
        setFieldValue("propertyForm:propertyContentPage:basicTable:rowGroup1:0:col3:col1St", "value");
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton", TRIGGER_NEW_VALUES_SAVED);

        clickAndWait("treeForm:tree:configurations:server-config:jmsConfiguration:jmsHosts:jmsHosts_link", TRIGGER_JMS_HOSTS);
        clickAndWait("treeForm:tree:configurations:server-config:jmsConfiguration:jmsConfiguration_link", TRIGGER_JMS_SERVICE);

        assertEquals(timeout, getFieldValue("propertyForm:propertyContentPage:propertySheet:propertSectionTextField:timeoutProp:Timeout"));
        assertEquals(interval, getFieldValue("propertyForm:propertyContentPage:propertySheet:propertSectionTextField:intervalProp:Interval"));
        assertEquals(attempts, getFieldValue("propertyForm:propertyContentPage:propertySheet:propertSectionTextField:attemptsProp:Attempts"));
        assertTableRowCount("propertyForm:propertyContentPage:basicTable", count);
    }

    @Test
    public void testJmsHosts() {
        String hostText = "host" + generateRandomString();
        String host = "somemachine" + generateRandomNumber(1000);
        String port = Integer.toString(generateRandomNumber(32768));

        clickAndWait("treeForm:tree:configurations:server-config:jmsConfiguration:jmsHosts:jmsHosts_link", TRIGGER_JMS_HOSTS);
        clickAndWait("propertyForm:configs:topActionsGroup1:newButton", TRIGGER_NEW_JMS_HOST);
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:JmsHostTextProp:JmsHostText", hostText);
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:HostProp:Host", host);
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:PortProp:Port", port);
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:AdminUserProp:AdminUser", "admin");
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:newPasswordProp:NewPassword", "admin");
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:confirmPasswordProp:ConfirmPassword", "admin");
        clickAndWait("propertyForm:propertyContentPage:topButtons:newButton", TRIGGER_NEW_VALUES_SAVED);
        clickAndWait(this.getLinkIdByLinkText("propertyForm:configs", hostText), "Edit JMS Host");
        assertTrue(isTextPresent(hostText));
        assertEquals(host, getFieldValue("propertyForm:propertySheet:propertSectionTextField:HostProp:Host"));
        clickAndWait("propertyForm:propertyContentPage:topButtons:cancelButton", TRIGGER_JMS_HOSTS);
        deleteRow("propertyForm:configs:topActionsGroup1:button1", "propertyForm:configs", hostText, "col0", "colName");
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
        clickAndWait(LINK_HOSTS, TRIGGER_JMS_HOSTS);
        clickAndWait("propertyForm:configs:topActionsGroup1:newButton", TRIGGER_NEW_JMS_HOST);
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:JmsHostTextProp:JmsHostText", hostText);
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:HostProp:Host", "localhost");
        clickAndWait("propertyForm:propertyContentPage:topButtons:newButton", TRIGGER_NEW_VALUES_SAVED);

        // Verify that the host is not visible to the DAS
        reset();
        clickAndWait("treeForm:tree:configurations:server-config:jmsConfiguration:jmsHosts:jmsHosts_link", TRIGGER_JMS_HOSTS);
        assertFalse(isTextPresent(hostText));

        // Delete the default host for the SA instance
        reset();
        clickAndWait(LINK_HOSTS, TRIGGER_JMS_HOSTS);
        deleteRow("propertyForm:configs:topActionsGroup1:button1", "propertyForm:configs", DEFAULT_JMS_HOST, "col0", "colName");

        // Verify that the DAS still has the default JMS Host
        reset();
        clickAndWait("treeForm:tree:configurations:server-config:jmsConfiguration:jmsHosts:jmsHosts_link", TRIGGER_JMS_HOSTS);
        assertTrue(isTextPresent(DEFAULT_JMS_HOST));

        // Delete SA config's new host
        reset();
        clickAndWait(LINK_HOSTS, TRIGGER_JMS_HOSTS);
        deleteRow("propertyForm:configs:topActionsGroup1:button1", "propertyForm:configs", hostText, "col0", "colName");

        sat.deleteAllStandaloneInstances();
    }

    @Test
    public void testJmsPhysicalDestinations() {
        final String name = "dest" + generateRandomString();
        final String maxUnconsumed = Integer.toString(generateRandomNumber(100));
        final String maxMessageSize = Integer.toString(generateRandomNumber(100));
        final String maxTotalMemory = Integer.toString(generateRandomNumber(100));
        final String maxProducers = Integer.toString(generateRandomNumber(500));
        final String consumerFlowLimit = Integer.toString(generateRandomNumber(5000));

        clickAndWait("treeForm:tree:applicationServer:applicationServer_link", TRIGGER_GENERAL_INFORMATION);
        clickAndWait("propertyForm:serverInstTabs:jmsPhysDest", TRIGGER_JMS_PHYSICAL_DESTINATIONS);
        clickAndWait("propertyForm:configs:topActionsGroup1:newButton", TRIGGER_NEW_JMS_PHYSICAL_DESTINATION);

        setFieldValue("jmsPhysDestForm:propertySheet:propertSectionTextField:NameTextProp:NameText", name);
        setFieldValue("jmsPhysDestForm:propertySheet:propertSectionTextField:maxNumMsgsProp:maxNumMsgs", maxUnconsumed);
        setFieldValue("jmsPhysDestForm:propertySheet:propertSectionTextField:maxBytesPerMsgProp:maxBytesPerMsg", maxMessageSize);
        setFieldValue("jmsPhysDestForm:propertySheet:propertSectionTextField:maxTotalMsgBytesProp:maxTotalMsgBytes", maxTotalMemory);
        selectDropdownOption("jmsPhysDestForm:propertySheet:propertSectionTextField:limitBehaviorProp:Type", "i18njms.jmsPhysDestinations.REMOVE_LOW_PRIORITY");
        setFieldValue("jmsPhysDestForm:propertySheet:propertSectionTextField:maxNumProducersProp:maxNumProducers", maxProducers);
        setFieldValue("jmsPhysDestForm:propertySheet:propertSectionTextField:consumerFlowLimitProp:consumerFlowLimit", consumerFlowLimit);
        selectDropdownOption("jmsPhysDestForm:propertySheet:propertSectionTextField:useDmqProp:useDmq", "i18n.common.false");
        selectDropdownOption("jmsPhysDestForm:propertySheet:propertSectionTextField:validateSchemaProp:validateXMLSchemaEnabled", "i18n.common.true");
        clickAndWait("jmsPhysDestForm:propertyContentPage:topButtons:newButton", TRIGGER_JMS_PHYSICAL_DESTINATIONS);

        clickAndWait(getLinkIdByLinkText("propertyForm:configs", name), TRIGGER_EDIT_JMS_PHYSICAL_DESTINATION);

        assertTrue(isTextPresent(name));
        assertEquals(maxUnconsumed, getFieldValue("jmsPhysDestForm:propertySheet:propertSectionTextField:maxNumMsgsProp:maxNumMsgs"));
        assertEquals(maxMessageSize, getFieldValue("jmsPhysDestForm:propertySheet:propertSectionTextField:maxBytesPerMsgProp:maxBytesPerMsg"));
        assertEquals(maxTotalMemory, getFieldValue("jmsPhysDestForm:propertySheet:propertSectionTextField:maxTotalMsgBytesProp:maxTotalMsgBytes"));

        // TODO: These options do not seem to be be supported by the backend. Passing these props to the CLI does not affect its value. Disabling for now.
        // FIXME
//        assertEquals("REMOVE_LOW_PRIORITY", getFieldValue("jmsPhysDestForm:propertySheet:propertSectionTextField:limitBehaviorProp:Type"));
//        assertEquals(maxProducers, getFieldValue("jmsPhysDestForm:propertySheet:propertSectionTextField:maxNumProducersProp:maxNumProducers"));
//        assertEquals("false", getFieldValue("jmsPhysDestForm:propertySheet:propertSectionTextField:useDmqProp:useDmq"));

        assertEquals(consumerFlowLimit, getFieldValue("jmsPhysDestForm:propertySheet:propertSectionTextField:consumerFlowLimitProp:consumerFlowLimit"));
        assertEquals("true", getFieldValue("jmsPhysDestForm:propertySheet:propertSectionTextField:validateSchemaProp:validateXMLSchemaEnabled"));
        clickAndWait("jmsPhysDestForm:propertyContentPage:topButtons:cancelButton", TRIGGER_JMS_PHYSICAL_DESTINATIONS);

        this.selectTableRowByValue("propertyForm:configs", name);
        clickAndWait("propertyForm:configs:topActionsGroup1:flushButton", TRIGGER_FLUSH);
//        this.selectTableRowByValue("propertyForm:configs", name); // Deselect row. This is ugly, but will have to stay this way for now

        reset();
        clickAndWait("treeForm:tree:applicationServer:applicationServer_link", TRIGGER_GENERAL_INFORMATION);
        clickAndWait("propertyForm:serverInstTabs:jmsPhysDest", TRIGGER_JMS_PHYSICAL_DESTINATIONS);
        deleteRow("propertyForm:configs:topActionsGroup1:deleteButton", "propertyForm:configs", name);
    }

    @Test
    public void testMasterBroker() {
        ClusterTest ct = new ClusterTest();
        try {
            final String FIELD_MASTER_BROKER = "propertyForm:propertyContentPage:propertySheet:propertSectionTextField:maseterBrokerProp:MasterBroker";

            String clusterName = "clusterName" + generateRandomString();
            ct.deleteAllClusters();
            final String instance1 = clusterName + generateRandomString();
            final String instance2 = clusterName + generateRandomString();
            ct.createCluster(clusterName, instance1, instance2);
            final String ELEMENT_JMS_LINK = "treeForm:tree:configurations:" + clusterName + "-config:jmsConfiguration:jmsConfiguration_link";

            clickAndWait(ELEMENT_JMS_LINK, TRIGGER_JMS_SERVICE);
            selectDropdownOption(FIELD_MASTER_BROKER, instance2);

            clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton", TRIGGER_NEW_VALUES_SAVED);
            reset();
            clickAndWait(ELEMENT_JMS_LINK, TRIGGER_JMS_SERVICE);

            assertEquals(instance2, getFieldValue(FIELD_MASTER_BROKER));
        } finally {
            ct.deleteAllClusters();
        }
    }
}
