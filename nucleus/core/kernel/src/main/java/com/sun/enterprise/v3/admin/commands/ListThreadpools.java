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
// Portions Copyright [2020-2021] Payara Foundation and/or affiliates

package com.sun.enterprise.v3.admin.commands;


import java.util.List;

import com.sun.enterprise.config.serverbeans.Config;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.ThreadPools;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import jakarta.inject.Inject;


import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;

import org.glassfish.grizzly.config.dom.ThreadPool;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.StringUtils;
import java.util.HashSet;
import java.util.Set;
import org.glassfish.api.admin.*;
import org.glassfish.hk2.api.ServiceHandle;

/**
 * List Thread Pools command
 */
@Service(name = "list-threadpools")
@PerLookup
@CommandLock(CommandLock.LockType.NONE)
@I18n("list.threadpools")
@TargetType({CommandTarget.DAS, CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTER, CommandTarget.CONFIG,
    CommandTarget.CLUSTERED_INSTANCE})
@RestEndpoints({
    @RestEndpoint(configBean=ThreadPools.class,
        opType=RestEndpoint.OpType.GET, 
        path="list-threadpools", 
        description="list-threadpools")
})
public class ListThreadpools implements AdminCommand, AdminCommandSecurity.Preauthorization {

    @Inject
    Domain domain;

    @Param(name = "target", primary = true, optional=true)
    String target;

    @Inject
    ServiceLocator habitat;

    final private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(ListThreadpools.class);

    private ThreadPools threadPools;
    
    @Override
    public boolean preAuthorization(AdminCommandContext context) {
        if (StringUtils.ok(target)) {
            Config config = habitat.getService(Config.class, target);
            if (config == null) { //allow target to include or not include the -config part of the name
                config = habitat.getService(Config.class, target + "-config");
            }
            if (config != null) {
                threadPools  = config.getThreadPools();
            }
        }
        return true;
    }
    
    /**
     * Executes the command
     *
     * @param context information
     */
    @Override
    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();
        if (!StringUtils.ok(target)) {
            List<ServiceHandle<ThreadPool>> pools = habitat.getAllServiceHandles(ThreadPool.class);
            Set<String> poolNames = new HashSet<>();
            for (ServiceHandle<ThreadPool> poolHandle : pools) {
                poolNames.add(poolHandle.getService().getName());
            }
            for (String poolName : poolNames) {
                final ActionReport.MessagePart part = report.getTopMessagePart().addChild();
                part.setMessage(poolName);
            }
            return;
        }
        if (threadPools == null) {
            report.setMessage(localStrings.getLocalString("list.thread.pools.failed", "List Thread Pools failed because of: target " + target + " not found"));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
        }
        try {
            List<ThreadPool> poolList = threadPools.getThreadPool();
            for (ThreadPool pool : poolList) {
                final ActionReport.MessagePart part = report.getTopMessagePart()
                        .addChild();
                part.setMessage(pool.getName());
            }
            report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        } catch (Exception e) {
            String str = e.getMessage();
            report.setMessage(localStrings.getLocalString("list.thread.pools.failed", "List Thread Pools failed because of: " + str));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
        }
    }
}

