/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2011 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.admin.rest;

import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.jersey.api.client.Client;
import java.io.File;
import org.glassfish.admin.rest.provider.ProviderUtil;
import javax.ws.rs.core.UriInfo;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.core.PathSegment;
import org.glassfish.admin.rest.utils.xml.RestActionReporter;
import org.glassfish.api.ActionReport.MessagePart;
import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandModel;
import org.glassfish.api.admin.ParameterMap;
import org.jvnet.hk2.component.Habitat;
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

    public static void main (String... args) {
        String url = "http://localhost:4848/management/domain/configs/config/server-config/java-config/generate-jvm-report";
        String gp = getGrandparentName(url);
        System.out.println("gp = " + gp);
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
        if ((null == elementName) || (null == prefix)
                || (prefix.length() <= 0)) {
            return elementName;
        }
        String methodName = upperCaseFirstLetter(elementName);
        return methodName = prefix + methodName;
    }
    
    public static Client getJerseyClient() {
        if (client == null) {
            client = Client.create();
        }
        
        return client;
    }


    /**
     * Apply changes passed in <code>data</code> using CLI "set".
     * @param data The set of changes to be applied
     * @return ActionReporter containing result of "set" execution
     */
    public static RestActionReporter applyChanges(Map<String, String> data, UriInfo uriInfo, Habitat habitat) {
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
        String setBasePath = sb.toString();
        ParameterMap parameters = new ParameterMap();
        Map<String, String> currentValues = getCurrentValues(setBasePath, habitat);

        for (Map.Entry<String, String> entry : data.entrySet()) {
            String currentValue = currentValues.get(setBasePath + entry.getKey());
            if ((currentValue == null) || entry.getValue().equals("") || (!currentValue.equals(entry.getValue()))) {
                parameters.add("DEFAULT", setBasePath + entry.getKey() + "=" + entry.getValue());
            }
        }
        if (!parameters.entrySet().isEmpty()) {
           return ResourceUtil.runCommand("set", parameters, habitat, ""); //TODO The last parameter is resultType and is not used. Refactor the called method to remove it
        } else {
            return new RestActionReporter(); // noop
        }
    }

    private static Map<String, String> getCurrentValues(String basePath, Habitat habitat) {
        Map<String, String> values = new HashMap<String, String>();
        final String path = (basePath.endsWith(".")) ? basePath.substring(0, basePath.length()-1) : basePath;
        RestActionReporter gr = ResourceUtil.runCommand("get", new ParameterMap() {{
            add ("DEFAULT", path);
        }}, habitat, "");

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
        tempDir.mkdirs();
        tempDir.deleteOnExit();
        
        return tempDir;
    }
    
    public static void deleteDirectory (final File dir) {

        if (dir == null || !dir.exists()) {
            return;
        }
        Logger logger = Logger.getLogger(Util.class.getName());

        if (dir.isDirectory()) {
            File[] f = dir.listFiles();
            if (f.length == 0) {
                if (!dir.delete()) {
                    if (logger.isLoggable(Level.WARNING)) {
                        logger.warning(String.format("Unable to delete directory %s.  Will attempt deletion again upon JVM exit.",
                                dir.getAbsolutePath()));
                    }
                }
            } else {
                for (final File ff : f) {
                    deleteDirectory(ff);
                }
            }
        } else {
            if (!dir.delete()) {
                if (logger.isLoggable(Level.WARNING)) {
                    logger.warning(String.format("Unable to delete file %s.  Will attempt deletion again upon JVM exit.",
                                   dir.getAbsolutePath()));
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
                    if (type.startsWith("java.lang")) {
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
}