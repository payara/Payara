/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.security.services.commands;

import java.beans.PropertyVetoException;

import javax.inject.Inject;

import org.glassfish.security.services.config.SecurityConfiguration;

import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;

import org.glassfish.security.services.config.SecurityConfigurations;
import org.glassfish.security.services.config.SecurityProvider;

import com.sun.enterprise.config.serverbeans.Domain;
import org.glassfish.api.admin.AccessRequired;
import org.glassfish.api.admin.AdminCommandSecurity;

/**
 * General create security provider command.
 */
@Service(name="_create-security-provider")
@PerLookup
@ExecuteOn(RuntimeType.DAS)
@TargetType(CommandTarget.DAS)
public class CreateSecurityProvider implements AdminCommand, AdminCommandSecurity.Preauthorization {

    @Param(optional = false)
    private String serviceName;

    @Param(optional = false)
    private String providerName;

    @Param(optional = false)
    private String providerType;

    @Param(primary = true)
    private String name;

    @Inject
    private Domain domain;

    @AccessRequired.NewChild(type=SecurityProvider.class)
    private SecurityConfiguration securityServiceConfiguration;
    
    @Override
    public boolean preAuthorization(AdminCommandContext context) {
        securityServiceConfiguration = CLIUtil.findSecurityConfiguration(domain,
                serviceName, context.getActionReport());
        return (securityServiceConfiguration != null);
    }

	/**
	 * Execute the create-security-provider admin command.
	 */
	@Override
	public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();

                // Add security provider configuration to the service
        // TODO - Add validation logic required for security provider attributes
        try {
            ConfigSupport.apply(new SingleConfigCode<SecurityConfiguration>() {
                @Override
                public Object run(SecurityConfiguration param) throws PropertyVetoException, TransactionFailure {
                	SecurityProvider providerConfig = param.createChild(SecurityProvider.class);
                	providerConfig.setName(name);
                	providerConfig.setType(providerType);
                	providerConfig.setProviderName(providerName);
                    param.getSecurityProviders().add(providerConfig);
                    return providerConfig;
                }
            }, securityServiceConfiguration);
        } catch (TransactionFailure transactionFailure) {
            report.setMessage("Unable to create security provider: " + transactionFailure.getMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(transactionFailure);
        }
	}
        
    
}
