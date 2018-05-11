package fish.payara.microprofile.openapi.impl.model;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.eclipse.microprofile.openapi.models.Operation;
import org.eclipse.microprofile.openapi.models.PathItem;
import org.eclipse.microprofile.openapi.models.parameters.Parameter;
import org.eclipse.microprofile.openapi.models.servers.Server;

public class PathItemImpl extends ExtensibleImpl implements PathItem {

    protected String ref;
    protected String summary;
    protected String description;
    protected Operation get;
    protected Operation put;
    protected Operation post;
    protected Operation delete;
    protected Operation options;
    protected Operation head;
    protected Operation patch;
    protected Operation trace;
    protected List<Server> servers = new ArrayList<>();
    protected List<Parameter> parameters = new ArrayList<>();

    @Override
    public String getRef() {
        return ref;
    }

    @Override
    public void setRef(String ref) {
        this.ref = ref;
    }

    @Override
    public PathItem ref(String ref) {
        setRef(ref);
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
    public PathItem summary(String summary) {
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
    public PathItem description(String description) {
        setDescription(description);
        return this;
    }

    @Override
    public Operation getGET() {
        return get;
    }

    @Override
    public void setGET(Operation get) {
        this.get = get;
    }

    @Override
    public PathItem GET(Operation get) {
        setGET(get);
        return this;
    }

    @Override
    public Operation getPUT() {
        return put;
    }

    @Override
    public void setPUT(Operation put) {
        this.put = put;
    }

    @Override
    public PathItem PUT(Operation put) {
        setPUT(put);
        return this;
    }

    @Override
    public Operation getPOST() {
        return post;
    }

    @Override
    public void setPOST(Operation post) {
        this.post = post;
    }

    @Override
    public PathItem POST(Operation post) {
        setPOST(post);
        return this;
    }

    @Override
    public Operation getDELETE() {
        return delete;
    }

    @Override
    public void setDELETE(Operation delete) {
        this.delete = delete;
    }

    @Override
    public PathItem DELETE(Operation delete) {
        setDELETE(delete);
        return this;
    }

    @Override
    public Operation getOPTIONS() {
        return options;
    }

    @Override
    public void setOPTIONS(Operation options) {
        this.options = options;
    }

    @Override
    public PathItem OPTIONS(Operation options) {
        setOPTIONS(options);
        return this;
    }

    @Override
    public Operation getHEAD() {
        return head;
    }

    @Override
    public void setHEAD(Operation head) {
        this.head = head;
    }

    @Override
    public PathItem HEAD(Operation head) {
        setHEAD(head);
        return this;
    }

    @Override
    public Operation getPATCH() {
        return patch;
    }

    @Override
    public void setPATCH(Operation patch) {
        this.patch = patch;
    }

    @Override
    public PathItem PATCH(Operation patch) {
        setPATCH(patch);
        return this;
    }

    @Override
    public Operation getTRACE() {
        return trace;
    }

    @Override
    public void setTRACE(Operation trace) {
        this.trace = trace;
    }

    @Override
    public PathItem TRACE(Operation trace) {
        setTRACE(trace);
        return this;
    }

    @Override
    public List<Operation> readOperations() {
        List<Operation> allOperations = new ArrayList<>();
        if (this.get != null) {
            allOperations.add(this.get);
        }
        if (this.put != null) {
            allOperations.add(this.put);
        }
        if (this.head != null) {
            allOperations.add(this.head);
        }
        if (this.post != null) {
            allOperations.add(this.post);
        }
        if (this.delete != null) {
            allOperations.add(this.delete);
        }
        if (this.patch != null) {
            allOperations.add(this.patch);
        }
        if (this.options != null) {
            allOperations.add(this.options);
        }
        if (this.trace != null) {
            allOperations.add(this.trace);
        }

        return allOperations;
    }

    @Override
    public Map<HttpMethod, Operation> readOperationsMap() {
        Map<HttpMethod, Operation> result = new EnumMap<>(HttpMethod.class);

        if (this.get != null) {
            result.put(HttpMethod.GET, this.get);
        }
        if (this.put != null) {
            result.put(HttpMethod.PUT, this.put);
        }
        if (this.post != null) {
            result.put(HttpMethod.POST, this.post);
        }
        if (this.delete != null) {
            result.put(HttpMethod.DELETE, this.delete);
        }
        if (this.patch != null) {
            result.put(HttpMethod.PATCH, this.patch);
        }
        if (this.head != null) {
            result.put(HttpMethod.HEAD, this.head);
        }
        if (this.options != null) {
            result.put(HttpMethod.OPTIONS, this.options);
        }
        if (this.trace != null) {
            result.put(HttpMethod.TRACE, this.trace);
        }

        return result;
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
    public PathItem servers(List<Server> servers) {
        setServers(servers);
        return this;
    }

    @Override
    public PathItem addServer(Server server) {
        servers.add(server);
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
    public PathItem parameters(List<Parameter> parameters) {
        setParameters(parameters);
        return this;
    }

    @Override
    public PathItem addParameter(Parameter parameter) {
        setParameters(parameters);
        return this;
    }

}
