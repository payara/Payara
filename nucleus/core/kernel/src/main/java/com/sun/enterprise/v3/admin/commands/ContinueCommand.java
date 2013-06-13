/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2013 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.v3.admin.CommandRunnerImpl;
import com.sun.enterprise.v3.admin.CompletedJob;
import com.sun.enterprise.v3.admin.JobManagerService;
import static com.sun.enterprise.v3.admin.commands.AttachCommand.strings;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.CommandLock;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.JobManager;
import org.glassfish.api.admin.ManagedJob;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.kernel.KernelLoggerInfo;
import org.jvnet.hk2.annotations.Service;

/**
 *
 * @author martinmares
 */
//@Service(name = "continue")
//@PerLookup
//@CommandLock(CommandLock.LockType.NONE)
//@I18n("continue")
//@ManagedJob
public class ContinueCommand implements AdminCommand {
    
    protected final static LocalStringManagerImpl strings = new LocalStringManagerImpl(AttachCommand.class);
    
    @Inject
    JobManagerService registry;
    
    @Inject
    CommandRunnerImpl commandRunner;

    @Param(primary=true, optional=false, multiple=false)
    protected String jobID;

    @Override
    public void execute(AdminCommandContext context) {
        ActionReport report = context.getActionReport();
        CompletedJob completedJob = registry.getRetryableJobsInfo().get(jobID);
        if (completedJob == null) {
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setMessage(strings.getLocalString("continue.wrong.commandinstance.id", "Parked job with id {0} does not exist.", jobID));
            return;
        }
        try {
            JobManager.Checkpoint checkpoint = registry.loadCheckpoint(jobID, context.getOutboundPayload());
            if (checkpoint == null) {
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setMessage(strings.getLocalString("continue.wrong.commandinstance.id", "Parked job with id {0} does not exist.", jobID));
                return;
            }
            //Execute it
            commandRunner.executeFromCheckpoint(checkpoint, false, context.getEventBroker());
        } catch (Exception ex) {
            context.getLogger().log(Level.WARNING, KernelLoggerInfo.exceptionLoadCheckpoint, ex);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(ex);
        }
    }
    
}
