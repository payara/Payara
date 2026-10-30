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
import static org.eclipse.microprofile.openapi.OASFactory.createContent;
import static org.eclipse.microprofile.openapi.OASFactory.createExample;
import static org.eclipse.microprofile.openapi.OASFactory.createHeader;
import static org.eclipse.microprofile.openapi.OASFactory.createMediaType;
import static org.eclipse.microprofile.openapi.OASFactory.createSchema;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.headers.Header.Style;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Checks the JSON rendering of {@link fish.payara.microprofile.openapi.impl.model.headers.*}.
 */
public class HeadersBuilderTest extends OpenApiBuilderTest {

    @Override
    protected void setupBaseDocument(OpenAPI document) {
        document.getComponents().addHeader("header1", createHeader()
                .required(true)
                .deprecated(true)
                .allowEmptyValue(true)
                .explode(true)
                .style(Style.SIMPLE)
                .description("description")
                .example("example")
                .content(createContent().addMediaType("type1", createMediaType().schema(createSchema().ref("ref"))))
                .schema(createSchema().ref("ref"))
                .addExample("example1", createExample().ref("ref"))
                .addExtension("x-ext", "ext-value")
                );
    }

    @Test
    public void headerHasExpectedFields() {
        JsonNode header = path(getOpenAPIJson(), "components.headers.header1");
        assertNotNull(header);
        assertEquals("description", header.get("description").textValue());
        assertEquals("example", header.get("example").textValue());
        assertEquals("simple", header.get("style").textValue());
        assertEquals("ext-value", header.get("x-ext").textValue());
        assertTrue(header.get("required").booleanValue());
        assertTrue(header.get("deprecated").booleanValue());
        assertTrue(header.get("allowEmptyValue").booleanValue());
        assertTrue(header.get("explode").booleanValue());
        assertTrue(header.get("content").isObject());
        assertTrue(header.get("schema").isObject());
        assertTrue(header.get("examples").isObject());
        assertTrue(header.get("examples").get("example1").isObject());
    }
}
