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
package org.glassfish.admin.rest.utils;

import com.sun.enterprise.util.LocalStringManagerImpl;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.glassfish.admin.rest.provider.ProviderUtil;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.UriInfo;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import javax.security.auth.Subject;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.core.HttpHeaders;
import org.glassfish.admin.rest.Constants;
import org.glassfish.admin.rest.RestLogging;
import org.glassfish.admin.rest.model.ResponseBody;

import org.glassfish.admin.rest.utils.xml.RestActionReporter;
import org.glassfish.admin.restconnector.RestConfig;
import org.glassfish.api.ActionReport.MessagePart;
import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandModel;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;
import org.jvnet.hk2.config.ConfigModel;

/**
 * Utilities class. Extended by ResourceUtil and ProviderUtil utilities. Used by
 * resource and providers.
 *
 * @author Rajeshwar Patil
 */
public class Util {
    private static final String JAVA_IO_TMPDIR = "java.io.tmpdir";
    public final static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(Util.class);
    private static Client client;

    private Util() {
    }

    public static void logTimingMessage(String msg) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS");
        RestLogging.restLogger.log(Level.INFO, RestLogging.TIMESTAMP_MESSAGE, new Object[]{sdf.format(new Date()), msg});
    }

    /**
     * Returns name of the resource from UriInfo.
     */
    public static String getResourceName(UriInfo uriInfo) {
        return upperCaseFirstLetter(eleminateHypen(getName(uriInfo.getPath(), '/')));
    }

    /**
     * Returns name of the resource parent from UriInfo.
     */
    public static String getParentName(UriInfo uriInfo) {
        if (uriInfo == null) {
            return null;
        }
        return getParentName(uriInfo.getPath());
    }

    public static String getGrandparentName(UriInfo uriInfo) {
        if (uriInfo == null) {
            return null;
        }
        return getGrandparentName(uriInfo.getPath());
    }

    /**
     * Returns just the name of the given fully qualified name.
     */
    public static String getName(String typeName) {
        return getName(typeName, '.');
    }

    /**
     * Returns just the name of the given fully qualified name.
     */
    public static String getName(String typeName, char delimiter) {
        if ((typeName == null) || ("".equals(typeName))) {
            return typeName;
        }

        //elimiate last char from typeName if its a delimiter
        if (typeName.length() - 1 == typeName.lastIndexOf(delimiter)) {
            typeName = typeName.substring(0, typeName.length() - 1);
        }

        if ((typeName != null) && (typeName.length() > 0)) {
            int index = typeName.lastIndexOf(delimiter);
            if (index != -1) {
                return typeName.substring(index + 1);
            }
        }
        return typeName;
    }

    /**
     * returns just the parent name of the resource from the resource url.
     */
    public static String getParentName(String url) {
        if ((url == null) || ("".equals(url))) {
            return url;
        }
        String name = getName(url, '/');
        // Find the : to skip past the protocal part of the URL, as that is causing
        // problems with resources named 'http'.
        int nameIndex = url.indexOf(name, url.indexOf(":") + 1);
        return getName(url.substring(0, nameIndex - 1), '/');
    }

    public static String getGrandparentName(String url) {
        if ((url == null) || ("".equals(url))) {
            return url;
        }
        String name = getParentName(url);
        // Find the : to skip past the protocal part of the URL, as that is causing
        // problems with resources named 'http'.
        int nameIndex = url.indexOf(name, url.indexOf(":") + 1);
        return getName(url.substring(0, nameIndex - 1), '/');
    }

    /**
     * Removes any hypens ( - ) from the given string.
     * When it removes a hypen, it converts next immediate
     * character, if any,  to an Uppercase.(schema2beans convention)
     * @param string the input string
     * @return a <code>String</code> resulted after removing the hypens
     */
    public static String eleminateHypen(String string) {
        if (!(string == null || string.length() <= 0)) {
            int index = string.indexOf('-');
            while (index != -1) {
                if (index == 0) {
                    string = string.substring(1);
                } else {
                    if (index == (string.length() - 1)) {
                        string = string.substring(0, string.length() - 1);
                    } else {
                        string = string.substring(0, index)
                                + upperCaseFirstLetter(string.substring(index + 1));
                    }
                }
                index = string.indexOf('-');
            }
        }
        return string;
    }

    public static String decode(String string) {
        String ret = string;

        try {
            ret = URLDecoder.decode(string, "UTF-8");
        } catch (UnsupportedEncodingException e) {
        }

        return ret;
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
        return string.substring(0, 1).toUpperCase(Locale.US) + string.substring(1);
    }

    /**
     * Converts the first letter of the given string to lower case.
     *
     * @param string the input string
     * @return the string with the lower case first letter
     */
    public static String lowerCaseFirstLetter(String string) {
        if (string == null || string.length() <= 0) {
            return string;
        }
        return string.substring(0, 1).toLowerCase(Locale.US) + string.substring(1);
    }

    /**
     * Returns the html for the given message.
     *
     * @param uriInfo the uriInfo context of the request
     * @return String the html representation of the given message
     */
    protected static String getHtml(String message, UriInfo uriInfo, boolean delete) {
        String result = ProviderUtil.getHtmlHeader(uriInfo.getBaseUri().toASCIIString());
        String uri = uriInfo.getAbsolutePath().toString();
        if (delete) {
            uri = uri + "/..";
        }
        String name = upperCaseFirstLetter(eleminateHypen(getName(uri, '/')));

        result = result + "<h1>" + name + "</h1>";
        result = result + message;//+ "<br><br>";
        result = result + "<a href=\"" + uri + "\">Back</a>";

        //  result =  result +  "<br>";
        result = result + "</body></html>";
        return result;
    }

    /**
     * Constructs a method name from  element's dtd name
     * name for a given prefix.(schema2beans convention)
     *
     * @param elementName the given element name
     * @param prefix the given prefix
     * @return a method name formed from the given name and the prefix
     */
    public static String methodNameFromDtdName(String elementName, String prefix) {
        return methodNameFromBeanName(eleminateHypen(elementName), prefix);
    }

    /**
     * Constructs a method name from  element's bean
     * name for a given prefix.(schema2beans convention)
     *
     * @param elementName the given element name
     * @param prefix the given prefix
     * @return a method name formed from the given name and the prefix
     */
    public static String methodNameFromBeanName(String elementName, String prefix) {
        if ((null == elementName) || (null == prefix) || (prefix.length() <= 0)) {
            return elementName;
        }
        return prefix + upperCaseFirstLetter(elementName);
    }

    public static synchronized Client getJerseyClient() {
        if (client == null) {
            client = ClientBuilder.newClient();
        }

        return client;
    }

    /**
     * Apply changes passed in <code>data</code> using CLI "set".
     * @param data The set of changes to be applied
     * @return ActionReporter containing result of "set" execution
     */
    public static RestActionReporter applyChanges(Map<String, String> data, UriInfo uriInfo, Subject subject) {
        return applyChanges(data, getBasePathFromUri(uriInfo), subject);
    }

    public static RestActionReporter applyChanges(Map<String, String> data, String basePath, Subject subject) {
        ParameterMap parameters = new ParameterMap();
        Map<String, String> currentValues = getCurrentValues(basePath, subject);

        for (Map.Entry<String, String> entry : data.entrySet()) {
            String currentValue = currentValues.get(basePath + entry.getKey());
            if ((currentValue == null) || entry.getValue().equals("") || (!currentValue.equals(entry.getValue()))) {
                parameters.add("DEFAULT", basePath + entry.getKey() + "=" + entry.getValue());
            }
        }
        if (!parameters.entrySet().isEmpty()) {
           return ResourceUtil.runCommand("set", parameters, subject);
        } else {
            return new RestActionReporter(); // noop
        }
    }

    private static String getBasePathFromUri(UriInfo uriInfo) {
        List<PathSegment> pathSegments = uriInfo.getPathSegments();
        // Discard the last segment if it is empty. This happens if some one accesses the resource
        // with trailing '/' at end like in htto://host:port/mangement/domain/.../pathelement/
        PathSegment lastSegment = pathSegments.get(pathSegments.size() - 1);
        if(lastSegment.getPath().isEmpty()) {
            pathSegments = pathSegments.subList(0, pathSegments.size() - 1);
        }
        List<PathSegment> candidatePathSegment = null;
        if(pathSegments.size() != 1) {
            // Discard "domain"
            candidatePathSegment = pathSegments.subList(1, pathSegments.size());
        } else {
            // We are being called for a config change at domain level.
            // CLI "set" requires name to be of form domain.<attribute-name>.
            // Preserve "domain"
            candidatePathSegment = pathSegments;
        }
        final StringBuilder sb = new StringBuilder();
        for(PathSegment pathSegment :  candidatePathSegment) {
            sb.append(pathSegment.getPath());
            sb.append('.');
        }
        return sb.toString();
    }

    public static Map<String, String> getCurrentValues(String basePath, Subject subject) {
        ServiceLocator serviceLocator = Globals.getDefaultBaseServiceLocator();
        return getCurrentValues(basePath, serviceLocator, subject);
    }

    public static Map<String, String> getCurrentValues(String basePath, ServiceLocator habitat, Subject subject) {
        Map<String, String> values = new HashMap<String, String>();
        final String path = (basePath.endsWith(".")) ? basePath.substring(0, basePath.length()-1) : basePath;
        RestActionReporter gr = ResourceUtil.runCommand("get", new ParameterMap() {{
            add ("DEFAULT", path);
        }}, subject);

        MessagePart top = gr.getTopMessagePart();
        for (MessagePart child : top.getChildren()) {
            String message = child.getMessage();
            if (message.contains("=")) {
                String[] parts = message.split("=");
                values.put(parts[0], (parts.length > 1) ? parts[1] : "");
            }
        }

        return values;
    }

    /**
     * @param model
     * @return name of the key attribute for the given model.
     */
    public static String getKeyAttributeName(ConfigModel model) {
        if (model == null) {
            return null;
        }

        String keyAttributeName = null;
        if (model.key == null) {
            // .contains()?
            for (String s : model.getAttributeNames()) {//no key, by default use the name attr
                if (s.equals("name")) {
                    keyAttributeName = getBeanName(s);
                }
            }
        } else {
            keyAttributeName = getBeanName(model.key.substring(1, model.key.length()));
        }
        return keyAttributeName;
    }

    public static String getBeanName(String elementName) {
        StringBuilder ret = new StringBuilder();
        boolean nextisUpper = true;
        for (int i = 0; i < elementName.length(); i++) {
            if (nextisUpper == true) {
                ret.append(elementName.substring(i, i + 1).toUpperCase(Locale.US));
                nextisUpper = false;
            } else {
                if (elementName.charAt(i) == '-') {
                    nextisUpper = true;
                } else {
                    nextisUpper = false;
                    ret.append(elementName.substring(i, i + 1));
                }
            }
        }
        return ret.toString();
    }

    public static File createTempDirectory() {
        File baseTempDir = new File(System.getProperty(JAVA_IO_TMPDIR));
        File tempDir = new File(baseTempDir, Long.toString(System.currentTimeMillis()));
        if (!tempDir.mkdirs()) {
            throw new RuntimeException("Unable to create directories"); // i81n
        }
        tempDir.deleteOnExit();

        return tempDir;
    }

    public static void deleteDirectory (final File dir) {

        if (dir == null || !dir.exists()) {
            return;
        }

        if (dir.isDirectory()) {
            File[] f = dir.listFiles();
            if (f.length == 0) {
                if (!dir.delete()) {
                    if (RestLogging.restLogger.isLoggable(Level.WARNING)) {
                        RestLogging.restLogger.log(Level.WARNING, RestLogging.UNABLE_DELETE_DIRECTORY, dir.getAbsolutePath());
                    }
                }
            } else {
                for (final File ff : f) {
                    deleteDirectory(ff);
                }
            }
        } else {
            if (!dir.delete()) {
                if (RestLogging.restLogger.isLoggable(Level.WARNING)) {
                    RestLogging.restLogger.log(Level.WARNING, RestLogging.UNABLE_DELETE_FILE, dir.getAbsolutePath());
                }
                dir.deleteOnExit();
            }
        }

    }

    public static String getMethodParameterList(CommandModel cm, boolean withType, boolean includeOptional) {
        StringBuilder sb = new StringBuilder();
        Collection<CommandModel.ParamModel> params = cm.getParameters();
        if ((params != null) && (!params.isEmpty())) {
            String sep = "";
            for (CommandModel.ParamModel model : params) {
                Param param = model.getParam();
                boolean include = true;
                if (param.optional() && !includeOptional) {
                    continue;
                }

                sb.append(sep);
                if (withType) {
                    String type = model.getType().getName();
                    if (model.getType().isArray()) {
                        type = model.getType().getName()
                                .substring(2);
                        type = type.substring(0, type.length()-1) + "[]";

                    } else if (type.startsWith("java.lang")) {
                        type = model.getType().getSimpleName();
                    }
                    sb.append(type);
                }
                sb.append(" _").append(Util.eleminateHypen(model.getName()));
                sep = ", ";
            }
        }

        return sb.toString();
    }

    public static File saveFile(String fileName, String mimeType, InputStream fileStream) {
        BufferedOutputStream out = null;
        File f = null;
        try {
            if (fileName.contains(".")) {
                //String prefix = fileName.substring(0, fileName.indexOf("."));
                // String suffix = fileName.substring(fileName.indexOf("."), fileName.length());
                //if (prefix.length() < 3) {
                //    prefix = "glassfish" + prefix;
                //}
                f = new File(new File(System.getProperty("java.io.tmpdir")), fileName);
            }


            out = new BufferedOutputStream(new FileOutputStream(f));
            byte[] buffer = new byte[32 * 1024];
            int bytesRead = 0;
            while ((bytesRead = fileStream.read(buffer)) != -1) {
                out.write(buffer, 0, bytesRead);
            }
            return f;
        } catch (IOException ex) {
            RestLogging.restLogger.log(Level.SEVERE, RestLogging.IO_EXCEPTION,
                    ex.getMessage());
        } finally {
            try {
                if (out != null) {
                    out.close();
                }
            } catch (IOException ex) {
                RestLogging.restLogger.log(Level.SEVERE, RestLogging.IO_EXCEPTION, 
                        ex.getMessage());
            }
        }
        return null;
    }

    public static boolean isGenericType(Type type) {
        return ParameterizedType.class.isAssignableFrom(type.getClass());
    }

    /**
     * This method takes a Type argument that represents a generic class (e.g., <code>List&lt;String&gt;) and returns the
     * <code>Class</code> for the first generic type.  If the <code>Class</code> is not a generic type,
     * <code>null</code> is returned. The primary intended usage for this is in the <code>MessageBodyReader</code>s to
     * help return a more accurate result from <code>isReadable</code>, though it may also be helpful in other, more
     * general situations.
     * @param genericType
     * @return
     */
    public static Class<?> getFirstGenericType(Type genericType) {
        if (genericType instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType)genericType;
            Type [] typeArgs = pt.getActualTypeArguments();
            if ((typeArgs != null) && (typeArgs.length >= 1)) {
                final Type type = typeArgs[0];
                if (ParameterizedType.class.isAssignableFrom(type.getClass())) {
                    return (Class<?>)((ParameterizedType)type).getRawType();
                } else {
                    return (Class<?>)type;
                }
            }
        }

        return null;
    }

    /**
     * Get the current configured indenting value for the REST layer
     * @return
     */
    public static int getFormattingIndentLevel() {
        RestConfig rg = ResourceUtil.getRestConfig(Globals.getDefaultBaseServiceLocator());
        if (rg == null) {
            return -1;
        } else {
            return Integer.parseInt(rg.getIndentLevel());
        }

    }

    public static boolean useLegacyResponseFormat(HttpHeaders requestHeaders) {
        final boolean legacyHeaderPresent = requestHeaders.getHeaderString(Constants.HEADER_LEGACY_FORMAT) != null;
        final boolean acceptsHtml = requestHeaders.getHeaderString("Accept").contains("html");
        final boolean acceptsJson = requestHeaders.getHeaderString("Accept").contains("json");
        return legacyHeaderPresent || acceptsHtml || !acceptsJson;
    }

   /**
     * Convenience wrapper around ParameterMap constructor to make it easier to use its fluent API
     * @return ParameterMap
     */
    public static ParameterMap parameterMap() {
        return new ParameterMap();
    }
}
