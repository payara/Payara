package fish.payara.microprofile.openapi.resource.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import org.eclipse.microprofile.openapi.models.OpenAPI;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.PathItem.HttpMethod;
import org.eclipse.microprofile.openapi.models.callbacks.Callback;
import org.eclipse.microprofile.openapi.models.media.Content;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.media.Schema.SchemaType;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.parameters.Parameter.In;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;
import org.eclipse.microprofile.openapi.models.responses.APIResponses;
import org.eclipse.microprofile.openapi.models.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.models.servers.Server;
import org.eclipse.microprofile.openapi.models.servers.ServerVariable;
import org.eclipse.microprofile.openapi.models.tags.Tag;

public final class TestUtils {

    private TestUtils() {
    }

    /**
     * Tests that a given operation exists in the document.
     * 
     * @param document     the OpenAPI document to scan for the operation.
     * @param endpointPath the path of the operation.
     * @param httpMethod   the name of the method mapped to the operation. If this
     *                     value is null, it will not be checked.
     * @param method       the HTTP Method of the operation.
     * 
     * @throws AssertionError if the operation isn't found.
     */
    public static void testOperation(OpenAPI document, String endpointPath, String httpMethod, HttpMethod method) {
        assertNotNull(endpointPath + " doesn't exist.", document.getPaths().get(endpointPath));
        assertNotNull(endpointPath + " has no operations.", document.getPaths().get(endpointPath).readOperationsMap());
        assertNotNull(endpointPath + " has no " + method.toString() + ".",
                document.getPaths().get(endpointPath).readOperationsMap().get(method));
        if (httpMethod != null) {
            assertEquals(endpointPath + " has the wrong method name.",
                    document.getPaths().get(endpointPath).readOperationsMap().get(method).getOperationId(), httpMethod);
        }
    }

    /**
     * Tests that a named parameter with the given type exists for an endpoint in
     * the document.
     * 
     * @param document     the OpenAPI document to scan for the operation.
     * @param endpointPath the path of the operation.
     * @param httpMethod   the name of the method mapped to the operation. If this
     *                     value is null, it will not be checked.
     * @param parameterMap A map mapping the name of a parameter to search for to
     *                     the parameter type.
     * 
     * @throws AssertionError if the parameter or operation aren't found.
     */
    public static void testParameter(OpenAPI document, String endpointPath, HttpMethod httpMethod,
            Map<String, In> parameterMap) {
        testOperation(document, endpointPath, null, httpMethod);
        Operation operation = document.getPaths().get(endpointPath).readOperationsMap().get(httpMethod);

        // If the parameter map is null, check there are no parameters
        if (parameterMap == null) {
            assertTrue(endpointPath + " has parameters.",
                    operation.getParameters() == null || operation.getParameters().isEmpty());
            return;
        }

        List<Parameter> parameters = operation.getParameters();
        for (Entry<String, In> entry : parameterMap.entrySet()) {
            String parameterName = entry.getKey();
            In parameterType = entry.getValue();

            assertTrue(endpointPath + " has no parameter with name " + parameterName,
                    parameters.stream().anyMatch(param -> param.getName().equals(parameterName)));
            assertEquals(
                    endpointPath + " parameter " + parameterName + " is the wrong type.", parameters.stream()
                            .filter(param -> param.getName().equals(parameterName)).findFirst().get().getIn(),
                    parameterType);
        }
    }

