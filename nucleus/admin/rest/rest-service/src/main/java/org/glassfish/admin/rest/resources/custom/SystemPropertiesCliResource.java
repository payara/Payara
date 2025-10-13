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

package org.glassfish.admin.rest.resources.custom;

import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.PathSegment;
import jakarta.ws.rs.core.Response;

import com.sun.enterprise.config.serverbeans.Cluster;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Server;

import org.glassfish.admin.rest.resources.TemplateExecCommand;
import org.glassfish.admin.rest.results.ActionReportResult;
import org.glassfish.admin.rest.results.OptionsResult;
import org.glassfish.admin.rest.utils.ResourceUtil;
import org.glassfish.admin.rest.utils.xml.RestActionReporter;
import org.glassfish.api.ActionReport;
import org.glassfish.api.ActionReport.ExitCode;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.config.Dom;

/**
 *
 * @author jasonlee
 */
@Produces({MediaType.TEXT_HTML, MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.APPLICATION_FORM_URLENCODED})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.APPLICATION_FORM_URLENCODED})
@CommandLock(CommandLock.LockType.NONE)
@ExecuteOn({RuntimeType.DAS})
@TargetType(value = {CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG, CommandTarget.DAS,
    CommandTarget.DOMAIN, CommandTarget.STANDALONE_INSTANCE})
public class SystemPropertiesCliResource extends TemplateExecCommand {
    protected static final String TAG_SYSTEM_PROPERTY = "system-property";
    private static final String VALUE = "value";
    private static final String DEFAULT_VALUE = "defaultValue";
    private static final String DESCRIPTION = "description";


    @Context
    protected ServiceLocator injector;

    protected Dom entity;

    protected Domain domain;

    public SystemPropertiesCliResource() {
        super(SystemPropertiesCliResource.class.getSimpleName(), "", "", "", "", true);
    }

    public void setEntity(Dom p) {
        entity = p;
    }

    public Dom getEntity() {
        return entity;
    }

    @GET
    public ActionReportResult get() {
        domain = locatorBridge.getRemoteLocator().getService(Domain.class);

        ParameterMap data = new ParameterMap();
        processCommandParams(data);
        addQueryString(uriInfo.getQueryParameters(), data);
        adjustParameters(data);
        Map<String, Map<String, String>> properties = new TreeMap<String, Map<String, String>>();

        RestActionReporter actionReport = new RestActionReporter();
        getSystemProperties(properties, getEntity(), false);

        actionReport.getExtraProperties().put("systemProperties", new ArrayList<>(properties.values()));
        if (properties.isEmpty()) {
            actionReport.getTopMessagePart().setMessage("Nothing to list."); // i18n
        }
        return new ActionReportResult(commandName, actionReport, new OptionsResult());
    }

    @POST
    public Response create(Map<String, String> data) {
        return saveProperties(data);
    }

    @PUT
    public Response update(Map<String, String> data) {
        data.entrySet().removeIf(entry -> entry.getValue() == null || entry.getValue().isEmpty());
        Response resp = deleteRemovedProperties(data.keySet());
        return (resp == null) ? saveProperties(data) : resp;
    }

    @Path("{Name}/")
    @POST
    public Response getSystemPropertyResource(@PathParam("Name") String id, Map<String, String> data) {
        data.put(id, data.get(VALUE));
        data.remove(VALUE);
        List<PathSegment> segments = uriInfo.getPathSegments(true);
        String grandParent = segments.get(segments.size() - 3).getPath();

        return saveProperties(grandParent, data);
    }

    @Path("{Name}/")
    @DELETE
    public Response deleteSystemProperty(@PathParam("Name") String id, HashMap<String, String> data) {
        List<PathSegment> segments = uriInfo.getPathSegments(true);
        String grandParent = segments.get(segments.size() - 3).getPath();

        return deleteProperty(grandParent, id);
    }

