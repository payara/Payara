/*
 * Copyright (c) 2017-2018 Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package fish.payara.arquillian.container.payara.managed;

import static java.lang.Runtime.getRuntime;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.logging.Level.SEVERE;
import static java.util.regex.Pattern.CASE_INSENSITIVE;
import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.MULTILINE;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;

import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.spi.client.protocol.metadata.Servlet;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.exporter.ZipExporter;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;

import fish.payara.arquillian.container.payara.PayaraVersion;
import fish.payara.arquillian.container.payara.file.LogReader;
import fish.payara.arquillian.container.payara.file.StringConsumer;
import fish.payara.arquillian.container.payara.process.OutputLoggingConsumer;
import fish.payara.arquillian.container.payara.process.ProcessOutputConsumer;
import fish.payara.arquillian.container.payara.process.SilentOutputConsumer;

/**
 * Container for Payara Micro 5
 *
 * @author Arjan Tijms
 */
public class PayaraMicroDeployableContainer implements DeployableContainer<PayaraMicroContainerConfiguration> {

    private static final Logger logger = Logger.getLogger(PayaraMicroDeployableContainer.class.getName());

    private static final Pattern instanceConfigPattern = Pattern
            .compile("Instance Configuration(?<jsonFormat>\")?.*Host\"?: \"?(?<host>.*?)\"?,{0,1}$.*HTTP Port\\(s\\)\"?: \"?(?<ports>.*?)\"?,?$.*HTTPS", DOTALL | MULTILINE | CASE_INSENSITIVE);
    
    private static final Pattern jsonPattern = Pattern.compile(
            "Deployed\": (?<jsonArray>\\[.+?\\])", DOTALL | MULTILINE);
    private static final Pattern appPattern = Pattern.compile(
            "Deployed: (?<appName>.*) \\( (?<modules>.*) \\)");
    private static final Pattern modulePattern = Pattern.compile(
            "(?<modName>.*?) (war) (?<modContextRoot>.*?) \\[ (?<servletMappings>.*?) \\]");
    private static final Pattern servletMappingsPattern = Pattern.compile(
            "\\< (?<servletName>.*?) (?<servletMapping>.*?) \\>");

    private PayaraMicroContainerConfiguration configuration;
    private Process payaraMicroProcess;
    private Thread shutdownHook;

    private String log = "";

    private int startupTimeoutInSeconds = 180;

    @Override
    public Class<PayaraMicroContainerConfiguration> getConfigurationClass() {
        return PayaraMicroContainerConfiguration.class;
    }

    @Override
    public void setup(PayaraMicroContainerConfiguration configuration) {
        if (configuration == null) {
            throw new IllegalArgumentException("configuration must not be null");
        }

        this.configuration = configuration;
    }

    @Override
    public ProtocolDescription getDefaultProtocol() {
        return new ProtocolDescription("Servlet 3.0");
    }

    @Override
    public void start() throws LifecycleException {
        // Nothing to do here, we start and deploy to Payara Micro in one step
    }

