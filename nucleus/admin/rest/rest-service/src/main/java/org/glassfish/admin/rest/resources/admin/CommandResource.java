/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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
 *
 * Portions Copyright [2017-2019] [Payara Foundation and/or its affiliates.]
 */
package org.glassfish.admin.rest.resources.admin;

import com.sun.enterprise.admin.remote.ParamsWithPayload;
import com.sun.enterprise.admin.remote.RemoteRestAdminCommand;
import com.sun.enterprise.admin.remote.RestPayloadImpl;
import com.sun.enterprise.admin.util.CachedCommandModel;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.universal.collections.ManifestUtils;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.StringUtils;
import com.sun.enterprise.util.uuid.UuidGenerator;
import com.sun.enterprise.util.uuid.UuidGeneratorImpl;
import com.sun.enterprise.admin.report.ActionReporter;
import com.sun.enterprise.admin.report.PlainTextActionReporter;
import com.sun.enterprise.admin.report.PropsFileActionReporter;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.logging.Level;
import javax.inject.Inject;
import javax.security.auth.Subject;
import javax.ws.rs.*;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.ResponseBuilder;
import org.glassfish.admin.rest.RestLogging;
import org.glassfish.admin.rest.utils.SseCommandHelper;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.*;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.media.sse.SseFeature;


/**
 *
 * @author mmares
 */
@Path("/")
public class CommandResource {
    private static final LocalStringManagerImpl strings = new LocalStringManagerImpl(CommandResource.class);

    public static final String SESSION_COOKIE_NAME = "JSESSIONID";
    public static final int MAX_AGE = 86400 ;

    private static UuidGenerator uuidGenerator = new UuidGeneratorImpl();
    private static volatile String serverName;

    private CommandRunner commandRunner;

    @Inject
    protected Ref<Subject> subjectRef;

    // -------- GET+OPTION: Get CommandModel

