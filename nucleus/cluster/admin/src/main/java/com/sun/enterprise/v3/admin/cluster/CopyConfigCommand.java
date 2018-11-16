/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2018] Payara Foundation and/or affiliates

package com.sun.enterprise.v3.admin.cluster;


import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.admin.AdminCommandContext;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import java.beans.PropertyVetoException;
import java.util.logging.Logger;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoint.OpType;
import org.glassfish.api.admin.RestEndpoints;

/**
 *  This is a remote command that copies a config to a destination config.
 * Usage: copy-config
 	[--systemproperties  (name=value)[:name=value]*]
	source_configuration_name destination_configuration_name
 * @author Bhakti Mehta
 */
@Service(name = "copy-config")
@I18n("copy.config.command")
@PerLookup
//        {"Configs", "copy-config", "POST", "copy-config", "Copy Config"},
@RestEndpoints({
    @RestEndpoint(configBean=Configs.class, opType=OpType.POST, path="copy-config", description="Copy Config")
})
public final class CopyConfigCommand extends CopyConfig {

    private static final LocalStringManagerImpl localStrings = new LocalStringManagerImpl(CopyConfigCommand.class);

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);

        if (configs.size() != 2) {
            report.setMessage(localStrings.getLocalString("Config.badConfigNames",
                    "You must specify a source and destination config."));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        final String srcConfig = configs.get(0);
        final String destConfig = configs.get(1);
        //Get the config from the domain
        //does the src config exist
        final Config config = domain.getConfigNamed(srcConfig);
        if (config == null ){
            report.setMessage(localStrings.getLocalString(
                    "Config.noSuchConfig", "Config {0} does not exist.", srcConfig));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        //does dest config exist
        final Config destinationConfig = domain.getConfigNamed(destConfig);
        if (destinationConfig != null ){
            report.setMessage(localStrings.getLocalString(
                    "Config.configExists", "Config {0} already exists.", destConfig));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        //Copy the config
        final String configName = destConfig ;
        final Logger logger = context.getLogger();
        try {
            ConfigSupport.apply(new SingleConfigCode<Configs>(){
                @Override
                public Object run(Configs configs ) throws PropertyVetoException, TransactionFailure {
                    return copyConfig(configs,config,configName,logger);
                }
            }   ,domain.getConfigs());


        } catch (TransactionFailure e) {
            report.setMessage(
                localStrings.getLocalString(
                    "Config.copyConfigError",
                    "CopyConfig error caused by " ,
                 e.getLocalizedMessage()));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
        }
    }

}
