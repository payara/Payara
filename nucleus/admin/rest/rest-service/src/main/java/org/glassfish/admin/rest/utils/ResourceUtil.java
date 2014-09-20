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

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.security.AccessController;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.regex.Pattern;
import javax.security.auth.Subject;
import javax.security.auth.login.LoginException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import org.glassfish.admin.rest.Constants;
import org.glassfish.admin.rest.RestLogging;
import org.glassfish.admin.rest.generator.CommandResourceMetaData;
import org.glassfish.admin.rest.provider.MethodMetaData;
import org.glassfish.admin.rest.provider.ParameterMetaData;
import org.glassfish.admin.rest.provider.ProviderUtil;
import static org.glassfish.admin.rest.provider.ProviderUtil.getElementLink;
import org.glassfish.admin.rest.results.ActionReportResult;
import static org.glassfish.admin.rest.utils.Util.eleminateHypen;
import static org.glassfish.admin.rest.utils.Util.getHtml;
import static org.glassfish.admin.rest.utils.Util.methodNameFromDtdName;
import org.glassfish.admin.rest.utils.xml.RestActionReporter;
import org.glassfish.admin.restconnector.RestConfig;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.CommandModel;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.CommandRunner.CommandInvocation;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.admin.RestRedirect;
import org.glassfish.api.admin.RestRedirects;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.hk2.api.MultiException;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.AdminAccessController;
import org.glassfish.internal.api.Globals;
import org.glassfish.jersey.media.sse.EventOutput;
import org.glassfish.security.services.api.authorization.AuthorizationService;
import org.glassfish.security.services.common.PrivilegedLookup;

import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigModel;
import org.jvnet.hk2.config.Dom;
import org.jvnet.hk2.config.DomDocument;

/**
 * Resource utilities class. Used by resource templates,
 * <code>TemplateListOfResource</code> and
 * <code>TemplateRestResource</code>
 *
 * @author Rajeshwar Patil
 */
public class ResourceUtil {
    private static final String DAS_LOOK_FOR_CERT_PROPERTY_NAME = "org.glassfish.admin.DASCheckAdminCert";
    private static final String MESSAGE_PARAMETERS = "messageParameters";
    private static RestConfig restConfig = null;
    // TODO: this is copied from org.jvnet.hk2.config.Dom. If we are not able to encapsulate the conversion in Dom,
    // need to make sure that the method convertName is refactored into smaller methods such that trimming of prefixes
    // stops. We will need a promotion of HK2 for this.
    static final Pattern TOKENIZER;

    static {
        String pattern = or(
                split("x", "X"), // AbcDef -> Abc|Def
                split("X", "Xx"), // USArmy -> US|Army
                //split("\\D","\\d"), // SSL2 -> SSL|2
                split("\\d", "\\D") // SSL2Connector -> SSL|2|Connector
                );
        pattern = pattern.replace("x", "\\p{Lower}").replace("X", "\\p{Upper}");
        TOKENIZER = Pattern.compile(pattern);
    }

    protected static byte[] getBytesFromStream(final InputStream is) {
        final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] bytes = null;
        int nRead;
        byte[] data = new byte[16384];
        try {
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            buffer.flush();
            bytes = buffer.toByteArray();
            buffer.close();
        } catch (IOException ex) {
            RestLogging.restLogger.log(Level.SEVERE, RestLogging.IO_EXCEPTION, ex.getMessage());
        }

