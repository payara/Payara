/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.admin.cli;

import java.io.*;
import java.net.URISyntaxException;
import java.text.*;
import java.util.*;
import java.util.logging.*;
import org.glassfish.common.util.admin.AsadminInput;

import org.jvnet.hk2.annotations.Contract;
import org.jvnet.hk2.component.*;
import org.glassfish.api.admin.*;
import com.sun.enterprise.module.*;
import com.sun.enterprise.module.single.StaticModulesRegistry;

import com.sun.enterprise.admin.cli.remote.*;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import com.sun.enterprise.universal.io.SmartFile;
import com.sun.enterprise.universal.glassfish.ASenvPropertyReader;
import com.sun.enterprise.util.JDK;
import com.sun.enterprise.util.SystemPropertyConstants;

/**
 * The asadmin main program.
 */
public class AsadminMain {

    private       static String classPath;
    private       static String className;
    private       static String command;
    private       static ProgramOptions po;
    private       static Habitat habitat;
    private       static Logger logger;

    private final static int ERROR = 1;
    private final static int CONNECTION_ERROR = 2;
    private final static int INVALID_COMMAND_ERROR = 3;
    private final static int SUCCESS = 0;
    private final static int WARNING = 4;

    // XXX - move this to LogDomains?
    private final static String ADMIN_CLI_LOGGER =
                                    "com.sun.enterprise.admin.cli";

    private final static String DEBUG_FLAG = "Debug";
    private final static String ENV_DEBUG_FLAG = "AS_DEBUG";

    private static final String[] copyProps = {
        SystemPropertyConstants.INSTALL_ROOT_PROPERTY,
        SystemPropertyConstants.CONFIG_ROOT_PROPERTY,
        SystemPropertyConstants.PRODUCT_ROOT_PROPERTY
    };

    private static final LocalStringsImpl strings =
                                new LocalStringsImpl(AsadminMain.class);

    static {
        Map<String, String> systemProps = new ASenvPropertyReader().getProps();
        for (String prop : copyProps) {
            String val = systemProps.get(prop);
            if (ok(val))
                System.setProperty(prop, val);
        }
    }

    /**
     * A ConsoleHandler that prints all non-SEVERE messages to System.out
     * and all SEVERE messages to System.err.
     */
    private static class CLILoggerHandler extends ConsoleHandler {
        
        private CLILoggerHandler() {
            setFormatter(new CLILoggerFormatter());
        }
        
        @Override
        public void publish(java.util.logging.LogRecord logRecord) {
            if (!isLoggable(logRecord))
                return;
            final PrintStream ps = (logRecord.getLevel() == Level.SEVERE) ? System.err : System.out;
            ps.println(getFormatter().format(logRecord));
        }
    }
    
    private static class CLILoggerFormatter extends SimpleFormatter {

        @Override
        public synchronized String format(LogRecord record) {
            return formatMessage(record);
        }
    }

