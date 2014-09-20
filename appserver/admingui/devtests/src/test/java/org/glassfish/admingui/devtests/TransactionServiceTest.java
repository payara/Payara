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

/**
 * Created by IntelliJ IDEA.
 * User: jasonlee
 * Date: Mar 12, 2010
 * Time: 1:36:39 PM
 * To change this template use File | Settings | File Templates.
 */
public class TransactionServiceTest extends BaseSeleniumTestClass {
    private static final String TRIGGER_TRANSACTION_SERVICE = "i18n_jts.ts.PageHelp";

    @Test
    public void testTransactionService() {
        final String timeout = Integer.toString(generateRandomNumber(60));
        final String retry = Integer.toString(generateRandomNumber(600));
        final String keypoint = Integer.toString(generateRandomNumber(65535));

        clickAndWait("treeForm:tree:configurations:server-config:transactionService:transactionService_link", TRIGGER_TRANSACTION_SERVICE);
        markCheckbox("propertyForm:propertySheet:propertSectionTextField:onRestartProp:enabled");
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:timeoutProp:Timeout", timeout);
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:retryProp:Retry", retry);
        selectDropdownOption("propertyForm:propertySheet:propertSectionTextField:heuristicProp:HeuristicDecision", "Commit");
        setFieldValue("propertyForm:propertySheet:propertSectionTextField:keyPointProp:Keypoint", keypoint);
        int count = addTableRow("propertyForm:basicTable", "propertyForm:basicTable:topActionsGroup1:addSharedTableButton");

        setFieldValue("propertyForm:basicTable:rowGroup1:0:col2:col1St", "property"+generateRandomString());
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col3:col1St", "value");
        setFieldValue("propertyForm:basicTable:rowGroup1:0:col4:col1St", "description");
        clickAndWait("propertyForm:propertyContentPage:topButtons:saveButton", TRIGGER_NEW_VALUES_SAVED);
        reset();

        clickAndWait("treeForm:tree:configurations:server-config:transactionService:transactionService_link", TRIGGER_TRANSACTION_SERVICE);
        assertEquals(true, isChecked("propertyForm:propertySheet:propertSectionTextField:onRestartProp:enabled"));
        assertEquals(timeout, getFieldValue("propertyForm:propertySheet:propertSectionTextField:timeoutProp:Timeout"));
        assertEquals(retry, getFieldValue("propertyForm:propertySheet:propertSectionTextField:retryProp:Retry"));
        assertEquals("commit", getFieldValue("propertyForm:propertySheet:propertSectionTextField:heuristicProp:HeuristicDecision"));
        assertEquals(keypoint, getFieldValue("propertyForm:propertySheet:propertSectionTextField:keyPointProp:Keypoint"));
        assertTableRowCount("propertyForm:basicTable", count);
    }
}
