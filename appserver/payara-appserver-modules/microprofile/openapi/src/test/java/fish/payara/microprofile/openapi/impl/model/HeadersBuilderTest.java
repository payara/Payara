package fish.payara.microprofile.openapi.impl.model;

import static fish.payara.microprofile.openapi.test.util.JsonUtils.path;
import static org.eclipse.microprofile.openapi.OASFactory.createContent;
import static org.eclipse.microprofile.openapi.OASFactory.createExample;
import static org.eclipse.microprofile.openapi.OASFactory.createHeader;
import static org.eclipse.microprofile.openapi.OASFactory.createMediaType;
import static org.eclipse.microprofile.openapi.OASFactory.createSchema;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.headers.Header.Style;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Checks the JSON rendering of {@link fish.payara.microprofile.openapi.impl.model.headers.HeaderImpl}.
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
