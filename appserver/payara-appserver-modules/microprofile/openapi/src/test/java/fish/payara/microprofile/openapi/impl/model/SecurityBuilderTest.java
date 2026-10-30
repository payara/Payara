/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2019] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.microprofile.openapi.impl.model;

import com.fasterxml.jackson.databind.JsonNode;
import static fish.payara.microprofile.openapi.test.util.JsonUtils.path;
import java.util.Arrays;
import java.util.HashMap;

import static org.eclipse.microprofile.openapi.OASFactory.createOAuthFlow;
import static org.eclipse.microprofile.openapi.OASFactory.createOAuthFlows;
import static org.eclipse.microprofile.openapi.OASFactory.createSecurityRequirement;
import static org.eclipse.microprofile.openapi.OASFactory.createSecurityScheme;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.security.OAuthFlow;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme.In;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme.Type;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Checks the JSON rendering of {@link fish.payara.microprofile.openapi.impl.model.security.*}.
 */
public class SecurityBuilderTest extends OpenApiBuilderTest {

    @Override
    @SuppressWarnings("serial")
    protected void setupBaseDocument(OpenAPI document) {
        document.addSecurityRequirement(createSecurityRequirement()
                .addScheme("scheme1", "scope1")
                .addScheme("scheme2")
                .addScheme("scheme3", Arrays.asList("scope2", "scope3")));

        OAuthFlow flow = createOAuthFlow()
                .authorizationUrl("authorizationUrl")
                .tokenUrl("tokenUrl")
                .refreshUrl("refreshUrl")
                .scopes(new HashMap<String, String>() {{
                    put("scope", "description");
                    put("x-ext", "ext-value");
                }})
                .addExtension("x-ext", "ext-value");

        document.getComponents().addSecurityScheme("scheme1", createSecurityScheme()
                .description("description")
                .name("name")
                .type(Type.HTTP)
                .in(In.QUERY)
                .scheme("scheme")
                .bearerFormat("bearerFormat")
                .openIdConnectUrl("openIdConnectUrl")
                .addExtension("x-ext", "ext-value")
                .flows(createOAuthFlows()
                        .implicit(flow)
                        .password(flow)
                        .clientCredentials(flow)
                        .authorizationCode(flow)
                        .addExtension("x-ext", "ext-value"))
                );
    }

    @Test
    public void securityRequirementHasExpectedFields() {
        JsonNode security = path(getOpenAPIJson(), "security.[0]");
        assertNotNull(security);
        assertTrue(security.get("scheme1").isArray());
        assertTrue(security.get("scheme2").isArray());
        assertTrue(security.get("scheme3").isArray());
        JsonNode securityScheme = security.get("scheme1");
        assertEquals(1, securityScheme.size());
        assertEquals("scope1", securityScheme.get(0).textValue());
    }

    @Test
    public void securitySchemaHasExpectedFields() {
        JsonNode schema = path(getOpenAPIJson(), "components.securitySchemes.scheme1");
        assertNotNull(schema);
        assertEquals("http", schema.get("type").textValue());
        assertEquals("description", schema.get("description").textValue());
        assertEquals("name", schema.get("name").textValue());
        assertEquals("query", schema.get("in").textValue());
        assertEquals("scheme", schema.get("scheme").textValue());
        assertEquals("bearerFormat", schema.get("bearerFormat").textValue());
        assertEquals("openIdConnectUrl", schema.get("openIdConnectUrl").textValue());
        assertEquals("ext-value", schema.get("x-ext").textValue());
        assertTrue(schema.get("flows").isObject());
    }

    @Test
    public void oAuthFlowsHasExpectedFields() {
        JsonNode flows = path(getOpenAPIJson(), "components.securitySchemes.scheme1.flows");
        assertNotNull(flows);
        assertEquals(5, flows.size());
        assertTrue(flows.get("implicit").isObject());
        assertTrue(flows.get("password").isObject());
        assertTrue(flows.get("clientCredentials").isObject());
        assertTrue(flows.get("authorizationCode").isObject());
        assertEquals("ext-value", flows.get("x-ext").textValue());
    }

    @Test
    public void oAuthFlowHasExpectedFields() {
        JsonNode flow = path(getOpenAPIJson(), "components.securitySchemes.scheme1.flows.implicit");
        assertNotNull(flow);
        assertEquals("authorizationUrl", flow.get("authorizationUrl").textValue());
        assertEquals("tokenUrl", flow.get("tokenUrl").textValue());
        assertEquals("refreshUrl", flow.get("refreshUrl").textValue());
        assertEquals("ext-value", flow.get("x-ext").textValue());
        assertTrue(flow.get("scopes").isObject());
    }

    @Test
    public void scopesHasExpectedFields() {
        JsonNode scopes = path(getOpenAPIJson(), "components.securitySchemes.scheme1.flows.implicit.scopes");
        assertNotNull(scopes);
        assertEquals("description", scopes.get("scope").textValue());
        assertEquals("ext-value", scopes.get("x-ext").textValue());
    }
}
