package fish.payara.microprofile.openapi.test.app.annotation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.microprofile.openapi.annotations.ExternalDocumentation;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.junit.BeforeClass;
import org.junit.Test;

import fish.payara.microprofile.openapi.resource.rule.AnnotationProcessedDocument;

@Path("/externaldocs")
@ExternalDocumentation(description = "Inherited external documentation.", url = "http://inherited")
public class ExternalDocumentationTest {

    public transient static OpenAPI document;

    @BeforeClass
    public static void createDocument() {
        document = new AnnotationProcessedDocument();
    }

    @GET
    @Path("/specified")
    @ExternalDocumentation(description = "Specified external documentation.", url = "http://external-docs")
    public String specifiedExternalDocs() {
        return null;
    }

    @Test
    public void specifiedExternalDocsTest() {
        org.eclipse.microprofile.openapi.models.ExternalDocumentation externalDocs = document.getPaths()
                .get("/test/externaldocs/specified").getGET().getExternalDocs();
        assertNotNull("No external docs found.", externalDocs);
        assertEquals("Incorrect description.", "Specified external documentation.", externalDocs.getDescription());
        assertEquals("Incorrect url.", "http://external-docs", externalDocs.getUrl());
    }

    @GET
    @Path("/inherited")
    public String inheritedExternalDocs() {
        return null;
    }

    @Test
    public void inheritedExternalDocsTest() {
        org.eclipse.microprofile.openapi.models.ExternalDocumentation externalDocs = document.getPaths()
                .get("/test/externaldocs/inherited").getGET().getExternalDocs();
        assertNotNull("No external docs found.", externalDocs);
        assertEquals("Incorrect description.", "Inherited external documentation.", externalDocs.getDescription());
        assertEquals("Incorrect url.", "http://inherited", externalDocs.getUrl());
    }

    @GET
    @Path("/ignored")
    @ExternalDocumentation
    public String ignoredExternalDocs() {
        return null;
    }

    @Test
    public void ignoredExternalDocsTest() {
        org.eclipse.microprofile.openapi.models.ExternalDocumentation externalDocs = document.getPaths()
                .get("/test/externaldocs/ignored").getGET().getExternalDocs();
        assertNull("External docs found.", externalDocs);
    }

}