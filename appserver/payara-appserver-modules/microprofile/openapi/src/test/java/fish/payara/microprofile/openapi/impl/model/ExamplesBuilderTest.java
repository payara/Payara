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
import com.fasterxml.jackson.databind.ObjectMapper;
import static fish.payara.microprofile.openapi.test.util.JsonUtils.path;
import java.util.Arrays;
import static org.eclipse.microprofile.openapi.OASFactory.createExample;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Checks the JSON rendering of {@link fish.payara.microprofile.openapi.impl.model.examples.*}.
 */
public class ExamplesBuilderTest extends OpenApiBuilderTest {

    @Override
    protected void setupBaseDocument(OpenAPI document) {
        document.getComponents()
            .addExample("example1", createExample()
                    .description("description")
                    .summary("summary")
                    .value("value")
                    .externalValue("externalValue")
                    .addExtension("x-ext", "ext-value")
                    )
            .addExample("example2", createExample()
                    .value(new ObjectMapper().createArrayNode().add("a").add("b"))
                    )
            .addExample("example3", createExample()
                    .value(Arrays.asList("a", "b")));
    }

    @Test
    public void exampleHasExpectedFields() {
        JsonNode example1 = path(getOpenAPIJson(), "components.examples.example1");
        assertNotNull(example1);
        assertEquals("summary", example1.get("summary").textValue());
        assertEquals("description", example1.get("description").textValue());
        assertEquals("externalValue", example1.get("externalValue").textValue());
        assertEquals("value", example1.get("value").textValue());
        assertEquals("ext-value", example1.get("x-ext").textValue());
    }

    @Test
    public void exampleCanUseJsonValue() {
        JsonNode example2 = path(getOpenAPIJson(), "components.examples.example2");
        assertNotNull(example2);
        assertTrue(example2.get("value").isArray());
        assertEquals("a", example2.get("value").get(0).textValue());
        assertEquals("b", example2.get("value").get(1).textValue());
    }

    @Test
    public void exampleCanUseCollectionValue() {
        JsonNode example3 = path(getOpenAPIJson(), "components.examples.example3");
        assertNotNull(example3);
        assertTrue(example3.get("value").isArray());
        assertEquals("a", example3.get("value").get(0).textValue());
        assertEquals("b", example3.get("value").get(1).textValue());
    }
}
