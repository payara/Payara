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
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2018-2021] Payara Foundation and/or affiliates

package com.sun.enterprise.v3.admin.cluster;

import static org.glassfish.api.ActionReport.ExitCode.FAILURE;
import static org.glassfish.api.ActionReport.ExitCode.SUCCESS;

import java.beans.PropertyVetoException;
import java.util.List;

import jakarta.inject.Inject;

import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RestParam;
import org.glassfish.api.admin.config.ReferenceContainer;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Configs;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.LocalStringManagerImpl;

/**
 *  This is a remote command that deletes a destination config.
 * Usage: delete-config
 configuration_name

 * @author Bhakti Mehta
 */
@Service(name = "delete-config")
@PerLookup
@I18n("delete.config.command")
@RestEndpoints({
    @RestEndpoint(configBean = Config.class,
        opType = RestEndpoint.OpType.POST, // TODO: Should be DELETE
        path = "delete-config", 
        description = "Delete Config",
        params = 
            @RestParam(name = "id", value = "$parent")
        )
})
public final class DeleteConfigCommand implements AdminCommand {

    private final static LocalStringManagerImpl LOCAL_STRINGS = new LocalStringManagerImpl(DeleteConfigCommand.class);
    
    @Param(primary = true)
    private String destConfig;

    @Inject
    private Configs configs;

    @Inject
    private Domain domain;

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        report.setActionExitCode(SUCCESS);

        // Do not delete default-config
        if (destConfig.equals("default-config")) {
            report.setMessage(
                LOCAL_STRINGS.getLocalString("Config.defaultConfig", "The default configuration template " + "named default-config cannot be deleted."));
            report.setActionExitCode(FAILURE);
            
            return;
        }

        // Get the config from the domain. Does the config exist?
        // if not return
        Config config = domain.getConfigNamed(destConfig);
        if (config == null) {
            report.setMessage(LOCAL_STRINGS.getLocalString("Config.noSuchConfig", "Config {0} does not exist.", destConfig));
            report.setActionExitCode(FAILURE);
            
            return;
        }

        // Check if the config in use by some other
        // ReferenceContainer -- if so just return...
        List<ReferenceContainer> referenceContainers = domain.getReferenceContainersOf(config);
        if (!referenceContainers.isEmpty()) {
            StringBuilder namesOfContainers = new StringBuilder();
            for (ReferenceContainer referenceContainer : referenceContainers) {
                namesOfContainers.append(referenceContainer.getReference()).append(',');
            }
            report.setMessage(LOCAL_STRINGS.getLocalString("Config.inUseConfig",
                    "Config {0} is in use " + "and must be referenced by no server instances or clusters", destConfig, namesOfContainers));
            report.setActionExitCode(FAILURE);
            
            return;
        }
        
        try {
            ConfigSupport.apply(new SingleConfigCode<Configs>() {
                @Override
                public Object run(Configs configs) throws PropertyVetoException, TransactionFailure {
                    configs.getConfig().remove(config);
                    return null;
                }
            }, configs);

        } catch (TransactionFailure ex) {
            report.setMessage(
                    LOCAL_STRINGS.getLocalString("Config.deleteConfigFailed", "Unable to remove config {0} ", config) + " " + ex.getLocalizedMessage());
            report.setActionExitCode(FAILURE);
            report.setFailureCause(ex);
        }

    }

}
