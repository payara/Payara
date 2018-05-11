package fish.payara.microprofile.openapi.impl.model.links;

import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.applyReference;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.isAnnotationNull;
import static fish.payara.microprofile.openapi.impl.model.util.ModelUtils.mergeProperty;

import java.util.HashMap;
import java.util.Map;

import org.eclipse.microprofile.openapi.models.links.Link;
import org.eclipse.microprofile.openapi.models.servers.Server;

import fish.payara.microprofile.openapi.impl.model.ExtensibleImpl;

public class LinkImpl extends ExtensibleImpl implements Link {

    protected String operationRef;
    protected String operationId;
    protected Map<String, Object> parameters = new HashMap<>();
    protected Object requestBody;
    protected String description;
    protected String ref;
    protected Server server;

    @Override
    public Server getServer() {
        return server;
    }

    @Override
    public void setServer(Server server) {
        this.server = server;
    }

    @Override
    public Link server(Server server) {
        setServer(server);
        return this;
    }

    @Override
    public String getOperationRef() {
        return operationRef;
    }

    @Override
    public void setOperationRef(String operationRef) {
        this.operationRef = operationRef;
    }

    @Override
    public Link operationRef(String operationRef) {
        setOperationRef(operationRef);
        return this;
    }

    @Override
    public Object getRequestBody() {
        return requestBody;
    }

    @Override
    public void setRequestBody(Object requestBody) {
        this.requestBody = requestBody;
    }

    @Override
    public Link requestBody(Object requestBody) {
        setRequestBody(requestBody);
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
    public Link operationId(String operationId) {
        setOperationId(operationId);
        return this;
    }

    @Override
    public Map<String, Object> getParameters() {
        return parameters;
    }

    @Override
    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    @Override
    public Link parameters(Map<String, Object> parameters) {
        setParameters(parameters);
        return this;
    }

    @Override
    public Link addParameter(String name, Object parameter) {
        this.parameters.put(name, parameter);
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
    public Link description(String description) {
        setDescription(description);
        return this;
    }

    @Override
    public String getRef() {
        return ref;
    }

    @Override
    public void setRef(String ref) {
        if (ref != null && !ref.contains(".") && !ref.contains("/")) {
            ref = "#/components/links/" + ref;
        }
        this.ref = ref;
    }

    @Override
    public Link ref(String ref) {
        setRef(ref);
        return this;
    }

    public static void merge(org.eclipse.microprofile.openapi.annotations.links.Link from, Link to, boolean override) {
        if (isAnnotationNull(from)) {
            return;
        }
        if (from.ref() != null && !from.ref().isEmpty()) {
            applyReference(to, from.ref());
            return;
        }
        to.setDescription(mergeProperty(to.getDescription(), from.description(), override));
        to.setOperationId(mergeProperty(to.getOperationId(), from.operationId(), override));
        to.setOperationRef(mergeProperty(to.getOperationRef(), from.operationRef(), override));
        to.setRequestBody(mergeProperty(to.getRequestBody(), from.requestBody(), override));
    }

}
