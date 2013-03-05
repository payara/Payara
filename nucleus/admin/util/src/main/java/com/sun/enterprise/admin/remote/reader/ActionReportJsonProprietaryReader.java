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
 */
package com.sun.enterprise.admin.remote.reader;

import com.sun.enterprise.admin.util.AdminLoggerInfo;
import com.sun.enterprise.util.io.FileUtils;
import java.io.*;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.ActionReport.MessagePart;

/** Reads ActionReport from JSON format.
 *
 * @author mmares
 */
public class ActionReportJsonProprietaryReader implements ProprietaryReader<ActionReport> {
    
    static class LoggerRef {
        private static final Logger logger = AdminLoggerInfo.getLogger();
    }
            
    @Override
    public boolean isReadable(final Class<?> type,
                               final String mimetype) {
        return type.isAssignableFrom(ActionReport.class);
    }
    
    public ActionReport readFrom(final HttpURLConnection urlConnection) throws IOException {
        return readFrom(urlConnection.getInputStream(), urlConnection.getContentType());
    }

    @Override
    public ActionReport readFrom(final InputStream is, final String contentType) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        FileUtils.copy(is, baos, 0);
        String str = baos.toString("UTF-8");
        try {
            JSONObject json = new JSONObject(str);
            //JSONObject jsonObject = json.getJSONObject("action-report");
            CliActionReport result = new CliActionReport();
            fillActionReport(result, json);
            return result;
        } catch (JSONException ex) {
            LoggerRef.logger.log(Level.SEVERE, AdminLoggerInfo.mUnexpectedException, ex);
            throw new IOException(ex);
        }
    }
    
    public static void fillActionReport(final ActionReport ar, final JSONObject json) throws JSONException {
        ar.setActionExitCode(ActionReport.ExitCode.valueOf(json.getString("exit_code")));
        ar.setActionDescription(json.optString("command"));
        String failure = json.optString("failure_cause");
        if (failure != null && !failure.isEmpty()) {
            ar.setFailureCause(new Exception(failure));
        }
        ar.setExtraProperties((Properties) extractMap(json.optJSONObject("extraProperties"), new Properties()));
        ar.getTopMessagePart().setMessage(json.optString("top_message", json.optString("message")));
        Properties props = (Properties) extractMap(json.optJSONObject("properties"), new Properties());
        for (Map.Entry entry : props.entrySet()) {
            ar.getTopMessagePart().addProperty(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
        }
        //Sub messages
        fillSubMessages(ar.getTopMessagePart(), json.optJSONArray("children"));
        //Sub action reports
        JSONArray subJsons = json.optJSONArray("subReports");
        if (subJsons != null) {
            for (int i = 0; i < subJsons.length(); i++) {
                JSONObject subJson = subJsons.getJSONObject(i);
                fillActionReport(ar.addSubActionsReport(), subJson);
            }
        }
    }
    
    private static void fillSubMessages(final ActionReport.MessagePart mp, final JSONArray json) throws JSONException {
        if (json == null) {
            return;
        }
        for (int i = 0; i < json.length(); i++) {
            JSONObject subJson = json.getJSONObject(i);
            MessagePart child = mp.addChild();
            child.setMessage(subJson.optString("message"));
            Properties props = (Properties) extractMap(subJson.optJSONObject("properties"), new Properties());
            for (Map.Entry entry : props.entrySet()) {
                child.addProperty(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
            }
            fillSubMessages(child, subJson.optJSONArray("children"));
        }
    }
    
//    private void fillMessage(ActionReport.MessagePart mp, JSONObject json) throws JSONException {
//        mp.setMessage(json.optString("value"));
//        mp.setChildrenType(json.optString("children-type"));
//        Properties props = extractProperties("properties", json);
//        for (String key : props.stringPropertyNames()) {
//            mp.addProperty(key, props.getProperty(key));
//        }
//        JSONArray subJsons = extractArray("messages", json);
//        for (int i = 0; i < subJsons.length(); i++) {
//            JSONObject subJson = subJsons.getJSONObject(i);
//            fillMessage(mp.addChild(), subJson);
//        }
//    }
    
    private static Object extractGeneral(final Object obj) throws JSONException {
        if (obj == null) {
            return null;
        }
        if (obj instanceof JSONObject) {
            return extractMap((JSONObject) obj, null);
        } else if (obj instanceof JSONArray) {
            return extractCollection((JSONArray) obj, null);
        } else {
            return obj;
        }
    }
    
    private static Map extractMap(final JSONObject json, Map preferredResult) throws JSONException {
        if (json == null) {
            return preferredResult;
        }
        if (preferredResult == null) {
            preferredResult = new HashMap();
        }
        Iterator keys = json.keys();
        while (keys.hasNext()) {
            String key = (String) keys.next();
            preferredResult.put(key, extractGeneral(json.get(key)));
        }
        return preferredResult;
    }
    
    private static Collection extractCollection(final JSONArray array, Collection preferredResult) throws JSONException {
        if (array == null) {
            return preferredResult;
        }
        if (preferredResult == null) {
            preferredResult = new ArrayList(array.length());
        }
        for (int i = 0; i < array.length(); i++) {
            preferredResult.add(extractGeneral(array.get(i)));
        }
        return preferredResult;
    }
    
//    private Properties extractProperties(final String key, final JSONObject json) throws JSONException {
//        Properties result = new Properties();
//        JSONArray array = extractArray(key, json);
//        for (int i = 0; i < array.length(); i++) {
//            JSONObject entry = array.getJSONObject(i);
//            Iterator keys = entry.keys();
//            while (keys.hasNext()) {
//                String inKey = (String) keys.next();
//                result.put(inKey, entry.getString(key));
//            }
//        }
//        return result;
//    }
//    
//    private JSONArray extractArray(final String key, final JSONObject json) {
//        Object res = json.opt(key);
//        if (res == null) {
//            return new JSONArray();
//        }
//        if (res instanceof JSONArray) {
//            return (JSONArray) res;
//        } else {
//            JSONArray result = new JSONArray();
//            result.put(res);
//            return result;
//        }
//    }
    
}
