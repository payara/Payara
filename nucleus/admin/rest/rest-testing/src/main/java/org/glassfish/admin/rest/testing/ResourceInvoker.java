package org.glassfish.admin.rest.testing;

import org.codehaus.jettison.json.JSONObject;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.client.Entity;
import org.glassfish.jersey.client.filter.HttpBasicAuthFilter;

import java.util.HashMap;
import java.util.Map;

public abstract class ResourceInvoker {

    public static final String METHOD_OPTIONS = "options";
    public static final String METHOD_GET = "get";
    public static final String METHOD_POST = "post";
    public static final String METHOD_PUT = "put";
    public static final String METHOD_DELETE = "delete";
    private Environment env;

    protected ResourceInvoker(Environment env) {
        this.env = env;
    }

    protected Environment getEnvironment() {
        return env;
    }

    protected abstract String getContextRoot();

    protected abstract String getResourceRoot();

    protected abstract String getMediaType();
    private String url = null;

    protected String getUrl() {
        return (this.url == null) ? getBaseUrl() + "/" + getUri() : this.url;
    }

    public ResourceInvoker url(String val) {
        this.url = val;
        return this;
    }
    private String baseUrl = null;

    protected String getBaseUrl() {
        if (this.baseUrl != null) {
            return this.baseUrl;
        }
        return getProtocol() + "://" + getHost() + ":" + getPort() + "/" + getContextRoot() + "/" + getResourceRoot();
    }

    public ResourceInvoker baseUrl(String val) {
        this.baseUrl = val;
        return this;
    }
    private String protocol = null;

    protected String getProtocol() {
        return (this.protocol == null) ? getEnvironment().getProtocol() : this.protocol;
    }

    public ResourceInvoker protocol(String val) {
        this.protocol = val;
        return this;
    }
    private String host = null;

    protected String getHost() {
        return (this.host == null) ? getEnvironment().getHost() : this.host;
    }

    public ResourceInvoker host(String val) {
        this.host = val;
        return this;
    }
    private String port = null;

    protected String getPort() {
        return (this.port == null) ? getEnvironment().getPort() : this.port;
    }

    public ResourceInvoker port(String val) {
        this.port = val;
        return this;
    }
    private String username = null;

    protected String getUserName() {
        return (this.username == null) ? getEnvironment().getUserName() : this.username;
    }

    public ResourceInvoker username(String val) {
        this.username = val;
        return this;
    }
    private String password = null;

    protected String getPassword() {
        return (this.password == null) ? getEnvironment().getPassword() : this.password;
    }

    public ResourceInvoker password(String val) {
        this.password = val;
        return this;
    }
    private String uri;

    protected String getUri() {
        return uri;
    }

    public ResourceInvoker uri(String val) {
        this.uri = val;
        return this;
    }
    private Map<String, String> queryParams = new HashMap<String, String>();

    protected Map<String, String> getQueryParams() {
        return this.queryParams;
    }

    public ResourceInvoker queryParam(String name, String value) {
        this.queryParams.put(name, value);
        return this;
    }
    private JSONObject body = new JSONObject();

    private JSONObject getBody() {
        return this.body;
    }

    public ResourceInvoker body(JSONObject val) {
        this.body = val;
        return this;
    }

    public ResourceInvoker body(ObjectValue val) throws Exception {
        return body(val.toJSONObject());
    }

    public Response options() throws Exception {
        return wrapResponse(METHOD_OPTIONS, getInvocationBuilder().options());
    }

    public Response get() throws Exception {
        return wrapResponse(METHOD_GET, getInvocationBuilder().get());
    }

    public Response post() throws Exception {
        return wrapResponse(METHOD_POST, getInvocationBuilder().post(getRequestBody()));
    }

    public Response put() throws Exception {
        return wrapResponse(METHOD_PUT, getInvocationBuilder().put(getRequestBody()));
    }

    public Response delete() throws Exception {
        return wrapResponse(METHOD_DELETE, getInvocationBuilder().delete());
    }

    private Builder getInvocationBuilder() throws Exception {
        Client client = ClientBuilder.newClient();
        client.register(new HttpBasicAuthFilter(getUserName(), getPassword()));
        WebTarget target = client.target(getUrl());
        for (Map.Entry<String, String> e : getQueryParams().entrySet()) {
            target = target.queryParam(e.getKey(), e.getValue());
        }
        return target.request(getMediaType()).header("X-Include-Resource-Links", "true").header("X-Requested-By", "MyClient");
    }

    private Response wrapResponse(String method, javax.ws.rs.core.Response response) throws Exception {
        return new Response(method, response);
    }

    private Entity getRequestBody() throws Exception {
        return Entity.entity(getBody().toString(), getMediaType());
    }

    private void print(IndentingStringBuffer sb) throws Exception {
        sb.println("ResourceInvoker");
        sb.indent();
        try {
            sb.println("url " + getUrl());

            sb.println("query parmas");
            sb.indent();
            try {
                for (Map.Entry<String, String> e : getQueryParams().entrySet()) {
                    sb.println(e.getKey() + "=" + e.getValue());
                }
            } finally {
                sb.undent();
            }

            sb.println("body ");
            sb.indent();
            try {
                String body = getBody().toString(Indenter.INDENT);
                for (String line : body.split("\n")) {
                    sb.println(line);
                }
            } finally {
                sb.undent();
            }
        } finally {
            sb.undent();
        }
    }

    private void debug(String message) {
        getEnvironment().debug(message);
    }
}
