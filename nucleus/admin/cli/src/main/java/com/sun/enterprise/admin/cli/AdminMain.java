/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2017 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2018-2021] Payara Foundation and/or affiliates
package com.sun.enterprise.admin.cli;

import com.sun.enterprise.admin.remote.reader.ProprietaryReaderFactory;
import com.sun.enterprise.admin.remote.writer.ProprietaryWriterFactory;
import com.sun.enterprise.universal.glassfish.ASenvPropertyReader;
import com.sun.enterprise.universal.i18n.LocalStringsImpl;
import com.sun.enterprise.universal.io.SmartFile;
import com.sun.enterprise.util.JDK;
import com.sun.enterprise.util.SystemPropertyConstants;

import fish.payara.logging.jul.PayaraLogManagerInitializer;
import fish.payara.logging.jul.cfg.PayaraLogManagerConfiguration;
import fish.payara.logging.jul.tracing.PayaraLoggingTracer;

import java.io.File;
import java.io.PrintStream;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.time.Instant;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import org.glassfish.api.admin.CommandException;
import org.glassfish.api.admin.CommandValidationException;
import org.glassfish.api.admin.InvalidCommandException;
import org.glassfish.api.admin.ParameterMap;
import org.glassfish.common.util.admin.AsadminInput;

import static fish.payara.logging.jul.PayaraLogManager.getLogManager;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.FINER;
import static java.util.logging.Level.FINEST;

/**
 * The admin main program (asadmin).
 * <p>
 * If you need tracing of what it does, set AS_TRACE to true
 */
public class AdminMain {

    private static final Properties LOGGING_CFG;
    static {
        PayaraLoggingTracer.trace(AdminMain.class, "Preconfiguring logging for asadmin.");
        // The logging is explicitly configured in doMain method
        LOGGING_CFG = new Properties();
        LOGGING_CFG.setProperty("handlers", "fish.payara.logging.jul.handler.BlockingExternallyManagedLogHandler");
        if (!PayaraLogManagerInitializer.tryToSetAsDefault(LOGGING_CFG)) {
            throw new IllegalStateException("PayaraLogManager is not set as the default LogManager!");
        }
    }

    private String classPath;
    private String className;
    private String command;
    private ProgramOptions po;
    private CLIContainer cliContainer;
    private final Environment env = new Environment();
    private Logger logger;
    private final static int SUCCESS = 0;
    private final static int ERROR = 1;
    private final static int CONNECTION_ERROR = 2;
    private final static int INVALID_COMMAND_ERROR = 3;
    private final static int WARNING = 4;
    private final static String ADMIN_CLI_LOGGER = "com.sun.enterprise.admin.cli";

    private static final String[] SYS_PROPERTIES_TO_SET_FROM_ASENV = {
        SystemPropertyConstants.INSTALL_ROOT_PROPERTY,
        SystemPropertyConstants.CONFIG_ROOT_PROPERTY,
        SystemPropertyConstants.PRODUCT_ROOT_PROPERTY
    };
    private static final LocalStringsImpl strings = new LocalStringsImpl(AdminMain.class);

    static {
        Map<String, String> systemProps = new ASenvPropertyReader().getProps();
        for (String prop : SYS_PROPERTIES_TO_SET_FROM_ASENV) {
            String val = systemProps.get(prop);
            if (isNotEmpty(val)) {
                System.setProperty(prop, val);
            }
        }
    }

    /**
     * Get the class loader that is used to load local commands.
     *
     * @return a class loader used to load local commands
     */
    private ClassLoader getExtensionClassLoader(final Set<File> extensions) {
        final ClassLoader ecl = AdminMain.class.getClassLoader();
        if (extensions == null || extensions.isEmpty()) {
            return ecl;
        }
        final PrivilegedAction<ClassLoader> action = () -> {
            try {
                return new DirectoryClassLoader(extensions, ecl);
            } catch (final RuntimeException ex) {
                // any failure here is fatal
                logger.info(strings.get("ExtDirFailed", ex));
            }
            return ecl;
        };
        return AccessController.doPrivileged(action);
    }

