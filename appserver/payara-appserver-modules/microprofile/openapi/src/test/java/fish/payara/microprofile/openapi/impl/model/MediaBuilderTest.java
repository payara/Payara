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
import java.math.BigDecimal;
import static org.eclipse.microprofile.openapi.OASFactory.createAPIResponse;
import static org.eclipse.microprofile.openapi.OASFactory.createContent;
import static org.eclipse.microprofile.openapi.OASFactory.createDiscriminator;
import static org.eclipse.microprofile.openapi.OASFactory.createEncoding;
import static org.eclipse.microprofile.openapi.OASFactory.createExample;
import static org.eclipse.microprofile.openapi.OASFactory.createExternalDocumentation;
import static org.eclipse.microprofile.openapi.OASFactory.createHeader;
import static org.eclipse.microprofile.openapi.OASFactory.createMediaType;
import static org.eclipse.microprofile.openapi.OASFactory.createSchema;
import static org.eclipse.microprofile.openapi.OASFactory.createXML;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.media.Discriminator;
import org.eclipse.microprofile.openapi.models.media.Encoding;
import org.eclipse.microprofile.openapi.models.media.Encoding.Style;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.media.Schema.SchemaType;
import org.eclipse.microprofile.openapi.models.media.XML;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

/**
 * Checks the JSON rendering of {@link fish.payara.microprofile.openapi.impl.model.media.*}.
 */
public class MediaBuilderTest extends OpenApiBuilderTest {

    @Override
    protected void setupBaseDocument(OpenAPI document) {
        document.getComponents().addSchema("SimpleMap", createSchema()
                .type(SchemaType.OBJECT)
                .additionalPropertiesSchema(createSchema().type(SchemaType.STRING)));

        XML xml = createXML()
                .name("name")
                .namespace("namespace")
                .prefix("prefix")
                .attribute(true)
                .wrapped(true)
                .addExtension("x-ext", "ext-value");

        document.getComponents().addSchema("XML", createSchema()
                .xml(xml));

        Encoding encoding = createEncoding()
                .contentType("contentType")
                .style(Style.FORM)
                .explode(true)
                .allowReserved(true)
                .addExtension("x-ext", "ext-value")
                .addHeader("header1", createHeader().ref("ref1"))
                .addHeader("header2", createHeader().ref("ref2"));

        MediaType mediaType = createMediaType()
                .example("example")
                .schema(createSchema().ref("ref"))
                .addExtension("x-ext", "ext-value")
                .addExample("example1", createExample().ref("ref1"))
                .addExample("example2", createExample().ref("ref2"))
                .addEncoding("encoding1", encoding);

        document.getComponents().addResponse("MediaType", createAPIResponse()
                .description("description")
                .content(createContent()
                        .addMediaType("type1", mediaType)));

        Discriminator discriminator = createDiscriminator()
                .propertyName("propertyName")
                .addMapping("key1", "value1")
                .addMapping("key2", "value2");

        document.getComponents().addSchema("Discriminator", createSchema()
                .discriminator(discriminator));

        document.getComponents().addSchema("Schema", createSchema()
                .title("title")
                .multipleOf(BigDecimal.ONE)
                .maximum(BigDecimal.ONE)
                .exclusiveMaximum(true)
                .minimum(BigDecimal.ONE)
                .exclusiveMinimum(true)
                .maxLength(10)
                .minLength(1)
                .pattern("pattern")
                .maxItems(11)
                .minItems(2)
                .uniqueItems(true)
                .maxProperties(12)
                .minProperties(3)
                .addRequired("required1")
                .addRequired("required2")
                .type(SchemaType.NUMBER)
                .not(createSchema().ref("not"))
                .addProperty("property1", createSchema().ref("property1"))
                .description("description")
                .format("format")
                .nullable(true)
                .readOnly(true)
                .writeOnly(true)
                .example("example")
                .externalDocs(createExternalDocumentation().url("url"))
                .deprecated(true)
                .xml(xml)
                .addEnumeration("enumeration1")
                .addEnumeration("enumeration2")
                .discriminator(discriminator)
                .addAnyOf(createSchema().ref("anyOf1"))
                .addAnyOf(createSchema().ref("anyOf2"))
                .addAllOf(createSchema().ref("allOf1"))
                .addAllOf(createSchema().ref("allOf2"))
                .addOneOf(createSchema().ref("oneOf1"))
                .addOneOf(createSchema().ref("oneOf2"))
                .additionalPropertiesBoolean(true)
                .items(createSchema().ref("items"))
                .addExtension("x-ext", "ext-value")
                );
    }

    @Test
    public void simpleMapSchemaHasExpectedFields() {
        JsonNode mapEntry = path(getOpenAPIJson(), "components.schemas.SimpleMap");
        assertNotNull(mapEntry);
        assertEquals("object", mapEntry.get("type").textValue());
        JsonNode additionalProperties = mapEntry.get("additionalProperties");
        assertNotNull(additionalProperties);
        assertEquals("string", additionalProperties.get("type").textValue());
    }

    @Test
    public void xmlHasExpectedFields() {
        JsonNode xml = path(getOpenAPIJson(), "components.schemas.XML.xml");
        assertNotNull(xml);
        assertEquals("name", xml.get("name").textValue());
        assertEquals("namespace", xml.get("namespace").textValue());
        assertEquals("prefix", xml.get("prefix").textValue());
        assertEquals("ext-value", xml.get("x-ext").textValue());
        assertTrue(xml.get("attribute").booleanValue());
        assertTrue(xml.get("wrapped").booleanValue());
    }

