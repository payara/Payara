package fish.payara.microprofile.openapi.test.app.annotation;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.microprofile.openapi.annotations.callbacks.Callback;
import org.eclipse.microprofile.openapi.annotations.callbacks.CallbackOperation;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.PathItem.HttpMethod;
import org.junit.BeforeClass;
import org.junit.Test;

import fish.payara.microprofile.openapi.resource.rule.AnnotationProcessedDocument;
import fish.payara.microprofile.openapi.resource.util.TestUtils;

@Path("/callback")
public class CallbackTest {

    public transient static OpenAPI document;

    @BeforeClass
    public static void createDocument() {
        document = new AnnotationProcessedDocument();
    }

    @GET
    @Path("/specified")
    @Callback(
        name = "testCallback",
        callbackUrlExpression = "api/callbackUrl",
        operations = {
            @CallbackOperation(method = "POST", description = "The callback operation.")
        }
    )
    @Callback(
        name = "testCallback",
        callbackUrlExpression = "api/callbackUrl2",
        operations = {
            @CallbackOperation(method = "POST", description = "The callback2 operation.")
        }
    )
    @Callback(
        name = "testCallback2",
        callbackUrlExpression = "whatever",
        operations = {
            @CallbackOperation(method = "OPTIONS", description = "The second callback operation.")
        }
    )
    public String specifiedCallback() {
        return null;
    }

    @Test
    public void specifiedCallbackTest() {
        TestUtils.testCallback(document, "/test/callback/specified", HttpMethod.GET, "testCallback", "api/callbackUrl",
                HttpMethod.POST, "The callback operation.");
        TestUtils.testCallback(document, "/test/callback/specified", HttpMethod.GET, "testCallback", "api/callbackUrl2",
                HttpMethod.POST, "The callback2 operation.");
        TestUtils.testCallback(document, "/test/callback/specified", HttpMethod.GET, "testCallback2", "whatever",
                HttpMethod.OPTIONS, "The second callback operation.");
    }

}