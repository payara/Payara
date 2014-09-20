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
import javax.ws.rs.core.Response;
import static org.testng.AssertJUnit.*;
import org.testng.annotations.Test;

/**
 * Created by IntelliJ IDEA.
 * User: jasonlee
 * Date: May 26, 2010
 * Time: 2:28:27 PM
 * To change this template use File | Settings | File Templates.
 */
public class JmsTest extends RestTestBase {
    static final String URL_ADMIN_OBJECT_RESOURCE = "/domain/resources/admin-object-resource";
    static final String URL_CONNECTOR_CONNECTION_POOL = "/domain/resources/connector-connection-pool";
    static final String URL_CONNECTOR_RESOURCE = "/domain/resources/connector-resource";
    static final String URL_JMS_HOST = "/domain/configs/config/server-config/jms-service/jms-host";
    static final String URL_SEVER_JMS_DEST = "/domain/servers/server/server";
    static final String DEST_TYPE = "topic";

    @Test(enabled=false)
    public void testJmsConnectionFactories() {
        final String poolName = "JmsConnectionFactory" + generateRandomString();
        Map<String, String> ccp_attrs = new HashMap<String, String>() {
            {
                put("name", poolName);
                put("connectiondefinition", "javax.jms.ConnectionFactory");
                put("raname", "jmsra");
            }
        };
        Map<String, String> cr_attrs = new HashMap<String, String>() {
            {
                put("id", poolName);
                put("poolname", poolName);
            }
        };

        // Create connection pool
        Response response = post(URL_CONNECTOR_CONNECTION_POOL, ccp_attrs);
        checkStatusForSuccess(response);

        // Check connection pool creation
        Map<String, String> pool = getEntityValues(get(URL_CONNECTOR_CONNECTION_POOL + "/" + poolName));
        assertFalse(pool.isEmpty());

        // Create connector resource
        response = post(URL_CONNECTOR_RESOURCE, cr_attrs);
        checkStatusForSuccess(response);

        // Check connector resource
        Map<String, String> resource = getEntityValues(get(URL_CONNECTOR_RESOURCE + "/" + poolName));
        assertFalse(resource.isEmpty());

        // Edit and check ccp
        response = post(URL_CONNECTOR_CONNECTION_POOL + "/" + poolName, new HashMap<String, String>() {
            {
                put("description", poolName);
            }
        });
        checkStatusForSuccess(response);

        pool = getEntityValues(get(URL_CONNECTOR_CONNECTION_POOL + "/" + poolName));
        assertTrue(pool.get("description").equals(poolName));

        // Edit and check cr
        response = post(URL_CONNECTOR_RESOURCE + "/" + poolName, new HashMap<String, String>() {
            {
                put("description", poolName);
            }
        });
        checkStatusForSuccess(response);

        resource = getEntityValues(get(URL_CONNECTOR_RESOURCE + "/" + poolName));
        assertTrue(pool.get("description").equals(poolName));

        // Delete objects
        response = delete(URL_CONNECTOR_CONNECTION_POOL + "/" + poolName, new HashMap<String, String>() {
            {
                put("cascade", "true");
            }
        });
        checkStatusForSuccess(response);
    }

    @Test(enabled=false)
    public void testJmsDestinationResources() {
        final String jndiName = "jndi/" + generateRandomString();
        String encodedJndiName = jndiName;
        try {
            encodedJndiName = URLEncoder.encode(jndiName, "UTF-8");
        } catch (UnsupportedEncodingException e) {
        }

        Map<String, String> attrs = new HashMap<String, String>() {
            {
                put("id", jndiName);
                put("raname", "jmsra");
                put("restype", "javax.jms.Topic");
            }
        };

        Response response = post(URL_ADMIN_OBJECT_RESOURCE, attrs);
        checkStatusForSuccess(response);

        Map<String, String> entity = getEntityValues(get(URL_ADMIN_OBJECT_RESOURCE + "/" + encodedJndiName));
        assertFalse(entity.isEmpty());

        response = delete(URL_ADMIN_OBJECT_RESOURCE + "/" + encodedJndiName);
        checkStatusForSuccess(response);
    }

