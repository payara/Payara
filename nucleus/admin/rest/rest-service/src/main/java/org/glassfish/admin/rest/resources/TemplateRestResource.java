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

import com.sun.enterprise.config.modularity.ConfigModularityUtils;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.admin.rest.provider.MethodMetaData;
import org.glassfish.admin.rest.results.ActionReportResult;
import org.glassfish.admin.rest.results.OptionsResult;
import org.glassfish.admin.rest.utils.ResourceUtil;
import org.glassfish.admin.rest.utils.Util;
import org.glassfish.admin.rest.utils.xml.RestActionReporter;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.RestRedirect;
import org.glassfish.config.support.Delete;
import org.glassfish.hk2.api.MultiException;
import org.glassfish.jersey.media.multipart.FormDataBodyPart;
import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.jvnet.hk2.config.ConfigBean;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigModel;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.Dom;
import org.jvnet.hk2.config.TransactionFailure;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.OPTIONS;
import javax.ws.rs.POST;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.File;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.logging.Level;
import javax.ws.rs.core.Response.Status;
import org.glassfish.admin.rest.Constants;
import org.glassfish.admin.rest.OptionsCapable;
import org.glassfish.admin.rest.RestLogging;
import org.glassfish.admin.rest.composite.metadata.RestResourceMetadata;
import org.glassfish.api.ActionReport.ExitCode;

import static org.glassfish.admin.rest.utils.Util.eleminateHypen;
import java.net.URLDecoder;

/**
 * @author Ludovic Champenois ludo@java.net
 * @author Rajeshwar Patil
 */
@Produces({"text/html", MediaType.APPLICATION_JSON+";qs=0.5", MediaType.APPLICATION_XML+";qs=0.5", MediaType.APPLICATION_FORM_URLENCODED+";qs=0.5"})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.APPLICATION_FORM_URLENCODED})
public class TemplateRestResource extends AbstractResource implements OptionsCapable {
    protected Dom entity;  //may be null when not created yet...
    protected Dom parent;
    protected String tagName;
    protected ConfigModel childModel; //good model even if the child entity is null
    protected String childID; // id of the current child if part of a list, might be null
    public final static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(TemplateRestResource.class);
    final private static List<String> attributesToSkip = new ArrayList<String>() {

        {
            add("parent");
            add("name");
            add("children");
            add("submit");
        }
    };

    /**
     * Creates a new instance of xxxResource
     */
    public TemplateRestResource() {
    }

