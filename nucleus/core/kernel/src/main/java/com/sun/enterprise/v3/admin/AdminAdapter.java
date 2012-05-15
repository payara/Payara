/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.v3.admin;

import java.security.Principal;
import java.util.Set;

import com.sun.enterprise.config.serverbeans.AdminService;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.SecureAdmin;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.module.ModulesRegistry;
import com.sun.enterprise.module.common_impl.LogHelper;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.uuid.UuidGenerator;
import com.sun.enterprise.util.uuid.UuidGeneratorImpl;
import com.sun.logging.LogDomains;
import org.glassfish.admin.payload.PayloadImpl;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.*;
import org.glassfish.api.event.EventListener;
import org.glassfish.api.container.Adapter;
import org.glassfish.grizzly.http.Cookie;
import org.jvnet.hk2.annotations.Optional;
import org.jvnet.hk2.component.PostConstruct;

import java.net.InetAddress;
import java.net.URLDecoder;
import java.util.StringTokenizer;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import com.sun.enterprise.util.SystemPropertyConstants;

import java.net.HttpURLConnection;
import com.sun.enterprise.universal.GFBase64Decoder;
import com.sun.enterprise.v3.admin.adapter.AdminEndpointDecider;

import java.io.*;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.CountDownLatch;

import org.glassfish.api.event.EventTypes;
import org.glassfish.api.event.Events;
import org.glassfish.api.event.RestrictTo;
import org.glassfish.grizzly.http.server.HttpHandler;
import org.glassfish.grizzly.http.server.Request;
import org.glassfish.grizzly.http.server.Response;
import org.glassfish.grizzly.http.server.StaticHttpHandler;
import org.glassfish.internal.api.AdminAccessController;
import org.glassfish.internal.api.Privacy;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.server.ServerEnvironmentImpl;
import javax.inject.Inject;
import javax.inject.Named;

import org.jvnet.hk2.component.BaseServiceLocator;
/**
 * Listen to admin commands...
 * @author dochez
 */
public abstract class AdminAdapter extends StaticHttpHandler implements Adapter, PostConstruct, EventListener {

    public final static String VS_NAME="__asadmin";
    public final static String PREFIX_URI = "/" + VS_NAME;
    private final static LocalStringManagerImpl adminStrings = new LocalStringManagerImpl(AdminAdapter.class);
    private final static Logger aalogger = LogDomains.getLogger(AdminAdapter.class, LogDomains.ADMIN_LOGGER);
    private final static String GET = "GET";
    private final static String POST = "POST";
    private static final GFBase64Decoder decoder = new GFBase64Decoder();
    private static final String BASIC = "Basic ";

    private static final String SET_COOKIE2_HEADER = "Set-Cookie2";

    public static final String SESSION_COOKIE_NAME = "JSESSIONID";
    private static final String QUERY_STRING_SEPARATOR = "&";

    private static final String[] authRelatedHeaderNames = {
        SecureAdmin.Util.ADMIN_INDICATOR_HEADER_NAME,
        SecureAdmin.Util.ADMIN_ONE_TIME_AUTH_TOKEN_HEADER_NAME};

    @Inject
    ModulesRegistry modulesRegistry;

    @Inject
    CommandRunnerImpl commandRunner;

    @Inject
    ServerEnvironmentImpl env;

    @Inject
    Events events;
    
    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    Config config;

    private AdminEndpointDecider epd = null;
    
    @Inject
    ServerContext sc;

    @Inject
    BaseServiceLocator habitat;

    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    volatile AdminService as;

    @Inject
    volatile Domain domain;

    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    private volatile Server server;
    
    @Inject @Optional
    AdminAccessController authenticator;
   
    final Class<? extends Privacy> privacyClass;

    private boolean isRegistered = false;
            
    CountDownLatch latch = new CountDownLatch(1);


    protected AdminAdapter(Class<? extends Privacy> privacyClass) {
        super((Set) null);
        this.privacyClass = privacyClass;
    }

    @Override
    public final HttpHandler getHttpService() {
        return this;
    }

