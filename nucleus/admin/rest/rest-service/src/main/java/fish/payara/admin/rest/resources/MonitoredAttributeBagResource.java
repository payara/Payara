/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017-2019 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.admin.rest.resources;

import org.glassfish.admin.rest.resources.*;
import com.sun.enterprise.util.LocalStringManagerImpl;

import java.util.*;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.glassfish.admin.rest.utils.Util;
import org.glassfish.admin.rest.results.ActionReportResult;
import org.glassfish.admin.rest.results.OptionsResult;
import org.glassfish.admin.rest.utils.ResourceUtil;
import org.glassfish.admin.rest.utils.xml.RestActionReporter;
import org.glassfish.api.ActionReport;
import org.glassfish.config.support.TranslatedConfigView;
import org.jvnet.hk2.config.Dom;
import org.jvnet.hk2.config.TransactionFailure;

/**
 * Resource for managing monitored attributes. Supports GET, POST, PUT and
 * DELETE commands.
 */
public class MonitoredAttributeBagResource extends AbstractResource {

    public static final LocalStringManagerImpl LOCAL_STRINGS = new LocalStringManagerImpl(MonitoredAttributeBagResource.class);

    /**
     * A list of the monitored attributes in the service.
     */
    protected List<Dom> entity;

    /**
     * The service containing the monitored attributes.
     */
    protected Dom parent;

    /**
     * The name of the element the monitored attributes are stored under.
     */
    protected String tagName;

