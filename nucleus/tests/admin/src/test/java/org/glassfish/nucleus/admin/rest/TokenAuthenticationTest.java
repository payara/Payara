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
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.Response;
import org.glassfish.admin.rest.client.utils.MarshallingUtils;
import static org.testng.AssertJUnit.*;
import org.testng.annotations.Test;

/**
 * @author Mitesh Meswani
 */
public class TokenAuthenticationTest extends RestTestBase {
    private static final String URL_DOMAIN_SESSIONS = "/sessions";
    private static final String URL_CREATE_USER = "/domain/configs/config/server-config/security-service/auth-realm/admin-realm/create-user";
    private static final String URL_DELETE_USER = "/domain/configs/config/server-config/security-service/auth-realm/admin-realm/delete-user";
    private static final String GF_REST_TOKEN_COOKIE_NAME = "gfresttoken";
    private static final String TEST_GROUP = "newgroup";

    @Test
    public void testTokenCreateAndDelete() {
        deleteUserAuthTestUser(null); // just in case
        //Verify a session token got created
        String token = getSessionToken();

        // Verify we can use the session token.
        Response response = getClient().target(getAddress("/domain")).request().cookie(new Cookie(GF_REST_TOKEN_COOKIE_NAME, token)).get(Response.class);
        assertTrue(isSuccess(response));

        //Delete the token
        response = getClient().target(getAddress(URL_DOMAIN_SESSIONS) + "/" + token).request().cookie(new Cookie(GF_REST_TOKEN_COOKIE_NAME, token)).delete(Response.class);
        delete(URL_DOMAIN_SESSIONS);
        assertTrue(isSuccess(response));
    }

    @Test
    public void testAuthRequired() {
        Map<String, String> newUser = new HashMap<String, String>() {{
            put("id", AUTH_USER_NAME);
            put("groups", "asadmin");
            put("authrealmname", "admin-realm");
            put("AS_ADMIN_USERPASSWORD", AUTH_PASSWORD);
        }};
        String token = null;

        try {
            // Delete the test user if it exists
            deleteUserAuthTestUser(token);

            // Verify that we can get unauthenticated access to the server
            Response response = get("/domain");
            assertTrue(isSuccess(response));

            // Create the new user
            response = post(URL_CREATE_USER, newUser);
            assertTrue(isSuccess(response));

            // Verify that we must now authentication (response.status = 401)
            response = get("/domain");
            assertFalse(isSuccess(response));

            // Authenticate, get the token, then "clear" the authentication
            authenticate();
            token = getSessionToken();
            resetClient();

            // Build this request manually so we can pass the cookie
            response = getClient().target(getAddress("/domain")).request().cookie(new Cookie(GF_REST_TOKEN_COOKIE_NAME, token)).get(Response.class);
            assertTrue(isSuccess(response));
            resetClient();

            // Request again w/o the cookie.  This should fail.
            response = getClient().target(getAddress("/domain")).request().get(Response.class);
            assertFalse(isSuccess(response));
            authenticate();
        } finally {
            // Clean up after ourselves
            deleteUserAuthTestUser(token);
        }
    }

    protected String getSessionToken() {
        Response response = post(URL_DOMAIN_SESSIONS);
        assertTrue(isSuccess(response));
        Map<String, Object> responseMap = MarshallingUtils.buildMapFromDocument(response.readEntity(String.class));
        Map<String, Object> extraProperties = (Map<String, Object>)responseMap.get("extraProperties");
        return (String)extraProperties.get("token");
    }

    private void deleteUserAuthTestUser(String token) {
        if (token != null) {
            final String address = getAddress(URL_DELETE_USER);
            Response response = getClient().target(address).queryParam("id", AUTH_USER_NAME).request()
                    .cookie(new Cookie(GF_REST_TOKEN_COOKIE_NAME, token)).delete(Response.class);
            assertTrue(isSuccess(response));
            resetClient();
        } else {
            Response response = delete(URL_DELETE_USER, new HashMap<String, String>() {{ put("id", AUTH_USER_NAME); }});
            if (response.getStatus() == 401) {
                authenticate();
                response = delete(URL_DELETE_USER, new HashMap<String, String>() {{ put("id", AUTH_USER_NAME); }});
                assertTrue(isSuccess(response));
                resetClient();
            }
        }
    }
}
