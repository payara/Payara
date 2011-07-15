/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2011 Oracle and/or its affiliates. All rights reserved.
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
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.admin.rest.results.ActionReportResult;
import org.glassfish.api.ActionReport.MessagePart;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;
import java.lang.reflect.Field;
import java.util.*;
import org.glassfish.admin.rest.utils.xml.RestActionReporter;

/**
 * @author Ludovic Champenois
 * @author Jason Lee
 */
@Provider
@Produces({MediaType.APPLICATION_JSON,"application/x-javascript"})
public class ActionReportResultJsonProvider extends BaseProvider<ActionReportResult> {
    public ActionReportResultJsonProvider() {
        super(ActionReportResult.class, MediaType.APPLICATION_JSON_TYPE);
    }

    @Override
    public String getContent(ActionReportResult proxy) {
        RestActionReporter ar = (RestActionReporter)proxy.getActionReport();
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
        result.put("message", (ar instanceof RestActionReporter) ? ((RestActionReporter)ar).getCombinedMessage() : ar.getMessage());
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
            object.put("message", part.getMessage());
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
            Object value = getJsonObject(entry.getValue());
            extraProperties.put(key, value);
        }

        return extraProperties;
    }

    protected Object getJsonObject(Object object) throws JSONException {
        Object result = null;
        if (object instanceof Collection) {
            result = processCollection((Collection)object);
        } else if (object instanceof Map) {
            result = processMap((Map)object);
        } else if (object == null) {
            result = JSONObject.NULL;
        } else {
            result = object;
        }

        return result;
    }

    protected JSONArray processCollection(Collection c) throws JSONException {
        JSONArray result = new JSONArray();
        Iterator i = c.iterator();
        while (i.hasNext()) {
            Object item = getJsonObject(i.next());
            result.put(item);
        }

        return result;
    }

    protected JSONObject processMap(Map map) throws JSONException {
        JSONObject result = new JSONObject();

        for (Map.Entry entry : (Set<Map.Entry>)map.entrySet()) {
            result.put(entry.getKey().toString(), getJsonObject(entry.getValue()));
        }

        return result;
    }

    protected <T> T getFieldValue (ActionReporter ar, String name, T type) {
        try {
            Class<?> clazz = ar.getClass().getSuperclass();
            Field field = clazz.getDeclaredField(name);
            field.setAccessible(true);
            return (T)field.get(ar);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
}