    /**
     * Tests that a named content type with the given schema type exists in the
     * request body of an endpoint in the document.
     * 
     * @param document     the OpenAPI document to scan for the operation.
     * @param endpointPath the path of the operation.
     * @param httpMethod   the name of the method mapped to the operation. If this
     *                     value is null, it will not be checked.
     * @param requestMap   A map mapping the content type to search for to the
     *                     schema type or reference. A value of type
     *                     {@link SchemaType} will attempt to match a content type.
     *                     A value of type {@link String} will attempt to match a
     *                     reference instead. If this map is null, the operation
     *                     will be tested for no request body.
     * 
     * @return the request body found.
     * 
     * @throws AssertionError if the content type or operation aren't found.
     */
    public static RequestBody testRequestBody(OpenAPI document, String endpointPath, HttpMethod httpMethod,
            Map<String, Object> requestMap) {
        testOperation(document, endpointPath, null, httpMethod);
        Operation operation = document.getPaths().get(endpointPath).readOperationsMap().get(httpMethod);
        if (requestMap == null) {
            assertNull(endpointPath + " has a requestBody.", operation.getRequestBody());
            return null;
        }
        assertNotNull(endpointPath + " has no requestBody.", operation.getRequestBody());
        assertNotNull(endpointPath + " has no content.", operation.getRequestBody().getContent());

        Content content = operation.getRequestBody().getContent();
        try {
            testContent(content, requestMap);
        } catch (AssertionError ex) {
            fail(endpointPath + " -> " + ex.getMessage());
        }
        return operation.getRequestBody();
    }

    /**
     * Tests that a named content type with the given schema type exists in a named
     * response of an endpoint in the document.
     * 
     * @param document     the OpenAPI document to scan for the operation.
     * @param endpointPath the path of the operation.
     * @param httpMethod   the name of the method mapped to the operation. If this
     *                     value is null, it will not be checked.
     * @param responseCode the name of the response.
     * @param responseMap  A map mapping the content type to search for to the
     *                     schema type or reference. A value of type
     *                     {@link SchemaType} will attempt to match a content type.
     *                     A value of type {@link String} will attempt to match a
     *                     reference instead. If this map is null, the operation
     *                     will be tested for no response with the given name.
     * 
     * @return the found response.
     * 
     * @throws AssertionError if the content type or operation aren't found.
     */
    public static org.eclipse.microprofile.openapi.models.responses.APIResponse testResponse(OpenAPI document,
            String endpointPath, HttpMethod httpMethod, String responseCode, Map<String, Object> responseMap) {
        assertNotNull(endpointPath + " doesn't exist.", document.getPaths().get(endpointPath));
        Operation operation = document.getPaths().get(endpointPath).readOperationsMap().get(httpMethod);
        assertNotNull("Operation not found.", operation);
        assertNotNull("No responses found.", operation.getResponses());
        assertNotNull("Response not found.", operation.getResponses().get(responseCode));

        Content content = operation.getResponses().get(responseCode).getContent();
        try {
            testContent(content, responseMap);
        } catch (AssertionError ex) {
            fail(endpointPath + " -> " + ex.getMessage());
        }
        return operation.getResponses().get(responseCode);
    }

    /**
     * Tests that a named content type with the given schema type exists in the
     * default response of an endpoint in the document.
     * 
     * @param document     the OpenAPI document to scan for the operation.
     * @param endpointPath the path of the operation.
     * @param httpMethod   the name of the method mapped to the operation. If this
     *                     value is null, it will not be checked.
     * @param responseMap  A map mapping the content type to search for to the
     *                     schema type or reference. A value of type
     *                     {@link SchemaType} will attempt to match a content type.
     *                     A value of type {@link String} will attempt to match a
     *                     reference instead.
     * 
     * @return the found response.
     * 
     * @throws AssertionError if the content type or operation aren't found.
     */
    public static org.eclipse.microprofile.openapi.models.responses.APIResponse testResponse(OpenAPI document,
            String endpointPath, HttpMethod httpMethod, Map<String, Object> responseMap) {
        return testResponse(document, endpointPath, httpMethod, APIResponses.DEFAULT, responseMap);
    }

