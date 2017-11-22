package fish.payara.admin.rest.resources;

import org.glassfish.admin.rest.resources.*;
import com.sun.enterprise.util.LocalStringManagerImpl;

import java.util.*;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import org.glassfish.admin.rest.utils.Util;
import org.glassfish.admin.rest.results.ActionReportResult;
import org.glassfish.admin.rest.results.OptionsResult;
import org.glassfish.admin.rest.utils.ResourceUtil;
import org.glassfish.admin.rest.utils.xml.RestActionReporter;
import org.glassfish.api.ActionReport;
import org.jvnet.hk2.config.Dom;

/**
 * Resource for managing monitored attributes. Supports GET, POST, PUT and
 * DELETE commands.
 */
public class MonitoredAttributeBagResource extends AbstractResource {

    public final static LocalStringManagerImpl LOCAL_STRINGS = new LocalStringManagerImpl(MonitoredAttributeBagResource.class);

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
     * @return a list of the monitored-attributes in the specified format.
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
     * Creates new monitored-attributes. Existing monitored-attributes will be
     * ignored, and others will be created.
     *
     * @param data the list of monitored-attributes to be created.
     * @return a list of the monitored-attributes after the transaction.
     */
    @POST
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.APPLICATION_FORM_URLENCODED})
    @Produces({MediaType.TEXT_HTML, MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public ActionReportResult post(List<Map<String, String>> data) {
        List<Map<String, String>> currentData = getMonitoredAttributes();
        data.addAll(currentData);
        return put(data);
    }

    /**
     * Creates new monitored-attributes. This method deletes all of the existing
     * monitored-attributes.
     *
     * @param data the list of monitored-attributes to be created.
     * @return a list of the monitored-attributes after the transaction.
     */
    @PUT
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.APPLICATION_FORM_URLENCODED})
    @Produces({MediaType.TEXT_HTML, MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public ActionReportResult put(List<Map<String, String>> data) {
        setMonitoredAttributes(data);
        return get();
    }

    /**
     * Deletes the specified monitored-attributes. All specified
     * monitored-attributes will not exist after this transaction.
     *
     * @param data the list of the monitored-attributes to be deleted.
     * @return a list of the monitored-attributes which were deleted.
     */
    @DELETE
    @Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.APPLICATION_FORM_URLENCODED})
    @Produces({MediaType.TEXT_HTML, MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
    public ActionReportResult delete(List<Map<String, String>> data) {
        return get();
    }

    /**
     * Gets all of the monitored attributes in the entity.
     *
     * @return a list of the monitored attributes
     */
    public List<Map<String, String>> getMonitoredAttributes() {
        List<Map<String, String>> monitoredAttributes = new ArrayList<>();

        for (Dom child : entity) {
            Map<String, String> entry = new HashMap<>();

            entry.put("attributeName", child.attribute("attribute-name"));
            entry.put("objectName", child.attribute("object-name"));
            String description = child.attribute("description");
            if (description != null) {
                entry.put("description", description);
            }

            monitoredAttributes.add(entry);
        }
        return monitoredAttributes;
    }

    public void setMonitoredAttributes(List<Map<String, String>> monitoredAttributes) {
        for (Map<String, String> currentAttribute : getMonitoredAttributes()) {
            Map<String, String> parameters = new HashMap<>();
            parameters.put("delattribute", String.format("attributeName=%s objectName=%s", currentAttribute.get("attributeName"), currentAttribute.get("objectName")));
            ResourceUtil.runCommand("set-monitoring-configuration", parameters, getSubject());
        }
        for (Map<String, String> data : monitoredAttributes) {
            Map<String, String> parameters = new HashMap<>();
            parameters.put("addattribute", String.format("attributeName=%s objectName=%s", data.get("attributeName"), data.get("objectName")));
            ResourceUtil.runCommand("set-monitoring-configuration", parameters, getSubject());
        }
    }

    @Path("{ObjectName}/{AttributeName}")
    @GET
    public MonitoredAttributeResource getOne(@PathParam("ObjectName") String objectName, @PathParam("AttributeName") String attributeName) {
        MonitoredAttributeResource resource
                = serviceLocator.createAndInitialize(MonitoredAttributeResource.class);
        resource.setBeanByKey(entity, objectName + "/" + attributeName, tagName);
        return resource;
    }

    public static class MonitoredAttributeResource extends TemplateRestResource {

        @Override
        public String getDeleteCommand() {
            return "GENERIC-DELETE";
        }
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
            entity = parent.nodeElements("monitored-attributes");
        }
    }
}
