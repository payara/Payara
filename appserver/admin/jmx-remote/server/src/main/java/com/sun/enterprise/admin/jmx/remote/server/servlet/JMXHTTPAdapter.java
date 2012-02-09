/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.admin.jmx.remote.server.servlet;

import com.sun.enterprise.module.ModulesRegistry;
import com.sun.enterprise.module.common_impl.LogHelper;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.v3.common.ActionReporter;
import com.sun.enterprise.v3.common.HTMLActionReporter;
import com.sun.enterprise.v3.common.PropsFileActionReporter;
import com.sun.logging.LogDomains;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.tcp.http11.InternalOutputBuffer;
import org.apache.commons.beanutils.ConvertUtils;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Async;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.container.Adapter;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.ComponentException;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.component.InjectionManager;
import org.jvnet.hk2.component.UnsatisfiedDepedencyException;

import java.io.ByteArrayOutputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URLDecoder;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.inject.Inject;

/**
 * Service the JMX HTTP clients talking to GF v3 using JMX Service URL...
 * @author ne110415
 */
@Service
public class JMXHTTPAdapter implements Adapter {

    public final static String PREFIX_URI="/web1/remotejmx";
    public final static Logger logger = LogDomains.getLogger(ServerEnvironmentImpl.class, LogDomains.ADMIN_LOGGER);
    public final static LocalStringManagerImpl adminStrings = new LocalStringManagerImpl(JMXHTTPAdapter.class);

    @Inject
    Habitat habitat;

    @Inject
    ModulesRegistry modulesRegistry;


    /**
     * Call the service method, and notify all listeners
     *
     * @exception Exception if an error happens during handling of
     *   the request. Common errors are:
     *   <ul><li>IOException if an input/output error occurs and we are
     *   processing an included servlet (otherwise it is swallowed and
     *   handled by the top level error handler mechanism)
     *       <li>ServletException if a servlet throws an exception and
     *  we are processing an included servlet (otherwise it is swallowed
     *  and handled by the top level error handler mechanism)
     *  </ul>
     *  Tomcat should be able to handle and log any other exception ( including
     *  runtime exceptions )
     */
    public void service(Request req, Response res)
        throws Exception {

        LogHelper.getDefaultLogger().info("New HTTP JMX adapter !");
        LogHelper.getDefaultLogger().info("Received something on " + req.requestURI());
        LogHelper.getDefaultLogger().info("QueryString = " + req.queryString());

        // so far, I only use HTMLActionReporter, but I should really look at
        // the request client.
        ActionReporter report;
        if (req.getHeader("User-Agent").startsWith("hk2")) {
            report = new PropsFileActionReporter();
        } else {
            report = new HTMLActionReporter();
        }

        doCommand(req, report);

        InternalOutputBuffer outputBuffer = (InternalOutputBuffer) res.getOutputBuffer();
        res.setStatus(200);
        res.setContentType("text/html");
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        report.writeReport(bos);
        res.setContentLength(bos.size());
        outputBuffer.flush();
        outputBuffer.realWriteBytes(bos.toByteArray(), 0, bos.size());

        res.finish();
    }

