package fish.payara.microprofile.openapi.test.app.annotation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.junit.BeforeClass;
import org.junit.Test;

import fish.payara.microprofile.openapi.resource.rule.AnnotationProcessedDocument;

@Path("/operation")
public class OperationTest {

    public transient static OpenAPI document;

    @BeforeClass
    public static void createDocument() {
        document = new AnnotationProcessedDocument();
    }

    @GET
    @Path("/specified")
    @Operation(description = "A GET operation.", operationId = "getOperation", summary = "GET operation summary.")
    public String specifiedOperation() {
        return null;
    }

    @Test
    public void specifiedOperationTest() {
        org.eclipse.microprofile.openapi.models.Operation operation = document.getPaths()
                .get("/test/operation/specified").getGET();
        assertEquals("Operation had the wrong description.", "A GET operation.", operation.getDescription());
        assertEquals("Operation had the wrong operation id.", "getOperation", operation.getOperationId());
        assertEquals("Operation had the wrong summary.", "GET operation summary.", operation.getSummary());
    }

    @GET
    @Path("/hidden")
    @Operation(hidden = true)
    public String hiddenOperation() {
        return null;
    }

    @Test
    public void hiddenOperationTest() {
        org.eclipse.microprofile.openapi.models.Operation operation = document.getPaths()
                .get("/test/operation/hidden").getGET();
        assertNull("Operation was not hidden.", operation);
    }

}