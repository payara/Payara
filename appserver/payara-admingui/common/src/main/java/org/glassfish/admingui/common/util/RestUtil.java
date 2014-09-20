/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.admingui.common.util;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

import javax.faces.context.FacesContext;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.xml.parsers.ParserConfigurationException;

import org.glassfish.jersey.client.filter.CsrfProtectionFilter;
import org.glassfish.jersey.jackson.JacksonFeature;

import org.glassfish.hk2.api.ServiceLocator;

import org.glassfish.admingui.common.security.AdminConsoleAuthModule;
import org.glassfish.api.ActionReport.ExitCode;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.sun.enterprise.config.serverbeans.SecureAdmin;
import com.sun.enterprise.security.SecurityServicesUtil;
import com.sun.enterprise.security.ssl.SSLUtils;
import com.sun.jsftemplating.layout.descriptors.handler.HandlerContext;

/**
 *
 * @author anilam
 */
public class RestUtil {

    public static final String FORM_ENCODING = "application/x-www-form-urlencoded";
    //default to .json instead of .xml
    public static final String RESPONSE_TYPE = "application/json";
    public static final String GUI_TOKEN_FOR_EMPTY_PROPERTY_VALUE = "()";
    public static final String REST_TOKEN_COOKIE = "gfresttoken";
    private static Client JERSEY_CLIENT;

    public static synchronized Client getJerseyClient() {
        if (JERSEY_CLIENT == null) {
            JERSEY_CLIENT = initialize(ClientBuilder.newBuilder()).build();
            JERSEY_CLIENT.register(new RequiredHeadersFilter())
                    .register(new JacksonFeature());
        }

        return JERSEY_CLIENT;
    }

    public static String getPropValue(String endpoint, String propName, HandlerContext handlerCtx) {
        Map responseMap = (Map) restRequest(endpoint + "/property.json", null, "GET", handlerCtx, false);
        Map extraPropertiesMap = (Map) ((Map) responseMap.get("data")).get("extraProperties");
        if (extraPropertiesMap != null) {
            List<Map> props = (List) extraPropertiesMap.get("properties");
            for (Map oneProp : props) {
                if (oneProp.get("name").equals(propName)) {
                    return (String) oneProp.get("value");
                }
            }
        }
        return "";
    }

    public static String resolveToken(String endpoint, String token) {
        String tokenStartMarker = "${", tokenEndMarker = "}";

        if (!token.trim().startsWith(tokenStartMarker)) {
            return token;
        }
        int start = token.indexOf(tokenStartMarker);
        if (start < 0) {
            return token;
        }
        int end = token.lastIndexOf(tokenEndMarker);
        if (end < 0) {
            return token;
        }

        Map attrMap = new HashMap();
        String str = token.substring(start + tokenStartMarker.length(), end - tokenEndMarker.length() + 1);
        attrMap.put("tokens", str);

        try {
            Map result = restRequest(endpoint + "/resolve-tokens.json", attrMap, "GET", null, true);
            return (String) GuiUtil.getMapValue(result, "data,extraProperties,tokens," + str);
        } catch (Exception ex) {
            GuiUtil.getLogger().info(ex.getMessage());
        }
        return token;
    }

    public static Map<String, Object> restRequest(String endpoint, Map<String, Object> attrs, String method, HandlerContext handlerCtx, boolean quiet) {
        return restRequest(endpoint, attrs, method, handlerCtx, quiet, true);
    }

