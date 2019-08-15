/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.v3.admin;

import com.sun.enterprise.util.LocalStringManagerImpl;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.data.EngineInfo;
import org.glassfish.internal.data.ContainerRegistry;
import com.sun.enterprise.module.Module;
import com.sun.enterprise.module.ModulesRegistry;
import com.sun.enterprise.config.serverbeans.Application;
import com.sun.enterprise.config.serverbeans.Applications;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Engine;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.admin.*;
import org.glassfish.api.container.Sniffer;
import javax.inject.Inject;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Singleton;

/**
 * This admin command list the containers currentely running within that
 * Glassfish instance
 */
@Service(name="list-containers")
@Singleton        // no per-execution state
@CommandLock(CommandLock.LockType.NONE)
@I18n("list.containers.command")
@RestEndpoints({
    @RestEndpoint(configBean=Domain.class,
        opType=RestEndpoint.OpType.GET, 
        path="list-containers", 
        description="list-containers")
})
@AccessRequired(resource="domain", action="read")
public class ListContainersCommand implements AdminCommand {

    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(ListContainersCommand.class);

    @Inject
    ContainerRegistry containerRegistry;

    @Inject
    ModulesRegistry modulesRegistry;

    @Inject
    ServiceLocator habitat;

    @Inject
    Applications applications;

    public void execute(AdminCommandContext context) {

        ActionReport report = context.getActionReport();
        report.setActionDescription(localStrings.getLocalString("list.containers.command", "List of Containers"));
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);

        ActionReport.MessagePart top = report.getTopMessagePart();
        top.setMessage(localStrings.getLocalString("list.containers.command", "List of Containers"));
        top.setChildrenType(localStrings.getLocalString("container", "Container"));

        Iterable<? extends Sniffer> sniffers = habitat.getAllServices(Sniffer.class);
        if (sniffers ==null) {
            top.setMessage(localStrings.getLocalString("list.containers.nocontainer",
                    "No container currently configured"));
        } else {
            for (Sniffer sniffer : sniffers) {
                ActionReport.MessagePart container = top.addChild();
                container.setMessage(sniffer.getModuleType());
                container.addProperty(localStrings.getLocalString("contractprovider", "ContractProvider"),
                        sniffer.getModuleType());
                EngineInfo engineInfo = containerRegistry.getContainer(sniffer.getModuleType());

                if (engineInfo != null) {
                    container.addProperty(
                            localStrings.getLocalString("status", "Status"),
                            localStrings.getLocalString("started", "Started"));
                    Module connectorModule = modulesRegistry.find(engineInfo.getSniffer().getClass());
                    container.addProperty(localStrings.getLocalString("connector", "Connector"),
                            connectorModule.getModuleDefinition().getName() +
                            ":" + connectorModule.getModuleDefinition().getVersion());
                    container.addProperty(localStrings.getLocalString("implementation", "Implementation"),
                            engineInfo.getContainer().getClass().toString());
                    boolean atLeastOne = false;
                    for (Application app : applications.getApplications()) {
                        for (com.sun.enterprise.config.serverbeans.Module module : app.getModule()) {
                            Engine engine = module.getEngine(engineInfo.getSniffer().getModuleType());
                            if (engine!=null) {
                                if (!atLeastOne) {
                                    atLeastOne=true;
                                    container.setChildrenType(localStrings.getLocalString("list.containers.listapps",
                                            "Applications deployed"));

                                }
                                container.addChild().setMessage(app.getName());
                            }
                        }

                        
                    }
                    if (!atLeastOne) {
                       container.addProperty("Status", "Not Started");
                    }
                }
            }
        }
    }
}
