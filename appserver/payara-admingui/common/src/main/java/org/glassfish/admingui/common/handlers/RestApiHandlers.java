/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admingui.common.handlers;

import java.util.*;
import java.util.logging.Logger;
import com.sun.jsftemplating.annotation.Handler;
import com.sun.jsftemplating.annotation.HandlerInput;
import com.sun.jsftemplating.annotation.HandlerOutput;
import com.sun.jsftemplating.layout.descriptors.handler.HandlerContext;
import java.net.URLEncoder;
import java.util.logging.Level;
import org.glassfish.admingui.common.util.GuiUtil;
import org.glassfish.admingui.common.util.RestResponse;
import org.glassfish.admingui.common.util.RestUtil;
import static org.glassfish.admingui.common.util.RestUtil.appendEncodedSegment;
import static org.glassfish.admingui.common.util.RestUtil.buildChildEntityList;
import static org.glassfish.admingui.common.util.RestUtil.buildDefaultValueMap;
import static org.glassfish.admingui.common.util.RestUtil.delete;
import static org.glassfish.admingui.common.util.RestUtil.get;
import static org.glassfish.admingui.common.util.RestUtil.getChildMap;
import static org.glassfish.admingui.common.util.RestUtil.parseResponse;
import static org.glassfish.admingui.common.util.RestUtil.sendCreateRequest;
import static org.glassfish.admingui.common.util.RestUtil.sendUpdateRequest;

/**
 *
 */
public class RestApiHandlers {
    @Handler(id = "gf.getDefaultValues",
            input = {
                    @HandlerInput(name = "endpoint", type = String.class, required = true),
                    @HandlerInput(name = "orig", type = Map.class)
            },
            output = {
                    @HandlerOutput(name = "valueMap", type = Map.class)
            })
    public static void getDefaultValues(HandlerContext handlerCtx) {
        try {
            String endpoint = (String) handlerCtx.getInputValue("endpoint");
            Map<String, String> orig = (Map) handlerCtx.getInputValue("orig");
            Map<String, String> defaultValues = buildDefaultValueMap(endpoint);

            if (orig == null) {
                handlerCtx.setOutputValue("valueMap", defaultValues);
            } else {
                //we only want to fill in any default value that is available. Preserve all other fields user has entered.
                for (String origKey : orig.keySet()) {
                    String defaultV = defaultValues.get(origKey);
                    if (defaultV != null) {
                        orig.put(origKey, defaultV);
                    }else{
                        //this is a hack for 4.0. refer to GLASSFISH-20192
                        if (origKey.equals("deploymentOrder")){
                            orig.put(origKey, "100");
                        }
                    }
                }
                handlerCtx.setOutputValue("valueMap", orig);
            }
        } catch (Exception ex) {
            GuiUtil.handleException(handlerCtx, ex);
        }
    }

    /**
     *        <p> For the given REST endpoint, retrieve the values of the entity and
     *            return those as a Map.  If the entity is not found, an Exception is
     *            thrown.  This is the REST-based alternative to getProxyAttrs.</p>
     */
    @Handler(id = "gf.getEntityAttrs",
            input = {
                    @HandlerInput(name = "endpoint", type = String.class, required = true),
                    @HandlerInput(name = "currentMap", type = Map.class),
                            @HandlerInput(name = "key", type=String.class, defaultValue="entity")},
            output = {
                    @HandlerOutput(name = "valueMap", type = Map.class)
            })
    public static void getEntityAttrs(HandlerContext handlerCtx) {
        // Get the inputs...
        String key = (String) handlerCtx.getInputValue("key");
        String endpoint = (String) handlerCtx.getInputValue("endpoint");
        Map<String, Object> currentMap = (Map<String, Object>) handlerCtx.getInputValue("currentMap");
        Map<String, Object> valueMap = new HashMap();

        try {
            valueMap = RestUtil.getEntityAttrs(endpoint, key);
            // Current values already set?
            if (currentMap != null) {
                valueMap.putAll(currentMap);
            }
        } catch (Exception ex) {
            GuiUtil.handleException(handlerCtx, ex);
        }

        // Return the Map
        handlerCtx.setOutputValue("valueMap", valueMap);
    }