    /** Get set of JAR files that is used to locate local commands (CLICommand).
     * Results can contain JAR files or directories where all JAR files are
     * used. It must return all JARs or directories
     * with acceptable CLICommands excluding admin-cli.jar.
     * Default implementation returns INSTALL_ROOT_PROPERTY/lib/asadmin
     *
     * @return set of JAR files or directories with JAR files
     */
    protected Set<File> getExtensions() {
        Set<File> result = new HashSet<>();
        final File inst = new File(System.getProperty(SystemPropertyConstants.INSTALL_ROOT_PROPERTY));
        final File ext = new File(new File(inst, "lib"), "asadmin");
        if (ext.exists() && ext.isDirectory()) {
            result.add(ext);
        } else {
            if (logger.isLoggable(FINER)) {
                logger.finer(strings.get("ExtDirMissing", ext));
            }
        }
        result.add(new File(new File(inst, "modules"), "admin-cli.jar"));
        return result;
    }

    protected String getCommandName() {
        return "nadmin";
    }

    /*
     * End of skinning methods -------------------------------------------------
     */

    /**
     * A ConsoleHandler that prints all non-SEVERE messages to System.out and
     * all SEVERE messages to System.err.
     */
    private static class CLILoggerHandler extends ConsoleHandler {

        private CLILoggerHandler(final Formatter formatter) {
            setFormatter(formatter);
        }

        @Override
        public void publish(java.util.logging.LogRecord logRecord) {
            if (!isLoggable(logRecord)) {
                return;
            }
            final PrintStream ps = (logRecord.getLevel() == Level.SEVERE) ? System.err : System.out;
            ps.print(getFormatter().format(logRecord));
            ps.flush();
        }
    }

    private static class CLILoggerFormatter extends SimpleFormatter {

        @Override
        public synchronized String format(LogRecord record) {
            // this formatter adds blank lines between records
            return formatMessage(record) + System.lineSeparator();
        }
    }

    public static void main(String[] args) {
        AdminMain adminMain = new AdminMain();
        int code = adminMain.doMain(args);
        System.exit(code);
    }

    protected int doMain(String[] args) {
        int minor = JDK.getMinor();
        int major = JDK.getMajor();
        // In case of JDK1 to JDK8 the major version would be 1 always.Starting from
        // JDK9 the major verion would be the real major version e.g in case
        // of JDK9 major version is 9.So in that case checking the major version only.
        if (major < 9 && minor < 6) {
            System.err.println(strings.get("OldJdk", major, minor));
            return ERROR;
        }

        System.setProperty(CLIConstants.WALL_CLOCK_START_PROP, Instant.now().toString());
        getLogManager().reconfigure(new PayaraLogManagerConfiguration(LOGGING_CFG), this::reconfigureLogging, null);

        // Set the thread's context class loader so that everyone can load from our extension directory.
        Set<File> extensions = getExtensions();
        ClassLoader ecl = getExtensionClassLoader(extensions);
        Thread.currentThread().setContextClassLoader(ecl);

         // It helps a little with CLI performance
        Thread thread = new Thread(() -> {
            ProprietaryReaderFactory.getReader(Class.class, "not/defined");
            ProprietaryWriterFactory.getWriter(Class.class);
        });
        thread.setDaemon(true);
        thread.start();

        cliContainer = new CLIContainer(ecl, extensions, logger);

        classPath = SmartFile.sanitizePaths(System.getProperty("java.class.path"));
        className = AdminMain.class.getName();

        if (logger.isLoggable(FINER)) {
            logger.log(FINER, "Classpath: {0}\nArguments: {1}", new Object[] {classPath, Arrays.toString(args)});
        }

        if (args.length == 0) {
            // Special case: no arguments is the same as "multimode".
            args = new String[] {"multimode"};
        } else if (args[0].equals("-V")) {
            // Special case: -V argument is the same as "version".
            args = new String[] {"version"};
        }

        command = args[0];
        int exitCode = executeCommand(args);

        switch (exitCode) {
            case SUCCESS:
                if (!po.isTerse() && logger.isLoggable(FINE)) {
                    String key = po.isDetachedCommand() ? "CommandSuccessfulStarted" : "CommandSuccessful";
                    logger.fine(strings.get(key, command));
                }
                break;

            case WARNING:
                if (logger.isLoggable(FINE)) {
                    logger.fine(strings.get("CommandSuccessfulWithWarnings", command));
                }
                exitCode = SUCCESS;
                break;

            case ERROR:
            case INVALID_COMMAND_ERROR:
            case CONNECTION_ERROR:
            default:
                if (logger.isLoggable(FINE)) {
                    logger.fine(strings.get("CommandUnSuccessful", command));
                }
                break;
        }
        CLIUtil.writeCommandToDebugLog(getCommandName(), env, args, exitCode);
        return exitCode;
    }