        return bytes;
    }

    private ResourceUtil() {
    }

    /**
     * Adjust the input parameters. In case of POST and DELETE methods, user can
     * provide name, id or DEFAULT parameter for primary parameter(i.e the
     * object to create or delete). This method is used to rename primary
     * parameter name to DEFAULT irrespective of what user provides.
     */
    public static void adjustParameters(Map<String, String> data) {
        if (data != null) {
            if (!(data.containsKey("DEFAULT"))) {
                boolean isRenamed = renameParameter(data, "id", "DEFAULT");
                if (!isRenamed) {
                    renameParameter(data, "name", "DEFAULT");
                }
            }
        }
    }

    /**
     * Adjust the input parameters. In case of POST and DELETE methods, user can
     * provide id or DEFAULT parameter for primary parameter(i.e the object to
     * create or delete). This method is used to rename primary parameter name
     * to DEFAULT irrespective of what user provides.
     */
    public static void defineDefaultParameters(Map<String, String> data) {
        if (data != null) {
            if (!(data.containsKey("DEFAULT"))) {
                renameParameter(data, "id", "DEFAULT");
            }
        }
    }

    /**
     * Returns the name of the command associated with this resource,if any, for
     * the given operation.
     *
     * @param type the given resource operation
     * @return String the associated command name for the given operation.
     */
    public static String getCommand(RestRedirect.OpType type, ConfigModel model) {

        Class<? extends ConfigBeanProxy> cbp = null;
        try {
            cbp = (Class<? extends ConfigBeanProxy>) model.classLoaderHolder.loadClass(model.targetTypeName);
        } catch (MultiException e) {
            e.printStackTrace();
        }

        if (cbp != null) {
            RestRedirects restRedirects = cbp.getAnnotation(RestRedirects.class);
            if (restRedirects != null) {
                RestRedirect[] values = restRedirects.value();
                for (RestRedirect r : values) {
                    if (r.opType().equals(type)) {
                        return r.commandName();
                    }
                }
            }
        }
        return null;
    }

    /**
     * Executes the specified __asadmin command.
     * @param commandName
     * @param parameters
     * @param subject
     * @return
     */
    public static RestActionReporter runCommand(String commandName,
                                                ParameterMap parameters,
                                                Subject subject) {
        return runCommand(commandName, parameters, subject, false);
    }

    /**
     * Executes the specified __asadmin command.
     * @param commandName
     * @param parameters
     * @param subject
     * @param managedJob
     * @return
     */
    public static RestActionReporter runCommand(String commandName,
                                                ParameterMap parameters,
                                                Subject subject,
                                                boolean managedJob) {
        CommandRunner cr = Globals.getDefaultHabitat().getService(CommandRunner.class);
        RestActionReporter ar = new RestActionReporter();
        final CommandInvocation commandInvocation =
                cr.getCommandInvocation(commandName, ar, subject);
        if (managedJob) {
            commandInvocation.managedJob();
        }
        commandInvocation.parameters(parameters).execute();
        addCommandLog(ar, commandName, parameters);

        return ar;
    }

    /**
     * Executes the specified __asadmin command.
     *
     * @param commandName the command to execute
     * @param parameters the command parameters
     * @param subject Subject
     * @return ActionReport object with command execute status details.
     */
    public static RestActionReporter runCommand(String commandName,
                                                Map<String, String> parameters,
                                                Subject subject) {
        ParameterMap p = new ParameterMap();
        for (Map.Entry<String, String> entry : parameters.entrySet()) {
            p.set(entry.getKey(), entry.getValue());
        }

        return runCommand(commandName, p, subject);
    }

    public static EventOutput runCommandWithSse(final String commandName,
                                                final ParameterMap parameters,
                                                final Subject subject,
                                                final SseCommandHelper.ActionReportProcessor processor) {
        CommandRunner cr = Globals.getDefaultHabitat().getService(CommandRunner.class);
        final RestActionReporter ar = new RestActionReporter();
        final CommandInvocation commandInvocation =
            cr.getCommandInvocation(commandName, ar, subject).
            parameters(parameters);
        return SseCommandHelper.invokeAsync(commandInvocation,
                    new SseCommandHelper.ActionReportProcessor() {
                            @Override
                            public ActionReport process(ActionReport report, EventOutput ec) {
                                addCommandLog(ar, commandName, parameters);
                                if (processor != null) {
                                    return processor.process(report, ec);
                                }
                                return ar;
                            }
                        });
    }


    public static void addCommandLog(RestActionReporter ar, String commandName, ParameterMap parameters) {
        List<String> logs = (List<String>) ar.getExtraProperties().get("commandLog");
        if (logs == null) {
            logs = new ArrayList<String>();
            ar.getExtraProperties().put("commandLog", logs);
        }
        final String parameterList = encodeString(getParameterList(parameters));

        logs.add(commandName + parameterList);
    }

    public static String encodeString(String text) {
        if (text == null) {
            return "";
        }
        String result = text.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
        result = result.replaceAll("eval\\((.*)\\)", "");
        result = result.replaceAll("[\\\"\\\'][\\s]*((?i)javascript):(.*)[\\\"\\\']", "\"\"");
        result = result.replaceAll("((?i)script)", "");
        return result;
    }

    public static String getParameterList(ParameterMap parameters) {
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
            String paramName = entry.getKey();
            for (String param : entry.getValue()) {
                sb.append(" --").append(paramName).append(" ").append(param);
            }
        }

        return sb.toString();
    }

    /**
     * Constructs and returns the resource method meta-data.
     *
     * @param command the command associated with the resource method
     * @param habitat the habitat
     * @param logger the logger to use
     * @return MethodMetaData the meta-data store for the resource method.
     */
    public static MethodMetaData getMethodMetaData(String command, ServiceLocator habitat) {
        return getMethodMetaData(command, null, habitat);
    }

    /**
     * Constructs and returns the resource method meta-data.
     *
     * @param command the command associated with the resource method
     * @param commandParamsToSkip the command parameters for which not to
     * include the meta-data.
     * @param habitat the habitat
     * @param logger the logger to use
     * @return MethodMetaData the meta-data store for the resource method.
     */
    public static MethodMetaData getMethodMetaData(String command, HashMap<String, String> commandParamsToSkip,
            ServiceLocator habitat) {
        MethodMetaData methodMetaData = new MethodMetaData();

        if (command != null) {
            Collection<CommandModel.ParamModel> params;
            if (commandParamsToSkip == null) {
                params = getParamMetaData(command, habitat);
            } else {
                params = getParamMetaData(command, commandParamsToSkip.keySet(), habitat);
            }

            if (params != null) {
                Iterator<CommandModel.ParamModel> iterator = params.iterator();
                CommandModel.ParamModel paramModel;
                while (iterator.hasNext()) {
                    paramModel = iterator.next();
                    Param param = paramModel.getParam();
                    ParameterMetaData parameterMetaData = getParameterMetaData(paramModel);

                    String parameterName = (param.primary()) ? "id" : paramModel.getName();

                    // If the Param has an alias, use it instead of the name
                    String alias = param.alias();
                    if (alias != null && (!alias.isEmpty())) {
                        parameterName = alias;
                    }


                    methodMetaData.putParameterMetaData(parameterName, parameterMetaData);
                }
            }
        }

        return methodMetaData;
    }

    public static void resolveParamValues(Map<String, String> commandParams, UriInfo uriInfo) {
        List<PathSegment> pathSegments = uriInfo.getPathSegments();
        Map<String, String> processParams = new HashMap<String, String>();
        processParams.putAll(commandParams);

        for (Map.Entry<String, String> entry : commandParams.entrySet()) {
            String value = entry.getValue();
            if (value.equals(Constants.VAR_PARENT)) {
                processParams.put(entry.getKey(), pathSegments.get(pathSegments.size() - 2).getPath());
            } else if (value.startsWith(Constants.VAR_GRANDPARENT)) {
                int number =
                        (value.equals(Constants.VAR_GRANDPARENT))
                        ? 1 : // no number given
                        Integer.parseInt(value.substring(Constants.VAR_GRANDPARENT.length()));

                processParams.put(entry.getKey(), pathSegments.get(pathSegments.size() - (number + 2)).getPath());
            }
        }

        commandParams.clear();
        commandParams.putAll(processParams);
    }

    /**
     * Constructs and returns the resource method meta-data. This method is
     * called to get meta-data in case of update method (POST).
     *
     * @param configBeanModel the config bean associated with the resource.
     * @return MethodMetaData the meta-data store for the resource method.
     */
    public static MethodMetaData getMethodMetaData(ConfigModel configBeanModel) {
        MethodMetaData methodMetaData = new MethodMetaData();

        Class<? extends ConfigBeanProxy> configBeanProxy = null;
        try {
            configBeanProxy = (Class<? extends ConfigBeanProxy>) configBeanModel.classLoaderHolder
                    .loadClass(configBeanModel.targetTypeName);

            Set<String> attributeNames = configBeanModel.getAttributeNames();
            for (String attributeName : attributeNames) {
                String methodName = getAttributeMethodName(attributeName);
                Method method = null;
                try {
                    method = configBeanProxy.getMethod(methodName);
                } catch (NoSuchMethodException e) {
                    // Method not found, so let's try a brute force method if the method
                    // can't be found via the method above.  For example: for
                    // Ssl.getSSLInactivityTimeout(), we calculate getSslInactivityTimeout,
                    // which doesn't match due to case.
                    for (Method m : configBeanProxy.getMethods()) {
                        if (m.getName().equalsIgnoreCase(methodName)) {
                            method = m;
                        }
                    }
                }
                Attribute attribute = method.getAnnotation(Attribute.class);
                if (attribute != null) {
                    ParameterMetaData parameterMetaData = getParameterMetaData(attribute);
                    if (method.getAnnotation(Deprecated.class) != null) {
                        parameterMetaData.putAttribute(Constants.DEPRECATED, "true");
                    }

                    //camelCase the attributeName before passing out
                    attributeName = eleminateHypen(attributeName);

                    methodMetaData.putParameterMetaData(attributeName, parameterMetaData);

                }
            }
        } catch (MultiException e) {
            e.printStackTrace();
        }

        return methodMetaData;
    }

    public static MethodMetaData getMethodMetaData2(Dom parent, ConfigModel childModel, int parameterType) {
        MethodMetaData methodMetaData = new MethodMetaData();
        List<Class<?>> interfaces = new ArrayList<Class<?>>();
        Map<String, ParameterMetaData> params = new HashMap<String, ParameterMetaData>();

        try {
            Class<? extends ConfigBeanProxy> configBeanProxy =
                    (Class<? extends ConfigBeanProxy>) childModel.classLoaderHolder.loadClass(childModel.targetTypeName);
            getInterfaces(configBeanProxy, interfaces);

            Set<String> attributeNames = childModel.getAttributeNames();
            for (String attributeName : attributeNames) {
                String methodName = ResourceUtil.getAttributeMethodName(attributeName);

                //camelCase the attributeName before passing out
                attributeName = Util.eleminateHypen(attributeName);

                ParameterMetaData parameterMetaData = params.get(attributeName);
                if (parameterMetaData == null) {
                    parameterMetaData = new ParameterMetaData();
                    params.put(attributeName, parameterMetaData);
                }
                // Check parent interfaces
                for (int i = interfaces.size() - 1; i >= 0; i--) {
                    Class<?> intf = interfaces.get(i);
                    try {
                        Method method = intf.getMethod(methodName);
                        Attribute attribute = method.getAnnotation(Attribute.class);
                        if (attribute != null) {
                            ParameterMetaData localParam = ResourceUtil.getParameterMetaData(attribute);
                            copyParameterMetaDataAttribute(localParam, parameterMetaData, Constants.DEFAULT_VALUE);
                            copyParameterMetaDataAttribute(localParam, parameterMetaData, Constants.KEY);
                            copyParameterMetaDataAttribute(localParam, parameterMetaData, Constants.TYPE);
                            copyParameterMetaDataAttribute(localParam, parameterMetaData, Constants.OPTIONAL);
                        }
                    } catch (NoSuchMethodException e) {
                    }
                }

                // Check ConfigBean
                try {
                    Method method = configBeanProxy.getMethod(methodName);
                    Attribute attribute = method.getAnnotation(Attribute.class);
                    if (attribute != null) {
                        ParameterMetaData localParam = ResourceUtil.getParameterMetaData(attribute);
                        copyParameterMetaDataAttribute(localParam, parameterMetaData, Constants.DEFAULT_VALUE);
                        copyParameterMetaDataAttribute(localParam, parameterMetaData, Constants.KEY);
                        copyParameterMetaDataAttribute(localParam, parameterMetaData, Constants.TYPE);
                        copyParameterMetaDataAttribute(localParam, parameterMetaData, Constants.OPTIONAL);
                    }
                } catch (NoSuchMethodException e) {
                }


                methodMetaData.putParameterMetaData(attributeName, parameterMetaData);

            }
        } catch (MultiException cnfe) {
            throw new RuntimeException(cnfe);
        }

        return methodMetaData;
    }

    protected static void copyParameterMetaDataAttribute(ParameterMetaData from, ParameterMetaData to, String key) {
        if (from.getAttributeValue(key) != null) {
            to.putAttribute(key, from.getAttributeValue(key));
        }
    }

    protected static void getInterfaces(Class<?> clazz, List<Class<?>> interfaces) {
        for (Class<?> intf : clazz.getInterfaces()) {
            interfaces.add(intf);
            getInterfaces(intf, interfaces);
        }
    }

    /*
     * test if a command really exists in the current runningVM
     */
    public static boolean commandIsPresent(ServiceLocator habitat, String commandName) {
        try {
            habitat.getService(AdminCommand.class, commandName);
            return true;
        } catch (Exception e) {

        }
        return false;
    }

    /**
     * Constructs and returns the parameter meta-data.
     *
     * @param commandName the command associated with the resource method
     * @param habitat the habitat
     * @param logger the logger to use
     * @return Collection the meta-data for the parameter of the resource
     * method.
     */
    public static Collection<CommandModel.ParamModel> getParamMetaData(
            String commandName, ServiceLocator habitat) {
        final CommandModel model = habitat.<CommandRunner>getService(CommandRunner.class).getModel(commandName, RestLogging.restLogger);
        if (model == null) {
            return null;
        }

        return model.getParameters();
    }

    /**
     * Constructs and returns the parameter meta-data.
     *
     * @param commandName the command associated with the resource method
     * @param commandParamsToSkip the command parameters for which not to
     * include the meta-data.
     * @param habitat the habitat
     * @param logger the logger to use
     * @return Collection the meta-data for the parameter of the resource
     * method.
     */
    public static Collection<CommandModel.ParamModel> getParamMetaData(
            String commandName, Collection<String> commandParamsToSkip,
            ServiceLocator habitat) {
        CommandModel cm = habitat.<CommandRunner>getService(CommandRunner.class).getModel(commandName, RestLogging.restLogger);
        Collection<String> parameterNames = cm.getParametersNames();

        ArrayList<CommandModel.ParamModel> metaData = new ArrayList<CommandModel.ParamModel>();
        CommandModel.ParamModel paramModel;
        for (String name : parameterNames) {
            paramModel = cm.getModelFor(name);
            String parameterName = (paramModel.getParam().primary()) ? "id" : paramModel.getName();

            if (!commandParamsToSkip.contains(parameterName)) {
                metaData.add(paramModel);
            }
        }

        //print(metaData);
        return metaData;
    }

    //removes entries with empty value from the given Map
    public static void purgeEmptyEntries(Map<String, String> data) {

        //hack-2 : remove empty entries if the form has a hidden param for __remove_empty_entries__
        if ("true".equals(data.get("__remove_empty_entries__"))) {
            data.remove("__remove_empty_entries__");
            //findbug
            for (Iterator<Map.Entry<String, String>> i = data.entrySet().iterator(); i.hasNext();) {
                Map.Entry<String, String> entry = i.next();
                String value = entry.getValue();
                if ((value == null) || (value.length() < 1)) {
                    i.remove();
                }
            }
        }
    }

    /**
     * Constructs and returns the appropriate response object based on the
     * client.
     *
     * @param status the http status code for the response
     * @param message message for the response
     * @param requestHeaders request headers of the request
     * @return Response the response object to be returned to the client
     */
    public static Response getResponse(int status, String message, HttpHeaders requestHeaders, UriInfo uriInfo) {
        if (isBrowser(requestHeaders)) {
            message = getHtml(message, uriInfo, false);
        }
        return Response.status(status).entity(message).build();
    }

    public static ActionReportResult getActionReportResult(ActionReport.ExitCode status, String message, HttpHeaders requestHeaders, UriInfo uriInfo) {
        RestActionReporter ar = new RestActionReporter();
        ar.setActionExitCode(status);
        return getActionReportResult(ar, message, requestHeaders, uriInfo);
    }

    public static ActionReportResult getActionReportResult(RestActionReporter ar, String message, HttpHeaders requestHeaders, UriInfo uriInfo) {
        if (isBrowser(requestHeaders)) {
            message = getHtml(message, uriInfo, false);
        }
        ActionReport.ExitCode status = ar.getActionExitCode();
        ActionReportResult result = new ActionReportResult(ar);

        if (status != ActionReport.ExitCode.SUCCESS && status != ActionReport.ExitCode.WARNING) {
            result.setErrorMessage(message);
            result.setIsError(true);
        }

        ar.setActionExitCode(status);
        ar.setMessage(message);
        return result;
    }

    /**
     * special case for the delete operation: we need to give back the URI of
     * the parent since the resource we are on is deleted
     *
     * @param status
     * @param message
     * @param requestHeaders
     * @param uriInfo
     * @return
     */
    // FIXME: This doesn't do what the javadoc says it should
    public static Response getDeleteResponse(int status, String message,
            HttpHeaders requestHeaders, UriInfo uriInfo) {
        if (isBrowser(requestHeaders)) {
            message = getHtml(message, uriInfo, true);
        }
        return Response.status(status).entity(message).build();
    }

    /**
     * <p>This method takes any query string parameters and adds them to the
     * specified map. This is used, for example, with the delete operation when
     * cascading deletes are required:</p> <code style="margin-left: 3em">DELETE http://localhost:4848/.../foo?cascade=true</code>
     * <p>The reason we need to use query parameters versus "body" variables is
     * the limitation that HttpURLConnection has in this regard.
     *
     * @param data
     */
    public static void addQueryString(MultivaluedMap<String, String> qs, Map<String, String> data) {
        for (Map.Entry<String, List<String>> entry : qs.entrySet()) {
            String key = entry.getKey();
            for (String value : entry.getValue()) {
                try {
                    data.put(URLDecoder.decode(key, "UTF-8"), URLDecoder.decode(value, "UTF-8")); // TODO: Last one wins? Can't imagine we'll see List.size() > 1, but...
                } catch (UnsupportedEncodingException ex) {
                    throw new RuntimeException(ex);
                }
            }
        }
    }

    public static void addQueryString(MultivaluedMap<String, String> qs, Properties data) {
        for (Map.Entry<String, List<String>> entry : qs.entrySet()) {
            String key = entry.getKey();
            for (String value : entry.getValue()) {
                data.put(key, value); // TODO: Last one wins? Can't imagine we'll see List.size() > 1, but...
            }
        }
    }

    //Construct parameter meta-data from the model
    static ParameterMetaData getParameterMetaData(CommandModel.ParamModel paramModel) {
        Param param = paramModel.getParam();
        ParameterMetaData parameterMetaData = new ParameterMetaData();

        parameterMetaData.putAttribute(Constants.TYPE, getXsdType(paramModel.getType().toString()));
        parameterMetaData.putAttribute(Constants.OPTIONAL, Boolean.toString(param.optional()));
        String val = param.defaultValue();
        if ((val != null) && (!val.equals("\u0000"))) {
            parameterMetaData.putAttribute(Constants.DEFAULT_VALUE, param.defaultValue());
        }
        parameterMetaData.putAttribute(Constants.ACCEPTABLE_VALUES, param.acceptableValues());

        return parameterMetaData;
    }

    //Construct parameter meta-data from the attribute annotation
    static ParameterMetaData getParameterMetaData(Attribute attribute) {
        ParameterMetaData parameterMetaData = new ParameterMetaData();
        parameterMetaData.putAttribute(Constants.TYPE, getXsdType(attribute.dataType().toString()));
        parameterMetaData.putAttribute(Constants.OPTIONAL, Boolean.toString(!attribute.required()));
        if (!(attribute.defaultValue().equals("\u0000"))) {
            parameterMetaData.putAttribute(Constants.DEFAULT_VALUE, attribute.defaultValue());
        }
        parameterMetaData.putAttribute(Constants.KEY, Boolean.toString(attribute.key()));
        //FIXME - Currently, Attribute class does not provide acceptable values.
        //parameterMetaData.putAttribute(Contants.ACCEPTABLE_VALUES,
        //    getXsdType(attribute.acceptableValues()));

        return parameterMetaData;
    }

    //rename the given input parameter
    private static boolean renameParameter(Map<String, String> data,
            String parameterToRename, String newName) {
        if ((data.containsKey(parameterToRename))) {
            String value = data.get(parameterToRename);
            data.remove(parameterToRename);
            data.put(newName, value);
            return true;
        }
        return false;
    }

    //returns true only if the request is from browser
    private static boolean isBrowser(HttpHeaders requestHeaders) {
        boolean isClientAcceptsHtml = false;
        MediaType media = requestHeaders.getMediaType();
        java.util.List<String> acceptHeaders =
                requestHeaders.getRequestHeader(HttpHeaders.ACCEPT);

        for (String header : acceptHeaders) {
            if (header.contains(MediaType.TEXT_HTML)) {
                isClientAcceptsHtml = true;
                break;
            }
        }

        if (media != null) {
            if ((media.equals(MediaType.APPLICATION_FORM_URLENCODED_TYPE))
                    && (isClientAcceptsHtml)) {
                return true;
            }
        }

        return false;
    }

    private static String getXsdType(String javaType) {
        if (javaType.indexOf(Constants.JAVA_STRING_TYPE) != -1) {
            return Constants.XSD_STRING_TYPE;
        }
        if (javaType.indexOf(Constants.JAVA_BOOLEAN_TYPE) != -1) {
            return Constants.XSD_BOOLEAN_TYPE;
        }
        if (javaType.indexOf(Constants.JAVA_INT_TYPE) != -1) {
            return Constants.XSD_INT_TYPE;
        }
        if (javaType.indexOf(Constants.JAVA_PROPERTIES_TYPE) != -1) {
            return Constants.XSD_PROPERTIES_TYPE;
        }
        return javaType;
    }

    public static String getAttributeMethodName(String attributeName) {
        return methodNameFromDtdName(attributeName, "get");
    }

    private static String split(String lookback, String lookahead) {
        return "((?<=" + lookback + ")(?=" + lookahead + "))";
    }

    private static String or(String... tokens) {
        StringBuilder buf = new StringBuilder();
        for (String t : tokens) {
            if (buf.length() > 0) {
                buf.append('|');
            }
            buf.append(t);
        }
        return buf.toString();
    }

    public static String convertToXMLName(String name) {
        // tokenize by finding 'x|X' and 'X|Xx' then insert '-'.
        StringBuilder buf = new StringBuilder(name.length() + 5);
        for (String t : TOKENIZER.split(name)) {
            if (buf.length() > 0) {
                buf.append('-');
            }
            buf.append(t.toLowerCase(Locale.US));
        }
        return buf.toString();
    }

    /**
     * @return A copy of given
     * <code>sourceData</code> where key of each entry from it is converted to
     * xml name
     */
    public static HashMap<String, String> translateCamelCasedNamesToXMLNames(Map<String, String> sourceData) {
        HashMap<String, String> convertedData = new HashMap<String, String>(sourceData.size());
        for (Map.Entry<String, String> entry : sourceData.entrySet()) {
            String camelCasedKeyName = entry.getKey();
            String xmlKeyName = convertToXMLName(camelCasedKeyName);
            convertedData.put(xmlKeyName, entry.getValue());
        }
        return convertedData;
    }

    /*
     * we try to prefer html by default for all browsers (safari, chrome,
     * firefox). Same if the request is asking for "*" among all the possible
     * AcceptableMediaTypes
     */
    public static String getResultType(HttpHeaders requestHeaders) {
        String result = "html";
        String firstOne = null;
        List<MediaType> lmt = requestHeaders.getAcceptableMediaTypes();
        for (MediaType mt : lmt) {
            if (mt.getSubtype().equals("html")) {
                return result;
            }
            if (mt.getSubtype().equals("*")) {
                return result;
            }
            if (firstOne == null) { //default to the first one if many are there.
                firstOne = mt.getSubtype();
            }
        }

        if (firstOne != null) {
            return firstOne;
        } else {
            return result;
        }
    }

    public static Map buildMethodMetadataMap(MethodMetaData mmd) { // yuck
        Map<String, Map> map = new TreeMap<String, Map>();
        Set<String> params = mmd.parameters();
        Iterator<String> iterator = params.iterator();
        String param;
        while (iterator.hasNext()) {
            param = iterator.next();
            ParameterMetaData parameterMetaData = mmd.getParameterMetaData(param);
            map.put(param, processAttributes(parameterMetaData.attributes(), parameterMetaData));
        }

        return map;
    }

    private static Map<String, String> processAttributes(Set<String> attributes, ParameterMetaData parameterMetaData) {
        Map<String, String> pmdm = new HashMap<String, String>();

        Iterator<String> attriter = attributes.iterator();
        String attributeName;
        while (attriter.hasNext()) {
            attributeName = attriter.next();
            String attributeValue = parameterMetaData.getAttributeValue(attributeName);
            pmdm.put(attributeName, attributeValue);
        }

        return pmdm;
    }

    /*
     * REST can now be configured via RestConfig to show or hide the deprecated
     * elements and attributes @return true if this model is deprecated
     */
    static public boolean isDeprecated(ConfigModel model) {
        try {
            Class<? extends ConfigBeanProxy> cbp = (Class<? extends ConfigBeanProxy>) model.classLoaderHolder.loadClass(model.targetTypeName);
            Deprecated dep = cbp.getAnnotation(Deprecated.class);
            return dep != null;
        } catch (MultiException e) {
            //e.printStackTrace();
        }
        return false;

    }

    public static Map<String, String> getResourceLinks(Dom dom, UriInfo uriInfo, boolean canShowDeprecated) {
        Map<String, String> links = new TreeMap<String, String>();

        for (String elementName : dom.model.getElementNames()) { //for each element
            if (elementName.equals("*")) {
                ConfigModel.Node node = (ConfigModel.Node) dom.model.getElement(elementName);
                ConfigModel childModel = node.getModel();
                List<ConfigModel> lcm = getRealChildConfigModels(childModel, dom.document);

                Collections.sort(lcm, new ConfigModelComparator());
                if (lcm != null) {
                    for (ConfigModel cmodel : lcm) {
                        if ((!isDeprecated(cmodel) || canShowDeprecated)) {
                            links.put(cmodel.getTagName(), ProviderUtil.getElementLink(uriInfo, cmodel.getTagName()));
                        }
                    }
                }

            } else {
                ConfigModel.Property childElement = dom.model.getElement(elementName);
                boolean deprec = false;

                if (childElement instanceof ConfigModel.Node) {
                    ConfigModel.Node node = (ConfigModel.Node) childElement;
                    deprec = isDeprecated(node.getModel());
                }
                for (String annotation : childElement.getAnnotations()) {
                    if (annotation.equals(Deprecated.class.getName())) {
                        deprec = true;
                    }
                }
                if ((!deprec || canShowDeprecated)) {
                    links.put(elementName, ProviderUtil.getElementLink(uriInfo, elementName));
                }

            }
        }

        String beanName = getUnqualifiedTypeName(dom.model.targetTypeName);

        for (CommandResourceMetaData cmd : CommandResourceMetaData.getCustomResourceMapping(beanName)) {
            links.put(cmd.resourcePath, ProviderUtil.getElementLink(uriInfo, cmd.resourcePath));
        }

        return links;
    }

    /**
     * @param qualifiedTypeName
     * @return unqualified type name for given qualified type name. This is a
     * substring of qualifiedTypeName after last "."
     */
    public static String getUnqualifiedTypeName(String qualifiedTypeName) {
        return qualifiedTypeName.substring(qualifiedTypeName.lastIndexOf(".") + 1, qualifiedTypeName.length());
    }

    public static boolean isOnlyATag(ConfigModel model) {
        return (model.getAttributeNames().isEmpty()) && (model.getElementNames().isEmpty());
    }

    public static List<ConfigModel> getRealChildConfigModels(ConfigModel childModel, DomDocument domDocument) {
        List<ConfigModel> retlist = new ArrayList<ConfigModel>();
        try {
            Class<?> subType = childModel.classLoaderHolder.loadClass(childModel.targetTypeName);
            List<ConfigModel> list = domDocument.getAllModelsImplementing(subType);

            if (list != null) {
                for (ConfigModel el : list) {
                    if (isOnlyATag(el)) { //this is just a tag element
                        retlist.addAll(getRealChildConfigModels(el, domDocument));
                    } else {
                        retlist.add(el);
                    }
                }
            } else {//https://glassfish.dev.java.net/issues/show_bug.cgi?id=12654
                if (!isOnlyATag(childModel)) { //this is just a tag element
                    retlist.add(childModel);
                }

            }
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
        return retlist;
    }

    /**
     * @param model
     * @return name of the key attribute for the given model.
     */
    private static String getKey(Dom model) {
        String key = model.getKey();
        if (key == null) {
            for (String s : model.getAttributeNames()) {//no key, by default use the name attr
                if (s.equals("name")) {
                    key = model.attribute(s);
                }
            }
            if (key == null)//nothing, so pick the first one
            {
                Set<String> attributeNames = model.getAttributeNames();
                if (!attributeNames.isEmpty()) {
                    key = model.attribute(attributeNames.iterator().next());
                } else {
                    //TODO carried forward from old generator. Should never reach here. But we do for ConfigExtension and WebModuleConfig
                    key = "ThisIsAModelBug:NoKeyAttr"; //no attr choice fo a key!!! Error!!!
                    key = "";
                }

            }
        }
        return key;
    }

    public static Map<String, String> getResourceLinks(List<Dom> proxyList, UriInfo uriInfo) {
        Map<String, String> links = new TreeMap<String, String>();
        Collections.sort(proxyList, new DomConfigurator());
        for (Dom proxy : proxyList) { //for each element
            try {
                links.put(
                        getKey(proxy),
                        getElementLink(uriInfo, getKey(proxy)));
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

        return links;
    }

    public static List<Map<String, String>> getCommandLinks(String[][] commandResourcesPaths) {
        List<Map<String, String>> commands = new ArrayList<Map<String, String>>();
        for (String[] array : commandResourcesPaths) {
            Map<String, String> command = new HashMap<String, String>();
            command.put("command", array[0]);
            command.put("method", array[1]);
            command.put("path", array[2]);
            commands.add(command);
        }

        return commands;
    }

    public static void addMethodMetaData(ActionReport ar, Map<String, MethodMetaData> mmd) {
        List<Map> methodMetaData = new ArrayList<Map>();

        MethodMetaData getMetaData = mmd.get("GET");
        methodMetaData.add(new HashMap() {

            {
                put("name", "GET");
            }
        });
        if (getMetaData != null) { //are they extra params for a GET command?
            Map<String, Object> getMetaDataMap = new HashMap<String, Object>();
            if (getMetaData.sizeParameterMetaData() > 0) {
                getMetaDataMap.put(MESSAGE_PARAMETERS, buildMethodMetadataMap(getMetaData));
            }
            methodMetaData.add(getMetaDataMap);
        }

        MethodMetaData postMetaData = mmd.get("POST");
        Map<String, Object> postMetaDataMap = new HashMap<String, Object>();
        if (postMetaData != null) {
            postMetaDataMap.put("name", "POST");
//            if (postMetaData.sizeQueryParamMetaData() > 0) {
//                postMetaDataMap.put(QUERY_PARAMETERS, buildMethodMetadataMap(postMetaData, true));
//            }
            if (postMetaData.sizeParameterMetaData() > 0) {
                postMetaDataMap.put(MESSAGE_PARAMETERS, buildMethodMetadataMap(postMetaData));
            }
            methodMetaData.add(postMetaDataMap);
        }

        MethodMetaData deleteMetaData = mmd.get("DELETE");
        if (deleteMetaData != null) {
            Map<String, Object> deleteMetaDataMap = new HashMap<String, Object>();

            deleteMetaDataMap.put("name", "DELETE");
            deleteMetaDataMap.put(MESSAGE_PARAMETERS, buildMethodMetadataMap(deleteMetaData));
            methodMetaData.add(deleteMetaDataMap);
        }

        ar.getExtraProperties().put("methods", methodMetaData);
    }

    public static synchronized RestConfig   getRestConfig(ServiceLocator habitat) {
        if (restConfig == null) {
            if (habitat == null) {
                return null;
            }
            Domain domain = Globals.getDefaultBaseServiceLocator().getService(Domain.class);
            if (domain != null) {
                Config config = domain.getConfigNamed("server-config");
                if (config != null) {
                    restConfig = config.getExtensionByType(RestConfig.class);

                }
            }
        }

        return restConfig;
    }
    /*
     * returns true if the HTML viewer displays the deprecated elements or
     * attributes of a config bean
     */

    public static boolean canShowDeprecatedItems(ServiceLocator habitat) {

        RestConfig rg = getRestConfig(habitat);
        if ((rg != null) && (rg.getShowDeprecatedItems().equalsIgnoreCase("true"))) {
            return true;
        }
        return false;
    }

    /**
     * Authenticate the given req as originated from given remoteHost against
     * admin realm.
     *
     * @return subject identifying the user/client
     */
    public static Subject authenticateViaAdminRealm(ServiceLocator habitat, Request req, String remoteHost) throws LoginException, IOException {
        Subject subject = null;
        final AdminAccessController authenticator = habitat.getService(AdminAccessController.class);
        if (authenticator != null) {
            // This is temporary workaround for a Grizzly issue that prohibits uploading (deploy)  files larger than 2MB when secure admin is enabled.
            // The workaround will not be required in trunk when the corresponding Grizzly issue is fixed.
            // Please see http://java.net/jira/browse/GLASSFISH-16665 for details
            // The workaround duplicates code from AdminAdapter.

            subject = authenticator.loginAsAdmin(req, remoteHost);
        }
        return subject;
    }

    /**
     * Indicates whether the subject can perform the action on the resource.
     *
     * @param habitat ServiceLocator for finding services
     * @param subject the Subject to be qualified
     * @param resource the resource affected by the action
     * @param action the action being attempted by the subject on the resource
     * @return true if the subject is allowed to perform the action, false otherwise
     * @throws URISyntaxException
     */
    public static boolean isAuthorized(final ServiceLocator habitat, final Subject subject, final String resource, final String action) throws URISyntaxException {
        final AuthorizationService authorizationSvc =
            AccessController.doPrivileged(
                    new PrivilegedLookup<AuthorizationService>(habitat, AuthorizationService.class));
        return authorizationSvc.isAuthorized(subject, new URI("admin", resource, null), action);
    }
}
