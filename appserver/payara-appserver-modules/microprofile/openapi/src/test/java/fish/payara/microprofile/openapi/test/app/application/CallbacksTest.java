package fish.payara.microprofile.openapi.test.app.application;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.eclipse.microprofile.openapi.annotations.Components;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.callbacks.Callback;
import org.eclipse.microprofile.openapi.annotations.callbacks.CallbackOperation;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.junit.Test;

import com.fasterxml.jackson.databind.JsonNode;

import fish.payara.microprofile.openapi.test.app.OpenAPIApplicationTest;

@OpenAPIDefinition(
        info = @Info(title = "title", version = "version"),
        components = @Components(
                callbacks= {
                        @Callback(name = "name1", ref = "ref1"),
                        @Callback(name = "name2", callbackUrlExpression = "http://callbackUrlExpression.com", 
                            operations = 
                            @CallbackOperation(method = "GET", summary = "summary",
                                    responses = {
                                        @APIResponse(
                                            responseCode = "200",
                                            description = "successful operation",
                                            content = @Content(
                                                mediaType = "applictaion/json",
                                                schema = @Schema(
                                                    type = SchemaType.ARRAY,
                                                    implementation = String.class
                                                )
                                            )
                                        )}))
                }
                ))
@Path("/callbacks")
@Produces(MediaType.APPLICATION_JSON)
public class CallbacksTest extends OpenAPIApplicationTest {

    @Test
    public void callbackReference() {
        JsonNode callback = path(getOpenAPIJson(), "components.callbacks.name1");
        assertEquals(1, callback.size());
        assertEquals("#/components/callbacks/ref1", callback.get("$ref").textValue());
    }

    @Test
    public void callbackObject() {
        JsonNode callback = path(getOpenAPIJson(), "components.callbacks.name2");
        assertNull(callback.get("$ref"));
        JsonNode operations = callback.get("http://callbackUrlExpression.com");
        assertNotNull(operations);
        JsonNode getOperation = operations.get("get");
        assertNotNull(getOperation);
        assertEquals("summary", getOperation.get("summary").textValue());
        JsonNode responses = getOperation.get("responses");
        assertNotNull(responses);
        JsonNode response200 = responses.get("200");
        assertNotNull(response200);
        assertEquals("successful operation", response200.get("description").textValue());
        assertNotNull(response200.get("content"));
    }
}
