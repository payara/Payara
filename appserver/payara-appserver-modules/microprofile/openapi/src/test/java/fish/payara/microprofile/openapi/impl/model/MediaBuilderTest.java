package fish.payara.microprofile.openapi.impl.model;

import static fish.payara.microprofile.openapi.test.util.JsonUtils.path;
import static org.eclipse.microprofile.openapi.OASFactory.createAPIResponse;
import static org.eclipse.microprofile.openapi.OASFactory.createContent;
import static org.eclipse.microprofile.openapi.OASFactory.createDiscriminator;
import static org.eclipse.microprofile.openapi.OASFactory.createEncoding;
import static org.eclipse.microprofile.openapi.OASFactory.createExample;
import static org.eclipse.microprofile.openapi.OASFactory.createHeader;
import static org.eclipse.microprofile.openapi.OASFactory.createMediaType;
import static org.eclipse.microprofile.openapi.OASFactory.createSchema;
import static org.eclipse.microprofile.openapi.OASFactory.createXML;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.media.Discriminator;
import org.eclipse.microprofile.openapi.models.media.Encoding;
import org.eclipse.microprofile.openapi.models.media.Encoding.Style;
import org.eclipse.microprofile.openapi.models.media.MediaType;
import org.eclipse.microprofile.openapi.models.media.Schema.SchemaType;
import org.eclipse.microprofile.openapi.models.media.XML;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

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
}