    @Override
    public ProtocolMetaData deploy(Archive<?> archive) throws DeploymentException {
        if (archive == null) {
            throw new IllegalArgumentException("archive must not be null");
        }

        WatchKey watchKey = null;

        try {
            // The main directory from which we'll conduct our business
            Path arquillianMicroDir = Files.createTempDirectory("arquillian-payara-micro");

            // Create paths for the directories we'll be using
            Path targetLogDir = arquillianMicroDir.resolve("logs/");
            Path deploymentDir = arquillianMicroDir.resolve("deployments/");
            Path targetZipDir = arquillianMicroDir.resolve("zipped-logs/");

            // Create the directories
            targetLogDir.toFile().mkdir();
            deploymentDir.toFile().mkdir();
            targetZipDir.toFile().mkdir();

            // Create the path for the commands.txt file, which holds the asadmin post boot commands
            File commandFile = arquillianMicroDir.resolve("commands.txt").toFile();

            // Create the commands.txt file - write a single command to zip up the log files
            // and store this zip into the /zipped-logs/ directory.
            Files.write(commandFile.toPath(), 
                ("collect-log-files --retrieve true " + targetZipDir.toAbsolutePath().resolve("log.zip").toString()).getBytes()
            );

            // Create the path for the deployment archive (e.g. the application war or ear)
            File deploymentFile = deploymentDir.resolve(archive.getName()).toFile();

            // Create the deployment file itself
            archive.as(ZipExporter.class).exportTo(deploymentFile);

            // Create the log file
            File logFile = targetLogDir.resolve("log.txt").toFile();
            logFile.createNewFile();

            // Create the list of commands to start Payara Micro
            List<String> cmd = new ArrayList<>(asList(
                    "java", "-jar", configuration.getMicroJarFile().getAbsolutePath(),
                    "--logtofile", logFile.getAbsolutePath(),
                    "--postdeploycommandfile", commandFile.getAbsolutePath(),
                    "--deploy", deploymentFile.getAbsolutePath()
                    ));

            // Disable clustering if it's not explicitly enabled
            if (!configuration.isClusterEnabled()) {
                cmd.add("--nocluster");
            }

            // Enable --showServletMappings if it's supported
            if (configuration.getMicroVersion().isMoreRecentThan(new PayaraVersion("5.181-SNAPSHOT"))) {
                cmd.add("--showServletMappings");
            }

            // Start Payara Micro in debug mode if it's been enabled
            if (configuration.isDebug()) {
                cmd.add(1, "-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=5006");
            }

            // Add the extra cmd options to the Payara Micro instance
            if (configuration.getCmdOptions() != null) {
                cmd.add(1, configuration.getCmdOptions());
            }

            // Add the extra micro options to the Payara Micro instance
            if (configuration.getExtraMicroOptions() != null) {
                cmd.add(configuration.getExtraMicroOptions());
            }

            logger.info("Starting Payara Micro using cmd: " + cmd);

            // Register a watch service to wait for the logs to be zipped.
            WatchService watcher = FileSystems.getDefault().newWatchService();
            targetZipDir.register(watcher, ENTRY_CREATE);

            // Allow Ctrl-C to stop the test, then start Payara Micro
            registerShutdownHook();
            payaraMicroProcess = new ProcessBuilder(cmd)
                .redirectErrorStream(true)
                .redirectOutput(logFile)
                .start();

            // Create an executor for handling the log reading and writing
            ScheduledExecutorService executor = Executors.newScheduledThreadPool(2);

            // Create a consumer for reading Payara Micro output
            StringConsumer consumer = new StringConsumer(createProcessOutputConsumer());
            LogReader logReader = new LogReader(logFile, consumer);
            executor.execute(logReader);

            // Wait for the logs to be zipped.
            // TODO: implement post-post boot command or similar to properly check when Micro has finished booting up.
            watchKey = watcher.poll(startupTimeoutInSeconds, SECONDS);
            if (watchKey == null) {
                logger.severe("Timeout (" + startupTimeoutInSeconds + "s) reached waiting for Payara Micro to start.");
            }

            // Check at intervals if Payara Micro has finished starting up or failed to start.
            CountDownLatch payaraMicroStarted = new CountDownLatch(1);
            executor.scheduleAtFixedRate(() -> {
                log = consumer.getBuilder().toString();
                // Check for app deployed
                Matcher startupMatcher = instanceConfigPattern.matcher(log);
                if (startupMatcher.find()) {
                    payaraMicroStarted.countDown();
                    executor.shutdown();
                }
            }, 500, 500, MILLISECONDS);

            // Wait for Payara Micro to start up, or time out after 10 seconds
            if (payaraMicroStarted.await(10, SECONDS)) {

                // Create a matcher for the 'Instance Configured' message
                Matcher instanceConfigMatcher = instanceConfigPattern.matcher(log);
                
                if (instanceConfigMatcher.find()) {

                    // Get the host and port that the application started on.
                    String host = instanceConfigMatcher.group("host").trim();
                    String[] ports = instanceConfigMatcher.group("ports").trim().split(" ");
                    int firstPort = Integer.parseInt(ports[0].trim());
                    logger.info("Payara Micro running on host: " + host + " port: " + firstPort);

                    HTTPContext httpContext = new HTTPContext(host, firstPort);

                    // If the instance config is in the new JSON format, parse the JSON object.
                    if (instanceConfigMatcher.group("jsonFormat") != null) {
                        processDeploymentAsJson(log.substring(instanceConfigMatcher.start()), httpContext);
                    } else {
                        // Otherwise, parse it the old way
                        processDeploymentOldMethod(log.substring(instanceConfigMatcher.start()), httpContext);
                    }

                    ProtocolMetaData protocolMetaData = new ProtocolMetaData();
                    protocolMetaData.addContext(httpContext);

                    return protocolMetaData;
                }
            }
        } catch (IOException e) {
            // Occurs when there was an error starting the Payara Micro thread or the ConsoleReader thread.
            logger.severe("Failed in creating a thread for Payara Micro.\n" + e.getMessage());
            Thread.currentThread().interrupt();
            return null;
        } catch (InterruptedException e) {
            // Occurs when the timeout is reached in waiting for Payara Micro to start
            logger.severe("Timeout reached waiting for Payara Micro to start.\n" + e.getMessage());
            Thread.currentThread().interrupt();
            return null;
        } finally {
            if (watchKey != null) {
                watchKey.cancel();
            }
        }

        throw new DeploymentException("No applications were found deployed to Payara Micro.");
    }

