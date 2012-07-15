/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.webservices.cli;

import com.sun.enterprise.config.serverbeans.Application;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.webservices.WebServicesContainer;
import org.glassfish.webservices.deployment.DeployedEndpointData;
import org.glassfish.webservices.deployment.WebServicesDeploymentMBean;
import org.jvnet.hk2.annotations.Optional;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;

import java.util.Map;
import java.util.Properties;
import javax.inject.Inject;
import javax.inject.Provider;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RestParam;

/**
 * CLI for listing all web services.
 * <p>
 * asadmin __list-webservices [--appname <appname> [--modulename <modulename> [--
endpointname <endpointname>]]]
 *
 * Will be executed on DAS

 * @author Jitendra Kotamraju
 */
@Service(name = "__list-webservices")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@ExecuteOn(RuntimeType.DAS)
@RestEndpoints({
    @RestEndpoint(configBean=Application.class,
        opType=RestEndpoint.OpType.GET, 
        path="list-webservices", 
        description="list-webservices",
        params={
            @RestParam(name="appName", value="$parent")
        })
})
public class ListWebServicesCommand implements AdminCommand {
    @Inject @Optional
    private Provider<WebServicesContainer> containerProvider;

    @Param(optional=true, alias="applicationname")
    String appName;

    @Param(optional=true)
    String moduleName;

    @Param(optional=true)
    String endpointName;

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        WebServicesContainer container = containerProvider.get();
        if (container == null) {
            return;
        }
        WebServicesDeploymentMBean bean = container.getDeploymentBean();

        if (appName != null && moduleName != null && endpointName != null) {
            Map<String, Map<String, Map<String, DeployedEndpointData>>> endpoints =
                    bean.getEndpoint(appName, moduleName, endpointName);
            fillEndpoints(report, endpoints);
        } else if (appName != null && moduleName != null) {
            Map<String, Map<String, Map<String, DeployedEndpointData>>> endpoints =
                    bean.getEndpoints(appName, moduleName);
            fillEndpoints(report, endpoints);
        } else if (appName != null) {
            Map<String, Map<String, Map<String, DeployedEndpointData>>> endpoints =
                    bean.getEndpoints(appName);
            fillEndpoints(report, endpoints);
        } else {
            Map<String, Map<String, Map<String, DeployedEndpointData>>> endpoints = bean.getEndpoints();
            fillEndpoints(report, endpoints);
        }

    }

    private void fillEndpoints(ActionReport report, Map<String, Map<String, Map<String, DeployedEndpointData>>> endpoints) {
        if (!endpoints.isEmpty()) {
            Properties extra = new Properties();
            extra.putAll(endpoints);
            report.setExtraProperties(extra);
            ActionReport.MessagePart top = report.getTopMessagePart();
            for(Map.Entry<String, Map<String, Map<String, DeployedEndpointData>>> app : endpoints.entrySet()) {
                ActionReport.MessagePart child = top.addChild();
                child.setMessage("application:"+app.getKey());
                for(Map.Entry<String, Map<String, DeployedEndpointData>> module : app.getValue().entrySet()) {
                    child = child.addChild();
                    child.setMessage("  module:"+module.getKey());
                    for(Map.Entry<String, DeployedEndpointData> endpoint : module.getValue().entrySet()) {
                        child = child.addChild();
                        child.setMessage("    endpoint:"+endpoint.getKey());
                        for(Map.Entry<String, String> endpointData : endpoint.getValue().getStaticAsMap().entrySet()) {
                            child = child.addChild();
                            child.setMessage("      "+endpointData.getKey()+":"+endpointData.getValue());
                        }
                    }
                }
            }   
        }
    }

}
