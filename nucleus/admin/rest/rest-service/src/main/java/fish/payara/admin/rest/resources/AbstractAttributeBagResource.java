/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2020-2021] Payara Foundation and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.util.LocalStringManagerImpl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.glassfish.admin.rest.resources.AbstractResource;
import org.glassfish.admin.rest.results.ActionReportResult;
import org.glassfish.admin.rest.results.OptionsResult;
import org.glassfish.admin.rest.utils.Util;
import org.glassfish.admin.rest.utils.xml.RestActionReporter;
import org.glassfish.api.ActionReport;
import org.glassfish.config.support.TranslatedConfigView;

import org.jvnet.hk2.config.Dom;
import org.jvnet.hk2.config.TransactionFailure;

/**
 * Resource for managing monitored attributes. Supports GET, POST, PUT and
 * DELETE commands.
 */
public abstract class AbstractAttributeBagResource extends AbstractResource {

    public static final LocalStringManagerImpl LOCAL_STRINGS = new LocalStringManagerImpl(AbstractAttributeBagResource.class);

    /**
     * A list of attributes in the service.
     */
    protected List<Dom> entity;

    /**
     * The service containing the attributes.
     */
    protected Dom parent;

    /**
     * The getPropertiesName of the element the attributes are stored under.
     */
    protected String tagName;

    public abstract String getDescriptionName();

    public abstract String getPropertiesName();

    public abstract String getnodeElementName();

    /**
     * Gets all of the attributes in the entity.
     *
     * @return a list of the attributes
     */
    public abstract List<Map<String, String>> getAllAttributes();

    /**
     * Gets the attributes.
     *
     * @return a list of the attributes after the transaction.
     */
    @GET
    @Produces({MediaType.TEXT_HTML, MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public ActionReportResult get() {

        RestActionReporter ar = new RestActionReporter();
        ar.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        ar.setActionDescription(getDescriptionName());

        List attributes = getAllAttributes();

        Properties extraProperties = new Properties();
        extraProperties.put(getPropertiesName(), attributes);
        ar.setExtraProperties(extraProperties);

        return new ActionReportResult(tagName, ar, new OptionsResult(Util.getResourceName(uriInfo)));
    }

    /**
     * Creates new attributes. This method deletes all of the existing
     * attributes.
     *
     * @param attributes the list of attributes to be created.
     * @return a list of the attributes after the transaction.
     */
    @PUT
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.APPLICATION_FORM_URLENCODED})
    @Produces({MediaType.TEXT_HTML, MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public ActionReportResult put(List<Map<String, String>> attributes) {

        RestActionReporter ar = new RestActionReporter();
        ar.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        ar.setActionDescription(getDescriptionName());

        try {
            setAttributes(attributes);

            List currentAttributes = getAllAttributes();

            Properties extraProperties = new Properties();
            extraProperties.put(getPropertiesName(), currentAttributes);
            ar.setExtraProperties(extraProperties);
        } catch (TransactionFailure ex) {
            ar.setActionExitCode(ActionReport.ExitCode.FAILURE);
            ar.setMessage(ex.getMessage());
        }

        return new ActionReportResult(tagName, ar, new OptionsResult(Util.getResourceName(uriInfo)));

    }

    /**
     * Creates new attributes. Existing attributes will be ignored, and others
     * will be created.
     *
     * @param attributes the list of attributes to be created.
     * @return a list of the attributes after the transaction.
     */
    @POST
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.APPLICATION_FORM_URLENCODED})
    @Produces({MediaType.TEXT_HTML, MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public ActionReportResult post(List<Map<String, String>> attributes) {
        List<Map<String, String>> currentAttributes = getAllAttributes();
        attributes.addAll(currentAttributes);
        return put(attributes);
    }

    /**
     * Deletes all attributes.
     *
     * @return a list of the attributes after the transaction.
     */
    @DELETE
    @Produces({MediaType.TEXT_HTML, MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public ActionReportResult delete() {
        List<Map<String, String>> emptyList = new ArrayList<>();
        return put(emptyList);
    }

    /**
     * Sets the attribute list to the specified list.
     *
     * @param attributes the intended list of attributes.
     * @throws TransactionFailure if an error occurs in removing or adding
     * attributes.
     */
    public void setAttributes(List<Map<String, String>> attributes) throws TransactionFailure {
        TranslatedConfigView.doSubstitution.set(false);
        List<Map<String, String>> existingAttributes = getAllAttributes();

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

        excuteSetCommand(attributesToAdd, attributesToDelete);
        synchronized (parent) {
            entity = parent.nodeElements(getnodeElementName());

        }
    }

    public abstract void excuteSetCommand(List<Map<String, String>> attributesToAdd, List<Map<String, String>> attributesToDelete) throws TransactionFailure;

    public abstract boolean attributesAreEqual(Map<String, String> attribute1, Map<String, String> attribute2);

    /**
     * Sets the parent and the tag getPropertiesName of the resource. The Dom
     * entity will be derived from this information.
     *
     * @param parent
     * @param tagName
     */
    public void setParentAndTagName(Dom parent, String tagName) {
        this.parent = parent;
        this.tagName = tagName;
        if (parent != null) {
            synchronized (parent) {
                entity = parent.nodeElements(getnodeElementName());
            }
        }
    }
}