    @Test(enabled=false)
    public void testJmsPhysicalDestination() {
        final String destName = "jmsDest" + generateRandomString();
        final int maxNumMsgs = generateRandomNumber(500);
        final int consumerFlowLimit = generateRandomNumber(500);

        createJmsPhysicalDestination(destName, DEST_TYPE, URL_SEVER_JMS_DEST);

        final HashMap<String, String> newDest = new HashMap<String, String>() {
            {
                put("id", destName);
                put("desttype", DEST_TYPE);
            }
        };
        Map<String, String> destProps = new HashMap<String, String>() {
            {
                putAll(newDest);
                put("property", "MaxNumMsgs=" + maxNumMsgs + ":ConsumerFlowLimit=" + consumerFlowLimit);
            }
        };

        Response response = get(URL_SEVER_JMS_DEST + "/__get-jmsdest", newDest);
        checkStatusForSuccess(response);

        response = post(URL_SEVER_JMS_DEST + "/__update-jmsdest", destProps);
        checkStatusForSuccess(response);
        response = get(URL_SEVER_JMS_DEST + "/__get-jmsdest", newDest);
        checkStatusForSuccess(response);
        Map<String, String> entity = this.getEntityValues(response);
        assertEquals(maxNumMsgs, entity.get("MaxNumMsgs"));
        assertEquals(consumerFlowLimit, entity.get("ConsumerFlowLimit"));

        deleteJmsPhysicalDestination(destName, URL_SEVER_JMS_DEST);
    }

    @Test(enabled=false)
    public void testJmsPhysicalDestionationsWithClusters() {
        final String destName = "jmsDest" + generateRandomString();
        ClusterTest ct = getTestClass(ClusterTest.class);
        final String clusterName = ct.createCluster();
        ct.createClusterInstance(clusterName, "in1_"+clusterName);
        ct.startCluster(clusterName);
        final String endpoint = "/domain/clusters/cluster/" + clusterName;
        try {
            
            createJmsPhysicalDestination(destName, "topic", endpoint);
            final HashMap<String, String> newDest = new HashMap<String, String>() {
                {
                    put("id", destName);
                    put("desttype", DEST_TYPE);
                }
            };

            Response response = get(endpoint + "/__get-jmsdest", newDest);
            checkStatusForSuccess(response);

            response = get(URL_SEVER_JMS_DEST + "/__get-jmsdest", newDest);
            checkStatusForFailure(response);
        }
        finally {
            deleteJmsPhysicalDestination(destName, endpoint);
            ct.stopCluster(clusterName);
            ct.deleteCluster(clusterName);
        }
    }

    @Test(enabled=false)
    public void testJmsPing() {
        String results = get(URL_SEVER_JMS_DEST + "/jms-ping").readEntity(String.class);
        assertTrue(results.contains("JMS-ping command executed successfully"));
    }

    @Test(enabled=false)
    public void testJmsFlush() {
        Map<String, String> payload = new HashMap<String, String>() {
            {
                put("id", "mq.sys.dmq");
                put("destType", "queue");
            }
        };

        Response response = post(URL_SEVER_JMS_DEST + "/flush-jmsdest", payload);
        checkStatusForSuccess(response);
    }

    @Test(enabled=false)
    public void testJmsHosts() {
        final String jmsHostName = "jmshost" + generateRandomString();
        Map<String, String> newHost = new HashMap<String, String>() {
            {
                put("id", jmsHostName);
                put("adminPassword", "admin");
                put("port", "7676");
                put("adminUserName", "admin");
                put("host", "localhost");
            }
        };

        // Test create
        Response response = post(URL_JMS_HOST, newHost);
        checkStatusForSuccess(response);

        // Test edit
        Map<String, String> entity = getEntityValues(get(URL_JMS_HOST + "/" + jmsHostName));
        assertFalse(entity.isEmpty());
        assertEquals(jmsHostName, entity.get("name"));
        entity.put("port", "8686");
        response = post(URL_JMS_HOST + "/" + jmsHostName, entity);
        checkStatusForSuccess(response);
        entity = getEntityValues(get(URL_JMS_HOST + "/" + jmsHostName));
        assertEquals("8686", entity.get("port"));

        // Test delete
        response = delete(URL_JMS_HOST + "/" + jmsHostName);
        checkStatusForSuccess(response);
    }

    public void createJmsPhysicalDestination(final String destName, final String type, String endpoint) {
        final Map<String, String> newDest = new HashMap<String, String>() {
            {
                put("id", destName);
                put("desttype", type);
            }
        };

        // Test Create
        Response response = post(endpoint + "/create-jmsdest", newDest);
        // This command returns 200 instead of 201, for some reason.  Odd.
        checkStatusForSuccess(response);
    }

    public void deleteJmsPhysicalDestination(final String destName, String endpoint) {
        final HashMap<String, String> newDest = new HashMap<String, String>() {
            {
                put("id", destName);
                put("desttype", DEST_TYPE);
            }
        };

        // Test deletion
        Response response = delete(endpoint + "/delete-jmsdest", newDest); // You POST to commands
        checkStatusForSuccess(response);

        response = get(endpoint + "__get-jmsdest", newDest);
        assertFalse(response.getStatus() == 200);
    }
}