    @GET
    @Path("/{command:.*}/")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, "application/x-javascript"})
    public Response getCommandModel(@PathParam("command") String command) throws WebApplicationException {
        CommandName commandName = new CommandName(normalizeCommandName(command));
        if (RestLogging.restLogger.isLoggable(Level.FINEST)) {
            RestLogging.restLogger.log(Level.FINEST, "getCommandModel({0})", commandName);
        }
        CommandModel model = getCommandModel(commandName);
        String eTag = CachedCommandModel.computeETag(model);
        return Response.ok(model).tag(new EntityTag(eTag, true)).build();
    }

    @OPTIONS
    @Path("/{command:.*}/")
    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, "application/x-javascript"})
    public Response optionsCommandModel(@PathParam("command") String commandName) throws WebApplicationException {
        return getCommandModel(commandName);
    }

    // -------- GET: Manpage

    @GET
    @Path("/{command:.*}/manpage")
    @Produces({MediaType.TEXT_HTML})
    public String getManPageHtml(@PathParam("command") String command)
            throws IOException, WebApplicationException {
        CommandName commandName = new CommandName(normalizeCommandName(command));
        if (RestLogging.restLogger.isLoggable(Level.FINEST)) {
            RestLogging.restLogger.log(Level.FINEST, "getManPageHtml({0})", commandName);
        }
        BufferedReader help = getManPageReader(commandName);
        if (help == null) {
            return null;
        }
        StringBuilder result = new StringBuilder();
        result.append("<html><body>");
        String line;
        while ((line = help.readLine()) != null) {
            result.append(leadingSpacesToNbsp(StringUtils.escapeForHtml(line))).append("<br/>\n");
        }
        result.append("</body></html>");
        return result.toString();
    }

    @GET
    @Path("/{command:.*}/manpage")
    @Produces({MediaType.TEXT_PLAIN})
    public String getManPageTxt(@PathParam("command") String command, @QueryParam("eol") String eol)
            throws IOException, WebApplicationException {
        CommandName commandName = new CommandName(normalizeCommandName(command));
        if (RestLogging.restLogger.isLoggable(Level.FINEST)) {
            RestLogging.restLogger.log(Level.FINEST, "getManPageTxt({0}, {1})", new Object[]{commandName, eol});
        }
        BufferedReader help = getManPageReader(commandName);
        if (help == null) {
            return null;
        }
        if (!StringUtils.ok(eol)) {
            eol = ManifestUtils.EOL;
        }
        StringBuilder result = new StringBuilder();
        String line;
        while ((line = help.readLine()) != null) {
            result.append(line).append(eol);
        }
        return result.toString();
    }

    // -------- POST: Execute command [just ACTION-REPORT]

    @POST
    @Path("/{command:.*}/")
    @Consumes({MediaType.APPLICATION_FORM_URLENCODED})
    @Produces({MediaType.APPLICATION_JSON, "application/x-javascript"})
    public Response execCommandSimpInSimpOut(@PathParam("command") String command,
                @HeaderParam("X-Indent") String indent,
                @HeaderParam(RemoteRestAdminCommand.COMMAND_MODEL_MATCH_HEADER) String modelETag,
                @CookieParam(SESSION_COOKIE_NAME) Cookie jSessionId,
                ParameterMap data) {
        CommandName commandName = new CommandName(normalizeCommandName(command));
        if (RestLogging.restLogger.isLoggable(Level.FINEST)) {
            RestLogging.restLogger.log(Level.FINEST, "execCommandSimpInSimpOut({0})", commandName);
        }
        return executeCommand(commandName, null, data, false, indent, modelETag, jSessionId);
    }

    @POST
    @Path("/{command:.*}/")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces({MediaType.APPLICATION_JSON, "application/x-javascript"})
    public Response execCommandMultInSimpOut(@PathParam("command") String command,
                @HeaderParam("X-Indent") String indent,
                @HeaderParam(RemoteRestAdminCommand.COMMAND_MODEL_MATCH_HEADER) String modelETag,
                @CookieParam(SESSION_COOKIE_NAME) Cookie jSessionId,
                ParamsWithPayload pwp) {
        CommandName commandName = new CommandName(normalizeCommandName(command));
        if (RestLogging.restLogger.isLoggable(Level.FINEST)) {
            RestLogging.restLogger.log(Level.FINEST, "execCommandMultInSimpOut({0})", commandName);
        }
        ParameterMap data = null;
        Payload.Inbound inbound = null;
        if (pwp != null) {
            data = pwp.getParameters();
            inbound = pwp.getPayloadInbound();
        }
        return executeCommand(commandName, inbound, data, false, indent, modelETag, jSessionId);
    }

    @POST
    @Path("/{command:.*}/")
    @Produces({MediaType.APPLICATION_JSON, "application/x-javascript"})
    public Response execCommandEmptyInSimpOut(@PathParam("command") String command,
                @HeaderParam("X-Indent") String indent,
                @HeaderParam(RemoteRestAdminCommand.COMMAND_MODEL_MATCH_HEADER) String modelETag,
                @CookieParam(SESSION_COOKIE_NAME) Cookie jSessionId) {
        CommandName commandName = new CommandName(normalizeCommandName(command));
        if (RestLogging.restLogger.isLoggable(Level.FINEST)) {
            RestLogging.restLogger.log(Level.FINEST, "execCommandEmptyInSimpOut({0})", commandName);
        }
        ParameterMap data = new ParameterMap();
        return executeCommand(commandName, null, data, false, indent, modelETag, jSessionId);
    }

    // -------- POST: Execute command [MULTIPART result]

    @POST
    @Path("/{command:.*}/")
    @Consumes({MediaType.APPLICATION_FORM_URLENCODED})
    @Produces("multipart/mixed")
    public Response execCommandSimpInMultOut(@PathParam("command") String command,
                @HeaderParam("X-Indent") String indent,
                @HeaderParam(RemoteRestAdminCommand.COMMAND_MODEL_MATCH_HEADER) String modelETag,
                @CookieParam(SESSION_COOKIE_NAME) Cookie jSessionId,
                ParameterMap data) {
        CommandName commandName = new CommandName(normalizeCommandName(command));
        if (RestLogging.restLogger.isLoggable(Level.FINEST)) {
            RestLogging.restLogger.log(Level.FINEST, "execCommandSimpInMultOut({0})", commandName);
        }
        return executeCommand(commandName, null, data, true, indent, modelETag, jSessionId);
    }

    @POST
    @Path("/{command:.*}/")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces("multipart/mixed")
    public Response execCommandMultInMultOut(@PathParam("command") String command,
                @HeaderParam("X-Indent") String indent,
                @HeaderParam(RemoteRestAdminCommand.COMMAND_MODEL_MATCH_HEADER) String modelETag,
                @CookieParam(SESSION_COOKIE_NAME) Cookie jSessionId,
                ParamsWithPayload pwp) {
        CommandName commandName = new CommandName(normalizeCommandName(command));
        if (RestLogging.restLogger.isLoggable(Level.FINEST)) {
            RestLogging.restLogger.log(Level.FINEST, "execCommandMultInMultOut({0})", commandName);
        }
        ParameterMap data = null;
        Payload.Inbound inbound = null;
        if (pwp != null) {
            data = pwp.getParameters();
            inbound = pwp.getPayloadInbound();
        }
        return executeCommand(commandName, inbound, data, true, indent, modelETag, jSessionId);
    }

    @POST
    @Path("/{command:.*}/")
    @Produces("multipart/mixed")
    public Response execCommandEmptyInMultOut(@PathParam("command") String command,
                @HeaderParam("X-Indent") String indent,
                @HeaderParam(RemoteRestAdminCommand.COMMAND_MODEL_MATCH_HEADER) String modelETag,
                @CookieParam(SESSION_COOKIE_NAME) Cookie jSessionId) {
        CommandName commandName = new CommandName(normalizeCommandName(command));
        if (RestLogging.restLogger.isLoggable(Level.FINEST)) {
            RestLogging.restLogger.log(Level.FINEST, "execCommandEmptyInMultOut({0})", commandName);
        }
        ParameterMap data = new ParameterMap();
        return executeCommand(commandName, null, data, true, indent, modelETag, jSessionId);
    }

    // -------- POST: Execute command [SSE]

    @POST
    @Path("/{command:.*}/")
    @Consumes({MediaType.APPLICATION_FORM_URLENCODED})
    @Produces(SseFeature.SERVER_SENT_EVENTS)
    public Response execCommandSimpInSseOut(@PathParam("command") String command,
                @HeaderParam(RemoteRestAdminCommand.COMMAND_MODEL_MATCH_HEADER) String modelETag,
                @CookieParam(SESSION_COOKIE_NAME) Cookie jSessionId,
                ParameterMap data) {
        CommandName commandName = new CommandName(normalizeCommandName(command));
        if (RestLogging.restLogger.isLoggable(Level.FINEST)) {
            RestLogging.restLogger.log(Level.FINEST, "execCommandSimpInSseOut({0})", commandName);
        }
        return executeSseCommand(commandName, null, data, modelETag, jSessionId);
    }

    @POST
    @Path("/{command:.*}/")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(SseFeature.SERVER_SENT_EVENTS)
    public Response execCommandMultInSseOut(@PathParam("command") String command,
                @HeaderParam(RemoteRestAdminCommand.COMMAND_MODEL_MATCH_HEADER) String modelETag,
                @CookieParam(SESSION_COOKIE_NAME) Cookie jSessionId,
                ParamsWithPayload pwp) {
        CommandName commandName = new CommandName(normalizeCommandName(command));
        if (RestLogging.restLogger.isLoggable(Level.FINEST)) {
            RestLogging.restLogger.log(Level.FINEST, "execCommandMultInMultOut({0})", commandName);
        }
        ParameterMap data = null;
        if (pwp != null) {
            data = pwp.getParameters();
        }
        return executeSseCommand(commandName, null, data, modelETag, jSessionId);
    }

    @POST
    @Path("/{command:.*}/")
    @Produces(SseFeature.SERVER_SENT_EVENTS)
    public Response execCommandEmptyInSseOut(@PathParam("command") String command,
                @HeaderParam(RemoteRestAdminCommand.COMMAND_MODEL_MATCH_HEADER) String modelETag,
                @CookieParam(SESSION_COOKIE_NAME) Cookie jSessionId) {
        CommandName commandName = new CommandName(normalizeCommandName(command));
        if (RestLogging.restLogger.isLoggable(Level.FINEST)) {
            RestLogging.restLogger.log(Level.FINEST, "execCommandEmptyInMultOut({0})", commandName);
        }
        ParameterMap data = new ParameterMap();
        return executeSseCommand(commandName, null, data, modelETag, jSessionId);
    }

    // -------- private implementation

    private String normalizeCommandName(String str) {
        if (str == null) {
            return null;
        }
        if (str.endsWith("/")) {
            return str.substring(0, str.length() - 1);
        } else {
            return str;
        }
    }

    private void checkCommandModelETag(CommandModel model, String modelETag) throws WebApplicationException {
        CommandRunner cr = getCommandRunner();
        if (StringUtils.ok(modelETag) && !cr.validateCommandModelETag(model, modelETag)) {
            String message =
                    strings.getLocalString("commandmodel.etag.invalid",
                        "Cached command model for command {0} is invalid.", model.getCommandName());
            throw new WebApplicationException(Response.status(Response.Status.PRECONDITION_FAILED)
                        .type(MediaType.TEXT_PLAIN)
                        .entity(message)
                        .build());
        }
    }

    private Response executeSseCommand(CommandName commandName, Payload.Inbound inbound,
            ParameterMap params, String modelETag, Cookie jSessionId) throws WebApplicationException {
        //Scope support
        if (RestLogging.restLogger.isLoggable(Level.FINEST)) {
            RestLogging.restLogger.log(Level.FINEST, "executeSseCommand(): ", commandName);
        }
        //Check command model
        CommandModel model = getCommandModel(commandName);
        checkCommandModelETag(model, modelETag);
        //Execute it
        boolean notifyOption = false;
        if (params != null)  {
            notifyOption = params.containsKey("notify");
        }
        final CommandRunner.CommandInvocation commandInvocation =
                getCommandRunner().getCommandInvocation(commandName.getScope(),
                commandName.getName(), new PropsFileActionReporter(), getSubject(),notifyOption);
        if (inbound != null) {
            commandInvocation.inbound(inbound);
        }
        commandInvocation
                .outbound(new RestPayloadImpl.Outbound(false))
                .managedJob()
                .parameters(params);
        ResponseBuilder rb = Response.status(HttpURLConnection.HTTP_OK);
        if ( isSingleInstanceCommand(model)) {
            rb.cookie(getJSessionCookie(jSessionId));
        }
        rb.entity(SseCommandHelper.invokeAsync(commandInvocation, null));
        return rb.build();
    }

    private Response executeCommand(CommandName commandName, Payload.Inbound inbound,
            ParameterMap params, boolean supportsMultiparResult, String xIndentHeader,
            String modelETag, Cookie jSessionId) throws WebApplicationException {
        //Scope support
        if (RestLogging.restLogger.isLoggable(Level.FINEST)) {
            RestLogging.restLogger.log(Level.FINEST, "executeCommand(): ", commandName);
        }
        //Check command model
        CommandModel model = getCommandModel(commandName);
        checkCommandModelETag(model, modelETag);
        //Execute it
        boolean notifyOption = false;
        if (params != null)  {
            notifyOption = params.containsKey("notify");
        }
        ActionReporter ar = new PropsFileActionReporter(); //new RestActionReporter(); //Must use PropsFileActionReporter because some commands react diferently on it :-(
        final RestPayloadImpl.Outbound outbound = new RestPayloadImpl.Outbound(false);
        final CommandRunner.CommandInvocation commandInvocation =
                getCommandRunner().getCommandInvocation(commandName.getScope(), commandName.getName(), ar, getSubject(),notifyOption);
        if (inbound != null) {
            commandInvocation.inbound(inbound);
        }
        commandInvocation
                .outbound(outbound)
                .parameters(params)
                .execute();
        ar = (ActionReporter) commandInvocation.report();
        fixActionReporterSpecialCases(ar);
        ActionReport.ExitCode exitCode = ar.getActionExitCode();
        int status = HttpURLConnection.HTTP_OK; /*200 - ok*/
        if (exitCode == ActionReport.ExitCode.FAILURE) {
            status = HttpURLConnection.HTTP_INTERNAL_ERROR;
        }
        ResponseBuilder rb = Response.status(status);
        if (xIndentHeader != null) {
            rb.header("X-Indent", xIndentHeader);
        }
        if (supportsMultiparResult && outbound.size() > 0) {
            ParamsWithPayload pwp = new ParamsWithPayload(outbound, ar);
            rb.entity(pwp);
        } else {
            rb.type(MediaType.APPLICATION_JSON_TYPE);
            rb.entity(ar);
        }
        if ( isSingleInstanceCommand(model)) {
            rb.cookie(getJSessionCookie(jSessionId));
        }
        return rb.build();
    }

    /** Some ActionReporters has special logic which must be reflected here
     */
    private void fixActionReporterSpecialCases(ActionReporter ar) {
        if (ar == null) {
            return;
        }
        if (ar instanceof PlainTextActionReporter) {
            PlainTextActionReporter par = (PlainTextActionReporter) ar;
            StringBuilder finalOutput = new StringBuilder();
            par.getCombinedMessages(par, finalOutput);
            String outs = finalOutput.toString();
            if (!StringUtils.ok(outs)) {
                par.getTopMessagePart().setMessage(strings.getLocalString("get.mon.no.data", "No monitoring data to report.") + "\n");
            }
        }
    }

    /**
     * This will create a unique SessionId, Max-Age,Version,Path to be added to the Set-Cookie header
     */
    public NewCookie getJSessionCookie(Cookie jSessionId) {
        String value;
        // If the request has a Cookie header and
        // there is no failover then send back the same
        // JSESSIONID
        if (jSessionId != null && isJSessionCookieOk(jSessionId.getValue())) {
            value = jSessionId.getValue();
        }  else {
            value = uuidGenerator.generateUuid() + '.' + getServerName();
        }
        return new NewCookie(SESSION_COOKIE_NAME, value, "/command", null, null, MAX_AGE, false);
    }

    private boolean isJSessionCookieOk(String value) {
        if (!StringUtils.ok(value)) {
            return false;
        }
        return value.endsWith("." + getServerName());
    }

    private static boolean isSingleInstanceCommand(CommandModel model) {
        if (model != null ) {
            ExecuteOn executeOn = model.getClusteringAttributes();
            if ((executeOn != null) && (executeOn.value().length ==1) &&
                    executeOn.value()[0].equals(org.glassfish.api.admin.RuntimeType.SINGLE_INSTANCE)) {
                return true;
            }
        }
        return false;
    }

    private static String leadingSpacesToNbsp(String str) {
        if (str == null) {
            return null;
        }
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(i) != ' ') {
                StringBuilder sb = new StringBuilder((i * 6) + (str.length() - i));
                for (int j = 0; j < i; j++) {
                    sb.append("&nbsp;");
                }
                sb.append(str.substring(i));
                return sb.toString();
            }
        }
        return str;
    }

    private CommandModel getCommandModel(CommandName commandName) throws WebApplicationException {
        CommandRunner cr = getCommandRunner();
        CommandModel model = cr.getModel(commandName.getScope(), commandName.getName(), RestLogging.restLogger);
        if (model == null) {
            throw new WebApplicationException(Response.status(Response.Status.NOT_FOUND)
                    .type(MediaType.TEXT_PLAIN)
                    .entity(strings.getLocalString("adapter.command.notfound",
                        "Command {0} not found. \nCheck the entry of command name. This command may be provided by a package that is not installed.", commandName.getName()))
                    .build());
        }
        return model;
    }

    private BufferedReader getManPageReader(CommandName commandName) throws WebApplicationException {
        CommandModel model = getCommandModel(commandName);
        return getCommandRunner().getHelp(model);
    }

    private CommandRunner getCommandRunner() {
        if (this.commandRunner == null) {
            commandRunner = getHabitat().getService(CommandRunner.class);
        }
        return this.commandRunner;
    }

    private ServiceLocator getHabitat() {
        return Globals.getDefaultHabitat();
    }

    private String getServerName() {
        if (serverName == null) {
            Server server = getHabitat().getService(Server.class, ServerEnvironment.DEFAULT_INSTANCE_NAME);
            if (server != null) {
                serverName = server.getName();
            }
        }
        return serverName;
    }

    private Subject getSubject() {
        return subjectRef.get();
    }

    private static class CommandName {
        private String scope;
        private String name;

        public CommandName(String scope, String name) {
            this.scope = scope;
            this.name = name;
        }

        public CommandName(String fullName) {
            if (fullName == null) {
                return;
            }
            int ind = fullName.indexOf('/');
            if (ind > 0) {
                this.scope = fullName.substring(0, ind + 1);
                this.name = fullName.substring(ind + 1);
            } else {
                this.name = fullName;
            }
        }

        public String getName() {
            return name;
        }

        public String getScope() {
            return scope;
        }

        @Override
        public String toString() {
            if (this.scope == null) {
                return "CommandName[" + name + "]";
            } else {
                return "CommandName[" + scope + name + "]";
            }
        }

    }

}
