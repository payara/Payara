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

import static org.junit.Assert.assertEquals;
/**
 * 
 * @author Jeremy Lv
 *
 */
public class VirtualServerTest extends BaseSeleniumTestClass {

    @Test
    public void testAddVirtualServer() {
        gotoDasPage();
        final String serverName = "vs" + generateRandomString();

        clickAndWait("treeForm:tree:configurations:server-config:virtualServers:virtualServers_link");
        clickAndWait("propertyForm:configs:topActionsGroup1:newButton");
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:IdTextProp:IdText", serverName);
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:hostsProp:Hosts", "localhost");
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:logFileProp:LogFile", "logfile.txt");
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:docroot:docroot", "/tmp");
        Select select = new Select(driver.findElement(By.id("propertyForm:propertySheet:propertSectionTextField:nwProps:nw")));
        select.selectByVisibleText("http-listener-1");
        int count = addTableRow("propertyForm:basicTable", "propertyForm:basicTable:topActionsGroup1:addSharedTableButton");

        setFieldValue("propertyForm:basicTable:rowGroup1:0:col2:col1St", "property");
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col3:col1St", "value");
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col4:col1St", "description");

        clickAndWait("propertyForm:propertyContentPage:topButtons:newButton");

        String prefix = getTableRowByValue("propertyForm:configs", serverName, "col1");
        assertEquals(serverName, getText(prefix + "col1:link"));
        String clickId = prefix + "col1:link";
        clickByIdAction(clickId);

        assertTableRowCount("propertyForm:basicTable", count);

        clickAndWait("propertyForm:propertyContentPage:topButtons:cancelButton");

        deleteRow("propertyForm:configs:topActionsGroup1:button1", "propertyForm:configs", serverName);
    }
}