    @Override
    public void postConstruct() {
        events.register(this);
        
        epd = new AdminEndpointDecider(config, aalogger);
        addDocRoot(env.getProps().get(SystemPropertyConstants.INSTANCE_ROOT_PROPERTY) + "/asadmindocroot/");
    }

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
    @Override
    public void onMissingResource(Request req, Response res) {

        LogHelper.getDefaultLogger().log(Level.FINER, "Received something on {0}", req.getRequestURI());
        LogHelper.getDefaultLogger().log(Level.FINER, "QueryString = {0}", req.getQueryString());

        String requestURI = req.getRequestURI();
    /*    if (requestURI.startsWith("/__asadmin/ADMINGUI")) {
            super.service(req, res);

        }*/
        ActionReport report = getClientActionReport(requestURI, req);
        // remove the qualifier if necessary
        if (requestURI.indexOf('.')!=-1) {
            requestURI = requestURI.substring(0, requestURI.indexOf('.'));
        }

        Payload.Outbound outboundPayload = PayloadImpl.Outbound.newInstance();

        try {
            if (!latch.await(20L, TimeUnit.SECONDS)) {
                report = getClientActionReport(req.getRequestURI(), req);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage("V3 cannot process this command at this time, please wait");            
            } else {
                if (!authenticate(req, report, res))
                    return;
                report = doCommand(requestURI, req, report, outboundPayload);
            }
        } catch(InterruptedException e) {
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage("V3 cannot process this command at this time, please wait");                        
        } catch (Exception e) {
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage("Exception while processing command: " + e);
        }
        
        try {
            res.setStatus(200);
            /*
             * Format the command result report into the first part (part #0) of
             * the outbound payload and set the response's content type based
             * on the payload's.  If the report is the only part then the
             * stream will be written as content type text/something and
             * will contain only the report.  If the payload already has
             * content - such as files to be downloaded, for example - then the
             * content type of the payload reflects its multi-part nature and
             * an implementation-specific content type will be set in the response.
             */
            ByteArrayOutputStream baos = new ByteArrayOutputStream(1024);
            report.writeReport(baos);
            ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
            final Properties reportProps = new Properties();
            reportProps.setProperty("data-request-type", "report");
            outboundPayload.addPart(0, report.getContentType(), "report",
                    reportProps, bais);
            res.setContentType(outboundPayload.getContentType());
            String commandName = req.getRequestURI().substring(getContextRoot().length() + 1);
            if (! hasCookieHeaders(req) && isSingleInstanceCommand(commandName)) {
               res.addCookie(new Cookie(SESSION_COOKIE_NAME, getSessionID()));
            }
            outboundPayload.writeTo(res.getOutputStream());
            res.getOutputStream().flush();
            res.finish();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * This method checks if the request has a Cookie header and
     * if the instance name serving the request is the same as the
     * jvmRoute information
     * @param req Request to examine the Cookie header
     * @return true if the Cookie header is set and the jvmRoute information is correct
     *
     */
    public boolean hasCookieHeaders(Request req) {
        for (String header : req.getHeaders("Cookie")){
            if (header.contains(SESSION_COOKIE_NAME) &&
               (header.substring(header.lastIndexOf(".")+1).equals(server.getName()))) {
                return true;
            }

        }
        return false;

    }

    /**
     * This method checks if this command has @ExecuteOn annotation with
     * RuntimeType.SINGle_INSTANCE
     * @param commandName  the command which is executed
     * @return  true only if @ExecuteOn has RuntimeType.SINGLE_INSTANCE false for
     * other cases
     */
    public boolean isSingleInstanceCommand(String commandName) {

        CommandModel model = commandRunner.getModel(commandName,aalogger) ;
        ExecuteOn executeOn = model.getClusteringAttributes();
        if ((executeOn != null) && (executeOn.value().length ==1) &&
                executeOn.value()[0].equals(RuntimeType.SINGLE_INSTANCE)) {
            return true;

        }
        return false;
    }

    /**
     * This will create a unique SessionId to be added to the Set-Cookie header
     * @return JSESSIONID string
     */
    public String getSessionID() {
        UuidGenerator uuidGenerator = new UuidGeneratorImpl();
        String sessionId = uuidGenerator.generateUuid();
        StringBuffer sb = new StringBuffer();
        sb.append(sessionId).append(".").append(server.getName());

        return sb.toString();

    }

    public AdminAccessController.Access authenticate(Request req) throws Exception {
        String[] up = getUserPassword(req);
        String user = up[0];
        String password = up.length > 1 ? up[1] : "";
        if (authenticator != null) {
            /*
             * If an admin request includes a large payload and secure admin is
             * enabled and the request does NOT include a client cert, then
             * the getUsePrincipal invocation can cause problems.  When secure
             * admin is enabled, we set the admin listener configuration on the DAS
             * to suppress renegotiation for the cert if the client provided none.  The
             * GlassFish processes, when secure admin is enabled, will provide
             * their client certs to each other to start with, so no renegotiation
             * would be needed because the client cert will be available 
             * immediately to the server.  By suppressing renegotiation this 
             * way we prevent the problem in which renegotiation interrupts
             * a large payload.
             */
            final Principal sslPrincipal = req.getUserPrincipal();
            return authenticator.loginAsAdmin(user, password, as.getAuthRealmName(),
                    req.getRemoteHost(), authRelatedHeaders(req), sslPrincipal);
        }
        return AdminAccessController.Access.FULL;   //if the authenticator is not available, allow all access - per Jerome
    }
    
    private Map<String,String> authRelatedHeaders(final Request gr) {
        final Map<String,String> result = new HashMap<String,String>();
        for (String authRelatedHeaderName : authRelatedHeaderNames) {
            final String value = gr.getHeader(authRelatedHeaderName);
            if (value != null) {
                result.put(authRelatedHeaderName, value);
            }
        }
        return result;
    }

    /** A convenience method to extract user name from a request. It assumes the HTTP Basic Auth.
     *
     * @param req instance of Request
     * @return a two-element string array. If Auth header exists and can be correctly decoded, returns the user name
     *   and password as the two elements. If any error occurs or if the header does not exist, returns an array with
     *   two blank strings. Never returns a null.
     * @throws IOException in case of error with decoding the buffer (HTTP basic auth)
     */
    public static String[] getUserPassword(Request req) throws IOException {
        //implementation note: other adapters make use of this method
        String authHeader = req.getHeader("Authorization");
        if (authHeader == null) {
            return new String[]{"", ""};
        }
        String enc = authHeader.substring(BASIC.length());
        String dec = new String(decoder.decodeBuffer(enc));
        int i = dec.indexOf(':');
        if (i < 0)
            return new String[] { "", "" };
        return new String[] { dec.substring(0, i), dec.substring(i + 1) };
    }

    private boolean authenticate(Request req, ActionReport report, Response res)
            throws Exception {
        
        AdminAccessController.Access access = authenticate(req);
        /*
         * Admin requests through this adapter are assumed to change the
         * configuration, which means the access granted needs to be FULL.
         */
        switch (access)  {
            case FULL:
                return true;

            case FORBIDDEN:
                /*
                 * The request authenticated OK but it is remote and this is the DAS;
                 * that's why FORBIDDEN rather than FULL came back.
                 * 
                 * For user-friendliness respond with Forbidden.
                 */
                reportAuthFailure(res, report,
                        "adapter.auth.remoteReqSecAdminOff",
                        "Remote configuration is currently disabled",
                        HttpURLConnection.HTTP_FORBIDDEN);

                break;

            case NONE:
//                if (env.isDas()) {
                    reportAuthFailure(res, report, "adapter.auth.userpassword",
                        "Invalid user name or password",
                        HttpURLConnection.HTTP_UNAUTHORIZED,
                        "WWW-Authenticate", "BASIC");
//                } else {
//                    reportAuthFailure(res, report, "adapter.auth.notOnInstance",
//                            "Configuration access to an instance is not allowed; please connect to the domain admin server instead to make configuration changes",
//                        HttpURLConnection.HTTP_FORBIDDEN);
//                }
                break;
                
            default:
                final String msg = adminStrings.getLocalString("admin.adapter.unkAuth", 
                        "Unknown admin access {0} returned; expected one of {1}",
                        access.name(), AdminAccessController.Access.values());
                throw new IllegalStateException(msg);

        }

        return access == AdminAccessController.Access.FULL;
    }

    private void reportAuthFailure(final Response res,
            final ActionReport report,
            final String msgKey,
            final String msg,
            final int httpStatus) throws IOException {
        reportAuthFailure(res, report, msgKey, msg, httpStatus, null, null);
    }

    private void reportAuthFailure(final Response res,
            final ActionReport report,
            final String msgKey,
            final String msg,
            final int httpStatus,
            final String headerName,
            final String headerValue) throws IOException {
        report.setActionExitCode(ActionReport.ExitCode.FAILURE);
        final String messageForResponse = adminStrings.getLocalString(msgKey, msg);
        report.setMessage(messageForResponse);
        report.setActionDescription("Authentication error");
        res.setStatus(httpStatus, messageForResponse);
        if (headerName != null) {
            res.setHeader(headerName, headerValue);
        }
        res.setContentType(report.getContentType());
        report.writeReport(res.getOutputStream());
        res.getOutputStream().flush();
        res.finish();
    }

    private ActionReport getClientActionReport(String requestURI, Request req) {


        ActionReport report=null;

        // first we look at the command extension (ie list-applications.[json | html | mf]
        if (requestURI.indexOf('.')!=-1) {
            String qualifier = requestURI.substring(requestURI.indexOf('.')+1);
            report = habitat.getComponent(ActionReport.class, qualifier);
        } else {
            String userAgent = req.getHeader("User-Agent");
            if (userAgent!=null)
                report = habitat.getComponent(ActionReport.class, userAgent.substring(userAgent.indexOf('/')+1));
            if (report==null) {
                String accept = req.getHeader("Accept");
                if (accept!=null) {
                    StringTokenizer st = new StringTokenizer(accept, ",");
                    while (report==null && st.hasMoreElements()) {
                        final String scheme=st.nextToken();
                        report = habitat.getComponent(ActionReport.class, scheme.substring(scheme.indexOf('/')+1));
                    }
                }
            }
        }
        if (report==null) {
            // get the default one.
            report = habitat.getComponent(ActionReport.class, "html");
        }
        return report;
    }

    protected abstract boolean validatePrivacy(AdminCommand command);

    private ActionReport doCommand(String requestURI, Request req, ActionReport report,
            Payload.Outbound outboundPayload) {

        if (!requestURI.startsWith(getContextRoot())) {
            String msg = adminStrings.getLocalString("adapter.panic",
                    "Wrong request landed in AdminAdapter {0}", requestURI);
            report.setMessage(msg);
            LogHelper.getDefaultLogger().info(msg);
            return report;
        }

        // wbn handle no command and no slash-suffix
        String command ="";
        if (requestURI.length() > getContextRoot().length() + 1)
            command = requestURI.substring(getContextRoot().length() + 1);

        // check for a command scope
        String scope = null;
        int ci = command.indexOf("/");
        if (ci != -1) {
            scope = command.substring(0, ci + 1);
            command = command.substring(ci + 1);
        }
        
        String qs = req.getQueryString();
        final ParameterMap parameters = extractParameters(qs);
        String passwordOptions = req.getHeader("X-passwords");
        if (passwordOptions != null) {
            decodePasswords(parameters, passwordOptions);
        }
        
        try {
            Payload.Inbound inboundPayload = PayloadImpl.Inbound
                .newInstance(req.getContentType(), req.getInputStream());
            if (aalogger.isLoggable(Level.FINE)) {
                aalogger.log(Level.FINE, "***** AdminAdapter {0}  *****", req.getMethod());
            }
            AdminCommand adminCommand = commandRunner.getCommand(scope, command, report, aalogger);
            if (adminCommand==null) {
                // maybe commandRunner already reported the failure?
                if (report.getActionExitCode() == ActionReport.ExitCode.FAILURE)
                    return report;
                String message =
                    adminStrings.getLocalString("adapter.command.notfound",
                        "Command {0} not found", command);
                // cound't find command, not a big deal
                aalogger.log(Level.FINE, message);
                report.setMessage(message);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return report;
            }
            if (validatePrivacy(adminCommand)) {
            //if (adminCommand.getClass().getAnnotation(Visibility.class).privacy().equals(visibility.privacy())) {
                // todo : needs to be changed, we should reuse adminCommand
                CommandRunner.CommandInvocation inv = commandRunner.getCommandInvocation(scope, command, report);
                inv.parameters(parameters).inbound(inboundPayload).outbound(outboundPayload).execute();
                try {
                    // note it has become extraordinarily difficult to change the reporter!
                    CommandRunnerImpl.ExecutionContext inv2 = (CommandRunnerImpl.ExecutionContext) inv;
                    report = inv2.report();
                }
                catch(Exception e) {
                }
            } else {
                report.failure( aalogger,
                                adminStrings.getLocalString("adapter.wrongprivacy",
                                    "Command {0} does not have {1} visibility",
                                    command, privacyClass.getSimpleName().toLowerCase(Locale.ENGLISH)),
                                null);
                return report;

            }
        } catch (Throwable t) {
            /*
             * Must put the error information into the report
             * for the client to see it.
             */
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(t);
            report.setMessage(t.getLocalizedMessage());
            report.setActionDescription("Last-chance AdminAdapter exception handler");
        }
        return report;
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
     * decode the parameters that were passed in the X-Passwords header
     * 
     * @params requestString value of the X-Passwords header
     * @returns a decoded requestString
     */
    void decodePasswords(ParameterMap pmap, final String requestString) {
        StringTokenizer stoken = new StringTokenizer(requestString == null ? "" : requestString, QUERY_STRING_SEPARATOR);
        while (stoken.hasMoreTokens()) {
            String token = stoken.nextToken();            
            if (token.indexOf("=") == -1) 
                continue;
            String paramName = token.substring(0, token.indexOf("="));
            String value = token.substring(token.indexOf("=") + 1);

            try {
                value = URLDecoder.decode(value, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                aalogger.log(Level.WARNING, adminStrings.getLocalString("adapter.param.decode",
                        "Cannot decode parameter {0} = {1}"));
                continue;
            }

            try {               
                value = new String(decoder.decodeBuffer(value));
            } catch (IOException e) {
                aalogger.log(Level.WARNING, adminStrings.getLocalString("adapter.param.decode",
                        "Cannot decode parameter {0} = {1}"));
                continue;
            }
            pmap.add(paramName, value);
        }
       
    }
     
    /**
     *  extract parameters from URI and save it in ParameterMap obj
     *  
     *  @params requestString string URI to extract
     *
     *  @returns ParameterMap
     */
    ParameterMap extractParameters(final String requestString) {
        // extract parameters...
        final ParameterMap parameters = new ParameterMap();
        StringTokenizer stoken = new StringTokenizer(requestString == null ? "" : requestString, QUERY_STRING_SEPARATOR);
        while (stoken.hasMoreTokens()) {
            String token = stoken.nextToken();            
            if (token.indexOf("=") == -1) 
                continue;
            String paramName = token.substring(0, token.indexOf("="));
            String value = token.substring(token.indexOf("=") + 1);
            try {
                value = URLDecoder.decode(value, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                aalogger.log(Level.WARNING, adminStrings.getLocalString("adapter.param.decode",
                        "Cannot decode parameter {0} = {1}"));
            }

            parameters.add(paramName, value);
        }

        // Dump parameters...
        if (aalogger.isLoggable(Level.FINER)) {
            for (Map.Entry<String, List<String>> entry : parameters.entrySet()) {
                for (String v : entry.getValue())
                    aalogger.log(Level.FINER, "Key {0} = {1}", new Object[]{entry.getKey(), v});
            }
        }
        return parameters;
    }

    @Override
    public void event(@RestrictTo(EventTypes.SERVER_READY_NAME) Event event) {
        if (event.is(EventTypes.SERVER_READY)) {
            latch.countDown();
            aalogger.fine("Ready to receive administrative commands");       
        }
        //the count-down does not start if any other event is received
    }
    
    
    @Override
    public int getListenPort() {
        return epd.getListenPort();
    }

    @Override
    public InetAddress getListenAddress() {
        return epd.getListenAddress();
    }

    @Override
    public List<String> getVirtualServers() {
        return epd.getAsadminHosts();
    }

    /**
     * Checks whether this adapter has been registered as a network endpoint.
     */
    @Override
    public boolean isRegistered() {
	return isRegistered;
    }

    /**
     * Marks this adapter as having been registered or unregistered as a
     * network endpoint
     */
    @Override
    public void setRegistered(boolean isRegistered) {
	this.isRegistered = isRegistered;
    }
}
