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

package org.glassfish.jts.admin.cli;

import com.sun.enterprise.config.serverbeans.Server;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.AdminCommand;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.ExecuteOn;
import org.glassfish.api.admin.RuntimeType;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.config.support.CommandTarget;
import org.glassfish.config.support.TargetType;

import org.jvnet.hk2.annotations.Service;

import javax.inject.Inject;
import org.glassfish.hk2.api.PerLookup;

import java.util.logging.Level;
import org.glassfish.api.admin.RestEndpoint;
import org.glassfish.api.admin.RestEndpoints;
import org.glassfish.api.admin.RestParam;

@Service(name = "recover-transactions")
@TargetType({CommandTarget.DAS,CommandTarget.STANDALONE_INSTANCE, CommandTarget.CLUSTERED_INSTANCE})
@ExecuteOn(RuntimeType.DAS)
@PerLookup
@I18n("recover.transactions")
@RestEndpoints({
    @RestEndpoint(configBean=Server.class,
        opType=RestEndpoint.OpType.POST, 
        path="recover-transactions", 
        description="Recover Transactions",
        params={
            @RestParam(name="id", value="$parent")
        })
})
public class RecoverTransactions extends RecoverTransactionsBase implements AdminCommand {

    @Param(name="target", alias = "destination", optional = true)
    String destinationServer;

    @Inject 
    CommandRunner runner;

    public void execute(AdminCommandContext context) {
        final ActionReport report = context.getActionReport();

        if (_logger.isLoggable(Level.INFO)) {
            _logger.info("==> original target: " + destinationServer + " ... server: " + serverToRecover);
        }

        String error = validate(destinationServer, false);
        if (error != null) {
            _logger.log(Level.WARNING, localStrings.getString("recover.transactions.failed") + " " + error);
            report.setMessage(error);
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            return;
        }

        //here we are only if parameters consistent
        if(destinationServer==null)
            destinationServer = serverToRecover;

        try {
            boolean result;
            CommandRunner.CommandInvocation inv = runner.getCommandInvocation(
                    "_recover-transactions-internal", report, context.getSubject());

            final ParameterMap parameters = new ParameterMap();
            parameters.add("target", destinationServer);
            parameters.add("DEFAULT", serverToRecover);
            parameters.add("transactionlogdir", transactionLogDir);

            if (_logger.isLoggable(Level.INFO)) {
                _logger.info("==> calling _recover-transactions-internal with params: " + parameters);
            }

            inv.parameters(parameters).execute();

            if (_logger.isLoggable(Level.INFO)) {
                _logger.info("==> _recover-transactions-internal returned with: " + report.getActionExitCode());
            }

            // Exit code is set by _recover-transactions-internal

        } catch (Exception e) {
            _logger.log(Level.WARNING, localStrings.getString("recover.transactions.failed"), e);
            report.setMessage(localStrings.getString("recover.transactions.failed"));
            report.setActionExitCode(ActionReport.ExitCode.FAILURE);
            report.setFailureCause(e);
        }
    }
}