    public static void main(String[] args) {
        int minor = JDK.getMinor();

        if (minor < 6) {
            System.err.println(strings.get("OldJdk", "" + minor));
            System.exit(ERROR);
        }

        // bnevins 4-18-08 A quickly added trace. should clean up later.
        // TODO TODO TODO TODO

        // System Prop just needs to exist
        // Env Var. needs to be set to "true"
        String sys = System.getProperty(DEBUG_FLAG);
        boolean env = Boolean.parseBoolean(System.getenv(ENV_DEBUG_FLAG));
        boolean trace = Boolean.parseBoolean(System.getenv("AS_TRACE"));
        boolean debug = sys != null || env;

        /*
         * Use a logger associated with the top-most package that we
         * expect all admin commands to share.  Only this logger and
         * its children obey the conventions that map terse=false to
         * the INFO level and terse=true to the FINE level.
         */
        logger = Logger.getLogger(ADMIN_CLI_LOGGER);
        if (trace)
            logger.setLevel(Level.FINEST);
        else if (debug)
            logger.setLevel(Level.FINER);
        else {
            logger.setLevel(Level.FINE);
        }
        logger.setUseParentHandlers(false);
        Handler h = new CLILoggerHandler();
        h.setLevel(logger.getLevel());
        logger.addHandler(h);

        // make sure the root logger uses our handler as well
        Logger rlogger = Logger.getLogger("");
        rlogger.setUseParentHandlers(false);
        for (Handler lh : rlogger.getHandlers())
            rlogger.removeHandler(lh);
        rlogger.addHandler(h);

        if (CLIConstants.debugMode) {
            System.setProperty(CLIConstants.WALL_CLOCK_START_PROP,
                "" + System.currentTimeMillis());
            logger.finer("CLASSPATH= " +
                    System.getProperty("java.class.path") +
                    "\nCommands: " + Arrays.toString(args));
        }

        /*
         * Create a ClassLoader to load from all the jar files in the
         * lib/asadmin directory.  This directory can contain extension
         * jar files with new local asadmin commands.
         */
        ClassLoader ecl = AsadminMain.class.getClassLoader();
        try {
            File inst = new File(System.getProperty(
                                SystemPropertyConstants.INSTALL_ROOT_PROPERTY));
            File ext = new File(new File(inst, "lib"), "asadmin");
            logger.finer(
                                    "asadmin extension directory: " + ext);
            if (ext.isDirectory())
                ecl = new DirectoryClassLoader(ext, ecl);
            else
                logger.info(
                                            strings.get("ExtDirMissing", ext));
        } catch (IOException ex) {
            // any failure here is fatal
            logger.info(
                                    strings.get("ExtDirFailed", ex));
            System.exit(1);
        }

        /*
         * Set the thread's context class laoder so that everyone can
         * load from our extension directory.
         */
        Thread.currentThread().setContextClassLoader(ecl);

        /*
         * Create a habitat that can load from the extension directory.
         */
        ModulesRegistry registry = new StaticModulesRegistry(ecl);
        habitat = registry.createHabitat("default");

        classPath =
            SmartFile.sanitizePaths(System.getProperty("java.class.path"));
        className = AsadminMain.class.getName();

        /*
         * Special case: no arguments is the same as "multimode".
         */
        if (args.length == 0)
             args = new String[] { "multimode" };

        /*
         * Special case: -V argument is the same as "version".
         */
        if (args[0].equals("-V"))
             args = new String[] { "version" };

        command = args[0];
        int exitCode = executeCommand(args);

        switch (exitCode) {
        case SUCCESS:
            if (!po.isTerse())
                logger.fine(
                    strings.get("CommandSuccessful", command));
            break;

        case WARNING:
            logger.fine(
                strings.get("CommandSuccessfulWithWarnings", command));
            exitCode = SUCCESS;
            break;

        case ERROR:
        case INVALID_COMMAND_ERROR:
        case CONNECTION_ERROR:
        default:
            logger.fine(
                strings.get("CommandUnSuccessful", command));
            break;
        }
        CLIUtil.writeCommandToDebugLog(args, exitCode);
        System.exit(exitCode);
    }

    public static int executeCommand(String[] argv) {
        CLICommand cmd = null;
        Environment env = new Environment();
        try {

            // if the first argument is an option, we're using the new form
            if (argv.length > 0 && argv[0].startsWith("-")) {
                /*
                 * Parse all the asadmin options, stopping at the first
                 * non-option, which is the command name.
                 */
                Parser rcp = new Parser(argv, 0,
                                ProgramOptions.getValidOptions(), false);
                ParameterMap params = rcp.getOptions();
                po = new ProgramOptions(params, env);
                readAndMergeOptionsFromAuxInput(po);
                List<String> operands = rcp.getOperands();
                argv = operands.toArray(new String[operands.size()]);
            } else
                po = new ProgramOptions(env);
            po.toEnvironment(env);
            po.setClassPath(classPath);
            po.setClassName(className);
            if (argv.length == 0) {
                if (po.isHelp())
                    argv = new String[] { "help" };
                else
                    argv = new String[] { "multimode" };
            }
            command = argv[0];

            habitat.addComponent(env);
            habitat.addComponent(po);
            cmd = CLICommand.getCommand(habitat, command);
            return cmd.execute(argv);
        } catch (CommandValidationException cve) {
            logger.severe(cve.getMessage());
            if (cmd == null)    // error parsing program options
                printUsage();
            else
                logger.severe(cmd.getUsage());
            return ERROR;
        } catch (InvalidCommandException ice) {
            // find closest match with local or remote commands
            logger.severe(ice.getMessage());
            try {
                CLIUtil.displayClosestMatch(command,
                    CLIUtil.getAllCommands(habitat, po, env),
                    strings.get("ClosestMatchedLocalAndRemoteCommands"), logger);
            } catch (InvalidCommandException e) {
                // not a big deal if we cannot help
            }
            return ERROR;
        } catch (CommandException ce) {
            if (ce.getCause() instanceof java.net.ConnectException) {
                // find closest match with local commands
                logger.severe(ce.getMessage());
                try {
                    CLIUtil.displayClosestMatch(command,
                        CLIUtil.getLocalCommands(habitat),
                        strings.get("ClosestMatchedLocalCommands"), logger);
                } catch (InvalidCommandException e) {
                    logger.info(
                            strings.get("InvalidRemoteCommand", command));
                }
            } else
                logger.severe(ce.getMessage());
            return ERROR;
        }
    }