    private static void testContent(Content content, Map<String, Object> typeMap) {
        assertNotNull("The found content was null.", content);
        for (Entry<String, Object> entry : typeMap.entrySet()) {
            String contentName = entry.getKey();
            assertTrue("No content type found with name " + contentName, content.keySet().contains(contentName));
            if (entry.getValue() instanceof SchemaType) {
                SchemaType schemaType = (SchemaType) entry.getValue();
                assertNull("The found schema should not contain a reference.",
                        content.get(contentName).getSchema().getRef());
                assertEquals("The found schema had the wrong type.", content.get(contentName).getSchema().getType(),
                        schemaType);
            } else if (entry.getValue() instanceof String) {
                String refName = (String) entry.getValue();
                assertEquals("The found schema had the wrong reference.", refName,
                        content.get(contentName).getSchema().getRef());
                assertNull("The found schema should not contain a type when it has a reference.",
                        content.get(contentName).getSchema().getType());
            }
        }
    }

    /**
     * Tests that a component with the specified name exists in the document. Will
     * also test that the found component contains a property with the given name
     * and type.
     * 
     * @param document      the OpenAPI document to search in.
     * @param componentName the name of the component to search for.
     * @param propertyName  the name of the property to search for.
     * @param propertyType  the type of the propetry to search for. If more than one
     *                      type is specified, the sub item type will be checked
     *                      recursively. For example, the types "array, array, int",
     *                      will check for a multidimensional array of ints.
     * 
     * @throws AssertionError if the component or property aren't found.
     */
    public static void testComponentProperty(OpenAPI document, String componentName, String propertyName,
            SchemaType... propertyTypes) {
        // Check the property exists
        assertNotNull("The component property " + propertyName + " wasn't found.",
                document.getComponents().getSchemas().get(componentName).getProperties().get(propertyName));
        // Check the property and each sub property has the correct type
        Schema schema = document.getComponents().getSchemas().get(componentName).getProperties().get(propertyName);
        for (SchemaType propertyType : propertyTypes) {
            assertNotNull("The schema had no sub items.", schema);
            // Check the property has the correct type
            assertEquals("The component property " + propertyName + " wasn't the correct type.", schema.getType(),
                    propertyType);
            try {
                schema = schema.getItems();
            } catch (NullPointerException ex) {
                // Ignore
            }
        }
    }

    /**
     * Tests that a server with the given values exists at the given operation. If
     * <code>url</code> is null, then the server is searched for at the document
     * root.
     * 
     * @param document          the OpenAPI document to search in.
     * @param url               the url of the path item to test.
     * @param method            the {@link HttpMethod} of the operation.
     * @param serverUrl         the url of the server to expect.
     * @param serverDescription the expected description of the found server.
     * 
     * @return the found server.
     */
    public static Server testServer(OpenAPI document, String url, HttpMethod method, String serverUrl,
            String serverDescription) {
        List<Server> servers = null;
        if (url == null) {
            servers = document.getServers();
        } else {
            PathItem pathItem = document.getPaths().get(url);
            assertNotNull("No Path item found for url: " + url, pathItem);
            Operation operation = pathItem.readOperationsMap().get(method);
            assertNotNull("No operation found for method: " + method + " at url: " + url, pathItem);
            servers = operation.getServers();
        }
        assertFalse("There were no servers found for element", servers == null || servers.isEmpty());
        Optional<Server> optional = servers.stream().filter(s -> s.getUrl().equals(serverUrl)).findAny();
        assertTrue("No server found with url: " + serverUrl, optional.isPresent());
        Server server = optional.get();
        assertEquals("Server with url: " + server.getUrl() + " has the wrong description.", server.getDescription(),
                serverDescription);
        return server;
    }

    /**
     * Tests that a server with the given values doesn't exist at the given
     * operation. If <code>url</code> is null, then the server is searched for at
     * the document root.
     * 
     * @param document  the OpenAPI document to search in.
     * @param url       the url of the path item to test.
     * @param method    the {@link HttpMethod} of the operation.
     * @param serverUrl the url of the server to expect.
     * 
     * @throws AssertionError if the server isn't found.
     */
    public static void testNotServer(OpenAPI document, String url, HttpMethod method, String serverUrl) {
        List<Server> servers = null;
        if (url == null) {
            servers = document.getServers();
        } else {
            PathItem pathItem = document.getPaths().get(url);
            assertNotNull("No Path item found for url: " + url, pathItem);
            Operation operation = pathItem.readOperationsMap().get(method);
            assertNotNull("No operation found for method: " + method + " at url: " + url, pathItem);
            servers = operation.getServers();
        }
        if (servers == null || servers.isEmpty()) {
            return;
        }
        Optional<Server> optional = document.getServers().stream().filter(s -> s.getUrl().equals(serverUrl)).findAny();
        assertFalse("No server found with url: " + url, optional.isPresent());
    }

