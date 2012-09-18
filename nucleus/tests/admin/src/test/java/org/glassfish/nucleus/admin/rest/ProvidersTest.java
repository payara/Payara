/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.nucleus.admin.rest;

import javax.ws.rs.core.Response;
import static org.testng.AssertJUnit.*;
import org.testng.annotations.Test;

/**
 *
 * @author jasonlee
 */
public class ProvidersTest extends RestTestBase {
    private static final String URL_ACTION_REPORT_RESULT = "/domain/uptime";
    private static final String URL_COMMAND_RESOURCE_GET_RESULT = "/domain/stop";
    private static final String URL_GET_RESULT = "/domain";
    private static final String URL_GET_RESULT_LIST = "/domain/servers/server";
    private static final String URL_OPTIONS_RESULT = "/domain";
    private static final String URL_STRING_LIST_RESULT = "/domain/configs/config/server-config/java-config/jvm-options";
    private static String URL_TREE_NODE;

    public ProvidersTest() {
        URL_TREE_NODE = "http://localhost:" + getParameter("admin.port", "4848") + "/monitoring/domain";
    }

    @Test
    public void testActionReportResultHtmlProvider() {
        Response response = get(URL_ACTION_REPORT_RESULT + ".html");
        assertTrue(isSuccess(response));
    }

    @Test
    public void testActionReportResultXmlProvider() {
        Response response = get(URL_ACTION_REPORT_RESULT + ".xml");
        assertTrue(isSuccess(response));
    }

    @Test
    public void testActionReportResultJsonProvider() {
        Response response = get(URL_ACTION_REPORT_RESULT + ".json");
        assertTrue(isSuccess(response));
    }

    @Test
    public void testCommandResourceGetResultHtmlProvider() {
        Response response = get(URL_COMMAND_RESOURCE_GET_RESULT + ".html");
        assertTrue(isSuccess(response));
    }

    @Test
    public void testCommandResourceGetResultXmlProvider() {
        Response response = get(URL_COMMAND_RESOURCE_GET_RESULT + ".xml");
        assertTrue(isSuccess(response));
    }

    @Test
    public void testCommandResourceGetResultJsonProvider() {
        Response response = get(URL_COMMAND_RESOURCE_GET_RESULT + ".json");
        assertTrue(isSuccess(response));
    }

    @Test
    public void testGetResultHtmlProvider() {
        Response response = get(URL_GET_RESULT + ".html");
        assertTrue(isSuccess(response));
    }

    @Test
    public void testGetResultXmlProvider() {
        Response response = get(URL_GET_RESULT + ".xml");
        assertTrue(isSuccess(response));
    }

    @Test
    public void testGetResultJsonProvider() {
        Response response = get(URL_GET_RESULT + ".json");
        assertTrue(isSuccess(response));
    }

    @Test
    public void testGetResultListHtmlProvider() {
        Response response = get(URL_GET_RESULT_LIST + ".html");
        assertTrue(isSuccess(response));
    }

    @Test
    public void testGetResultListXmlProvider() {
        Response response = get(URL_GET_RESULT_LIST + ".xml");
        assertTrue(isSuccess(response));
    }

    @Test
    public void testGetResultListJsonProvider() {
        Response response = get(URL_GET_RESULT_LIST + ".json");
        assertTrue(isSuccess(response));
    }

    @Test
    public void testOptionsResultXmlProvider() {
        Response response = options(URL_OPTIONS_RESULT + ".xml");
        assertTrue(isSuccess(response));
    }

    @Test
    public void testOptionsResultJsonProvider() {
        Response response = options(URL_OPTIONS_RESULT + ".json");
        assertTrue(isSuccess(response));
    }

    @Test
    public void testStringListResultHtmlProvider() {
        Response response = get(URL_STRING_LIST_RESULT + ".html");
        assertTrue(isSuccess(response));
    }

    @Test
    public void testStringListResultXmlProvider() {
        Response response = get(URL_STRING_LIST_RESULT + ".xml");
        assertTrue(isSuccess(response));
    }

    @Test
    public void testStringListResultJsonProvider() {
        Response response = get(URL_STRING_LIST_RESULT + ".json");
        assertTrue(isSuccess(response));
    }

    @Test
    public void testTreeNodeHtmlProvider() {
        Response response = get(URL_TREE_NODE + ".html");
        assertTrue(isSuccess(response));
    }

    @Test
    public void testTreeNodeXmlProvider() {
        Response response = get(URL_TREE_NODE + ".xml");
        assertTrue(isSuccess(response));
    }

    @Test
    public void testTreeNodeJsonProvider() {
        Response response = get(URL_TREE_NODE + ".json");
        assertTrue(isSuccess(response));
    }
}
