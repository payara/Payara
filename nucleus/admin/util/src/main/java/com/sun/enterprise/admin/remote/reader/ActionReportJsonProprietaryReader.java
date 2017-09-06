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
 *
 * Portions Copyright [2017] Payara Foundation and/or affiliates
 */
package com.sun.enterprise.admin.remote.reader;

import com.sun.enterprise.admin.util.AdminLoggerInfo;
import com.sun.enterprise.util.io.FileUtils;
import java.io.*;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonException;
import javax.json.JsonObject;
import javax.json.stream.JsonParser;
import org.glassfish.api.ActionReport;
import org.glassfish.api.ActionReport.MessagePart;

/** Reads ActionReport from Json format.
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
            JsonParser parser = Json.createParser(new StringReader(str));
            parser.next();
            JsonObject json = parser.getObject();
            //JsonObject jsonObject = json.getJsonObject("action-report");
            CliActionReport result = new CliActionReport();
            fillActionReport(result, json);
            return result;
        } catch (JsonException ex) {
            LoggerRef.logger.log(Level.SEVERE, AdminLoggerInfo.mUnexpectedException, ex);
            throw new IOException(ex);
        }
    }
    
    public static void fillActionReport(final ActionReport ar, final JsonObject json) throws JsonException {
        ar.setActionExitCode(ActionReport.ExitCode.valueOf(json.getString("exit_code")));
        ar.setActionDescription(json.getString("command", null));
        String failure = json.getString("failure_cause", null);
        if (failure != null && !failure.isEmpty()) {
            ar.setFailureCause(new Exception(failure));
        }
        ar.setExtraProperties((Properties) extractMap(json.getJsonObject("extraProperties"), new Properties()));
        ar.getTopMessagePart().setMessage(json.getString("top_message", json.getString("message", null)));
        Properties props = (Properties) extractMap(json.getJsonObject("properties"), new Properties());
        for (Map.Entry entry : props.entrySet()) {
            ar.getTopMessagePart().addProperty(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
        }
        //Sub messages
        fillSubMessages(ar.getTopMessagePart(), json.getJsonArray("children"));
        //Sub action reports
        JsonArray subJsons = json.getJsonArray("subReports");
        if (subJsons != null) {
            for (int i = 0; i < subJsons.size(); i++) {
                JsonObject subJson = subJsons.getJsonObject(i);
                fillActionReport(ar.addSubActionsReport(), subJson);
            }
        }
    }
    
    private static void fillSubMessages(final ActionReport.MessagePart mp, final JsonArray json) throws JsonException {
        if (json == null) {
            return;
        }
        for (int i = 0; i < json.size(); i++) {
            JsonObject subJson = json.getJsonObject(i);
            MessagePart child = mp.addChild();
            child.setMessage(subJson.getString("message", null));
            Properties props = (Properties) extractMap(subJson.getJsonObject("properties"), new Properties());
            for (Map.Entry entry : props.entrySet()) {
                child.addProperty(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
            }
            fillSubMessages(child, subJson.getJsonArray("children"));
        }
    }
    
//    private void fillMessage(ActionReport.MessagePart mp, JsonObject json) throws JsonException {
//        mp.setMessage(json.optString("value"));
//        mp.setChildrenType(json.optString("children-type"));
//        Properties props = extractProperties("properties", json);
//        for (String key : props.stringPropertyNames()) {
//            mp.addProperty(key, props.getProperty(key));
//        }
//        JsonArray subJsons = extractArray("messages", json);
//        for (int i = 0; i < subJsons.length(); i++) {
//            JsonObject subJson = subJsons.getJsonObject(i);
//            fillMessage(mp.addChild(), subJson);
//        }
//    }
    
    private static Object extractGeneral(final Object obj) throws JsonException {
        if (obj == null) {
            return null;
        }
        if (obj instanceof JsonObject) {
            return extractMap((JsonObject) obj, null);
        } else if (obj instanceof JsonArray) {
            return extractCollection((JsonArray) obj, null);
        } else {
            return obj;
        }
    }
    
    private static Map extractMap(final JsonObject json, Map preferredResult) throws JsonException {
        if (json == null) {
            return preferredResult;
        }
        if (preferredResult == null) {
            preferredResult = new HashMap();
        }
        for (String key : json.keySet()) {
            preferredResult.put(key, extractGeneral(json.get(key)));
        }
        return preferredResult;
    }
    
    private static Collection extractCollection(final JsonArray array, Collection preferredResult) throws JsonException {
        if (array == null) {
            return preferredResult;
        }
        if (preferredResult == null) {
            preferredResult = new ArrayList(array.size());
        }
        for (int i = 0; i < array.size(); i++) {
            preferredResult.add(extractGeneral(array.get(i)));
        }
        return preferredResult;
    }
    
//    private Properties extractProperties(final String key, final JsonObject json) throws JsonException {
//        Properties result = new Properties();
//        JsonArray array = extractArray(key, json);
//        for (int i = 0; i < array.length(); i++) {
//            JsonObject entry = array.getJsonObject(i);
//            Iterator keys = entry.keys();
//            while (keys.hasNext()) {
//                String inKey = (String) keys.next();
//                result.put(inKey, entry.getString(key));
//            }
//        }
//        return result;
//    }
//    
//    private JsonArray extractArray(final String key, final JsonObject json) {
//        Object res = json.opt(key);
//        if (res == null) {
//            return new JsonArray();
//        }
//        if (res instanceof JsonArray) {
//            return (JsonArray) res;
//        } else {
//            JsonArray result = new JsonArray();
//            result.put(res);
//            return result;
//        }
//    }
    
}
