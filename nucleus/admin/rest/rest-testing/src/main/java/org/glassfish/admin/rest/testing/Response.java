package org.glassfish.admin.rest.testing;

import org.codehaus.jettison.json.JSONObject;

public class Response {
    private String method;
    private javax.ws.rs.core.Response jaxrsResponse;
    private String bodyAsString;

    public Response(String method, javax.ws.rs.core.Response jaxrsResponse) {
        this.method = method;
        this.jaxrsResponse = jaxrsResponse;
        // get the response body now in case the caller releases the connection before asking for the response body
        try {
            this.bodyAsString = this.jaxrsResponse.readEntity(String.class);
        } catch (Exception e) {
        }
    }

    public javax.ws.rs.core.Response getJaxrsResponse() {
        return this.jaxrsResponse;
    }

    public String getMethod() {
        return this.method;
    }

    public int getStatus() {
        return getJaxrsResponse().getStatus();
    }

    public String getStringBody() {
        return this.bodyAsString;
    }

    public JSONObject getJsonBody() throws Exception {
        return new JSONObject(getStringBody());
    }
}
