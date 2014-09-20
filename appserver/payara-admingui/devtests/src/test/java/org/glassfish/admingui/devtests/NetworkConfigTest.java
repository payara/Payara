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

import java.util.ArrayList;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class NetworkConfigTest extends BaseSeleniumTestClass {
    private static final String TRIGGER_NETWORK_LISTENERS = "i18n_web.grizzly.networkListenersPageTitleHelp";
    private static final String TRIGGER_NEW_NETWORK_LISTENER = "i18n_web.grizzly.networkListenerNewPageTitle";
    private static final String TRIGGER_PROTOCOLS = "Click New to define a new protocol.";
    private static final String TRIGGER_NEW_PROTOCOL = "i18n_web.grizzly.protocolNewPageTitleHelp";
    private static final String TRIGGER_TRANSPORTS = "i18n_web.transport.listPageTitleHelp";
    private static final String TRIGGER_NEW_TRANSPORT = "i18n_web.transport.newPageTitleHelp";
    private static final String TRIGGER_CONFIGURATION = "i18nc.configurations.PageTitleHelp";
    private static final String TRIGGER_NEW_CONFIGURATION = "i18nc.configurations.NewPageTitle";
    ArrayList<String> list = new ArrayList(); {list.add("server-config"); list.add("new-config");}

    @Test
    public void testAddingNetworkListener() {
        final String listenerName = "listener"+generateRandomString();
        createConfig("new-config");
        for (String configName : list) {
            clickAndWait("treeForm:tree:configurations:" + configName + ":networkConfig:networkListeners:networkListeners_link", TRIGGER_NETWORK_LISTENERS);
            clickAndWait("propertyForm:configs:topActionsGroup1:newButton", TRIGGER_NEW_NETWORK_LISTENER);
            setFieldValue("propertyForm:propertySheet:propertSectionTextField:nameNew:name", listenerName);
            pressButton("propertyForm:propertySheet:propertSectionTextField:prop1:existingRdBtn");
            selectDropdownOption("propertyForm:propertySheet:propertSectionTextField:prop1:protocoldw", "http-listener-1");
            setFieldValue("propertyForm:propertySheet:propertSectionTextField:port:port", "1234");
            clickAndWait("propertyForm:propertyContentPage:topButtons:newButton", TRIGGER_NETWORK_LISTENERS);
            assertTrue(isTextPresent(listenerName));

            clickAndWait(getLinkIdByLinkText("propertyForm:configs", listenerName), "Edit Network Listener");

            assertTrue(isTextPresent(listenerName));
            assertTrue(isTextPresent("http-listener-1"));

            assertEquals("1234", getFieldValue("propertyForm:propertySheet:propertSectionTextField:port:port"));
            clickAndWait("propertyForm:propertyContentPage:topButtons:cancelButton", TRIGGER_NETWORK_LISTENERS);

            deleteRow("propertyForm:configs:topActionsGroup1:button1", "propertyForm:configs", listenerName);
        }
    }

    public void createConfig(String configName) {
        clickAndWait("treeForm:tree:configurations:configurations_link", TRIGGER_CONFIGURATION);
        if (!isTextPresent("new-config")) {
            clickAndWait("propertyForm:configs:topActionsGroup1:newButton", TRIGGER_NEW_CONFIGURATION);
            setFieldValue("propertyForm:propertySheet:propertSectionTextField:NameProp:Name", configName);
            clickAndWait("propertyForm:propertyContentPage:topButtons:okButton", TRIGGER_CONFIGURATION);
            assertTrue(isTextPresent(configName));
        }
    }

    @Test
    public void testAddingTransport() {
        final String transportName = "transport"+generateRandomString();
        createConfig("new-config");
        for (String configName : list) {
            clickAndWait("treeForm:tree:configurations:" + configName + ":networkConfig:transports:transports_link", TRIGGER_TRANSPORTS);
            clickAndWait("propertyForm:configs:topActionsGroup1:newButton", TRIGGER_NEW_TRANSPORT);
            setFieldValue("propertyForm:propertySheet:propertSectionTextField:IdTextProp:IdText", transportName);
            selectDropdownOption("propertyForm:propertySheet:propertSectionTextField:ByteBufferType:ByteBufferType", "DIRECT");
            setFieldValue("propertyForm:propertySheet:propertSectionTextField:BufferSizeBytes:BufferSizeBytes", "1000");
            setFieldValue("propertyForm:propertySheet:propertSectionTextField:AcceptorThreads:AcceptorThreads", "-1");
            clickAndWait("propertyForm:propertyContentPage:topButtons:newButton", TRIGGER_TRANSPORTS);
            assertTrue(isTextPresent(transportName));

            clickAndWait(getLinkIdByLinkText("propertyForm:configs", transportName), "Edit Transport");
            assertTrue(isTextPresent(transportName));
            assertTrue(isTextPresent("DIRECT"));
            assertEquals("1000", getFieldValue("propertyForm:propertySheet:propertSectionTextField:BufferSizeBytes:BufferSizeBytes"));
            clickAndWait("propertyForm:propertyContentPage:topButtons:cancelButton", TRIGGER_TRANSPORTS);

            deleteRow("propertyForm:configs:topActionsGroup1:button1", "propertyForm:configs", transportName);
        }
    }

    @Test
    public void testAddingProtocol() {
        final String protocol = "protocol"+generateRandomString();
        final String maxAge = Integer.toString(generateRandomNumber(60));
        final String maxCacheSizeBytes = Integer.toString(generateRandomNumber(10485760));
        final String maxFile = Integer.toString(generateRandomNumber(2048));
        final String maxC = Integer.toString(generateRandomNumber(512));
        final String timeoutSeconds = Integer.toString(generateRandomNumber(60));
        final String connectionUploadTimeout = Integer.toString(generateRandomNumber(600000));
        final String requestTimeoutSeconds = Integer.toString(generateRandomNumber(60));
        final String sendBsize = Integer.toString(generateRandomNumber(16384));
        final String headerBLength = Integer.toString(generateRandomNumber(16384));
        final String maxPostSize = Integer.toString(generateRandomNumber(2097152));
        final String compressableMime = Integer.toString(generateRandomNumber(4096));

        createConfig("new-config");
        for (String configName : list) {
            clickAndWait("treeForm:tree:configurations:" + configName +":networkConfig:protocols:protocols_link", TRIGGER_PROTOCOLS);
            clickAndWait("propertyForm:configs:topActionsGroup1:newButton", TRIGGER_NEW_PROTOCOL);
            setFieldValue("propertyForm:propertySheet:propertSectionTextField:nameNew:name", protocol);
            setFieldValue("propertyForm:propertySheet:fileTextField:maxAge:maxAge", maxAge);
            setFieldValue("propertyForm:propertySheet:fileTextField:maxCacheSizeBytes:maxCacheSizeBytes", maxCacheSizeBytes);
            setFieldValue("propertyForm:propertySheet:fileTextField:maxFile:maxFile", maxFile);
            setFieldValue("propertyForm:propertySheet:httpTextField:maxC:maxC", maxC);
            setFieldValue("propertyForm:propertySheet:httpTextField:TimeoutSeconds:TimeoutSeconds", timeoutSeconds);
            setFieldValue("propertyForm:propertySheet:httpTextField:connectionUploadTimeout:connectionUploadTimeout", connectionUploadTimeout);
            setFieldValue("propertyForm:propertySheet:httpTextField:RequestTimeoutSeconds:RequestTimeoutSeconds", requestTimeoutSeconds);
            setFieldValue("propertyForm:propertySheet:httpTextField:sendBsize:sendBsize", sendBsize);
            setFieldValue("propertyForm:propertySheet:httpTextField:headerBLength:headerBLength", headerBLength);
            setFieldValue("propertyForm:propertySheet:httpTextField:MaxPostSize:headerBLength", maxPostSize);
            selectDropdownOption("propertyForm:propertySheet:httpTextField:Compression:Compression", "on");
            setFieldValue("propertyForm:propertySheet:httpTextField:compressableMime:compressableMime", compressableMime);
            markCheckbox("propertyForm:propertySheet:httpTextField:Comet:cometEnabled");

            clickAndWait("propertyForm:propertyContentPage:topButtons:newButton", TRIGGER_PROTOCOLS);
            assertTrue(isTextPresent(protocol));

            clickAndWait(getLinkIdByLinkText("propertyForm:configs", protocol), "Edit Protocol");
            assertTrue(isTextPresent(protocol));

            clickAndWait("propertyForm:protocolTabs:httpTab", "Modify HTTP settings for the protocol.");
            assertEquals(maxC, getFieldValue("propertyForm:propertySheet:httpTextField:maxC:maxC"));
            assertEquals(timeoutSeconds, getFieldValue("propertyForm:propertySheet:httpTextField:TimeoutSeconds:TimeoutSeconds"));
            assertEquals(requestTimeoutSeconds, getFieldValue("propertyForm:propertySheet:httpTextField:RequestTimeoutSeconds:RequestTimeoutSeconds"));
            assertEquals(connectionUploadTimeout, getFieldValue("propertyForm:propertySheet:httpTextField:connectionUploadTimeout:connectionUploadTimeout"));
            assertEquals(sendBsize, getFieldValue("propertyForm:propertySheet:httpTextField:sendBsize:sendBsize"));
            assertEquals(headerBLength, getFieldValue("propertyForm:propertySheet:httpTextField:headerBLength:headerBLength"));
            assertEquals(maxPostSize, getFieldValue("propertyForm:propertySheet:httpTextField:MaxPostSize:headerBLength"));
            assertEquals(compressableMime, getFieldValue("propertyForm:propertySheet:httpTextField:compressableMime:compressableMime"));
            assertEquals("true", getFieldValue("propertyForm:propertySheet:httpTextField:Comet:cometEnabled"));

            clickAndWait("propertyForm:protocolTabs:fileCacheTab", "Modify file cache settings for the protocol.");
            assertEquals(maxAge, getFieldValue("propertyForm:propertySheet:fileTextField:maxAge:maxAge"));
            assertEquals(maxCacheSizeBytes, getFieldValue("propertyForm:propertySheet:fileTextField:maxCacheSizeBytes:maxCacheSizeBytes"));
            assertEquals(maxFile, getFieldValue("propertyForm:propertySheet:fileTextField:maxFile:maxFile"));

            clickAndWait("propertyForm:propertyContentPage:topButtons:cancelButton", TRIGGER_PROTOCOLS);


            deleteRow("propertyForm:configs:topActionsGroup1:button1", "propertyForm:configs", protocol);
        }

    }
}
