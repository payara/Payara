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
// Portions Copyright [2017-2021] [Payara Foundation and/or its affiliates]
package com.sun.enterprise.security.cli;

import static com.sun.enterprise.util.SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME;
import static org.glassfish.api.admin.ServerEnvironment.DEFAULT_INSTANCE_NAME;
import static org.glassfish.config.support.CommandTarget.CLUSTER;
import static org.glassfish.config.support.CommandTarget.CLUSTERED_INSTANCE;
import static org.glassfish.config.support.CommandTarget.CONFIG;
import static org.glassfish.config.support.CommandTarget.DAS;
import static org.glassfish.config.support.CommandTarget.STANDALONE_INSTANCE;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AccessRequired;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.AdminCommandSecurity;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.annotations.Service;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.JaccProvider;
import com.sun.enterprise.config.serverbeans.SecurityService;

/**
 *Usage: list-jacc-providers
 *         [--help] [--user admin_user] [--passwordfile file_name]
 *         [target(Default server)]
 *
 */
@Service(name="list-jacc-providers")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("list.jacc.provider")
@ExecuteOn({RuntimeType.DAS})
@TargetType({ DAS, CLUSTERED_INSTANCE, STANDALONE_INSTANCE, CLUSTER, CONFIG })
@RestEndpoints({
    @RestEndpoint(configBean=SecurityService.class,
        opType=RestEndpoint.OpType.GET, 
        path="list-jacc-providers", 
        description="list-jacc-providers")
})
public class ListJaccProviders implements AdminCommand, AdminCommandSecurity.Preauthorization {

    @Param(name = "target", primary = true, optional = true, defaultValue = DEFAULT_SERVER_INSTANCE_NAME)
    private String target;

    @Inject
    @Named(DEFAULT_INSTANCE_NAME)
    private Config config;

    @Inject
    private Domain domain;

    @AccessRequired.To("read")
    private SecurityService securityService;

    @Override
    public boolean preAuthorization(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        
        config = CLIUtil.chooseConfig(domain, target, report);
        if (config == null) {
            return false;
        }
        
        securityService = config.getSecurityService();
        return true;
    }

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();

        for (JaccProvider jaccProvider : securityService.getJaccProvider()) {
            report.getTopMessagePart()
                  .addChild()
                  .setMessage(jaccProvider.getName());
        }
        
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }

}
