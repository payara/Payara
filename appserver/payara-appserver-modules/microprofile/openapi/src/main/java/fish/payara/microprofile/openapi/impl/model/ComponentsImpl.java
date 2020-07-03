/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2018-2020] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package fish.payara.microprofile.openapi.impl.model;

import fish.payara.microprofile.openapi.api.visitor.ApiContext;
import fish.payara.microprofile.openapi.impl.model.callbacks.CallbackImpl;
import fish.payara.microprofile.openapi.impl.model.examples.ExampleImpl;
import fish.payara.microprofile.openapi.impl.model.headers.HeaderImpl;
import fish.payara.microprofile.openapi.impl.model.links.LinkImpl;
import fish.payara.microprofile.openapi.impl.model.media.SchemaImpl;
import fish.payara.microprofile.openapi.impl.model.parameters.ParameterImpl;
import fish.payara.microprofile.openapi.impl.model.parameters.RequestBodyImpl;
import fish.payara.microprofile.openapi.impl.model.responses.APIResponseImpl;
import fish.payara.microprofile.openapi.impl.model.security.SecuritySchemeImpl;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.extractAnnotations;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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
import org.glassfish.hk2.classmodel.reflect.AnnotationModel;

public class ComponentsImpl extends ExtensibleImpl<Components> implements Components {

    protected Map<String, Schema> schemas = new TreeMap<>();
    protected Map<String, APIResponse> responses = new TreeMap<>();
    protected Map<String, Parameter> parameters = new TreeMap<>();
    protected Map<String, Example> examples = new TreeMap<>();
    protected Map<String, RequestBody> requestBodies = new TreeMap<>();
    protected Map<String, Header> headers = new TreeMap<>();
    protected Map<String, SecurityScheme> securitySchemes = new TreeMap<>();
    protected Map<String, Link> links = new TreeMap<>();
    protected Map<String, Callback> callbacks = new TreeMap<>();

    public static Components createInstance(AnnotationModel annotation, ApiContext context) {
        Components from = new ComponentsImpl();
        extractAnnotations(annotation, context, "schemas", "name", SchemaImpl::createInstance, from.getSchemas());
        extractAnnotations(annotation, context, "responses", "name", APIResponseImpl::createInstance, from.getResponses());
        extractAnnotations(annotation, context, "parameters", "name", ParameterImpl::createInstance, from.getParameters());
        extractAnnotations(annotation, context, "examples", "name", ExampleImpl::createInstance, from.getExamples());
        extractAnnotations(annotation, context, "requestBodies", "name", RequestBodyImpl::createInstance, from.getRequestBodies());
        extractAnnotations(annotation, context, "securitySchemes", "securitySchemeName", SecuritySchemeImpl::createInstance, from.getSecuritySchemes());
        extractAnnotations(annotation, context, "links", "name", LinkImpl::createInstance, from.getLinks());
        extractAnnotations(annotation, context, "callbacks", "name", CallbackImpl::createInstance, from.getCallbacks());
        from.getHeaders().putAll(HeaderImpl.createInstances(annotation, context));
        return from;
    }

    @Override
    public Map<String, Schema> getSchemas() {
        return schemas;
    }

    @Override
    public void setSchemas(Map<String, Schema> schemas) {
        this.schemas = schemas;
    }

    @Override
    public Components addSchema(String key, Schema schema) {
        if (schema != null) {
            schemas.put(key, schema);
        }
        return this;
    }

