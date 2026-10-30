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
import com.fasterxml.jackson.databind.node.ArrayNode;
import static fish.payara.microprofile.openapi.test.util.JsonUtils.path;
import static org.eclipse.microprofile.openapi.OASFactory.createAPIResponse;
import static org.eclipse.microprofile.openapi.OASFactory.createAPIResponses;
import static org.eclipse.microprofile.openapi.OASFactory.createOperation;
import static org.eclipse.microprofile.openapi.OASFactory.createParameter;
import static org.eclipse.microprofile.openapi.OASFactory.createPathItem;
import static org.eclipse.microprofile.openapi.OASFactory.createPaths;
import static org.eclipse.microprofile.openapi.OASFactory.createServer;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.parameters.Parameter.In;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Checks the JSON rendering of {@link PathsImpl} and {@link PathItemImpl}.
 */
public class PathsBuilderTest extends OpenApiBuilderTest {

    @Override
    protected void setupBaseDocument(OpenAPI document) {
        Operation operation = createOperation()
                .summary("summary")
                .description("description")
                .responses(createAPIResponses()
                        .addAPIResponse("200", createAPIResponse()
                                .ref("ref")));
        document.paths(createPaths()
                .addExtension("x-ext", "ext-value")
                .addPathItem("/item1/", createPathItem()
                        .summary("summary")
                        .description("description")
                        .GET(operation)
                        .DELETE(operation)
                        .HEAD(operation)
                        .OPTIONS(operation)
                        .PATCH(operation)
                        .POST(operation)
                        .PUT(operation)
                        .TRACE(operation)
                        .addExtension("x-ext", "ext-value")
                        .addServer(createServer().url("url1"))
                        .addServer(createServer().url("url2"))
                        .addParameter(createParameter().name("name1").in(In.QUERY))
                        .addParameter(createParameter().name("name2").in(In.COOKIE))
                        ));
    }

    @Test
    public void pathsHasExpectedFields() {
        JsonNode paths = getOpenAPIJson().get("paths");
        assertNotNull(paths);
        assertEquals("ext-value", paths.get("x-ext").textValue());
    }

    @Test
    public void pathHasExpectedFields() {
        JsonNode pathItem1 = path(getOpenAPIJson(), "paths./item1/");
        assertNotNull(pathItem1);
        assertEquals("description", pathItem1.get("description").textValue());
        assertEquals("summary", pathItem1.get("summary").textValue());
        assertEquals("ext-value", pathItem1.get("x-ext").textValue());
        assertTrue(pathItem1.get("post").isObject());
        assertTrue(pathItem1.get("put").isObject());
        assertTrue(pathItem1.get("get").isObject());
        assertTrue(pathItem1.get("delete").isObject());
        assertTrue(pathItem1.get("options").isObject());
        assertTrue(pathItem1.get("head").isObject());
        assertTrue(pathItem1.get("patch").isObject());
        assertTrue(pathItem1.get("trace").isObject());
        assertTrue(pathItem1.get("parameters").isArray());
        ArrayNode parameters = (ArrayNode) pathItem1.get("parameters");
        assertEquals(2, parameters.size());
    }
}
