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

package com.sun.enterprise.admin.cli.embeddable;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import javax.inject.Inject;

import org.glassfish.api.ActionReport;
import org.glassfish.api.admin.CommandException;
import org.glassfish.api.admin.CommandModel;
import org.glassfish.api.admin.CommandRunner;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.embeddable.CommandResult;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.annotations.ContractsProvided;
import org.jvnet.hk2.annotations.Service;

import com.sun.enterprise.admin.cli.CLIUtil;
import com.sun.enterprise.admin.cli.Parser;
import com.sun.enterprise.admin.cli.ProgramOptions;
import org.glassfish.internal.api.EmbeddedSystemAdministrator;

/**
 * @author bhavanishankar@dev.java.net
 * @author sanjeeb.sahoo@sun.com
 */
@Service()
@PerLookup // this is a PerLookup service
@ContractsProvided({org.glassfish.embeddable.CommandRunner.class, CommandExecutorImpl.class})
// bcos CommandRunner interface can't depend on HK2, we need ContractProvided here.

public class CommandExecutorImpl implements org.glassfish.embeddable.CommandRunner {

    @Inject
    CommandRunner commandRunner;

    @Inject
    ServiceLocator habitat;
    
    @Inject
    private EmbeddedSystemAdministrator embeddedSystemAdministrator;

    private boolean terse;

    private Logger logger = Logger.getAnonymousLogger();

    public CommandResult run(String command, String... args){
        try {
            ActionReport actionReport = executeCommand(command, args);
            return convert(actionReport);
        } catch (Exception e) {
            return convert(e);
        }
    }

    ParameterMap getParameters(String command, String[] args) throws CommandException {
        CommandModel commandModel = commandRunner.getModel(command, logger);
        if (commandModel == null) {
            throw new CommandException("Command lookup failed for command " + command);
        }

        // Filter out the global options.
        // We are interested only in --passwordfile option. No other options are relevant when GlassFish is running in embedded mode.
        Parser parser = new Parser(args, 0, ProgramOptions.getValidOptions(), true);
        ParameterMap globalOptions = parser.getOptions();
        List<String> operands = parser.getOperands();
        String argv[] = operands.toArray(new String[operands.size()]);

        parser = new Parser(argv, 0, commandModel.getParameters(), false);
        ParameterMap options = parser.getOptions();
        operands = parser.getOperands();
        options.set("DEFAULT", operands);
        // if command has a "terse" option, set it in options
        if (commandModel.getModelFor("terse") != null)
            options.set("terse", Boolean.toString(terse));

        // Read the passwords from the password file and set it in command options.
        if (globalOptions.size() > 0) {
            String pwfile = globalOptions.getOne(ProgramOptions.PASSWORDFILE);
            if (pwfile != null && pwfile.length() > 0) {
                Map<String, String> passwords = CLIUtil.readPasswordFileOptions(pwfile, true);
                for (CommandModel.ParamModel opt : commandModel.getParameters()) {
                    if (opt.getParam().password()) {
                        String pwdname = opt.getName();
                        String pwd = passwords.get(pwdname);
                        if (pwd != null) {
                            options.set(pwdname, pwd);
                        }
                    }
                }
            }
        }
        
        return options;
    }

    public void setTerse(boolean terse) {
        this.terse = terse;
    }

    /**
     * Runs a command from somewhere OTHER THAN an already-running, 
     * previously-authorized command.
     * <p>
     * If a command is already running then it should have a valid Subject and
     * that Subject must be used in running a nested command.  This
     * method uses the internal system admin identity to authorize the command to be run and
     * this should never be done if a user has authenticated to the system
     * and is running a separate, already-authorized command.  This method
     * is, therefore, used from some embedded functionality.
     * 
     * @param command
     * @param args
     * @return
     * @throws CommandException 
     */
    /* package */ ActionReport executeCommand(String command, String... args) throws CommandException {
        ParameterMap commandParams = getParameters(command, args);
        final ActionReport actionReport = createActionReport();

        org.glassfish.api.admin.CommandRunner.CommandInvocation inv =
                commandRunner.getCommandInvocation(command, actionReport, embeddedSystemAdministrator.getSubject(),false);

        inv.parameters(commandParams).execute();

        return actionReport;
    }

    private CommandResult convert(final ActionReport actionReport) {
        return new CommandResult(){
            public ExitStatus getExitStatus() {
                final ActionReport.ExitCode actionExitCode = actionReport.getActionExitCode();
                switch (actionExitCode) {
                    case SUCCESS:
                        return ExitStatus.SUCCESS;
                    case WARNING:
                        return ExitStatus.WARNING;
                    case FAILURE:
                        return ExitStatus.FAILURE;
                    default:
                        throw new RuntimeException("Unknown exit code: " + actionExitCode);
                }
            }

            public String getOutput() {
                final ByteArrayOutputStream os = new ByteArrayOutputStream();
                try {
                    actionReport.writeReport(os);
                    return os.toString();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                } finally {
                    try {
                        os.close();
                    } catch (IOException e) {
                        // ignore
                    }
                }
            }

            public Throwable getFailureCause() {
                return actionReport.getFailureCause();
            }
        };
    }

    private CommandResult convert(final Exception e) {
        return new CommandResult() {
            public ExitStatus getExitStatus() {
                return ExitStatus.FAILURE;
            }

            public String getOutput() {
                return "Exception while executing command.";
            }

            public Throwable getFailureCause() {
                return e;
            }
        };
    }

    ActionReport createActionReport() {
        return habitat.getService(ActionReport.class, "plain");
    }

    CommandRunner getCommandRunner() {
        return commandRunner;
    }

}
