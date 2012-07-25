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
package com.sun.enterprise.admin.remote.reader;

import com.sun.enterprise.util.io.FileUtils;
import com.sun.logging.LogDomains;
import java.io.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.Consumes;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;
import javax.ws.rs.ext.Provider;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.api.ActionReport;

/** JAX-WS reader for ActionReport in JSON format. This is reader of format 
 * created by {@code ActionReportDtoJsonProvider}.
 *
 * @author mmares
 */
@Provider
@Consumes({"actionreport/json"})
public class ActionReportJsonReader implements MessageBodyReader<ActionReport> {
    
    private static final Logger logger =
            LogDomains.getLogger(ActionReportJsonReader.class, LogDomains.ADMIN_LOGGER);

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        return type.isAssignableFrom(ActionReport.class);
    }

    @Override
    public ActionReport readFrom(Class<ActionReport> type, Type genericType, Annotation[] annotations, MediaType mediaType, MultivaluedMap<String, String> httpHeaders, InputStream entityStream) throws IOException, WebApplicationException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FileUtils.copy(entityStream, baos, 0);
        String str = baos.toString("UTF-8");
        try {
            JSONObject json = new JSONObject(str);
            JSONObject jsonObject = json.getJSONObject("action-report");
            CliActionReport result = new CliActionReport();
            fillActionReport(result, jsonObject);
            return result;
        } catch (JSONException ex) {
            logger.log(Level.SEVERE, null, ex);
            throw new IOException(ex);
        }
    }
    
    private void fillActionReport(final ActionReport ar, final JSONObject json) throws JSONException {
        ar.setActionExitCode(ActionReport.ExitCode.valueOf(json.getString("exit-code")));
        ar.setActionDescription(json.optString("description"));
        String failure = json.optString("failure-cause");
        if (failure != null && !failure.isEmpty()) {
            ar.setFailureCause(new Exception(failure));
        }
        ar.setExtraProperties(extractProperties("extra-properties", json));
        JSONObject message = json.optJSONObject("message");
        if (message != null) {
            fillMessage(ar.getTopMessagePart(), message);
        }
        //Sub actionm reports
        JSONArray subJsons = extractArray("action-reports", json);
        for (int i = 0; i < subJsons.length(); i++) {
            JSONObject subJson = subJsons.getJSONObject(i);
            fillActionReport(ar.addSubActionsReport(), subJson);
        }
    }
    
    private void fillMessage(ActionReport.MessagePart mp, JSONObject json) throws JSONException {
        mp.setMessage(json.optString("value"));
        mp.setChildrenType(json.optString("children-type"));
        Properties props = extractProperties("properties", json);
        for (String key : props.stringPropertyNames()) {
            mp.addProperty(key, props.getProperty(key));
        }
        JSONArray subJsons = extractArray("messages", json);
        for (int i = 0; i < subJsons.length(); i++) {
            JSONObject subJson = subJsons.getJSONObject(i);
            fillMessage(mp.addChild(), subJson);
        }
    }
    
    private Properties extractProperties(final String key, final JSONObject json) throws JSONException {
        Properties result = new Properties();
        JSONArray array = extractArray(key, json);
        for (int i = 0; i < array.length(); i++) {
            JSONObject entry = array.getJSONObject(i);
            result.put(entry.getString("name"), entry.getString("value"));
        }
        return result;
    }
    
    private JSONArray extractArray(final String key, final JSONObject json) {
        Object res = json.opt(key);
        if (res == null) {
            return new JSONArray();
        }
        if (res instanceof JSONArray) {
            return (JSONArray) res;
        } else {
            JSONArray result = new JSONArray();
            result.put(res);
            return result;
        }
    }
    
}
