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
 *
 */
// Portions Copyright [2017-2020] Payara Foundation and/or affilates

package com.sun.enterprise.glassfish.bootstrap;

import fish.payara.boot.runtime.BootCommands;
import fish.payara.logging.jul.PayaraLogManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.glassfish.embeddable.BootstrapProperties;
import org.glassfish.embeddable.CommandResult;
import org.glassfish.embeddable.CommandRunner;
import org.glassfish.embeddable.Deployer;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishException;
import org.glassfish.embeddable.GlassFishProperties;
import org.glassfish.embeddable.GlassFishRuntime;

import static com.sun.enterprise.module.bootstrap.ArgumentManager.argsToMap;
import static java.util.logging.Level.SEVERE;

/**
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class GlassFishMain {

    private static final Pattern COMMAND_PATTERN = Pattern.compile("([^\"']\\S*|\".*?\"|'.*?')\\s*");
    private static final String[] COMMAND_TYPE = new String[0];
    // logging system may override original output streams.
    private static final PrintStream STDOUT = System.out;
    private static final PrintStream STDERR = System.err;

    public static void main(final String args[]) throws Exception {
        // The PayaraLogManager must be set before the first usage of any JUL component,
        // it would be replaced by another implementation otherwise.
        final ClassLoader jdkExtensionCL = ClassLoader.getSystemClassLoader().getParent();
        final File installRoot = MainHelper.findInstallRoot();
        final GlassfishMainClassLoader gfMainCL = new GlassfishMainClassLoader(installRoot, jdkExtensionCL);
        final Class<?> plm = gfMainCL.loadClass("fish.payara.logging.jul.PayaraLogManagerInitializer") ;
        final Properties loggingCfg = createDefaultLoggingProperties();
        plm.getMethod("tryToSetAsDefault", Properties.class).invoke(plm, loggingCfg);

        MainHelper.checkJdkVersion();

        final Properties argsAsProps = argsToMap(args);
        final String platform = MainHelper.whichPlatform();
        STDOUT.println("Launching Payara Server on " + platform + " platform");

        final File instanceRoot = MainHelper.findInstanceRoot(installRoot, argsAsProps);
        final Properties startupCtx = MainHelper.buildStartupContext(platform, installRoot, instanceRoot, args);
        final ClassLoader launcherCL = MainHelper.createLauncherCL(startupCtx, gfMainCL);
        final Class<?> launcherClass = launcherCL.loadClass(GlassFishMain.Launcher.class.getName());
        final Object launcher = launcherClass.newInstance();
        final Method method = launcherClass.getMethod("launch", Properties.class);

        // launcherCL is used only to load the RuntimeBuilder service.
        // on all other places is used classloader which loaded the GlassfishRuntime class
        // -> it must not be loaded by any parent classloader, it's children would be ignored.
        method.invoke(launcher, startupCtx);
    }


    private static Properties createDefaultLoggingProperties() {
        final Properties cfg = new Properties();
        // ConsoleHandler processes output to STDOUT
        // PayaraLogHandler collects log records until it will be completely configured with
        // an instance configuration so it will have complete information about filtering
        // and formatting those records
        cfg.setProperty("handlers", "java.util.logging.ConsoleHandler,fish.payara.logging.jul.PayaraLogHandler");
        // useful to track any startup race conditions etc. Logging is always in game.
        cfg.setProperty("fish.payara.logging.jul.tracingEnabled", "false");
        // warning: there is no other way to force HK2 loggers or any other loggers, created
        // via new Logger(..) constructor to wait until logging would be completely configured.
        // These instances make decisions about log loggability immediately.
        // So in case you need to trace what server does between startup
        // and completing the configuration, set this.
        cfg.setProperty("systemRootLoggerLevel", Level.INFO.getName());
        cfg.setProperty(".level", Level.INFO.getName());

        // also note that debugging is not possible until the debug port is open.
        return cfg;
    }


    // must be public to be accessible via reflection
    public static final class Launcher {
        private volatile GlassFish gf;
        private volatile GlassFishRuntime gfRuntime;

        public void launch(Properties ctx) throws Exception {
            addShutdownHook();
            final ClassLoader launcherCL = getClass().getClassLoader();
            gfRuntime = GlassFishRuntime.bootstrap(new BootstrapProperties(ctx), launcherCL);
            gf = gfRuntime.newGlassFish(new GlassFishProperties(ctx));
            doBootCommands(ctx.getProperty("-prebootcommandfile"));
            if (Boolean.valueOf(Util.getPropertyOrSystemProperty(ctx, "GlassFish_Interactive", "false"))) {
                startConsole();
            } else {
                gf.start();
            }
            doBootCommands(ctx.getProperty("-postbootcommandfile"));
        }

        private void startConsole() throws IOException {
            String command;
            final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            while ((command = readCommand(reader)) != null) {
                try {
                    STDOUT.println("command = " + command);
                    if ("start".equalsIgnoreCase(command)) {
                        if (gf.getStatus() != GlassFish.Status.STARTED || gf.getStatus() == GlassFish.Status.STOPPING
                            || gf.getStatus() == GlassFish.Status.STARTING) {
                            gf.start();
                        } else {
                            STDOUT.println("Already started or stopping or starting");
                        }
                    } else if ("stop".equalsIgnoreCase(command)) {
                        if (gf.getStatus() != GlassFish.Status.STARTED) {
                            STDOUT.println("GlassFish is not started yet. Please execute start first.");
                            continue;
                        }
                        gf.stop();
                    } else if (command.startsWith("deploy")) {
                        if (gf.getStatus() != GlassFish.Status.STARTED) {
                            STDOUT.println("GlassFish is not started yet. Please execute start first.");
                            continue;
                        }
                        Deployer deployer = gf.getService(Deployer.class, null);
                        String[] tokens = command.split("\\s");
                        if (tokens.length < 2) {
                            STDOUT.println("Syntax: deploy <options> file");
                            continue;
                        }
                        final URI uri = URI.create(tokens[tokens.length -1]);
                        String[] params = Arrays.copyOfRange(tokens, 1, tokens.length-1);
                        String name = deployer.deploy(uri, params);
                        STDOUT.println("Deployed = " + name);
                    } else if (command.startsWith("undeploy")) {
                        if (gf.getStatus() != GlassFish.Status.STARTED) {
                            STDOUT.println("GlassFish is not started yet. Please execute start first.");
                            continue;
                        }
                        Deployer deployer = gf.getService(Deployer.class, null);
                        String name = command.substring(command.indexOf(' ')).trim();
                        deployer.undeploy(name);
                        STDOUT.println("Undeployed = " + name);
                    } else if ("quit".equalsIgnoreCase(command)) {
                        System.exit(0);
                    } else {
                        if (gf.getStatus() != GlassFish.Status.STARTED) {
                            STDOUT.println("GlassFish is not started yet. Please execute start first.");
                            continue;
                        }
                        CommandRunner cmdRunner = gf.getCommandRunner();
                        runCommand(cmdRunner, command);
                    }
                } catch (Exception e) {
                    e.printStackTrace(STDERR);
                }
            }

        }

        private String readCommand(BufferedReader reader) throws IOException {
            prompt();
            String command = null;
            while((command = reader.readLine()) != null && command.isEmpty()) {
                // loop until a non empty command or Ctrl-D is inputted.
            }
            return command;
        }

        private void prompt() {
            STDOUT.print( //
                "Enter any of the following commands: start, stop, quit, deploy <path to file>, undeploy <name of app>\n"
                    + "glassfish$ ");
            STDOUT.flush();
        }

        private void addShutdownHook() {
            Runtime.getRuntime().addShutdownHook(new Thread("GlassFish Shutdown Hook") {
                @Override
                public void run() {
                    try {
                        if (gfRuntime != null) {
                            gfRuntime.shutdown();
                        }
                    } catch (Exception ex) {
                        STDERR.println("Error stopping framework: " + ex.getLocalizedMessage());
                        ex.printStackTrace(STDERR);
                    }
                    if (PayaraLogManager.isPayaraLogManager()) {
                        PayaraLogManager.getLogManager().closeAllExternallyManagedLogHandlers();
                    } else {
                        LogManager.getLogManager().reset();
                    }
                }
            });

        }

        /**
         * Runs a command read from a string
         *
         * @param cmdRunner
         * @param line
         * @throws GlassFishException
         */
        private void runCommand(CommandRunner cmdRunner, String line) throws GlassFishException {

            line = cleanCommand(line);
            if(line == null) {
                return;
            }

            STDOUT.println("Running command: " + line);
            List<String> tokens = parseCommand(line);
            CommandResult result = cmdRunner.run(
                    tokens.get(0),
                    tokens.subList(1, tokens.size()).toArray(COMMAND_TYPE)
            );
            STDOUT.println(result.getOutput());
            if(result.getFailureCause() != null) {
                result.getFailureCause().printStackTrace(STDERR);
            }
        }

        /**
         * Cleans a command read from a string (removes comments)
         */
        private String cleanCommand(String line) {
            if(line == null) {
                return null;
            }
            // Remove comments at the start of lines
            line = line.replaceAll("^\\s*#.*", "");
            // Remove comments with whitespace before them. This allows for hashtags used in commands to be ignored.
            line = line.replaceAll("\\s#.*", "");
            if (line.isEmpty() || line.replaceAll("\\s", "").isEmpty()) {
                return null;
            }
            return line;
        }

        /**
         * Parse a command from a string
         */
        private List<String> parseCommand(final String line) {
            final List<String> tokens = new ArrayList<>();
            final Matcher matcher = COMMAND_PATTERN.matcher(line);
            while (matcher.find()) {
                String token = matcher.group(1);
                if ((token.startsWith("\"") && token.endsWith("\""))
                        || token.startsWith("'") && token.endsWith("'")) {
                    token = token.substring(1, token.length() - 1);
                }
                tokens.add(token);
            }
            return tokens;
        }

        /**
         * Runs a series of commands from a file
         */
        private void doBootCommands(String file) {
            if (file == null) {
                return;
            }
            try {
                BootCommands bootCommands = new BootCommands();
                STDOUT.println("Reading in commandments from " + file);
                bootCommands.parseCommandScript(new File(file));
                bootCommands.executeCommands(gf.getCommandRunner());
            } catch (IOException ex) {
                getLogger().log(SEVERE, "Error reading from file " + file, ex);
            } catch (Throwable ex) {
                getLogger().log(SEVERE, "Error executing commands from file " + file, ex);
            }
        }

        private Logger getLogger() {
            return Logger.getLogger(GlassFishMain.class.getName());
        }
    }
}
