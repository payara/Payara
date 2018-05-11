package fish.payara.microprofile.openapi.test.app;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.junit.BeforeClass;
import org.junit.Test;

import fish.payara.microprofile.openapi.resource.rule.BaseProcessedDocument;
import fish.payara.microprofile.openapi.test.app.application.FormParamTest;
import fish.payara.microprofile.openapi.test.app.application.MethodMergeTest;
import fish.payara.microprofile.openapi.test.app.application.MethodTest;
import fish.payara.microprofile.openapi.test.app.application.ParameterTest;
import fish.payara.microprofile.openapi.test.app.application.PathTest;
import fish.payara.microprofile.openapi.test.app.application.RequestTest;
import fish.payara.microprofile.openapi.test.app.application.ResponseTest;
import fish.payara.microprofile.openapi.test.app.application.RootPathTest;
import fish.payara.microprofile.openapi.test.app.annotation.APIResponseTest;
import fish.payara.microprofile.openapi.test.app.annotation.CallbackTest;
import fish.payara.microprofile.openapi.test.app.annotation.ExtensionTest;
import fish.payara.microprofile.openapi.test.app.annotation.ExternalDocumentationTest;
import fish.payara.microprofile.openapi.test.app.annotation.OpenAPIDefinitionTest;
import fish.payara.microprofile.openapi.test.app.annotation.OperationTest;
import fish.payara.microprofile.openapi.test.app.annotation.ParameterAnnotationTest;
import fish.payara.microprofile.openapi.test.app.annotation.RequestBodyTest;
import fish.payara.microprofile.openapi.test.app.annotation.SchemaTest;
import fish.payara.microprofile.openapi.test.app.annotation.SecurityRequirementTest;
import fish.payara.microprofile.openapi.test.app.annotation.SecuritySchemeTest;
import fish.payara.microprofile.openapi.test.app.annotation.ServerTest;
import fish.payara.microprofile.openapi.test.app.annotation.TagTest;


@ApplicationPath("/test")
public class TestApplication extends Application {

    public transient static OpenAPI document;

    @BeforeClass
    public static void createDocument() {
        document = new BaseProcessedDocument();
    }

    @Override
    public Set<Class<?>> getClasses() {
        Set<Class<?>> classes = new HashSet<>();
        // All JAX-RS classes
        classes.add(RootPathTest.class);
        classes.add(PathTest.class);
        classes.add(MethodTest.class);
        classes.add(ParameterTest.class);
        classes.add(RequestTest.class);
        classes.add(ResponseTest.class);
        classes.add(MethodMergeTest.class);
        classes.add(FormParamTest.class);

        // All OpenAPI classes
        classes.add(OpenAPIDefinitionTest.class);
        classes.add(ServerTest.class);
        classes.add(TagTest.class);
        classes.add(RequestBodyTest.class);
        classes.add(APIResponseTest.class);
        classes.add(SchemaTest.class);
        classes.add(SecuritySchemeTest.class);
        classes.add(SecurityRequirementTest.class);
        classes.add(ParameterAnnotationTest.class);
        classes.add(OperationTest.class);
        classes.add(CallbackTest.class);
        classes.add(ExternalDocumentationTest.class);
        classes.add(ExtensionTest.class);
        return classes;
    }
    
    @Test
    public void testBase() {
        assertEquals("The document has the wrong version.", "3.0.0", document.getOpenapi());
        assertEquals("The document has the wrong title.", "Deployed Resources", document.getInfo().getTitle());
        assertEquals("The document has the wrong info version.", "1.0.0", document.getInfo().getVersion());
        assertEquals("The document has the wrong server list.", 1, document.getServers().size());
        assertEquals("The document has the wrong server URL.", "http://localhost:8080/testlocation_123",
                document.getServers().get(0).getUrl());
        assertNotNull("The document paths should be an empty array if empty.", document.getPaths());
        assertTrue("The document shouldn't have any paths.", document.getPaths().isEmpty());
        assertTrue("The document shouldn't have any extensions.", document.getExtensions().isEmpty());
        assertNull("The document shouldn't have any external docs.", document.getExternalDocs());
        assertTrue("The document shouldn't have any security properties.", document.getSecurity().isEmpty());
        assertTrue("The document shouldn't have any tags.", document.getTags().isEmpty());
    }

}