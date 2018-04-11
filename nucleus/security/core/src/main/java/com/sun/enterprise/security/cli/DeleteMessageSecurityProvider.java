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
import com.sun.enterprise.config.serverbeans.MessageSecurityConfig;
import com.sun.enterprise.config.serverbeans.ProviderConfig;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;

import java.beans.PropertyVetoException;
import java.util.List;
import org.glassfish.api.admin.AccessRequired;
import org.glassfish.api.admin.AdminCommandSecurity;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.jvnet.hk2.config.ConfigListener;

/**
 * Delete Message Security Provider Command
 * 
 * Usage: delete-message-security-provider --layer message_layer [--terse=false] 
 *        [--echo=false] [--interactive=true] [--host localhost] [--port 4848|4849] 
 *        [--secure | -s] [--user admin_user] [--passwordfile file_name] 
 *        [--target target(Defaultserver)] provider_name
 *
 * @author Nandini Ektare
 */
@Service(name="delete-message-security-provider")
@PerLookup
@I18n("delete.message.security.provider")
@ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType({CommandTarget.DAS,CommandTarget.STANDALONE_INSTANCE,CommandTarget.CLUSTER,CommandTarget.CONFIG})
public class DeleteMessageSecurityProvider implements AdminCommand, AdminCommandSecurity.Preauthorization {
    
    private final static LocalStringManagerImpl localStrings = 
        new LocalStringManagerImpl(DeleteMessageSecurityProvider.class);

    @Param(name="providername", primary=true)
    String providerId;
 
    // auth-layer can only be SOAP | HttpServlet
    @Param(name="layer",defaultValue="SOAP")
    String authLayer;
    
    @Param(name = "target", optional = true, defaultValue =
        SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME)
    private String target;

    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    private Config config;

    @Inject
    private Domain domain;

    ProviderConfig thePC = null;

    @AccessRequired.To("delete")
    private MessageSecurityConfig msgSecCfg = null;
    
    private SecurityService secService;

    @Override
    public boolean preAuthorization(AdminCommandContext context) {
        config = CLIUtil.chooseConfig(domain, target, context.getActionReport());
        if (config == null) {
            return false;
        }
        secService = config.getSecurityService();
        msgSecCfg = CLIUtil.findMessageSecurityConfig(secService, authLayer);
        if (msgSecCfg == null) {
            final ActionReport report = context.getActionReport();
            report.setMessage(localStrings.getLocalString(
                "delete.message.security.provider.confignotfound", 
                "A Message security config does not exist for the layer {0}", 
                authLayer));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return false;            
        }
        return true;
    }
    
    /**
     * Executes the command with the command parameters passed as Properties
     * where the keys are the paramter names and the values the parameter values
     *
     * @param context information
     */
    @Override
    public void execute(AdminCommandContext context) {
        
        ActionReport report = context.getActionReport();
        
        List<ProviderConfig> pcs = msgSecCfg.getProviderConfig();
        for (ProviderConfig pc : pcs) {
            if (pc.getProviderId().equals(providerId)) { 
                thePC = pc;
                try {
                    ConfigSupport.apply(new SingleConfigCode<MessageSecurityConfig>() {
                        
                        @Override
                        public Object run(MessageSecurityConfig param) 
                        throws PropertyVetoException, TransactionFailure {

                            if ((param.getDefaultProvider() != null) &&
                                param.getDefaultProvider().equals(
                                    thePC.getProviderId())) {
                                param.setDefaultProvider(null);
                            }
                                
                            if ((param.getDefaultClientProvider() != null) &&
                                 param.getDefaultClientProvider().equals(
                                    thePC.getProviderId())) {
                                param.setDefaultClientProvider(null);
                            }
                            
                            param.getProviderConfig().remove(thePC);                                                                                       
                            return null;
                        }
                    }, msgSecCfg);
                } catch(TransactionFailure e) {
                    e.printStackTrace();
                    report.setMessage(localStrings.getLocalString(
                        "delete.message.security.provider.fail", 
                        "Deletion of message security provider named {0} failed", 
                        providerId));
                    report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                    report.setFailureCause(e);
                    return;
                }
                /*report.setMessage(localStrings.getLocalString(
                    "delete.message.security.provider.success", 
                    "Deletion of message security provider {0} completed " +
                    "successfully", providerId));*/
                report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
                return;                
            }
        }
    }
}
