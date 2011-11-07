/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011 Oracle and/or its affiliates. All rights reserved.
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

import java.util.Map;
import java.net.URISyntaxException;
import org.junit.After;
import com.sun.jersey.api.client.ClientResponse;
import java.util.HashMap;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * This class tests the changes to the handling of @Element("*") instances
 * @author jasonlee
 */
public class ElementStarTest extends RestTestBase {
    protected static final String URL_CREATE_INSTANCE = "/domain/servers/server";

    protected String instanceName1;
    protected String instanceName2;

    @Before
    public void before() {
        instanceName1 = "instance_" + generateRandomString();
        instanceName2 = "instance_" + generateRandomString();

        ClientResponse response = post(URL_CREATE_INSTANCE, new HashMap<String, String>() {{ put("id", instanceName1); put("node", "localhost-domain1"); }});
        assertTrue(isSuccess(response));
        response = post(URL_CREATE_INSTANCE, new HashMap<String, String>() {{ put("id", instanceName2); put("node", "localhost-domain1"); }});
        assertTrue(isSuccess(response));
    }

    @After
    public void after() {
        ClientResponse response = delete("/domain/servers/server/" + instanceName1);
        assertTrue(isSuccess(response));
        response = delete("/domain/servers/server/" + instanceName2);
        assertTrue(isSuccess(response));
    }

    @Test
    public void testApplications() throws URISyntaxException {
        ApplicationTest at = getTestClass(ApplicationTest.class);
        final String app1 = "app" + generateRandomString();
        final String app2 = "app" + generateRandomString();

        at.deployApp("test.war", app1, app1);
        at.deployApp("test.war", app2, app2);
        at.addAppRef(app1, instanceName1);
        at.addAppRef(app2, instanceName1);

        ClientResponse response = get("/domain/servers/server/"+instanceName1+"/application-ref");
        Map<String, String> children = this.getChildResources(response);
        assertEquals(2, children.size());
    }

    @Test
    public void testResources() {
        // The DAS should already have two resource-refs (jdbc/__TimerPool and jdbc/__default)
        ClientResponse response = get ("/domain/servers/server/server/resource-ref");
        Map<String, String> children = this.getChildResources(response);
        assertTrue(children.size() >= 2);
    }

    @Test
    public void testLoadBalancerConfigs() {
        final String lbName = "lbconfig-" + generateRandomString();
        ClientResponse response = post ("/domain/lb-configs/lb-config/",
                new HashMap<String, String>() {{
                    put("id", lbName);
                    put("target", instanceName1);
                }});
        assertTrue(isSuccess(response));

        response = post("/domain/lb-configs/lb-config/" + lbName + "/create-http-lb-ref",
                new HashMap<String,String>() {{
                    put ("id", instanceName2);
                }});
        assertTrue(isSuccess(response));

        response = get ("/domain/lb-configs/lb-config/" + lbName + "/server-ref");
        Map<String, String> children = this.getChildResources(response);
        assertTrue(!children.isEmpty());
    }
}
