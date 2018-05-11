package fish.payara.microprofile.openapi.test.app.annotation;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.microprofile.openapi.annotations.Components;
import org.eclipse.microprofile.openapi.annotations.ExternalDocumentation;
import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition;
import org.eclipse.microprofile.openapi.annotations.callbacks.Callback;
import org.eclipse.microprofile.openapi.annotations.callbacks.CallbackOperation;
import org.eclipse.microprofile.openapi.annotations.enums.ParameterIn;
import org.eclipse.microprofile.openapi.annotations.enums.SchemaType;
import org.eclipse.microprofile.openapi.annotations.enums.SecuritySchemeType;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.headers.Header;
import org.eclipse.microprofile.openapi.annotations.info.Contact;
import org.eclipse.microprofile.openapi.annotations.info.Info;
import org.eclipse.microprofile.openapi.annotations.info.License;
import org.eclipse.microprofile.openapi.annotations.links.Link;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.OAuthFlow;
import org.eclipse.microprofile.openapi.annotations.security.OAuthFlows;
import org.eclipse.microprofile.openapi.annotations.security.OAuthScope;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.security.SecurityScheme;
import org.eclipse.microprofile.openapi.annotations.servers.Server;
import org.eclipse.microprofile.openapi.annotations.servers.ServerVariable;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.examples.Example;
import org.eclipse.microprofile.openapi.models.parameters.Parameter.In;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme.Type;
import org.junit.BeforeClass;
import org.junit.Test;

import fish.payara.microprofile.openapi.resource.rule.AnnotationProcessedDocument;
import fish.payara.microprofile.openapi.resource.util.TestUtils;

@OpenAPIDefinition(
    info = @Info(
        title = "Test Application",
        version = "1.2.3",
        description = "Application to test OpenAPI implementation.",
        termsOfService = "OpenAPI terms of service.",
        contact = @Contact(
            name = "test-person",
            email = "openapi-test@payara.fish",
            url = "http://payara.fish"
        ),
        license = @License(
            name = "test-license",
            url = "http://payara.fish/openapi-test-license"
        )
    ),
    servers = {
        @Server(
            url = "http://server1",
            description = "the first server.",
            variables = {
                @ServerVariable(
                    name = "serverVariable1",
                    description = "the first server variable for server1.",
                    defaultValue = "null"
                )
            }
        )
    },
    externalDocs = @ExternalDocumentation(
        description = "the first external docs.",
        url = "http://external-docs"
    ),
    security = {
        @SecurityRequirement(
            name = "securityRequirement1",
            scopes = {"scope1", "scope2"}
        ),
        @SecurityRequirement(
            name = "securityRequirement2",
            scopes = {"lion", "tiger"}
        )
    },
    tags = {
        @Tag(
            name = "tag1",
            description = "the first tag.",
            externalDocs = @ExternalDocumentation(
                description = "the first external docs for tag1.",
                url = "http://external-docs/tag1"
            )
        ),
        @Tag(
            name = "tag2",
            description = "the second tag.",
            externalDocs = @ExternalDocumentation(
                description = "the first external docs for tag2.",
                url = "http://external-docs/tag2"
            )
        )
    },
    components = @Components(
        schemas = {
            @Schema(
                name = "schema1",
                title = "the first schema.",
                description = "An integer that is divisible by 2.3.",
                deprecated = false,
                type = SchemaType.INTEGER,
                multipleOf = 2.3,
                defaultValue = "23"
            )
        },
        callbacks = {
            @Callback(
                name = "callback1",
                callbackUrlExpression = "http://callback1.org",
                operations = {
                    @CallbackOperation(
                        description = "callback1 operation1",
                        method = "OPTIONS",
                        summary = "The first operation of callback1.",
                        extensions = {
                            @Extension(name = "extension1", value = "extension2")
                        }
                    )
                }
            )
        },
        examples = {
            @ExampleObject(
                name = "exampleObject1",
                summary = "The first example object.",
                description = "longer description of the same thing",
                value = "test content",
                externalValue = "http://test-content"
            )
        },
        headers = {
            @Header(
                name = "header1",
                description = "the first header.",
                required = false
            )
        },
        links = {
            @Link(
                name = "link1",
                description = "the first link.",
                requestBody = "request body content."
            )
        },
        parameters = {
            @Parameter(
                name = "parameter1",
                description = "the first parameter.",
                in = ParameterIn.PATH
            )
        },
        requestBodies = {
            @RequestBody(
                name = "requestBody1",
                description = "the first request body.",
                content = {
                    @Content(
                        mediaType = "app/test",
                        schema = @Schema(
                            ref = "schema1"
                        )
                    )
                }
            )
        },
        responses = {
            @APIResponse(
                name = "response1",
                description = "the first response.",
                responseCode = "200",
                content = {
                    @Content(
                        mediaType = "app/test2",
                        schema = @Schema(
                            ref = "schema1"
                        )
                    )
                }
            )
        },
        securitySchemes = {
            @SecurityScheme(
                securitySchemeName = "securityScheme1",
                description = "the first security scheme.",
                scheme = "BASIC",
                type = SecuritySchemeType.HTTP,
                flows = @OAuthFlows(
                    password = @OAuthFlow(
                        authorizationUrl = "http://auth",
                        scopes = {
                            @OAuthScope(
                                name = "oauthScope1",
                                description = "the first OAuth scope."
                            )
                        }
                    )
                )
            )
        }
    )
)
public class OpenAPIDefinitionTest {

