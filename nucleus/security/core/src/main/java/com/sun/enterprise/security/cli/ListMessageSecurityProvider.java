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


import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.ActionReport;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import javax.inject.Named;

import org.glassfish.hk2.api.PerLookup;
import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.config.serverbeans.SecurityService;
import com.sun.enterprise.config.serverbeans.MessageSecurityConfig;
import com.sun.enterprise.config.serverbeans.ProviderConfig;
import com.sun.enterprise.util.SystemPropertyConstants;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
/**
 * List Message Security Providers Command
 * 
 * Usage: list-message-security-providers [--terse=false] [--echo=false] 
 *        [--interactive=true] [--host localhost] [--port 4848|4849] 
 *        [--secure | -s] [--user admin_user] [--passwordfile file_name] 
 *        [--layer message_layer] [target(Default server)] 
 *
 * @author Nandini Ektare
 */

@Service(name="list-message-security-providers")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("list.message.security.provider")
@ExecuteOn({RuntimeType.DAS})
@TargetType({CommandTarget.DAS,CommandTarget.CLUSTERED_INSTANCE,
CommandTarget.STANDALONE_INSTANCE,CommandTarget.CLUSTER, CommandTarget.CONFIG})
@RestEndpoints({
    @RestEndpoint(configBean=SecurityService.class,
        opType=RestEndpoint.OpType.GET, 
        path="list-message-security-providers", 
        description="list-message-security-providers")
})
public class ListMessageSecurityProvider implements AdminCommand, AdminCommandSecurity.Preauthorization {
    
    final private static LocalStringManagerImpl localStrings = 
        new LocalStringManagerImpl(ListMessageSecurityProvider.class);    

    @Param(name = "target", primary=true, optional = true, defaultValue =
        SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME)
    private String target;

    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    private Config config;

    @Inject
    private Domain domain;
    // auth-layer can only be SOAP | HttpServlet
    @Param(name="layer", acceptableValues="SOAP,HttpServlet", optional=true)
    String authLayer;
    
    @AccessRequired.To("read")
    private SecurityService secService;

    @Override
    public boolean preAuthorization(AdminCommandContext context) {
        config = CLIUtil.chooseConfig(domain, target, context.getActionReport());
        if (config == null) {
            return false;
        }
        secService = config.getSecurityService();
        return true;
    }
    
    /**
     * Executes the command with the command parameters passed as Properties
     * where the keys are the paramter names and the values the parameter values
     *
     * @param context information
     */
    public void execute(AdminCommandContext context) {
        
        final ActionReport report = context.getActionReport();

        secService.getMessageSecurityConfig();

        report.getTopMessagePart().setMessage(
            localStrings.getLocalString(
                "list.message.security.provider.success", 
                "list-message-security-providers successful"));
        report.getTopMessagePart().setChildrenType("");
        
        for (MessageSecurityConfig msc : secService.getMessageSecurityConfig()) {
            if (authLayer == null) {
                for (ProviderConfig pc : msc.getProviderConfig()) {
                    ActionReport.MessagePart part = 
                        report.getTopMessagePart().addChild();
                    part.setMessage(pc.getProviderId());                
                }
            } else {
                if (msc.getAuthLayer().equals(authLayer)) {
                    for (ProviderConfig pc : msc.getProviderConfig()) {
                        ActionReport.MessagePart part = 
                            report.getTopMessagePart().addChild();
                        part.setMessage(pc.getProviderId());                                    
                    }
                }
            }
        }
    }
}
