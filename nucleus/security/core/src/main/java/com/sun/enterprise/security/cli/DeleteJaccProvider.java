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
import java.util.List;
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
 *Usage: delete-jacc-provider
 *         [--help] [--user admin_user] [--passwordfile file_name]
 *         [ --target  target_name] jacc_provider_name
 * 
 */
@Service(name="delete-jacc-provider")
@PerLookup
@I18n("delete.jacc.provider")
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType({CommandTarget.DAS,CommandTarget.STANDALONE_INSTANCE,CommandTarget.CLUSTER, CommandTarget.CONFIG})
public class DeleteJaccProvider implements AdminCommand, AdminCommandSecurity.Preauthorization {

    final private static LocalStringManagerImpl localStrings =
        new LocalStringManagerImpl(DeleteJaccProvider.class);

    @Param(name="jaccprovidername", primary=true)
    private String jaccprovider;

    @Param(name = "target", optional = true, defaultValue =
    SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME)
    private String target;

    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    private Config config;

    @Inject
    private Domain domain;

    private SecurityService securityService;
    
    @AccessRequired.To("delete")
    private JaccProvider jprov;
    
    @Override
    public boolean preAuthorization(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();
        config = CLIUtil.chooseConfig(domain, target, report);
        if (config == null) {
            return false;
        }
        securityService = config.getSecurityService();
        jprov = CLIUtil.findJaccProvider(securityService, jaccprovider);
        if (jprov == null) {
            report.setMessage(localStrings.getLocalString(
                    "delete.jacc.provider.notfound",
                    "JaccProvider named {0} not found", jaccprovider));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return false;
        }
        if ("default".equals(jprov.getName())
                    || "simple".equals(jprov.getName())) {
            report.setMessage(localStrings.getLocalString(
                   "delete.jacc.provider.notallowed",
                   "JaccProvider named {0} is a system provider and cannot be deleted", jaccprovider));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return false;
         }
        
        return true;
    }
    
    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();
        try {
            List<JaccProvider> jaccProviders = securityService.getJaccProvider();
            JaccProvider jprov = null;
            for (JaccProvider jaccProv : jaccProviders) {
               if (jaccProv.getName().equals(jaccprovider)) {
                   jprov = jaccProv;
                   break;
               }
            }
            
            final JaccProvider jaccprov = jprov;
            ConfigSupport.apply(new SingleConfigCode<SecurityService>() {
                public Object run(SecurityService param)
                throws PropertyVetoException, TransactionFailure {
                    param.getJaccProvider().remove(jaccprov);
                    return null;
                }
            }, securityService);
        } catch(TransactionFailure e) {
            report.setMessage(localStrings.getLocalString(
                "delete.jacc.provider.fail", "Deletion of JaccProvider {0} failed",
                jaccprovider) + "  " + e.getLocalizedMessage());
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
            return;
        }
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);

    }

}
