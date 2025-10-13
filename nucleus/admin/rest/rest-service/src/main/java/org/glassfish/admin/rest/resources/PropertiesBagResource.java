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
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2016-2021] [Payara Foundation and/or its affiliates]

package org.glassfish.admin.rest.resources;

import com.sun.enterprise.util.LocalStringManagerImpl;

import java.util.*;
import java.util.logging.Level;
import jakarta.validation.ValidationException;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.glassfish.admin.rest.utils.ResourceUtil;
import org.glassfish.admin.rest.utils.Util;
import org.glassfish.admin.rest.results.ActionReportResult;
import org.glassfish.admin.rest.results.GetResultList;
import org.glassfish.admin.rest.results.OptionsResult;
import org.glassfish.admin.rest.utils.xml.RestActionReporter;
import org.glassfish.api.ActionReport;
import org.glassfish.config.support.TranslatedConfigView;
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
    public static final LocalStringManagerImpl localStrings = new LocalStringManagerImpl(PropertiesBagResource.class);

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
    @Produces({MediaType.TEXT_HTML, MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
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
    @Produces({MediaType.TEXT_HTML, MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public ActionReportResult createProperties(List<Map<String, String>> data) {
        return clearThenSaveProperties(data);
    }

    @PUT  // create
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.APPLICATION_FORM_URLENCODED})
    @Produces({MediaType.TEXT_HTML, MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public ActionReportResult replaceProperties(List<Map<String, String>> data) {
        return clearThenSaveProperties(data);
    }

    @DELETE
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.APPLICATION_FORM_URLENCODED, MediaType.APPLICATION_OCTET_STREAM})
    @Produces({MediaType.TEXT_HTML, MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public Response delete() {
        try {
            Map<String, Property> existing = getExistingProperties();
            deleteMissingProperties(existing, null);

            String successMessage = localStrings.getLocalString("rest.resource.delete.message", "\"{0}\" deleted successfully.", uriInfo.getAbsolutePath());
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
        return propName.replaceAll("\\.","\\\\\\.");
    }

    protected ActionReportResult clearThenSaveProperties(List<Map<String, String>> properties) {
        RestActionReporter ar = new RestActionReporter();
        ar.setActionDescription("property");
        try {
            TranslatedConfigView.doSubstitution.set(Boolean.FALSE);
            Map<String, Property> existing = getExistingProperties();
            deleteMissingProperties(existing, properties);
            Map<String, String> data = new LinkedHashMap<String, String>();
            Map<String, String> descriptionData = new LinkedHashMap<String, String>();

            for (Map<String, String> property : properties) {
                Property existingProp = existing.get(property.get("name"));

                String unescapedName = property.get("name");
                String escapedName = getEscapedPropertyName(unescapedName);

                String value = property.get("value");
                String unescapedValue = value.replaceAll("\\\\", "");
                
                String description = null;
                if (property.get("description") != null) {
                    description = property.get("description");
                }

                // the prop name can not contain .
                // need to remove the . test when http://java.net/jira/browse/GLASSFISH-15418  is fixed
                boolean isDottedName = property.get("name").contains(".");

                if ((existingProp == null) || !unescapedValue.equals(existingProp.getValue())) {
                    
                    data.put(escapedName, property.get("value"));
                    
                    if (isDottedName) {
                        data.put(unescapedName + ".name", unescapedName);
                    }
                    
                    if (description != null) {
                        descriptionData.put(unescapedName + ".description", description);
                    }

                }

                //update the description only if not null/blank
                if ((description != null) && (existingProp != null)) {
                     if (!"".equals(description) && (!description.equals(existingProp.getDescription()))) {
                        descriptionData.put(unescapedName + ".description", description);
                    }
                }
            }

            if (!data.isEmpty() || !descriptionData.isEmpty()) {
                Util.applyChanges(data, uriInfo, getSubject());
                Util.applyChanges(descriptionData, uriInfo, getSubject());
            }

            String successMessage = localStrings.getLocalString("rest.resource.update.message",
                    "\"{0}\" updated successfully.", uriInfo.getAbsolutePath());

            ar.setSuccess();
            ar.setMessage(successMessage);
        } catch (ValidationException ex) {
            ar.setFailure();
            ar.setFailureCause(ex);
            ar.setMessage(ex.getLocalizedMessage());
        } catch (Exception ex) {
                logger.log(Level.FINE, "Error processing properties", ex);
                throw new WebApplicationException(ex, Response.Status.INTERNAL_SERVER_ERROR);
        } finally {
            TranslatedConfigView.doSubstitution.set(Boolean.TRUE);
        }

        return new ActionReportResult("properties", ar, new OptionsResult(Util.getResourceName(uriInfo)));
    }

    protected Map<String, Property> getExistingProperties() {
        Map<String, Property> properties = new HashMap<>();
        if (parent != null) {
            List<Dom> children;
            synchronized (parent) {
                children = parent.nodeElements(tagName);
            }
            for (Dom child : children) {
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
            synchronized (parent) {
                entity = parent.nodeElements(tagName);
            }
        }
    }
}
