/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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
 * @author Mitesh Meswani
 */
public class MonitoringTest extends RestTestBase {
    private static final String MONITORING_RESOURCE_URL = "/domain/configs/config/server-config/monitoring-service/module-monitoring-levels";
    private static final String JDBC_CONNECTION_POOL_URL = "/domain/resources/jdbc-connection-pool";
    private static final String PING_CONNECTION_POOL_URL = "/domain/resources/ping-connection-pool";
    private static final String CONTEXT_ROOT_MONITORING = "monitoring";

    @Override
    protected String getContextRoot() {
        return CONTEXT_ROOT_MONITORING;
    }

    @Test(enabled=false)
    public void initializeMonitoring() {
        // Enable monitoring
        String url = getManagementURL(MONITORING_RESOURCE_URL);
        Map<String, String> payLoad = new HashMap<String, String>() {
            {
                put("ThreadPool", "HIGH");
                put("Orb", "HIGH");
                put("EjbContainer", "HIGH");
                put("WebContainer", "HIGH");
                put("Deployment", "HIGH");
                put("TransactionService", "HIGH");
                put("HttpService", "HIGH");
                put("JdbcConnectionPool", "HIGH");
                put("ConnectorConnectionPool", "HIGH");
                put("ConnectorService", "HIGH");
                put("JmsService", "HIGH");
                put("Jvm", "HIGH");
                put("Security", "HIGH");
                put("WebServicesContainer", "HIGH");
                put("Jpa", "HIGH");
                put("Jersey", "HIGH");
            }
        };
        Response response = post(url, payLoad);
        checkStatusForSuccess(response);
    }

    /**
     * Objective - Verify that basic monitoring is working
     * Strategy - Call /monitoring/domain and assert that "server" is present as child element
     */
    @Test(enabled=false)
    public void testBaseURL() {
        Map<String, String> entity = getChildResources(get("/domain")); // monitoring/domain
        assertNotNull(entity.get("server"));
    }

    /**
     * Objective - Verify that resources with dot work
     * Strategy - create a resource with "." in name and then try to access it
     */
    @Test(enabled=false)
    public void testDot() {
        // Step 1- Create a resource with "."
        final String POOL_NAME = "poolNameWith.dot";

        // Clean up from leftover from previous run
        String url = getManagementURL(JDBC_CONNECTION_POOL_URL + '/' + POOL_NAME);
        delete(url);

        url = getManagementURL(JDBC_CONNECTION_POOL_URL);
        Map<String, String> payLoad = new HashMap<String, String>() {
            {
                put("name", POOL_NAME);
                put("resType", "javax.sql.DataSource");
                put("datasourceClassname", "foo.bar");
            }
        };
        Response response = post(url, payLoad);
        checkStatusForSuccess(response);


        // Step 2- Ping the connection pool to generate some monitoring data
        url = getManagementURL(PING_CONNECTION_POOL_URL);
        payLoad.clear();
        payLoad.put("id", POOL_NAME);
        get(url, payLoad);

       // Step 3 - Access monitoring tree to assert it is accessible
        response = get("/domain/server/resources/"+ POOL_NAME);
        checkStatusForSuccess(response);
        Map<String, String> responseEntity = getEntityValues(response);
        assertTrue("No Monitoring data found for pool " + POOL_NAME, responseEntity.size() > 0 );
    }

    /**
     * Objective - Verify that invalid resources returns 404
     * Strategy - Request an invalid resource under monitoring and ensure that 404 is returned
     */
    @Test(enabled=false)
    public void testInvalidResource() {
        Response response = get("/domain/server/foo");
        assertTrue("Did not receive ", response.getStatus() == javax.ws.rs.core.Response.Status.NOT_FOUND.getStatusCode() ) ;
    }


    private String getManagementURL(String targetResourceURL) {
        return getBaseUrl() + CONTEXT_ROOT_MANAGEMENT + targetResourceURL;

    }


}
