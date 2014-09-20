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

package org.glassfish.osgi.cli.interactive;

import com.sun.enterprise.admin.cli.ArgumentTokenizer;
import com.sun.enterprise.admin.cli.CLICommand;
import com.sun.enterprise.admin.cli.CLIUtil;
import com.sun.enterprise.admin.cli.Environment;
import com.sun.enterprise.admin.cli.MultimodeCommand;
import com.sun.enterprise.admin.cli.ProgramOptions;
import com.sun.enterprise.admin.cli.remote.RemoteCLICommand;
import com.sun.enterprise.admin.util.CommandModelData;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import jline.console.ConsoleReader;
import jline.console.completer.Completer;
import jline.console.completer.NullCompleter;
import jline.console.completer.StringsCompleter;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.CommandException;
import org.glassfish.api.admin.CommandModel.ParamModel;
import org.glassfish.api.admin.CommandValidationException;
import org.glassfish.api.admin.InvalidCommandException;
import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.DynamicConfigurationService;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.jvnet.hk2.annotations.Service;

/**
 * A simple local asadmin sub-command to establish an interactive osgi shell.
 *
 * This class is forked from com.sun.enterprise.admin.cli.MultimodeCommand
 *
 * Original code authors:
 *      केदार(km@dev.java.net)
 *      Bill Shannon
 * @author ancoron
 */
@Service(name = "osgi-shell")
@I18n("osgi-shell")
@PerLookup
public class LocalOSGiShellCommand extends CLICommand {

    protected static final String REMOTE_COMMAND = "osgi";
    protected static final String SESSIONID_OPTION = "--session-id";
    protected static final String SESSION_OPTION = "--session";
    protected static final String SESSION_OPTION_EXECUTE = "execute";
    protected static final String SESSION_OPTION_START = "new";
    protected static final String SESSION_OPTION_STOP = "stop";

    @Inject
    private ServiceLocator locator;

    @Param(name = "instance", optional = true)
    private String instance;

    @Param(optional = true, shortName = "f")
    private File file;

    @Param(name = "printprompt", optional = true)
    private Boolean printPromptOpt;

    private boolean printPrompt;

    @Param(optional = true)
    private String encoding;

    private boolean echo;       // saved echo flag
    private RemoteCLICommand cmd;     // the remote sub-command
    private String shellType;

    // re-using existing strings...
    private static final LocalStringsImpl strings =
            new LocalStringsImpl(MultimodeCommand.class);

    protected String[] enhanceForTarget(String[] args) {
        if(instance != null) {
            String[] targetArgs = new String[args.length + 2];
            targetArgs[1] = "--instance";
            targetArgs[2] = instance;
            System.arraycopy(args, 0, targetArgs, 0, 1);
            System.arraycopy(args, 1, targetArgs, 3, args.length - 1);
            args = targetArgs;
        }
        return args;
    }

    protected String[] prepareArguments(String sessionId, String[] args) {
        if(sessionId != null) {
            // attach command to remote session...
            String[] sessionArgs = new String[args.length + 5];
            sessionArgs[0] = REMOTE_COMMAND;
            sessionArgs[1] = SESSION_OPTION;
            sessionArgs[2] = SESSION_OPTION_EXECUTE;
            sessionArgs[3] = SESSIONID_OPTION;
            sessionArgs[4] = sessionId;
            System.arraycopy(args, 0, sessionArgs, 5, args.length);
            args = sessionArgs;
        } else {
            String[] osgiArgs = new String[args.length + 1];
            osgiArgs[0] = REMOTE_COMMAND;
            System.arraycopy(args, 0, osgiArgs, 1, args.length);
            args = osgiArgs;
        }
        return args;
    }

    protected String startSession() throws CommandException {
        String sessionId = null;
        if("gogo".equals(shellType)) {
            // start a remote session...
            String[] args = {REMOTE_COMMAND, SESSION_OPTION,
                SESSION_OPTION_START};
            args = enhanceForTarget(args);
            sessionId = cmd.executeAndReturnOutput(args).trim();
        }
        return sessionId;
    }

    protected int stopSession(String sessionId) throws CommandException {
        int rc = 0;
        if(sessionId != null) {
            // stop the remote session...
            String[] args = {REMOTE_COMMAND, SESSION_OPTION,
                SESSION_OPTION_STOP, SESSIONID_OPTION, sessionId};
            args = enhanceForTarget(args);
            rc = cmd.execute(args);
        }
        return rc;
    }

