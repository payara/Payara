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
package com.sun.enterprise.admin.cli;

import com.sun.enterprise.module.ModulesRegistry;
import com.sun.enterprise.module.single.StaticModulesRegistry;
import com.sun.enterprise.universal.glassfish.ASenvPropertyReader;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import com.sun.enterprise.universal.io.SmartFile;
import com.sun.enterprise.util.JDK;
import com.sun.enterprise.util.SystemPropertyConstants;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.*;
import org.glassfish.api.admin.CommandException;
import org.glassfish.api.admin.CommandValidationException;
import org.glassfish.api.admin.InvalidCommandException;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.common.util.admin.AsadminInput;
import org.jvnet.hk2.component.BaseServiceLocator;
import org.jvnet.hk2.component.Habitat;

/**
 * The asadmin main program.
 */
public class AsadminMain {

    private String classPath;
    private String className;
    private String command;
    private ProgramOptions po;
    private Habitat habitat;
    private Environment env = new Environment();
    protected Logger logger;
    private final static int ERROR = 1;
    private final static int CONNECTION_ERROR = 2;
    private final static int INVALID_COMMAND_ERROR = 3;
    private final static int SUCCESS = 0;
    private final static int WARNING = 4;
    private final static String ADMIN_CLI_LOGGER =
            "com.sun.enterprise.admin.cli";
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
            if (ok(val)) {
                System.setProperty(prop, val);
            }
        }
    }

    /*
     * Skinning Methods
     *
     * The AsadminMain class can be "skinned" to present different CLI interface
     * for different products. Skinning is achieved by extending AsadminMain and
     * redefining the methods in this section.
     */
    /**
     * Get the class loader that is used to load local commands.
     *
     * @return a class loader used to load local commands
     */
    protected ClassLoader getExtensionClassLoader() {
        /*
         * Create a ClassLoader to load from all the jar files in the
         * lib/asadmin directory. This directory can contain extension jar files
         * with new local asadmin commands.
         */
        final ClassLoader ecl = AsadminMain.class.getClassLoader();
        
        File inst = new File(System.getProperty(
                SystemPropertyConstants.INSTALL_ROOT_PROPERTY));
        final File ext = new File(new File(inst, "lib"), "asadmin");
        logger.log(Level.FINER, "asadmin extension directory: {0}", ext);
        if (ext.isDirectory()) {
            return (ClassLoader) AccessController.doPrivileged(
                    new PrivilegedAction() {
                        @Override
                        public Object run() {
                            try {
                                return new DirectoryClassLoader(ext, ecl);
                            } catch (IOException ex) {
                                // any failure here is fatal
                                logger.info(strings.get("ExtDirFailed", ex));
                            }
                            return ecl;
                        }
                    });
        } else {
            logger.info(strings.get("ExtDirMissing", ext));
        }

        return ecl;
    }
    
    

    protected String getCommandName() {
        return "asadmin";
    }
    
    /*
     * End of skinning methods -------------------------------------------------
     */

    /**
     * A ConsoleHandler that prints all non-SEVERE messages to System.out and
     * all SEVERE messages to System.err.
     */
    private static class CLILoggerHandler extends ConsoleHandler {

        private CLILoggerHandler() {
            setFormatter(new CLILoggerFormatter());
        }

        @Override
        public void publish(java.util.logging.LogRecord logRecord) {
            if (!isLoggable(logRecord)) {
                return;
            }
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
        System.exit(new AsadminMain().doMain(args));
    }

    protected int doMain(String[] args) {
        int minor = JDK.getMinor();

        if (minor < 6) {
            System.err.println(strings.get("OldJdk", "" + minor));
            return ERROR;
        }

        boolean trace = env.trace();
        boolean debug = env.debug();

        /*
         * Use a logger associated with the top-most package that we expect all
         * admin commands to share. Only this logger and its children obey the
         * conventions that map terse=false to the INFO level and terse=true to
         * the FINE level.
         */
        logger = Logger.getLogger(ADMIN_CLI_LOGGER);
        if (trace) {
            logger.setLevel(Level.FINEST);
        } else if (debug) {
            logger.setLevel(Level.FINER);
        } else {
            logger.setLevel(Level.FINE);
        }
        logger.setUseParentHandlers(false);
        Handler h = new CLILoggerHandler();
        h.setLevel(logger.getLevel());
        logger.addHandler(h);

        // make sure the root logger uses our handler as well
        Logger rlogger = Logger.getLogger("");
        rlogger.setUseParentHandlers(false);
        for (Handler lh : rlogger.getHandlers()) {
            rlogger.removeHandler(lh);
        }
        rlogger.addHandler(h);

        if (debug) {
            System.setProperty(CLIConstants.WALL_CLOCK_START_PROP,
                    "" + System.currentTimeMillis());
            logger.log(Level.FINER, "CLASSPATH= {0}\nCommands: {1}",
                    new Object[]{System.getProperty("java.class.path"),
                        Arrays.toString(args)});
        }

        ClassLoader ecl = getExtensionClassLoader();
        /*
         * Set the thread's context class laoder so that everyone can load from
         * our extension directory.
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
        if (args.length == 0) {
            args = new String[]{"multimode"};
        }

        /*
         * Special case: -V argument is the same as "version".
         */
        if (args[0].equals("-V")) {
            args = new String[]{"version"};
        }

        command = args[0];
        int exitCode = executeCommand(args);

        switch (exitCode) {
            case SUCCESS:
                if (!po.isTerse()) {
                    logger.fine(
                            strings.get("CommandSuccessful", command));
                }
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
        CLIUtil.writeCommandToDebugLog(po, env, args, exitCode);
        return exitCode;
    }

    public int executeCommand(String[] argv) {
        CLICommand cmd = null;
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
            } else {
                po = new ProgramOptions(env);
            }
            po.toEnvironment(env);
            po.setClassPath(classPath);
            po.setClassName(className);
            po.setCommandName(getCommandName());
            if (argv.length == 0) {
                if (po.isHelp()) {
                    argv = new String[]{"help"};
                } else {
                    argv = new String[]{"multimode"};
                }
            }
            command = argv[0];

            habitat.addComponent(env);
            habitat.addComponent(po);
            cmd = CLICommand.getCommand(habitat, command);
            return cmd.execute(argv);
        } catch (CommandValidationException cve) {
            logger.severe(cve.getMessage());
            if (cmd == null) // error parsing program options
            {
                printUsage();
            } else {
                logger.severe(cmd.getUsage());
            }
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
            } else {
                logger.severe(ce.getMessage());
            }
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
     * Print usage message for asadmin command. XXX - should be derived from
     * ProgramOptions.
     */
    private void printUsage() {
        logger.severe(strings.get("Asadmin.usage", getCommandName()));
    }

    private static boolean ok(String s) {
        return s != null && s.length() > 0;
    }
}
