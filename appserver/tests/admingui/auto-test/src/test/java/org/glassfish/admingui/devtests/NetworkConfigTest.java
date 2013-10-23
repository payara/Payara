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

import java.util.ArrayList;
import org.junit.Test;
import org.openqa.selenium.By;
import org.openqa.selenium.support.ui.Select;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
/**
 * 
 * @author Jeremy Lv
 *
 */
public class NetworkConfigTest extends BaseSeleniumTestClass {
    ArrayList<String> list = new ArrayList(); {list.add("server-config"); list.add("new-config");}

    @Test
    public void testAddingNetworkListener() {
        final String listenerName = "listener"+generateRandomString();
        createConfig("new-config");
        for (String configName : list) {
            gotoDasPage();
            clickAndWait("treeForm:tree:configurations:" + configName + ":networkConfig:networkListeners:networkListeners_link");
            clickAndWait("propertyForm:configs:topActionsGroup1:newButton");
            setFieldValue("propertyForm:propertySheet:propertSectionTextField:nameNew:name", listenerName);
            clickByIdAction("propertyForm:propertySheet:propertSectionTextField:prop1:existingRdBtn");
            Select select = new Select(driver.findElement(By.id("propertyForm:propertySheet:propertSectionTextField:prop1:protocoldw")));
            select.selectByVisibleText("http-listener-1");
            setFieldValue("propertyForm:propertySheet:propertSectionTextField:port:port", "1234");
            clickAndWait("propertyForm:propertyContentPage:topButtons:newButton");
            
            String prefix = getTableRowByValue("propertyForm:configs", listenerName, "col1");
            assertEquals(listenerName, getText(prefix + "col1:link"));
            
            String clickId = prefix + "col1:link";
            clickByIdAction(clickId);

            assertTrue(getText("propertyForm:propertySheet:propertSectionTextField:name:name").equals(listenerName));
            assertTrue(getText("propertyForm:propertySheet:propertSectionTextField:protocol:protocol").equals("http-listener-1"));

            assertEquals("1234", getValue("propertyForm:propertySheet:propertSectionTextField:port:port", "value"));
            clickAndWait("propertyForm:propertyContentPage:topButtons:cancelButton");

            deleteRow("propertyForm:configs:topActionsGroup1:button1", "propertyForm:configs", listenerName);
        }
        deleteConfig("new-config");

    }

    private void deleteConfig(String configName) {
        gotoDasPage();
        clickAndWait("treeForm:tree:configurations:configurations_link");
        deleteRow("propertyForm:configs:topActionsGroup1:button1", "propertyForm:configs", configName);
    }

    public void createConfig(String configName) {
        gotoDasPage();
        clickAndWait("treeForm:tree:configurations:configurations_link");
        int emptyCount = getTableRowCountByValue("propertyForm:configs", "new-config", "col1:link", true);
        if (emptyCount == 0) {
            clickAndWait("propertyForm:configs:topActionsGroup1:newButton");
            setFieldValue("propertyForm:propertySheet:propertSectionTextField:NameProp:Name", configName);
            clickAndWait("propertyForm:propertyContentPage:topButtons:okButton");
            
            String prefix = getTableRowByValue("propertyForm:configs", configName, "col1");
            assertEquals(configName, getText(prefix + "col1:link"));
        }
    }

