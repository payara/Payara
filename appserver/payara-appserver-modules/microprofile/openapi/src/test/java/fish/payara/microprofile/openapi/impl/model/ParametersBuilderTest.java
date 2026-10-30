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
import static java.util.Collections.singletonMap;
import static org.eclipse.microprofile.openapi.OASFactory.createContent;
import static org.eclipse.microprofile.openapi.OASFactory.createExample;
import static org.eclipse.microprofile.openapi.OASFactory.createMediaType;
import static org.eclipse.microprofile.openapi.OASFactory.createParameter;
import static org.eclipse.microprofile.openapi.OASFactory.createPathItem;
import static org.eclipse.microprofile.openapi.OASFactory.createPaths;
import static org.eclipse.microprofile.openapi.OASFactory.createRequestBody;
import static org.eclipse.microprofile.openapi.OASFactory.createSchema;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.parameters.Parameter.In;
import org.eclipse.microprofile.openapi.models.parameters.Parameter.Style;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Checks the JSON rendering of {@link fish.payara.microprofile.openapi.impl.model.parameters.*}.
 */
public class ParametersBuilderTest extends OpenApiBuilderTest {

    @Override
    protected void setupBaseDocument(OpenAPI document) {
        document.paths(createPaths()
                .addPathItem("path1", createPathItem()
                        .addParameter(createParameter()
                                .name("name")
                                .in(In.QUERY)
                                .description("description")
                                .required(true)
                                .deprecated(true)
                                .allowEmptyValue(true)
                                .style(Style.SIMPLE)
                                .explode(true)
                                .allowReserved(true)
                                .schema(createSchema().ref("ref"))
                                .example("example")
                                .examples(singletonMap("example1", createExample().ref("ref")))
                                .addExtension("x-ext", "ext-value")
                                .content(createContent().addMediaType("mediaType1", 
                                        createMediaType().schema(createSchema().ref("ref"))))
                                )));

        document.getComponents().addRequestBody("body1", createRequestBody()
                .description("description")
                .required(true)
                .content(createContent().addMediaType("type1", createMediaType())));
    }

    @Test
    public void parameterHasExpectedFields() {
        JsonNode parameter = path(getOpenAPIJson(), "paths.path1.parameters.[0]");
        assertNotNull(parameter);
        assertEquals("name", parameter.get("name").textValue());
        assertEquals("description", parameter.get("description").textValue());
        assertEquals("query", parameter.get("in").textValue());
        assertEquals("example", parameter.get("example").textValue());
        assertTrue(parameter.get("required").booleanValue());
        assertTrue(parameter.get("deprecated").booleanValue());
        assertTrue(parameter.get("allowEmptyValue").booleanValue());
        assertTrue(parameter.get("explode").booleanValue());
        assertTrue(parameter.get("allowReserved").booleanValue());
        assertTrue(parameter.get("schema").isObject());
        assertTrue(parameter.get("examples").isObject());
        assertEquals("description", parameter.get("description").textValue());
        assertEquals("description", parameter.get("description").textValue());
        assertEquals("ext-value", parameter.get("x-ext").textValue());
    }

    @Test
    public void requestBodyHasExpectedFields() {
        JsonNode requestBody = path(getOpenAPIJson(), "components.requestBodies.body1");
        assertNotNull(requestBody);
        assertEquals("description", requestBody.get("description").textValue());
        assertTrue(requestBody.get("required").booleanValue());
        assertTrue(requestBody.get("content").isObject());
    }
}
