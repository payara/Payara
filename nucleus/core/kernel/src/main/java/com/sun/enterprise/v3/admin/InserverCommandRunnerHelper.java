/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * 
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.v3.admin;

import com.sun.enterprise.util.LocalStringManagerImpl;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.security.auth.Subject;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.kernel.KernelLoggerInfo;
import org.jvnet.hk2.annotations.Service;

/**
 *
 * Allows commands executing in the DAS or an instance to invoke other
 * commands in the same server.  This will be most useful from commands
 * that need to change configuration settings by delegating to a
 * sequence of other commands.
 * <p>
 * This is similar to some logic in the AdminAdapter.  
 *
 * @author Tim Quinn
 *
 */
@Service
@Singleton
public class InserverCommandRunnerHelper {

    public final static Logger logger = KernelLoggerInfo.getLogger();
    public final static LocalStringManagerImpl adminStrings = new LocalStringManagerImpl(InserverCommandRunnerHelper.class);

    @Inject
    private CommandRunnerImpl commandRunner;

    public ActionReport runCommand(final String command,
            final ParameterMap parameters,
            final ActionReport report,
            final Subject subject) {
        try {
            final AdminCommand adminCommand = commandRunner.getCommand(command, report, logger);
            if (adminCommand==null) {
                // maybe commandRunner already reported the failure?
                if (report.getActionExitCode() == ActionReport.ExitCode.FAILURE)
                    return report;
                String message =
                    adminStrings.getLocalString("adapter.command.notfound",
                        "Command {0} not found", command);
                // cound't find command, not a big deal
                logger.log(Level.FINE, message);
                report.setMessage(message);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                return report;
            }
            CommandRunner.CommandInvocation inv = commandRunner.getCommandInvocation(command, report, subject);
            inv.parameters(parameters).execute();
        } catch (Throwable t) {
            /*
             * Must put the error information into the report
             * for the client to see it.
             */
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(t);
            report.setMessage(t.getLocalizedMessage());
            report.setActionDescription("Last-chance exception handler");
        }
        return report;
    }
}
