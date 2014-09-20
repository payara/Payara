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
import java.util.List;
import java.util.Map;
import javax.ws.rs.core.Response;
import org.glassfish.admin.rest.client.utils.MarshallingUtils;
import static org.testng.AssertJUnit.*;
import org.testng.annotations.Test;

public class AuthRealmTest extends RestTestBase {
    public static final String URL_LIST_GROUP_NAMES = "/domain/configs/config/server-config/security-service/auth-realm/admin-realm/list-group-names";
    public static final String URL_SUPPORTS_USER_MANAGEMENT = "/domain/configs/config/server-config/security-service/auth-realm/admin-realm/supports-user-management";
    public static final String URL_LIST_ADMIN_REALM_USERS = "/domain/configs/config/server-config/security-service/auth-realm/admin-realm/list-users";
    public static final String URL_LIST_FILE_USERS = "/domain/configs/config/server-config/security-service/auth-realm/file/list-users";
    public static final String URL_CREATE_USER = "/domain/configs/config/server-config/security-service/auth-realm/file/create-user";
    public static final String URL_DELETE_USER = "/domain/configs/config/server-config/security-service/auth-realm/file/delete-user";
    public static final String URL_AUTH_REALM_CLASS_NAMES = "/domain/list-predefined-authrealm-classnames";

    // Disable this test for now...
//    @Test
    public void testListGroupNames() {
        Response response = get(URL_LIST_GROUP_NAMES, new HashMap<String, String>() {{
            put("userName", "admin");
            put("realmName", "admin-realm");
        }});
        checkStatusForSuccess(response);
        final String entity = response.readEntity(String.class);
        Map responseMap = MarshallingUtils.buildMapFromDocument(entity);
        Map extraProperties = (Map)responseMap.get("extraProperties");
        List<String> groups = (List<String>)extraProperties.get("groups");

        assertTrue(groups.size() > 0);
    }

    @Test
    public void testSupportsUserManagement() {
        List<String> groups = getCommandResults(get(URL_SUPPORTS_USER_MANAGEMENT));
        assertEquals("true", groups.get(0));
    }

//    @Test
    public void testUserManagement() {
        final String userName = "user" + generateRandomString();
        Map<String, String> newUser = new HashMap<String, String>() {{
           put ("id", userName);
           put ("AS_ADMIN_USERPASSWORD", "password");
        }};

        Response response = post(URL_CREATE_USER, newUser);
        assertTrue(isSuccess(response));

        List<String> values = getCommandResults(get(URL_LIST_FILE_USERS));
        assertTrue(values.contains(userName));

        response = delete(URL_DELETE_USER, newUser);
        assertTrue(isSuccess(response));

        values = getCommandResults(get(URL_LIST_FILE_USERS));
        assertFalse(values.contains(userName));
    }

    @Test
    public void testListAuthRealmClassNames() {
        List<String> classNameList = getCommandResults(get(URL_AUTH_REALM_CLASS_NAMES));
        assertTrue(!classNameList.isEmpty());
    }
}
