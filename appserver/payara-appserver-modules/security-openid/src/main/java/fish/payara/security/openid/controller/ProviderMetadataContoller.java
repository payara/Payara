/*
 *  DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 *  Copyright (c) [2018] Payara Foundation and/or its affiliates. All rights reserved.
 *
 *  The contents of this file are subject to the terms of either the GNU
 *  General Public License Version 2 only ("GPL") or the Common Development
 *  and Distribution License("CDDL") (collectively, the "License").  You
 *  may not use this file except in compliance with the License.  You can
 *  obtain a copy of the License at
 *  https://github.com/payara/Payara/blob/master/LICENSE.txt
 *  See the License for the specific
 *  language governing permissions and limitations under the License.
 *
 *  When distributing the software, include this License Header Notice in each
 *  file and include the License file at glassfish/legal/LICENSE.txt.
 *
 *  GPL Classpath Exception:
 *  The Payara Foundation designates this particular file as subject to the "Classpath"
 *  exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 *  file that accompanied this code.
 *
 *  Modifications:
 *  If applicable, add the following below the License Header, with the fields
 *  enclosed by brackets [] replaced by your own identifying information:
 *  "Portions Copyright [year] [name of copyright owner]"
 *
 *  Contributor(s):
 *  If you wish your version of this file to be governed by only the CDDL or
 *  only the GPL Version 2, indicate your decision by adding "[Contributor]
 *  elects to include this software in this distribution under the [CDDL or GPL
 *  Version 2] license."  If you don't indicate a single choice of license, a
 *  recipient has the option to distribute your version of this file under
 *  either the CDDL, the GPL Version 2 or to extend the choice of license to
 *  its licensees as provided above.  However, if you add GPL Version 2 code
 *  and therefore, elected the GPL Version 2 license, then the option applies
 *  only if the new code is made subject to such option by the copyright
 *  holder.
 */
package fish.payara.security.openid.controller;

import java.io.StringReader;
import java.util.HashMap;
import java.util.Map;
import static java.util.Objects.isNull;
import javax.enterprise.context.ApplicationScoped;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import static javax.ws.rs.core.MediaType.APPLICATION_JSON;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;

/**
 * Manages the OpenId Connect Provider metadata
 *
 * @author Gaurav Gupta
 */
@ApplicationScoped
public class ProviderMetadataContoller {

    private static final String WELL_KNOWN_PREFIX = "/.well-known/openid-configuration";

    private final Map<String, JsonObject> providerDocuments = new HashMap<>();

    /**
     * Request to the provider
     * https://example.com/.well-known/openid-configuration to obtain its
     * Configuration information / document which includes all necessary
     * endpoints (authorization_endpoint, token_endpoint, userinfo_endpoint,
     * revocation_endpoint etc), scopes, Claims, and public key location
     * information (jwks_uri)
     *
     * @param providerURI the OpenID Provider's uri
     * @return the OpenID Provider's configuration information / document
     *
     */
    public JsonObject getDocument(String providerURI) {
        if (isNull(providerDocuments.get(providerURI))) {
            if (providerURI.endsWith("/")) {
                providerURI = providerURI.substring(0, providerURI.length() - 1);
            }

            if (!providerURI.endsWith(WELL_KNOWN_PREFIX)) {
                providerURI = providerURI + WELL_KNOWN_PREFIX;
            }

            Client client = ClientBuilder.newClient();
            WebTarget target = client.target(providerURI);
            Response response = target.request()
                    .accept(APPLICATION_JSON)
                    .get();

            if (response.getStatus() == Status.OK.getStatusCode()) {
                // Get back the result of the REST request
                String responseBody = response.readEntity(String.class);
                try (JsonReader reader = Json.createReader(new StringReader(responseBody))) {
                    JsonObject responseObject = reader.readObject();
                    providerDocuments.put(providerURI, responseObject);
                }
            } else {
                throw new IllegalStateException(String.format(
                        "Unable to retrieve OpenID Provider's [%s] configuration document, HTTP respons code : [%s] ",
                        providerURI,
                        response.getStatus()
                ));
            }
        }
        return providerDocuments.get(providerURI);
    }

}
