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
import static org.eclipse.microprofile.openapi.OASFactory.createAPIResponses;
import static org.eclipse.microprofile.openapi.OASFactory.createCallback;
import static org.eclipse.microprofile.openapi.OASFactory.createExternalDocumentation;
import static org.eclipse.microprofile.openapi.OASFactory.createOperation;
import static org.eclipse.microprofile.openapi.OASFactory.createParameter;
import static org.eclipse.microprofile.openapi.OASFactory.createPathItem;
import static org.eclipse.microprofile.openapi.OASFactory.createRequestBody;
import static org.eclipse.microprofile.openapi.OASFactory.createSecurityRequirement;
import static org.eclipse.microprofile.openapi.OASFactory.createServer;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.parameters.Parameter.In;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Checks the JSON rendering of {@link OperationImpl} and {@link ExternalDocumentationImpl}.
 */
public class OperationsBuilderTest extends OpenApiBuilderTest {

    @Override
    protected void setupBaseDocument(OpenAPI document) {
        document.getPaths().addPathItem("path1", createPathItem()
                .GET(createOperation()
                        .description("description")
                        .operationId("operationId")
                        .summary("summary")
                        .deprecated(true)
                        .externalDocs(createExternalDocumentation()
                                .url("url")
                                .description("description"))
                        .requestBody(createRequestBody().ref("ref"))
                        .responses(createAPIResponses())
                        .addTag("tag1")
                        .addTag("tag2")
                        .addExtension("x-ext", "ext-value")
                        .addCallback("callback1", createCallback().ref("ref"))
                        .addSecurityRequirement(createSecurityRequirement()
                                .addScheme("scheme1", "scope1")
                                .addScheme("scheme2"))
                        .addServer(createServer().url("server1"))
                        .addParameter(createParameter().name("param1").in(In.QUERY))
                        ));
    }

    @Test
    public void operationHasExpectedFields() {
        JsonNode operation = path(getOpenAPIJson(), "paths.path1.get");
        assertNotNull(operation);
        assertEquals("summary", operation.get("summary").textValue());
        assertEquals("description", operation.get("description").textValue());
        assertEquals("operationId", operation.get("operationId").textValue());
        assertEquals("ext-value", operation.get("x-ext").textValue());
        assertTrue(operation.get("deprecated").booleanValue());
        assertTrue(operation.get("externalDocs").isObject());
        assertTrue(operation.get("requestBody").isObject());
        assertTrue(operation.get("responses").isObject());
        assertTrue(operation.get("security").isArray());
        assertTrue(operation.get("parameters").isArray());
        assertTrue(operation.get("servers").isArray());
        assertTrue(operation.get("tags").isArray());
        JsonNode tags = operation.get("tags");
        assertEquals(2, tags.size());
        assertEquals("tag1", tags.get(0).textValue());
        assertEquals("tag2", tags.get(1).textValue());
    }

    @Test
    public void externalDocsHasExpectedFields() {
        JsonNode externalDocs = path(getOpenAPIJson(), "paths.path1.get.externalDocs");
        assertNotNull(externalDocs);
        assertEquals("description", externalDocs.get("description").textValue());
        assertEquals("url", externalDocs.get("url").textValue());
    }
}