    @Override
    public void removeSchema(String key) {
        schemas.remove(key);
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
    public Components addResponse(String key, APIResponse response) {
        if (response != null) {
            responses.put(key, response);
        }
        return this;
    }

    @Override
    public void removeResponse(String key) {
        responses.remove(key);
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
    public Components addParameter(String key, Parameter parameter) {
        if (parameter != null) {
            parameters.put(key, parameter);
        }
        return this;
    }

    @Override
    public void removeParameter(String key) {
        parameters.remove(key);
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
    public Components addExample(String key, Example example) {
        if (example != null) {
            examples.put(key, example);
        }
        return this;
    }

    @Override
    public void removeExample(String key) {
        examples.remove(key);
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
    public Components addRequestBody(String key, RequestBody requestBody) {
        if (requestBody != null) {
            requestBodies.put(key, requestBody);
        }
        return this;
    }

    @Override
    public void removeRequestBody(String key) {
        requestBodies.remove(key);
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
    public Components addHeader(String key, Header header) {
        if (header != null) {
            headers.put(key, header);
        }
        return this;
    }

    @Override
    public void removeHeader(String key) {
        headers.remove(key);
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
    public Components addSecurityScheme(String key, SecurityScheme securityScheme) {
        if (securityScheme != null) {
            securitySchemes.put(key, securityScheme);
        }
        return this;
    }

    @Override
    public void removeSecurityScheme(String key) {
        securitySchemes.remove(key);
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
    public Components addLink(String key, Link link) {
        if (link != null) {
            links.put(key, link);
        }
        return this;
    }

    @Override
    public void removeLink(String key) {
        links.remove(key);
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
    public Components addCallback(String key, Callback callback) {
        if (callback != null) {
            callbacks.put(key, callback);
        }
        return this;
    }

    @Override
    public void removeCallback(String key) {
        callbacks.remove(key);
    }

    public static void merge(Components from, Components to,
            boolean override, ApiContext context) {
        if (from == null) {
            return;
        }
        // Handle @Schema
        if (from.getSchemas()!= null) {
            for (String schemaName : from.getSchemas().keySet()) {
                if (schemaName != null) {
                    Schema newSchema = new SchemaImpl();
                    SchemaImpl.merge(from.getSchemas().get(schemaName), newSchema, override, context);
                    to.addSchema(schemaName, newSchema);
                }
            }
        }
        // Handle @Callback
        if (from.getCallbacks()!= null) {
            for (String callbackName : from.getCallbacks().keySet()) {
                if (callbackName != null) {
                    Callback newCallback = new CallbackImpl();
                    CallbackImpl.merge(from.getCallbacks().get(callbackName), newCallback, override, context);
                    to.addCallback(callbackName, newCallback);
                }
            }
        }
        // Handle @ExampleObject
        if (from.getExamples() != null) {
            for (String exampleName : from.getExamples().keySet()) {
                if (exampleName != null) {
                    Example newExample = new ExampleImpl();
                    ExampleImpl.merge(from.getExamples().get(exampleName), newExample, override);
                    to.addExample(exampleName, newExample);
                }
            }
        }
        // Handle @Header
        if (from.getHeaders()!= null) {
            for (String headerName : from.getHeaders().keySet()) {
                if (headerName != null) {
                    Header newHeader = new HeaderImpl();
                    HeaderImpl.merge(from.getHeaders().get(headerName), newHeader, override, context);
                    to.addHeader(headerName, newHeader);
                }
            }
        }
        // Handle @Link
        if (from.getLinks()!= null) {
            for (String linkName : from.getLinks().keySet()) {
                if (linkName != null) {
                    Link newLink = new LinkImpl();
                    LinkImpl.merge(from.getLinks().get(linkName), newLink, override);
                    to.addLink(linkName, newLink);
                }
            }
        }
        // Handle @Parameter
        if (from.getParameters() != null) {
            for (String parameterName : from.getParameters().keySet()) {
                if (parameterName != null) {
                    Parameter newParameter = new ParameterImpl();
                    ParameterImpl.merge(from.getParameters().get(parameterName), newParameter, override, context);
                    to.addParameter(parameterName, newParameter);
                }
            }
        }
        // Handle @RequestBody
        if (from.getRequestBodies()!= null) {
            for (String requestBodyName : from.getRequestBodies().keySet()) {
                if (requestBodyName != null) {
                    RequestBody newRequestBody = new RequestBodyImpl();
                    RequestBodyImpl.merge(from.getRequestBodies().get(requestBodyName), newRequestBody, override, context);
                    to.addRequestBody(requestBodyName, newRequestBody);
                }
            }
        }
        // Handle @APIResponse
        if (from.getResponses()!= null) {
            for (String responseName : from.getResponses().keySet()) {
                if (responseName != null) {
                    APIResponse newResponse = new APIResponseImpl();
                    APIResponseImpl.merge(from.getResponses().get(responseName), newResponse, override, context);
                    to.addResponse(responseName, newResponse);
                }
            }
        }
        // Handle @SecurityScheme
        if (from.getSecuritySchemes()!= null) {
            for (String securitySchemeName : from.getSecuritySchemes().keySet()) {
                if (securitySchemeName != null) {
                    SecurityScheme newSecurity = new SecuritySchemeImpl();
                    SecuritySchemeImpl.merge(from.getSecuritySchemes().get(securitySchemeName), newSecurity, override);
                    to.addSecurityScheme(securitySchemeName, newSecurity);
                }
            }
        }
    }

}