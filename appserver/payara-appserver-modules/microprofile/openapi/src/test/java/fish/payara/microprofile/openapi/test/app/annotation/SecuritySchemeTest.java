package fish.payara.microprofile.openapi.test.app.annotation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeIn;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.security.OAuthFlow;
import org.eclipse.microprofile.openapi.annotations.security.OAuthFlows;
import org.eclipse.microprofile.openapi.annotations.security.OAuthScope;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme.In;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme.Type;
import org.junit.BeforeClass;
import org.junit.Test;

import fish.payara.microprofile.openapi.resource.rule.AnnotationProcessedDocument;

/**
 * A resource to test that the @SecurityScheme annotation is handled correctly.
 */
@SecurityScheme(
    securitySchemeName = "classDeclaredOAuth",
    type = SecuritySchemeType.OAUTH2,
    description = "OAuth key.",
    flows = @OAuthFlows(
        authorizationCode = @OAuthFlow(
            tokenUrl = "/api/auth",
            scopes = {
                @OAuthScope(name = "read", description = "Permission to read."),
                @OAuthScope(name = "write", description = "Permission to write.")
            }
        )
    )
)
@Path("/security/scheme")
public class SecuritySchemeTest {

    public transient static OpenAPI document;

    @BeforeClass
    public static void createDocument() {
        document = new AnnotationProcessedDocument();
    }

    @Test
    public void testClassDeclaredOauth() {
        org.eclipse.microprofile.openapi.models.security.SecurityScheme scheme = document.getComponents()
                .getSecuritySchemes().get("classDeclaredOAuth");
        assertNotNull("The scheme wasn't found.", scheme);
        // Test the type
        assertEquals("The scheme had the wrong type.", Type.OAUTH2, scheme.getType());
        // Test the description
        assertEquals("The scheme had the wrong description.", "OAuth key.", scheme.getDescription());
        // Test the OAuth Flows
        assertNotNull("The scheme had no flows.", scheme.getFlows());
        assertNotNull("The scheme had no authorization flow.", scheme.getFlows().getAuthorizationCode());
        // Test the OAuth Flow
        assertEquals("The authorization flow had the wrong token url.", "/api/auth",
                scheme.getFlows().getAuthorizationCode().getTokenUrl());
        // Test the OAuth Scopes
        assertNotNull("The authorization flow had no scopes.", scheme.getFlows().getAuthorizationCode().getScopes());
        // Test the OAuth READ scope
        assertTrue("The authorization flow had no read scope.",
                scheme.getFlows().getAuthorizationCode().getScopes().containsKey("read"));
        assertEquals("The write scope has the wrong description.", "Permission to read.",
                scheme.getFlows().getAuthorizationCode().getScopes().get("read"));
        // Test the OAuth WRITE scope
        assertTrue("The authorization flow had no write scope.",
                scheme.getFlows().getAuthorizationCode().getScopes().containsKey("write"));
        assertEquals("The write scope has the wrong description.", "Permission to write.",
                scheme.getFlows().getAuthorizationCode().getScopes().get("write"));
    }

    @GET
    @SecurityScheme(
        securitySchemeName = "methodDeclaredApiKey",
        type = SecuritySchemeType.APIKEY,
        in = SecuritySchemeIn.QUERY,
        apiKeyName = "key",
        description = "Insecure key."
    )
    public String specifiedScheme(@QueryParam(value = "key") String authKey) {
        return null;
    }

    @Test
    public void testMethodDeclaredApiKey() {
        org.eclipse.microprofile.openapi.models.security.SecurityScheme scheme = document.getComponents()
                .getSecuritySchemes().get("methodDeclaredApiKey");
        assertNotNull("The scheme wasn't found.", scheme);
        assertEquals("The scheme had the wrong type.", Type.APIKEY, scheme.getType());
        assertEquals("The scheme had the wrong location.", In.QUERY, scheme.getIn());
        assertEquals("The scheme had the wrong apiKeyName.", "key", scheme.getName());
        assertEquals("The scheme had the wrong description.", "Insecure key.", scheme.getDescription());
    }
}