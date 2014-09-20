/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.loadbalancer.admin.cli;

import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.List;


import org.jvnet.hk2.annotations.Service;
import org.jvnet.hk2.component.*;
import org.glassfish.api.Param;
import org.glassfish.api.ActionReport;
import com.sun.enterprise.util.LocalStringManagerImpl;

import org.glassfish.hk2.api.PerLookup;
import org.glassfish.internal.api.Target;

import com.sun.enterprise.config.serverbeans.ClusterRef;
import com.sun.enterprise.config.serverbeans.ServerRef;
import com.sun.enterprise.config.serverbeans.Domain;
import org.glassfish.loadbalancer.config.LbConfigs;
import org.glassfish.loadbalancer.config.LbConfig;

import org.glassfish.api.admin.*;

import javax.inject.Inject;

/**
 * This is a remote commands to list lb configs (ported from v2)
 * Interestingly, in this command, operand has multiple meanings - it can be
 * a target (cluster or standalone instance) or a lb config name.
 * And operand is optional!
 *
 *   No operand          : list all lb configs
 *   Operand is LB config: list all cluster-refs and server-refs for the LB config
 *   Operand is cluster  : list lb configs referencing the cluster
 *   Operand is instance : list all configs referencing the server instance
 * 
 * @author Yamini K B
 */
@Service(name = "list-http-lb-configs")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@org.glassfish.api.admin.ExecuteOn(RuntimeType.DAS)
@RestEndpoints({
    @RestEndpoint(configBean=LbConfigs.class,
        opType=RestEndpoint.OpType.GET, 
        path="list-http-lb-configs", 
        description="list-http-lb-configs")
})
public final class ListLBConfigsCommand implements AdminCommand {

    @Param(primary=true, optional=true)
    String list_target;

    @Inject
    Domain domain;

    @Inject
    Target tgt;

    @Inject
    Logger logger;

    private ActionReport report;

    final private static LocalStringManagerImpl localStrings =
        new LocalStringManagerImpl(ListLBConfigsCommand.class);
    
    @Override
    public void execute(AdminCommandContext context) {

        report = context.getActionReport();

        ActionReport.MessagePart part = report.getTopMessagePart();

        boolean isCluster = tgt.isCluster(list_target);

        LbConfigs lbconfigs = domain.getExtensionByType(LbConfigs.class);
        if (lbconfigs == null) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.fine(localStrings.getLocalString(
                            "http_lb_admin.NoLbConfigs", "No lb configs"));
                }
                return;
        }

        List<LbConfig> lbconfigsList = lbconfigs.getLbConfig();
        if (lbconfigsList.size() == 0) {
                logger.fine(localStrings.getLocalString(
                        "http_lb_admin.NoLbConfigs", "No lb configs"));
                return;
        }
        
        if (list_target == null) {
            for (LbConfig lbc: lbconfigsList) {
                ActionReport.MessagePart childPart = part.addChild();
                childPart.setMessage(lbc.getName());
            }
        } else {
            // target is a cluster
            if (isCluster) {
                
                for (LbConfig lbc: lbconfigsList) {
                    List<ClusterRef> refs = lbc.getRefs(ClusterRef.class);
                    for (ClusterRef cRef:refs) {
                       if (cRef.getRef().equals(list_target) ) {
                            ActionReport.MessagePart childPart = part.addChild();
                            childPart.setMessage(lbc.getName());
                       }
                    }
                }


            // target is a server
            } else if (domain.isServer(list_target)) {
                
                for (LbConfig lbc: lbconfigsList) {
                    List<ServerRef> refs = lbc.getRefs(ServerRef.class);
                    for (ServerRef sRef:refs) {
                       if (sRef.getRef().equals(list_target) ) {
                            ActionReport.MessagePart childPart = part.addChild();
                            childPart.setMessage(lbc.getName());
                       }
                    }
                }


            } else {

                // target is a lb config
                LbConfig lbConfig = lbconfigs.getLbConfig(list_target);
                
                if (lbConfig != null) {
                    
                    List<ClusterRef> cRefs = lbConfig.getRefs(ClusterRef.class);
                    for (ClusterRef ref: cRefs) {
                        String s = localStrings.getLocalString("ClusterPrefix", "Cluster:");
                        ActionReport.MessagePart childPart = part.addChild();
                        childPart.setMessage(s + ref.getRef());
                    }

                    List<ServerRef> sRefs = lbConfig.getRefs(ServerRef.class);
                    for (ServerRef ref: sRefs) {
                        String s = localStrings.getLocalString("ServerPrefix", "Server:");
                        ActionReport.MessagePart childPart = part.addChild();
                        childPart.setMessage(s + ref.getRef());
                    }                    
                }
            } 
        }

        report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
    }
}
