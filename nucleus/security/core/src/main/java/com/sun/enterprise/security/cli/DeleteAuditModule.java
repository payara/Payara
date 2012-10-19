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

import com.sun.enterprise.config.serverbeans.AuditModule;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Configs;
import com.sun.enterprise.config.serverbeans.Domain;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.ActionReport;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import javax.inject.Named;

import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;
import com.sun.enterprise.config.serverbeans.SecurityService;
import com.sun.enterprise.config.serverbeans.Server;
import com.sun.enterprise.security.SecurityConfigListener;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;

import java.beans.PropertyVetoException;
import org.glassfish.api.admin.AccessRequired;
import org.glassfish.api.admin.AdminCommandSecurity;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;

/**
 * Delete Audit Module Command
 * 
* Usage: delete-audit-module [--terse=false] [--echo=false] 
 *        [--interactive=true] [--host localhost] [--port 4848|4849] 
 *        [--secure | -s] [--user admin_user] [--passwordfile file_name] 
 *        [--target target(Default server)] auth_realm_name
 *
 * @author Nandini Ektare
 */
@Service(name="delete-audit-module")
@PerLookup
@I18n("delete.audit.module")
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType({CommandTarget.DAS,CommandTarget.STANDALONE_INSTANCE,CommandTarget.CLUSTER,CommandTarget.CONFIG})
public class DeleteAuditModule implements AdminCommand, AdminCommandSecurity.Preauthorization {
    
    final private static LocalStringManagerImpl localStrings = 
        new LocalStringManagerImpl(DeleteAuditModule.class);

    @Param(name="auditmodulename", primary=true)
    String auditModuleName;

    @Param(name = "target", optional = true, defaultValue =
        SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME)
    private String target;

    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    private Config config;

    @Inject
    private Domain domain;

    @AccessRequired.To(value="delete")
    private AuditModule auditModule = null;

    @Inject
    SecurityConfigListener securityConfigListener;
    
    private SecurityService securityService;

    @Override
    public boolean preAuthorization(AdminCommandContext context) {
        auditModule = chooseAuditModule(context.getActionReport());
        return true;
    }
    
    /**
     * Executes the command with the command parameters passed as Properties
     * where the keys are the paramter names and the values the parameter values
     *
     * @param context information
     */
    public void execute(AdminCommandContext context) {
        
        ActionReport report = context.getActionReport();

        try {            
            if (auditModule == null) {
                report.setMessage(localStrings.getLocalString(
                    "delete.audit.module.notfound", 
                    "Specified Audit Module {0} not found", auditModuleName));
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return;
            }

            ConfigSupport.apply(new SingleConfigCode<SecurityService>() {
                public Object run(SecurityService param) 
                throws PropertyVetoException, TransactionFailure {
                    
                    param.getAuditModule().remove(auditModule);
                    return null;
                }
            }, securityService);
        } catch(TransactionFailure e) {
            report.setMessage(localStrings.getLocalString(
                "delete.audit.module.fail", "Deletion of Audit Module {0} failed", 
                auditModuleName));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
        }

        /*report.setMessage(localStrings.getLocalString("delete.audit.module.success",
            "Deletion of Audit Module {0} completed successfully", auditModuleName));*/
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }
    
    private AuditModule chooseAuditModule(final ActionReport report) {
        config = CLIUtil.chooseConfig(domain, target, report);
        if (config == null) {
            return null;
        }
        securityService = config.getSecurityService();
        for (AuditModule am : securityService.getAuditModule()) {
                if (am.getName().equals(auditModuleName)) {
                    return am;
                }
        }
        return null;
    }
}