    private static void readAndMergeOptionsFromAuxInput(final ProgramOptions progOpts) 
            throws CommandException {
        final String auxInput = progOpts.getAuxInput();
        if (auxInput == null || auxInput.length() == 0) {
            return;
        }
        final ParameterMap newParamMap = new ParameterMap();
        /*
         * We will place the options passed via the aux. input on the command
         * line and we do not want to repeat the read from stdin again, so
         * remove the aux input setting.
         */
        progOpts.setAuxInput(null);
        try {
            final AsadminInput.InputReader reader = AsadminInput.reader(auxInput);
            final Properties newOptions = reader.settings().get("option");
            for (String propName : newOptions.stringPropertyNames()) {
                newParamMap.add(propName, newOptions.getProperty(propName));
            }
            progOpts.updateOptions(newParamMap);
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * Print usage message for asadmin command.
     * XXX - should be derived from ProgramOptions.
     */
    private static void printUsage() {
        logger.severe(strings.get("Asadmin.usage"));
    }

    private static boolean ok(String s) {
        return s!= null && s.length() > 0;
    }

    /** Turned off for now -- it takes ~200 msec on a laptop!
    private final static boolean foundClass(String s) {
        try {
            Class.forName(s);
            return true;
        } catch (Throwable t) {
            System.out.println("Can not find class: " + s);
            return false;
        }
    }

    private final static String[] requiredClassnames = {
        // one from launcher jar        
        "com.sun.enterprise.admin.launcher.GFLauncher",
        // one from universal jar
        "com.sun.enterprise.universal.xml.MiniXmlParser",
        // one from glassfish bootstrap jar
        "com.sun.enterprise.glassfish.bootstrap.ASMain",
        // one from stax-api
        "javax.xml.stream.XMLInputFactory",
        // one from server-mgmt
        "com.sun.enterprise.admin.servermgmt.RepositoryException",
        // one from common-utils
        "com.sun.enterprise.util.net.NetUtils",
        // one from admin/util
        "com.sun.enterprise.admin.util.TokenValueSet",
        // here's one that server-mgmt is dependent on
        "com.sun.enterprise.security.auth.realm.file.FileRealm",
        // dol
        "com.sun.enterprise.deployment.PrincipalImpl",
        // kernel
        //"com.sun.appserv.server.util.Version",
    };

    static {
        // check RIGHT NOW to make sure all the classes we need are
        // available
        long start = System.currentTimeMillis();
        boolean gotError = false;
        for (String s : requiredClassnames) {
            if (!foundClass(s))
                gotError = true;
        }
        // final test -- see if sjsxp is available
        try {
            javax.xml.stream.XMLInputFactory.newInstance().getXMLReporter();
        } catch(Throwable t) {
            gotError = true;
            System.out.println("Can't access STAX classes");
        }
        if (gotError) {
            // messages already sent to stdout...
            System.exit(1);
        }
        long stop = System.currentTimeMillis();
        System.out.println("Time to pre-load classes = " + (stop-start) + " msec");
    }
     */
}