    /**
     * Gets the monitored-attributes.
     *
     * @return a list of the monitored-attributes after the transaction.
     */
    @GET
    @Produces({MediaType.TEXT_HTML, MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public ActionReportResult get() {

        RestActionReporter ar = new RestActionReporter();
        ar.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        ar.setActionDescription("monitored-attribute");

        List monitoredAttributes = getMonitoredAttributes();

        Properties extraProperties = new Properties();
        extraProperties.put("monitoredAttributes", monitoredAttributes);
        ar.setExtraProperties(extraProperties);

        return new ActionReportResult(tagName, ar, new OptionsResult(Util.getResourceName(uriInfo)));
    }

    /**
     * Creates new monitored-attributes. This method deletes all of the existing
     * monitored-attributes.
     *
     * @param attributes the list of monitored-attributes to be created.
     * @return a list of the monitored-attributes after the transaction.
     */
    @PUT
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.APPLICATION_FORM_URLENCODED})
    @Produces({MediaType.TEXT_HTML, MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public ActionReportResult put(List<Map<String, String>> attributes) {

        RestActionReporter ar = new RestActionReporter();
        ar.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        ar.setActionDescription("monitored-attribute");

        try {
            setMonitoredAttributes(attributes);

            List monitoredAttributes = getMonitoredAttributes();

            Properties extraProperties = new Properties();
            extraProperties.put("monitoredAttributes", monitoredAttributes);
            ar.setExtraProperties(extraProperties);
        } catch (TransactionFailure ex) {
            ar.setActionExitCode(ActionReport.ExitCode.FAILURE);
            ar.setMessage(ex.getMessage());
        }

        return new ActionReportResult(tagName, ar, new OptionsResult(Util.getResourceName(uriInfo)));
    }

    /**
     * Creates new monitored-attributes. Existing monitored-attributes will be
     * ignored, and others will be created.
     *
     * @param attributes the list of monitored-attributes to be created.
     * @return a list of the monitored-attributes after the transaction.
     */
    @POST
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.APPLICATION_FORM_URLENCODED})
    @Produces({MediaType.TEXT_HTML, MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public ActionReportResult post(List<Map<String, String>> attributes) {
        List<Map<String, String>> currentData = getMonitoredAttributes();
        attributes.addAll(currentData);
        return put(attributes);
    }

    /**
     * Deletes all monitored-attributes.
     *
     * @return a list of the monitored-attributes after the transaction.
     */
    @DELETE
    @Produces({MediaType.TEXT_HTML, MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public ActionReportResult delete() {
        List<Map<String, String>> emptyList = new ArrayList<>();
        return put(emptyList);
    }

    /**
     * Gets all of the monitored attributes in the entity.
     *
     * @return a list of the monitored attributes
     */
    public List<Map<String, String>> getMonitoredAttributes() {
        List<Map<String, String>> attributes = new ArrayList<>();

        for (Dom child : entity) {
            Map<String, String> entry = new HashMap<>();

            entry.put("attributeName", child.attribute("attribute-name"));
            entry.put("objectName", child.attribute("object-name"));
            String description = child.attribute("description");
            if (description != null) {
                entry.put("description", description);
            }

            attributes.add(entry);
        }
        return attributes;
    }

    /**
     * Sets the monitored attribute list to the specified list.
     *
     * @param attributes the intended list of attributes.
     * @throws TransactionFailure if an error occurs in removing or adding
     * attributes.
     */
    public void setMonitoredAttributes(List<Map<String, String>> attributes) throws TransactionFailure {
        TranslatedConfigView.doSubstitution.set(false);
        List<Map<String, String>> existingAttributes = getMonitoredAttributes();

        // Get a list of attributes that need adding
        List<Map<String, String>> attributesToAdd = new ArrayList<>(attributes);
        // Start with the list of specified attributes, and remove any that are also in the existing list
        Iterator<Map<String, String>> iterator = attributesToAdd.iterator();
        while (iterator.hasNext()) {
            Map<String, String> attributeToAdd = iterator.next();
            for (Map<String, String> existingAttribute : existingAttributes) {
                if (attributesAreEqual(existingAttribute, attributeToAdd)) {
                    iterator.remove();
                }
            }
        }

        // Get a list of attributes that need deleting
        List<Map<String, String>> attributesToDelete = new ArrayList<>(existingAttributes);
        // Start with the list of existing attributes, and remove any that aren't also in the specified list
        iterator = attributesToDelete.iterator();
        while (iterator.hasNext()) {
            Map<String, String> attributeToDelete = iterator.next();
            boolean specified = false;
            for (Map<String, String> specifiedAttribute : attributes) {
                if (attributesAreEqual(specifiedAttribute, attributeToDelete)) {
                    specified = true;
                }
            }
            if (specified) {
                iterator.remove();
            }
        }

        try {
            // Add all required attributes
            for (Map<String, String> attribute : attributesToAdd) {
                Map<String, String> parameters = new HashMap<>();
                parameters.put("addattribute", String.format("attributeName=%s objectName=%s description=%s", attribute.get("attributeName"), attribute.get("objectName"), attribute.get("description")));
                RestActionReporter reporter = ResourceUtil.runCommand("set-jmx-monitoring-configuration", parameters, getSubject());
                if (reporter.isFailure()) {
                    throw new TransactionFailure(reporter.getMessage());
                }
            }
            // Delete all unrequired attributes
            for (Map<String, String> attribute : attributesToDelete) {
                Map<String, String> parameters = new HashMap<>();
                parameters.put("delattribute", String.format("attributeName=%s objectName=%s", attribute.get("attributeName"), attribute.get("objectName")));
                RestActionReporter reporter = ResourceUtil.runCommand("set-jmx-monitoring-configuration", parameters, getSubject());
                if (reporter.isFailure()) {
                    throw new TransactionFailure(reporter.getMessage());
                }
            }
        } finally {
            TranslatedConfigView.doSubstitution.set(true);
        }
        synchronized (parent) {
            entity = parent.nodeElements("monitored-attributes");
        }
    }

    private boolean attributesAreEqual(Map<String, String> attribute1, Map<String, String> attribute2) {
        return attribute1.get("attributeName").equals(attribute2.get("attributeName"))
                && attribute1.get("objectName").equals(attribute2.get("objectName"));
    }

    /**
     * Sets the parent and the tag name of the resource. The Dom entity will be
     * derived from this information.
     *
     * @param parent
     * @param tagName
     */
    public void setParentAndTagName(Dom parent, String tagName) {
        this.parent = parent;
        this.tagName = tagName;
        if (parent != null) {
            synchronized (parent) {
                entity = parent.nodeElements("monitored-attributes");
            }
        }
    }
}
