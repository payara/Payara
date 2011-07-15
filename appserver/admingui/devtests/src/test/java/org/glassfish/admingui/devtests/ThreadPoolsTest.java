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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ThreadPoolsTest extends BaseSeleniumTestClass {
    private static final String TRIGGER_THREAD_POOLS = "i18n_web.configuration.threadPoolPageTitleHelp";
    private static final String TRIGGER_EDIT_THREAD_POOL = "i18n_web.configuration.threadPoolEditPageTitle";
    private static final String TRIGGER_NEW_THREAD_POOL = "i18n_web.configuration.threadPoolNewPageTitle";

    @Test
    public void testAddThreadPool() {
        final String threadPoolName = "testThreadPool"+generateRandomString();

        clickAndWait("treeForm:tree:configurations:server-config:threadPools:threadPools_link", TRIGGER_THREAD_POOLS);
        clickAndWait("propertyForm:configs:topActionsGroup1:newButton", TRIGGER_NEW_THREAD_POOL);
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:nameProp:nameText", threadPoolName);
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:max:max", "8192");
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:maxThread:maxThread", "10");
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:minThread:minThread", "4");
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:timeout:timeout", "1800");
        clickAndWait("propertyForm:propertyContentPage:topButtons:newButton", TRIGGER_THREAD_POOLS);
        
        assertTrue(isTextPresent(threadPoolName));
        clickAndWait(getLinkIdByLinkText("propertyForm:configs", threadPoolName), TRIGGER_EDIT_THREAD_POOL);
        assertEquals("8192", getFieldValue("propertyForm:propertySheet:propertSectionTextField:max:max"));
        assertEquals("10", getFieldValue("propertyForm:propertySheet:propertSectionTextField:maxThread:maxThread"));
        assertEquals("4", getFieldValue("propertyForm:propertySheet:propertSectionTextField:minThread:minThread"));
        assertEquals("1800", getFieldValue("propertyForm:propertySheet:propertSectionTextField:timeout:timeout"));
        clickAndWait("propertyForm:propertyContentPage:topButtons:cancelButton", TRIGGER_THREAD_POOLS);

        deleteRow("propertyForm:configs:topActionsGroup1:button1", "propertyForm:configs", threadPoolName);
    }
}
