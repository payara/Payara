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

import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.isAnnotationNull;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.mergeProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.openapi.annotations.callbacks.CallbackOperation;
import org.eclipse.microprofile.openapi.annotations.extensions.Extension;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.models.ExternalDocumentation;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.callbacks.Callback;
import org.eclipse.microprofile.openapi.models.media.Schema;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;
import org.eclipse.microprofile.openapi.models.responses.APIResponses;
import org.eclipse.microprofile.openapi.models.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.models.servers.Server;

import fish.payara.microprofile.openapi.impl.model.parameters.ParameterImpl;
import fish.payara.microprofile.openapi.impl.model.parameters.RequestBodyImpl;
import fish.payara.microprofile.openapi.impl.model.responses.APIResponsesImpl;

public class OperationImpl extends ExtensibleImpl implements Operation {

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

    @Override
    public List<String> getTags() {
        return tags;
    }

    @Override
    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    @Override
    public Operation tags(List<String> tags) {
        setTags(tags);
        return this;
    }

    @Override
    public Operation addTag(String tag) {
        tags.add(tag);
        return this;
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
    public Operation summary(String summary) {
        setSummary(summary);
        return this;
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
    public Operation description(String description) {
        setDescription(description);
        return this;
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
    public Operation externalDocs(ExternalDocumentation externalDocs) {
        setExternalDocs(externalDocs);
        return this;
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
    public Operation operationId(String operationId) {
        setOperationId(operationId);
        return this;
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
    public Operation parameters(List<Parameter> parameters) {
        setParameters(parameters);
        return this;
    }

    @Override
    public Operation addParameter(Parameter parameter) {
        parameters.add(parameter);
        return this;
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
    public Operation requestBody(RequestBody requestBody) {
        setRequestBody(requestBody);
        return this;
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
    public Operation responses(APIResponses responses) {
        setResponses(responses);
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
    public Operation callbacks(Map<String, Callback> callbacks) {
        setCallbacks(callbacks);
        return this;
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
    public Operation security(List<SecurityRequirement> security) {
        setSecurity(security);
        return this;
    }

    @Override
    public Operation addSecurityRequirement(SecurityRequirement securityReq) {
        security.add(securityReq);
        return this;
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
    public Operation servers(List<Server> servers) {
        setServers(servers);
        return this;
    }

    @Override
    public Operation addServer(Server server) {
        servers.add(server);
        return this;
    }

    public static void merge(org.eclipse.microprofile.openapi.annotations.Operation from, Operation to,
            boolean override) {
        if (isAnnotationNull(from)) {
            return;
        }
        to.setOperationId(mergeProperty(to.getOperationId(), from.operationId(), override));
        to.setSummary(mergeProperty(to.getSummary(), from.summary(), override));
        to.setDescription(mergeProperty(to.getDescription(), from.description(), override));
    }

    public static void merge(CallbackOperation from, Operation to,
            boolean override, Map<String, Schema> currentSchemas) {
        if (isAnnotationNull(from)) {
            return;
        }
        to.setSummary(mergeProperty(to.getSummary(), from.summary(), override));
        to.setDescription(mergeProperty(to.getDescription(), from.description(), override));
        if (from.extensions() != null) {
            for (Extension extension : from.extensions()) {
                ExtensibleImpl.merge(extension, to, override);
            }
        }
        if (!isAnnotationNull(from.externalDocs())) {
            if (to.getExternalDocs() == null) {
                to.setExternalDocs(new ExternalDocumentationImpl());
            }
            ExternalDocumentationImpl.merge(from.externalDocs(), to.getExternalDocs(), override);
        }
        if (from.parameters() != null) {
            for (org.eclipse.microprofile.openapi.annotations.parameters.Parameter parameter : from.parameters()) {
                Parameter newParameter = new ParameterImpl();
                ParameterImpl.merge(parameter, newParameter, override, currentSchemas);
            }
        }
        if (!isAnnotationNull(from.requestBody())) {
            if (to.getRequestBody() == null) {
                to.setRequestBody(new RequestBodyImpl());
            }
            RequestBodyImpl.merge(from.requestBody(), to.getRequestBody(), override, currentSchemas);
        }
        if (from.responses() != null) {
            for (APIResponse response : from.responses()) {
                APIResponsesImpl.merge(response, to.getResponses(), override, currentSchemas);
            }
        }
    }

}
