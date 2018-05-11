package fish.payara.microprofile.openapi.impl.model;

import java.util.Map;
import java.util.TreeMap;

import org.eclipse.microprofile.openapi.annotations.media.ExampleObject;
import org.eclipse.microprofile.openapi.models.Components;
import org.eclipse.microprofile.openapi.models.callbacks.Callback;
import org.eclipse.microprofile.openapi.models.examples.Example;
import org.eclipse.microprofile.openapi.models.headers.Header;
import org.eclipse.microprofile.openapi.models.links.Link;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.security.SecurityScheme;

import fish.payara.microprofile.openapi.impl.model.callbacks.CallbackImpl;
import fish.payara.microprofile.openapi.impl.model.examples.ExampleImpl;
import fish.payara.microprofile.openapi.impl.model.headers.HeaderImpl;
import fish.payara.microprofile.openapi.impl.model.links.LinkImpl;
import fish.payara.microprofile.openapi.impl.model.media.SchemaImpl;
import fish.payara.microprofile.openapi.impl.model.parameters.ParameterImpl;
import fish.payara.microprofile.openapi.impl.model.parameters.RequestBodyImpl;
import fish.payara.microprofile.openapi.impl.model.responses.APIResponseImpl;
import fish.payara.microprofile.openapi.impl.model.security.SecuritySchemeImpl;

public class ComponentsImpl extends ExtensibleImpl implements Components {

    protected Map<String, Schema> schemas = new TreeMap<>();
    protected Map<String, APIResponse> responses = new TreeMap<>();
    protected Map<String, Parameter> parameters = new TreeMap<>();
    protected Map<String, Example> examples = new TreeMap<>();
    protected Map<String, RequestBody> requestBodies = new TreeMap<>();
    protected Map<String, Header> headers = new TreeMap<>();
    protected Map<String, SecurityScheme> securitySchemes = new TreeMap<>();
    protected Map<String, Link> links = new TreeMap<>();
    protected Map<String, Callback> callbacks = new TreeMap<>();

    @Override
    public Map<String, Schema> getSchemas() {
        return schemas;
    }

    @Override
    public void setSchemas(Map<String, Schema> schemas) {
        this.schemas = schemas;
    }

    @Override
    public Components schemas(Map<String, Schema> schemas) {
        setSchemas(schemas);
        return this;
    }

    @Override
    public Components addSchema(String key, Schema schema) {
        schemas.put(key, schema);
        return this;
    }

    @Override
    public Map<String, APIResponse> getResponses() {
        return responses;
    }

    @Override
    public void setResponses(Map<String, APIResponse> responses) {
        this.responses = responses;
    }

    @Override
    public Components responses(Map<String, APIResponse> responses) {
        setResponses(responses);
        return this;
    }

    @Override
    public Components addResponse(String key, APIResponse response) {
        responses.put(key, response);
        return this;
    }

    @Override
    public Map<String, Parameter> getParameters() {
        return parameters;
    }

    @Override
    public void setParameters(Map<String, Parameter> parameters) {
        this.parameters = parameters;
    }

    @Override
    public Components parameters(Map<String, Parameter> parameters) {
        setParameters(parameters);
        return this;
    }

    @Override
    public Components addParameter(String key, Parameter parameter) {
        parameters.put(key, parameter);
        return this;
    }

    @Override
    public Map<String, Example> getExamples() {
        return examples;
    }

    @Override
    public void setExamples(Map<String, Example> examples) {
        this.examples = examples;
    }

    @Override
    public Components examples(Map<String, Example> examples) {
        setExamples(examples);
        return this;
    }

    @Override
    public Components addExample(String key, Example example) {
        examples.put(key, example);
        return this;
    }

    @Override
    public Map<String, RequestBody> getRequestBodies() {
        return requestBodies;
    }

    @Override
    public void setRequestBodies(Map<String, RequestBody> requestBodies) {
        this.requestBodies = requestBodies;
    }

    @Override
    public Components requestBodies(Map<String, RequestBody> requestBodies) {
        setRequestBodies(requestBodies);
        return this;
    }

    @Override
    public Components addRequestBody(String key, RequestBody requestBody) {
        requestBodies.put(key, requestBody);
        return this;
    }

    @Override
    public Map<String, Header> getHeaders() {
        return headers;
    }

    @Override
    public void setHeaders(Map<String, Header> headers) {
        this.headers = headers;
    }

    @Override
    public Components headers(Map<String, Header> headers) {
        setHeaders(headers);
        return this;
    }

    @Override
    public Components addHeader(String key, Header header) {
        headers.put(key, header);
        return this;
    }

    @Override
    public Map<String, SecurityScheme> getSecuritySchemes() {
        return securitySchemes;
    }

    @Override
    public void setSecuritySchemes(Map<String, SecurityScheme> securitySchemes) {
        this.securitySchemes = securitySchemes;
    }

