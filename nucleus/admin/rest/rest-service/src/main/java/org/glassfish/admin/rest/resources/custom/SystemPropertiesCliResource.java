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
package org.glassfish.admin.rest.resources.custom;

import com.sun.enterprise.config.serverbeans.Cluster;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Server;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.PathSegment;
import javax.ws.rs.core.Response;
import org.glassfish.admin.rest.utils.ResourceUtil;
import org.glassfish.admin.rest.resources.TemplateExecCommand;
import org.glassfish.admin.rest.results.ActionReportResult;
import org.glassfish.admin.rest.results.OptionsResult;
import org.glassfish.admin.rest.utils.xml.RestActionReporter;
import org.glassfish.api.ActionReport;
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
@Produces({"text/html", MediaType.APPLICATION_JSON+";qs=0.5", MediaType.APPLICATION_XML+";qs=0.5", MediaType.APPLICATION_FORM_URLENCODED+";qs=0.5"})
@Consumes({MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML, MediaType.APPLICATION_FORM_URLENCODED})
@CommandLock(CommandLock.LockType.NONE)
@ExecuteOn({RuntimeType.DAS})
@TargetType(value = {CommandTarget.CLUSTER, CommandTarget.CLUSTERED_INSTANCE, CommandTarget.CONFIG, CommandTarget.DAS,
    CommandTarget.DOMAIN, CommandTarget.STANDALONE_INSTANCE})
public class SystemPropertiesCliResource extends TemplateExecCommand {
    protected static final String TAG_SYSTEM_PROPERTY = "system-property";

    @Context
    protected ServiceLocator injector;

    protected Dom entity;
//    protected Dom parent;

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

        actionReport.getExtraProperties().put("systemProperties", new ArrayList(properties.values()));
        if (properties.isEmpty()) {
            actionReport.getTopMessagePart().setMessage("Nothing to list."); // i18n
        }
        ActionReportResult results = new ActionReportResult(commandName, actionReport, new OptionsResult());

        return results;
    }

    @POST
    public Response create(HashMap<String, String> data) {
	Response resp = deleteRemovedProperties(data);
        return (resp == null) ? saveProperties(data) : resp;
    }

    @PUT
    public Response update(HashMap<String, String> data) {
        return saveProperties(data);
    }

    @Path("{Name}/")
    @POST
    public Response getSystemPropertyResource(@PathParam("Name") String id, HashMap<String, String> data) {
        data.put(id, data.get("value"));
        data.remove("value");
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
        List<Dom> sysprops = dom.nodeElements("system-property");
        if ((sysprops != null) && (!sysprops.isEmpty())) {
            for (Dom sysprop : sysprops) {
                String name = sysprop.attribute("name");
                Map<String, String> currValue = properties.get(name);
                if (currValue == null) {
                    currValue = new HashMap<String, String>();
                    currValue.put("name", name);
                    currValue.put(getDefaults ? "defaultValue" : "value", sysprop.attribute("value"));

                    if (sysprop.attribute("description") != null) {
                        currValue.put("description", sysprop.attribute("description"));
                    }
                    properties.put(name, currValue);
                } else {
                    // Only add a default value if there isn't one already
                    if (currValue.get("defaultValue") == null) {
                        currValue.put("defaultValue", sysprop.attribute("value"));
                    }
                }
            }
        }

        // Figure out how to recurse
        if (dom.getProxyType().equals(Server.class)) {
//            Server server = (Server) spb;
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
        List<Dom> configs = domain.nodeElements("clusters").get(0).nodeElements("cluster");
        for(Dom config : configs) {
            if (config.attribute("name").equals(clusterName)) {
                return config;
            }
        }
        return null;
    }

    protected Dom getConfig(Dom domain, String configName) {
        List<Dom> configs = domain.nodeElements("configs").get(0).nodeElements("config");
        for(Dom config : configs) {
            if (config.attribute("name").equals(configName)) {
                return config;
            }
        }
        return null;
    }

    protected String convertPropertyMapToString(HashMap<String, String> data) {
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

    protected Response saveProperties(HashMap<String, String> data) {
        return saveProperties(null, data);
    }

    protected Response saveProperties(String parent, HashMap<String, String> data) {
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

    //returns null if successful or the Response which contains the error msg.
    protected Response deleteRemovedProperties(Map<String,String> newProps) {
        List<String> existingList = new ArrayList();
        Dom parent = getEntity();
        for (Dom existingProp : parent.nodeElements(TAG_SYSTEM_PROPERTY)) {
            existingList.add(existingProp.attribute("name"));
        }
        //no existing properites,return null
        if (existingList.isEmpty()){
            return null;
        }

        //delete the props thats no longer in the new list.
        for(String onePropName : existingList){
            if (!newProps.containsKey(onePropName)){
                Response resp = deleteProperty(null, onePropName);
                if (resp.getStatus() != HttpURLConnection.HTTP_OK){
                    return resp;
                }
            }
        }
        return null;
    }
}
