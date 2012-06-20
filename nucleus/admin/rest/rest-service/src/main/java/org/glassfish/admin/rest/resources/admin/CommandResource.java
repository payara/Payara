/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.admin.rest.resources.admin;

import com.sun.enterprise.universal.collections.ManifestUtils;
import com.sun.enterprise.util.StringUtils;
import com.sun.logging.LogDomains;
import java.io.BufferedReader;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.logging.Logger;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.ResponseBuilder;
import org.glassfish.admin.rest.shareable.RestPayloadImpl;
import org.glassfish.admin.rest.utils.xml.RestActionReporter;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.CommandModel;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.admin.Payload;
import org.glassfish.internal.api.Globals;
import org.glassfish.jersey.media.multipart.MultiPart;
import org.jvnet.hk2.component.BaseServiceLocator;

/**
 *
 * @author mmares
 */
@Path("/")
public class CommandResource {
    
    private final static Logger logger = 
            LogDomains.getLogger(CommandResource.class, LogDomains.ADMIN_LOGGER);
    
//    @Context
//    protected BaseServiceLocator habitat;
    
    @GET
    @Produces({MediaType.TEXT_PLAIN})
    public String emptyCallTxt() {
        return "command resource - Loaded";
    }
    
    @GET
    @Produces({MediaType.TEXT_HTML})
    public String emptyCallHtml() {
        return "<html><body><h1>command resource</h1>Loaded</body></html>";
    }
    
    @GET
    @Path("/{command}/")
    @Produces({MediaType.TEXT_HTML, MediaType.TEXT_PLAIN, MediaType.TEXT_XML, 
               MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, 
               "application/x-javascript"})
    public CommandModel getCommandModel(@PathParam("command") String commandName) {
        CommandRunner cr = getCommandRunner();
        return cr.getModel(commandName, logger);
    }
    
    @OPTIONS
    @Path("/{command}/")
    @Produces({MediaType.TEXT_HTML, MediaType.TEXT_PLAIN, MediaType.TEXT_XML, 
               MediaType.APPLICATION_XML, MediaType.APPLICATION_JSON, 
               "application/x-javascript"})
    public CommandModel optionsCommandModel(@PathParam("command") String commandName) {
        return getCommandModel(commandName);
    }
    
    @GET
    @Path("/{command}/manpage")
    @Produces({MediaType.TEXT_HTML})
    public String getManPageHtml(@PathParam("command") String commandName) throws IOException {
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
    @Path("/{command}/manpage")
    @Produces({MediaType.TEXT_PLAIN})
    public String getManPageTxt(@PathParam("command") String commandName, @QueryParam("eol") String eol) throws IOException {
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
    
//    @POST
//    @Path("/{command}/")
//    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.APPLICATION_FORM_URLENCODED})
//    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
//    public Response execCommandSimpInSimpOut(@PathParam("command") String commandName, ParameterMap data) {
//        return executeCommand(commandName, null, data, false);
//    }
//    
//    @POST
//    @Path("/{command}/")
//    @Consumes(MediaType.MULTIPART_FORM_DATA)
//    @Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
//    public Response execCommandMultInSimpOut(@PathParam("command") String commandName, FormDataMultiPart mp) {
//        ParameterMap data = new ParameterMap();
//        Payload.Inbound inbound = RestPayloadImpl.Inbound.parseFromFormDataMultipart(mp, data);
//        return executeCommand(commandName, inbound, data, false);
//    }
    
    @POST
    @Path("/{command}/")
    @Consumes("*/*")
    //@Produces({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response execCommandEmptyInSimpOut(@PathParam("command") String commandName, String some) {
        ParameterMap data = new ParameterMap();
        return executeCommand(commandName, null, data, false);
    }
    
//    @POST
//    @Path("/{command}/")
//    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.APPLICATION_FORM_URLENCODED})
//    @Produces("multipart/mixed")
//    public Response execCommandSimpInMultOut(@PathParam("command") String commandName, ParameterMap data) {
//        return executeCommand(commandName, null, data, true);
//    }
//    
//    @POST
//    @Path("/{command}/")
//    @Consumes(MediaType.MULTIPART_FORM_DATA)
//    @Produces("multipart/mixed")
//    public Response execCommandMultInMultOut(@PathParam("command") String commandName, FormDataMultiPart mp) {
//        ParameterMap data = new ParameterMap();
//        Payload.Inbound inbound = RestPayloadImpl.Inbound.parseFromFormDataMultipart(mp, data);
//        return executeCommand(commandName, inbound, data, true);
//    }
//    
//    @POST
//    @Path("/{command}/")
//    @Produces("multipart/mixed")
//    public Response execCommandEmptyInMultOut(@PathParam("command") String commandName) {
//        ParameterMap data = new ParameterMap();
//        return executeCommand(commandName, null, data, true);
//    }
    
    
    private Response executeCommand(String commandName, Payload.Inbound inbound, ParameterMap params, boolean supportsMultiparResult) {
        CommandRunner cr = getHabitat().getComponent(CommandRunner.class);
        RestActionReporter ar = new RestActionReporter();
        final RestPayloadImpl.Outbound outbound = new RestPayloadImpl.Outbound(false);
        final CommandRunner.CommandInvocation commandInvocation = cr.getCommandInvocation(commandName, ar);
        if (inbound != null) {
            commandInvocation.inbound(inbound);
        }
        commandInvocation
                .outbound(outbound)
                .parameters(params)
                .execute();
        ActionReport.ExitCode exitCode = ar.getActionExitCode();
        int status = HttpURLConnection.HTTP_OK; /*200 - ok*/
        if (exitCode == ActionReport.ExitCode.FAILURE) {
            status = HttpURLConnection.HTTP_INTERNAL_ERROR;
        }
        ResponseBuilder rb = Response.status(status); // entity(ar).build();
        if (supportsMultiparResult && outbound.size() > 0) {
            MultiPart mp = new MultiPart();
            mp.bodyPart(ar, MediaType.APPLICATION_XML_TYPE);
            outbound.addToMultipart(mp);
            rb.entity(mp);
        } else {
            rb.entity(ar);
        }
        return rb.build();
    }
    
    private String leadingSpacesToNbsp(String str) {
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
    
    private BufferedReader getManPageReader(String commandName) {
        CommandRunner cr = getCommandRunner();
        CommandModel model = cr.getModel(commandName, logger);
        if (model == null) {
            return null;
        }
        return cr.getHelp(model);
    }
    
    private CommandRunner getCommandRunner() {
        return getHabitat().getByContract(CommandRunner.class);
    }
    
    private BaseServiceLocator getHabitat() {
        return Globals.getDefaultBaseServiceLocator();
    }
    
}