    /**
     * Tests that a server with the given url contains a variable with the given
     * values.
     * 
     * @param server              the server to test.
     * @param variableName        the name of the variable.
     * @param variableDescription the description of the variable.
     * @param defaultValue        the default value of the variable.
     * @param enumValue           the enum value of the variable.
     * 
     * @throws AssertionError if the server variable isn't found.
     */
    public static void testServerContainsVariable(Server server, String variableName, String variableDescription,
            String defaultValue, String enumValue) {
        assertNotNull(server.getUrl() + " has no variables.", server.getVariables());
        ServerVariable variable = server.getVariables().get(variableName);
        assertNotNull(variableName + " has no variable called: " + variableName, variable);
        assertEquals(variableName + " has the wrong description.", variable.getDescription(), variableDescription);
        assertEquals(variableName + " has the wrong default value.", variable.getDefaultValue(), defaultValue);
        if (enumValue == null) {
            assertTrue(variableName + " contains enum values.",
                    variable.getEnumeration() == null || variable.getEnumeration().isEmpty());
        } else {
            assertFalse(variableName + " contains no enum values.",
                    variable.getEnumeration() == null || variable.getEnumeration().isEmpty());
        }
    }

    /**
     * Tests that a tag with the given values exists at the given operation. If
     * <code>url</code> is null, then the tag is searched for at the document root.
     * 
     * @param document       the OpenAPI document to search in.
     * @param url            the url of the path item to test.
     * @param method         the {@link HttpMethod} of the operation.
     * @param tagName        the name of the tag to expect.
     * @param tagDescription the expected description of the found tag.
     * 
     * @throws AssertionError if the tag isn't found.
     */
    public static void testTag(OpenAPI document, String url, HttpMethod method, String tagName, String tagDescription) {
        List<Tag> tags = document.getTags();
        if (url != null) {
            PathItem pathItem = document.getPaths().get(url);
            assertNotNull("No Path item found for url: " + url, pathItem);
            Operation operation = pathItem.readOperationsMap().get(method);
            assertNotNull("No operation found for method: " + method + " at url: " + url, pathItem);
            List<String> operationTags = operation.getTags();
            assertNotNull("The operation: " + method + " at url: " + url + " contains no tags.", operationTags);
            assertTrue("There were no tags with the name: " + tagName, operationTags.contains(tagName));
        }
        assertFalse("There were no tags found.", tags == null || tags.isEmpty());
        Optional<Tag> optional = tags.stream().filter(t -> t.getName().equals(tagName)).findAny();
        assertTrue("There were no tags found with name: " + tagName, optional.isPresent());
        Tag tag = optional.get();
        assertEquals("The tag " + tagName + " had the wrong description.", tag.getDescription(), tagDescription);
    }

    /**
     * Tests that a tag with the given values does not exist at the given operation.
     * If <code>url</code> is null, then the tag is searched for at the document
     * root.
     * 
     * @param document the OpenAPI document to search in.
     * @param url      the url of the path item to test.
     * @param method   the {@link HttpMethod} of the operation.
     * @param tagName  the name of the tag to expect.
     * 
     * @throws AssertionError if the tag is found.
     */
    public static void testNotTag(OpenAPI document, String url, HttpMethod method, String tagName) {
        List<Tag> tags = document.getTags();
        if (url != null) {
            PathItem pathItem = document.getPaths().get(url);
            assertNotNull("No Path item found for url: " + url, pathItem);
            Operation operation = pathItem.readOperationsMap().get(method);
            assertNotNull("No operation found for method: " + method + " at url: " + url, pathItem);
            List<String> operationTags = operation.getTags();
            assertTrue("The operation: " + method + " at url: " + url + " contains no tags.",
                    operationTags == null || !operationTags.contains(tagName));
        } else {
            boolean found = tags.stream().anyMatch(t -> t.getName().equals(tagName));
            assertFalse("There was a tag found with name: " + tagName, found);
        }
    }

