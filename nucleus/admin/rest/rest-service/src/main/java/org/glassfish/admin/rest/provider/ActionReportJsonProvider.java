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
 */

package org.glassfish.admin.rest.provider;

import com.sun.enterprise.v3.common.ActionReporter;
import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.*;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.admin.rest.utils.JsonUtil;
import org.glassfish.admin.rest.utils.xml.RestActionReporter;
import org.glassfish.api.ActionReport.MessagePart;

/**
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

    @Override
    public String getContent(ActionReporter ar) {
        String JSONP=getCallBackJSONP();
        try {
            JSONObject result = processReport(ar);
            int indent = getFormattingIndentLevel();
            if (indent > -1) {
                if (JSONP==null){
                    return result.toString(indent);
                }else{
                    return JSONP +"("+result.toString(indent)+")";
                }
            } else {
                if (JSONP==null){
                    return result.toString();
                }else{
                    return JSONP +"("+result.toString()+")";
                }
            }
        } catch (JSONException ex) {
            throw new RuntimeException(ex);
        }
    }

    protected JSONObject processReport(ActionReporter ar) throws JSONException {
        JSONObject result = new JSONObject();
        result.put("message", (ar instanceof RestActionReporter) ? ((RestActionReporter)ar).getCombinedMessage() : decodeEol(ar.getMessage()));
        result.put("command", ar.getActionDescription());
        result.put("exit_code", ar.getActionExitCode());

        Properties properties = ar.getTopMessagePart().getProps();
        if ((properties != null) && (!properties.isEmpty())) {
            result.put("properties", properties);
        }

        Properties extraProperties = ar.getExtraProperties();
        if ((extraProperties != null) && (!extraProperties.isEmpty())) {
            result.put("extraProperties", getExtraProperties(result, extraProperties));
        }

        List<MessagePart> children = ar.getTopMessagePart().getChildren();
        if ((children != null) && (!children.isEmpty())) {
            result.put("children", processChildren(children));
        }

        List<ActionReporter> subReports = ar.getSubActionsReport();
        if ((subReports != null) && (!subReports.isEmpty())) {
            result.put("subReports", processSubReports(subReports));
        }

        return result;
    }

    protected JSONArray processChildren(List<MessagePart> parts) throws JSONException {
        JSONArray array = new JSONArray();

        for (MessagePart part : parts) {
            JSONObject object = new JSONObject();
            object.put("message", decodeEol(part.getMessage()));
            object.put("properties", part.getProps());
            List<MessagePart> children = part.getChildren();
            if (children.size() > 0) {
                object.put("children", processChildren(part.getChildren()));
            }
            array.put(object);
        }

        return array;
    }

    protected JSONArray processSubReports(List<ActionReporter> subReports) throws JSONException {
        JSONArray array = new JSONArray();

        for (ActionReporter subReport : subReports) {
            array.put(processReport(subReport));
        }

        return array;
    }

    protected JSONObject getExtraProperties(JSONObject object, Properties props) throws JSONException {
        JSONObject extraProperties = new JSONObject();
        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            String key = entry.getKey().toString();
            Object value = JsonUtil.getJsonObject(entry.getValue());
            extraProperties.put(key, value);
        }

        return extraProperties;
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
            return str;
        }
        return str.replace(ActionReporter.EOL_MARKER, "\n");
    }
}
