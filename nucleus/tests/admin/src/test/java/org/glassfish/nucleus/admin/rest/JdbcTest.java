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

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.Response;
import static org.testng.AssertJUnit.*;
import org.testng.annotations.Test;

/**
 *
 * @author jasonlee
 */
public class JdbcTest extends RestTestBase {
    public static final String BASE_JDBC_RESOURCE_URL = "/domain/resources/jdbc-resource";
    public static final String BASE_JDBC_CONNECTION_POOL_URL = "/domain/resources/jdbc-connection-pool";

    @Test(enabled=false)
    public void testReadingPoolEntity() {
        Map<String, String> entity = getEntityValues(get(BASE_JDBC_CONNECTION_POOL_URL + "/__TimerPool"));
        assertEquals("__TimerPool", entity.get("name"));
    }

    @Test(enabled=false)
    public void testCreateAndDeletePool() {
        String poolName = "TestPool" + generateRandomString();
        Map<String, String> params = new HashMap<String, String>();
        params.put("name", poolName);
        params.put("datasourceClassname","org.apache.derby.jdbc.ClientDataSource");
        Response response = post(BASE_JDBC_CONNECTION_POOL_URL, params);
        assertTrue(isSuccess(response));

        Map<String, String> entity = getEntityValues(get(BASE_JDBC_CONNECTION_POOL_URL + "/"+poolName));
        assertNotSame(0, entity.size());

        response = delete(BASE_JDBC_CONNECTION_POOL_URL+"/"+poolName, new HashMap<String, String>());
        assertTrue(isSuccess(response));

        response = get(BASE_JDBC_CONNECTION_POOL_URL + "/" + poolName);
        assertEquals(404, response.getStatus());
    }

//    @Test(enabled=false)
    // TODO: Disabled until 13348 is resolved
    public void testCreateResourceWithBackslash() {
        String poolName = "TestPool\\" + generateRandomString();
        String encodedPoolName = poolName;
        try {
            encodedPoolName = URLEncoder.encode(poolName, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            Logger.getLogger(JdbcTest.class.getName()).log(Level.SEVERE, null, ex);
        }
        Map<String, String> params = new HashMap<String, String>();
        params.put("name", poolName);
        params.put("poolName", "DerbyPool");

        Response response = post (BASE_JDBC_RESOURCE_URL, params);
        assertTrue(isSuccess(response));

        Map<String, String> entity = getEntityValues(get(BASE_JDBC_CONNECTION_POOL_URL + "/" + encodedPoolName));
        assertNotSame(0, entity.size());

        response = delete("/" + encodedPoolName, new HashMap<String, String>());
        assertTrue(isSuccess(response));

        response = get(BASE_JDBC_CONNECTION_POOL_URL + "/" + encodedPoolName);
        assertEquals(404, response.getStatus());
    }

    @Test(enabled=false)
    public void createDuplicateResource() {
        final String resourceName = "jdbc/__default";
        Map<String, String> params = new HashMap<String, String>() {{
           put("id", resourceName);
           put("poolName", "DerbyPool");
        }};

        Response response = post (BASE_JDBC_RESOURCE_URL, params);
        assertFalse(isSuccess(response));
    }

    @Test(enabled=false)
    public void createDuplicateConnectionPool() {
        final String poolName = "DerbyPool";
        Map<String, String> params = new HashMap<String, String>() {{
           put("id", poolName);
           put("datasourceClassname", "org.apache.derby.jdbc.ClientDataSource");
        }};

        Response response = post (BASE_JDBC_CONNECTION_POOL_URL, params);
        assertFalse(isSuccess(response));
    }
}
