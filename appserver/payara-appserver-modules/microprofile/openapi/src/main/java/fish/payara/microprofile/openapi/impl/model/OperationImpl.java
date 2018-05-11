package fish.payara.microprofile.openapi.impl.model;

import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.isAnnotationNull;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.mergeProperty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.openapi.models.ExternalDocumentation;
import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.callbacks.Callback;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.parameters.RequestBody;
import org.eclipse.microprofile.openapi.models.responses.APIResponses;
import org.eclipse.microprofile.openapi.models.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.models.servers.Server;

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

}
