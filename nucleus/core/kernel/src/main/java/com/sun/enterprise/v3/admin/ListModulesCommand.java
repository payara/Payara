/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2010 Oracle and/or its affiliates. All rights reserved.
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

import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.annotations.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.component.Singleton;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import com.sun.enterprise.module.ModulesRegistry;
import com.sun.enterprise.module.Module;
import com.sun.enterprise.module.ModuleState;

import java.net.URI;

/**
 * List the modules available to this instance and their status
 */
@Service(name="list-modules")
@Scoped(Singleton.class)        // no per-execution state
@CommandLock(CommandLock.LockType.NONE)
@I18n("list.modules.command")
public class ListModulesCommand implements AdminCommand {

    @Inject
    ModulesRegistry modulesRegistry;

    public void execute(AdminCommandContext context) {

        ActionReport report = context.getActionReport();
        report.setActionDescription("List of modules");
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);

        ActionReport.MessagePart top = report.getTopMessagePart();
        top.setMessage("List Of Modules");
        top.setChildrenType("Module");
        for(Module module : modulesRegistry.getModules()) {

            ActionReport.MessagePart childPart = top.addChild();
            String version = module.getModuleDefinition().getVersion();
            if (version==null) {
                version = "any";

            }
            
            childPart.setMessage(module.getModuleDefinition().getName() +
                ":" + version);

            childPart.addProperty("State", module.getState().toString());
            if (module.isSticky()) {
                childPart.addProperty("Sticky", "true");
            }
            if (!module.isShared()) {
                childPart.addProperty("visibility", "private");
            } else {
                childPart.addProperty("visibility", "public");
            }
            
            if (module.getState().equals(ModuleState.READY)) {
                childPart.setChildrenType("Module Characteristics");
                ActionReport.MessagePart provides = childPart.addChild();
                provides.setMessage("Provides to following services");
                provides.setChildrenType("Provides");

                /*for (URL info : module.getServiceProviders().getDescriptors()) {
                    provides.addChild().setMessage(info.toString());
                } */

                ActionReport.MessagePart imports = childPart.addChild();
                imports.setMessage("List of imported modules");
                imports.setChildrenType("Imports");
                for (Module i : module.getImports()) {
                    String importVersion = i.getModuleDefinition().getVersion();
                    if (importVersion==null) {
                        importVersion="any";
                    }
                    imports.addChild().setMessage(i.getModuleDefinition().getName() + ":" + importVersion);
                }

                ActionReport.MessagePart implementations = childPart.addChild();
                implementations.setMessage("List of Jars implementing the module");
                implementations.setChildrenType("Jar");
                for (URI location : module.getModuleDefinition().getLocations()) {
                    implementations.addChild().setMessage(location.toString());
                }
            }
        }
    }
}