    private void doCommand(Request req, ActionReport report) {

        String requestURI = req.requestURI().toString();
        if (!requestURI.startsWith(PREFIX_URI)) {
            String msg = adminStrings.getLocalString("adapter.panic",
                    "Wrong request landed in AdminAdapter {0}", requestURI);
            report.setMessage(msg);
            LogHelper.getDefaultLogger().info(msg);
            return;
        }
        String command = requestURI.substring(PREFIX_URI.length());

        // extract parameters...
        final Properties parameters = new Properties();
        String queryString = req.queryString().toString();
        StringTokenizer stoken = new StringTokenizer(queryString, "?");
        while (stoken.hasMoreTokens()) {
            String token = stoken.nextToken();
            if (token.indexOf("=")==-1)
                continue;
            String paramName = token.substring(0, token.lastIndexOf("="));
            String value = token.substring(token.lastIndexOf("=")+1);
            try {
                value = URLDecoder.decode(value, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                logger.log(Level.WARNING, adminStrings.getLocalString("adapter.param.decode",
                        "Cannot decode parameter {0} = {1}"));
            }
            parameters.setProperty(paramName, value);
        }

        // Dump parameters...
        if (logger.isLoggable(Level.FINER)) {
            for (Object key : parameters.keySet()) {
              logger.finer("Key " + key + " = " + parameters.getProperty((String) key));
            }
        }

        doCommand(command, parameters, report);

    }

    public void doCommand(final String commandName, final Properties parameters, final ActionReport report) {

        final AdminCommand handler = getCommand(commandName, report, logger);
        if (handler==null) {
            return;
        }

        if (parameters.size()==1 && parameters.get("help")!=null) {
            usage(commandName, handler, report);
            return;
        }
        report.setActionDescription(commandName + " AdminCommand");

        final AdminCommandContext context = new AdminCommandContext(
                LogDomains.getLogger(ServerEnvironmentImpl.class, LogDomains.ADMIN_LOGGER),
                report, parameters);

        // initialize the injector.
        InjectionManager injectionMgr =  new InjectionManager<Param>() {

            protected boolean isOptional(Param annotation) {
                return annotation.optional();
            }

            protected Object getValue(Object component, AnnotatedElement target, Class type) throws ComponentException {
                // look for the name in the list of parameters passed.
                Param param = target.getAnnotation(Param.class);
                if (param.primary()) {
                    // this is the primary parameter for the command
                    String value = parameters.getProperty("DEFAULT");
                    if (value!=null) {
                        return value;
                    }
                }
                return ConvertUtils.convert((String) parameters.get(getParamName(param, target)), type);
            }
        };

        LocalStringManagerImpl localStrings = new LocalStringManagerImpl(handler.getClass());

        // Let's get the command i18n key
        I18n i18n = handler.getClass().getAnnotation(I18n.class);
        String i18n_key = "";
        if (i18n!=null) {
            i18n_key = i18n.value();
        }

        // inject
        try {
            injectionMgr.inject(handler, Param.class);
        } catch (UnsatisfiedDepedencyException e) {
            Param param = e.getUnsatisfiedElement().getAnnotation(Param.class);
            String paramName = getParamName(param, e.getUnsatisfiedElement());
            String paramDesc = getParamDescription(localStrings, i18n_key, paramName, e.getUnsatisfiedElement());

            String errorMsg;
            if (paramDesc!=null) {
                errorMsg = adminStrings.getLocalString("admin.param.missing",
                        "{0} command requires the {1} parameter : {2}", commandName, paramName, paramDesc);
            } else {
                errorMsg = adminStrings.getLocalString("admin.param.missing.nodesc",
                        "{0} command requires the {1} parameter", commandName, paramName);
            }
            logger.severe(errorMsg);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(errorMsg);
            report.setFailureCause(e);
            return;
        } catch (ComponentException e) {
            logger.severe(e.getMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(e.getMessage());
            report.setFailureCause(e);
        }

        // the command may be an asynchronous command, so we need to check
        // for the @Async annotation.
        Async async = handler.getClass().getAnnotation(Async.class);
        if (async==null) {
            try {
                handler.execute(context);
            } catch(Throwable e) {
                logger.log(Level.SEVERE,
                        adminStrings.getLocalString("adapter.exception","Exception in command execution : ", e), e);
            }
        } else {
            Thread t = new Thread() {
                public void run() {
                    try {
                        handler.execute(context);
                    } catch (RuntimeException e) {
                        logger.log(Level.SEVERE,e.getMessage(), e);
                    }
                }
            };
            t.setPriority(async.priority());
            t.start();
            report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
            report.setMessage(
                    adminStrings.getLocalString("adapter.command.launch", "{0} launch successful", commandName));
        }
    }

    public void usage(String commandName, AdminCommand command, ActionReport report) {

        report.setActionDescription(commandName + " help");
        LocalStringManagerImpl localStrings = new LocalStringManagerImpl(command.getClass());

        // Let's get the command i18n key
        I18n i18n = command.getClass().getAnnotation(I18n.class);
        String i18nKey = "";
        if (i18n!=null) {
            i18nKey = i18n.value();
        }
        report.setMessage(localStrings.getLocalString(i18nKey, null));

        for (Field f : command.getClass().getDeclaredFields()) {
            addParamUsage(report, localStrings, i18nKey, f);
        }
        for (Method m : command.getClass().getDeclaredMethods()) {
            addParamUsage(report, localStrings, i18nKey, m);
        }
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }

    private void addParamUsage(ActionReport report, LocalStringManagerImpl localStrings, String i18nKey, AnnotatedElement annotated) {

        Param param = annotated.getAnnotation(Param.class);
        if (param!=null) {
            // this is a param.
            String paramName = getParamName(param, annotated);
            report.getTopMessagePart().addProperty(paramName, getParamDescription(localStrings, i18nKey, paramName, annotated));
        }
    }

    private String getParamDescription(LocalStringManagerImpl localStrings, String i18nKey, String paramName, AnnotatedElement annotated) {

        I18n i18n = annotated.getAnnotation(I18n.class);
        String paramDesc;
        if (i18n==null) {
            paramDesc = localStrings.getLocalString(i18nKey+"."+paramName, null);
        } else {
            paramDesc = localStrings.getLocalString(i18n.value(), null);
        }
        if (paramDesc==null) {
            paramDesc = adminStrings.getLocalString("adapter.nodesc", "no description provided");
        }
        return paramDesc;
    }

    private String getParamName(Param param, AnnotatedElement annotated) {
        if (param.name().equals("")) {
            if (annotated instanceof Field) {
                return ((Field) annotated).getName();
            }
            if (annotated instanceof Method) {
                return ((Method) annotated).getName().substring(3).toLowerCase();
            }
        } else {
            return param.name();
        }
        return "";
    }

    /**
     * Finish the response and recycle the request/response tokens. Base on
     * the connection header, the underlying socket transport will be closed
     */
    public void afterService(Request req, Response res) throws Exception {

    }


    /**
     * Notify all container event listeners that a particular event has
     * occurred for this Adapter.  The default implementation performs
     * this notification synchronously using the calling thread.
     *
     * @param type Event type
     * @param data Event data
     */
    public void fireAdapterEvent(String type, Object data) {

    }

    /**
     * Returns the context root for this adapter
     *
     * @return context root
     */
    public String getContextRoot() {
        return PREFIX_URI;
    }

    /**
     * Return Command handlers from the lookup or if not found in the lookup,
     * look at META-INF/services implementations and add them to the lookup
     * @param commandName the request handler's command name
     * @param report the reporting facility
     * @return the admin command handler if found
     *
     */
    private AdminCommand getCommand(String commandName, ActionReport report, Logger logger) {

        AdminCommand command = null;
        try {
            command = habitat.getComponent(AdminCommand.class, commandName);
        } catch(ComponentException e) {
        }
        if (command==null) {
            String msg = adminStrings.getLocalString("adapter.command.notfound", "Command {0} not found", commandName);
            report.setMessage(msg);
            LogHelper.getDefaultLogger().info(msg);
        }
        return command;
    }


}
