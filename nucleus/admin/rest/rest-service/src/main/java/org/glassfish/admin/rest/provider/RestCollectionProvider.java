/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.admin.rest.provider;

import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.Produces;
import javax.ws.rs.ext.Provider;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.admin.rest.Constants;
import org.glassfish.admin.rest.composite.RestCollection;
import org.glassfish.admin.rest.composite.RestModel;
import org.glassfish.admin.rest.composite.metadata.RestModelMetadata;
import org.glassfish.admin.rest.utils.JsonUtil;

/**
 * @author: jdlee
 */
@Provider
@Produces({Constants.MEDIA_TYPE_JSON})
public class RestCollectionProvider extends BaseProvider<RestCollection> {
    public RestCollectionProvider() {
        super(RestCollection.class, Constants.MEDIA_TYPE_JSON_TYPE);
    }

    @Override
    public String getContent(RestCollection proxy) {
        StringBuilder sb = new StringBuilder();
        final List<String> wrapObjectHeader = requestHeaders.get().getRequestHeader("X-Wrap-Object");
        final List<String> skipMetadataHeader = requestHeaders.get().getRequestHeader("X-Skip-Metadata");
        boolean wrapObject = ((wrapObjectHeader != null) && (wrapObjectHeader.size() > 0));
        boolean skipMetadata = ((skipMetadataHeader != null) && (skipMetadataHeader.get(0).equalsIgnoreCase("true")));

        JSONArray models = new JSONArray();
        JSONArray metadata = new JSONArray();
        for (Map.Entry<RestModelMetadata, RestModel> entry : (Set<Map.Entry<RestModelMetadata, RestModel>>)proxy.entrySet()) {
            try {
                models.put(JsonUtil.getJsonObject(entry.getValue()));

                RestModelMetadata md = entry.getKey();
                JSONObject mdo = new JSONObject();
                mdo.put("id", md.getId());
                metadata.put(mdo);
            } catch (JSONException e) {
                e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
            }
        }
        JSONObject response = new JSONObject();
        try {
            response.put("items", models);
            if (!skipMetadata) {
                response.put("metadata", metadata);
            }
            sb.append(response.toString(getFormattingIndentLevel()));
        } catch (JSONException e) {
            e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
        }

        return (wrapObject ? " { items : " : "") + sb.toString() + (wrapObject ? "}" : "");
    }
}
