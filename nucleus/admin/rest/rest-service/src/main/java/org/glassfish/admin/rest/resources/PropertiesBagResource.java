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

import com.sun.enterprise.util.LocalStringManagerImpl;

import java.util.*;
import javax.validation.ValidationException;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import org.glassfish.admin.rest.utils.ResourceUtil;
import org.glassfish.admin.rest.utils.Util;
import org.glassfish.admin.rest.results.ActionReportResult;
import org.glassfish.admin.rest.results.GetResultList;
import org.glassfish.admin.rest.results.OptionsResult;
import org.glassfish.admin.rest.utils.xml.RestActionReporter;
import org.glassfish.api.ActionReport;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.config.Dom;
import org.jvnet.hk2.config.types.Property;
import org.jvnet.hk2.config.TransactionFailure;

/**
 *
 * @author jasonlee
 */
public class PropertiesBagResource extends AbstractResource {
    protected List<Dom> entity;
    protected Dom parent;
    protected String tagName;
    public final static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(PropertiesBagResource.class);

    static public class PropertyResource extends TemplateRestResource {
        @Override
        public String getDeleteCommand() {
            return "GENERIC-DELETE";
        }
    }
    @Path("{Name}/")
    public PropertyResource getProperty(@PathParam("Name") String id) {
        PropertyResource resource = serviceLocator.createAndInitialize(PropertyResource.class);
        resource.setBeanByKey(getEntity(), id, tagName);
        return resource;
    }

    @GET
    @Produces({"text/html", MediaType.APPLICATION_JSON+";qs=0.5", MediaType.APPLICATION_XML+";qs=0.5"})
    public Object get() {
        List<Dom> entities = getEntity();
        if (entities == null) {
            return new GetResultList(new ArrayList(), "", new String[][]{}, new OptionsResult(Util.getResourceName(uriInfo)));//empty dom list
        }

        RestActionReporter ar = new RestActionReporter();
        ar.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        ar.setActionDescription("property");
        List properties = new ArrayList();

        for (Dom child : entities) {
            Map<String, String> entry = new HashMap<String, String>();
            entry.put("name", child.attribute("name"));
            entry.put("value", child.attribute("value"));
            String description = child.attribute("description");
            if (description != null) {
                entry.put("description", description);
            }

            properties.add(entry);
        }

        Properties extraProperties = new Properties();
        extraProperties.put("properties", properties);
        ar.setExtraProperties(extraProperties);

        return new ActionReportResult("properties", ar, new OptionsResult(Util.getResourceName(uriInfo)));
    }