    public transient static OpenAPI document;

    @BeforeClass
    public static void createDocument() {
        document = new AnnotationProcessedDocument();
    }


    @Test
    public void infoTest() {
        org.eclipse.microprofile.openapi.models.info.Info info = document.getInfo();
        assertNotNull("The document has no info.", info);
        assertEquals("The Info object has the wrong title.", "Test Application", info.getTitle());
        assertEquals("The Info object has the wrong version.", "1.2.3", info.getVersion());
        assertEquals("The Info object has the wrong description.", "Application to test OpenAPI implementation.",
                info.getDescription());
        assertEquals("The Info object has the wrong terms of service.", "OpenAPI terms of service.",
                info.getTermsOfService());
    }

    @Test
    public void contactTest() {
        org.eclipse.microprofile.openapi.models.info.Contact contact = document.getInfo().getContact();
        assertNotNull("The document has no contacts.", contact);
        assertEquals("The Contacts object has the wrong name.", "test-person", contact.getName());
        assertEquals("The Contacts object has the wrong email.", "openapi-test@payara.fish", contact.getEmail());
        assertEquals("The Contacts object has the wrong url.", "http://payara.fish", contact.getUrl());
    }

    @Test
    public void licenseTest() {
        org.eclipse.microprofile.openapi.models.info.License license = document.getInfo().getLicense();
        assertNotNull("The document has no license.", license);
        assertEquals("The document has the wrong license name.", "test-license", license.getName());
        assertEquals("The document has the wrong license url.", "http://payara.fish/openapi-test-license",
                license.getUrl());
    }

    @Test
    public void serversTest() {
        org.eclipse.microprofile.openapi.models.servers.Server server = TestUtils.testServer(document, null,
                null, "http://server1", "the first server.");
        TestUtils.testServerContainsVariable(server, "serverVariable1", "the first server variable for server1.",
                "null", null);
    }

    @Test
    public void externalDocsTest() {
        org.eclipse.microprofile.openapi.models.ExternalDocumentation externalDocs = document
                .getExternalDocs();
        assertNotNull("The document has no external docs.", externalDocs);
        assertEquals("The external docs has the wrong description.", "the first external docs.",
                externalDocs.getDescription());
        assertEquals("The external docs has the wrong url.", "http://external-docs", externalDocs.getUrl());
    }