    /**
     * Tests that a security configuration with the specified name exists at the
     * given operation.
     * 
     * @param document   the OpenAPI document to search in.
     * @param url        the url of the path item to test.
     * @param httpMethod the {@link HttpMethod} of the operation.
     * @param schemeName the name of the expected scheme.
     * @param scope      the name of the scope to expect in the found scheme.
     */
    public static void testSecurityRequirement(OpenAPI document, String url, HttpMethod httpMethod, String schemeName, String scope) {
        testOperation(document, url, null, httpMethod);
        Operation operation = document.getPaths().get(url).readOperationsMap().get(httpMethod);
        List<SecurityRequirement> operationRequirements = operation.getSecurity();
        assertNotNull("No security requirements found at operation.", operationRequirements);
        for (SecurityRequirement requirement : operationRequirements) {
            if (requirement.containsKey(schemeName)) {
                assertTrue("Scope not found.", requirement.get(schemeName).contains(scope));
                return;
            }
        }
        fail("Security requirement not found.");
    }

    /**
     * Tests that a security configuration with the specified name doesn't exist at
     * the given operation.
     * 
     * @param document   the OpenAPI document to search in.
     * @param url        the url of the path item to test.
     * @param httpMethod the {@link HttpMethod} of the operation.
     * @param schemeName the name of the scheme to test for.
     */
    public static void testNotSecurityRequirement(OpenAPI document, String url, HttpMethod httpMethod, String schemeName) {
        testOperation(document, url, null, httpMethod);
        Operation operation = document.getPaths().get(url).readOperationsMap().get(httpMethod);
        List<SecurityRequirement> operationRequirements = operation.getSecurity();
        for (SecurityRequirement requirement : operationRequirements) {
            if (requirement.containsKey(schemeName)) {
                fail("Security requirement found.");
                return;
            }
        }
    }

    /**
     * Tests that a named callback operation with the given values exists for an
     * endpoint in the document.
     * 
     * @param document            the OpenAPI document to scan for the operation.
     * @param endpointPath        the path of the operation.
     * @param httpMethod          the name of the method mapped to the operation. If
     *                            this value is null, it will not be checked.
     * @param callbackName        the name of the callback to test for.
     * @param callbackUrl         the url of the callback to test for.
     * @param callbackOperation   the method of the callback operation to test for.
     * @param callbackDescription the description of the callback operation, or null
     *                            to not test.
     * 
     * @throws AssertionError if the callback or callback operation aren't found.
     */
    public static void testCallback(OpenAPI document, String endpointPath, HttpMethod httpMethod, String callbackName,
            String callbackUrl, HttpMethod callbackOperation, String callbackDescription) {
        testOperation(document, endpointPath, null, httpMethod);
        Operation operation = document.getPaths().get(endpointPath).readOperationsMap().get(httpMethod);
        assertNotNull("No callbacks found.", operation.getCallbacks());
        Callback callback = operation.getCallbacks().get(callbackName);
        assertNotNull("Callback " + callbackName + " not found.", callback);
        PathItem callbackPath = callback.get(callbackUrl);
        assertNotNull("Callback url expression " + callbackUrl + " not found.", callbackPath);
        operation = callbackPath.readOperationsMap().get(callbackOperation);
        assertNotNull("Callback operation " + callbackOperation + " not found.", operation);
        if (callbackDescription != null) {
            assertEquals("Wrong callback operation description.", callbackDescription, operation.getDescription());
        }
    }

}