    @GET
    public ActionReportResult getEntityLegacyFormat(@QueryParam("expandLevel") @DefaultValue("1") int expandLevel) {
        if (childModel == null) {//wrong entity name at this point
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        return buildActionReportResult(true);
    }

    @GET
    @Produces(Constants.MEDIA_TYPE_JSON+";qs=0.5")
    public Map<String,String> getEntity(@QueryParam("expandLevel") @DefaultValue("1") int expandLevel) {
        if (childModel == null) {//wrong entity name at this point
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }

        return getAttributes((ConfigBean) getEntity());
    }

    @POST
    //create or update
    public Response createOrUpdateEntityLegacyFormat(HashMap<String, String> data) {
        return Response.ok(ResourceUtil.getActionReportResult(doCreateOrUpdate(data),
                localStrings.getLocalString("rest.resource.update.message",
                "\"{0}\" updated successfully.", uriInfo.getAbsolutePath()),
                requestHeaders, uriInfo)).build();
    }

    @POST
    @Produces(Constants.MEDIA_TYPE_JSON+";qs=0.5")
    public Response createOrUpdateEntity(HashMap<String, String> data) {
        doCreateOrUpdate(data);
        return Response.status(Status.CREATED).build();
    }

    /**
     * allows for remote files to be put in a tmp area and we pass the
     * local location of this file to the corresponding command instead of the content of the file
     * * Yu need to add  enctype="multipart/form-data" in the form
     * for ex:  <form action="http://localhost:4848/management/domain/applications/application" method="post" enctype="multipart/form-data">
     * then any param of type="file" will be uploaded, stored locally and the param will use the local location
     * on the server side (ie. just the path)
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    public Object postLegacyFormat(FormDataMultiPart formData) {
        return createOrUpdateEntityLegacyFormat(createDataBasedOnForm(formData)); //execute the deploy command with a copy of the file locally
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(Constants.MEDIA_TYPE_JSON+";qs=0.5")
    public Object post(FormDataMultiPart formData) {
        return createOrUpdateEntity(createDataBasedOnForm(formData)); //execute the deploy command with a copy of the file locally
    }

    @DELETE
    public Response delete(HashMap<String, String> data) {
        return Response.ok(ResourceUtil.getActionReportResult(doDelete(data),
                localStrings.getLocalString("rest.resource.delete.message", "\"{0}\" deleted successfully.",
                new Object[]{ uriInfo.getAbsolutePath() }),
                requestHeaders,
                uriInfo))
                .build(); //200 - ok
    }

    @OPTIONS
    public ActionReportResult optionsLegacyFormat() {
        return buildActionReportResult(false);
    }

    @OPTIONS
    @Produces(Constants.MEDIA_TYPE_JSON+";qs=0.5")
    public RestResourceMetadata options() {
        return new RestResourceMetadata(this);
    }

    /**
     * This method performs the creation or updating of an entity, regardless of the
     * request's mime type.  If an error occurs, a <code>WebApplicationException</code>
     * is thrown, so if the method returns, the create/update was successful.
     * @param data
     * @return
     */
    protected RestActionReporter doCreateOrUpdate(HashMap<String, String> data) {
        if (data == null) {
            data = new HashMap<String, String>();
        }
        try {
            //data.remove("submit");
            removeAttributesToBeSkipped(data);
            if (data.containsKey("error")) {
                throw new WebApplicationException(Response.status(400)
                        .entity(ResourceUtil.getActionReportResult(ActionReport.ExitCode.FAILURE,
                            localStrings.getLocalString("rest.request.parsing.error", "Unable to parse the input entity. Please check the syntax."),
                            requestHeaders, uriInfo)).build());
            }

            ResourceUtil.purgeEmptyEntries(data);

            //hack-1 : support delete method for html
            //Currently, browsers do not support delete method. For html media,
            //delete operations can be supported through POST. Redirect html
            //client POST request for delete operation to DELETE method.
            if ("__deleteoperation".equals(data.get("operation"))) {
                data.remove("operation");
                delete(data);
                return new RestActionReporter();
            }
            //just update it.
            data = ResourceUtil.translateCamelCasedNamesToXMLNames(data);
            RestActionReporter ar = Util.applyChanges(data, uriInfo, getSubject());
            if (ar.getActionExitCode() != ActionReport.ExitCode.SUCCESS) {
                throwError(Status.BAD_REQUEST, "Could not apply changes" + ar.getMessage()); // i18n
            }

            return ar;
        } catch (Exception ex) {
            throw new WebApplicationException(ex, Response.Status.INTERNAL_SERVER_ERROR);
        }
    }

    protected ExitCode doDelete(HashMap<String, String> data) {
        if (data == null) {
            data = new HashMap<String, String>();
        }
        if (entity == null) {//wrong resource
//            return Response.status(404).entity(ResourceUtil.getActionReportResult(ActionReport.ExitCode.FAILURE, errorMessage, requestHeaders, uriInfo)).build();
            throwError(Status.NOT_FOUND,
                localStrings.getLocalString("rest.resource.erromessage.noentity", "Resource not found."));
        }

        if (getDeleteCommand() == null) {
            String message = localStrings.getLocalString("rest.resource.delete.forbidden",
                    "DELETE on \"{0}\" is forbidden.", new Object[]{uriInfo.getAbsolutePath()});
            throwError(Status.FORBIDDEN, message);
        }

        if (getDeleteCommand().equals("GENERIC-DELETE")) {
            try {
                ConfigBean p = (ConfigBean) parent;
                if (parent == null) {
                    p = (ConfigBean) entity.parent();
                }
                ConfigSupport.deleteChild(p, (ConfigBean) entity);
                return ExitCode.SUCCESS;
            } catch (TransactionFailure ex) {
                throw new WebApplicationException(ex, Response.Status.INTERNAL_SERVER_ERROR);
            }
        }

        //do the delete via the command:
        if (data.containsKey("error")) {
            throwError(Status.BAD_REQUEST,
                    localStrings.getLocalString("rest.request.parsing.error",
                    "Unable to parse the input entity. Please check the syntax."));
        }

        ResourceUtil.addQueryString(uriInfo.getQueryParameters(), data);
        ResourceUtil.purgeEmptyEntries(data);
        ResourceUtil.adjustParameters(data);

        if (data.get("DEFAULT") == null) {
            addDefaultParameter(data);
        } else {
            String resourceName = getResourceName(uriInfo.getAbsolutePath().getPath(), "/");
            if (!data.get("DEFAULT").equals(resourceName)) {
                throwError(Status.FORBIDDEN,
                        localStrings.getLocalString("rest.resource.not.deleted",
                        "Resource not deleted. Value of \"name\" should be the name of this resource."));
            }
        }

        RestActionReporter actionReport = runCommand(getDeleteCommand(), data);

        if (actionReport != null) {
            ActionReport.ExitCode exitCode = actionReport.getActionExitCode();
            if (exitCode != ActionReport.ExitCode.FAILURE) {
                return exitCode;
            }

            throwError(Status.BAD_REQUEST, actionReport.getMessage());
        }

        throw new WebApplicationException(handleError(Status.BAD_REQUEST,
                localStrings.getLocalString("rest.resource.delete.forbidden",
                "DELETE on \"{0}\" is forbidden.", new Object[]{uriInfo.getAbsolutePath()})));
    }

    @Override
    public UriInfo getUriInfo() {
        return this.uriInfo;
    }

    @Override
    public void setUriInfo(UriInfo uriInfo) {
        this.uriInfo = uriInfo;
    }

    public void setEntity(Dom p) {
        entity = p;
        childModel = p.model;
    }

    public Dom getEntity() {
        return entity;
    }

    public void setParentAndTagName(Dom parent, String tagName) {

        if (parent == null) { //prevent https://glassfish.dev.java.net/issues/show_bug.cgi?id=14125
            throw new WebApplicationException(Response.Status.NOT_FOUND);
        }
        this.parent = parent;
        this.tagName = tagName;
        entity = parent.nodeElement(tagName);
        if (entity == null) {
            // In some cases, the tagName requested is not found in the DOM tree.  This is true,
            // for example, for the various ZeroConf elements (e.g., transaction-service).  If
            // the zero conf element is not in domain.xml, then it won't be in the Dom tree
            // returned by HK2.  If that's the case, we can use ConfigModularityUtils.getOwningObject()
            // to find the ConfigBean matching the path requested, which will add the node to
            // the Dom tree. Once that's done, we can return that node and proceed as normal
            String location = buildPath(parent) + "/" + tagName;
            if (location.startsWith("domain/configs")) {
                final ConfigModularityUtils cmu = locatorBridge.getRemoteLocator().<ConfigModularityUtils>getService(ConfigModularityUtils.class);
                ConfigBeanProxy cbp = cmu.getOwningObject(location);
                if (cbp == null) {
                    cbp = cmu.getConfigBeanInstanceFor(cmu.getOwningClassForLocation(location));
                }
                if (cbp != null) {
                    entity = Dom.unwrap(cbp);
                    childModel = entity.model;
                }
            }
            //throw new WebApplicationException(new Exception("Trying to create an entity using generic create"),Response.Status.INTERNAL_SERVER_ERROR);
        } else {
            childModel = entity.model;
        }
    }

    /**
     * This method will build the path string as needed by ConfigModularityUtils.getOwningObject().
     * There is a mismatch between what the method expects and the way the REST URIs are constructed.
     * For example, for the transaction-service element, the REST URI, stripped of the HTTP and
     * server context information, looks like this:
     * /domain/configs/config/server-config/transaction-service.  The format expected by the
     * getOwningObject(), however, looks like this:
     * domain/configs/server-config/transaction-service. In the REST URIs, if there is a collection of
     * Named items, the type of the collection is inserted into the URI ("config" here) followed by
     * the name of the particular instance ("server-config").  In building the path, we must identify
     * Named instances and insert the name of the instance rather than the type.  We apply this logic
     * as we recurse up to the top of the Dom tree to finish building the path desired.
     * @param node
     * @return
     */
    private String buildPath (Dom node) {
        final Dom parentNode = node.parent();
        String part = node.model.getTagName();
        String name = node.attribute("name");
        if (name != null) {
            part = name;
        }
        return (parentNode != null) ? (buildPath(parentNode) + "/" + part) : part;
    }

    /**
     * allows for remote files to be put in a tmp area and we pass the
     * local location of this file to the corresponding command instead of the content of the file
     * * Yu need to add  enctype="multipart/form-data" in the form
     * for ex:  <form action="http://localhost:4848/management/domain/applications/application" method="post" enctype="multipart/form-data">
     * then any param of type="file" will be uploaded, stored locally and the param will use the local location
     * on the server side (ie. just the path)
     */
    public static HashMap<String, String> createDataBasedOnForm(FormDataMultiPart formData) {
        HashMap<String, String> data = new HashMap<String, String>();
        try {
            //data passed to the generic command running
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
                        data.put(fieldName, f.getAbsolutePath());

                    } else {
                        data.put(fieldName, bodyPart.getValue());
                    }
                }
            }
        } catch (Exception ex) {
            RestLogging.restLogger.log(Level.SEVERE, null, ex);
        } finally {
            formData.cleanup();
        }
        return data;

    }

    /*
     * This method is called by the ASM generated code change very carefully
     */
    public void setBeanByKey(List<Dom> parentList, String id, String tag) {
        this.tagName = tag;
        try {
            childID = URLDecoder.decode(id, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            childID = id;
        }
        if (parentList != null) { // Believe it or not, this can happen
            for (Dom c : parentList) {
                String keyAttributeName = null;
                ConfigModel model = c.model;
                if (model.key == null) {
                    try {
                        for (String s : model.getAttributeNames()) {//no key, by default use the name attr
                            if (s.equals("name")) {
                                keyAttributeName = s;
                            }
                        }
                        if (keyAttributeName == null) {//nothing, so pick the first one
                            keyAttributeName = model.getAttributeNames().iterator().next();
                        }
                    } catch (Exception e) {
                        keyAttributeName = "ThisIsAModelBug:NoKeyAttr"; //no attr choice fo a key!!! Error!!!
                    } //firstone
                } else {
                    keyAttributeName = model.key.substring(1, model.key.length());
                }

                String keyvalue = c.attribute(keyAttributeName.toLowerCase(Locale.US));
                if (keyvalue.equals(childID)) {
                    setEntity((ConfigBean) c);
                }
            }
        }
    }

    protected ActionReportResult buildActionReportResult(boolean showEntityValues) {
        RestActionReporter ar = new RestActionReporter();
        ar.setExtraProperties(new Properties());
        ConfigBean entity = (ConfigBean) getEntity();
        if (childID != null) {
            ar.setActionDescription(childID);

        } else if (childModel != null) {
            ar.setActionDescription(childModel.getTagName());
        }
        if (showEntityValues) {
            if (entity != null) {
                ar.getExtraProperties().put("entity", getAttributes(entity));
            }
        }
        OptionsResult optionsResult = new OptionsResult(Util.getResourceName(uriInfo));
        Map<String, MethodMetaData> mmd = getMethodMetaData();
        optionsResult.putMethodMetaData("GET", mmd.get("GET"));
        optionsResult.putMethodMetaData("POST", mmd.get("POST"));
        optionsResult.putMethodMetaData("DELETE", mmd.get("DELETE"));

        ResourceUtil.addMethodMetaData(ar, mmd);
        if (entity != null) {
            ar.getExtraProperties().put("childResources", ResourceUtil.getResourceLinks(entity, uriInfo,
                    ResourceUtil.canShowDeprecatedItems(locatorBridge.getRemoteLocator())));
        }
        ar.getExtraProperties().put("commands", ResourceUtil.getCommandLinks(getCommandResourcesPaths()));

        return new ActionReportResult(ar, entity, optionsResult);
    }

    protected void removeAttributesToBeSkipped(Map<String, String> data) {
        for (String item : attributesToSkip) {
            data.remove(item);
        }
    }

    protected String[][] getCommandResourcesPaths() {
        return new String[][]{};
    }

    protected String getDeleteCommand() {
        if (entity == null) {
            return null;
        }
        String result =
                ResourceUtil.getCommand(RestRedirect.OpType.DELETE, getEntity().model);

        if ((result == null) && (entity.parent() != null)) {
            //trying @Delete annotation that as a generic CRUD delete command, possibly...
            Class<? extends ConfigBeanProxy> cbp = null;
            try {
                cbp = (Class<? extends ConfigBeanProxy>) entity.parent().model.classLoaderHolder.loadClass(entity.parent().model.targetTypeName);
            } catch (MultiException e) {
                return null;//
            }
            Delete del = null;
            for (Method m : cbp.getMethods()) {
                ConfigModel.Property pp = entity.parent().model.toProperty(m);
                if ((pp != null) && (pp.xmlName.equals(tagName)) && m.isAnnotationPresent(Delete.class)) {
                    del = m.getAnnotation(Delete.class);
                    break;
                }
            }
            if (del != null) {
                return del.value();
            }

        }
        return result;
    }

    /**
     * Returns the list of command resource paths [command, http method, url/path]
     *
     * @return
     */
    private RestActionReporter runCommand(String commandName, HashMap<String, String> data) {
        if (commandName != null) {
            return ResourceUtil.runCommand(commandName, data, getSubject());
        }

        return null;//not processed
    }

    // This has to be smarter, since we are encoding / in resource names now
    private void addDefaultParameter(HashMap<String, String> data) {
        String defaultParameterValue = getEntity().getKey();
        if (defaultParameterValue == null) {// no primary key
            //we take the parent key.
            // see for example delete-ssl that that the parent key name as ssl does not have a key
            defaultParameterValue = parent.getKey();
        }
        data.put("DEFAULT", defaultParameterValue);
    }

    private String getResourceName(String absoluteName, String delimiter) {
        if (null == absoluteName) {
            return absoluteName;
        }
        int index = absoluteName.lastIndexOf(delimiter);
        if (index != -1) {
            index = index + delimiter.length();
            return absoluteName.substring(index);
        } else {
            return absoluteName;
        }
    }

    //******************************************************************************************************************
    private Map<String, String> getAttributes(Dom entity) {
        Map<String, String> result = new TreeMap<String, String>();
        Set<String> attributeNames = entity.model.getAttributeNames();
        for (String attributeName : attributeNames) {
            result.put(eleminateHypen(attributeName), entity.attribute(attributeName));
        }

        return result;
    }

    private Map<String, MethodMetaData> getMethodMetaData() {
        Map<String, MethodMetaData> map = new TreeMap<String, MethodMetaData>();
        //GET meta data
        map.put("GET", new MethodMetaData());

        /////optionsResult.putMethodMetaData("POST", new MethodMetaData());
        MethodMetaData postMethodMetaData = ResourceUtil.getMethodMetaData(childModel);
        map.put("POST", postMethodMetaData);


        //DELETE meta data
        String command = getDeleteCommand();
        if (command != null) {
            MethodMetaData deleteMethodMetaData;
            if (command.equals("GENERIC-DELETE")) {
                deleteMethodMetaData = new MethodMetaData();
            } else {
                deleteMethodMetaData = ResourceUtil.getMethodMetaData(
                        command, locatorBridge.getRemoteLocator());

                //In case of delete operation(command), do not  display/provide id attribute.
                deleteMethodMetaData.removeParamMetaData("id");
            }

            map.put("DELETE", deleteMethodMetaData);
        }
        return map;
    }

    protected void throwError(final Status error, final String message) throws WebApplicationException {
        throw new WebApplicationException(handleError(error, message));
    }

    protected Response handleError(final Status error, final String message) throws WebApplicationException {
        //TODO better error handling.
//                return Response.status(400).entity(ResourceUtil.getActionReportResult(ar, "Could not apply changes" + ar.getMessage(), requestHeaders, uriInfo)).build();
        return Response.status(error).entity(message).build();
    }
}