    @POST  // create
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.APPLICATION_FORM_URLENCODED})
    @Produces({"text/html", MediaType.APPLICATION_JSON+";qs=0.5", MediaType.APPLICATION_XML+";qs=0.5"})
    public ActionReportResult createProperties(List<Map<String, String>> data) {
        return clearThenSaveProperties(data);
    }

    @PUT  // create
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.APPLICATION_FORM_URLENCODED})
    @Produces({"text/html", MediaType.APPLICATION_JSON+";qs=0.5", MediaType.APPLICATION_XML+";qs=0.5"})
    public ActionReportResult replaceProperties(List<Map<String, String>> data) {
        return clearThenSaveProperties(data);
    }

    @DELETE
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.APPLICATION_FORM_URLENCODED, MediaType.APPLICATION_OCTET_STREAM})
    @Produces({"text/html", MediaType.APPLICATION_JSON+";qs=0.5", MediaType.APPLICATION_XML+";qs=0.5"})
    public Response delete() {
        try {
            Map<String, Property> existing = getExistingProperties();
            deleteMissingProperties(existing, null);

            String successMessage = localStrings.getLocalString("rest.resource.delete.message",
                        "\"{0}\" deleted successfully.", new Object[]{uriInfo.getAbsolutePath()});
            return ResourceUtil.getResponse(200, successMessage, requestHeaders, uriInfo);
        } catch (Exception ex) {
            if (ex.getCause() instanceof ValidationException) {
                return ResourceUtil.getResponse(400, /*400 - bad request*/ ex.getMessage(), requestHeaders, uriInfo);
            } else {
                throw new WebApplicationException(ex, Response.Status.INTERNAL_SERVER_ERROR);
            }
        }
    }
    /*
     * prop names that have . in them need to be entered with \. for the set command
     * so this routine replaces . with \.
     */
    private String getEscapedPropertyName(String propName){
        return propName.replaceAll("\\.","\\\\.");
    }

    protected ActionReportResult clearThenSaveProperties(List<Map<String, String>> properties) {
        RestActionReporter ar = new RestActionReporter();
        ar.setActionDescription("property");
        try {
            Map<String, Property> existing = getExistingProperties();
            deleteMissingProperties(existing, properties);
            Map<String, String> data = new LinkedHashMap<String, String>();

            for (Map<String, String> property : properties) {
                Property existingProp = existing.get(property.get("name"));
                String escapedName = getEscapedPropertyName(property.get("name"));
                String value = property.get("value");
                String description = property.get("description");
                final String unescapedValue = value.replaceAll("\\\\", "");

                // the prop name can not contain .
                // need to remove the . test when http://java.net/jira/browse/GLASSFISH-15418  is fixed
                boolean canSaveDesc = property.get("name").indexOf(".") == -1;

                if ((existingProp == null) || !unescapedValue.equals(existingProp.getValue())) {
                    data.put(escapedName, property.get("value"));
                    if (canSaveDesc && (description != null)) {
                        data.put(escapedName + ".description", description);
                    }
                }

                //update the description only if not null/blank
                if ((description != null) && (existingProp != null)) {
                     if (!"".equals(description) && (!description.equals(existingProp.getDescription()))) {
                        if (canSaveDesc) {
                            data.put(escapedName + ".description", description);
                        }
                    }
                }
            }

            if (!data.isEmpty()) {
                Util.applyChanges(data, uriInfo, getSubject());
            }

            String successMessage = localStrings.getLocalString("rest.resource.update.message",
                    "\"{0}\" updated successfully.", new Object[]{uriInfo.getAbsolutePath()});

            ar.setSuccess();
            ar.setMessage(successMessage);
        } catch (Exception ex) {
            if (ex.getCause() instanceof ValidationException) {
                ar.setFailure();
                ar.setFailureCause(ex);
                ar.setMessage(ex.getLocalizedMessage());
            } else {
                throw new WebApplicationException(ex, Response.Status.INTERNAL_SERVER_ERROR);
            }
        }

        return new ActionReportResult("properties", ar, new OptionsResult(Util.getResourceName(uriInfo)));
    }

    protected Map<String, Property> getExistingProperties() {
        Map<String, Property> properties = new HashMap<>();
        if (parent != null) {
            for (Dom child : parent.nodeElements(tagName)) {
                Property property = child.createProxy();
                properties.put(property.getName(), property);
            }
        }
        return properties;
    }

    protected void deleteMissingProperties(Map<String, Property> existing, List<Map<String, String>> properties) throws TransactionFailure {
        Set<String> propNames = new HashSet<String>();
        if (properties != null) {
            for (Map<String, String> property : properties) {
                propNames.add(property.get("name"));
            }
        }

        HashMap<String, String> data = new HashMap<String, String>();
        for (final Property existingProp : existing.values()) {
            if (!propNames.contains(existingProp.getName())) {
                String escapedName = getEscapedPropertyName(existingProp.getName());
                data.put (escapedName, "");
            }
        }
        if (!data.isEmpty()) {
            Util.applyChanges(data, uriInfo, getSubject());
        }
    }

    public void setEntity(List<Dom> p) {
        entity = p;
    }

    public List<Dom> getEntity() {
        return entity;
    }

    public void setParentAndTagName(Dom parent, String tagName) {
        this.parent = parent;
        this.tagName = tagName;
        if (parent != null) {
            entity = parent.nodeElements(tagName);
        }
    }
}
