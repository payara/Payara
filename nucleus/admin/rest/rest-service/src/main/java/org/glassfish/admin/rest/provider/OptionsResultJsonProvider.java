/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.rest.provider;

import org.glassfish.admin.rest.results.OptionsResult;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;
import java.util.Iterator;
import java.util.Set;
import java.util.logging.Level;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import org.glassfish.admin.rest.RestLogging;


/**
 * Json provider for OptionsResult.
 *
 * @author Rajeshwar Patil
 */
@Provider
@Produces(MediaType.APPLICATION_JSON)
public class OptionsResultJsonProvider extends BaseProvider<OptionsResult> {
    private static final String NAME = "name";
    private static final String MESSAGE_PARAMETERS = "messageParameters";

    public OptionsResultJsonProvider() {
        super(OptionsResult.class, MediaType.APPLICATION_JSON_TYPE);
    }

    /**
     * Gets json representation for the given OptionsResult object
     * @param proxy
     * @return 
     */
    @Override
    public String getContent(OptionsResult proxy) {
        JsonObjectBuilder obj = Json.createObjectBuilder();
        try {
            obj.add(proxy.getName(), getRespresenationForMethodMetaData(proxy));
        } catch (JsonException ex) {
            RestLogging.restLogger.log(Level.SEVERE, null, ex);
        }
        return obj.toString();
    }

    public JsonArray getRespresenationForMethodMetaData(OptionsResult proxy) {
        JsonArrayBuilder arr = Json.createArrayBuilder();
        Set<String> methods = proxy.methods();
        Iterator<String> iterator = methods.iterator();
        String methodName;
        while (iterator.hasNext()) {
            try {
                methodName = iterator.next();
                MethodMetaData methodMetaData = proxy.getMethodMetaData(methodName);
                JsonObjectBuilder method = Json.createObjectBuilder();
                method.add(NAME, methodName);
//                method.put(QUERY_PARAMETERS, getQueryParams(methodMetaData));
                method.add(MESSAGE_PARAMETERS, getMessageParams(methodMetaData));
                arr.add(method.build());
            } catch (JsonException ex) {
                RestLogging.restLogger.log(Level.SEVERE, null, ex);
            }
        }

        return arr.build();
    }

//    //get json representation for the method query parameters
//    private JsonObject getQueryParams(MethodMetaData methodMetaData) throws JsonException {
//        JsonObject obj = new JsonObject();
//        if (methodMetaData.sizeQueryParamMetaData() > 0) {
//            Set<String> queryParams = methodMetaData.queryParams();
//            Iterator<String> iterator = queryParams.iterator();
//            String queryParam;
//            while (iterator.hasNext()) {
//                queryParam = iterator.next();
//                ParameterMetaData parameterMetaData = methodMetaData.getQueryParamMetaData(queryParam);
//                obj.put(queryParam, getParameter(parameterMetaData));
//            }
//        }
//
//        return obj;
//    }

    private JsonObject getParameter(ParameterMetaData parameterMetaData) throws JsonException {
        JsonObjectBuilder result = Json.createObjectBuilder();
        Iterator<String> iterator = parameterMetaData.attributes().iterator();
        String attributeName;
        while (iterator.hasNext()) {
            attributeName = iterator.next();
            result.add(attributeName, parameterMetaData.getAttributeValue(attributeName));
        }
        return result.build();
    }

    private JsonObject getMessageParams(MethodMetaData methodMetaData) throws JsonException {
        JsonObjectBuilder result = Json.createObjectBuilder();
        if (methodMetaData.sizeParameterMetaData() > 0) {
            Set<String> parameters = methodMetaData.parameters();
            Iterator<String> iterator = parameters.iterator();
            String parameter;
            while (iterator.hasNext()) {
                parameter = iterator.next();
                ParameterMetaData parameterMetaData = methodMetaData.getParameterMetaData(parameter);
                result.add(parameter, getParameter(parameterMetaData));
            }
        }

        return result.build();
    }
}
