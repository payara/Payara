package org.glassfish.admin.rest.testing;

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

public class ResponseVerifier {
    private Environment env;
    private Response response;

    public ResponseVerifier(Environment env, Response response) {
        this.env = env;
        this.response = response;
    }

    protected Environment getEnvironment() {
        return this.env;
    }

    public Response getResponse() {
        return this.response;
    }

    public ResponseVerifier status(int... statuses) {
        if (statuses == null || statuses.length == 0) {
            statuses = new int[]{getDefaultStatus()};
        }
        int have = getResponse().getStatus();
        debug("Statuses want : " + Arrays.toString(statuses));
        debug("Status have : " + have);
        for (int want : statuses) {
            if (want == Common.SC_ANY || want == have) {
                debug("Received expected status code: " + want);
                return this;
            }
        }
        throw new IllegalArgumentException("Unexpected status code.  want=" + Arrays.toString(statuses) + ", have=" + have + ", body=" + getResponse().getStringBody());
    }

    protected int getDefaultStatus() {
        String method = getResponse().getMethod();
        if (ResourceInvoker.METHOD_OPTIONS.equals(method)) {
            return Common.SC_OK;
        }
        if (ResourceInvoker.METHOD_GET.equals(method)) {
            return Common.SC_OK;
        }
        if (ResourceInvoker.METHOD_POST.equals(method)) {
            return Common.SC_CREATED;
        }
        if (ResourceInvoker.METHOD_PUT.equals(method)) {
            return Common.SC_OK;
        }
        if (ResourceInvoker.METHOD_DELETE.equals(method)) {
            return Common.SC_OK;
        }
        throw new AssertionError("Unknown Method: " + method);
    }

    public ResponseVerifier locationHeader(String uriWant) throws Exception {
        StringValue stringWant = new StringValue();
        stringWant.regexp(".*" + uriWant);
        return locationHeader(stringWant);
    }

    public ResponseVerifier locationHeader(StringValue stringWant) throws Exception {
        return header("Location", stringWant);
    }

    public ResponseVerifier header(String name, StringValue want) throws Exception {
        ObjectValue objectWant = new ObjectValue();
        objectWant.ignoreExtra();
        ArrayValue valuesWant = new ArrayValue();
        valuesWant.add(want);
        objectWant.put(name, valuesWant);
        return headers(objectWant);
    }

    public ResponseVerifier headers(ObjectValue objectWant) throws Exception {
        JSONObject objectHave = new JSONObject();
        for (Entry<String, List<String>> header : getResponse().getJaxrsResponse().getStringHeaders().entrySet()) {
            String name = header.getKey();
            JSONArray values = new JSONArray();
            for (String value : header.getValue()) {
                values.put(value);
            }
            objectHave.put(name, values);
        }
        verifyData(objectWant, objectHave);
        return this;
    }

    public ResponseVerifier body(ObjectValue objectWant) throws Exception {
        verifyData(objectWant, getResponse().getJsonBody());
        return this;
    }

    public ResponseVerifier body(StringValue want) throws Exception {
        ObjectValue objectWant = new ObjectValue();
        objectWant.put("string", want);
        JSONObject objectHave = new JSONObject();
        objectHave.put("string", getResponse().getStringBody());
        verifyData(objectWant, objectHave);
        return this;
    }

    private void verifyData(ObjectValue want, JSONObject have) throws Exception {
        IndentingStringBuffer sb = new IndentingStringBuffer();
        want.print(sb);
        debug("Body want : " + sb.toString());
        debug("Body have : " + have.toString(2));
        DataVerifier.verify(getEnvironment(), want, have);
    }

    private void debug(String message) {
        getEnvironment().debug(message);
    }
}
