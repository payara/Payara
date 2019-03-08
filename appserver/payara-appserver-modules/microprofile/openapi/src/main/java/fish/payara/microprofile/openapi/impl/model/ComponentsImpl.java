/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2018] Payara Foundation and/or its affiliates. All rights reserved.
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
        schemas.put(key, schema);
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
        responses.put(key, response);
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
        parameters.put(key, parameter);
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
        examples.put(key, example);
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
        requestBodies.put(key, requestBody);
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
        headers.put(key, header);
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
        securitySchemes.put(key, securityScheme);
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
        links.put(key, link);
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
        callbacks.put(key, callback);
        return this;
    }

    @Override
    public void removeCallback(String key) {
        callbacks.remove(key);
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
                if (callback != null && callback.name() != null) {
                    Callback newCallback = new CallbackImpl();
                    CallbackImpl.merge(callback, newCallback, override, currentSchemas);
                    to.addCallback(callback.name(), newCallback);
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