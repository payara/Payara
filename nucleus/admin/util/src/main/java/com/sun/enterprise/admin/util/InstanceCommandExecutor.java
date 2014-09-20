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

package com.sun.enterprise.admin.util;

import com.sun.enterprise.admin.remote.ServerRemoteAdminCommand;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.StringUtils;
import java.util.Map;
import java.util.logging.Logger;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.*;
import org.glassfish.hk2.api.ServiceLocator;

/**
 *
 * Causes execution of an administrative command on one or more remote instances to be
 * triggered from code running inside the DAS.
 * 
 * @author Vijay Ramachandran
 */
public class InstanceCommandExecutor extends ServerRemoteAdminCommand implements Runnable, InstanceCommand {

    private Server server;
    private ParameterMap params;
    private ActionReport aReport;
    private String commandName;
    private FailurePolicy offlinePolicy;
    private FailurePolicy failPolicy;
    private InstanceCommandResult result;

    private static final LocalStringManagerImpl strings =
                        new LocalStringManagerImpl(InstanceCommandExecutor.class);

    public InstanceCommandExecutor(ServiceLocator habitat,
                                   String name, FailurePolicy fail, FailurePolicy offline, Server server,
                                   String host, int port, Logger logger,
                                   ParameterMap p, ActionReport r, InstanceCommandResult res) throws CommandException {
        super(habitat, name, host, port, false, "admin", "", logger);
        this.server = server;
        this.params = p;
        this.aReport = r;
        this.commandName = name;
        this.offlinePolicy = offline;
        this.failPolicy = fail;
        this.result = res;
    }

    @Override
    public String getCommandOutput() { return this.output; }

    public Server getServer() { return server; }

    public ActionReport getReport() { return this.aReport; }

    @Override
    public void run() {
        try {
            executeCommand(params);
            aReport.setActionExitCode(ActionReport.ExitCode.SUCCESS);
            if(StringUtils.ok(getCommandOutput()))
                aReport.setMessage(strings.getLocalString("ice.successmessage", 
                        "{0}:\n{1}\n", getServer().getName(), getCommandOutput()));
            Map<String, String> attributes = this.getAttributes();
            for(Map.Entry<String, String> ae : attributes.entrySet()) {
                String key = ae.getKey();
                if(key.endsWith("_value"))
                    continue;
                if(!key.endsWith("_name")) {
                    if (ae.getValue() != null) {
                        aReport.getTopMessagePart().addProperty(key, ae.getValue());
                    }
                    continue;
                }
                String keyWithoutSuffix = key.substring(0, key.lastIndexOf("_name"));
                aReport.getTopMessagePart().addProperty(keyWithoutSuffix, attributes.get(keyWithoutSuffix+"_value"));
            }
            /*
            else
                aReport.setMessage(strings.getLocalString("glassfish.clusterexecutor.commandSuccessful",
                    "Command {0} executed successfully on server instance {1}", commandName, getServer().getName()));
                    */
        } catch (CommandException cmdEx) {
            ActionReport.ExitCode finalResult;
            if(cmdEx.getCause() instanceof java.net.ConnectException) {
                finalResult = FailurePolicy.applyFailurePolicy(offlinePolicy, ActionReport.ExitCode.FAILURE);
                if(!finalResult.equals(ActionReport.ExitCode.FAILURE))
                    aReport.setMessage(strings.getLocalString("clusterutil.warnoffline",
                        "WARNING: Instance {0} seems to be offline; command {1} was not replicated to that instance",
                            getServer().getName(), commandName));
                else
                    aReport.setMessage(strings.getLocalString("clusterutil.failoffline",
                            "FAILURE: Instance {0} seems to be offline; command {1} was not replicated to that instance",
                            getServer().getName(), commandName));
            } else {
                finalResult = FailurePolicy.applyFailurePolicy(failPolicy, ActionReport.ExitCode.FAILURE);
                if(finalResult.equals(ActionReport.ExitCode.FAILURE))
                    aReport.setMessage(strings.getLocalString("clusterutil.commandFailed",
                        "FAILURE: Command {0} failed on server instance {1}: {2}", commandName, getServer().getName(),
                            cmdEx.getMessage()));
                else
                    aReport.setMessage(strings.getLocalString("clusterutil.commandWarning",
                        "WARNING: Command {0} did not complete successfully on server instance {1}: {2}",
                            commandName, getServer().getName(), cmdEx.getMessage()));
            }
            aReport.setActionExitCode(finalResult);
        }
        result.setInstanceCommand(this);
    }
}
