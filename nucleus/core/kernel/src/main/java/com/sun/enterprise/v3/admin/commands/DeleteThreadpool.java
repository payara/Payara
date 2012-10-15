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

package com.sun.enterprise.v3.admin.commands;

import java.beans.PropertyVetoException;
import java.util.List;

import org.glassfish.internal.api.Target;
import com.sun.enterprise.config.serverbeans.*;
import org.glassfish.api.admin.*;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.ActionReport;

import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;
import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import javax.inject.Named;

import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

import org.glassfish.grizzly.config.dom.ThreadPool;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.grizzly.config.dom.Protocol;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;
import org.glassfish.api.ActionReport.ExitCode;

@Service(name="delete-threadpool")
@PerLookup
@I18n("delete.threadpool")
@org.glassfish.api.admin.ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType({CommandTarget.DAS,CommandTarget.STANDALONE_INSTANCE,CommandTarget.CLUSTER,CommandTarget.CONFIG})
public class DeleteThreadpool implements AdminCommand, AdminCommandSecurity.Preauthorization {

    final private static LocalStringManagerImpl localStrings =
            new LocalStringManagerImpl(DeleteThreadpool.class);

    @Param(name="threadpool_id", primary=true)
    String threadpool_id;

    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME)
    String target;
    
    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    Config config;

    @Inject
    Configs configs;

    @Inject
    Domain domain;

    @Inject
    ServiceLocator habitat;    

    private ThreadPools threadPools;
    
    @AccessRequired.To("delete")
    private ThreadPool pool;
    
    @Override
    public boolean preAuthorization(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();
        config = CLIUtil.updateConfigIfNeeded(config, target, habitat);
        threadPools  = config.getThreadPools();
        if(!isThreadPoolExists(threadPools)) {
            report.setMessage(localStrings.getLocalString("delete.threadpool.notexists",
                "Thread Pool named {0} does not exist.", threadpool_id));
            report.setActionExitCode(ExitCode.FAILURE);
            return false;
        }
        pool = null;
        for (ThreadPool tp : config.getThreadPools().getThreadPool()) {
            if (tp.getName().equals(threadpool_id)) {
                pool = tp;
            }
        }

        List<NetworkListener> nwlsnrList = pool.findNetworkListeners();
        for (NetworkListener nwlsnr : nwlsnrList) {
            if (pool.getName().equals(nwlsnr.getThreadPool())) {
                report.setMessage(localStrings.getLocalString(
                    "delete.threadpool.beingused",
                    "{0} threadpool is being used in the network listener {1}",
                    threadpool_id, nwlsnr.getName()));
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return false;
            }
        }
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
            ConfigSupport.apply(new SingleConfigCode<ThreadPools>() {
                public Object run(ThreadPools param) throws PropertyVetoException,
                        TransactionFailure {
                    List<ThreadPool> poolList = param.getThreadPool();
                    for (ThreadPool pool : poolList) {
                        String currPoolId = pool.getName();
                        if (currPoolId != null && currPoolId.equals
                                (threadpool_id)) {
                            poolList.remove(pool);
                            break;
                        }
                    }
                    return poolList;
                }
            }, threadPools);
            report.setActionExitCode(ExitCode.SUCCESS);
        } catch(TransactionFailure e) {
            String str = e.getMessage();
            report.setMessage(localStrings.getLocalString("delete.threadpool" +
                    ".failed", "Delete Thread Pool failed because of: ", str));
            report.setActionExitCode(ExitCode.FAILURE);
            report.setFailureCause(e);
        }
    }

    private boolean isThreadPoolExists(ThreadPools threadPools) {

        for (ThreadPool pool : threadPools.getThreadPool()) {
            String currPoolId = pool.getName();
            if (currPoolId != null && currPoolId.equals(threadpool_id)) {
                return true;
            }
        }
        return false;
    }

}
