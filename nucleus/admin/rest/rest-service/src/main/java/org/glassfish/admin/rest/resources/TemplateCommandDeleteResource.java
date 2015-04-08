/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.admin.rest.resources;

import java.util.List;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.codehaus.jettison.json.JSONException;
import org.glassfish.admin.rest.Constants;
import org.glassfish.admin.rest.results.ActionReportResult;

import org.glassfish.jersey.media.sse.SseFeature;

import org.glassfish.admin.rest.utils.ResourceUtil;
import org.glassfish.api.admin.ParameterMap;

import static org.glassfish.admin.rest.resources.TemplateExecCommand.localStrings;

/**
 *
 * @author ludovic champenois ludo@dev.java.net
 * Code moved from generated classes to here. Gen code inherits from this template class
 * that contains the logic for mapped commands RS Resources
 *
 */
@Produces({"text/html", MediaType.APPLICATION_JSON+";qs=0.5", MediaType.APPLICATION_XML+";qs=0.5"})
public class TemplateCommandDeleteResource extends TemplateExecCommand {

    public TemplateCommandDeleteResource(String resourceName, String commandName, String commandMethod, String commandAction, String commandDisplayName,  boolean b) {
        super(resourceName, commandName, commandMethod, commandAction, commandDisplayName, b);
    }

    @DELETE
    @Consumes({
        MediaType.APPLICATION_JSON,
        MediaType.APPLICATION_XML,
        MediaType.APPLICATION_FORM_URLENCODED})
    public Response processDeleteLegacyFormat(ParameterMap data) {
        if (data == null) {
            data = new ParameterMap();
        }
        if (data.containsKey("error")) {
            String errorMessage = localStrings.getLocalString("rest.request.parsing.error",
                    "Unable to parse the input entity. Please check the syntax.");
            throw new WebApplicationException(ResourceUtil.getResponse(400, /*parsing error*/ errorMessage, requestHeaders, uriInfo));
        }
        return executeCommandLegacyFormat(preprocessData(data));
    }

    @DELETE
    @Produces(Constants.MEDIA_TYPE_JSON+";qs=0.5")
    public CommandResult processDelete(ParameterMap data) {
        if (data == null) {
            data = new ParameterMap();
        }
        if (data.containsKey("error")) {
            String errorMessage = localStrings.getLocalString("rest.request.parsing.error",
                    "Unable to parse the input entity. Please check the syntax.");
            throw new WebApplicationException(ResourceUtil.getResponse(400, /*parsing error*/ errorMessage, requestHeaders, uriInfo));
        }
        return executeCommand(preprocessData(data));
    }

    @DELETE
    @Consumes({
        MediaType.APPLICATION_JSON,
        MediaType.APPLICATION_XML,
        MediaType.APPLICATION_FORM_URLENCODED})
    @Produces(SseFeature.SERVER_SENT_EVENTS+";qs=0.5")
    public Response processDeleteSse(ParameterMap data) {
        if (data == null) {
            data = new ParameterMap();
        }
        if (data.containsKey("error")) {
            String errorMessage = localStrings.getLocalString("rest.request.parsing.error",
                    "Unable to parse the input entity. Please check the syntax.");
            throw new WebApplicationException(ResourceUtil.getResponse(400, /*parsing error*/ errorMessage, requestHeaders, uriInfo));
        }
        return executeCommandAsSse(preprocessData(data));
    }

//    //Handle POST request without any entity(input).
//    //Do not care what the Content-Type is.
//    @DELETE
//    @Produces({
//        "text/html",
//        MediaType.APPLICATION_JSON+";qs=0.5",
//        MediaType.APPLICATION_XML+";qs=0.5"})
//    public ActionReportResult processDelete() {
//        try {
//            return processDelete(new ParameterMap());
//        } catch (Exception e) {
//            throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
//        }
//    }
//hack-1 : support delete method for html
//Currently, browsers do not support delete method. For html media,
//delete operations can be supported through POST. Redirect html
//client POST request for delete operation to DELETE method.

//In case of delete command reosurce, we will also create post method
//which simply forwards the request to delete method. Only in case of
//html client delete request is routed through post. For other clients
//delete request is directly handled by delete method.
    @POST
    @Consumes({
        MediaType.APPLICATION_JSON,
        MediaType.APPLICATION_XML,
        MediaType.APPLICATION_FORM_URLENCODED})
    public Response hack(ParameterMap data) {
        if (data != null && data.containsKey("operation")) {
            List<String> l = data.get("operation");
            if (l.contains("__deleteoperation")) {

                data.remove("operation");
            }
        }
        return processDeleteLegacyFormat(data);
    }

    @GET
    public ActionReportResult getLegacyFormat() {
        return optionsLegacyFormat();
    }

    @GET
    @Produces(Constants.MEDIA_TYPE_JSON+";qs=0.5")
    public String get() throws JSONException {
        return options();
    }

    private ParameterMap preprocessData(final ParameterMap data) {
        processCommandParams(data);
        addQueryString(uriInfo.getQueryParameters(), data);
        adjustParameters(data);
        purgeEmptyEntries(data);
        return data;
    }
}
