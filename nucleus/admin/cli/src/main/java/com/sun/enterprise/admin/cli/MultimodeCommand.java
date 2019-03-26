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
// Portions Copyright [2018] Payara Foundation and/or affiliates

package com.sun.enterprise.admin.cli;

import static com.sun.enterprise.admin.cli.CLICommand.ERROR;
import java.io.*;
import java.util.*;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.api.admin.CommandModel.ParamModel;
import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.DynamicConfigurationService;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.BuilderHelper;

import com.sun.enterprise.admin.util.*;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import java.util.logging.Level;

import javax.inject.Inject;
import jline.console.ConsoleReader;
import jline.console.completer.Completer;
import jline.console.completer.StringsCompleter;

/**
 * A scaled-down implementation of multi-mode command.
 *
 * @author केदार(km@dev.java.net)
 * @author Bill Shannon
 */
@Service(name = "multimode")
@PerLookup
public class MultimodeCommand extends CLICommand {

    @Inject
    private ServiceLocator habitat;
    @Inject
    private CLIContainer container;
    @Param(optional = true, shortName = "f")
    private File file;
    @Param(name = "printprompt", optional = true)
    private Boolean printPromptOpt;
    private boolean printPrompt;
    @Param(optional = true)
    private String encoding;
    private boolean echo;       // saved echo flag
    private static final LocalStringsImpl strings = new LocalStringsImpl(MultimodeCommand.class);
    
    private static final String ASADMIN = "asadmin";

    /**
     * The validate method validates that the type and quantity of parameters
     * and operands matches the requirements for this command. The validate
     * method supplies missing options from the environment.
     * @throws CommandException
     * @throws CommandValidationException
     */
    @Override
    protected void validate() throws CommandException, CommandValidationException {
        if (printPromptOpt != null) {
            printPrompt = printPromptOpt;
        } else {
            printPrompt = programOpts.isInteractive();
        }
        /*
         * Save value of --echo because CLICommand will reset it
         * before calling our executeCommand method but we want it
         * to also apply to all commands in multimode.
         */
        echo = programOpts.isEcho();
    }

    /**
     * In the usage message modify the --printprompt option to have a default
     * based on the --interactive option.
     * @return 
     */
    @Override
    protected Collection<ParamModel> usageOptions() {
        Collection<ParamModel> opts = commandModel.getParameters();
        Set<ParamModel> uopts = new LinkedHashSet<ParamModel>();
        ParamModel p = new CommandModelData.ParamModelData("printprompt", boolean.class, true, Boolean.toString(programOpts.isInteractive()));
        for (ParamModel pm : opts) {
            if (pm.getName().equals("printprompt")) {
                uopts.add(p);
            } else {
                uopts.add(pm);
            }
        }
        return uopts;
    }

    @Override
    protected int executeCommand() throws CommandException, CommandValidationException {
        ConsoleReader reader = null;
        programOpts.setEcho(echo);       // restore echo flag, saved in validate
        try {            
            if (file == null) {
                System.out.println(strings.get("multimodeIntro"));
                reader = new ConsoleReader(ASADMIN, System.in, System.out, null, encoding);
            } else {
                printPrompt = false;
                if (!file.canRead()) {
                    throw new CommandException("File: " + file + " can not be read");
                }
                OutputStream out = new OutputStream() {

                    @Override
                    public void write(int b) throws IOException {
                        return;
                    }

                    @Override
                    public void write(byte[] b) throws IOException {
                        return;
                    }

                    @Override
                    public void write(byte[] b, int off, int len) throws IOException {
                        return;
                    }
                };                
                reader = new ConsoleReader(ASADMIN, new FileInputStream(file), out, null, encoding);
            }
            
            reader.setBellEnabled(false);
            reader.addCompleter(getAllCommandsCompleter());
            
            return executeCommands(reader);
        }
        catch (IOException e) {
            throw new CommandException(e);
        }
        finally {
            try {
                if (file != null && reader != null)
                    reader.close();
            }
            catch (Exception e) {
                // ignore it
            }
        }
    }

    private static void atomicReplace(ServiceLocator locator, ProgramOptions options) {
        DynamicConfigurationService dcs = locator.getService(DynamicConfigurationService.class);
        DynamicConfiguration config = dcs.createDynamicConfiguration();

        config.addUnbindFilter(BuilderHelper.createContractFilter(ProgramOptions.class.getName()));
        ActiveDescriptor<ProgramOptions> desc = BuilderHelper.createConstantDescriptor(options, null, ProgramOptions.class);
        config.addActiveDescriptor(desc);

        config.commit();
    }

