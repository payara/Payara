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

package com.sun.enterprise.security.cli;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.JaccProvider;
import com.sun.enterprise.config.serverbeans.SecurityService;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;
import java.beans.PropertyVetoException;
import java.util.Properties;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import javax.inject.Inject;
import javax.inject.Named;
import org.glassfish.api.admin.AccessRequired;
import org.glassfish.api.admin.AdminCommandSecurity;


import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

/**
 * Create Jacc Provider Command
 *
 * Usage: create-jacc-provider   --policyconfigfactoryclass pc_factory_class
 *         --policyproviderclass pol_provider_class
 *         [--help] [--user admin_user] [--passwordfile file_name]
 *         [ --property (name=value)[:name=value]*]
 *         [ --target  target_name] jacc_provider_name
 *
 *
 * domain.xml element example
 *   <jacc-provider policy-provider="com.sun.enterprise.security.provider.PolicyWrapper" name="default" policy-configuration-factory-provider="com.sun.enterprise.security.provider.PolicyConfigurationFactoryImpl">
 *         <property name="repository" value="${com.sun.aas.instanceRoot}/generated/policy" />
 *   </jacc-provider>
 *
 */
@Service(name="create-jacc-provider")
@PerLookup
@I18n("create.jacc.provider")
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType({CommandTarget.DAS,CommandTarget.STANDALONE_INSTANCE,CommandTarget.CLUSTER, CommandTarget.CONFIG})
public class CreateJACCProvider implements AdminCommand, AdminCommandSecurity.Preauthorization {

    final private static LocalStringManagerImpl localStrings =
        new LocalStringManagerImpl(CreateJACCProvider.class);

    @Param(name="policyconfigfactoryclass", alias="policyConfigurationFactoryProvider")
    private String polConfFactoryClass;

    @Param(name="policyproviderclass", alias="policyProvider")
    private String polProviderClass;

    @Param(name="jaccprovidername", primary=true)
    private String jaccProviderName;

    @Param(optional=true, name="property", separator=':')
    private Properties properties;

    @Param(name = "target", optional = true,  defaultValue =
    SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME)
    private String target;

    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    private Config config;

    @Inject
    private Domain domain;

    @AccessRequired.NewChild(type=JaccProvider.class)
    private SecurityService securityService;
    

    @Override
    public boolean preAuthorization(AdminCommandContext context) {
        config = CLIUtil.chooseConfig(domain, target, context.getActionReport());
        if (config == null) {
            return false;
        }
        securityService = config.getSecurityService();
        JaccProvider jaccProvider = CLIUtil.findJaccProvider(securityService, jaccProviderName);
        if (jaccProvider != null) {
            final ActionReport report = context.getActionReport();
            report.setMessage(localStrings.getLocalString(
                    "create.jacc.provider.duplicatefound",
                    "JaccProvider named {0} exists. Cannot add duplicate JaccProvider.",
                    jaccProviderName));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return false;
        }
        return true;
    }
    
    @Override
    public void execute(AdminCommandContext context) {
       final ActionReport report = context.getActionReport();

        // No duplicate auth realms found. So add one.
        try {
            ConfigSupport.apply(new SingleConfigCode<SecurityService>() {

                public Object run(SecurityService param)
                        throws PropertyVetoException, TransactionFailure {
                    JaccProvider newJacc = param.createChild(JaccProvider.class);
                    newJacc.setName(jaccProviderName);
                    newJacc.setPolicyConfigurationFactoryProvider(polConfFactoryClass);
                    newJacc.setPolicyProvider(polProviderClass);
                    param.getJaccProvider().add(newJacc);
                    return newJacc;
                }
            }, securityService);

        } catch(TransactionFailure e) {
            report.setMessage(localStrings.getLocalString("create.auth.realm.fail",
                    "Creation of Authrealm {0} failed", jaccProviderName) +
                              "  " + e.getLocalizedMessage() );
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
            return;
        }
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }
}
