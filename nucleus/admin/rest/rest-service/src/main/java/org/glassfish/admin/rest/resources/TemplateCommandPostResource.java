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

import java.io.File;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.Status;
import org.codehaus.jettison.json.JSONException;
import org.glassfish.admin.rest.Constants;
import org.glassfish.admin.rest.RestLogging;
import org.glassfish.admin.rest.results.ActionReportResult;

import org.glassfish.admin.rest.utils.ResourceUtil;
import org.glassfish.admin.rest.utils.Util;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.glassfish.jersey.media.sse.SseFeature;

import static org.glassfish.admin.rest.resources.TemplateExecCommand.localStrings;

/**
 *
 * @author ludovic champenois ludo@dev.java.net
 * Code moved from generated classes to here. Gen code inherits from this template class
 * that contains the logic for mapped commands RS Resources
 *
 */
@Produces({"text/html", MediaType.APPLICATION_JSON+";qs=0.5", MediaType.APPLICATION_XML+";qs=0.5"})
public class TemplateCommandPostResource extends TemplateExecCommand {

    public TemplateCommandPostResource(String resourceName, String commandName, String commandMethod, String commandAction, String commandDisplayName, boolean isLinkedToParent) {
        super(resourceName, commandName, commandMethod, commandAction, commandDisplayName, isLinkedToParent);
    }

    // ---------------- POST

    @POST
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.APPLICATION_FORM_URLENCODED})
    public Response processPostLegacyFormat(ParameterMap data) {
        if (data == null) {
            data = new ParameterMap();
        }
        if (data.containsKey("error")) {
            String errorMessage = localStrings.getLocalString("rest.request.parsing.error", "Unable to parse the input entity. Please check the syntax.");
            throw new WebApplicationException(ResourceUtil.getResponse(400, /*parsing error*/ errorMessage, requestHeaders, uriInfo));
        }
        return super.executeCommandLegacyFormat(preprocessData(data));
    }

    @POST
    @Consumes(Constants.MEDIA_TYPE_JSON)
    @Produces(Constants.MEDIA_TYPE_JSON+";qs=0.5")
    public CommandResult processPost(ParameterMap data) {
        if (data == null) {
            data = new ParameterMap();
        }
        if (data.containsKey("error")) {
            String errorMessage = localStrings.getLocalString("rest.request.parsing.error", "Unable to parse the input entity. Please check the syntax.");
            throw new WebApplicationException(ResourceUtil.getResponse(400, /*parsing error*/ errorMessage, requestHeaders, uriInfo));
        }
        return super.executeCommand(preprocessData(data));
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Response post(FormDataMultiPart formData) {
        /* data passed to the generic command running
         *
         * */
        return processPostLegacyFormat(createDataBasedOnForm(formData));

    }

    //Handle POST request without any entity(input).
    //Do not care what the Content-Type is.
    @POST
    public Response processPost() {
        try {
            return processPostLegacyFormat(new ParameterMap());
        } catch (Exception e) {
            throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    // ---------------- SSE POST

    @POST
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.APPLICATION_FORM_URLENCODED})
    @Produces(SseFeature.SERVER_SENT_EVENTS+";qs=0.5")
    public Response processSsePost(ParameterMap data) {
        if (data == null) {
            data = new ParameterMap();
        }
        if (data.containsKey("error")) {
            String errorMessage = localStrings.getLocalString("rest.request.parsing.error", "Unable to parse the input entity. Please check the syntax.");
            throw new WebApplicationException(ResourceUtil.getResponse(400, /*parsing error*/ errorMessage, requestHeaders, uriInfo));
        }
        return super.executeCommandAsSse(preprocessData(data));
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(SseFeature.SERVER_SENT_EVENTS+";qs=0.5")
    public Response ssePost(FormDataMultiPart formData) {
        return processSsePost(createDataBasedOnForm(formData));
    }

    @POST
    @Produces(SseFeature.SERVER_SENT_EVENTS+";qs=0.5")
    public Response processSsePost() {
        try {
            return processSsePost(new ParameterMap());
        } catch (Exception e) {
            throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    // ---------------- GET

    @GET
    public ActionReportResult getLegacyFormat() {
        return optionsLegacyFormat();
    }

    @GET
    @Produces(Constants.MEDIA_TYPE_JSON+";qs=0.5")
    public String get() {
        try {
            return options();
        } catch (JSONException ex) {
            throw new WebApplicationException(ex, Status.INTERNAL_SERVER_ERROR);
        }
    }

    private ParameterMap preprocessData(final ParameterMap data) {
        processCommandParams(data);
        adjustParameters(data);
        purgeEmptyEntries(data);
        return data;
    }

    private static ParameterMap createDataBasedOnForm(FormDataMultiPart formData) {
        ParameterMap data = new ParameterMap();
        if (formData == null) {
            formData = new FormDataMultiPart();
        }
        try {
            /* data passed to the generic command running
             *
             * */

            Map<String, List<FormDataBodyPart>> m1 = formData.getFields();

            Set<String> ss = m1.keySet();
            for (String fieldName : ss) {
                for (FormDataBodyPart bodyPart : formData.getFields(fieldName)) {

                    if (bodyPart.getContentDisposition().getFileName() != null) {//we have a file
                        //save it and mark it as delete on exit.
                        InputStream fileStream = bodyPart.getValueAs(InputStream.class);
                        String mimeType = bodyPart.getMediaType().toString();

                        //Use just the filename without complete path. File creation
                        //in case of remote deployment failing because fo this.
                        String fileName = bodyPart.getContentDisposition().getFileName();
                        if (fileName.contains("/")) {
                            fileName = Util.getName(fileName, '/');
                        } else {
                            if (fileName.contains("\\")) {
                                fileName = Util.getName(fileName, '\\');
                            }
                        }

                        File f = Util.saveFile(fileName, mimeType, fileStream);
                        f.deleteOnExit();
                        //put only the local path of the file in the same field.
                        data.add(fieldName, f.getAbsolutePath());
                    } else {
                        data.add(fieldName, bodyPart.getValue());
                    }
                }
            }
        } catch (Exception ex) {
            RestLogging.restLogger.log(Level.SEVERE, null, ex);
        } finally {
            if (formData != null) {
                formData.cleanup();
            }
        }
        return data;

    }
}