    @Override
    public Components securitySchemes(Map<String, SecurityScheme> securitySchemes) {
        setSecuritySchemes(securitySchemes);
        return this;
    }

    @Override
    public Components addSecurityScheme(String key, SecurityScheme securityScheme) {
        securitySchemes.put(key, securityScheme);
        return this;
    }

    @Override
    public Map<String, Link> getLinks() {
        return links;
    }

    @Override
    public void setLinks(Map<String, Link> links) {
        this.links = links;
    }

    @Override
    public Components links(Map<String, Link> links) {
        setLinks(links);
        return this;
    }

    @Override
    public Components addLink(String key, Link link) {
        links.put(key, link);
        return this;
    }

    @Override
    public Map<String, Callback> getCallbacks() {
        return callbacks;
    }

    @Override
    public void setCallbacks(Map<String, Callback> callbacks) {
        this.callbacks = callbacks;
    }

    @Override
    public Components callbacks(Map<String, Callback> callbacks) {
        setCallbacks(callbacks);
        return this;
    }

    @Override
    public Components addCallback(String key, Callback callback) {
        callbacks.put(key, callback);
        return this;
    }

    public static void merge(org.eclipse.microprofile.openapi.annotations.Components from, Components to,
            boolean override, Map<String, Schema> currentSchemas) {
        if (from == null) {
            return;
        }
        // Handle @Schema
        if (from.schemas() != null) {
            for (org.eclipse.microprofile.openapi.annotations.media.Schema schema : from.schemas()) {
                if (schema.name() != null) {
                    Schema newSchema = new SchemaImpl();
                    SchemaImpl.merge(schema, newSchema, override, currentSchemas);
                    to.addSchema(schema.name(), newSchema);
                }
            }
        }
        // Handle @Callback
        if (from.callbacks() != null) {
            for (org.eclipse.microprofile.openapi.annotations.callbacks.Callback callback : from.callbacks()) {
                if (callback != null) {
                    if (callback.name() != null) {
                        Callback newCallback = new CallbackImpl();
                        CallbackImpl.merge(callback, newCallback, override);
                        to.addCallback(callback.name(), newCallback);
                    }
                }
            }
        }
        // Handle @ExampleObject
        if (from.examples() != null) {
            for (ExampleObject example : from.examples()) {
                if (example.name() != null) {
                    Example newExample = new ExampleImpl();
                    ExampleImpl.merge(example, newExample, override);
                    to.addExample(example.name(), newExample);
                }
            }
        }
        // Handle @Header
        if (from.headers() != null) {
            for (org.eclipse.microprofile.openapi.annotations.headers.Header header : from.headers()) {
                if (header.name() != null) {
                    Header newHeader = new HeaderImpl();
                    HeaderImpl.merge(header, newHeader, override, currentSchemas);
                    to.addHeader(header.name(), newHeader);
                }
            }
        }
        // Handle @Link
        if (from.links() != null) {
            for (org.eclipse.microprofile.openapi.annotations.links.Link link : from.links()) {
                if (link.name() != null) {
                    Link newLink = new LinkImpl();
                    LinkImpl.merge(link, newLink, override);
                    to.addLink(link.name(), newLink);
                }
            }
        }
        // Handle @Parameter
        if (from.parameters() != null) {
            for (org.eclipse.microprofile.openapi.annotations.parameters.Parameter parameter : from.parameters()) {
                if (parameter.name() != null) {
                    Parameter newParameter = new ParameterImpl();
                    ParameterImpl.merge(parameter, newParameter, override, currentSchemas);
                    to.addParameter(parameter.name(), newParameter);
                }
            }
        }
        // Handle @RequestBody
        if (from.requestBodies() != null) {
            for (org.eclipse.microprofile.openapi.annotations.parameters.RequestBody requestBody : from
                    .requestBodies()) {
                if (requestBody.name() != null) {
                    RequestBody newRequestBody = new RequestBodyImpl();
                    RequestBodyImpl.merge(requestBody, newRequestBody, override, currentSchemas);
                    to.addRequestBody(requestBody.name(), newRequestBody);
                }
            }
        }
        // Handle @APIResponse
        if (from.responses() != null) {
            for (org.eclipse.microprofile.openapi.annotations.responses.APIResponse response : from.responses()) {
                if (response.name() != null) {
                    APIResponse newResponse = new APIResponseImpl();
                    APIResponseImpl.merge(response, newResponse, override, currentSchemas);
                    to.addResponse(response.name(), newResponse);
                }
            }
        }
        // Handle @SecurityScheme
        if (from.securitySchemes() != null) {
            for (org.eclipse.microprofile.openapi.annotations.security.SecurityScheme security : from
                    .securitySchemes()) {
                if (security.securitySchemeName() != null) {
                    SecurityScheme newSecurity = new SecuritySchemeImpl();
                    SecuritySchemeImpl.merge(security, newSecurity, override);
                    to.addSecurityScheme(security.securitySchemeName(), newSecurity);
                }
            }
        }
    }

}