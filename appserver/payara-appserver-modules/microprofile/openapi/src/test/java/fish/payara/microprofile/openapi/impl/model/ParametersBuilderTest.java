package fish.payara.microprofile.openapi.impl.model;

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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.parameters.Parameter.In;
import org.eclipse.microprofile.openapi.models.parameters.Parameter.Style;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

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
        JsonNode parameter = path(getOpenAPIJson(), "paths.path1.parameters.0");
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
