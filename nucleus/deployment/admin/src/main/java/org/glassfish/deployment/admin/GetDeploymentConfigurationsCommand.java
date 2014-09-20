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

package org.glassfish.deployment.admin;

import com.sun.enterprise.config.serverbeans.Application;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.Param;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.container.Sniffer;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.data.ApplicationRegistry;
import org.glassfish.internal.data.ModuleInfo;
import org.glassfish.internal.data.EngineRef;
import org.glassfish.deployment.common.DeploymentProperties;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.api.ActionReport;
import javax.inject.Inject;
import org.jvnet.hk2.annotations.Service;

import org.glassfish.hk2.api.PerLookup;

import java.util.Map;
import java.io.IOException;
import org.glassfish.api.admin.AccessRequired;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RestParam;

/**
 * Get deployment configurations command
 */
@Service(name="_get-deployment-configurations")
@ExecuteOn(value={RuntimeType.DAS})
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@RestEndpoints({
    @RestEndpoint(configBean=Application.class,
        opType=RestEndpoint.OpType.GET, 
        path="_get-deployment-configurations",
        description="Get Deployment Configurations",
        params={@RestParam(name="appname", value="$parent")})
})
@AccessRequired(resource=DeploymentCommandUtils.APPLICATION_RESOURCE_NAME + "/$appname", action="read")
public class GetDeploymentConfigurationsCommand implements AdminCommand {

    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(GetDeploymentConfigurationsCommand.class);

    @Param(primary=true)
    private String appname = null;

    @Inject
    ApplicationRegistry appRegistry;

    /**
     * Entry point from the framework into the command execution
     * @param context context for the command.
     */
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();

        ActionReport.MessagePart part = report.getTopMessagePart();

        final ApplicationInfo appInfo = appRegistry.get(appname);

        if (appInfo == null) {
            return;
        }

        try
        {
            if (appInfo.getEngineRefs().size() > 0)
            {
                // composite archive case, i.e. ear
                for (EngineRef ref : appInfo.getEngineRefs())
                {
                    Sniffer appSniffer = ref.getContainerInfo().getSniffer();
                    addToResultDDList("", appSniffer.getDeploymentConfigurations(appInfo.getSource()), part);
                }

                for (ModuleInfo moduleInfo : appInfo.getModuleInfos())
                {
                    for (Sniffer moduleSniffer : moduleInfo.getSniffers())
                    {
                        ReadableArchive moduleArchive = appInfo.getSource().getSubArchive(moduleInfo.getName());
                        addToResultDDList(moduleInfo.getName(), moduleSniffer.getDeploymentConfigurations(moduleArchive), part);
                    }
                }
            }
            else
            {
                // standalone module
                for (Sniffer sniffer : appInfo.getSniffers())
                {
                    addToResultDDList(appname, sniffer.getDeploymentConfigurations(appInfo.getSource()), part);
                }
            }
        }
        catch ( final IOException e)
        {
            throw new RuntimeException(e);
        }
    }

    private void addToResultDDList(String moduleName, Map<String, String> snifferConfigs, ActionReport.MessagePart part)
    {
        for (Map.Entry<String, String> pathEntry : snifferConfigs.entrySet()) {
            ActionReport.MessagePart childPart = part.addChild();
            childPart.addProperty(DeploymentProperties.MODULE_NAME, 
                moduleName);
            childPart.addProperty(DeploymentProperties.DD_PATH, 
                pathEntry.getKey());
            childPart.addProperty(DeploymentProperties.DD_CONTENT, 
                pathEntry.getValue());
        }
    }
}
