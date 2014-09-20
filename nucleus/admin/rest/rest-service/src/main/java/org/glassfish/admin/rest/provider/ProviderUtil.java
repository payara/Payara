/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.appserv.server.util.Version;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.glassfish.admin.rest.Constants;
import org.glassfish.admin.rest.utils.ResourceUtil;
import org.glassfish.admin.rest.utils.Util;
import org.glassfish.admin.rest.results.OptionsResult;
import org.glassfish.external.statistics.Statistic;
import org.glassfish.external.statistics.impl.StatisticImpl;
import org.jvnet.hk2.config.ConfigBean;


/**
 *
 * @author Pajeshwar Patil
 * @author Ludovic Champenois ludo@dev.java.net
 */
public class ProviderUtil {
    public static final String KEY_CHILD_RESOURCE = "childResource";
    public static final String KEY_CHILD_RESOURCES = "childResources";
    public static final String KEY_COMMAND = "command";
    public static final String KEY_COMMANDS = "commands";
    public static final String KEY_ENTITY = "entity";
    public static final String KEY_METHODS = "methods";

    /**
     * Produce a string in double quotes with backslash sequences in all the
     * right places.
     */
    public static String quote(String string) {
        if (string == null || string.length() == 0) {
            return "\"\"";
        }

        char b;
        char c = 0;
        int i;
        int len = string.length();
        StringBuilder sb = new StringBuilder(len + 4);
        String t;

        sb.append('"');
        for (i = 0; i < len; i += 1) {
            b = c;
            c = string.charAt(i);
            switch (c) {
                case '\\':
                case '"':
                    sb.append('\\');
                    sb.append(c);
                    break;
                case '/':
                    if (b == '<') {
                        sb.append('\\');
                    }
                    sb.append(c);
                    break;
                case '\b':
                    sb.append("\\b");
                    break;
                case '\t':
                    sb.append("\\t");
                    break;
                case '\n':
                    sb.append("\\n");
                    break;
                case '\f':
                    sb.append("\\f");
                    break;
                case '\r':
                    sb.append("\\r");
                    break;
                default:
                    if (c < ' ') {
                        t = "000" + Integer.toHexString(c);
                        sb.append("\\u").append(t.substring(t.length() - 4));
                    } else {
                        sb.append(c);
                    }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    protected static String slashToDash(String string) {
        if (string != null && !string.isEmpty()) {
            return string.replaceAll("/", "-");
        } else {
            return string;
        }
    }

    protected static String readAsString(InputStream in) throws IOException {
        Reader reader = new InputStreamReader(in);
        StringBuilder sb = new StringBuilder();
        char[] c = new char[1024];
        int l;
        while ((l = reader.read(c)) != -1) {
            sb.append(c, 0, l);
        }
        return sb.toString();
    }

    static public String getElementLink(UriInfo uriInfo, String elementName) {
        return uriInfo.getRequestUriBuilder().segment(elementName).build().toASCIIString();

    }

    static protected String getStartXmlElement(String name) {
        assert((name != null) && name.length() > 0);
        String result ="<";
        result = result + name;
        result = result + ">";
        return result;
    }

    static protected String getEndXmlElement(String name) {
        assert((name != null) && name.length() > 0);
        String result ="<";
        result = result + "/";
        result = result + name;
        result = result + ">";
        return result;
    }

    static public Map getStatistics(Statistic statistic) throws
            IllegalAccessException, IllegalArgumentException, InvocationTargetException {
        HashMap results = new HashMap();
        Class classObject = statistic.getClass();
        Method[] methods = 	classObject.getMethods();
        for (Method method: methods) {
             int modifier = method.getModifiers();
             //consider only the public methods
             if (Modifier.isPublic(modifier)) {
                 String name = method.getName();
                 //considier only the get* methods
                 if (name.startsWith("get")) {
                     name = name.substring("get".length());
                     Class<?> returnType = method.getReturnType();
                     //consider only the methods that return primitives or String objects)
                     if (returnType.isPrimitive() || returnType.getName().equals("java.lang.String")) {
                         results.put(name, method.invoke(statistic, null));
                     } else {
                         //control should never reach here
                         //we do not expect statistic object to return object
                         //as value for any of it's stats.
                     }
                 }
             }
        }
        return results;
    }

    static public Map<String, Object> getStatistic(Statistic statistic) {
        Map<String,Object> statsMap;
        // Most likely we will get the proxy of the StatisticImpl,
        // reconvert that so you can access getStatisticAsMap method
        if (Proxy.isProxyClass(statistic.getClass())) {
                statsMap = ((StatisticImpl)Proxy.getInvocationHandler(statistic)).getStaticAsMap();
        } else {
            statsMap = ((StatisticImpl)statistic).getStaticAsMap();
        }

        return  statsMap;
    }

    static public HashMap<String, String> getStringMap(Map<String, Object> map) {
        HashMap<String, String> stringMap = new HashMap<String, String>();
        if (map != null) {
            //Convert attribute value to String if that's not the case.
            //Attribute value can be Boolean, Interger etc.
            String key = null;
            Object value = null;
            //We know keys in the map are always stored as String objects
            for (Map.Entry<String, Object> entry : map.entrySet()) {
                stringMap.put(entry.getKey(), entry.getValue().toString());
            }
            /*
            Iterator<String> iterator = map.keySet().iterator();
            while (iterator.hasNext()) {
                key = iterator.next();
                value = map.get(key);
                stringMap.put(key, value.toString());
            }
            */
        }
        return stringMap;
    }

    static protected String getHtmlRepresentationForAttributes(ConfigBean proxy, UriInfo uriInfo) {
        StringBuilder result = new StringBuilder();

        MethodMetaData methodMetaData = ResourceUtil.getMethodMetaData(proxy.model);

        Set<String> parameters = methodMetaData.parameters();
        Iterator<String> iterator = parameters.iterator();
        ParameterMetaData parameterMetaData;
        String parameter;

        while (iterator.hasNext()) {
            parameter = iterator.next();
            parameterMetaData = methodMetaData.getParameterMetaData(parameter);
            //parameterMetaData contains attributeNames in camelCasedNames convert them to xmlNames to get the attribute's current value
            String xmlAttributeName = ResourceUtil.convertToXMLName(parameter);
            result.append(getHtmlRespresentationForParameter(parameter, parameterMetaData,
                    proxy.attribute(xmlAttributeName)));
        }

        if (result.length() > 0) {
            return "<div><form action=\"" + uriInfo.getAbsolutePath().toString() +
                    "\" method=\"post\"><dl>" + result.toString() +
                    "<dt class=\"button\"></dt><dd class=\"button\"><input value=\"Update\" type=\"submit\"></dd>" +
                    "</dl></form></div>";
        } else {
            return "";
        }
    }

    static protected String getHtmlRespresentationsForCommand(MethodMetaData methodMetaData, String commandMethod,
                                                              String commandDisplayName, UriInfo uriInfo) {
        StringBuilder result = new StringBuilder();
        if (methodMetaData != null) {
            Set<String> parameters = methodMetaData.parameters();
            Iterator<String> iterator = parameters.iterator();
            String parameter;
            ParameterMetaData parameterMetaData;
            while (iterator.hasNext()) {
                parameter = iterator.next();
                parameterMetaData = methodMetaData.getParameterMetaData(parameter);
                if ((methodMetaData.isFileUploadOperation()) && (parameter.equals("id"))) {
                    parameterMetaData.setIsFileParameter(true);
                }
                result.append(getHtmlRespresentationForParameter(parameter, parameterMetaData));
            }

            //Fix to diplay component for commands with 0 arguments.
            //For example, rotate-log or restart.
            if (result.length() == 0) {
                result.append(" ");
            }

        }
        //hack-1 : support delete method for html
        //hardcode "post" instead of commandMethod which should be post or delete.
        String webMethod="post";
        if (commandMethod.equalsIgnoreCase("get")){
             webMethod="get";
        }
        if (result.length() != 0) {
            //set encType if file upload operation
            String encType = methodMetaData.isFileUploadOperation() ?
                " enctype=\"multipart/form-data\"" : "" ;
            result = new StringBuilder("<div><form action=\"")
                    .append(uriInfo.getAbsolutePath().toString())
                    .append("\" method=\"")
                    .append(webMethod)
                    .append("\"")
                    .append(encType)
                    .append(">")
                    .append("<dl>")
                    .append(result);

            //hack-1 : support delete method for html
            //add hidden field
            if(commandMethod.equalsIgnoreCase("DELETE")) {
                result.append("<dd><input name=\"operation\" value=\"__deleteoperation\" type=\"hidden\"></dd>");
            }
            //hack-2 : put a flag to delte the empty value for CLI parameters...
            //add hidden field
            result.append("<dd><input name=\"__remove_empty_entries__\" value=\"true\" type=\"hidden\"></dd>");

            result.append("<dt class=\"button\"></dt><dd class=\"button\"><input value=\"")
                    .append(commandDisplayName)
                    .append("\" type=\"submit\"></dd>");
            result.append("</dl></form></div>");
        }

        return result.toString();
    }

    static protected String getHtmlForComponent(String component, String heading, String result) {
        if ((component != null) && (component.length() > 0)) {
            result = result + "<h2>" + heading + "</h2>";
            result = result + component;
            result = result + "<hr class=\"separator\"/>";
        }
        return result;
    }

    /**
     * Method to get the hint to display in case module monitoring levels are OFF
     *
     * @param uriInfo the uri context object of the input request
     * @param mediaType the media type of the input request
     * @return a hit to display when module monitoring levels are all OFF
     */
    static protected String getHint(UriInfo uriInfo, String mediaType) {
        String result = "";
        java.net.URI baseUri = uriInfo.getBaseUri();
        String monitoringLevelsConfigUrl = baseUri.getScheme() + "://" +
            baseUri.getHost() + ":" +  baseUri.getPort() +
                "/management/domain/configs/config/server-config/monitoring-service/module-monitoring-levels";

        String name = Util.localStrings.getLocalString(
            "rest.monitoring.levels.hint.heading", "Hint");
        String value = Util.localStrings.getLocalString("rest.monitoring.levels.hint.message",
            "Module monitoring levels may be OFF. To set module monitoring levels please visit following url: {0}",
                new Object[] {monitoringLevelsConfigUrl});

        if (mediaType.equals(MediaType.TEXT_HTML)) {
            monitoringLevelsConfigUrl =
                "<br><a href=\"" + monitoringLevelsConfigUrl + "\">" +
                    monitoringLevelsConfigUrl + "</a>";

            value = Util.localStrings.getLocalString("rest.monitoring.levels.hint.message",
                "Module monitoring levels may be OFF. To set module monitoring levels please visit following url: {0}",
                    new Object[] {monitoringLevelsConfigUrl});

            result = result + "<h2>" + name + "</h2>";
            result = result + value + "<br>";
            result = "<div>" + result + "</div>" + "<br>";
            return result;
       }

       if (mediaType.equals(MediaType.APPLICATION_JSON)) {
           result = " " + quote(name) + ":" + jsonValue(value);
           return result;
       }

       if (mediaType.equals(MediaType.APPLICATION_XML)) {
           result = result + " " + name + "=" + quote(value);
           return result;
       }

       return result;
    }

    static public String jsonValue(Object value) {
        String result ="";

        if (value.getClass().getName().equals("java.lang.String")) {
            result = quote(value.toString());
        } else {
            result =  value.toString();
        }

        return result;
    }

    static public String getHtmlHeader(String baseUri) {
        String title = Version.getVersion() + " REST Interface";
        String result = "<html><head><title>" + title + "</title>";
        result = result + getInternalStyleSheet(baseUri);
        result = result + getAjaxJavascript(baseUri);
        result = result + "</head><body>";
        result = result + "<h1 class=\"mainheader\">" + title + "</h1>";
        result = result + "<hr/>";
        return result;
    }

    static protected JSONArray getJsonForMethodMetaData(OptionsResult metaData) throws JSONException {
        OptionsResultJsonProvider provider = new OptionsResultJsonProvider();
        return provider.getRespresenationForMethodMetaData(metaData);
    }

    static protected String getJsonForMethodMetaData(OptionsResult metaData, String indent) {
        OptionsResultJsonProvider provider = new OptionsResultJsonProvider();
        return provider.getRespresenationForMethodMetaData(metaData).toString();
    }

    static protected String getXmlForMethodMetaData(OptionsResult metaData, String indent) {
        OptionsResultXmlProvider provider = new OptionsResultXmlProvider();
        return provider.getRespresenationForMethodMetaData(metaData, indent);
    }

    static private String getHtmlRespresentationForParameter(String parameter, ParameterMetaData parameterMetaData) {
        return getHtmlRespresentationForParameter(parameter, parameterMetaData, null);
    }

    static private String getHtmlRespresentationForParameter(String parameter,
            ParameterMetaData parameterMetaData, String parameterValue) {

      if ("true".equals(parameterMetaData.getAttributeValue(Constants.DEPRECATED))) {
            return "";
        }

        //set appropriate type of input field. In can be of type file or text
        //file type is used in case of deploy operation
        String parameterType = parameterMetaData.isFileParameter() ? "file" : "text";

        StringBuilder result = new StringBuilder();
        result.append("<dt><label for=\"")
                .append(parameter)
                .append("\">")
                .append(parameter)
                .append(parameterMetaData.getAttributeValue(Constants.OPTIONAL).equalsIgnoreCase("false") ? "<sup>*</sup>" : "") //indicate mandatory field with * super-script
                .append(":&nbsp;")
                .append("</label></dt>");

        boolean isBoolean = false;
        if((parameterMetaData.getAttributeValue(Constants.TYPE).endsWith(Constants.JAVA_BOOLEAN_TYPE)) ||
                (parameterMetaData.getAttributeValue(Constants.TYPE).equals(Constants.XSD_BOOLEAN_TYPE))) {
            isBoolean = true;
        }

        boolean hasAcceptableValues = false;
        String acceptableValues = parameterMetaData.getAttributeValue(Constants.ACCEPTABLE_VALUES);
        if ((acceptableValues != null) && (acceptableValues.length() > 0)) {
            hasAcceptableValues = true;
        }

        boolean hasValue = false;
        if ((parameterValue == null) || (parameterValue.equals(""))) {
            String defaultValue = parameterMetaData.getAttributeValue(Constants.DEFAULT_VALUE);
            if ((defaultValue != null) && (defaultValue.length() > 0)) {
                parameterValue = defaultValue;
            }
        }

        if ((parameterValue != null) && (parameterValue.length() > 0)) {
            hasValue = true;
        }

        boolean keyAttribute = Boolean.valueOf(parameterMetaData.getAttributeValue(Constants.KEY)).booleanValue();
        if (keyAttribute) {
            if (hasValue) {
                result.append("<dd><input name=\"")
                        .append(parameter)
                        .append("\" value =\"")
                        .append(parameterValue)
                        .append("\" type=\"")
                        .append(parameterType)
                        .append("\" disabled=\"disabled\"></dd>");
            } else { //no value for the key, so we are in create mode, enable the entry field in the ui
                //control should never reach here.
                result.append("<dd><input name=\"")
                        .append(parameter)
                        .append("\" type=\"")
                        .append(parameterType)
                        .append("\"></dd>");
           }
        } else {
            if (isBoolean || hasAcceptableValues) {
                //use combo box
                result.append("<dd><select name=").append(parameter).append(">");
                String[] values;
                if (isBoolean) {
                    values = new String[] {"true", "false"};
                } else {
                    values = stringToArray(acceptableValues, ",");
                }

                for (String value : values) {
                    if ((hasValue) && (value.equalsIgnoreCase(parameterValue))){
                        if (isBoolean) { parameterValue = parameterValue.toLowerCase(Locale.US);} //boolean options are all displayed as lowercase
                        result.append("<option selected>").append(parameterValue).append("<br>");
                    } else {
                        result.append("<option>").append(value).append("<br>");
                    }
                }
                result.append("</select></dd>");
            } else {
                //use text box
                String field;
                boolean isList = parameterMetaData.getAttributeValue(Constants.TYPE).equals("interface java.util.List");
                if (hasValue) {
                    field = "<input name=\"" + parameter + "\" value =\"" +
                        parameterValue + "\" type=\"" +  parameterType + "\">";
                } else {
                    field = "<input name=\"" + parameter + "\" type=\"" + parameterType + "\">";
                }
                result.append("<dd>").append(field);
                if (isList) {
                    result.append("<a href=\"#\" onclick=\"try { var newNode = this.previousSibling.cloneNode(false); this.parentNode.insertBefore(newNode, this);} catch (err) { alert (err); } return false; return false;\">Add row<a/>");
                }
                result.append("</dd>");
            }
        }

        return result.toString();
    }

    /**
     *  This method converts a string into string array, uses the delimeter as the
     *  separator character. If the delimiter is null, uses space as default.
     */
    private static String[] stringToArray(String str, String delimiter) {
        String[] retString = new String[0];

        if (str != null) {
            if(delimiter == null) {
                delimiter = " ";
            }
            StringTokenizer tokens = new StringTokenizer(str, delimiter);
            retString = new String[tokens.countTokens()];
            int i = 0;
            while(tokens.hasMoreTokens()) {
                retString[i++] = tokens.nextToken();
            }
        }
        return retString;
    }

    private static String getInternalStyleSheet(String baseUri) {

        return " <link rel=\"stylesheet\" type=\"text/css\" href=\""+baseUri+"static/std.css\" />";
    }

    private static String getAjaxJavascript(String baseUri) {
        return " <script type=\"text/javascript\" src=\""+baseUri+"static/ajax.javascript\"></script>";
    }
}