    public int executeCommand(String[] argv) {
        CLICommand cmd = null;
        try {
            // if the first argument is an option, we're using the new form
            if (argv.length > 0 && argv[0].startsWith("-")) {
                // Parse all the admin options, stopping at the first
                // non-option, which is the command name.
                Parser rcp = new Parser(argv, 0, ProgramOptions.getValidOptions(), false);
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

            cliContainer.setEnvironment(env);
            cliContainer.setProgramOptions(po);
            cmd = CLICommand.getCommand(cliContainer, command);
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
                po.setEcho(false);
                CLIUtil.displayClosestMatch(command,
                        CLIUtil.getAllCommands(cliContainer, po, env),
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
                            CLIUtil.getLocalCommands(cliContainer),
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

    private void reconfigureLogging() {
        PayaraLoggingTracer.trace(AdminMain.class, "Configuring logging for asadmin.");
        boolean trace = env.trace();
        boolean debug = env.debug();

        // Use a logger associated with the top-most package that we expect all
        // admin commands to share. Only this logger and its children obey the
        // conventions that map terse=false to the INFO level and terse=true to
        // the FINE level.
        logger = Logger.getLogger(ADMIN_CLI_LOGGER);
        if (trace) {
            logger.setLevel(FINEST);
        } else if (debug) {
            logger.setLevel(FINER);
        } else {
            logger.setLevel(FINE);
        }
        logger.setUseParentHandlers(false);
        Formatter formatter = env.getLogFormatter();
        Handler cliHandler = new CLILoggerHandler(formatter == null ? new CLILoggerFormatter() : formatter);
        cliHandler.setLevel(logger.getLevel());
        logger.addHandler(cliHandler);

        // make sure the root logger uses our handler as well
        Logger rootLogger = Logger.getLogger("");
        rootLogger.setUseParentHandlers(false);
        if (trace) {
            rootLogger.setLevel(logger.getLevel());
        }
        for (Handler handler : rootLogger.getHandlers()) {
            rootLogger.removeHandler(handler);
            handler.close();
        }
        rootLogger.addHandler(cliHandler);
    }

    private static void readAndMergeOptionsFromAuxInput(final ProgramOptions progOpts) {
        final String auxInput = progOpts.getAuxInput();
        if (auxInput == null || auxInput.length() == 0) {
            return;
        }
        // We will place the options passed via the aux. input on the command
        // line and we do not want to repeat the read from stdin again, so
        // remove the aux input setting.
        progOpts.setAuxInput(null);
        final ParameterMap newParamMap = new ParameterMap();
        try {
            final AsadminInput.InputReader reader = AsadminInput.reader(auxInput);
            final Properties newOptions = reader.settings().get("option");
            for (String propName : newOptions.stringPropertyNames()) {
                newParamMap.add(propName, newOptions.getProperty(propName));
            }
            progOpts.updateOptions(newParamMap);
        } catch (Exception ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    /**
     * Print usage message for the admin command. XXX - should be derived from
     * ProgramOptions.
     */
    private void printUsage() {
        logger.severe(strings.get("Usage.full", getCommandName()));
    }

    private static boolean isNotEmpty(String s) {
        return s != null && !s.isEmpty();
    }
}
