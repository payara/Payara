/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2014 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
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

package org.glassfish.admin.rest.testing;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation.Builder;
import javax.ws.rs.client.WebTarget;

import org.glassfish.jersey.client.authentication.HttpAuthenticationFeature;

import org.codehaus.jettison.json.JSONObject;

public abstract class ResourceInvoker {

    public static final String METHOD_OPTIONS = "options";
    public static final String METHOD_GET = "get";
    public static final String METHOD_POST = "post";
    public static final String METHOD_PUT = "put";
    public static final String METHOD_DELETE = "delete";
    private final Environment env;
    private String password = null;
    private String uri;
    private Map<String, String> queryParams = new HashMap<String, String>();
    private String url = null;
    private String baseUrl = null;
    private String protocol = null;
    private String host = null;
    private String port = null;
    private String username = null;
    private JSONObject body = new JSONObject();

    protected ResourceInvoker(Environment env) {
        this.env = env;
    }

    protected Environment getEnvironment() {
        return env;
    }

    protected abstract String getContextRoot();

    protected abstract String getResourceRoot();

    protected abstract String getMediaType();

    protected String getRequestBodyMediaType() {
        return getMediaType();
    }

    protected String getResponseBodyMediaType() {
        return getMediaType();
    }

    protected String getUrl() {
        return (this.url == null) ? getBaseUrl() + "/" + getUri() : this.url;
    }

    public ResourceInvoker url(String val) {
        this.url = val;
        return this;
    }

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

    protected String getProtocol() {
        return (this.protocol == null) ? getEnvironment().getProtocol() : this.protocol;
    }

    public ResourceInvoker protocol(String val) {
        this.protocol = val;
        return this;
    }

    protected String getHost() {
        return (this.host == null) ? getEnvironment().getHost() : this.host;
    }

    public ResourceInvoker host(String val) {
        this.host = val;
        return this;
    }

    protected String getPort() {
        return (this.port == null) ? getEnvironment().getPort() : this.port;
    }

    public ResourceInvoker port(String val) {
        this.port = val;
        return this;
    }

    protected String getUserName() {
        return (this.username == null) ? getEnvironment().getUserName() : this.username;
    }

    public ResourceInvoker username(String val) {
        this.username = val;
        return this;
    }

    protected String getPassword() {
        return (this.password == null) ? getEnvironment().getPassword() : this.password;
    }

    public ResourceInvoker password(String val) {
        this.password = val;
        return this;
    }

    protected String getUri() {
        return uri;
    }

    public ResourceInvoker uri(String val) {
        this.uri = val;
        return this;
    }

    protected Map<String, String> getQueryParams() {
        return this.queryParams;
    }

    public ResourceInvoker queryParam(String name, String value) {
        this.queryParams.put(name, value);
        return this;
    }

    protected JSONObject getBody() {
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
        Client client = customizeClient(ClientBuilder.newClient());
        client.register(HttpAuthenticationFeature.basic(getUserName(), getPassword()));
        WebTarget target = client.target(getUrl());
        for (Map.Entry<String, String> e : getQueryParams().entrySet()) {
            target = target.queryParam(e.getKey(), e.getValue());
        }
        return target.request(getResponseBodyMediaType()).header("X-Include-Resource-Links", "true").header("X-Requested-By", "MyClient");
    }

    protected Client customizeClient(Client client) {
        return client;
    }

    protected Response wrapResponse(String method, javax.ws.rs.core.Response response) throws Exception {
        return new Response(method, response);
    }

    private Entity getRequestBody() throws Exception {
        return Entity.entity(getBody().toString(), getRequestBodyMediaType());
    }
}
