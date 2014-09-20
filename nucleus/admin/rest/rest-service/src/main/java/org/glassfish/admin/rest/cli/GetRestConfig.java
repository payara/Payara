/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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
import javax.inject.Inject;
import javax.inject.Named;
import org.glassfish.admin.restconnector.RestConfig;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.*;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.Service;

/**
 * Remote asadmin command: get-rest-config
 *
 * Purpose: Allows the invoker to get values for the REST module.
 *
 *
 *
 * @author Ludovic Champenois
 *
 */
@Service(name = "_get-rest-admin-config")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@RestEndpoints({
    @RestEndpoint(configBean=Domain.class)
})
public class GetRestConfig implements AdminCommand {

    @AccessRequired.To("read")
    @Inject
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    Config config;
    @Inject
    private ServiceLocator habitat;

    @Override
    public void execute(AdminCommandContext context) {

        ActionReport report = context.getActionReport();

        RestConfig restConfig = config.getExtensionByType(RestConfig.class);


        if (restConfig == null) {
            report.setMessage("debug=false, indentLevel=-1, showHiddenCommands=false, wadlGeneration=false, logOutput=false, logInput=false, showDeprecatedItems=false, sessionTokenTimeout=30");

            report.getTopMessagePart().addProperty("debug", "false");
            report.getTopMessagePart().addProperty("indentLevel", "-1");
            report.getTopMessagePart().addProperty("showHiddenCommands", "false");
            report.getTopMessagePart().addProperty("showDeprecatedItems", "false");
            report.getTopMessagePart().addProperty("wadlGeneration", "" + "false");
            report.getTopMessagePart().addProperty("logOutput", "" + "false");
            report.getTopMessagePart().addProperty("logInput", "" + "false");
            report.getTopMessagePart().addProperty("sessionTokenTimeout", "30");

        } else {
            report.setMessage("debug=" + restConfig.getDebug() + ", indentLevel=" + restConfig.getIndentLevel() + ", showHiddenCommands=" + restConfig.getShowHiddenCommands() + ", wadlGeneration=" + restConfig.getWadlGeneration() + ", logOutput=" + restConfig.getLogOutput()
                    + ", logInput=" + restConfig.getLogInput() + ", sessionTokenTimeout=" + restConfig.getSessionTokenTimeout());

            report.getTopMessagePart().addProperty("debug", restConfig.getDebug());
            report.getTopMessagePart().addProperty("indentLevel", restConfig.getIndentLevel());
            report.getTopMessagePart().addProperty("showHiddenCommands", restConfig.getShowHiddenCommands());
            report.getTopMessagePart().addProperty("showDeprecatedItems", restConfig.getShowDeprecatedItems());
            report.getTopMessagePart().addProperty("wadlGeneration", restConfig.getWadlGeneration());
            report.getTopMessagePart().addProperty("logOutput", restConfig.getLogOutput());
            report.getTopMessagePart().addProperty("logInput", restConfig.getLogInput());
            report.getTopMessagePart().addProperty("sessionTokenTimeout", "" + restConfig.getSessionTokenTimeout());
        }



        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);


        return;

    }
}