    /**
     * Read commands from the specified BufferedReader and execute them. If
     * printPrompt is set, prompt first.
     *
     * @return the exit code of the last command executed
     */
    private int executeCommands(ConsoleReader reader) throws CommandException, CommandValidationException, IOException {
        String line = null;
        int rc = 0;

        /*
         * Any program options we start with are copied to the environment
         * to serve as defaults for commands we run, and then we give each
         * command an empty program options.
         */
        programOpts.toEnvironment(env);
        String prompt = programOpts.getCommandName() + "> ";
        for (;;) {
            if (printPrompt) {
                line = reader.readLine(prompt);
            } else {
                line = reader.readLine();
            }
            if (line == null) {
                if (printPrompt)
                    System.out.println();
                break;
            }

            if (line.trim().startsWith("#")){   // ignore comment lines
                continue;
            }
            
            String[] args;
            try {
                args = getArgs(line);
            } catch (ArgumentTokenizer.ArgumentException ex) {
                logger.info(ex.getMessage());
                continue;
            }

            if (args.length == 0) {
                continue;
            }

            String command = args[0];
            if (command.length() == 0) {
                continue;
            }
            
            // handle built-in exit and quit commands
            // XXX - care about their arguments?
            if (command.equals("exit") || command.equals("quit")){
                break;
            }

            CLICommand cmd = null;
            ProgramOptions po = null;
            try {
                /*
                 * Every command gets its own copy of program options
                 * so that any program options specified in its
                 * command line options don't effect other commands.
                 * But all commands share the same environment.
                 */
                po = new ProgramOptions(env);
                // copy over AsadminMain info
                po.setClassPath(programOpts.getClassPath());
                po.setClassName(programOpts.getClassName());
                po.setCommandName(programOpts.getCommandName());
                // remove the old one and replace it
                atomicReplace(habitat, po);

                cmd = CLICommand.getCommand(habitat, command);
                rc = cmd.execute(args);
            }
            catch (CommandValidationException cve) {
                logger.severe(cve.getMessage());
                if (cmd != null) {
                    logger.severe(cmd.getUsage());
                }
                rc = ERROR;
            }
            catch (InvalidCommandException ice) {
                // find closest match with local or remote commands
                logger.severe(ice.getMessage());
                try {
                    if(po != null)  // many layers below, null WILL be de-referenced.
                        CLIUtil.displayClosestMatch(command,
                            CLIUtil.getAllCommands(container, po, env),
                            strings.get("ClosestMatchedLocalAndRemoteCommands"), logger);
                }
                catch (InvalidCommandException e) {
                    // not a big deal if we cannot help
                }
                rc = ERROR;
            }
            catch (CommandException ce) {
                if (ce.getCause() instanceof java.net.ConnectException) {
                    // find closest match with local commands
                    logger.severe(ce.getMessage());
                    try {
                        CLIUtil.displayClosestMatch(command,
                                CLIUtil.getLocalCommands(container),
                                strings.get("ClosestMatchedLocalCommands"), logger);
                    }
                    catch (InvalidCommandException e) {
                        logger.info(strings.get("InvalidRemoteCommand", command));
                    }
                } else {
                    logger.severe(ce.getMessage());
                }
                rc = ERROR;
            } finally {
                // restore the original program options
                // XXX - is this necessary?
                atomicReplace(habitat, programOpts);
            }

            // XXX - this duplicates code in AsadminMain, refactor it
            switch (rc) {
                case SUCCESS:
                    if (!programOpts.isTerse())
                        logger.log(Level.FINE, strings.get("CommandSuccessful", command));
                    break;

                case ERROR:
                case INVALID_COMMAND_ERROR:
                case CONNECTION_ERROR:
                default:
                    logger.log(Level.FINE, strings.get("CommandUnSuccessful", command));
                    break;
            }
            CLIUtil.writeCommandToDebugLog(programOpts.getCommandName() + "[multimode]", env, args, rc);
        }
        return rc;
    }

    private String[] getArgs(String line) throws ArgumentTokenizer.ArgumentException {
        
        List<String> args = new ArrayList<String>();
        ArgumentTokenizer t = new ArgumentTokenizer(line);
        while (t.hasMoreTokens()) {
            args.add(t.nextToken());
        }
        return args.toArray(new String[args.size()]);
    }
    
    private Completer getAllCommandsCompleter(){
        return new StringsCompleter(CLIUtil.getAllCommands(container, programOpts, env));
        
    }
}