    @Test
    public void securityTest() {
        List<org.eclipse.microprofile.openapi.models.security.SecurityRequirement> requirements = document
                .getSecurity();
        assertNotNull("The document has no security requirements.", requirements);

        Optional<org.eclipse.microprofile.openapi.models.security.SecurityRequirement> optional = requirements.stream()
                .filter(r -> r.containsKey("securityRequirement1")).findAny();
        assertTrue("securityRequirement1 wasn't found.", optional.isPresent());
        org.eclipse.microprofile.openapi.models.security.SecurityRequirement requirement = optional.get();
        assertTrue("securityRequirement1 didn't contain scope1.",
                requirement.get("securityRequirement1").contains("scope1"));
        assertTrue("securityRequirement1 didn't contain scope2.",
                requirement.get("securityRequirement1").contains("scope2"));

        optional = requirements.stream().filter(r -> r.containsKey("securityRequirement2")).findAny();
        assertTrue("securityRequirement2 wasn't found.", optional.isPresent());
        requirement = optional.get();
        assertTrue("securityRequirement2 didn't contain scope1.",
                requirement.get("securityRequirement2").contains("lion"));
        assertTrue("securityRequirement2 didn't contain scope2.",
                requirement.get("securityRequirement2").contains("tiger"));
    }
    @Test
    public void tagsTest() {
        // Test that the base tags were created
        TestUtils.testTag(document, null, null, "tag1", "the first tag.");
        TestUtils.testTag(document, null, null, "tag2", "the second tag.");
    }

    @Test
    public void schemaTest() {
        assertNotNull("The document components were null.", document.getComponents());
        Map<String, org.eclipse.microprofile.openapi.models.media.Schema> schemas = document.getComponents()
                .getSchemas();
        assertFalse("The document contained no schemas.", schemas == null || schemas.isEmpty());
    }

