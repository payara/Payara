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
 *
 * Portions Copyright [2017] Payara Foundation and/or affiliates
 */

package org.glassfish.admin.rest.testing;

import java.io.StringReader;
import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;
import javax.json.stream.JsonParser;
import static org.glassfish.admin.rest.testing.Common.*;

public class Response {
    private String method;
    private javax.ws.rs.core.Response jaxrsResponse;
    private String bodyAsString;

    public Response(String method, javax.ws.rs.core.Response jaxrsResponse) {
        this(method, jaxrsResponse, true);
    }

    public Response(String method, javax.ws.rs.core.Response jaxrsResponse, boolean readEntity) {
        this.method = method;
        this.jaxrsResponse = jaxrsResponse;
        if (readEntity) {
            // get the response body now in case the caller releases the connection before asking for the response body
            try {
                this.bodyAsString = this.jaxrsResponse.readEntity(String.class);
            } catch (Exception e) {
            }
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

    public JsonObject getJsonBody() throws Exception {
        try (JsonParser parser = Json.createParser(new StringReader(getStringBody()))){
            if (parser.hasNext()){
                parser.next();
                return parser.getObject();
            } else {
                return JsonValue.EMPTY_JSON_OBJECT;
            }
        }
    }

    public JsonObject getItem() throws Exception {
        return getJsonBody().getJsonObject(PROP_ITEM);
    }

    public String getLocationHeader() throws Exception {
        return getJaxrsResponse().getHeaderString(HEADER_LOCATION);
    }

    public String getXLocationHeader() throws Exception {
        return getJaxrsResponse().getHeaderString(HEADER_X_LOCATION);
    }
}
