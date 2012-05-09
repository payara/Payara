/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
import java.util.Properties;

import javax.inject.Inject;
import org.jvnet.hk2.annotations.Scoped;
import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.PerLookup;
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

import org.glassfish.security.services.config.AuthenticationService;
import org.glassfish.security.services.config.SecurityConfigurations;

import com.sun.enterprise.config.serverbeans.Domain;

/**
 * General create security service command.
 */
@Service(name="create-security-service")
@Scoped(PerLookup.class)
@ExecuteOn(RuntimeType.DAS)
@TargetType(CommandTarget.DAS)
public class CreateSecurityService implements AdminCommand {
    @Param(optional = false)
    private String serviceType;

    @Param(defaultValue = "false", optional = true)
    private Boolean enableDefault;

    @Param(optional = true, separator = ':')
    private Properties configuration;

    @Param(primary = true)
    private String serviceName;

    @Inject
    private Domain domain;

	@Override
	public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();

        // Lookup or Create the security configurations
        SecurityConfigurations secConfigs = domain.getExtensionByType(SecurityConfigurations.class);
        if (secConfigs==null) {
            try {
            	secConfigs = (SecurityConfigurations) ConfigSupport.apply(new SingleConfigCode<Domain>() {
                    @Override
                    public Object run(Domain wDomain) throws PropertyVetoException, TransactionFailure {
                    	SecurityConfigurations s = wDomain.createChild(SecurityConfigurations.class);
                        wDomain.getExtensions().add(s);
                        return s;
                    }
                }, domain);
            } catch (TransactionFailure transactionFailure)  {
                report.setMessage("Unable to create security configurations: " + transactionFailure.getMessage());
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setFailureCause(transactionFailure);
                return;
            }
        }

        // TODO - Validate all arguments
        if (serviceType.isEmpty()) {
            report.setMessage("Security service type not specified");
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        // Add service configuration
        // TODO - Address different service types
        try {
            ConfigSupport.apply(new SingleConfigCode<SecurityConfigurations>() {
                public Object run(SecurityConfigurations param) throws PropertyVetoException, TransactionFailure {
                    AuthenticationService svcConfig = param.createChild(AuthenticationService.class);
                    svcConfig.setName(serviceName);
                    svcConfig.setDefault(enableDefault);
                    if (configuration != null) {
                    	String realmName = configuration.getProperty("auth-realm");
                    	svcConfig.setAuthRealm(realmName);
                    }
                    param.getSecurityServices().add(svcConfig);
                    return svcConfig;
                }
            }, secConfigs);
        } catch (TransactionFailure transactionFailure) {
            report.setMessage("Unable to create security service: " + transactionFailure.getMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(transactionFailure);
        }
	}
}