    protected void getSystemProperties(Map<String, Map<String, String>> properties, Dom dom, boolean getDefaults) {
        List<Dom> sysprops;
        synchronized (dom) {
            sysprops = dom.nodeElements(TAG_SYSTEM_PROPERTY);
        }
        if ((sysprops != null) && (!sysprops.isEmpty())) {
            for (Dom sysprop : sysprops) {
                String name = sysprop.attribute("name");
                Map<String, String> currValue = properties.get(name);
                if (currValue == null) {
                    currValue = new HashMap<String, String>();
                    currValue.put("name", name);
                    currValue.put(getDefaults ? DEFAULT_VALUE : VALUE, sysprop.attribute(VALUE));

                    if (sysprop.attribute(DESCRIPTION) != null) {
                        currValue.put(DESCRIPTION, sysprop.attribute(DESCRIPTION));
                    }
                    properties.put(name, currValue);
                } else {
                    // Only add a default value if there isn't one already
                    if (currValue.get(DEFAULT_VALUE) == null) {
                        currValue.put(DEFAULT_VALUE, sysprop.attribute(VALUE));
                    }
                }
            }
        }

        // Figure out how to recurse
        if (dom.getProxyType().equals(Server.class)) {
            // Clustered instance
            if (((Server)dom.createProxy()).getCluster() != null) {
                getSystemProperties(properties, getCluster(dom.parent().parent(), ((Server)dom.createProxy()).getCluster().getName()), true);
            } else {
                // Standalone instance or DAS
                getSystemProperties(properties, getConfig(dom.parent().parent(), dom.attribute("config-ref")), true);
            }
        } else if (dom.getProxyType().equals(Cluster.class)) {
            getSystemProperties(properties, getConfig(dom.parent().parent(), dom.attribute("config-ref")), true);
        }
    }

    protected Dom getCluster(Dom domain, String clusterName) {
        List<Dom> configs;
        Dom clusterElements;
        synchronized (domain) {
            clusterElements = domain.nodeElements("clusters").get(0);
        }
        synchronized (clusterElements) {
            configs = clusterElements.nodeElements("cluster");
        }
        for(Dom config : configs) {
            if (config.attribute("name").equals(clusterName)) {
                return config;
            }
        }
        return null;
    }

    protected Dom getConfig(Dom domain, String configName) {
        Dom rootConfig;
        List<Dom> configs;
        synchronized (domain) {
            rootConfig = domain.nodeElements("configs").get(0);
        }
        synchronized (rootConfig) {
            configs = rootConfig.nodeElements("config");
        }
        for(Dom config : configs) {
            if (config.attribute("name").equals(configName)) {
                return config;
            }
        }
        return null;
    }

    /**
     * Saves the passed map of system properties. Any entry with a null or empty
     * value will be deleted with the delete-system-property command, and the rest
     * will then be created with create-system-properties.
     *
     * @param data a map of properties to create or delete
     * @return the result of the command
     */
    protected Response saveProperties(Map<String, String> data) {
        return saveProperties(null, data);
    }

    /**
     * Saves the passed map of system properties. Any entry with a null or empty
     * value will be deleted with the delete-system-property command, and the rest
     * will then be created with create-system-properties.
     *
     * @param parent the name of the parent object of the target
     * @param data   a map of properties to create or delete
     * @return the result of the command
     */
    protected Response saveProperties(String parent, Map<String, String> data) {

        // Prepare all empty properties for explicit deletion
        Collection<String> emptyProps = new HashSet<>();
        Iterator<Entry<String, String>> dataIterator = data.entrySet().iterator();
        while (dataIterator.hasNext()) {
            Entry<String, String> dataEntry = dataIterator.next();
            String value = dataEntry.getValue();
            if (value == null || value.isEmpty()) {
                dataIterator.remove();
                emptyProps.add(dataEntry.getKey());
            }
        }

        // Delete the prepared properties for deletion
        if (!emptyProps.isEmpty()) {
            Response response = deleteProperties(parent, emptyProps);
            if (data.isEmpty() || response.getStatus() != HttpURLConnection.HTTP_OK) {
                return response;
            }
        }

        return createProperties(null, data);
    }

    /**
     * Create some system properties using the create-system-properties asadmin
     * command.
     *
     * @param parent the name of the parent object of the target
     * @param data   a map of properties to create
     * @return the result of the command
     */
    protected Response createProperties(String parent, Map<String, String> data) {
        String propertiesString = convertPropertyMapToString(data);

        data = new HashMap<String, String>();
        data.put("DEFAULT", propertiesString);
        data.put("target", (parent == null) ? getParent(uriInfo) : parent);

        RestActionReporter actionReport = ResourceUtil.runCommand("create-system-properties", data, getSubject());
        ActionReport.ExitCode exitCode = actionReport.getActionExitCode();
        ActionReportResult results = new ActionReportResult(commandName, actionReport, new OptionsResult());

        int status = HttpURLConnection.HTTP_OK; /*200 - ok*/
        if (exitCode == ActionReport.ExitCode.FAILURE) {
            status = HttpURLConnection.HTTP_INTERNAL_ERROR;
        }

        return Response.status(status).entity(results).build();
    }

