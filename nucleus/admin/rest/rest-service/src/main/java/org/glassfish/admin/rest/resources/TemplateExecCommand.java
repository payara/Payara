/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2011 Oracle and/or its affiliates. All rights reserved.
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
import java.net.HttpURLConnection;
import org.glassfish.admin.rest.ResourceUtil;
import org.glassfish.admin.rest.RestService;
import org.glassfish.admin.rest.provider.MethodMetaData;
import org.glassfish.admin.rest.results.ActionReportResult;
import org.glassfish.admin.rest.results.OptionsResult;
import org.glassfish.admin.rest.utils.xml.RestActionReporter;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.ParameterMap;

import javax.ws.rs.OPTIONS;
import javax.ws.rs.Produces;
import javax.ws.rs.core.*;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Logger;
import org.glassfish.admin.rest.Constants;
import org.glassfish.admin.rest.Util;
import org.jvnet.hk2.component.Habitat;

/**
 * @author ludo
 */
public class TemplateExecCommand {
    public final static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(TemplateExecCommand.class);
    @Context
    protected HttpHeaders requestHeaders;
    @Context
    protected UriInfo uriInfo;

    @Context
    protected Habitat habitat;

    protected String resourceName;
    protected String commandName;
    protected String commandDisplayName;
    protected String commandMethod;
    protected String commandAction;
//    protected HashMap<String, String> commandParams = null;
    protected boolean isLinkedToParent = false;
    protected Logger logger = Logger.getLogger(TemplateExecCommand.class.getName());


    public TemplateExecCommand(String resourceName, String commandName, String commandMethod, String commandAction, String commandDisplayName,
                               boolean isLinkedToParent) {
        this.resourceName = resourceName;
        this.commandName = commandName;
        this.commandMethod = commandMethod;
        this.commandAction = commandAction;
        this.commandDisplayName = commandDisplayName;
        this.isLinkedToParent = isLinkedToParent;

    }

    @OPTIONS
    @Produces({
            MediaType.APPLICATION_JSON,
            "text/html;qs=2",
            MediaType.APPLICATION_XML})
    public ActionReportResult options() {
        RestActionReporter ar = new RestActionReporter();
        ar.setExtraProperties(new Properties());
        ar.setActionDescription(commandDisplayName);

        OptionsResult optionsResult = new OptionsResult(resourceName);
        Map<String, MethodMetaData> mmd = new HashMap<String, MethodMetaData>();
        MethodMetaData methodMetaData = ResourceUtil.getMethodMetaData(commandName, getCommandParams(),  habitat, RestService.logger);

        optionsResult.putMethodMetaData(commandMethod, methodMetaData);
        mmd.put(commandMethod, methodMetaData);
        ResourceUtil.addMethodMetaData(ar, mmd);

        ActionReportResult ret=  new ActionReportResult(ar, null, optionsResult);
        ret.setCommandDisplayName(commandDisplayName);
        return ret;
    }

    protected Response executeCommand(ParameterMap data) {
        RestActionReporter actionReport = ResourceUtil.runCommand(commandName, data, habitat,
                ResourceUtil.getResultType(requestHeaders));
        ActionReport.ExitCode exitCode = actionReport.getActionExitCode();
        ActionReportResult option = options();
        ActionReportResult results = new ActionReportResult(commandName, actionReport, option.getMetaData());
        results.getActionReport().getExtraProperties().putAll(option.getActionReport().getExtraProperties());
        results.setCommandDisplayName(commandDisplayName);
        int status = HttpURLConnection.HTTP_OK; /*200 - ok*/
        if (exitCode == ActionReport.ExitCode.FAILURE) {
            status = HttpURLConnection.HTTP_INTERNAL_ERROR;
        }
        return Response.status(status).entity(results).build();

    }

    /*override it
     *
     *
     */

    protected HashMap<String, String> getCommandParams() {
        return null;
    }

    protected void processCommandParams(ParameterMap data) {
        HashMap<String, String> commandParams = getCommandParams();
        if (commandParams != null) {
            ResourceUtil.resolveParamValues(commandParams, uriInfo);
            for (Map.Entry<String, String> entry : commandParams.entrySet()) {
                data.add(entry.getKey(), entry.getValue());
            }
        }
    }

    protected void addQueryString(MultivaluedMap<String, String> qs, ParameterMap data) {
        for (Map.Entry<String, List<String>> entry : qs.entrySet()) {
            String key = entry.getKey();
            for (String value : entry.getValue()) {
                data.add(key, value);
            }
        }
    }

    protected void adjustParameters(ParameterMap data) {
        if (data != null) {
            if (!(data.containsKey("DEFAULT"))) {
                boolean isRenamed = renameParameter(data, "name", "DEFAULT");
                if (!isRenamed) {
                    renameParameter(data, "id", "DEFAULT");
                }
            }
            data.remove("jsoncallback"); //these 2 are for JSONP padding, not needed for CLI execs
            data.remove("_");
        }
    }

    protected boolean renameParameter(ParameterMap data, String parameterToRename, String newName) {

        if ((data.containsKey(parameterToRename))) {
            List<String> value = data.get(parameterToRename);
            data.remove(parameterToRename);
            data.set(newName, value);
            return true;
        }
        return false;
    }

    protected void purgeEmptyEntries(ParameterMap data) {

        HashSet<String> keyToRemove = new HashSet<String>();
        Set<Entry<String, List<String>>> entries = data.entrySet();
        for (Entry<String, List<String>> entry : entries) {
            if ((entry.getValue() == null) || (entry.getValue().isEmpty())) {
                keyToRemove.add(entry.getKey());

            }
        }
        if ("true".equals(data.getOne("__remove_empty_entries__"))) {
            data.remove("__remove_empty_entries__");
            //now remove list of 1 element which is "" only
            Set<Entry<String, List<String>>> entries2 = data.entrySet();
            //temp list to avoid Concurrent Modification Exception
            for (Entry<String, List<String>> entry : entries2) {
                if (entry.getValue().size() == 1) {
                    if (entry.getValue().get(0).equals("")) {
                        keyToRemove.add(entry.getKey());
                    }
                }
            }
        }
        for (String k : keyToRemove) {
            data.remove(k);

        }
    }

    protected String getParent(UriInfo uriInfo) {
        List<PathSegment> segments = uriInfo.getPathSegments(true);
        String parent = segments.get(segments.size()-2).getPath();

        return parent;
    }
}
