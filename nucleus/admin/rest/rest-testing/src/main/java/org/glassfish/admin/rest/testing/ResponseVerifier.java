/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;
import java.util.List;
import java.util.Map.Entry;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

import static org.glassfish.admin.rest.testing.Common.*;

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
        return header(HEADER_LOCATION, stringWant);
    }

    public ResponseVerifier xLocationHeader(String uriWant) throws Exception {
        StringValue stringWant = new StringValue();
        stringWant.regexp(".*" + uriWant);
        return xLocationHeader(stringWant);
    }

    public ResponseVerifier xLocationHeader(StringValue stringWant) throws Exception {
        return header(HEADER_X_LOCATION, stringWant);
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
        DataVerifier.verify(getEnvironment(), want, have);
    }

    private void debug(String message) {
        getEnvironment().debug(message);
    }
}
