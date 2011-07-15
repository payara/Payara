/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.rest;

import com.sun.jersey.api.client.ClientResponse;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.glassfish.admin.rest.clientutils.MarshallingUtils;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jasonlee
 */
public class ConnectorsTest extends RestTestBase {
    protected static final String URL_CONNECTOR_CONNECTION_POOL = "/domain/resources/connector-connection-pool";

    @Test
    public void createSecurityMapUserGroups() {
        final String connPoolName = "conn_pool_" + generateRandomString();
        final String secMapName = "sm_" + generateRandomString();
        Map<String, String> connPool = new HashMap<String, String>() {{
            put("id", connPoolName);
            put("connectiondefinitionname", "javax.jms.ConnectionFactory");
            put("resourceAdapterName", "jmsra");
        }};
        Map<String, String> secMap = new HashMap<String, String>() {{
            put("id", secMapName);
            put("poolName", connPoolName);
            put("userGroups", "group1");
//            put("principals", "principal1");
            put("mappedPassword","admin");
            put("mappedUserName","admin");
        }};
        Map<String, String> groups = new HashMap<String, String>() {{
            put("mapName", secMapName);
            put("poolName", connPoolName);
            put("groups", "g1,g2,g3");
        }};

        try {
            ClientResponse response = post(URL_CONNECTOR_CONNECTION_POOL, connPool);
            checkStatusForSuccess(response);

            createConnectorConnectionPool(connPoolName, secMap, response);

            response = get(URL_CONNECTOR_CONNECTION_POOL+"/"+connPoolName+"/security-map/"+secMapName+"/user-group");
            checkStatusForSuccess(response);
            List<String> groupList = getLeafList(response);
            assertTrue(groupList.contains("group1"));

            response = post(URL_CONNECTOR_CONNECTION_POOL+"/"+connPoolName+"/security-map/"+secMapName+"/user-group", groups);
            checkStatusForSuccess(response);
            response = get(URL_CONNECTOR_CONNECTION_POOL+"/"+connPoolName+"/security-map/"+secMapName+"/user-group");
            checkStatusForSuccess(response);
            groupList = getLeafList(response);
            assertTrue(groupList.contains("g1"));
            assertTrue(groupList.contains("g2"));
            assertTrue(groupList.contains("g3"));

            groups.put("groups", "g1,g3");
            response = delete(URL_CONNECTOR_CONNECTION_POOL+"/"+connPoolName+"/security-map/"+secMapName+"/user-group", groups);
            checkStatusForSuccess(response);
            response = get(URL_CONNECTOR_CONNECTION_POOL+"/"+connPoolName+"/security-map/"+secMapName+"/user-group");
            checkStatusForSuccess(response);
            groupList = getLeafList(response);
            assertFalse(groupList.contains("g1"));
            assertTrue(groupList.contains("g2"));
            assertFalse(groupList.contains("g3"));

            response = get(URL_CONNECTOR_CONNECTION_POOL+"/"+connPoolName+"/security-map/"+secMapName);
        } finally {
            deleteConnectorConnectionPool(connPoolName);
        }
    }

    @Test
    public void createSecurityMapPrincipals() {
        final String connPoolName = "conn_pool_" + generateRandomString();
        final String secMapName = "sm_" + generateRandomString();
        Map<String, String> connPool = new HashMap<String, String>() {{
            put("id", connPoolName);
            put("connectiondefinitionname", "javax.jms.ConnectionFactory");
            put("resourceAdapterName", "jmsra");
        }};
        Map<String, String> secMap = new HashMap<String, String>() {{
            put("id", secMapName);
            put("poolName", connPoolName);
            put("principals", "principal1");
            put("mappedPassword","admin");
            put("mappedUserName","admin");
        }};
        Map<String, String> principals = new HashMap<String, String>() {{
            put("mapName", secMapName);
            put("poolName", connPoolName);
            put("principals", "p1,p2,p3");
        }};

        try {
            ClientResponse response = post(URL_CONNECTOR_CONNECTION_POOL, connPool);
            checkStatusForSuccess(response);

            createConnectorConnectionPool(connPoolName, secMap, response);

            response = get(URL_CONNECTOR_CONNECTION_POOL+"/"+connPoolName+"/security-map/"+secMapName+"/principal");
            checkStatusForSuccess(response);
            List<String> principalList = getLeafList(response);
            assertTrue(principalList.contains("principal1"));

            response = post(URL_CONNECTOR_CONNECTION_POOL+"/"+connPoolName+"/security-map/"+secMapName+"/principal", principals);
            checkStatusForSuccess(response);
            response = get(URL_CONNECTOR_CONNECTION_POOL+"/"+connPoolName+"/security-map/"+secMapName+"/principal");
            checkStatusForSuccess(response);
            principalList = getLeafList(response);
            assertTrue(principalList.contains("p1"));
            assertTrue(principalList.contains("p2"));
            assertTrue(principalList.contains("p3"));

            principals.put("principals", "p1,p3");
            response = delete(URL_CONNECTOR_CONNECTION_POOL+"/"+connPoolName+"/security-map/"+secMapName+"/principal", principals);
            checkStatusForSuccess(response);
            response = get(URL_CONNECTOR_CONNECTION_POOL+"/"+connPoolName+"/security-map/"+secMapName+"/principal");
            checkStatusForSuccess(response);
            principalList = getLeafList(response);
            assertFalse(principalList.contains("p1"));
            assertTrue(principalList.contains("p2"));
            assertFalse(principalList.contains("p3"));

            response = get(URL_CONNECTOR_CONNECTION_POOL+"/"+connPoolName+"/security-map/"+secMapName);
        } finally {
            deleteConnectorConnectionPool(connPoolName);
        }
    }

    protected void deleteConnectorConnectionPool(final String connPoolName) {
        ClientResponse response = delete(URL_CONNECTOR_CONNECTION_POOL + "/" + connPoolName);
    }

    protected ClientResponse createConnectorConnectionPool(final String connPoolName, Map<String, String> secMap, ClientResponse response) {
        response = post(URL_CONNECTOR_CONNECTION_POOL + "/" + connPoolName + "/security-map", secMap);
        checkStatusForSuccess(response);
        return response;
    }

    private List<String> getLeafList(ClientResponse response) {
        Map<String, Object> responseMap = MarshallingUtils.buildMapFromDocument(response.getEntity(String.class));
        Map ep = (Map)responseMap.get("extraProperties");
        List<String> ug = (List<String>)ep.get("leafList");

        return ug;
    }
}
