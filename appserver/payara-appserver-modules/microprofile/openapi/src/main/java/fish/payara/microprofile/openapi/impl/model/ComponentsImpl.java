/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2018-2023] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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

import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.createOrderedMap;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.extractAnnotations;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.mergeProperty;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.readOnlyView;

import java.util.Map;
import java.util.Map.Entry;

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

    protected Map<String, Schema> schemas = createOrderedMap();
    protected Map<String, APIResponse> responses = createOrderedMap();
    protected Map<String, Parameter> parameters = createOrderedMap();
    protected Map<String, Example> examples = createOrderedMap();
    protected Map<String, RequestBody> requestBodies = createOrderedMap();
    protected Map<String, Header> headers = createOrderedMap();
    protected Map<String, SecurityScheme> securitySchemes = createOrderedMap();
    protected Map<String, Link> links = createOrderedMap();
    protected Map<String, Callback> callbacks = createOrderedMap();

    public static Components createInstance(AnnotationModel annotation, ApiContext context) {
        Components from = new ComponentsImpl();
        from.setExtensions(parseExtensions(annotation));
        extractAnnotations(annotation, context, "schemas", "name", SchemaImpl::createInstance, from::addSchema);
        extractAnnotations(annotation, context, "responses", "name", APIResponseImpl::createInstance, from::addResponse);
        extractAnnotations(annotation, context, "parameters", "name", ParameterImpl::createInstance, from::addParameter);
        extractAnnotations(annotation, context, "examples", "name", ExampleImpl::createInstance, from::addExample);
        extractAnnotations(annotation, context, "requestBodies", "name", RequestBodyImpl::createInstance, from::addRequestBody);
        extractAnnotations(annotation, context, "securitySchemes", "securitySchemeName", SecuritySchemeImpl::createInstance, from::addSecurityScheme);
        extractAnnotations(annotation, context, "links", "name", LinkImpl::createInstance, from::addLink);
        extractAnnotations(annotation, context, "callbacks", "name", CallbackImpl::createInstance, from::addCallback);
        HeaderImpl.createInstances(annotation, context).forEach(from::addHeader);
        return from;
    }

    @Override
    public Map<String, Schema> getSchemas() {
        return readOnlyView(schemas);
    }

    @Override
    public void setSchemas(Map<String, Schema> schemas) {
        this.schemas = createOrderedMap(schemas);
    }

    @Override
    public Components addSchema(String key, Schema schema) {
        if (schema != null) {
            if (schemas == null) {
                schemas = createOrderedMap();
            }
            schemas.put(key, schema);
        }
        return this;
    }

    @Override
    public void removeSchema(String key) {
        if (schemas != null) {
            schemas.remove(key);
        }
    }

    @Override
    public Map<String, APIResponse> getResponses() {
        return readOnlyView(responses);
    }

    @Override
    public void setResponses(Map<String, APIResponse> responses) {
        this.responses = createOrderedMap(responses);
    }

    @Override
    public Components addResponse(String key, APIResponse response) {
        if (response != null) {
            if (responses == null) {
                responses = createOrderedMap();
            }
            responses.put(key, response);
        }
        return this;
    }

    @Override
    public void removeResponse(String key) {
        if (responses != null) {
            responses.remove(key);
        }
    }

    @Override
    public Map<String, Parameter> getParameters() {
        return readOnlyView(parameters);
    }

    @Override
    public void setParameters(Map<String, Parameter> parameters) {
        this.parameters = createOrderedMap(parameters);
    }

    @Override
    public Components addParameter(String key, Parameter parameter) {
        if (parameter != null) {
            if (parameters == null) {
                parameters = createOrderedMap();
            }
            parameters.put(key, parameter);
        }
        return this;
    }

    @Override
    public void removeParameter(String key) {
        if (parameters != null) {
            parameters.remove(key);
        }
    }

    @Override
    public Map<String, Example> getExamples() {
        return readOnlyView(examples);
    }

    @Override
    public void setExamples(Map<String, Example> examples) {
        this.examples = createOrderedMap(examples);
    }

    @Override
    public Components addExample(String key, Example example) {
        if (example != null) {
            if (examples == null) {
                examples = createOrderedMap();
            }
            examples.put(key, example);
        }
        return this;
    }

    @Override
    public void removeExample(String key) {
        if (examples != null) {
            examples.remove(key);
        }
    }

    @Override
    public Map<String, RequestBody> getRequestBodies() {
        return readOnlyView(requestBodies);
    }

    @Override
    public void setRequestBodies(Map<String, RequestBody> requestBodies) {
        this.requestBodies = createOrderedMap(requestBodies);
    }

    @Override
    public Components addRequestBody(String key, RequestBody requestBody) {
        if (requestBody != null) {
            if (requestBodies == null) {
                requestBodies = createOrderedMap();
            }
            requestBodies.put(key, requestBody);
        }
        return this;
    }

    @Override
    public void removeRequestBody(String key) {
        if (requestBodies != null) {
            requestBodies.remove(key);
        }
    }

    @Override
    public Map<String, Header> getHeaders() {
        return readOnlyView(headers);
    }

    @Override
    public void setHeaders(Map<String, Header> headers) {
        this.headers = createOrderedMap(headers);
    }

    @Override
    public Components addHeader(String key, Header header) {
        if (header != null) {
            if (headers == null) {
                headers = createOrderedMap();
            }
            headers.put(key, header);
        }
        return this;
    }

    @Override
    public void removeHeader(String key) {
        if (headers != null) {
            headers.remove(key);
        }
    }

    @Override
    public Map<String, SecurityScheme> getSecuritySchemes() {
        return readOnlyView(securitySchemes);
    }

    @Override
    public void setSecuritySchemes(Map<String, SecurityScheme> securitySchemes) {
        this.securitySchemes = createOrderedMap(securitySchemes);
    }

    @Override
    public Components addSecurityScheme(String key, SecurityScheme securityScheme) {
        if (securityScheme != null) {
            if (securitySchemes == null) {
                securitySchemes = createOrderedMap();
            }
            securitySchemes.put(key, securityScheme);
        }
        return this;
    }

    @Override
    public void removeSecurityScheme(String key) {
        if (securitySchemes != null) {
            securitySchemes.remove(key);
        }
    }

    @Override
    public Map<String, Link> getLinks() {
        return readOnlyView(links);
    }

    @Override
    public void setLinks(Map<String, Link> links) {
        this.links = createOrderedMap(links);
    }

    @Override
    public Components addLink(String key, Link link) {
        if (link != null) {
            if (links == null) {
                links = createOrderedMap();
            }
            links.put(key, link);
        }
        return this;
    }

    @Override
    public void removeLink(String key) {
        if (links != null) {
            links.remove(key);
        }
    }

    @Override
    public Map<String, Callback> getCallbacks() {
        return readOnlyView(callbacks);
    }

    @Override
    public void setCallbacks(Map<String, Callback> callbacks) {
        this.callbacks = createOrderedMap(callbacks);
    }

    @Override
    public Components addCallback(String key, Callback callback) {
        if (callback != null) {
            if (callbacks == null) {
                callbacks = createOrderedMap();
            }
            callbacks.put(key, callback);
        }
        return this;
    }

    @Override
    public void removeCallback(String key) {
        if (callbacks != null) {
            callbacks.remove(key);
        }
    }

    public static void merge(Components from, Components to,
            boolean override, ApiContext context) {
        if (from == null) {
            return;
        }
        to.setExtensions(mergeProperty(to.getExtensions(), from.getExtensions(), override));
        // Handle @Schema
        if (from.getSchemas()!= null) {
            for (Entry<String, Schema> fromEntry : from.getSchemas().entrySet()) {
                final String schemaName = fromEntry.getKey();
                if (schemaName != null) {
                    final Schema fromSchema = fromEntry.getValue();
                    final Schema toSchema = to.getSchemas().getOrDefault(schemaName, new SchemaImpl());
                    SchemaImpl.merge(fromSchema, toSchema, override, context);
                    to.addSchema(schemaName, toSchema);
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