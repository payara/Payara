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

import java.util.HashMap;
import java.util.Map;
import javax.ws.rs.core.Response;
import static org.testng.AssertJUnit.*;
import org.testng.annotations.Test;

/**
 *
 * @author jasonlee
 */
public class ResourceRefTest extends RestTestBase {
    private static final String URL_CREATE_INSTANCE = "/domain/create-instance";
    private static final String URL_JDBC_RESOURCE = "/domain/resources/jdbc-resource";
    private static final String URL_RESOURCE_REF = "/domain/servers/server/server/resource-ref";

    @Test(enabled=false)
    public void testCreatingResourceRef() {
        final String instanceName = "instance_" + generateRandomString();
        final String jdbcResourceName = "jdbc_" + generateRandomString();
        Map<String, String> newInstance = new HashMap<String, String>() {{
            put("id", instanceName);
            put("node", "localhost-domain1");
        }};
        Map<String, String> jdbcResource = new HashMap<String, String>() {{
            put("id", jdbcResourceName);
            put("connectionpoolid", "DerbyPool");
            put("target", instanceName);
        }};
        Map<String, String> resourceRef = new HashMap<String, String>() {{
            put("id", jdbcResourceName);
            put("target", "server");
        }};

        try {
            Response response = post(URL_CREATE_INSTANCE, newInstance);
            assertTrue(isSuccess(response));

            response = post(URL_JDBC_RESOURCE, jdbcResource);
            assertTrue(isSuccess(response));

            response = post(URL_RESOURCE_REF, resourceRef);
            assertTrue(isSuccess(response));
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            Response response = delete("/domain/servers/server/" + instanceName + "/resource-ref/" + jdbcResourceName, new HashMap<String, String>() {{
                put("target", instanceName);
            }});
            assertTrue(isSuccess(response));
            response = get("/domain/servers/server/" + instanceName + "/resource-ref/" + jdbcResourceName);
            assertFalse(isSuccess(response));

            response = delete(URL_JDBC_RESOURCE + "/" + jdbcResourceName);
            assertTrue(isSuccess(response));
            response = get(URL_JDBC_RESOURCE + "/" + jdbcResourceName);
            assertFalse(isSuccess(response));

            response = delete("/domain/servers/server/" + instanceName + "/delete-instance");
            assertTrue(isSuccess(response));
            response = get("/domain/servers/server/" + instanceName);
            assertFalse(isSuccess(response));
        }
    }
}
