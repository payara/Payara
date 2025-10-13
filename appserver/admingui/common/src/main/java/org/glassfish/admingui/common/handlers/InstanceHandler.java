/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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
 */
// Portions Copyright [2018-2020] [Payara Foundation and/or its affiliates]

/*
 * InstanceHandler.java
 *
 * Created on August 10, 2006, 2:32 PM
 *
 * To change this template, choose Tools | Template Manager
 * and open the template in the editor.
 */
/**
 *
 * @author anilam
 */
package org.glassfish.admingui.common.handlers;

import com.sun.enterprise.universal.xml.MiniXmlParser.JvmOption;
import com.sun.jsftemplating.annotation.Handler;
import com.sun.jsftemplating.annotation.HandlerInput;
import com.sun.jsftemplating.annotation.HandlerOutput;
import com.sun.jsftemplating.layout.descriptors.handler.HandlerContext;
import org.glassfish.admingui.common.util.GuiUtil;
import org.glassfish.admingui.common.util.RestUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

public class InstanceHandler {
    private static final String SELECTED = "selected";
    private static final String PROFILER = "profiler";
    static final String TARGET = "target";
    static final String MAX_VERSION = "maxVersion";
    static final String MIN_VERSION = "minVersion";
    static final String JVM_OPTION = "jvmOption";

    /** Creates a new instance of InstanceHandler */
    public InstanceHandler() {
    }

    @Handler(id="getJvmOptionsValues",
        input={
            @HandlerInput(name="endpoint",   type=String.class, required=true),
            @HandlerInput(name="attrs", type=Map.class, required=false)
        },
        output={
            @HandlerOutput(name="result", type=java.util.List.class)})
    public static void getJvmOptionsValues(HandlerContext handlerCtx) {
        try {
            List<Map<String, String>> list = getJvmOptions(handlerCtx);
            List<Map<String, Object>> optionValues = new ArrayList<>();
            for (Map<String, String> item : list) {
                Map<String, Object> valueMap = new HashMap<>(item);
                valueMap.put(SELECTED, false);
                optionValues.add(valueMap);
            }
            handlerCtx.setOutputValue("result", optionValues);
        } catch (Exception ex){
            handlerCtx.setOutputValue("result", new ArrayList<>());
            GuiUtil.getLogger().info(GuiUtil.getCommonMessage("log.error.getJvmOptionsValues") + ex.getLocalizedMessage());
            if (GuiUtil.getLogger().isLoggable(Level.FINE)){
                ex.printStackTrace();
            }
        }
    }
    
    public static List<Map<String, String>> getJvmOptions(HandlerContext handlerCtx) {
        List<Map<String, String>> list;
        String endpoint = (String) handlerCtx.getInputValue("endpoint");
        if (!endpoint.endsWith(".json"))
            endpoint = endpoint + ".json";
        Map<String, Object> attrs = (Map<String, Object>) handlerCtx.getInputValue("attrs");
        Map<String, Map> result = (Map<String, Map>) RestUtil.restRequest(endpoint, attrs, "get", handlerCtx, false).get("data");
        if (result == null) {
            list = new ArrayList<>();
        } else {
            list = (ArrayList<Map<String, String>>) result.get("extraProperties").get("leafList");
        }
        return list;
    }
 
   @Handler(id="saveJvmOptionValues",
        input={
            @HandlerInput(name="endpoint",   type=String.class, required=true),
            @HandlerInput(name=TARGET,   type=String.class, required=true),
            @HandlerInput(name="attrs", type=Map.class, required=false),
            @HandlerInput(name=PROFILER, type=String.class, required=true),
            @HandlerInput(name="options",   type=List.class),
            @HandlerInput(name="deleteProfileEndpoint",   type=String.class),
            @HandlerInput(name="origList",   type=List.class)
            } )
   public static void saveJvmOptionValues(HandlerContext handlerCtx) {
        String endpoint = (String) handlerCtx.getInputValue("endpoint");
        String target = (String) handlerCtx.getInputValue(TARGET);
        try {
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> options = (List<Map<String, Object>>) handlerCtx.getInputValue("options");
            List<Map<String, Object>> newList = new ArrayList<>();
            for (Map<String, Object> oneRow : options) {
                Map<String, Object> newRow = new HashMap<>();
                oneRow.forEach((key, value) -> {
                    if (!SELECTED.equalsIgnoreCase(key)) {
                        newRow.put(key, value);
                    }
                });
                newList.add(newRow);
            }
            List<Map<String, String>> oldList = getJvmOptions(handlerCtx);
            if (newList.equals(oldList)) {
                // if old list is same as new list, return without saving anything
                return;
            }
            Map<String, Object> payload = new HashMap<>();
            payload.put(PROFILER, (String)handlerCtx.getInputValue(PROFILER));
            prepareJvmOptionPayload(payload, target, newList);
            RestUtil.restRequest(endpoint, payload, "POST", handlerCtx, false, true);
        } catch (Exception ex) {
            //If this is called during create profile, we want to delete the profile which was created, and stay at the same
            //place for user to fix the jvm options.
            String deleteProfileEndpoint = (String) handlerCtx.getInputValue("deleteProfileEndpoint");
            if (!GuiUtil.isEmpty(deleteProfileEndpoint)){
                Map<String, Object> attrMap = new HashMap<>();
                attrMap.put(TARGET, (String) handlerCtx.getInputValue(TARGET));
                RestUtil.restRequest(deleteProfileEndpoint, attrMap, "DELETE", handlerCtx, false, false);
            }

            //If the origList is not empty,  we want to restore it. Since POST remove all options first and then add it back. As a
            //result, all previous existing option is gone.
            List<Map<String, Object>> origList = (List<Map<String, Object>>) handlerCtx.getInputValue("origList");
            Map<String, Object> payload1 = new HashMap<>();
            if (endpoint.contains(PROFILER)) {
                payload1.put(PROFILER, "true");
            }
            if ( (origList != null) && origList.size()>0){
                prepareJvmOptionPayload(payload1, target, origList);
                RestUtil.restRequest(endpoint, payload1, "POST", handlerCtx, false, false);
            }
            GuiUtil.handleException(handlerCtx, ex);
        }
    }

    private static void prepareJvmOptionPayload(Map<String, Object> payload, String target, List<Map<String, Object>> options) {
        payload.put(TARGET, target);
        for (Map<String, Object> oneRow : options) {
            String jvmOptionUnescaped = new JvmOption((String) oneRow.get(JVM_OPTION),
                    (String) oneRow.get(MIN_VERSION), (String) oneRow.get(MAX_VERSION)).toString();
            String jvmOptionEscape = UtilHandlers.escapePropertyValue(jvmOptionUnescaped);         //refer to GLASSFISH-19069
            ArrayList<String> kv = getKeyValuePair(jvmOptionEscape);
            payload.put(kv.get(0), kv.get(1));
        }
    }

    public static ArrayList<String> getKeyValuePair(String str) {
        ArrayList<String> list = new ArrayList<>(2);
        int index = str.indexOf('=');
        String key = "";
        String value = "";
        if (index != -1) {
            key = str.substring(0,str.indexOf('='));
            value = str.substring(str.indexOf('=')+1,str.length());
        } else {
            key = str;
        }
        if (key.startsWith("-XX:"))
            key = "\"" + key + "\"";
        list.add(0, key);
        list.add(1, value);
        return list;
    }
}