    @Handler(id = "gf.checkIfEndPointExist",
            input = {
                    @HandlerInput(name = "endpoint", type = String.class, required = true)},
            output = {
                    @HandlerOutput(name = "exists", type = Boolean.class)
            })
    public static void checkIfEndPointExist(HandlerContext handlerCtx) {
        boolean result = false;
        RestResponse response = null;
        try {
            response = get((String) handlerCtx.getInputValue("endpoint"));
            result = response.isSuccess();
        }catch(Exception ex){
            GuiUtil.getLogger().info("checkIfEnpointExist failed.");
            if (GuiUtil.getLogger().isLoggable(Level.FINE)){
                ex.printStackTrace();
            }
        } finally {
            if (response != null) {
                response.close();
            }
        }
        handlerCtx.setOutputValue("exists", result);
    }

    /**
     *
     * REST-based version of createProxy
     * @param handlerCtx
     */
    @Handler(id = "gf.createEntity",
            input = {
                    @HandlerInput(name = "endpoint", type = String.class, required = true),
                    @HandlerInput(name = "attrs", type = Map.class, required = true),
                    @HandlerInput(name = "skipAttrs", type = List.class),
                    @HandlerInput(name = "onlyUseAttrs", type = List.class),
                    @HandlerInput(name = "convertToFalse", type = List.class),
                    @HandlerInput(name = "throwException", type = boolean.class, defaultValue = "true")},
            output = {
                    @HandlerOutput(name = "result", type = String.class)
            })
    public static void createEntity(HandlerContext handlerCtx) {
        Map<String, Object> attrs = (Map) handlerCtx.getInputValue("attrs");
        if (attrs == null) {
            attrs = new HashMap<String, Object>();
        }
        String endpoint = (String) handlerCtx.getInputValue("endpoint");

        RestResponse response  = sendCreateRequest(endpoint, attrs, (List) handlerCtx.getInputValue("skipAttrs"),
                (List) handlerCtx.getInputValue("onlyUseAttrs"), (List) handlerCtx.getInputValue("convertToFalse"));

        boolean throwException = (Boolean) handlerCtx.getInputValue("throwException");
        parseResponse(response, handlerCtx, endpoint, attrs, false, throwException);
        //??? I believe this should return a Map, whats the point of returning the endpoint that was passed in.
        //But i haven't looked through all the code, so decide to leave it for now.
        handlerCtx.setOutputValue("result", endpoint);
    }

    /**
     *        <p> This handler can be used to execute a generic REST request.  It
     *            will return a Java data structure based on the response of the
     *            REST request.  'data' and 'attrs' are mutually exclusive.  'data'
     *            is used to pass RAW data to the endpoint (such as JSON).</p>
     */
    @Handler(id = "gf.restRequest",
            input = {
                    @HandlerInput(name="endpoint", type=String.class, required=true),
                    @HandlerInput(name="attrs", type=Map.class, required=false),
                    @HandlerInput(name="data", type=Object.class, required=false),
                    @HandlerInput(name="contentType", type=String.class, required=false),
                    @HandlerInput(name="method", type=String.class, defaultValue="post"),
                    @HandlerInput(name="quiet", type=boolean.class, defaultValue="false"),
                    @HandlerInput(name="throwException", type=boolean.class, defaultValue="true")},
            output = {
                    @HandlerOutput(name="result", type=Map.class)})
    public static void restRequest(HandlerContext handlerCtx) {
        Map<String, Object> attrs = (Map<String, Object>) handlerCtx.getInputValue("attrs");
        String endpoint = (String) handlerCtx.getInputValue("endpoint");
        String method = (String) handlerCtx.getInputValue("method");
        boolean quiet = (Boolean) handlerCtx.getInputValue("quiet");
        boolean throwException = (Boolean) handlerCtx.getInputValue("throwException");
        //refer to bug#6942284. Some of the faulty URL may get here with endpoint set to null
        if (GuiUtil.isEmpty(endpoint)){
            handlerCtx.setOutputValue("result", new HashMap());
        }else{
            try{
                Map result = RestUtil.restRequest(endpoint, attrs, method, handlerCtx, quiet, throwException);
                handlerCtx.setOutputValue("result", result );
            }catch(Exception ex){
                Logger logger = GuiUtil.getLogger();
                if (logger.isLoggable(Level.FINE)) {
                    ex.printStackTrace();
                }
                Map maskedAttr = RestUtil.maskOffPassword(attrs);
                GuiUtil.getLogger().log(
                    Level.SEVERE, "{0};\n{1};\n{2}", new Object[]{
                        ex.getMessage(),
                        ex.getCause(),
                        GuiUtil.getCommonMessage("LOG_REST_REQUEST_INFO",
                        new Object[]{endpoint, maskedAttr, method})});
                GuiUtil.handleError(handlerCtx, GuiUtil.getMessage("msg.error.checkLog"));
                return;
            }
        }
    }