    public static Map<String, Object> restRequest(String endpoint, Map<String, Object> attrs, String method, HandlerContext handlerCtx, boolean quiet, boolean throwException) {
        boolean useData = false;

        Object data = null;
        if (attrs == null) {
            try {
                data = (handlerCtx == null) ? null : handlerCtx.getInputValue("data");
            } catch (Exception e) {
                //
            }
            if (data != null) {
                // We'll send the raw data
                useData = true;
            } else {
                // Initialize the attributes to an empty map
                attrs = new HashMap<String, Object>();
            }
        }
        method = method.toLowerCase(new Locale("UTF-8"));

        Logger logger = GuiUtil.getLogger();
        if (logger.isLoggable(Level.FINEST)) {
            Map maskedAttr = maskOffPassword(attrs);
            logger.log(Level.FINEST,
                       GuiUtil.getCommonMessage("LOG_REST_REQUEST_INFO",
                                                new Object[]{
                        endpoint,
                        (useData && "post".equals(method)) ? data : attrs, method
                    }));
        }

        // Execute the request...
        RestResponse restResponse = null;
        if ("post".equals(method)) {
            if (useData) {
                restResponse = post(endpoint, data, (String) handlerCtx.getInputValue("contentType"));
            } else {
                restResponse = post(endpoint, attrs);
            }
        } else if ("put".equals(method)) {
            restResponse = put(endpoint, attrs);
        } else if ("get".equals(method)) {
            restResponse = get(endpoint, attrs);
        } else if ("delete".equals(method)) {
            restResponse = delete(endpoint, attrs);
        } else {
            throw new RuntimeException(GuiUtil.getCommonMessage("rest.invalid_method", new Object[]{method}));
        }

        // If the REST request returns a 401 (authz required), the REST "session"
        // has likely expired.  If the requested console URL is NOT the login page,
        // invalidate the session and force the the user to log back in.
        if (restResponse.getResponseCode() == 401) {
            FacesContext fc = FacesContext.getCurrentInstance();
            HttpSession session = (HttpSession) fc.getExternalContext().getSession(false);

            HttpServletRequest request = (HttpServletRequest) fc.getExternalContext().getRequest();
            HttpServletResponse response = (HttpServletResponse) fc.getExternalContext().getResponse();
            if (!"/login.jsf".equals(request.getServletPath())) {
                try {
                    response.sendRedirect("/");

                    fc.responseComplete();
                    session.invalidate();
                } catch (Exception ex) {
                    throw new RuntimeException(ex);
                }
            }
        }

        return parseResponse(restResponse, handlerCtx, endpoint, (useData && "post".equals(method)) ? data : attrs, quiet, throwException);
    }

//    public static <T> T restRequest(String endpoint,
//                                    Map<String, Object> attrs,
//                                    String method,
//                                    String contentType,
//                                    Object data,
//                                    boolean useData,
//                                    boolean quiet,
//                                    boolean throwException) {
//        method = method.toLowerCase(new Locale("UTF-8"));
//
//        Logger logger = GuiUtil.getLogger();
//        if (logger.isLoggable(Level.FINEST)) {
//            Map maskedAttr = maskOffPassword(attrs);
//            logger.log(Level.FINEST,
//                    GuiUtil.getCommonMessage("LOG_REST_REQUEST_INFO",
//                        new Object[]{
//                            endpoint,
//                            (useData && "post".equals(method))? data: attrs, method
//                        }));
//        }
//
//        // Execute the request...
//        RestResponse restResponse = null;
//        if ("post".equals(method)) {
//            if (useData) {
//                restResponse = post(endpoint, data, contentType);
//            } else {
//                restResponse = post(endpoint, attrs);
//            }
//        } else if ("put".equals(method)) {
//            restResponse = put(endpoint, attrs);
//        } else if ("get".equals(method)) {
//            restResponse = get(endpoint, attrs);
//        } else if ("delete".equals(method)) {
//            restResponse = delete(endpoint, attrs);
//        } else {
//            throw new RuntimeException(GuiUtil.getCommonMessage("rest.invalid_method", new Object[]{method}));
//        }
//
//        // If the REST request returns a 401 (authz required), the REST "session"
//        // has likely expired.  If the requested console URL is NOT the login page,
//        // invalidate the session and force the the user to log back in.
//        if (restResponse.getResponseCode() == 401) {
//            FacesContext fc = FacesContext.getCurrentInstance();
//            HttpSession session = (HttpSession)fc.getExternalContext().getSession(false);
//
//            HttpServletRequest request = (HttpServletRequest)fc.getExternalContext().getRequest();
//            HttpServletResponse response = (HttpServletResponse)fc.getExternalContext().getResponse();
//            if (!"/login.jsf".equals(request.getServletPath())) {
//                try {
//                    response.sendRedirect("/");
//
//                    fc.responseComplete();
//                    initialize(null);
//                    session.invalidate();
//                } catch (Exception ex) {
//                    throw new RuntimeException(ex);
//                }
//            }
//        }
//
//        return restResponse.;
//    }
    public static Map maskOffPassword(Map<String, Object> attrs) {
        Map masked = new HashMap();
        if (attrs == null) {
            return masked;
        }

        for (Map.Entry<String, Object> e : attrs.entrySet()) {
            String key = e.getKey().toLowerCase(GuiUtil.guiLocale);
            if (pswdAttrList.contains(key)) {
                masked.put(e.getKey(), "*******");
            } else {
                masked.put(e.getKey(), e.getValue());
            }
        }
        return masked;
    }