    /**
     * The validate method validates that the type and quantity of
     * parameters and operands matches the requirements for this
     * command.  The validate method supplies missing options from
     * the environment.
     */
    @Override
    protected void validate()
            throws CommandException, CommandValidationException {
        if (printPromptOpt != null) {
            printPrompt = printPromptOpt.booleanValue();
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
     * In the usage message modify the --printprompt option to have a
     * default based on the --interactive option.
     */
    @Override
    protected Collection<ParamModel> usageOptions() {
        Collection<ParamModel> opts = commandModel.getParameters();
        Set<ParamModel> uopts = new LinkedHashSet<ParamModel>();
        ParamModel p = new CommandModelData.ParamModelData("printprompt",
                boolean.class, true,
                Boolean.toString(programOpts.isInteractive()));
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
    protected int executeCommand()
            throws CommandException, CommandValidationException {
        ConsoleReader reader = null;

        if(cmd == null) {
            throw new CommandException("Remote command 'osgi' is not available.");
        }
        
        programOpts.setEcho(echo);       // restore echo flag, saved in validate
        try {
            if (encoding != null) {
                // see Configuration.getEncoding()...
                System.setProperty("input.encoding", encoding);
            }

            String[] args = new String[] {REMOTE_COMMAND,
                "asadmin-osgi-shell"};
            args = enhanceForTarget(args);
            shellType = cmd.executeAndReturnOutput(args).trim();

            if (file == null) {
                System.out.println(strings.get("multimodeIntro"));
                reader = new ConsoleReader(REMOTE_COMMAND,
                        new FileInputStream(FileDescriptor.in), System.out,
                        null);
            } else {
                printPrompt = false;
                if (!file.canRead()) {
                    throw new CommandException("File: " + file
                            + " can not be read");
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

                reader = new ConsoleReader(REMOTE_COMMAND,
                        new FileInputStream(file), out,
                        null);
            }

            reader.setBellEnabled(false);
            reader.addCompleter(getCommandCompleter());

            return executeCommands(reader);
        } catch (IOException e) {
            throw new CommandException(e);
        }
    }

    /**
     * Get the command completion.
     * 
     * TODO: make this non-static!
     * TODO: ask remote for dynamically added commands
     * 
     * @return The command completer
     */
    private Completer getCommandCompleter() {
        if("gogo".equals(shellType)) {
            return new StringsCompleter(
                    "bundlelevel",
                    "cd",
                    "frameworklevel",
                    "headers",
                    "help",
                    "inspect",
                    "install",
                    "lb",
                    "log",
                    "ls",
                    "refresh",
                    "resolve",
                    "start",
                    "stop",
                    "uninstall",
                    "update",
                    "which",
                    "cat",
                    "each",
                    "echo",
                    "format",
                    "getopt",
                    "gosh",
                    "grep",
                    "not",
                    "set",
                    "sh",
                    "source",
                    "tac",
                    "telnetd",
                    "type",
                    "until",
                    "deploy",
                    "info",
                    "javadoc",
                    "list",
                    "repos",
                    "source"
                    );
        } else if("felix".equals(shellType)) {
            return new StringsCompleter(
                    "exit",
                    "quit",
                    "help",
                    "bundlelevel",
                    "cd",
                    "find",
                    "headers",
                    "inspect",
                    "install",
                    "log",
                    "ps",
                    "refresh",
                    "resolve",
                    "scr",
                    "shutdown",
                    "start",
                    "startlevel",
                    "stop",
                    "sysprop",
                    "uninstall",
                    "update",
                    "version"
                    );
        }

        return new NullCompleter();
    }
    

    /**
     * Read commands from the specified BufferedReader
     * and execute them.  If printPrompt is set, prompt first.
     *
     * @return the exit code of the last command executed
     */
    private int executeCommands(ConsoleReader reader)
            throws CommandException, CommandValidationException, IOException {
        String line = null;
        int rc = 0;

        /*
         * Any program options we start with are copied to the environment
         * to serve as defaults for commands we run, and then we give each
         * command an empty program options.
         */
        programOpts.toEnvironment(env);

        String sessionId = startSession();

        try {
            for (;;) {
                if (printPrompt) {
                    line = reader.readLine(shellType + "$ ");
                } else {
                    line = reader.readLine();
                }

                if (line == null) {
                    if (printPrompt) {
                        System.out.println();
                    }
                    break;
                }

                if (line.trim().startsWith("#")) // ignore comment lines
                {
                    continue;
                }

                String[] args = null;
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
                if (command.trim().length() == 0) {
                    continue;
                }

                // handle built-in exit and quit commands
                // XXX - care about their arguments?
                if (command.equals("exit") || command.equals("quit")) {
                    break;
                }

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
                    // remove the old one and replace it
                    atomicReplace(locator, po);

                    args = prepareArguments(sessionId, args);

                    args = enhanceForTarget(args);

                    String output = cmd.executeAndReturnOutput(args).trim();
                    if(output != null && output.length() > 0) {
                        logger.info(output);
                    }
                } catch (CommandValidationException cve) {
                    logger.severe(cve.getMessage());
                    logger.severe(cmd.getUsage());
                    rc = ERROR;
                } catch (InvalidCommandException ice) {
                    // find closest match with local or remote commands
                    logger.severe(ice.getMessage());
                } catch (CommandException ce) {
                    logger.severe(ce.getMessage());
                    rc = ERROR;
                } finally {
                    // restore the original program options
                    // XXX - is this necessary?
                    atomicReplace(locator, programOpts);
                }

                CLIUtil.writeCommandToDebugLog(name, env, args, rc);
            }
        } finally {
            // what if something breaks on the wire?
            rc = stopSession(sessionId);
        }
        return rc;
    }
    
    private static void atomicReplace(ServiceLocator locator, ProgramOptions options) {
        DynamicConfigurationService dcs = locator.getService(DynamicConfigurationService.class);
        DynamicConfiguration config = dcs.createDynamicConfiguration();
        
        config.addUnbindFilter(BuilderHelper.createContractFilter(ProgramOptions.class.getName()));
        ActiveDescriptor<ProgramOptions> desc = BuilderHelper.createConstantDescriptor(
                options, null, ProgramOptions.class);
        config.addActiveDescriptor(desc);
        
        config.commit();
    }

    private String[] getArgs(String line)
            throws ArgumentTokenizer.ArgumentException {
        List<String> args = new ArrayList<String>();
        ArgumentTokenizer t = new ArgumentTokenizer(line);
        while (t.hasMoreTokens()) {
            args.add(t.nextToken());
        }
        return args.toArray(new String[args.size()]);
    }

    @Override
    public void postConstruct() {
        super.postConstruct();
        try {
            cmd = new RemoteCLICommand(REMOTE_COMMAND,
                locator.<ProgramOptions>getService(ProgramOptions.class),
                locator.<Environment>getService(Environment.class));
        } catch (CommandException ex) {
            // ignore - will be handled by execute()
        }
    }
}
