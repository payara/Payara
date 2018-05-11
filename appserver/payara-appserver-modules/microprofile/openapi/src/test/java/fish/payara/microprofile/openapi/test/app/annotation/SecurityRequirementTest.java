package fish.payara.microprofile.openapi.test.app.annotation;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirements;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.PathItem.HttpMethod;
import org.junit.BeforeClass;
import org.junit.Test;

import fish.payara.microprofile.openapi.resource.rule.AnnotationProcessedDocument;
import fish.payara.microprofile.openapi.resource.util.TestUtils;

@SecurityRequirement(name = "securityScheme1", scopes = { "oauthScope1" })
@SecurityRequirements({
    @SecurityRequirement(name = "methodDeclaredApiKey", scopes = { "read" })
})
@Path("/security/requirement")
public class SecurityRequirementTest {

    public transient static OpenAPI document;

    @BeforeClass
    public static void createDocument() {
        document = new AnnotationProcessedDocument();
    }

    @GET
    @Path("/specified")
    @SecurityRequirement(name = "classDeclaredOAuth", scopes = { "write" })
    @SecurityRequirements({
        @SecurityRequirement(name = "notDeclared", scopes = { "whatever" })
    })
    public String specifiedRequirement() {
        return null;
    }

    @Test
    public void specifiedRequirementTest() {
        TestUtils.testSecurityRequirement(document, "/test/security/requirement/specified", HttpMethod.GET,
                "classDeclaredOAuth", "write");
        TestUtils.testSecurityRequirement(document, "/test/security/requirement/specified", HttpMethod.GET,
                "notDeclared", "whatever");
        TestUtils.testNotSecurityRequirement(document, "/test/security/requirement/specified", HttpMethod.GET,
                "securityScheme1");
        TestUtils.testNotSecurityRequirement(document, "/test/security/requirement/specified", HttpMethod.GET,
                "methodDeclaredApiKey");
    }

    @GET
    @Path("/inherited")
    public String inheritedRequirement() {
        return null;
    }

    @Test
    public void inheritedRequirementTest() {
        TestUtils.testSecurityRequirement(document, "/test/security/requirement/inherited", HttpMethod.GET,
                "securityScheme1", "oauthScope1");
        TestUtils.testSecurityRequirement(document, "/test/security/requirement/inherited", HttpMethod.GET,
                "methodDeclaredApiKey", "read");
        TestUtils.testNotSecurityRequirement(document, "/test/security/requirement/inherited", HttpMethod.GET,
                "classDeclaredOAuth");
        TestUtils.testNotSecurityRequirement(document, "/test/security/requirement/inherited", HttpMethod.GET,
                "notDeclared");
    }
}