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
import fish.payara.microprofile.openapi.impl.model.parameters.ParameterImpl;
import fish.payara.microprofile.openapi.impl.model.parameters.RequestBodyImpl;
import fish.payara.microprofile.openapi.impl.model.responses.APIResponseImpl;
import fish.payara.microprofile.openapi.impl.model.responses.APIResponsesImpl;
import fish.payara.microprofile.openapi.impl.model.security.SecurityRequirementImpl;
import fish.payara.microprofile.openapi.impl.model.servers.ServerImpl;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.mergeProperty;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.extractAnnotations;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.microprofile.openapi.models.ExternalDocumentation;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.callbacks.Callback;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;
import org.eclipse.microprofile.openapi.models.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.responses.APIResponses;
import org.eclipse.microprofile.openapi.models.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.models.servers.Server;
import org.glassfish.hk2.classmodel.reflect.AnnotationModel;

public class OperationImpl extends ExtensibleImpl<Operation> implements Operation {

    protected List<String> tags = new ArrayList<>();
    protected String summary;
    protected String description;
    protected ExternalDocumentation externalDocs;
    protected String operationId;
    protected List<Parameter> parameters = new ArrayList<>();
    protected RequestBody requestBody;
    protected APIResponses responses = new APIResponsesImpl();
    protected Map<String, Callback> callbacks = new HashMap<>();
    protected Boolean deprecated;
    protected List<SecurityRequirement> security = new ArrayList<>();
    protected List<Server> servers = new ArrayList<>();
    protected String method;

    public static Operation createInstance(AnnotationModel annotation, ApiContext context) {
        OperationImpl from = new OperationImpl();
        from.setSummary(annotation.getValue("summary", String.class));
        from.setDescription(annotation.getValue("description", String.class));
        AnnotationModel externalDocs = annotation.getValue("externalDocs", AnnotationModel.class);
        if (externalDocs != null) {
            from.setExternalDocs(ExternalDocumentationImpl.createInstance(externalDocs));
        }
        from.setOperationId(annotation.getValue("operationId", String.class));
        extractAnnotations(annotation, context, "parameters", ParameterImpl::createInstance, from.getParameters());
        AnnotationModel requestBody = annotation.getValue("requestBody", AnnotationModel.class);
        if (requestBody != null) {
            from.setRequestBody(RequestBodyImpl.createInstance(requestBody, context));
        }
        extractAnnotations(annotation, context, "responses", "responseCode", APIResponseImpl::createInstance, from.getResponses());
        extractAnnotations(annotation, context, "callbacks", "name", CallbackImpl::createInstance, from.getCallbacks());
        from.setDeprecated(annotation.getValue("deprecated", Boolean.class));
        extractAnnotations(annotation, context, "security", SecurityRequirementImpl::createInstance, from.getSecurity());
        extractAnnotations(annotation, context, "servers", ServerImpl::createInstance, from.getServers());
        from.setMethod(annotation.getValue("method", String.class));
        return from;
    }

    @Override
    public List<String> getTags() {
        return tags;
    }

    @Override
    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    @Override
    public Operation addTag(String tag) {
        tags.add(tag);
        return this;
    }

    @Override
    public void removeTag(String tag) {
        tags.remove(tag);
    }

    @Override
    public String getSummary() {
        return summary;
    }

    @Override
    public void setSummary(String summary) {
        this.summary = summary;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setDescription(String description) {
        this.description = description;
    }

    @Override
    public ExternalDocumentation getExternalDocs() {
        return externalDocs;
    }

    @Override
    public void setExternalDocs(ExternalDocumentation externalDocs) {
        this.externalDocs = externalDocs;
    }

    @Override
    public String getOperationId() {
        return operationId;
    }

    @Override
    public void setOperationId(String operationId) {
        this.operationId = operationId;
    }

    @Override
    public List<Parameter> getParameters() {
        return parameters;
    }

    @Override
    public void setParameters(List<Parameter> parameters) {
        this.parameters = parameters;
    }

    @Override
    public Operation addParameter(Parameter parameter) {
        if (parameter != null) {
            parameters.add(parameter);
        }
        return this;
    }

    @Override
    public void removeParameter(Parameter parameter) {
        parameters.remove(parameter);
    }

    @Override
    public RequestBody getRequestBody() {
        return requestBody;
    }

    @Override
    public void setRequestBody(RequestBody requestBody) {
        this.requestBody = requestBody;
    }

    @Override
    public APIResponses getResponses() {
        return responses;
    }

    @Override
    public void setResponses(APIResponses responses) {
        this.responses = responses;
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
    public Operation addCallback(String key, Callback callback) {
        if (callback != null) {
            this.callbacks.put(key, callback);
        }
        return this;
    }

    @Override
    public void removeCallback(String key) {
        this.callbacks.remove(key);
    }

    @Override
    public Boolean getDeprecated() {
        return deprecated;
    }

    @Override
    public void setDeprecated(Boolean deprecated) {
        this.deprecated = deprecated;
    }

    @Override
    public Operation deprecated(Boolean deprecated) {
        setDeprecated(deprecated);
        return this;
    }

    @Override
    public List<SecurityRequirement> getSecurity() {
        return security;
    }

    @Override
    public void setSecurity(List<SecurityRequirement> security) {
        this.security = security;
    }

    @Override
    public Operation addSecurityRequirement(SecurityRequirement securityReq) {
        security.add(securityReq);
        return this;
    }

    @Override
    public void removeSecurityRequirement(SecurityRequirement securityRequirement) {
        security.remove(securityRequirement);
    }

    @Override
    public List<Server> getServers() {
        return servers;
    }

    @Override
    public void setServers(List<Server> servers) {
        this.servers = servers;
    }

    @Override
    public Operation addServer(Server server) {
        servers.add(server);
        return this;
    }

    @Override
    public void removeServer(Server server) {
        servers.remove(server);
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public static void merge(Operation from, Operation to,
            boolean override) {
        if (from == null) {
            return;
        }
        to.setOperationId(mergeProperty(to.getOperationId(), from.getOperationId(), override));
        to.setSummary(mergeProperty(to.getSummary(), from.getSummary(), override));
        to.setDescription(mergeProperty(to.getDescription(), from.getDescription(), override));
        to.setDeprecated(mergeProperty(to.getDeprecated(), from.getDeprecated(), override));
    }

    public static void merge(Operation from, Operation to,
            boolean override, ApiContext context) {
        if (from == null) {
            return;
        }
        to.setSummary(mergeProperty(to.getSummary(), from.getSummary(), override));
        to.setDescription(mergeProperty(to.getDescription(), from.getDescription(), override));
        if (from.getExtensions() != null) {
            ExtensibleImpl.merge(from, to, override);
        }
        if (from.getExternalDocs() != null) {
            if (to.getExternalDocs() == null) {
                to.setExternalDocs(new ExternalDocumentationImpl());
            }
            ExternalDocumentationImpl.merge(from.getExternalDocs(), to.getExternalDocs(), override);
        }
        if (from.getParameters() != null) {
            for (Parameter parameter : from.getParameters()) {
                Parameter newParameter = new ParameterImpl();
                ParameterImpl.merge(parameter, newParameter, override, context);
            }
        }
        if (from.getRequestBody() != null) {
            if (to.getRequestBody() == null) {
                to.setRequestBody(new RequestBodyImpl());
            }
            RequestBodyImpl.merge(from.getRequestBody(), to.getRequestBody(), override, context);
        }
        if (from.getResponses() != null) {
            for (APIResponse response : from.getResponses().values()) {
                APIResponsesImpl.merge(response, to.getResponses(), override, context);
            }
        }
    }

}
