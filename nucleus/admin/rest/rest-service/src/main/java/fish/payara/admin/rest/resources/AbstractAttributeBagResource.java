/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package fish.payara.admin.rest.resources;

import com.sun.enterprise.util.LocalStringManagerImpl;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

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
