/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
 * Portions Copyright [2017-2019] [Payara Foundation and/or its affiliates]
 */

package org.glassfish.admin.rest.provider;

import com.sun.enterprise.admin.report.ActionReporter;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;
import org.glassfish.admin.rest.utils.JsonUtil;
import org.glassfish.admin.rest.utils.xml.RestActionReporter;
import org.glassfish.api.ActionReport.MessagePart;

/**
 * @since 4.0
 * @author Ludovic Champenois
 * @author Jason Lee
 * @author mmares
 */
@Provider
@Produces({MediaType.APPLICATION_JSON, "application/x-javascript"})
public class ActionReportJsonProvider extends BaseProvider<ActionReporter> {
    
    public ActionReportJsonProvider() {
        super(ActionReporter.class, MediaType.APPLICATION_JSON_TYPE);
    }
    
    @Override
    protected boolean isGivenTypeWritable(Class<?> type, Type genericType) {
        return desiredType.isAssignableFrom(type);
    }

    /**
     * Returns the content of the {@link ActionReporter} in Json format as a {@link String}.
     * <p>
     * This is equivalent to {@code processReport(ar).toString()}
     * @param ar
     * @return 
     */
    @Override
    public String getContent(ActionReporter ar) {
        String JsonP = getCallBackJSONP();
        try {
            JsonObject result = processReport(ar);
            
            if (JsonP == null) {
                return result.toString();
            } else {
                return JsonP + "(" + result.toString() + ")";
            }
        } catch (JsonException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     * Converts an ActionReport into a JsonObject
     * @param ar
     * @return
     * @throws JsonException 
     */
    protected JsonObject processReport(ActionReporter ar) throws JsonException {
        JsonObjectBuilder result = Json.createObjectBuilder();
        if (ar instanceof RestActionReporter){
            result.add("message", ((RestActionReporter)ar).getCombinedMessage());
        } else {
            String message = decodeEol(ar.getMessage());
            if (message != null){
                result.add("message", message);
            }
        }
        String desc = ar.getActionDescription();
        if (desc != null){
            result.add("command", ar.getActionDescription());
        } else {
            result.add("command", JsonValue.NULL);
        }
        result.add("exit_code", ar.getActionExitCode().toString());
        
        Properties properties = ar.getTopMessagePart().getProps();
        if ((properties != null) && (!properties.isEmpty())) {
            JsonObject propBuilder = Json.createObjectBuilder((Map)properties).build();
            result.add("properties", propBuilder);
        }
       
        Properties extraProperties = ar.getExtraProperties();
        if ((extraProperties != null) && (!extraProperties.isEmpty())) {
            result.add("extraProperties", getExtraProperties(extraProperties));
        }

        List<MessagePart> children = ar.getTopMessagePart().getChildren();
        if ((children != null) && (!children.isEmpty())) {
            result.add("children", processChildren(children));
        }

        List<ActionReporter> subReports = ar.getSubActionsReport();
        if ((subReports != null) && (!subReports.isEmpty())) {
            result.add("subReports", processSubReports(subReports));
        }

        return result.build();
    }

    protected JsonArray processChildren(List<MessagePart> parts) throws JsonException {
        JsonArrayBuilder array = Json.createArrayBuilder();

        for (MessagePart part : parts) {
            JsonObjectBuilder object = Json.createObjectBuilder();
            String message = decodeEol(part.getMessage());
            if (message != null){
                object.add("message", decodeEol(part.getMessage()));
            } else {
                 object.add("message", JsonValue.NULL);
            }
            object.add("properties", Json.createObjectBuilder((Map)part.getProps()).build());
            List<MessagePart> children = part.getChildren();
            if (!children.isEmpty()) {
                object.add("children", processChildren(part.getChildren()));
            }
            array.add(object.build());
        }

        return array.build();
    }

    protected JsonArray processSubReports(List<ActionReporter> subReports) throws JsonException {
        JsonArrayBuilder array = Json.createArrayBuilder();

        for (ActionReporter subReport : subReports) {
            array.add(processReport(subReport));
        }

        return array.build();
    }

    protected JsonObject getExtraProperties(Properties props) throws JsonException {
        JsonObjectBuilder extraProperties = Json.createObjectBuilder();
        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            String key = entry.getKey().toString();
            Object value = JsonUtil.getJsonValue(entry.getValue());
            extraProperties.add(key, JsonUtil.getJsonValue(value));
        }

        return extraProperties.build();
    }

    protected <T> T getFieldValue(final ActionReporter ar, final String name, final T type) {
        return AccessController.doPrivileged(new PrivilegedAction<T>() {
            @Override
            public T run() {
                T value = null;
                try {
                    final Class<?> clazz = ar.getClass().getSuperclass();
                    final Field field = clazz.getDeclaredField(name);
                    field.setAccessible(true);
                    value = (T) field.get(ar);
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
                return value;
            }
        });
    }
    
    protected String decodeEol(String str) {
        if (str == null) {
            return null;
        }
        
        return str.replace(ActionReporter.EOL_MARKER, "\n");
    }
}
