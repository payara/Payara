/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.connectors.admin.cli;

import com.sun.enterprise.config.serverbeans.*;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.connectors.config.ConnectorConnectionPool;
import org.glassfish.connectors.config.ResourceAdapterConfig;
import org.glassfish.connectors.config.WorkSecurityMap;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.jdbc.config.JdbcConnectionPool;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service(name="_list-resources")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@RestEndpoints({
    @RestEndpoint(configBean=Domain.class,
        opType=RestEndpoint.OpType.GET, 
        path="_list-resources", 
        description="_list-resources")
})
public class ListResources implements AdminCommand {

    @Inject
    private Applications applications;

    @Param(optional = false, name="appname")
    private String appName;

    @Param(optional = true, name="modulename")
    private String moduleName;

    @Inject
    org.glassfish.resourcebase.resources.util.BindableResourcesHelper bindableResourcesHelper;

    /**
     * Executes the command with the command parameters passed as Properties
     * where the keys are the parameter names and the values the parameter values
     *
     * @param context information
     */
    public void execute(AdminCommandContext context) {
        if(appName != null){
            if(!isValidApplication(appName)){
                ActionReport report = context.getActionReport();
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                ActionReport.MessagePart messagePart = report.getTopMessagePart();
                messagePart.setMessage("Invalid application ["+appName+"]");
                return;
            }
        }
        if(moduleName != null){
            if(!isValidModule(appName, moduleName)){
                ActionReport report = context.getActionReport();
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                ActionReport.MessagePart messagePart = report.getTopMessagePart();
                messagePart.setMessage("Invalid module ["+moduleName+"] in application ["+appName+"]");
                return;
            }
        }
        if(appName != null && moduleName != null){
            Application application = applications.getApplication(appName);
            Module module = application.getModule(moduleName);
            Resources moduleScopedResources = module.getResources();
            if(moduleScopedResources != null){
                ActionReport report = context.getActionReport();
                ActionReport.MessagePart messagePart = report.getTopMessagePart();
                generateResourcesList(messagePart, moduleScopedResources.getResources());
            }
        }else if(appName != null){
            Application application = applications.getApplication(appName);
            Resources appScopedResources = application.getResources();
            if(appScopedResources != null){
                ActionReport report = context.getActionReport();
                ActionReport.MessagePart messagePart = report.getTopMessagePart();
                generateResourcesList(messagePart, appScopedResources.getResources());
            }
        }
    }

    private void generateResourcesList(ActionReport.MessagePart part, List<Resource> resources) {
        Map<String, List<String>> list = new HashMap<String, List<String>>();
        for (Resource r : resources) {
            if (r instanceof BindableResource) {
                String name = ((BindableResource) r).getJndiName();
                String type = "";
                String resourceName = bindableResourcesHelper.getResourceTypeName((BindableResource)(r));
                type = "<" + resourceName + ">";

                List<String> typedResources = getResourcesByType(list, type);
                typedResources.add(name);
            } else if (r instanceof ResourcePool) {
                String name = ((ResourcePool) r).getName();
                String type = "";
                if (r instanceof JdbcConnectionPool) {
                    type = "<JdbcConnectionPool>";
                } else if (r instanceof ConnectorConnectionPool) {
                    type = "<ConnectorConnectionPool>";
                }
                List<String> typedResources = getResourcesByType(list, type);
                typedResources.add(name);

            } else if (r instanceof ResourceAdapterConfig) {
                String name = (((ResourceAdapterConfig) r).getResourceAdapterName());
                String type = "<ResourceAdapterConfig>";
                List<String> typedResources = getResourcesByType(list, type);
                typedResources.add(name);

            } else if (r instanceof WorkSecurityMap) {
                String name = (((WorkSecurityMap) r).getName());
                String type = "<WorkSecurityMap>";
                List<String> typedResources = getResourcesByType(list, type);
                typedResources.add(name);
            }
        }
        for (Map.Entry e : list.entrySet()) {
            String type = (String) e.getKey();
            List<String> values = (List<String>) e.getValue();
            for (String value : values) {
                ActionReport.MessagePart child = part.addChild();
                child.setMessage("  " + value + "\t" + type);
                part.addProperty(value, type);
            }
        }
    }

    private List<String> getResourcesByType(Map<String, List<String>> list, String type) {
        List<String> typedResources = list.get(type);
        if (typedResources == null) {
            typedResources = new ArrayList<String>();
            list.put(type, typedResources);
        }
        return typedResources;
    }

    private boolean isValidApplication(String appName){
        Application app = applications.getApplication(appName);
        return app != null;
    }

    private boolean isValidModule(String appName, String moduleName){
        Application app = applications.getApplication(appName);
        Module module = app.getModule(moduleName);
        return module != null;
    }
}
