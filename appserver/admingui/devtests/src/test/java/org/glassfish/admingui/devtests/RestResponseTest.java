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

import org.glassfish.admingui.devtests.util.SeleniumHelper;
import org.glassfish.admingui.common.util.RestUtil;
import java.util.HashMap;
import org.glassfish.admingui.common.util.RestResponse;
import org.junit.Test;

import static org.junit.Assert.*;


/**
 *
 * @author jasonlee
 */
public class RestResponseTest {
    private static final SeleniumHelper helper = SeleniumHelper.getInstance();
    public static final String BASE_URL = helper.getBaseUrl() + "/management/domain";
    static final String URL_UPTIME = BASE_URL + "/uptime";
    static final String URL_GENERATE_JVM_REPORT = BASE_URL + "/servers/server/server/generate-jvm-report";

    @Test
    public void testPostCommand() {
        RestResponse response = RestUtil.get(URL_GENERATE_JVM_REPORT, new HashMap<String, Object>(){{
            put ("type", "summary");
        }});
        final String responseBody = response.getResponseBody();
        System.err.println(responseBody);
        assertTrue(responseBody.contains("Operating System Information"));
    }

    @Test
    public void testGetCommand() {
        RestResponse response = RestUtil.get(URL_UPTIME);
        assertTrue(response.getResponseBody().contains("\"uptime AdminCommand\""));
    }

    @Test
    public void testEndpointExists() {
        RestResponse response = RestUtil.get(URL_UPTIME);
        assertTrue(response.isSuccess());

        response = RestUtil.get(URL_UPTIME + "/forceFailure");
        assertFalse(response.isSuccess());
    }
}