    public static Map<String, String> buildDefaultValueMap(String endpoint) throws ParserConfigurationException, SAXException, IOException {
        Map<String, String> defaultValues = new HashMap<String, String>();

        RestResponse response = options(endpoint, "application/json");
        //System.out.println("=========== response.getResponse():\n " + response.getResponse());
        Map<String, Object> data = (Map<String, Object>) response.getResponse().get("data");
        List<Map<String, Object>> methods = null;
        Map<String, Object> extraProperties = (Map<String, Object>) data.get("extraProperties");
        methods = (List<Map<String, Object>>) extraProperties.get("methods");
        for (Map<String, Object> method : methods) {
            if ("POST".equals(method.get("name"))) {
                Map<String, Object> messageParameters = (Map<String, Object>) method.get("messageParameters");
                if (messageParameters != null) {
                    for (Map.Entry<String, Object> entry : messageParameters.entrySet()) {
                        String param = entry.getKey();
                        String defaultValue = (String) ((Map) entry.getValue()).get("defaultValue");
                        if (!"".equals(defaultValue) && (defaultValue != null)) { // null test necessary?
                            defaultValues.put(param, defaultValue);
                        }
                    }
                }
            }
        }
        return defaultValues;
    }

    public static Map getAttributesMap(String endpoint) {
        RestResponse response = null;
        try {
            response = get(endpoint);
            if (!response.isSuccess()) {
                return new HashMap();
            }
            return getEntityAttrs(endpoint, "entity");
        } finally {
            if (response != null) {
                response.close();
            }
        }
    }

    public static Map<String, Object> getEntityAttrs(String endpoint, String key) {
        Map<String, Object> valueMap = new HashMap<String, Object>();
        try {
            // Use restRequest to query the endpoint
            Map<String, Object> result = restRequest(endpoint, (Map<String, Object>) null, "get", null, false);
            int responseCode = (Integer) result.get("responseCode");
            if ((responseCode < 200) || (responseCode > 299)) {
                throw new RuntimeException((String) result.get("responseBody"));
            }

            // Pull off the attribute Map
            Map<String, Object> data = (Map<String, Object>) result.get("data");
            Map<String, Object> extraProperties = (Map<String, Object>) data.get("extraProperties");
            if (extraProperties.containsKey(key)) {
                valueMap = (Map<String, Object>) extraProperties.get(key);
            }
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }

        return valueMap;
    }

    private static String getMessage(Map aMap) {
        String message = "";
        if (aMap != null) {
            message = (String) aMap.get("message");
        }
        return (message == null) ? "" : message;
    }

