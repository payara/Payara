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
package org.glassfish.admin.rest.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.json.Json;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import org.glassfish.admin.rest.composite.RestModel;

/**
 *
 * @author jdlee
 */
public class LegacySseResponseBody<T extends RestModel> extends RestModelResponseBody<T> {

    private Map<String, List<String>> headers = new HashMap<String, List<String>>();

    public LegacySseResponseBody() {
        super();
    }

    public LegacySseResponseBody(boolean includeResourceLinks) {
        super(includeResourceLinks);
    }

    public LegacySseResponseBody<T> addHeader(String name, Object value) {
        if (value != null) {
            List<String> values = headers.get(name);
            if (values == null) {
                values = new ArrayList<String>();
                headers.put(name, values);
            }
            values.add(value.toString());
        }

        return this;
    }

    @Override
    public JsonObject toJson() throws JsonException {
        JsonObjectBuilder jsonBuild = Json.createObjectBuilder(super.toJson());

        if (!headers.isEmpty()) {
            JsonObjectBuilder oBuild = Json.createObjectBuilder();
            for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
                final String key = entry.getKey();
                for (String value : entry.getValue()) {
                    oBuild.add(key, value);
                }
            }
            jsonBuild.add("headers", oBuild);
        }

        return jsonBuild.build();
    }


}
