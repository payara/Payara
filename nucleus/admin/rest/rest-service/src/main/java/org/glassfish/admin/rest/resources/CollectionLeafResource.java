/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2015 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.config.serverbeans.JavaConfig;

import javax.ws.rs.PUT;
import org.jvnet.hk2.config.TransactionFailure;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.Consumes;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;

import org.glassfish.admin.rest.provider.MethodMetaData;
import org.glassfish.admin.rest.results.ActionReportResult;
import org.glassfish.admin.rest.results.OptionsResult;
import org.glassfish.admin.rest.utils.xml.RestActionReporter;
import org.glassfish.api.ActionReport;

import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.admin.rest.utils.ResourceUtil;
import org.glassfish.admin.rest.utils.Util;
import org.jvnet.hk2.config.Dom;

import static org.glassfish.admin.rest.utils.Util.decode;
import static org.glassfish.admin.rest.utils.Util.upperCaseFirstLetter;


/**
 * @author Rajeshwar Patil
 */
@Produces({"text/html", MediaType.APPLICATION_JSON+";qs=0.5", MediaType.APPLICATION_XML+";qs=0.5", MediaType.APPLICATION_FORM_URLENCODED+";qs=0.5"})
public abstract class CollectionLeafResource extends AbstractResource {
    protected List<String> entity;
    protected Dom parent;
    protected String tagName;
    protected String target;
    protected String profiler = "false";

    public final static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(CollectionLeafResource.class);

    /** Creates a new instance of xxxResource */
    public CollectionLeafResource() {
    }

    public void setEntity(List<String> p) {
        entity = p;
    }

    public List<String> getEntity() {
        return entity;
    }

    public void setParentAndTagName(Dom parent, String tagName) {
        this.parent = parent;
        this.tagName = tagName;
        if (parent!=null){
            entity = parent.leafElements(tagName);

            if (parent.getImplementationClass().equals(JavaConfig.class)) {
                target = parent.parent().attribute("name");
            } else {
                target = parent.parent().parent().attribute("name");
                profiler = "true";
            }
        }
    }

