/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2020-2021] Payara Foundation and/or its affiliates. All rights reserved.
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
package fish.payara.microprofile.openapi.test.app.application;

import static fish.payara.microprofile.openapi.test.util.JsonUtils.path;
import static org.junit.Assert.assertEquals;

import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import org.junit.Test;

import fish.payara.microprofile.openapi.test.app.OpenApiApplicationTest;

@Path("enum")
public class EnumTest extends OpenApiApplicationTest {

    @Path("/add")
    @POST
    String add(Data body) {
        return null;
    }

    class Data {
        MyEnum prop1;
    }

    enum MyEnum {
        A, B, C
    }

    @Test
    public void testSchemaReferenceCreated() {
        assertEquals("#/components/schemas/Data",
                path(getOpenAPIJson(), "paths./test/enum/add.post.requestBody.content.application/json.schema.$ref").asText());
    }

    @Test
    public void testEnumPropertyReferenceCreated() {
        JsonNode data = path(getOpenAPIJson(), "components.schemas.Data.properties.prop1.$ref");
        assertEquals("#/components/schemas/MyEnum", data.asText());
    }

    @Test
    public void testEnumSchemaCreated() {
        ArrayNode enumProps = (ArrayNode) path(getOpenAPIJson(), "components.schemas.MyEnum.enum");
        assertEquals("A", enumProps.get(0).textValue());
        assertEquals("B", enumProps.get(1).textValue());
        assertEquals("C", enumProps.get(2).textValue());
        assertEquals("Enum contained an incorrect number of props: " + enumProps, 3, enumProps.size());
    }

}
