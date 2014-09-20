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
import org.codehaus.jettison.json.JSONObject;
import static org.testng.AssertJUnit.*;
import org.testng.annotations.Test;

/**
 *
 * @author jasonlee
 */
public class NetworkListenerTest extends RestTestBase {
    protected static final String URL_PROTOCOL = "/domain/configs/config/server-config/network-config/protocols/protocol";
    protected static final String URL_SSL = "/domain/configs/config/server-config/network-config/protocols/protocol/http-listener-2/ssl";

    @Test(enabled=false)
    public void createHttpListener() {
        final String redirectProtocolName = "http-redirect"; //protocol_" + generateRandomString();
        final String portUniProtocolName = "pu-protocol"; //protocol_" + generateRandomString();

        final String redirectFilterName = "redirect-filter"; //filter_" + generateRandomString();
        final String finderName1 = "http-finder"; //finder" + generateRandomString();
        final String finderName2 = "http-redirect"; //finder" + generateRandomString();

        try {
            Response response = post("/domain/set", new HashMap<String, String>() {{
                put("configs.config.server-config.network-config.network-listeners.network-listener.http-listener-1.protocol", "http-listener-1");
            }});
            checkStatusForSuccess(response);
            delete(URL_PROTOCOL + "/" + portUniProtocolName);
            checkStatusForSuccess(response);
            delete(URL_PROTOCOL + "/" + redirectProtocolName);
            checkStatusForSuccess(response);
// asadmin commands taken from: http://www.antwerkz.com/port-unification-in-glassfish-3-part-1/
//        asadmin create-protocol --securityenabled=false http-redirect
//        asadmin create-protocol --securityenabled=false pu-protocol
            response = post(URL_PROTOCOL, new HashMap<String, String>() {{ put ("securityenabled", "false"); put("id", redirectProtocolName); }});
            checkStatusForSuccess(response);
            response = post(URL_PROTOCOL, new HashMap<String, String>() {{ put ("securityenabled", "false"); put("id", portUniProtocolName); }});
            checkStatusForSuccess(response);

//        asadmin create-protocol-filter --protocol http-redirect --classname org.glassfish.grizzly.config.portunif.HttpRedirectFilter redirect-filter
            response = post (URL_PROTOCOL + "/" + redirectProtocolName + "/create-protocol-filter",
                new HashMap<String, String>() {{
                    put ("id", redirectFilterName);
                    put ("protocol", redirectProtocolName);
                    put ("classname", "org.glassfish.grizzly.config.portunif.HttpRedirectFilter");
                }});
            checkStatusForSuccess(response);

//        asadmin create-protocol-finder --protocol pu-protocol --targetprotocol http-listener-2 --classname org.glassfish.grizzly.config.portunif.HttpProtocolFinder http-finder
//        asadmin create-protocol-finder --protocol pu-protocol --targetprotocol http-redirect   --classname org.glassfish.grizzly.config.portunif.HttpProtocolFinder http-redirect
            response = post (URL_PROTOCOL + "/" + portUniProtocolName + "/create-protocol-finder",
                new HashMap<String, String>() {{
                    put ("id", finderName1);
                    put ("protocol", portUniProtocolName);
                    put ("targetprotocol", "http-listener-2");
                    put ("classname", "org.glassfish.grizzly.config.portunif.HttpProtocolFinder");
                }});
            checkStatusForSuccess(response);
            response = post (URL_PROTOCOL + "/" + portUniProtocolName + "/create-protocol-finder",
                new HashMap<String, String>() {{
                    put ("id", finderName2);
                    put ("protocol", portUniProtocolName);
                    put ("targetprotocol", redirectProtocolName);
                    put ("classname", "org.glassfish.grizzly.config.portunif.HttpProtocolFinder");
                }});
            checkStatusForSuccess(response);


//        asadmin set configs.config.server-config.network-config.network-listeners.network-listener.http-listener-1.protocol=pu-protocol
            response = post("/domain/configs/config/server-config/network-config/network-listeners/network-listener/http-listener-1", new HashMap<String, String>() {{
                put("protocol", portUniProtocolName);
            }});
            checkStatusForSuccess(response);

            response = get("/domain/configs/config/server-config/network-config/network-listeners/network-listener/http-listener-1/find-http-protocol");
            assertTrue(response.readEntity(String.class).contains("http-listener-2"));
        } finally {
//            ClientResponse response = post("/domain/set", new HashMap<String, String>() {{
            Response response = post("/domain/configs/config/server-config/network-config/network-listeners/network-listener/http-listener-1", new HashMap<String, String>() {{
                put("protocol", "http-listener-1");
            }});
            checkStatusForSuccess(response);
            response = delete(URL_PROTOCOL + "/" + portUniProtocolName + "/delete-protocol-finder",
                new HashMap<String, String>() {{
                    put("protocol", portUniProtocolName);
                    put("id", finderName1);
                }} );
            checkStatusForSuccess(response);
            response = delete(URL_PROTOCOL + "/" + portUniProtocolName + "/delete-protocol-finder",
                new HashMap<String, String>() {{
                    put("protocol", portUniProtocolName);
                    put("id", finderName2);
                }} );
            checkStatusForSuccess(response);
            response = delete(URL_PROTOCOL + "/" + redirectProtocolName + "/protocol-chain-instance-handler/protocol-chain/protocol-filter/" + redirectFilterName,
                    new HashMap<String, String>() {{ put("protocol", redirectProtocolName); }} );
            checkStatusForSuccess(response);
            response = delete(URL_PROTOCOL + "/" + portUniProtocolName);
            checkStatusForSuccess(response);
            response = delete(URL_PROTOCOL + "/" + redirectProtocolName);
            checkStatusForSuccess(response);
        }

    }

    @Test
    public void testClearingProperties() {
        Map<String, String> params = new HashMap<String, String>() {{
            put("keyStore", "foo");
            put("trustAlgorithm", "bar");
            put("trustMaxCertLength", "15");
            put("trustStore", "baz");
        }};

        Response response = post(URL_SSL, params);
        assertTrue(isSuccess(response));
        response = get(URL_SSL, params);
        Map<String, String> entity = this.getEntityValues(response);
        assertEquals(params.get("keyStore"), entity.get("keyStore"));
        assertEquals(params.get("trustAlgorithm"), entity.get("trustAlgorithm"));
        assertEquals(params.get("trustMaxCertLength"), entity.get("trustMaxCertLength"));
        assertEquals(params.get("trustStore"), entity.get("trustStore"));

        params.put("keyStore", "");
        params.put("trustAlgorithm", "");
        params.put("trustStore", "");
        response = post(URL_SSL, params);
        assertTrue(isSuccess(response));
        response = get(URL_SSL, params);
        entity = this.getEntityValues(response);
        assertEquals(JSONObject.NULL, entity.get("keyStore"));
        assertEquals(JSONObject.NULL, entity.get("trustAlgorithm"));
        assertEquals(JSONObject.NULL, entity.get("trustStore"));
    }
}