    public static Map<String, Object> parseResponse(RestResponse response, HandlerContext handlerCtx, String endpoint,
                                                    Object attrs, boolean quiet, boolean throwException) {
        // Parse the response
        String message = "";
        ExitCode exitCode = ExitCode.FAILURE;
        Object maskedAttr = attrs;
        if ((attrs != null) && (attrs instanceof Map)) {
            maskedAttr = maskOffPassword((Map<String, Object>) attrs);
        }
        if (response != null) {
            try {
                //int status = response.getResponseCode();
                Map responseMap = response.getResponse();
                if (responseMap.get("data") != null) {
                    String exitCodeStr = (String) ((Map) responseMap.get("data")).get("exit_code");
                    exitCode = (exitCodeStr != null) ? ExitCode.valueOf(exitCodeStr) : ExitCode.SUCCESS;
                }

                //Get the message for both WARNING and FAILURE exit_code
                if (exitCode != ExitCode.SUCCESS) {
                    Map dataMap = (Map) responseMap.get("data");
                    if (dataMap != null) {
                        message = getMessage(dataMap);
                        List<Map> subReports = (List<Map>) dataMap.get("subReports");
                        if (subReports != null) {
                            StringBuilder sb = new StringBuilder("");
                            for (Map oneSubReport : subReports) {
                                sb.append(" ").append(getMessage(oneSubReport));
                            }
                            message = message + sb.toString();
                        }
                    } else {
                        Object msgs = responseMap.get("message");
                        if (msgs == null) {
                            //According to security guideline, we shouldn't expose the endpoint to user for the error.
                            //message =  "REST Request '"  + endpoint + "' failed with response code '" + status + "'.";
                            message = "";
                        } else if (msgs instanceof List) {
                            StringBuilder builder = new StringBuilder("");
                            for (Object obj : ((List<Object>) msgs)) {
                                if ((obj instanceof Map) && ((Map<String, Object>) obj).containsKey("message")) {
                                    obj = ((Map<String, Object>) obj).get("message");
                                }
                                builder.append(obj.toString());
                            }
                            message = builder.toString();
                        } else if (msgs instanceof Map) {
                            message = ((Map<String, Object>) msgs).get("message").toString();
                        } else {
                            throw new RuntimeException("Unexpected message type.");
                        }
                    }
                }
                switch (exitCode) {
                    case FAILURE: {
                        // If this is called from jsf, stop processing/show error.
                        if (throwException) {
                            if (handlerCtx != null) {
                                GuiUtil.handleError(handlerCtx, message);
                                if (!quiet) {
				    Logger logger = GuiUtil.getLogger();
                                    logger.severe(GuiUtil.getCommonMessage("LOG_REQUEST_RESULT", new Object[]{exitCode, endpoint, maskedAttr}));
				    if (logger.isLoggable(Level.FINEST)){
                                    	logger.finest("response.getResponseBody(): " + response.getResponseBody());
				    }
                                }
                                return new HashMap();
                            } else {
                                //If handlerCtx is not passed in, it means the caller (java handler) wants to handle this exception itself.
                                throw new RuntimeException(message);
                            }
                        } else { // Issue Number :13312 handling the case when throwException is false.
                            if (!quiet) {
				Logger logger = GuiUtil.getLogger();
                                logger.severe(GuiUtil.getCommonMessage("LOG_REQUEST_RESULT", new Object[]{exitCode, endpoint, maskedAttr}));
				if (logger.isLoggable(Level.FINEST)){
                                	logger.finest("response.getResponseBody(): " + response.getResponseBody());
				}
                            }
                            return responseMap;
                        }
                    }
                    case WARNING: {
                        GuiUtil.prepareAlert("warning", GuiUtil.getCommonMessage("msg.command.warning"), message);
                        GuiUtil.getLogger().warning(GuiUtil.getCommonMessage("LOG_REQUEST_RESULT", new Object[]{exitCode, endpoint, maskedAttr}));
                        return responseMap;
                    }
                    case SUCCESS: {
                        return responseMap;
                    }
                }
            } catch (Exception ex) {
                if (!quiet) {
		    Logger logger = GuiUtil.getLogger();
                    logger.severe(GuiUtil.getCommonMessage("LOG_REQUEST_RESULT", new Object[]{exitCode, endpoint, maskedAttr}));
		    if (logger.isLoggable(Level.FINEST)){
                        logger.finest("response.getResponseBody(): " + response.getResponseBody());
		    }
                }
                if (handlerCtx != null) {
                    //If this is called from the jsf as handler, we want to stop processing and show error
                    //instead of dumping the exception on screen.
                    if (throwException) {
                        if ("".equals(message)) {
                            GuiUtil.handleException(handlerCtx, ex);
                        } else {
                            GuiUtil.handleError(handlerCtx, message);
                        }
                    }
                } else {
                    //if this is called by other java handler, we tell the called handle the exception.
                    if ("".equals(message)) {
                        throw new RuntimeException(ex);
                    } else {
                        throw new RuntimeException(message, ex);
                    }
                }
            }
        }
        return null;
    }

    public static boolean hasWarning(Map responseMap) {
        if (responseMap.get("data") != null) {
            String exitCodeStr = (String) ((Map) responseMap.get("data")).get("exit_code");
            ExitCode exitCode = (exitCodeStr != null) ? ExitCode.valueOf(exitCodeStr) : ExitCode.SUCCESS;
            return (exitCode == ExitCode.WARNING);
        }
        return false;
    }

    /**
     * This method will encode append segment to base, encoding it so that a correct URL is returned.
     *
     * @param base
     * @param segment
     * @return
     */
    public static String appendEncodedSegment(String base, String segment) {
        String encodedUrl = getJerseyClient().target(base).getUriBuilder().segment(segment).build().toASCIIString();
        //segment(elementName)

        return encodedUrl;
    }

