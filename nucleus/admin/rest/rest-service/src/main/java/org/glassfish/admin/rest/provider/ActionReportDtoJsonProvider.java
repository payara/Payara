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
import java.io.IOException;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.Provider;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.admin.rest.Constants;
import org.glassfish.api.ActionReport;

/** Transfers ActionReport like DTO (with all attributes and unsiplified 
 * complexity). The goal is to provide real object transfer mechanism.
 *
 * @author mmares
 */
@Provider
@Produces("actionreport/json")
public class ActionReportDtoJsonProvider extends BaseProvider<ActionReporter> {

    public ActionReportDtoJsonProvider() {
        super(ActionReporter.class, new MediaType("actionreport", "json"));
    }
    
    @Override
    protected boolean isGivenTypeWritable(Class<?> type, Type genericType) {
        return desiredType.isAssignableFrom(type);
    }
    
    @Override
    public void writeTo(ActionReporter proxy, Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders, OutputStream entityStream) throws IOException, WebApplicationException {
        Object indent = httpHeaders.getFirst("X-Indent");
        int indentLevel = indent == null ? -1 : 4;
        entityStream.write(getContent(proxy, indentLevel)
                .getBytes(Constants.ENCODING));
    }
    
    /** Use direct JSONObject implementation instead STaX approach to have
     * full control over result object.
     * 
     * @param proxy
     * @return 
     */
    public String getContent(ActionReporter proxy, int indentRequest) {
        String JSONP=getCallBackJSONP();
        try {
            JSONObject result = new JSONObject();
            result.put("action-report", createJson(proxy));
            int indent = getFormattingIndentLevel();
            if (indentRequest >= 0) {
                indent = indentRequest; //Client wins over configuration
            }
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
    
    private JSONObject createJson(ActionReporter ar) throws JSONException {
        JSONObject result = new JSONObject();
        result.putOpt("exit-code", ar.getActionExitCode().name());
        result.putOpt("description", ar.getActionDescription());
        result.putOpt("failure-cause", (ar.getFailureCause() == null ? null : ar.getFailureCause().getLocalizedMessage()));
        result.putOpt("extra-properties", createJson(ar.getExtraProperties()));
        result.putOpt("message", createJson(ar.getTopMessagePart()));
        result.putOpt("action-reports", createJson4SubReports(ar.getSubActionsReport()));
        return result;
    }
    
    private JSONArray createJson4SubReports(List<ActionReporter> ars) throws JSONException {
        if (ars == null || ars.isEmpty()) {
            return null;
        }
        JSONArray result = new JSONArray();
        for (ActionReporter ar : ars) {
            result.put(createJson(ar));
        }
        return result;
    }
    
    private JSONObject createJson(ActionReport.MessagePart mp) throws JSONException {
        if (mp == null) {
            return null;
        }
        JSONObject result = new JSONObject();
        result.putOpt("value", mp.getMessage());
        result.putOpt("children-type", mp.getChildrenType());
        result.putOpt("properties", createJson(mp.getProps()));
        result.putOpt("messages", createJson4SubMessages(mp.getChildren()));
        return result;
    }
    
    private JSONArray createJson4SubMessages(List<ActionReport.MessagePart> messages) throws JSONException {
        if (messages == null || messages.isEmpty()) {
            return null;
        }
        JSONArray result = new JSONArray();
        for (ActionReport.MessagePart mp : messages) {
            result.put(createJson(mp));
        }
        return result;
    }
    
    private JSONArray createJson(Properties props) throws JSONException {
        if (props == null || props.isEmpty()) {
            return null;
        }
        JSONArray result = new JSONArray();
        for (Map.Entry<Object, Object> entry : props.entrySet()) {
            JSONObject elem = new JSONObject();
            elem.put("name", (String) entry.getKey());
            elem.put("value", (String) entry.getValue());
            result.put(elem);
        }
        return result;
    }

    @Override
    public String getContent(ActionReporter proxy) {
        return getContent(proxy, -1);
    }
    
}
