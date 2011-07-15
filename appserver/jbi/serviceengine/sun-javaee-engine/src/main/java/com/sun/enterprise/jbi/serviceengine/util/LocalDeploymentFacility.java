/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.jbi.serviceengine.util;


import java.util.Collection;
import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.api.admin.CommandException;
import org.glassfish.deployment.client.*;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import org.glassfish.api.ActionReport.MessagePart;
import org.glassfish.internal.api.Globals;
import org.jvnet.hk2.component.Habitat;

/**
 * Provides a DeploymentFacility implementation
 * 
 */
public class LocalDeploymentFacility extends AbstractDeploymentFacility {

    private final CommandRunner commandRunner;

    /**
     * Some options on the remote commands are handled remotely and are not
     * valid when invoking the corresponding command class locally.
     */
    private final static Collection<String> ignoredParameters =
            new HashSet<String>(Arrays.asList("upload"));

    /**
     * Creates a new local DF.
     */
    public LocalDeploymentFacility() {
        super();
        final Habitat habitat = Globals.getDefaultHabitat();
        commandRunner = habitat.getComponent(CommandRunner.class);
    }

    /**
     * Connects to the local domain adminstration server
     * <p>
     * There is no real connection for a local DF so there is no additional,
     * local-specific work to do during connection.
     */
    @Override
    protected boolean doConnect() {
        return true;
    }

    /**
     * Disconnects.
     * <p>
     * There is no real connection, so nothing to do to disconnect.
     */
    @Override
    protected boolean doDisconnect() {
        return true;
    }

    /**
     * Returns the local-connection-specific DFCommandRunner to which the
     * AbstractDeploymentFacility will delegate the actual command execution.
     * @param commandName command to be run
     * @param commandOptions options to pass on the command
     * @param operands operands on the command
     * @return a command runner that works locally
     */
    @Override
    protected DFCommandRunner getDFCommandRunner(
            final String commandName,
            final Map<String,Object> commandOptions,
            final String[] operands) {
        return new LocalDFCommandRunner(commandName, commandOptions, operands);
    }

    /**
     * Executes commands using local access to the DAS.
     */
    private class LocalDFCommandRunner implements DFCommandRunner {

        private final String commandName;
        private final Map<String,Object> commandOptions;
        private final String [] operands;

        private LocalDFCommandRunner(
                final String commandName,
                final Map<String,Object> commandOptions,
                final String[] operands) {
            this.commandOptions = commandOptions;
            this.commandName = commandName;
            this.operands = operands;
        }

        public DFDeploymentStatus run() throws CommandException {
            final ActionReport report = commandRunner.getActionReport("xml");
            final ParameterMap parameters = prepareParameters(commandOptions, operands);
       		commandRunner.getCommandInvocation(commandName, report).parameters(parameters).execute();
            final DFDeploymentStatus status = actionReportToStatus(report);
            return status;
        }

        /**
         * Constructs a ParameterMap object to contain the options as well as
         * the operands at the end of the command.
         *
         * @param commandOptions
         * @param operands
         * @return ParameterMap object suitable for passing to the command runner
         */
        private ParameterMap prepareParameters(final Map<String,Object> commandOptions, final String[] operands) {
            final ParameterMap result = new ParameterMap();
            if (commandOptions != null) {
                for (Map.Entry<String,Object> entry : commandOptions.entrySet()) {
                    if ( ! ignoredParameters.contains(entry.getKey() )) {
                        result.set(entry.getKey(), entry.getValue().toString());
                    }
                }
            }
            if (operands != null) {
                for (String operand : operands) {
                    result.add("DEFAULT", operand);
                }
            }

            return result;
        }
    }

    /**
     * Constructs a DF deployment status object from the ActionReport
     * object.
     *
     * @param report the ActionReport object passed to and changed by the
     * command runner
     * @return DFDeploymentStatus corresponding to the action report
     */
    private static DFDeploymentStatus actionReportToStatus(final ActionReport report) {
        final DFDeploymentStatus result = new DFDeploymentStatus();
        report.getActionExitCode();
        final MessagePart topMessagePart = report.getTopMessagePart();

        result.setStageStatus(DFDeploymentStatus.Status.valueOf(report.getActionExitCode().name()));
        result.setStageStatusMessage(report.getMessage());
        result.setStageException(report.getFailureCause());

        for (final MessagePart messagePart : topMessagePart.getChildren()) {
            result.addSubStage(messagePartToStatus(messagePart));
        }
        return result;
    }

    /**
     * Converts an action report's message part into a DF deployment status,
     * possibly a substage status.
     *
     * @param messagePart action report's MessagePart (from any level)
     * @return the corresponding DFDeploymentStatus
     */
    private static DFDeploymentStatus messagePartToStatus(MessagePart messagePart) {
        final DFDeploymentStatus result = new DFDeploymentStatus();
        result.setStageStatusMessage(messagePart.getMessage());
        for (final MessagePart child : messagePart.getChildren()) {
            result.addSubStage(messagePartToStatus(child));
        }
        return result;
    }

}
