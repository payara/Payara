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

import static org.junit.Assert.assertEquals;
/**
 * 
 * @author Jeremy Lv
 *
 */
public class ThreadPoolsTest extends BaseSeleniumTestClass {

    @Test
    public void testAddThreadPool() {
        gotoDasPage();
        final String threadPoolName = "testThreadPool"+generateRandomString();

        clickAndWait("treeForm:tree:configurations:server-config:threadPools:threadPools_link");
        clickAndWait("propertyForm:configs:topActionsGroup1:newButton");
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:nameProp:nameText", threadPoolName);
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:max:max", "8192");
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:maxThread:maxThread", "10");
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:minThread:minThread", "4");
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:timeout:timeout", "1800");
        clickAndWait("propertyForm:propertyContentPage:topButtons:newButton");
        
        String prefix = getTableRowByValue("propertyForm:configs", threadPoolName, "col1");
        assertEquals(threadPoolName, getText(prefix + "col1:link"));
        
        String clickId = prefix + "col1:link";
        clickAndWait(clickId);
        
        assertEquals("8192", getValue("propertyForm:propertySheet:propertSectionTextField:max:max", "value"));
        assertEquals("10", getValue("propertyForm:propertySheet:propertSectionTextField:maxThread:maxThread", "value"));
        assertEquals("4", getValue("propertyForm:propertySheet:propertSectionTextField:minThread:minThread", "value"));
        assertEquals("1800", getValue("propertyForm:propertySheet:propertSectionTextField:timeout:timeout", "value"));
        clickAndWait("propertyForm:propertyContentPage:topButtons:cancelButton");

        deleteRow("propertyForm:configs:topActionsGroup1:button1", "propertyForm:configs", threadPoolName);
    }
}
