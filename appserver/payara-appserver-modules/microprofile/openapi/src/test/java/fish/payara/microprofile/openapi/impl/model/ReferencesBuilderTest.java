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
import static org.eclipse.microprofile.openapi.OASFactory.createAPIResponse;
import static org.eclipse.microprofile.openapi.OASFactory.createCallback;
import static org.eclipse.microprofile.openapi.OASFactory.createExample;
import static org.eclipse.microprofile.openapi.OASFactory.createHeader;
import static org.eclipse.microprofile.openapi.OASFactory.createLink;
import static org.eclipse.microprofile.openapi.OASFactory.createParameter;
import static org.eclipse.microprofile.openapi.OASFactory.createPathItem;
import static org.eclipse.microprofile.openapi.OASFactory.createRequestBody;
import static org.eclipse.microprofile.openapi.OASFactory.createSchema;
import static org.eclipse.microprofile.openapi.OASFactory.createSecurityScheme;
import org.eclipse.microprofile.openapi.models.Components;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Reference;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

/**
 * Checks the JSON rendering of all types that can be used as {@link Reference}.
 */
public class ReferencesBuilderTest extends OpenApiBuilderTest {

    private final String url = "http://example.com/";

    @Override
    protected void setupBaseDocument(OpenAPI document) {
        String name = "NameRef";
        Components components = document.getComponents();
        components.addCallback("callbackRef", createCallback().ref(name));
        components.addCallback("callbackUrlRef", createCallback().ref(url));
        components.addExample("exampleRef", createExample().ref(name));
        components.addExample("exampleUrlRef", createExample().ref(url));
        components.addHeader("headerRef", createHeader().ref(name));
        components.addHeader("headerUrlRef", createHeader().ref(url));
        components.addResponse("responseRef", createAPIResponse().ref(name));
        components.addResponse("responseUrlRef", createAPIResponse().ref(url));
        components.addLink("linkRef", createLink().ref(name));
        components.addLink("linkUrlRef", createLink().ref(url));
        components.addParameter("parameterRef", createParameter().ref(name));
        components.addParameter("parameterUrlRef", createParameter().ref(url));
        components.addRequestBody("requestBodyRef", createRequestBody().ref(name));
        components.addRequestBody("requestBodyUrlRef", createRequestBody().ref(url));
        components.addSchema("schemaRef", createSchema().ref(name));
        components.addSchema("schemaUrlRef", createSchema().ref(url));
        components.addSecurityScheme("securitySchemeRef", createSecurityScheme().ref(name));
        components.addSecurityScheme("securitySchemeUrlRef", createSecurityScheme().ref(url));
        document.getPaths().addPathItem("pathItemRef", createPathItem().ref(name));
        document.getPaths().addPathItem("pathItemUrlRef", createPathItem().ref(url));
    }

    @Test
    public void callbackReferenceHasExpectedFields() {
        assertReference("#/components/callbacks/NameRef", "components.callbacks.callbackRef");
        assertReference(url, "components.callbacks.callbackUrlRef");
    }

    @Test
    public void exampleReferenceHasExpectedFields() {
        assertReference("#/components/examples/NameRef", "components.examples.exampleRef");
        assertReference(url, "components.examples.exampleUrlRef");
    }

    @Test
    public void headerReferenceHasExpectedFields() {
        assertReference("#/components/headers/NameRef", "components.headers.headerRef");
        assertReference(url, "components.headers.headerUrlRef");
    }

    @Test
    public void responseReferenceHasExpectedFields() {
        assertReference("#/components/responses/NameRef", "components.responses.responseRef");
        assertReference(url, "components.responses.responseUrlRef");
    }

    @Test
    public void linkReferenceHasExpectedFields() {
        assertReference("#/components/links/NameRef", "components.links.linkRef");
        assertReference(url, "components.links.linkUrlRef");
    }

    @Test
    public void parameterReferenceHasExpectedFields() {
        assertReference("#/components/parameters/NameRef", "components.parameters.parameterRef");
        assertReference(url, "components.parameters.parameterUrlRef");
    }

    @Test
    public void requestBodyReferenceHasExpectedFields() {
        assertReference("#/components/requestBodies/NameRef", "components.requestBodies.requestBodyRef");
        assertReference(url, "components.requestBodies.requestBodyUrlRef");
    }

    @Test
    public void schemaReferenceHasExpectedFields() {
        assertReference("#/components/schemas/NameRef", "components.schemas.schemaRef");
        assertReference(url, "components.schemas.schemaUrlRef");
    }

    @Test
    public void securitySchemeReferenceHasExpectedFields() {
        assertReference("#/components/securitySchemes/NameRef", "components.securitySchemes.securitySchemeRef");
        assertReference(url, "components.securitySchemes.securitySchemeUrlRef");
    }

    @Test
    public void pathItemReferenceHasExpectedFields() {
        assertReference("NameRef", "paths.pathItemRef");
        assertReference(url, "paths.pathItemUrlRef");
    }

    private void assertReference(String expected, String actualPath) {
        JsonNode actual = path(getOpenAPIJson(), actualPath);
        assertNotNull(actual);
        assertEquals("References should only have one field", 1, actual.size());
        assertEquals(expected, actual.get("$ref").textValue());
    }
}