    private void processDeploymentAsJson(String deploymentInformation, HTTPContext httpContext) {
        Matcher jsonMatcher = jsonPattern.matcher(deploymentInformation);
        if (jsonMatcher.find()) {
            // Convert the deployment information into a JsonArray
            String jsonString = jsonMatcher.group("jsonArray");
            try (JsonReader reader = Json.createReader(new StringReader(jsonString))) {
                JsonArray array = reader.readArray();

                // For each deployed application
                for (JsonObject app : array.getValuesAs(JsonObject.class)) {

                    // Print the application details
                    printApplicationFound(app.getString("Name"));

                    // Store all application servlet mappings
                    Map<String, JsonObject> allMappings = new HashMap<>();

                    // If there's only one module, the mappings and context root are directly under the application
                    JsonObject appMappings = app.getJsonObject("Mappings");
                    if (appMappings != null) {
                        // Get the context root and store the relevant servlets
                        String contextRoot = app.getString("Context Root");
                        allMappings.put(contextRoot, app.getJsonObject("Mappings"));
                    } else {
                        JsonArray modules = app.getJsonArray("Modules");
                        modules.forEach(moduleValue -> {
                            // Get the context root and store the relevant servlets
                            JsonObject module = (JsonObject) moduleValue;
                            String contextRoot = module.getString("Context Root");
                            printModuleFound(module.getString("Name"), contextRoot);
                            allMappings.put(contextRoot, module);
                        });
                    }

                    // Handle each set of servlet mappings
                    for (String contextRoot : allMappings.keySet()) {
                        JsonObject mappings = allMappings.get(contextRoot);
                        mappings.values().forEach(name -> {
                            String servletName = name.toString().replaceAll("\"", "");
                            printServletFound(servletName);
                            httpContext.add(new Servlet(servletName, contextRoot));
                        });
                    }
                    
                }
            }
        }
    }

    private void processDeploymentOldMethod(String deploymentInformation, HTTPContext httpContext) {
        Matcher appMatcher = appPattern.matcher(deploymentInformation);
        // For each deployed application
        while (appMatcher.find()) {

            printApplicationFound(appMatcher.group("appName"));

            // For all modules within a single application
            Matcher moduleMatcher = modulePattern.matcher(appMatcher.group("modules"));
            while (moduleMatcher.find()) {

                printModuleFound(moduleMatcher.group("modName"), moduleMatcher.group("modContextRoot"));

                Matcher servletMappingsMatcher = servletMappingsPattern.matcher(moduleMatcher.group("servletMappings"));

                // For all Servlet mappings within a single module
                while (servletMappingsMatcher.find()) {
                    printServletFound(servletMappingsMatcher.group("servletName"));
                    httpContext.add(
                            new Servlet(servletMappingsMatcher.group("servletName"), moduleMatcher.group("modContextRoot")));
                }
            }

        }
    }

    private void printApplicationFound(String appName) {
        logger.log(Level.INFO, "Deployed application detected. Name: \"{0}\".", appName);
    }

    private void printModuleFound(String moduleName, String contextRoot) {
        logger.log(Level.INFO, "\tModule found. Name: \"{0}\". Context root: \"{1}\".", new Object[]{moduleName, contextRoot});
    }

    private void printServletFound(String servletName) {
        logger.log(Level.INFO, "\t\tServlet found. Name: \"{0}\".", servletName);
    }

    @Override
    public void undeploy(Archive<?> archive) throws DeploymentException {
        removeShutdownHook();
        try {
            stopContainer();
        } catch (LifecycleException failedStoppingContainer) {
            logger.log(SEVERE, "Failed stopping container.", failedStoppingContainer);
        }
    }

    @Override
    public void stop() throws LifecycleException {
        // Nothing to do here, we start and stop Payara micro in the un/deploy phases so no need to stop the container here.
    }

    @Override
    public void deploy(Descriptor descriptor) throws DeploymentException {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void undeploy(Descriptor descriptor) throws DeploymentException {
        throw new UnsupportedOperationException("Not implemented");
    }

    private ProcessOutputConsumer createProcessOutputConsumer() {
        if (configuration.isOutputToConsole()) {
            return new OutputLoggingConsumer();
        }

        return new SilentOutputConsumer();
    }

    private void registerShutdownHook() {
        shutdownHook = new Thread(() -> {

            logger.warning("Forcing container shutdown");

            try {
                stopContainer();
            } catch (LifecycleException e) {
                logger.log(SEVERE, "Failed stopping services through shutdown hook.", e);
            }
        });

        getRuntime().addShutdownHook(shutdownHook);
    }

    private void stopContainer() throws LifecycleException {
        payaraMicroProcess.destroy();
    }

    private void removeShutdownHook() {
        if (shutdownHook != null) {
            getRuntime().removeShutdownHook(shutdownHook);
            shutdownHook = null;
        }
    }

}