    @GET
    public Response get(@QueryParam("expandLevel") @DefaultValue("1") int expandLevel) {
        if (getEntity() == null) {
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        return Response.ok(buildActionReportResult()).build();
    }

    @POST //create
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.APPLICATION_FORM_URLENCODED})
    public Response create(HashMap<String, String> data) throws TransactionFailure {
        //hack-1 : support delete method for html
        //Currently, browsers do not support delete method. For html media,
        //delete operations can be supported through POST. Redirect html
        //client POST request for delete operation to DELETE method.
        if ((data.containsKey("operation")) &&
                (data.get("operation").equals("__deleteoperation"))) {
            data.remove("operation");
            return delete(data);
        }

        String postCommand = getPostCommand();
        Map<String, String> payload = null;

        if (isJvmOptions(postCommand)) {
            deleteExistingOptions();
            payload = processData(data);
        } else {
            payload = data;
        }


        return runCommand(postCommand, payload, "rest.resource.create.message",
            "\"{0}\" created successfully.", "rest.resource.post.forbidden","POST on \"{0}\" is forbidden.");
    }

    @PUT //create
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.APPLICATION_FORM_URLENCODED})
    @Deprecated
    public Response add(HashMap<String, String> data) throws TransactionFailure {
        String postCommand = getPostCommand();
        Map<String, String> payload = null;

        if (isJvmOptions(postCommand)) {
            payload = processData(data);
        } else {
            payload = data;
        }


        return runCommand(postCommand, payload, "rest.resource.create.message",
            "\"{0}\" created successfully.", "rest.resource.post.forbidden","POST on \"{0}\" is forbidden.");
    }

    @DELETE //delete
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.APPLICATION_FORM_URLENCODED})
    public Response delete(HashMap<String, String> data) {
        if (data == null) {
            data = new HashMap<String, String>();
        }
        ResourceUtil.addQueryString(uriInfo.getQueryParameters(), data);
        String deleteCommand = getDeleteCommand();

        if (isJvmOptions(deleteCommand)) {
            if (data.isEmpty()) {
                deleteExistingOptions();
                return Response.ok().build();
            } else {
                return runCommand(deleteCommand, processData(data), "rest.resource.delete.message",
                    "\"{0}\" deleted successfully.", "rest.resource.delete.forbidden", "DELETE on \"{0}\" is forbidden.");
            }
        } else {
            return runCommand(deleteCommand, data, "rest.resource.delete.message",
                "\"{0}\" deleted successfully.", "rest.resource.delete.forbidden", "DELETE on \"{0}\" is forbidden.");
        }

    }

    @OPTIONS
    @Produces({MediaType.APPLICATION_JSON+";qs=0.5", "text/html", MediaType.APPLICATION_XML+";qs=0.5"})
    public Response options() {
        return Response.ok(buildActionReportResult()).build();
    }

    protected ActionReportResult buildActionReportResult() {
        RestActionReporter ar = new RestActionReporter();
        final String typeKey = upperCaseFirstLetter((decode(getName())));
        ar.setActionDescription(typeKey);
        ar.getExtraProperties().put("leafList", getEntity());

        OptionsResult optionsResult = new OptionsResult(Util.getResourceName(uriInfo));
        Map<String, MethodMetaData> mmd = getMethodMetaData();
        optionsResult.putMethodMetaData("GET", mmd.get("GET"));
        optionsResult.putMethodMetaData("POST", mmd.get("POST"));

        ResourceUtil.addMethodMetaData(ar, mmd);
        return new ActionReportResult(ar, optionsResult);
    }

    protected Map<String, MethodMetaData> getMethodMetaData() {
        Map<String, MethodMetaData> mmd = new TreeMap<String, MethodMetaData>();
        //GET meta data
        mmd.put("GET", new MethodMetaData());

        //POST meta data
        String postCommand = getPostCommand();
        if (postCommand != null) {
            MethodMetaData postMethodMetaData = ResourceUtil.getMethodMetaData(postCommand, locatorBridge.getRemoteLocator());
            mmd.put("POST", postMethodMetaData);
        }

        //DELETE meta data
        String deleteCommand = getDeleteCommand();
        if (deleteCommand != null) {
            MethodMetaData deleteMethodMetaData = ResourceUtil.getMethodMetaData(deleteCommand, locatorBridge.getRemoteLocator());
            mmd.put("DELETE", deleteMethodMetaData);
        }

        return mmd;
    }

    protected void addDefaultParameter(Map<String, String> data) {
        int index = uriInfo.getAbsolutePath().getPath().lastIndexOf('/');
        String defaultParameterValue = uriInfo.getAbsolutePath().getPath().substring(index + 1);
        data.put("DEFAULT", defaultParameterValue);
    }

    protected String getPostCommand(){
        return null;
    }

    protected String getDeleteCommand() {
        return null;
    }

    protected String getName() {
        return Util.getResourceName(uriInfo);
    }

    private Response runCommand(String commandName, Map<String, String> data,
        String successMsgKey, String successMsg, String operationForbiddenMsgKey, String operationForbiddenMsg ) {
        try {
            if (data.containsKey("error")) {
                String errorMessage = localStrings.getLocalString("rest.request.parsing.error",
                        "Unable to parse the input entity. Please check the syntax.");
                return Response.status(400).
                        entity(ResourceUtil.getActionReportResult(ActionReport.ExitCode.FAILURE,
                            errorMessage, requestHeaders, uriInfo)).build();
            }

            ResourceUtil.purgeEmptyEntries(data);
            ResourceUtil.adjustParameters(data);

            String attributeName = data.get("DEFAULT");

            if (null != commandName) {
                RestActionReporter actionReport = ResourceUtil.runCommand(commandName,
                    data, getSubject());

                ActionReport.ExitCode exitCode = actionReport.getActionExitCode();
                if (exitCode != ActionReport.ExitCode.FAILURE) {
                    String successMessage =
                        localStrings.getLocalString(successMsgKey,
                            successMsg, new Object[] {attributeName});
                    return Response.ok(ResourceUtil.getActionReportResult(actionReport, successMessage, requestHeaders, uriInfo)).build();
                }

                String errorMessage = getErrorMessage(data, actionReport);
                return Response.status(400).entity(ResourceUtil.getActionReportResult(actionReport, errorMessage, requestHeaders, uriInfo)).build();
            }
            String message =
                localStrings.getLocalString(operationForbiddenMsgKey,
                    operationForbiddenMsg, new Object[] {uriInfo.getAbsolutePath()});
            return Response.status(403).entity(ResourceUtil.getActionReportResult(ActionReport.ExitCode.FAILURE, message, requestHeaders, uriInfo)).build();

        } catch (Exception e) {
            throw new WebApplicationException(e, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    private String getErrorMessage(Map<String, String> data, ActionReport ar) {
        String message = ar.getMessage();

        /*if (data.isEmpty()) {
            try {
                //usage info
                message = ar.getTopMessagePart().getChildren().get(0).getMessage();
            } catch (Exception e) {
                message = ar.getMessage();
            }
        }*/
        return message;
    }

    // Ugly, temporary hack
    private Map<String, String> processData(Map<String, String> data) {
        Map<String, String> results = new HashMap<String, String>();
        StringBuilder options = new StringBuilder();
        String sep = "";
        for (Map.Entry<String, String> entry : data.entrySet()) {
            String key = entry.getKey();
            if ("target".equals(key) || "profiler".equals(key)) {
                results.put(key, entry.getValue());
            } else {
//                options.append(sep).append(escapeOptionPart(entry.getKey()));
                options.append(sep).append(entry.getKey());

                String value = entry.getValue();
                if ((value != null) && (!value.isEmpty())) {
//                    options.append("=").append(escapeOptionPart(entry.getValue()));
                    options.append("=").append(entry.getValue());
                }
                sep = ":";
            }
        }

        results.put("id", options.toString());
        if (results.get("target") == null) {
            results.put("target", target);
        }
        if (results.get("profiler") == null) {
            results.put("profiler", profiler);
        }

        return results;
    }

    /**
     * Escapes special chars (e.g., colons) in a JVM Option part
     * @param part
     * @return
     */
    protected String escapeOptionPart(String part) {
        String changed = part
                .replace("\\", "\\\\")
                .replace(":", "\\:");
        return changed;
    }

    // TODO: JvmOptions needs to have its own class, but the generator doesn't seem to support
    // overriding resourcePath mappings.  We need to address this post-3.1
    private boolean isJvmOptions(String command) {
        return (command != null) && (command.contains("jvm-options"));
    }

    protected void deleteExistingOptions() {
        Map<String, String> existing = new HashMap<String, String>();
        existing.put("target", target);
        for (String option : getEntity()) {
            int index = option.indexOf("=");
            if (index > -1) {
                existing.put(escapeOptionPart(option.substring(0, index)), escapeOptionPart(option.substring(index+1)));
            } else {
                existing.put(escapeOptionPart(option), "");
            }
        }

        runCommand(getDeleteCommand(), processData(existing),
                "rest.resource.delete.message",
                "\"{0}\" deleted successfully.",
                "rest.resource.delete.forbidden",
                "DELETE on \"{0}\" is forbidden.");
    }

}