    @Test
    public void componentsTest() {
        org.eclipse.microprofile.openapi.models.Components components = document.getComponents();
        assertNotNull("The document has no components.", components);

        // Test the schema components
        Map<String, org.eclipse.microprofile.openapi.models.media.Schema> schemas = components.getSchemas();
        assertTrue("schema1 wasn't found.", schemas.containsKey("schema1"));
        assertEquals("schema1 has the wrong title.", "the first schema.", schemas.get("schema1").getTitle());
        assertEquals("schema1 has the wrong deprecated value.", false, schemas.get("schema1").getDeprecated());

        // Test the callback components
        Map<String, org.eclipse.microprofile.openapi.models.callbacks.Callback> callbacks = components.getCallbacks();
        assertTrue("callback1 wasn't found.", callbacks.containsKey("callback1"));
        assertTrue("callback1 has the wrong url.", callbacks.get("callback1").containsKey("http://callback1.org"));
        Operation callbackOperation = callbacks.get("callback1").get("http://callback1.org").getOPTIONS();
        assertNotNull("callback1 operation has the wrong HTTP method.", callbackOperation);
        assertEquals("callback1 operation has the wrong description.", "callback1 operation1",
                callbackOperation.getDescription());
        assertEquals("callback1 operation has the wrong summary.", "The first operation of callback1.",
                callbackOperation.getSummary());

        // Test the example components
        Map<String, Example> examples = components.getExamples();
        assertTrue("exampleObject1 wasn't found.", examples.containsKey("exampleObject1"));
        assertEquals("exampleObject1 has the wrong summary.", "The first example object.",
                examples.get("exampleObject1").getSummary());
        assertEquals("exampleObject1 has the wrong description.", "longer description of the same thing",
                examples.get("exampleObject1").getDescription());
        assertEquals("exampleObject1 has the wrong value.", "test content",
                examples.get("exampleObject1").getValue().toString());
        assertEquals("exampleObject1 has the wrong externalValue.", "http://test-content",
                examples.get("exampleObject1").getExternalValue());

        // Test the header components
        Map<String, org.eclipse.microprofile.openapi.models.headers.Header> headers = components.getHeaders();
        assertTrue("header1 wasn't found.", headers.containsKey("header1"));
        assertEquals("header1 has the wrong description.", "the first header.",
                headers.get("header1").getDescription());
        assertEquals("header1 has the wrong required value.", false, headers.get("header1").getRequired());

        // Test the link components
        Map<String, org.eclipse.microprofile.openapi.models.links.Link> links = components.getLinks();
        assertTrue("link1 wasn't found.", links.containsKey("link1"));
        assertEquals("link1 has the wrong description.", "the first link.", links.get("link1").getDescription());
        assertEquals("link1 has the wrong request body.", "request body content.", links.get("link1").getRequestBody());

        // Test the parameter components
        Map<String, org.eclipse.microprofile.openapi.models.parameters.Parameter> parameters = components
                .getParameters();
        assertTrue("parameter1 wasn't found.", parameters.containsKey("parameter1"));
        assertEquals("parameter1 has the wrong description.", "the first parameter.",
                parameters.get("parameter1").getDescription());
        assertEquals("parameter1 has the wrong request body.", In.PATH, parameters.get("parameter1").getIn());

        // Test the request body components
        Map<String, org.eclipse.microprofile.openapi.models.parameters.RequestBody> requestBodies = components
                .getRequestBodies();
        assertTrue("requestBody1 wasn't found.", requestBodies.containsKey("requestBody1"));
        assertEquals("requestBody1 has the wrong description.", "the first request body.",
                requestBodies.get("requestBody1").getDescription());
        assertNotNull("requestBody1 has the wrong content type.",
                requestBodies.get("requestBody1").getContent().get("app/test"));
        assertNotNull("requestBody1 has the wrong schema.",
                requestBodies.get("requestBody1").getContent().get("app/test").getSchema());
        assertNotNull("requestBody1 has no schema ref.",
                requestBodies.get("requestBody1").getContent().get("app/test").getSchema().getRef());
        assertEquals("requestBody1 has the wrong schema ref.", "#/components/schemas/schema1",
                requestBodies.get("requestBody1").getContent().get("app/test").getSchema().getRef());

        // Test the response components
        Map<String, org.eclipse.microprofile.openapi.models.responses.APIResponse> responses = components
                .getResponses();
        assertTrue("response1 wasn't found.", responses.containsKey("response1"));
        assertEquals("response1 has the wrong description.", "the first response.",
                responses.get("response1").getDescription());
        assertNotNull("response1 has the wrong content type.",
                responses.get("response1").getContent().get("app/test2"));
        assertNotNull("response1 has the wrong schema.",
                responses.get("response1").getContent().get("app/test2").getSchema());
        assertNotNull("response1 has no schema ref.",
                responses.get("response1").getContent().get("app/test2").getSchema().getRef());
        assertEquals("response1 has the wrong schema ref.", "#/components/schemas/schema1",
                responses.get("response1").getContent().get("app/test2").getSchema().getRef());

        // Test the security scheme components
        Map<String, org.eclipse.microprofile.openapi.models.security.SecurityScheme> security = components
                .getSecuritySchemes();
        assertTrue("securityScheme1 wasn't found.", security.containsKey("securityScheme1"));
        assertEquals("securityScheme1 has the wrong description.", "the first security scheme.",
                security.get("securityScheme1").getDescription());
        assertEquals("securityScheme1 has the wrong scheme.", "BASIC", security.get("securityScheme1").getScheme());
        assertEquals("securityScheme1 has the wrong type.", Type.HTTP, security.get("securityScheme1").getType());
        assertNotNull("securityScheme1 has no flows.", security.get("securityScheme1").getFlows());
        assertNotNull("securityScheme1 has no flow password.",
                security.get("securityScheme1").getFlows().getPassword());
        assertEquals("securityScheme1 has the wrong auth url.", "http://auth",
                security.get("securityScheme1").getFlows().getPassword().getAuthorizationUrl());
        assertNotNull("securityScheme1 has no scope.",
                security.get("securityScheme1").getFlows().getPassword().getScopes().get("oauthScope1"));
        assertEquals("securityScheme1 has the wrong auth url.", "the first OAuth scope.",
                security.get("securityScheme1").getFlows().getPassword().getScopes().get("oauthScope1"));
    }
}