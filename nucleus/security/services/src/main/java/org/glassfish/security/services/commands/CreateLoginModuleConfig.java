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
import java.util.Properties;

import javax.inject.Inject;

import org.glassfish.security.services.config.SecurityConfiguration;

import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;
import org.jvnet.hk2.config.types.Property;

import org.glassfish.api.ActionReport;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;

import org.glassfish.security.services.config.LoginModuleConfig;
import org.glassfish.security.services.config.SecurityConfigurations;
import org.glassfish.security.services.config.SecurityProvider;

import com.sun.enterprise.config.serverbeans.Domain;
import org.glassfish.api.admin.AccessRequired;
import org.glassfish.api.admin.AdminCommandSecurity;

/**
 * General create LoginModule config command.
 */
@Service(name="_create-login-module-config")
@PerLookup
@ExecuteOn(RuntimeType.DAS)
@TargetType(CommandTarget.DAS)
public class CreateLoginModuleConfig implements AdminCommand, AdminCommandSecurity.Preauthorization {

    @Param(optional = false)
    private String serviceName;

    @Param(optional = false)
    private String providerName;

    @Param(optional = false)
    private String moduleClass;

    @Param(optional = false)
    private String controlFlag;

    @Param(optional = true, separator = ':')
    private Properties configuration;

    @Param(primary = true)
    private String name;

    @Inject
    private Domain domain;
    
    @AccessRequired.NewChild(type=LoginModuleConfig.class)
    private SecurityProvider provider;

    @Override
    public boolean preAuthorization(AdminCommandContext context) {
        SecurityProvider provider = CLIUtil.findSecurityProvider(domain, serviceName, providerName, context.getActionReport());
        return (provider != null);
    }

    
	/**
	 * Execute the create-login-module-config admin command.
	 */
	@Override
	public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();

        // Add LoginModule configuration to the security provider setup
        // TODO - Add validation logic of the LoginModule config attributes
        LoginModuleConfig config = null;
        try {
            config = (LoginModuleConfig) ConfigSupport.apply(new SingleConfigCode<SecurityProvider>() {
                @Override
                public Object run(SecurityProvider param) throws PropertyVetoException, TransactionFailure {
                	LoginModuleConfig lmConfig = param.createChild(LoginModuleConfig.class);
                	lmConfig.setName(name);
                	lmConfig.setModuleClass(moduleClass);
                	lmConfig.setControlFlag(controlFlag);
                    // TODO - Should prevent multiple security provider config entries
                	param.getSecurityProviderConfig().add(lmConfig);
                    return lmConfig;
                }
            }, provider);
        } catch (TransactionFailure transactionFailure) {
            report.setMessage("Unable to create login module config: " + transactionFailure.getMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(transactionFailure);
            return;
        }

        // Setup LoginModule configuration options
        if ((config != null) && (configuration != null) && (!configuration.isEmpty())) {
	        try {
	            ConfigSupport.apply(new SingleConfigCode<LoginModuleConfig>() {
	                @Override
	                public Object run(LoginModuleConfig param) throws PropertyVetoException, TransactionFailure {
	                    for (Object configPropName: configuration.keySet()) {
		                	Property prop = param.createChild(Property.class);
		                	String propName = (String) configPropName; 
	                        prop.setName(propName);
	                        prop.setValue(configuration.getProperty(propName));            
	                        param.getProperty().add(prop);    
	                    }
	                    return param;
	                }
	            }, config);
	        } catch (TransactionFailure transactionFailure) {
	            report.setMessage("Unable to create login module options: " + transactionFailure.getMessage());
	            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
	            report.setFailureCause(transactionFailure);
	        }
        }
	}
}
