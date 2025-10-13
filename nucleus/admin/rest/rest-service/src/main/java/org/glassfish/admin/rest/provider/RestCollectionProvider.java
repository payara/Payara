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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
 * Portions Copyright [2017-2019] Payara Foudation and/or affiliates
 */
package org.glassfish.admin.rest.provider;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.json.Json;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonException;
import jakarta.json.JsonObjectBuilder;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.ext.Provider;

import org.glassfish.admin.rest.Constants;
import org.glassfish.admin.rest.composite.RestCollection;
import org.glassfish.admin.rest.composite.RestModel;
import org.glassfish.admin.rest.composite.metadata.RestModelMetadata;
import org.glassfish.admin.rest.utils.JsonUtil;

/**
 * @since 4.0
 * @author: jdlee
 */
@Provider
@Produces({Constants.MEDIA_TYPE_JSON})
public class RestCollectionProvider extends BaseProvider<RestCollection> {
    public RestCollectionProvider() {
        super(RestCollection.class, Constants.MEDIA_TYPE_JSON_TYPE);
    }

    /**
     * Converts a {@link RestCollection} into a Json object and then returns it as as String representation.
     * @param proxy
     * @return 
     */
    @Override
    public String getContent(RestCollection proxy) {
        StringBuilder sb = new StringBuilder();
        final List<String> wrapObjectHeader = requestHeaders.get().getRequestHeader("X-Wrap-Object");
        final List<String> skipMetadataHeader = requestHeaders.get().getRequestHeader("X-Skip-Metadata");
        boolean wrapObject = ((wrapObjectHeader != null) && (!wrapObjectHeader.isEmpty()));
        boolean skipMetadata = ((skipMetadataHeader != null) && (skipMetadataHeader.get(0).equalsIgnoreCase("true")));

        JsonArrayBuilder models = Json.createArrayBuilder();
        JsonArrayBuilder metadata = Json.createArrayBuilder();
        for (Map.Entry<RestModelMetadata, RestModel> entry : (Set<Map.Entry<RestModelMetadata, RestModel>>)proxy.entrySet()) {
            try {
                models.add(JsonUtil.getJsonValue(entry.getValue()));

                RestModelMetadata md = entry.getKey();
                JsonObjectBuilder mdo = Json.createObjectBuilder();
                mdo.add("id", md.getId());
                metadata.add(mdo.build());
            } catch (JsonException e) {
                Logger.getLogger(RestCollectionProvider.class.getName()).log(Level.SEVERE,"Unable to parse create Json",e);
            }
        }
        JsonObjectBuilder response = Json.createObjectBuilder();
        try {
            response.add("items", models.build());
            if (!skipMetadata) {
                response.add("metadata", metadata.build());
            }
            sb.append(response.toString());
        } catch (JsonException e) {
            Logger.getLogger(RestCollectionProvider.class.getName()).log(Level.SEVERE,"Unable to parse create Json",e);
        }

        return (wrapObject ? " { items : " : "") + sb.toString() + (wrapObject ? "}" : "");
    }
}
