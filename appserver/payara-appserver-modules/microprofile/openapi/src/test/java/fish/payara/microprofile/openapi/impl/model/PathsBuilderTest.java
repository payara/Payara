package fish.payara.microprofile.openapi.impl.model;

import static fish.payara.microprofile.openapi.test.util.JsonUtils.path;
import static org.eclipse.microprofile.openapi.OASFactory.createAPIResponse;
import static org.eclipse.microprofile.openapi.OASFactory.createAPIResponses;
import static org.eclipse.microprofile.openapi.OASFactory.createOperation;
import static org.eclipse.microprofile.openapi.OASFactory.createParameter;
import static org.eclipse.microprofile.openapi.OASFactory.createPathItem;
import static org.eclipse.microprofile.openapi.OASFactory.createPaths;
import static org.eclipse.microprofile.openapi.OASFactory.createServer;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.parameters.Parameter.In;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

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
