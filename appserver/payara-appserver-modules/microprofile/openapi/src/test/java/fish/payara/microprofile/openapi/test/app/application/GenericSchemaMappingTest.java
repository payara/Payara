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

import fish.payara.microprofile.openapi.test.app.OpenApiApplicationTest;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import static jakarta.ws.rs.core.MediaType.WILDCARD;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.media.Schema.SchemaType;
import org.eclipse.microprofile.openapi.models.responses.APIResponses;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

/**
 * A resource to test that schema classes with generic signature mapped
 * correctly.
 */
@Path("/response")
public class GenericSchemaMappingTest extends OpenApiApplicationTest {

    public class JsonData<T> {

        T data;

        public JsonData() {
        }

        public JsonData(T data) {
            this.data = data;
        }
    }

    public static class JsonItems<T> {

        List<T> items;
        Map<String, T> itemMap;
        Map<String, String> textMap;
        Map<String, List<T>> itemsMap;
        int totalItems;

        public JsonItems(List<T> items, int totalItems) {
            this.items = items;
            this.totalItems = totalItems;
        }
    }

    public class Animal {

        String name;
        int age;

        public Animal() {
        }

        public Animal(String name, int age) {
            this.name = name;
            this.age = age;
        }

    }

    @org.eclipse.microprofile.openapi.annotations.media.Schema(description = "JSON wrapper for a list of animals")
    public class JsonAnimalList extends JsonData<JsonItems<Animal>> {

        public JsonAnimalList(List<Animal> items, int totalItems) {
            super(new JsonItems<Animal>(items, totalItems));
        }
    }

    @GET
    public JsonAnimalList loadAnimals() {
        List<Animal> animals = new ArrayList<>();
        animals.add(new Animal("Leo the tiger", 7));
        animals.add(new Animal("Joe the wolf", 11));

        return new JsonAnimalList(animals, animals.size());
    }

    @Test
    public void genericSchemaTest() {
        APIResponses responses = getDocument().getPaths().getPathItem("/test/response").getGET().getResponses();
        assertNotNull("The default response should have been created.",
                responses.getDefaultValue());
        assertNotNull("The default response should return */*.",
                responses.getDefaultValue().getContent().getMediaType(WILDCARD));
        assertEquals("The default response */* should match the specified schema.",
                "#/components/schemas/JsonAnimalList",
                responses.getDefaultValue().getContent().getMediaType(WILDCARD).getSchema().getRef());

        Map<String, Schema> schemas = getDocument().getComponents().getSchemas();
        assertEquals(2, schemas.size());

        Schema jsonAnimalList = schemas.get("JsonAnimalList");
        assertNotNull(jsonAnimalList);
        assertEquals(1, jsonAnimalList.getProperties().size());
        assertEquals("JSON wrapper for a list of animals", jsonAnimalList.getDescription());
        assertEquals(SchemaType.OBJECT, jsonAnimalList.getType());

        Schema data = jsonAnimalList.getProperties().get("data");
        assertNotNull(data);
        assertEquals(5, data.getProperties().size());
        assertEquals(SchemaType.OBJECT, data.getType());

        Schema totalItems = data.getProperties().get("totalItems");
        assertNotNull(totalItems);
        assertEquals(SchemaType.INTEGER, totalItems.getType());

        Schema items = data.getProperties().get("items");
        assertNotNull(items);
        assertEquals(SchemaType.ARRAY, items.getType());
        assertEquals("#/components/schemas/Animal", items.getItems().getRef());

        Schema itemMap = data.getProperties().get("itemMap");
        assertNotNull(itemMap);
        assertEquals(SchemaType.OBJECT, itemMap.getType());
        assertNotNull(itemMap.getAdditionalPropertiesSchema());
        assertEquals("#/components/schemas/Animal", itemMap.getAdditionalPropertiesSchema().getRef());

        Schema textMap = data.getProperties().get("textMap");
        assertNotNull(textMap);
        assertEquals(SchemaType.OBJECT, textMap.getType());
        assertNotNull(textMap.getAdditionalPropertiesSchema());
        assertEquals(SchemaType.STRING, textMap.getAdditionalPropertiesSchema().getType());

        Schema itemsMap = data.getProperties().get("itemsMap");
        assertNotNull(itemsMap);
        assertEquals(SchemaType.OBJECT, itemsMap.getType());
        assertNotNull(itemsMap.getAdditionalPropertiesSchema());
        assertEquals(SchemaType.ARRAY, itemsMap.getAdditionalPropertiesSchema().getType());
        assertEquals("#/components/schemas/Animal", itemsMap.getAdditionalPropertiesSchema().getItems().getRef());

        Schema animal = schemas.get("Animal");
        assertNotNull(animal);
        assertEquals(2, animal.getProperties().size());
        assertEquals(SchemaType.OBJECT, animal.getType());

        Schema name = animal.getProperties().get("name");
        assertNotNull(name);
        assertEquals(SchemaType.STRING, name.getType());

        Schema age = animal.getProperties().get("age");
        assertNotNull(age);
        assertEquals(SchemaType.INTEGER, age.getType());
    }

}
