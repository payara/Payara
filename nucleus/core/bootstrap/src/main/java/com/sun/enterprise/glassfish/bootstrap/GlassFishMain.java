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
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright [2017-2022] Payara Foundation and/or affilates

package com.sun.enterprise.glassfish.bootstrap;

import fish.payara.logging.PayaraLogManager;
import fish.payara.boot.runtime.BootCommands;
import org.glassfish.embeddable.*;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.sun.enterprise.module.bootstrap.ArgumentManager.argsToMap;
import static java.util.logging.Level.SEVERE;

/**
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class GlassFishMain {

    private static final Pattern COMMAND_PATTERN = Pattern.compile("([^\"']\\S*|\".*?\"|'.*?')\\s*");
    private static final String[] COMMAND_TYPE = new String[0];
    private static final Logger LOGGER;

    // TODO(Sahoo): Move the code to ASMain once we are ready to phase out ASMain

    static {
        // This is not a JVM parameter in the domain as users should not have the possibility to not use the Payara Log Manager.
        System.setProperty("java.util.logging.manager", PayaraLogManager.class.getName());

        // Do not use as variable initialization as that will trigger LogManager before System property is set.
        LOGGER = Logger.getLogger(GlassFishMain.class.getName());
    }

    public static void main(final String args[]) throws Exception {
        MainHelper.checkJdkVersion();
        
        final Properties argsAsProps = argsToMap(args);

        String platform = MainHelper.whichPlatform();

        System.out.println("Launching Payara Server on " + platform + " platform");
        
        MainHelper.HotSwapHelper.updateHotSwapClassLoaderConfig();

        // Set the system property if downstream code wants to know about it
        System.setProperty(Constants.PLATFORM_PROPERTY_KEY, platform); // TODO(Sahoo): Why is this a system property?

        File installRoot = MainHelper.findInstallRoot();

        // domainDir can be passed as argument, so pass the agrgs as well.
        File instanceRoot = MainHelper.findInstanceRoot(installRoot, argsAsProps);

        Properties ctx = MainHelper.buildStartupContext(platform, installRoot, instanceRoot, args);
        /*
         * We have a tricky class loading issue to solve. GlassFishRuntime looks for an implementation of RuntimeBuilder.
         * In case of OSGi, the implementation class is OSGiGlassFishRuntimeBuilder. OSGiGlassFishRuntimeBuilder has
         * compile time dependency on OSGi APIs, which are unavoidable. More over, OSGiGlassFishRuntimeBuilder also
         * needs to locate OSGi framework factory using some class loader and that class loader must share same OSGi APIs
         * with the class loader of OSGiGlassFishRuntimeBuilder. Since we don't have the classpath for OSGi framework
         * until main method is called (note, we allow user to select what OSGi framework to use at runtime without
         * requiring them to add any extra jar in system classpath), we can't assume that everything is correctly set up
         * in system classpath. So, we create a class loader which can load GlassFishRuntime, OSGiGlassFishRuntimebuilder
         * and OSGi framework classes.
         */
        final ClassLoader launcherCL = MainHelper.createLauncherCL(ctx,
                ClassLoader.getSystemClassLoader().getParent());
        Class launcherClass = launcherCL.loadClass(GlassFishMain.Launcher.class.getName());
        Object launcher = launcherClass.newInstance();
        Method method = launcherClass.getMethod("launch", Properties.class);
        method.invoke(launcher, ctx);
    }

    public static class Launcher {
        /*
         * Only this class has compile time dependency on glassfishapi.
         */
        private volatile GlassFish gf;
        private volatile GlassFishRuntime gfr;

        public Launcher() {
        }

        public void launch(Properties ctx) throws Exception {
            addShutdownHook();
            gfr = GlassFishRuntime.bootstrap(new BootstrapProperties(ctx), getClass().getClassLoader());
            gf = gfr.newGlassFish(new GlassFishProperties(ctx));
            // Services required for variable expansion aren't available during pre-boot, so don't expand values
            doBootCommands(ctx.getProperty("-prebootcommandfile"), false);
            if (Boolean.valueOf(Util.getPropertyOrSystemProperty(ctx, "GlassFish_Interactive", "false"))) {
                startConsole();
            } else {
                gf.start();
            }
        }

        private void startConsole() throws IOException {
            String command;
            final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
            while ((command = readCommand(reader)) != null) {
                try {
                    System.out.println("command = " + command);
                    if ("start".equalsIgnoreCase(command)) {
                        if (gf.getStatus() != GlassFish.Status.STARTED || gf.getStatus() == GlassFish.Status.STOPPING || gf.getStatus() == GlassFish.Status.STARTING)
                            gf.start();
                        else System.out.println("Already started or stopping or starting");
                    } else if ("stop".equalsIgnoreCase(command)) {
                        if (gf.getStatus() != GlassFish.Status.STARTED) {
                            System.out.println("GlassFish is not started yet. Please execute start first.");
                            continue;
                        }
                        gf.stop();
                    } else if (command.startsWith("deploy")) {
                        if (gf.getStatus() != GlassFish.Status.STARTED) {
                            System.out.println("GlassFish is not started yet. Please execute start first.");
                            continue;
                        }
                        Deployer deployer = gf.getService(Deployer.class, null);
                        String[] tokens = command.split("\\s");
                        if (tokens.length < 2) {
                            System.out.println("Syntax: deploy <options> file");
                            continue;
                        }
                        final URI uri = URI.create(tokens[tokens.length -1]);
                        String[] params = Arrays.copyOfRange(tokens, 1, tokens.length-1);
                        String name = deployer.deploy(uri, params);
                        System.out.println("Deployed = " + name);
                    } else if (command.startsWith("undeploy")) {
                        if (gf.getStatus() != GlassFish.Status.STARTED) {
                            System.out.println("GlassFish is not started yet. Please execute start first.");
                            continue;
                        }
                        Deployer deployer = gf.getService(Deployer.class, null);
                        String name = command.substring(command.indexOf(' ')).trim();
                        deployer.undeploy(name);
                        System.out.println("Undeployed = " + name);
                    } else if ("quit".equalsIgnoreCase(command)) {
                        System.exit(0);
                    } else {
                        if (gf.getStatus() != GlassFish.Status.STARTED) {
                            System.out.println("GlassFish is not started yet. Please execute start first.");
                            continue;
                        }
                        CommandRunner cmdRunner = gf.getCommandRunner();
                        runCommand(cmdRunner, command);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
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
            System.out.print("Enter any of the following commands: start, stop, quit, deploy <path to file>, undeploy <name of app>\n" +
                    "glassfish$ ");
            System.out.flush();
        }

        private void addShutdownHook() {
            Runtime.getRuntime().addShutdownHook(new Thread("GlassFish Shutdown Hook") {
                public void run() {
                    try {
                        gfr.shutdown();
                    }
                    catch (Exception ex) {
                        System.err.println("Error stopping framework: " + ex);
                        ex.printStackTrace();
                    }
                }
            });

        }
        
        /**
         * Runs a command read from a string
         * @param cmdRunner
         * @param line
         * @throws GlassFishException 
         */
        private void runCommand(CommandRunner cmdRunner, String line) throws GlassFishException, IOException {
            
            line = cleanCommand(line);
            if(line == null) {
                return;
            }
            
            System.out.println("Running command: " + line);
            List<String> tokens = parseCommand(line);
            CommandResult result = cmdRunner.run(
                    tokens.get(0),
                    tokens.subList(1, tokens.size()).toArray(COMMAND_TYPE)
            );
            System.out.println(result.getOutput());
            if(result.getFailureCause() != null) {
                result.getFailureCause().printStackTrace();
            }
        }
        
        /**
         * Cleans a command read from a string
         * @param line
         */
        private String cleanCommand(String line) {
            if(line == null) {
                return null;
            }
            line = line.replaceAll("^\\s*#.*", ""); // Removes comments at the start of lines
            line = line.replaceAll("\\s#.*", ""); // Removes comments with whitespace before them. This allows for hashtags used in commands to be ignored.
            if (line.isEmpty() || line.replaceAll("\\s", "").isEmpty()) {
                return null;
            }
            return line;
        }
        
        /**
         * Parse a command read from a string
         * @param line
         */
        private List<String> parseCommand(String line) {
            List<String> tokens = new ArrayList<>();
            Matcher matcher = COMMAND_PATTERN.matcher(line);
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
         * @param file
         */
        private void doBootCommands(String file, boolean expandValues) {
            if (file == null) {
                return;
            }
            try {
                BootCommands bootCommands = new BootCommands();
                System.out.println("Reading in commandments from " + file);
                bootCommands.parseCommandScript(new File(file), expandValues);
                bootCommands.executeCommands(gf.getCommandRunner());
            } catch (IOException ex) {
                LOGGER.log(SEVERE, "Error reading from file");
            } catch (Throwable ex) {
                LOGGER.log(SEVERE, null, ex);
            }
        }
    }

}
