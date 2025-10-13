/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
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
//Portions Copyright [2018-2021] Payara Foundation and/or affiliates
package org.glassfish.web.admin.cli;

import static org.glassfish.api.admin.CommandLock.LockType.NONE;
import static org.glassfish.api.admin.RestEndpoint.OpType.GET;
import static org.glassfish.config.support.CommandTarget.CLUSTER;
import static org.glassfish.config.support.CommandTarget.CONFIG;
import static org.glassfish.config.support.CommandTarget.DAS;
import static org.glassfish.config.support.CommandTarget.STANDALONE_INSTANCE;

import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.admin.ServerEnvironment;
import org.glassfish.config.support.TargetType;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Target;
import org.jvnet.hk2.annotations.Service;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.HttpService;
import com.sun.enterprise.config.serverbeans.VirtualServer;
import com.sun.enterprise.util.SystemPropertyConstants;

/**
 * List virtual server command
 */
@Service(name = "list-virtual-servers")
@PerLookup
@CommandLock(NONE)
@I18n("list.virtual.servers")
@ExecuteOn(RuntimeType.DAS)
@TargetType({ DAS, STANDALONE_INSTANCE, CLUSTER, CONFIG })
@RestEndpoints({
        @RestEndpoint(
                configBean = HttpService.class, 
                opType = GET, 
                path = "list-virtual-servers", 
                description = "list-virtual-servers")
})
public class ListVirtualServers implements AdminCommand {
    
	@Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DAS_SERVER_NAME)
    String target;
    
    @Inject 
    @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    Config config;
    
    @Inject
    Domain domain;
    
    @Inject
    ServiceLocator services;

    /**
     * Executes the command with the command parameters passed as Properties where the keys are the paramter names and
     * the values the parameter values
     *
     * @param context information
     */
    public void execute(AdminCommandContext context) {
        Target targetUtil = services.getService(Target.class);
        
        Config newConfig = targetUtil.getConfig(target);
        if (newConfig!=null) {
            config = newConfig;
        }
        
        ActionReport report = context.getActionReport();
        List<VirtualServer> list = config.getHttpService().getVirtualServer();
        for (VirtualServer virtualServer : list) {
            report.getTopMessagePart()
                  .addChild()
                  .setMessage(virtualServer.getId());
        }
        
        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }
}
