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
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
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
 */
// Portions Copyright [2017-2021] Payara Foundation and/or affiliates

package org.glassfish.admin.rest.provider;

import org.glassfish.admin.rest.results.OptionsResult;

import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.ext.Provider;
import java.util.Set;
import java.util.logging.Level;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonArrayBuilder;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonObjectBuilder;
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
        for (String methodName : methods){
            try {
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
        
        for (String attributeName: parameterMetaData.attributes()){
            result.add(attributeName, parameterMetaData.getAttributeValue(attributeName));
        }
        
        return result.build();
    }

    private JsonObject getMessageParams(MethodMetaData methodMetaData) throws JsonException {
        JsonObjectBuilder result = Json.createObjectBuilder();
        if (methodMetaData.sizeParameterMetaData() > 0) {
            Set<String> parameters = methodMetaData.parameters();
            for (String parameter: parameters){
                 ParameterMetaData parameterMetaData = methodMetaData.getParameterMetaData(parameter);
                result.add(parameter, getParameter(parameterMetaData));
            }
        }

        return result.build();
    }
}