    /**
     * Create or update
     */
    @Handler(id = "gf.updateEntity",
            input = {
                    @HandlerInput(name = "endpoint", type = String.class, required = true),
                    @HandlerInput(name = "attrs", type = Map.class, required = true),
                    @HandlerInput(name = "skipAttrs", type = List.class),
                    @HandlerInput(name = "onlyUseAttrs", type = List.class),
                    @HandlerInput(name = "convertToFalse", type = List.class)},
            output = {
                    @HandlerOutput(name = "result", type = String.class)
            })
    public static void updateEntity(HandlerContext handlerCtx) {
        Map<String, Object> attrs = (Map) handlerCtx.getInputValue("attrs");
        if (attrs == null) {
            attrs = new HashMap<String, Object>();
        }
        String endpoint = (String) handlerCtx.getInputValue("endpoint");

        RestResponse response = sendUpdateRequest(endpoint, attrs, (List) handlerCtx.getInputValue("skipAttrs"),
                (List) handlerCtx.getInputValue("onlyUseAttrs"), (List) handlerCtx.getInputValue("convertToFalse"));

        if (!response.isSuccess()) {
             GuiUtil.getLogger().log(
                Level.SEVERE,
                GuiUtil.getCommonMessage("LOG_UPDATE_ENTITY_FAILED", new Object[]{endpoint, attrs}));
            GuiUtil.handleError(handlerCtx, GuiUtil.getMessage("msg.error.checkLog"));
            return;
        }

        handlerCtx.setOutputValue("result", endpoint);
    }

    /**
     * // TODO: just these resources?
     * deleteCascade handles delete for jdbc connection pool and connector connection pool
     * The dependent resources jdbc resource and connector resource are deleted on deleting
     * the pools
     */
    @Handler(id = "gf.deleteCascade",
            input = {
                    @HandlerInput(name = "endpoint", type = String.class, required = true),
                    @HandlerInput(name = "selectedRows", type = List.class, required = true),
                    @HandlerInput(name = "id", type = String.class, defaultValue = "name"),
                    @HandlerInput(name = "cascade", type = String.class)
            })
    public static void deleteCascade(HandlerContext handlerCtx) {
        try {
            Map<String, Object> payload = new HashMap<String, Object>();
            String endpoint = (String) handlerCtx.getInputValue("endpoint");
            String id = (String) handlerCtx.getInputValue("id");
            String cascade = (String) handlerCtx.getInputValue("cascade");
            if (cascade != null) {
                payload.put("cascade", cascade);
            }

            for (Map oneRow : (List<Map>) handlerCtx.getInputValue("selectedRows")) {
                RestResponse response = delete(endpoint + "/" +
                        URLEncoder.encode((String) oneRow.get(id), "UTF-8"), payload);
                if (!response.isSuccess()) {
                    GuiUtil.handleError(handlerCtx, "Unable to delete the resource " + (String) oneRow.get(id));
                }
            }
        } catch (Exception ex) {
            GuiUtil.handleException(handlerCtx, ex);
        }
    }


    @Handler(id = "gf.deleteConfigCascade",
            input = {
                    @HandlerInput(name = "endpoint", type = String.class, required = true),
                    @HandlerInput(name = "selectedRows", type = List.class, required = true),
                    @HandlerInput(name = "id", type = String.class, defaultValue = "name"),
                    @HandlerInput(name = "target", type = String.class, required = true)
            })
    public static void deleteConfigCascade(HandlerContext handlerCtx) {

            List<Map> selectedRows = (List<Map>) handlerCtx.getInputValue("selectedRows");
            String id = (String) handlerCtx.getInputValue("id");
            String prefix = (String) handlerCtx.getInputValue("endpoint");

            Map<String, Object> payload = new HashMap<String, Object>();
            payload.put("target", (String) handlerCtx.getInputValue("target"));

            for ( Map oneRow : selectedRows) {
                try{
                    String endpoint = prefix + "/" + oneRow.get(id);
                    RestUtil.restRequest(endpoint, payload, "DELETE",null, false);
                }catch (Exception ex){
                    GuiUtil.getLogger().severe(
                            GuiUtil.getCommonMessage("LOG_NODE_ACTION_ERROR", new Object[]{prefix + "/" + oneRow.get(id), "DELETE" , "null"}));
                    GuiUtil.prepareAlert("error", GuiUtil.getMessage("msg.Error"), ex.getMessage());
                    return;
                }
                continue;
            }
    }