    //*******************************************************************************************************************
    //*******************************************************************************************************************
    protected static MultivaluedMap buildMultivalueMap(Map<String, Object> payload) {
        MultivaluedMap formData = new MultivaluedHashMap();
        if (payload == null || payload.isEmpty()) {
            return formData;
        }

        for (final Map.Entry<String, Object> entry : payload.entrySet()) {
            final Object value = entry.getValue();
            final String key = entry.getKey();
            if (value instanceof Collection) {
                for (Object obj : ((Collection) value)) {
                    try {
                        formData.add(key, obj);
                    } catch (ClassCastException ex) {
                        Logger logger = GuiUtil.getLogger();
                        if (logger.isLoggable(Level.FINEST)) {
                            logger.log(Level.FINEST,
                                       GuiUtil.getCommonMessage("LOG_BUILD_MULTI_VALUE_MAP_ERROR", new Object[]{key, obj}));
                        }

                        // Allow it to continue b/c this property most likely
                        // should have been excluded for this request
                    }
                }
            } else {
                //formData.putSingle(key, (value != null) ? value.toString() : value);
                try {
                    if (key.equals("availabilityEnabled")) {
                        System.out.println("================== availabilityEnabled  skipped ");
                    }
                    formData.putSingle(key, value);
                } catch (ClassCastException ex) {
                    Logger logger = GuiUtil.getLogger();
                    if (logger.isLoggable(Level.FINEST)) {
                        logger.log(Level.FINEST,
                                   GuiUtil.getCommonMessage("LOG_BUILD_MULTI_VALUE_MAP_ERROR", new Object[]{key, value}));
                    }
                    // Allow it to continue b/c this property most likely
                    // should have been excluded for this request
                }
            }
        }
        return formData;
    }

    public static RestResponse sendCreateRequest(String endpoint, Map<String, Object> attrs, List<String> skipAttrs, List<String> onlyUseAttrs, List<String> convertToFalse) {
        removeSpecifiedAttrs(attrs, skipAttrs);
        attrs = buildUseOnlyAttrMap(attrs, onlyUseAttrs);
        attrs = convertNullValuesToFalse(attrs, convertToFalse);
        attrs = fixKeyNames(attrs);

        return post(endpoint, attrs);
    }

    // This will send an update request.  Currently, this calls post just like the create does,
    // but the REST API will be modified to use PUT for updates, a more correct use of HTTP
    public static RestResponse sendUpdateRequest(String endpoint, Map<String, Object> attrs, List<String> skipAttrs, List<String> onlyUseAttrs, List<String> convertToFalse) {
        removeSpecifiedAttrs(attrs, skipAttrs);
        attrs = buildUseOnlyAttrMap(attrs, onlyUseAttrs);
        attrs = convertNullValuesToFalse(attrs, convertToFalse);
        attrs = fixKeyNames(attrs);

        return post(endpoint, attrs);
    }

    protected static Map<String, Object> fixKeyNames(Map<String, Object> map) {
        Map<String, Object> results = new HashMap<String, Object>();
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            String key = entry.getKey().substring(0, 1).toLowerCase(GuiUtil.guiLocale) + entry.getKey().substring(1);
            Object value = entry.getValue();
            results.put(key, value);
        }

