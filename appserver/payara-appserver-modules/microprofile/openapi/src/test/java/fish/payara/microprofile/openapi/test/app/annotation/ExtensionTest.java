package fish.payara.microprofile.openapi.test.app.annotation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Map;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.junit.BeforeClass;
import org.junit.Test;

import fish.payara.microprofile.openapi.resource.rule.AnnotationProcessedDocument;

@Path("/extension")
@Extension(name = "class-extension", value = "http://extension")
public class ExtensionTest {

    public transient static OpenAPI document;

    @BeforeClass
    public static void createDocument() {
        document = new AnnotationProcessedDocument();
    }

    @Test
    public void classExtensionTest() {
        Map<String, Object> extensions = document.getExtensions();
        assertNotNull("No extensions were found.", extensions);
        assertTrue("Extension not found.", extensions.containsKey("class-extension"));
        assertEquals("Extension had wrong value.", extensions.get("class-extension"), "http://extension");
    }

    @GET
    @Path("/specified")
    @Extension(name = "method-extension", value = "http://extension2")
    public String methodExtension() {
        return null;
    }

    @Test
    public void methodExtensionTest() {
        Map<String, Object> extensions = document.getPaths().get("/test/extension/specified").getGET().getExtensions();
        assertNotNull("No extensions were found.", extensions);
        assertTrue("Extension not found.", extensions.containsKey("method-extension"));
        assertEquals("Extension had wrong value.", extensions.get("method-extension"), "http://extension2");
    }
}