    /*
     * Return List<Map<String, String>> which is for displaying as table in a the page.
     * If a skipList is specified,  any child whose id is specified in the skipList will not be included.
     * If a includeList is specifed,  any child whose id is NOT specified in the includeList will NOT be included.
     */
    @Handler(id = "gf.getChildList",
        input = {
            @HandlerInput(name = "parentEndpoint", type = String.class, required = true),
            @HandlerInput(name = "childType", type = String.class, required = true),
            @HandlerInput(name = "skipList", type = List.class, required = false),
            @HandlerInput(name = "includeList", type = List.class, required = false),
            @HandlerInput(name = "id", type = String.class, defaultValue = "name")},
        output = {
            @HandlerOutput(name = "result", type = java.util.List.class)
    })
    public static void getChildList(HandlerContext handlerCtx) {
        try {
            handlerCtx.setOutputValue("result",
                    buildChildEntityList((String)handlerCtx.getInputValue("parentEndpoint"),
                    (String)handlerCtx.getInputValue("childType"),
                    (List)handlerCtx.getInputValue("skipList"),
                    (List)handlerCtx.getInputValue("includeList"),
                    (String)handlerCtx.getInputValue("id")));
        } catch (Exception ex) {
            GuiUtil.handleException(handlerCtx, ex);
        }
    }


    @Handler(id = "gf.getChildrenNameFromListCmd",
            input = {
                    @HandlerInput(name = "endpoint", type = String.class, required = true),
                    @HandlerInput(name = "attrs", type = Map.class),
                    @HandlerInput(name = "id", type = String.class, defaultValue = "message")},
            output = {
                    @HandlerOutput(name = "result", type = java.util.List.class)
            })
    public static void getChildrenNameFromListCmd(HandlerContext handlerCtx) {
        try {
            String endpoint = (String)handlerCtx.getInputValue("endpoint");
            Map attrs = (Map) handlerCtx.getInputValue("attrs");
            String id =  (String)handlerCtx.getInputValue("id");
            List result = new ArrayList();
            if (RestUtil.doesProxyExist(endpoint)) {
                Map responseMap = RestUtil.restRequest(endpoint, attrs, "get", null, false);
                Map data = (Map) responseMap.get("data");
                if (data != null) {
                    List<Map> children = (List<Map>) data.get("children");
                    if (children != null) {
                        for(Map oneChild : children){
                            result.add(oneChild.get(id));
                        }
                    }
                }
            }
            Collections.sort(result);
            handlerCtx.setOutputValue("result", result);
        } catch (Exception ex) {
            GuiUtil.handleException(handlerCtx, ex);
        }
    }

    @Handler(id = "gf.getChildrenNamesList",
        input = {
            @HandlerInput(name = "endpoint", type = String.class, required = true),
            @HandlerInput(name = "attrs", type = Map.class),
            @HandlerInput(name = "id", type = String.class, defaultValue = "name")},
        output = {
            @HandlerOutput(name = "result", type = java.util.List.class)
    })
    public static void getChildrenNamesList(HandlerContext handlerCtx) {
        try {
            List list = new ArrayList(getChildMap((String)handlerCtx.getInputValue("endpoint"), (Map)handlerCtx.getInputValue("attrs")).keySet());
            Collections.sort(list);
            handlerCtx.setOutputValue("result", list);
        } catch (Exception ex) {
            GuiUtil.handleException(handlerCtx, ex);
        }
    }


    @Handler(id = "gf.buildResourceUrl",
        input = {
            @HandlerInput(name = "base", type = String.class, required = true),
            @HandlerInput(name = "resourceName", type = String.class, required = true)
        },
        output = {
            @HandlerOutput(name = "url", type = String.class)
        }
    )
    public static void encodeUrl(HandlerContext handlerCtx) {
        handlerCtx.setOutputValue("url",
            appendEncodedSegment((String)handlerCtx.getInputValue("base"), (String)handlerCtx.getInputValue("resourceName")));
    }
}