        return results;
    }

    protected static void removeSpecifiedAttrs(Map<String, Object> attrs, List<String> removeList) {
        if (removeList == null || removeList.size() <= 0) {
            return;
        }
        Set<Map.Entry<String, Object>> attrSet = attrs.entrySet();
        Iterator<Map.Entry<String, Object>> iter = attrSet.iterator();
        while (iter.hasNext()) {
            Map.Entry<String, Object> oneEntry = iter.next();
            if (removeList.contains(oneEntry.getKey())) {
                iter.remove();
            }
        }
    }

    protected static Map buildUseOnlyAttrMap(Map<String, Object> attrs, List<String> onlyUseAttrs) {
        if (onlyUseAttrs != null) {
            Map newAttrs = new HashMap();
            for (String key : onlyUseAttrs) {
                if (attrs.keySet().contains(key)) {
                    newAttrs.put(key, attrs.get(key));
                }
            }
            return newAttrs;
        } else {
            return attrs;
        }

    }

    // This is ugly, but I'm trying to figure out why the cleaner code doesn't work :(
    protected static Map<String, Object> convertNullValuesToFalse(Map<String, Object> attrs, List<String> convertToFalse) {
        if (convertToFalse != null) {
            Map<String, Object> newAttrs = new HashMap<String, Object>();
            String key;

            for (Map.Entry<String, Object> entry : attrs.entrySet()) {
                key = entry.getKey();
                if (convertToFalse.contains(key) && ((entry.getValue() == null) || "null".equals(entry.getValue()))) {
                    newAttrs.put(key, "false");
                } else {
                    newAttrs.put(key, entry.getValue());
                }
            }
            return newAttrs;
        } else {
            return attrs;
        }
    }

    /**
     * Converts the first letter of the given string to Uppercase.
     *
     * @param string the input string
     * @return the string with the Uppercase first letter
     */
    public static String upperCaseFirstLetter(String string) {
        if (string == null || string.length() <= 0) {
            return string;
        }
        return string.substring(0, 1).toUpperCase(new Locale("UTF-8")) + string.substring(1);
    }

    public static List<String> getChildResourceList(String document) throws SAXException, IOException, ParserConfigurationException {
        List<String> children = new ArrayList<String>();
        Document doc = MiscUtil.getDocument(document);
        Element root = doc.getDocumentElement();
        NodeList nl = root.getElementsByTagName("childResource");
        if (nl.getLength() > 0) {
            for (int i = 0; i < nl.getLength(); i++) {
                Node child = nl.item(i);
                if (child.getNodeType() == Node.ELEMENT_NODE) {
                    children.add(child.getTextContent());
                }
            }
        }

        return children;
    }

    /**
     * Given the parent URL and the desired childType, this method will build a List of Maps that contains each child
     * entities values. In addition to the entity values, each row will have a field, 'selected', set to false, as well
     * as the URL encoded entity name ('encodedName').
     *
     * @param parent
     * @param childType
     * @param skipList
     * @return
     * @throws Exception
     */
    public static List<Map> buildChildEntityList(String parent, String childType, List skipList, List includeList, String id) throws Exception {

        String endpoint = parent.endsWith("/") ? parent + childType : parent + "/" + childType;
        boolean hasSkip = (skipList != null);
        boolean hasInclude = (includeList != null);
        boolean convert = childType.equals("property");

        List<Map> childElements = new ArrayList<Map>();
        try {
            List<String> childUrls = getChildList(endpoint);
            for (String childUrl : childUrls) {
                Map<String, Object> entity = getEntityAttrs(childUrl, "entity");
                HashMap<String, Object> oneRow = new HashMap<String, Object>();

                if (hasSkip && skipList.contains(entity.get(id))) {
                    continue;
                }

                if (hasInclude && (!includeList.contains(entity.get(id)))) {
                    continue;
                }

                oneRow.put("selected", false);
                for (Map.Entry<String, Object> e : entity.entrySet()) {
                    oneRow.put(e.getKey(), getA(entity, e.getKey(), convert));
                }
                oneRow.put("encodedName", URLEncoder.encode(entity.get(id).toString(), "UTF-8"));
                oneRow.put("name", entity.get(id));
                childElements.add(oneRow);
            }
        } catch (Exception e) {
            throw e;
        }
        return childElements;
    }

    private static String getA(Map<String, Object> attrs, String key, boolean convert) {
        Object val = attrs.get(key);
        if (val == null) {
            return "";
        }
        return (convert && (val.equals(""))) ? GUI_TOKEN_FOR_EMPTY_PROPERTY_VALUE : val.toString();
    }

    /**
     * Given the parent URL and the desired childType, this method will build a List of Strings that contains child
     * entity names.
     *
     * @param endpoint
     * @return
     * @throws Exception
     */
    public static List<String> getChildList(String endpoint) throws Exception {
        List<String> childElements = new ArrayList<String>();
        Map<String, String> childResources = getChildMap(endpoint);
        childElements.addAll(childResources.values());
        Collections.sort(childElements);
        return childElements;
    }

    public static Map<String, String> getChildMap(String endpoint) throws Exception {
        return getChildMap(endpoint, null);
    }


    public static Map<String, String> getChildMap(String endpoint, Map attrs) throws Exception {
        Map<String, String> childElements = new TreeMap<String, String>();
        if (attrs == null){
            attrs = new HashMap<String, Object>();
        }
        if (doesProxyExist(endpoint)) {
            Map responseMap = restRequest(endpoint, attrs, "get", null, false);
            Map data = (Map) responseMap.get("data");
            if (data != null) {
                Map extraProperties = (Map) data.get("extraProperties");
                if (extraProperties != null) {
                    childElements = (Map<String, String>) extraProperties.get("childResources");
                    if (childElements == null) {
                        childElements = new TreeMap<String, String>();
                    }
                }
            }
        }

        return childElements;
    }

    public static Boolean doesProxyExist(String endpoint) {
        RestResponse response = null;
        try {
            response = RestUtil.get(endpoint);
            if (response.isSuccess()) {
                return true;
            }
        } catch (Exception e) {
            return false;
        } finally {
            if (response != null) {
                response.close();
            }
        }
        return false;
    }

    /**
     * <p> This method returns the value of the REST token if it is successfully set in session scope.</p>
     */
    public static String getRestToken() {
        String token = null;
        FacesContext ctx = FacesContext.getCurrentInstance();
        if (ctx != null) {
            token = (String) ctx.getExternalContext().getSessionMap().
                    get(AdminConsoleAuthModule.REST_TOKEN);
        }
        return token;
    }

    public static Response getRequestFromServlet(HttpServletRequest request, String endpoint, Map<String, Object> attrs) {
        String token = (String) request.getSession().getAttribute(AdminConsoleAuthModule.REST_TOKEN);
        WebTarget target = targetWithQueryParams(JERSEY_CLIENT.target(endpoint), attrs);
        Response cr = target
                .request().cookie(new Cookie(REST_TOKEN_COOKIE, token))
                .get(Response.class);

        return cr;
    }

    public static void getRestRequestFromServlet(HttpServletRequest request, String endpoint, Map<String, Object> attrs, boolean quiet, boolean throwException) {
        String token = (String) request.getSession().getAttribute(AdminConsoleAuthModule.REST_TOKEN);
        WebTarget target = targetWithQueryParams(JERSEY_CLIENT.target(endpoint), attrs);
        Response cr = target
                .request(RESPONSE_TYPE)
                .cookie(new Cookie(REST_TOKEN_COOKIE, token))
                .get(Response.class);
        RestResponse rr = RestResponse.getRestResponse(cr);
        parseResponse(rr, null, endpoint, attrs, quiet, throwException);

    }

    public static WebTarget targetWithQueryParams(WebTarget target, Map<String, Object> paramMap) {
        for (Map.Entry<String, Object> param : paramMap.entrySet()) {
            target = target.queryParam(param.getKey(), param.getValue());
        }
        return target;
    }

    //******************************************************************************************************************
    // Jersey client methods
    //******************************************************************************************************************
    public static RestResponse get(String address) {
        return get(address, new HashMap<String, Object>());
    }

    public static RestResponse get(String address, Map<String, Object> payload) {
        if (address.startsWith("/")) {
            address = FacesContext.getCurrentInstance().getExternalContext().getSessionMap().get("REST_URL") + address;
        }
        WebTarget target = targetWithQueryParams(getJerseyClient().target(address), payload);
        Response resp = target
                .request(RESPONSE_TYPE)
                .cookie(new Cookie(REST_TOKEN_COOKIE, getRestToken()))
                .get(Response.class);
        return RestResponse.getRestResponse(resp);
    }

    public static RestResponse post(String address, Object payload, String contentType) {
        WebTarget target = getJerseyClient().target(address);
        if (contentType == null) {
            contentType = MediaType.APPLICATION_JSON;
        }
        if (payload instanceof Map) {
            payload = buildMultivalueMap((Map<String, Object>) payload);
        }
        Response cr = target.request(RESPONSE_TYPE).header("Content-Type", contentType)
                .cookie(new Cookie(REST_TOKEN_COOKIE, getRestToken()))
                //                .header("Content-type", MediaType.APPLICATION_FORM_URLENCODED)
                .post(Entity.entity(payload, contentType), Response.class);
        RestResponse rr = RestResponse.getRestResponse(cr);
        return rr;
    }

    public static RestResponse  post(String address, Map<String, Object> payload) {
        WebTarget target = getJerseyClient().target(address);
        MultivaluedMap formData = buildMultivalueMap(payload);
        Response cr = target
                //                .header("Content-type", MediaType.APPLICATION_FORM_URLENCODED)
                .request(RESPONSE_TYPE)
                .cookie(new Cookie(REST_TOKEN_COOKIE, getRestToken()))
                .post(Entity.entity(formData, MediaType.APPLICATION_FORM_URLENCODED), Response.class);
        RestResponse rr = RestResponse.getRestResponse(cr);
        return rr;
    }

    public static RestResponse put(String address, Map<String, Object> payload) {
        WebTarget target = getJerseyClient().target(address);
        MultivaluedMap formData = buildMultivalueMap(payload);

        Response cr = target
                //                .header("Content-type", MediaType.APPLICATION_FORM_URLENCODED)
                .request(RESPONSE_TYPE)
                .cookie(new Cookie(REST_TOKEN_COOKIE, getRestToken()))
                .put(Entity.entity(formData, MediaType.APPLICATION_FORM_URLENCODED), Response.class);
        RestResponse rr = RestResponse.getRestResponse(cr);
        return rr;
    }

    public static RestResponse delete(String address, Map<String, Object> payload) {
        WebTarget target = getJerseyClient().target(address);
        Response cr = targetWithQueryParams(target, payload)
                .request(RESPONSE_TYPE)
                .cookie(new Cookie(REST_TOKEN_COOKIE, getRestToken()))
                .delete(Response.class);
        return RestResponse.getRestResponse(cr);
    }

    public static RestResponse options(String address, String responseType) {
        WebTarget target = getJerseyClient().target(address);
        Response cr = target
                .request(responseType)
                .cookie(new Cookie(REST_TOKEN_COOKIE, getRestToken()))
                .options(Response.class);
        return RestResponse.getRestResponse(cr);
    }

    public static void checkStatusForSuccess(Response cr) {
        int status = cr.getStatus();
        if ((status < 200) || (status > 299)) {
            throw new RuntimeException(cr.toString());
        }
    }
    //******************************************************************************************************************
    // Jersey client methods
    //******************************************************************************************************************

    public static ClientBuilder initialize(ClientBuilder clientBuilder) {
        try {
            ServiceLocator habitat = SecurityServicesUtil.getInstance().getHabitat();
            SecureAdmin secureAdmin = habitat.getService(SecureAdmin.class);

            final SSLContext sslContext = habitat
                    .<SSLUtils>getService(SSLUtils.class)
                    .getAdminSSLContext(SecureAdmin.Util.DASAlias(secureAdmin), null);

            // Instruct Jersey to use HostNameVerifier and SSLContext provided by us.
            clientBuilder
                    .hostnameVerifier(new BasicHostnameVerifier())
                    .sslContext(sslContext)
                    .register(CsrfProtectionFilter.class);

        } catch (Exception ex) {
            GuiUtil.getLogger().warning("RestUtil.initialize() failed");
            if (GuiUtil.getLogger().isLoggable(Level.FINE)) {
                ex.printStackTrace();
            }
        }
        return clientBuilder;
    }

    public static void postRestRequestFromServlet(HttpServletRequest request, String endpoint, Map<String, Object> attrs, boolean quiet, boolean throwException) {
        String token = (String) request.getSession().getAttribute(AdminConsoleAuthModule.REST_TOKEN);
        WebTarget target = JERSEY_CLIENT.target(endpoint);
        MultivaluedMap formData = buildMultivalueMap(attrs);
        Response cr = target
                .request(RESPONSE_TYPE)
                .cookie(new Cookie(REST_TOKEN_COOKIE, token))
                .post(Entity.entity(formData, MediaType.APPLICATION_FORM_URLENCODED), Response.class);
        RestResponse rr = RestResponse.getRestResponse(cr);
        parseResponse(rr, null, endpoint, attrs, quiet, throwException);
    }

    private static class RequiredHeadersFilter implements ClientRequestFilter {
        @Override
        public void filter(ClientRequestContext requestContext) throws IOException {
            requestContext.getHeaders().add("X-GlassFish-3", "true");
        }

    }

    private static class BasicHostnameVerifier implements HostnameVerifier {

        final HostnameVerifier defaultVerifier = javax.net.ssl.HttpsURLConnection.getDefaultHostnameVerifier();

        public BasicHostnameVerifier() {
        }

        public boolean verify(String host, SSLSession sslSession) {
            if (host.equals("localhost")) {
                return true;
            }
            boolean result = defaultVerifier.verify(host, sslSession);
            return (result) ? true : host.equals(sslSession.getPeerHost());
        }
    }

    /* This is a list of attribute name of password for different command.
     * We need to mask its value during logging.
     */
    private static final List<String> pswdAttrList =
            Arrays.asList(
            "sshpassword", /* create-node-ssh , setup-ssh , update-node, update-node-ssh */
            "windowspassword", /* create-node-dcom, validate-dcom, update-node-dcom,  */
            "dbpassword", /* jms-availability-service */
            "jmsdbpassword", /* configure-jms-cluster */
            "password", /* change-admin-password */
            "newpassword", /* change-admin-password */
            "jmsdbpassword", /* configure-jms-cluster */
            "mappedpassword", /* create-connector-security-map, update-connector-security-map */
            "userpassword", /* create-file-user , update-file-user */
            "aliaspassword" /* create-password-alias , update-password-alias */);
}
