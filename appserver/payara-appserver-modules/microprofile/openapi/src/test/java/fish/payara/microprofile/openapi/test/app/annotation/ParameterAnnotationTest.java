package fish.payara.microprofile.openapi.test.app.annotation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.parameters.Parameter.In;
import org.junit.BeforeClass;
import org.junit.Test;

import fish.payara.microprofile.openapi.resource.rule.AnnotationProcessedDocument;

@Path("/parameter/openapi/")
public class ParameterAnnotationTest {

    public transient static OpenAPI document;

    @BeforeClass
    public static void createDocument() {
        document = new AnnotationProcessedDocument();
    }

    @GET
    @Path("/inline")
    public String inlineQueryAnnotation(
            @QueryParam("format")
            @Parameter(
                description = "The format of the output.",
                in = ParameterIn.COOKIE,
                schema = @Schema(
                    type = SchemaType.ARRAY,
                    description = "format parameter schema."
                )
            )
            String formatVar) {
        return null;
    }

    @Test
    public void inlineQueryAnnotationTest() {
        List<org.eclipse.microprofile.openapi.models.parameters.Parameter> parameters = document.getPaths()
                .get("/test/parameter/openapi/inline").getGET().getParameters();
        for (org.eclipse.microprofile.openapi.models.parameters.Parameter parameter : parameters) {
            if (parameter.getName().equals("format")) {
                // Test that the description has been set
                assertEquals("The parameter has the wrong description.", "The format of the output.",
                        parameter.getDescription());
                // Test that the input type hasn't been changed
                assertEquals("The parameter has the wrong location.", In.QUERY, parameter.getIn());
                // Test that the schema type hasn't been changed
                assertEquals("The parameter has the wrong schema type.",
                        org.eclipse.microprofile.openapi.models.media.Schema.SchemaType.STRING,
                        parameter.getSchema().getType());
                // Test that the schema description has filtered through
                assertEquals("The parameter has the wrong schema description.", "format parameter schema.",
                        parameter.getSchema().getDescription());
                return;
            }
        }
        fail("Parameter not found.");
    }

    @GET
    @Path("/method")
    @Parameter(name = "format", in = ParameterIn.QUERY, description = "The format of the output.")
    public String methodQueryAnnotation(@QueryParam("format") String formatVar) {
        return null;
    }

    @Test
    public void methodQueryAnnotationTest() {
        List<org.eclipse.microprofile.openapi.models.parameters.Parameter> parameters = document.getPaths()
                .get("/test/parameter/openapi/method").getGET().getParameters();
        for (org.eclipse.microprofile.openapi.models.parameters.Parameter parameter : parameters) {
            if (parameter.getName().equals("format")) {
                // Test that the description has been set
                assertEquals("The parameter has the wrong description.", "The format of the output.",
                        parameter.getDescription());
                return;
            }
        }
        fail("Parameter not found.");
    }
    
}