    @Test
    public void testAddingTransport() {
        final String transportName = "transport"+generateRandomString();
        createConfig("new-config");
        for (String configName : list) {
            gotoDasPage();
            clickAndWait("treeForm:tree:configurations:" + configName + ":networkConfig:transports:transports_link");
            clickAndWait("propertyForm:configs:topActionsGroup1:newButton");
            setFieldValue("propertyForm:propertySheet:propertSectionTextField:IdTextProp:IdText", transportName);
            Select select = new Select(driver.findElement(By.id("propertyForm:propertySheet:propertSectionTextField:ByteBufferType:ByteBufferType")));
            select.selectByVisibleText("DIRECT");
            setFieldValue("propertyForm:propertySheet:propertSectionTextField:BufferSizeBytes:BufferSizeBytes", "1000");
            setFieldValue("propertyForm:propertySheet:propertSectionTextField:AcceptorThreads:AcceptorThreads", "-1");
            clickAndWait("propertyForm:propertyContentPage:topButtons:newButton");
            
            String prefix = getTableRowByValue("propertyForm:configs", transportName, "col1");
            assertEquals(transportName, getText(prefix + "col1:link"));

            String clickId = prefix + "col1:link";
            clickByIdAction(clickId);
            
            assertTrue(getText("propertyForm:propertySheet:propertSectionTextField:name:name").equals(transportName));
            Select select1 = new Select(driver.findElement(By.id("propertyForm:propertySheet:propertSectionTextField:ByteBufferType:ByteBufferType")));
            assertTrue(select1.getFirstSelectedOption().getAttribute("value").equals("DIRECT"));
            assertEquals("1000", getValue("propertyForm:propertySheet:propertSectionTextField:BufferSizeBytes:BufferSizeBytes", "value"));
            clickAndWait("propertyForm:propertyContentPage:topButtons:cancelButton");

            deleteRow("propertyForm:configs:topActionsGroup1:button1", "propertyForm:configs", transportName);
        }
        deleteConfig("new-config");

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
            gotoDasPage();
            clickAndWait("treeForm:tree:configurations:" + configName +":networkConfig:protocols:protocols_link");
            clickAndWait("propertyForm:configs:topActionsGroup1:newButton");
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
            Select select = new Select(driver.findElement(By.id("propertyForm:propertySheet:httpTextField:Compression:Compression")));
            select.selectByVisibleText("on");
            setFieldValue("propertyForm:propertySheet:httpTextField:compressableMime:compressableMime", compressableMime);
            if (!driver.findElement(By.id("propertyForm:propertySheet:httpTextField:Comet:cometEnabled")).isSelected()){
                clickByIdAction("propertyForm:propertySheet:httpTextField:Comet:cometEnabled");
            }

            clickAndWait("propertyForm:propertyContentPage:topButtons:newButton");
            
            String prefix = getTableRowByValue("propertyForm:configs", protocol, "col1");
            assertEquals(protocol, getText(prefix + "col1:link"));

            String clickId = prefix + "col1:link";
            clickByIdAction(clickId);

            assertTrue(getText("propertyForm:propertySheet:propertSectionTextField:name:name").equals(protocol));

            clickAndWait("propertyForm:protocolTabs:httpTab");
            assertEquals(maxC, getValue("propertyForm:propertySheet:httpTextField:maxC:maxC", "value"));
            assertEquals(timeoutSeconds, getValue("propertyForm:propertySheet:httpTextField:TimeoutSeconds:TimeoutSeconds", "value"));
            assertEquals(requestTimeoutSeconds, getValue("propertyForm:propertySheet:httpTextField:RequestTimeoutSeconds:RequestTimeoutSeconds", "value"));
            assertEquals(connectionUploadTimeout, getValue("propertyForm:propertySheet:httpTextField:connectionUploadTimeout:connectionUploadTimeout", "value"));
            assertEquals(sendBsize, getValue("propertyForm:propertySheet:httpTextField:sendBsize:sendBsize", "value"));
            assertEquals(headerBLength, getValue("propertyForm:propertySheet:httpTextField:headerBLength:headerBLength", "value"));
            assertEquals(maxPostSize, getValue("propertyForm:propertySheet:httpTextField:MaxPostSize:headerBLength", "value"));
            assertEquals(compressableMime, getValue("propertyForm:propertySheet:httpTextField:compressableMime:compressableMime", "value"));
            assertEquals("true", getValue("propertyForm:propertySheet:httpTextField:Comet:cometEnabled", "value"));

            clickAndWait("propertyForm:protocolTabs:fileCacheTab");
            assertEquals(maxAge, getValue("propertyForm:propertySheet:fileTextField:maxAge:maxAge", "value"));
            assertEquals(maxCacheSizeBytes, getValue("propertyForm:propertySheet:fileTextField:maxCacheSizeBytes:maxCacheSizeBytes", "value"));
            assertEquals(maxFile, getValue("propertyForm:propertySheet:fileTextField:maxFile:maxFile", "value"));

            clickAndWait("propertyForm:propertyContentPage:topButtons:cancelButton");


            deleteRow("propertyForm:configs:topActionsGroup1:button1", "propertyForm:configs", protocol);
        }
        deleteConfig("new-config");
    }
}
