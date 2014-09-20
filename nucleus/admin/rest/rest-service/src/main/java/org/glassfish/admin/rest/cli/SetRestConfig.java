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
package org.glassfish.admin.rest.cli;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import java.beans.PropertyVetoException;
import javax.inject.Inject;
import javax.inject.Named;
import org.glassfish.admin.restconnector.RestConfig;
import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AccessRequired;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

/**
 * Remote asadmin command: set-rest-config
 *
 * Purpose: Allows the invoker to configure the REST module.
 *
 *
 *
 * @author Ludovic Champenois
 *
 */
@Service(name = "_set-rest-admin-config")
@PerLookup
@ExecuteOn(RuntimeType.DAS)
@CommandLock(CommandLock.LockType.NONE)
@RestEndpoints({
    @RestEndpoint(configBean=Domain.class, opType=RestEndpoint.OpType.POST)
})
public class SetRestConfig implements AdminCommand {

    @AccessRequired.To("update")
    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    Config config;
    @Inject
    private ServiceLocator habitat;
    @Param(optional = true)
    private String debug;
    @Param(optional = true, defaultValue = "-100")
    private int indentLevel;
    @Param(optional = true)
    private String wadlGeneration;
    @Param(optional = true)
    private String showHiddenCommands;
    @Param(optional = true)
    private String showDeprecatedItems;
    @Param(optional = true)
    private String logOutput;
    @Param(optional = true)
    private String logInput;
    @Param(optional = true)
    private String sessionTokenTimeout;

    @Override
    public void execute(AdminCommandContext context) {

        ActionReport report = context.getActionReport();

        RestConfig restConfig = config.getExtensionByType(RestConfig.class);

        /**
         * The schedules does not exist in this Config.  We will need to
         * add it plus the default schedules.
         */
        if (restConfig == null) {
            try {
                ConfigSupport.apply(new SingleConfigCode<Config>() {

                    @Override
                    public Object run(Config parent) throws TransactionFailure {
                        RestConfig child = parent.createChild(RestConfig.class);
                        parent.getContainers().add(child);
                        return child;
                    }
                }, config);
            } catch (TransactionFailure e) {
                report.setMessage("TransactionFailure failure while creating the REST config");
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setFailureCause(e);
                return;
            }

            restConfig = config.getExtensionByType(RestConfig.class);
            if (restConfig == null) {
                report.setMessage("Rest Config is NULL...");
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }
        }





        try {
            ConfigSupport.apply(new SingleConfigCode<RestConfig>() {

                @Override
                public Object run(RestConfig param) throws
                        TransactionFailure,
                        PropertyVetoException {
                    if (debug != null) {
                        param.setDebug(debug);
                    }
                    if (indentLevel != -100) {
                        param.setIndentLevel("" + indentLevel);
                    }
                    if (showHiddenCommands != null) {
                        param.setShowHiddenCommands(showHiddenCommands);
                    }
                    if (showDeprecatedItems != null) {
                        param.setShowDeprecatedItems(showDeprecatedItems);
                    }
                    if (wadlGeneration != null) {
                        param.setWadlGeneration(wadlGeneration);
                    }
                    if (logOutput != null) {
                        param.setLogOutput(logOutput);
                    }
                    if (logInput != null) {
                        param.setLogInput(logInput);
                    }
                    if (sessionTokenTimeout != null) {
                        param.setSessionTokenTimeout(sessionTokenTimeout);
                    }



                    return param;
                }
            }, restConfig);
        } catch (TransactionFailure e) {
            report.setMessage("TransactionFailure while changing the REST config");
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
            return;
        }

        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        return;
    }
}
