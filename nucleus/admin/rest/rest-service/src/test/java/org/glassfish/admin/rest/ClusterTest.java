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
package org.glassfish.admin.rest;

import java.util.List;
import org.glassfish.admin.rest.clientutils.MarshallingUtils;
import com.sun.jersey.api.client.ClientResponse;
import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author jasonlee
 */
public class ClusterTest extends RestTestBase {
    public static final String URL_CLUSTER = "/domain/clusters/cluster";

    @Test
    public void testClusterCreationAndDeletion() {
        final String clusterName = "cluster_" + generateRandomString();
        createCluster(clusterName);

        Map<String, String> entity = getEntityValues(get(URL_CLUSTER + "/" + clusterName));
        assertEquals(clusterName + "-config", entity.get("configRef"));

        deleteCluster(clusterName);
    }

    @Test
    public void testListLifecycleModules() {
        final String clusterName = "cluster_" + generateRandomString();
        Map<String, String> newCluster = new HashMap<String, String>() {
            {
                put("id", clusterName);
            }
        };

        ClientResponse response = post(URL_CLUSTER, newCluster);
        assertTrue(isSuccess(response));

        response = get(URL_CLUSTER + "/" + clusterName + "/list-lifecycle-modules");
        assertTrue(isSuccess(response));

        response = post(URL_CLUSTER + "/" + clusterName + "/delete-cluster");
        assertTrue(isSuccess(response));

        response = get(URL_CLUSTER + "/" + clusterName);
        assertFalse(isSuccess(response));

    }

    public String createCluster() {
        final String clusterName = "cluster_" + generateRandomString();
        createCluster(clusterName);

        return clusterName;
    }

    public void createCluster(final String clusterName) {
        Map<String, String> newCluster = new HashMap<String, String>() {
            {
                put("id", clusterName);
            }
        };

        ClientResponse response = post(URL_CLUSTER, newCluster);
        assertTrue(isSuccess(response));
    }

    public void startCluster(String clusterName) {
        ClientResponse response = post(URL_CLUSTER + "/" + clusterName + "/start-cluster");
        assertTrue(isSuccess(response));
    }

    public void stopCluster(String clusterName) {
        ClientResponse response = post(URL_CLUSTER + "/" + clusterName + "/stop-cluster");
        assertTrue(isSuccess(response));
    }

    public void createClusterInstance(final String clusterName, final String instanceName) {
        ClientResponse response = post("/domain/create-instance", new HashMap<String, String>() {
            {
                put("cluster", clusterName);
                put("id", instanceName);
                put("node", "localhost-domain1");
            }
        });
        assertTrue(isSuccess(response));
    }

    public void deleteCluster(String clusterName) {
        ClientResponse response = get(URL_CLUSTER + "/" + clusterName + "/list-instances");
        Map body = MarshallingUtils.buildMapFromDocument(response.getEntity(String.class));
        Map extraProperties = (Map) body.get("extraProperties");
        if (extraProperties != null) {
            List<Map<String, String>> instanceList = (List<Map<String, String>>) extraProperties.get("instanceList");
            if ((instanceList != null) && (!instanceList.isEmpty())) {
                for (Map<String, String> instance : instanceList) {
                    String status = instance.get("status");
                    String instanceName = instance.get("name");
                    if (!"NOT_RUNNING".equalsIgnoreCase(status)) {
                        response = post("/domain/servers/server/" + instanceName + "/stop-instance");
                        assertTrue(isSuccess(response));
                    }
                    response = delete("/domain/servers/server/" + instanceName + "/delete-instance");
                    assertTrue(isSuccess(response));
                }
            }
        }


        response = post(URL_CLUSTER + "/" + clusterName + "/delete-cluster");
        assertTrue(isSuccess(response));

        response = get(URL_CLUSTER + "/" + clusterName);
        assertFalse(isSuccess(response));
    }
}
