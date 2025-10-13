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
 * Portions Copyright [2017-2021] Payara Foundation and/or affiliates
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
import java.util.Map.Entry;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.json.Json;
import jakarta.json.JsonArray;
import jakarta.json.JsonException;
import jakarta.json.JsonObject;
import jakarta.json.JsonString;
import jakarta.json.JsonValue;
import jakarta.json.stream.JsonParser;
import org.glassfish.api.ActionReport;
import org.glassfish.api.ActionReport.MessagePart;

/** Reads ActionReport from Json format.
 *
 * @since 4.0
 * @author mmares
 */
public class ActionReportJsonProprietaryReader implements ProprietaryReader<ActionReport> {
    
    static class LoggerRef {
        private static final Logger LOGGER = AdminLoggerInfo.getLogger();
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
        try (JsonParser parser = Json.createParser(new StringReader(str))) {
            parser.next();
            JsonObject json = parser.getObject();
            CliActionReport result = new CliActionReport();
            fillActionReport(result, json);
            return result;
        } catch (JsonException ex) {
            LoggerRef.LOGGER.log(Level.SEVERE, AdminLoggerInfo.mUnexpectedException, ex);
            throw new IOException(ex);
        }
    }
    
    /**
     * Turns a {@link JsonObject} into an {@link ActionReport}
     * <p>
     * The Json Object must contain a valid {@code exit_code} at the top level.
     * @param ar
     * @param json
     * @throws JsonException 
     */
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
            Object entryValue = entry.getValue();
            //Check if it is a JsonString, because String.valueOf(getValue() is equivalent to getValue().toString
            //which for JsonString is "entry" and the speech marks need to be removed to that it is just entry
            //See difference between JsonString.toString and JsonString.getValue
            if (entryValue instanceof JsonString){
                ar.getTopMessagePart().addProperty(String.valueOf(entry.getKey()), ((JsonString)entry.getValue()).getString());
            } else {
                ar.getTopMessagePart().addProperty(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
            }
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
    
    /**
     * Fills all messages below top_message of an action report
     * @param mp
     * @param json
     * @throws JsonException 
     */
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
                Object entryValue = entry.getValue();
                if (entryValue instanceof JsonString) {
                    child.addProperty(String.valueOf(entry.getKey()), ((JsonString) entry.getValue()).getString());
                } else {
                    child.addProperty(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
                }
                child.addProperty(String.valueOf(entry.getKey()), String.valueOf(entry.getValue()));
            }
            fillSubMessages(child, subJson.getJsonArray("children"));
        }
    }
    
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
    
    /**
     * Adds a {@link JsonObject} to a {@link Map}
     * @param json
     * @param preferredResult
     * @return
     * @throws JsonException 
     */
    private static Map extractMap(final JsonObject json, Map preferredResult) throws JsonException {
        if (json == null) {
            return preferredResult;
        }
        if (preferredResult == null) {
            preferredResult = new HashMap();
        }
        for (Entry<String, JsonValue> entry : json.entrySet()) {
            preferredResult.put(entry.getKey(), extractGeneral(entry.getValue()));
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
    
}