    /**
     * Delete some system properties using the delete-system-property asadmin
     * command and aggregating the results.
     *
     * @param parent    the name of the parent object of the target
     * @param propNames the names of the properties to delete
     * @return the result of the commands
     */
    protected Response deleteProperties(String parent, Collection<String> propNames) {
        int status = HttpURLConnection.HTTP_OK;

        // Prepare a report for aggregating results
        RestActionReporter report = new RestActionReporter();
        report.setActionExitCode(ExitCode.SUCCESS);
        report.setMessage("");

        for (String propName : propNames) {
            // Delete the property
            Response response = deleteProperty(null, propName);

            ActionReportResult result = (ActionReportResult) response.getEntity();
            ActionReport resultReport = result.getActionReport();
            ExitCode responseExitCode = resultReport.getActionExitCode();

            // Put the results into the aggregator report
            if (!responseExitCode.equals(ExitCode.SUCCESS)) {
                report.setActionExitCode(responseExitCode);
                if (responseExitCode.equals(ExitCode.FAILURE)) {
                    status = HttpURLConnection.HTTP_INTERNAL_ERROR;
                }
            }
            report.getExtraProperties().putAll(resultReport.getExtraProperties());
            report.setMessage((report.getMessage() + "\n" + resultReport.getMessage()).trim());
            if (response.getStatus() != HttpURLConnection.HTTP_OK){
                return response;
            }
        }

        return Response.status(status).entity(report).build();
    }

    /**
     * Delete a system property using the delete-system-property asadmin command.
     *
     * @param parent   the name of the parent object of the target
     * @param propName the name of the property to delete
     * @return the result of the command
     */
    protected Response deleteProperty(String parent, String propName) {
        ParameterMap pm = new ParameterMap();
        pm.add("DEFAULT", propName);
        pm.add("target", (parent == null) ? getParent(uriInfo) : parent);

        RestActionReporter actionReport = ResourceUtil.runCommand("delete-system-property", pm, getSubject());
        ActionReport.ExitCode exitCode = actionReport.getActionExitCode();
        ActionReportResult results = new ActionReportResult(commandName, actionReport, new OptionsResult());

        int status = HttpURLConnection.HTTP_OK; /*200 - ok*/
        if (exitCode == ActionReport.ExitCode.FAILURE) {
            status = HttpURLConnection.HTTP_INTERNAL_ERROR;
        }
        return Response.status(status).entity(results).build();
    }

    /**
     * Delete any properties not contained in the passed collection.
     *
     * @param newProps the properties to not delete
     * @return null if successful or a Response which contains the error message
     */
    private Response deleteRemovedProperties(Collection<String> newProps) {
        List<String> existingList = new ArrayList<>();
        Dom parent = getEntity();
        if (parent == null) {
            return null;
        }
        List<Dom> existingProps;
        synchronized (parent) {
            existingProps = parent.nodeElements(TAG_SYSTEM_PROPERTY);
        }
        for (Dom existingProp : existingProps) {
            existingList.add(existingProp.attribute("name"));
        }
        //no existing properites,return null
        if (existingList.isEmpty()){
            return null;
        }

        //delete the props thats no longer in the new list.
        for(String onePropName : existingList){
            if (!newProps.contains(onePropName)){
                Response resp = deleteProperty(null, onePropName);
                if (resp.getStatus() != HttpURLConnection.HTTP_OK){
                    return resp;
                }
            }
        }
        return null;
    }

    protected String convertPropertyMapToString(Map<String, String> data) {
        StringBuilder options = new StringBuilder();
        String sep = "";
        for (Map.Entry<String, String> entry : data.entrySet()) {
            final String value = entry.getValue();
            if ((value != null) && !value.isEmpty()) {
                options.append(sep)
                        .append(entry.getKey())
                        .append("=")
                        .append(value.replaceAll(":", "\\\\:").replaceAll("=", "\\\\="));
                sep = ":";
            }
        }

        return options.toString();
    }
}
