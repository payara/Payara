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
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.SystemPropertyConstants;


/**
 * Create Thread Pool Command
 *
 */
@Service(name="create-threadpool")
@PerLookup
@I18n("create.threadpool")
@org.glassfish.api.admin.ExecuteOn({RuntimeType.DAS, RuntimeType.INSTANCE})
@TargetType({CommandTarget.DAS,CommandTarget.STANDALONE_INSTANCE,CommandTarget.CLUSTER,CommandTarget.CONFIG})

public class CreateThreadpool implements AdminCommand, AdminCommandSecurity.Preauthorization {

    final private static LocalStringManagerImpl localStrings = new
            LocalStringManagerImpl(CreateThreadpool.class);

    // TODO:  Once Grizzly provides constants for default values, update this class to use those
    // constants: https://grizzly.dev.java.net/issues/show_bug.cgi?id=897 -- jdlee
    @Param(name="maxthreadpoolsize", optional=true, alias="maxThreadPoolSize", defaultValue = "5")
    String maxthreadpoolsize;

    @Param(name="minthreadpoolsize", optional=true, alias="minThreadPoolSize", defaultValue = "2")
    String minthreadpoolsize;

    @Param(name= "idletimeout", optional=true, alias="idleThreadTimeoutSeconds", defaultValue = "900")
    String idletimeout;

    @Param(name="workqueues", optional=true)
    String workqueues;

    @Param(name="maxqueuesize", optional=true, alias="maxQueueSize", defaultValue = "4096")
    String maxQueueSize;

    @Param(name = "target", optional = true, defaultValue = SystemPropertyConstants.DEFAULT_SERVER_INSTANCE_NAME)
    String target;
    
    @Param(name="threadpool_id", primary=true)
    String threadpool_id;
    
    @Inject @Named(ServerEnvironment.DEFAULT_INSTANCE_NAME)
    Config config;

    @Inject
    Domain domain;
    
    @Inject
    ServiceLocator habitat;

    @AccessRequired.NewChild(type=ThreadPool.class)
    private ThreadPools threadPools;
    
    @Override
    public boolean preAuthorization(AdminCommandContext context) {
        config = CLIUtil.updateConfigIfNeeded(config, target, habitat);
        threadPools  = config.getThreadPools();
        for (ThreadPool pool: threadPools.getThreadPool()) {
            final ActionReport report = context.getActionReport();
            if (pool.getName().equals(threadpool_id)) {
                report.setMessage(localStrings.getLocalString("create.threadpool.duplicate",
                        "Thread Pool named {0} already exists.", threadpool_id));
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
        final ActionReport report = context.getActionReport();
        if (workqueues != null) {
            report.setMessage(localStrings.getLocalString("create.threadpool.deprecated.workqueues",
                        "Deprecated Syntax: --workqueues option is deprecated for create-threadpool command."));
        }

        try {
            ConfigSupport.apply(new SingleConfigCode<ThreadPools>() {
                public Object run(ThreadPools param) throws PropertyVetoException, TransactionFailure {
                    ThreadPool newPool = param.createChild(ThreadPool.class);
                    newPool.setName(threadpool_id);
                    newPool.setMaxThreadPoolSize(maxthreadpoolsize);
                    newPool.setMinThreadPoolSize(minthreadpoolsize);
                    newPool.setMaxQueueSize(maxQueueSize);
                    newPool.setIdleThreadTimeoutSeconds(idletimeout);
                    param.getThreadPool().add(newPool);
                    return newPool;
                }
            }, threadPools);
            report.setActionExitCode(ActionReport.ExitCode.SUCCESS);
        } catch (TransactionFailure e) {
            String str = e.getMessage();
            String def = "Creation of: " + threadpool_id + "failed because of: " + str;
            String msg = localStrings.getLocalString("create.threadpool.failed", def, threadpool_id, str);
            report.setMessage(msg);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
        }
    }
}