    @Test
    public void mediaTypeHasExpectedFields() {
        JsonNode mediaType = path(getOpenAPIJson(), "components.responses.MediaType.content.type1");
        assertNotNull(mediaType);
        assertEquals("example", mediaType.get("example").textValue());
        assertEquals("ext-value", mediaType.get("x-ext").textValue());
        assertTrue(mediaType.get("schema").isObject());
        JsonNode examples = mediaType.get("examples");
        assertTrue(examples.isObject());
        assertEquals(2, examples.size());
        assertTrue(examples.get("example1").isObject());
        assertTrue(examples.get("example2").isObject());
        JsonNode encodings = mediaType.get("encoding");
        assertTrue(encodings.isObject());
        assertEquals(1, encodings.size());
        assertTrue(encodings.get("encoding1").isObject());
    }

    @Test
    public void encodingHasExpectedFields() {
        JsonNode encoding = path(getOpenAPIJson(), "components.responses.MediaType.content.type1.encoding.encoding1");
        assertNotNull(encoding);
        assertEquals("contentType", encoding.get("contentType").textValue());
        assertEquals("form", encoding.get("style").textValue());
        assertEquals("ext-value", encoding.get("x-ext").textValue());
        assertTrue(encoding.get("explode").booleanValue());
        assertTrue(encoding.get("allowReserved").booleanValue());
        JsonNode headers = encoding.get("headers");
        assertTrue(headers.isObject());
        assertEquals(2, headers.size());
        assertTrue(headers.get("header1").isObject());
        assertTrue(headers.get("header2").isObject());
    }

    @Test
    public void discriminatorHasExpectedFields() {
        JsonNode discriminator = path(getOpenAPIJson(), "components.schemas.Discriminator.discriminator");
        assertNotNull(discriminator);
        assertEquals("propertyName", discriminator.get("propertyName").textValue());
        JsonNode mapping = discriminator.get("mapping");
        assertTrue(mapping.isObject());
        assertEquals(2, mapping.size());
        assertEquals("value1", mapping.get("key1").textValue());
        assertEquals("value2", mapping.get("key2").textValue());
    }

    @Test
    public void schemaHasExpectedFields() {
        JsonNode schema = path(getOpenAPIJson(), "components.schemas.Schema");
        assertNotNull(schema);
        assertEquals("ext-value", schema.get("x-ext").textValue());
        assertEquals("title", schema.get("title").textValue());
        assertEquals(BigDecimal.ONE, schema.get("multipleOf").decimalValue());
        assertEquals(BigDecimal.ONE, schema.get("maximum").decimalValue());
        assertTrue(schema.get("exclusiveMaximum").booleanValue());
        assertEquals(BigDecimal.ONE, schema.get("minimum").decimalValue());
        assertTrue(schema.get("exclusiveMinimum").booleanValue());
        assertEquals(10, schema.get("maxLength").intValue());
        assertEquals(1, schema.get("minLength").intValue());
        assertEquals("pattern", schema.get("pattern").textValue());
        assertEquals(11, schema.get("maxItems").intValue());
        assertEquals(2, schema.get("minItems").intValue());
        assertTrue(schema.get("uniqueItems").booleanValue());
        assertEquals(12, schema.get("maxProperties").intValue());
        assertEquals(3, schema.get("minProperties").intValue());
        JsonNode required = schema.get("required");
        assertTrue(required.isArray());
        assertEquals(2, required.size());
        assertEquals("required1", required.get(0).textValue());
        assertEquals("required2", required.get(1).textValue());
        assertEquals("number", schema.get("type").textValue());
        assertTrue(schema.get("not").isObject());
        assertTrue(schema.get("properties").isObject());
        assertTrue(schema.get("properties").get("property1").isObject());
        assertEquals("description", schema.get("description").textValue());
        assertEquals("format", schema.get("format").textValue());
        assertTrue(schema.get("nullable").booleanValue());
        assertTrue(schema.get("readOnly").booleanValue());
        assertTrue(schema.get("writeOnly").booleanValue());
        assertEquals("example", schema.get("example").textValue());
        assertTrue(schema.get("externalDocs").isObject());
        assertTrue(schema.get("deprecated").booleanValue());
        assertTrue(schema.get("xml").isObject());
        JsonNode enumeration = schema.get("enum");
        assertTrue(enumeration.isArray());
        assertEquals(2, enumeration.size());
        assertEquals("enumeration1", enumeration.get(0).textValue());
        assertEquals("enumeration2", enumeration.get(1).textValue());
        assertTrue(schema.get("discriminator").isObject());
        JsonNode anyOf = schema.get("anyOf");
        assertTrue(anyOf.isArray());
        assertEquals(2, anyOf.size());
        JsonNode allOf = schema.get("allOf");
        assertTrue(allOf.isArray());
        assertEquals(2, allOf.size());
        JsonNode oneOf = schema.get("oneOf");
        assertTrue(oneOf.isArray());
        assertEquals(2, oneOf.size());
        assertTrue(schema.get("additionalProperties").booleanValue());
        assertTrue(schema.get("items").isObject());
    }
}
