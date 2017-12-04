/*
 * Copyright (c) 2017 Payara Foundation and/or its affiliates. All rights reserved.
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
import static java.nio.file.Files.createTempDirectory;
import static java.nio.file.Files.write;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static java.util.logging.Level.SEVERE;
import static java.util.regex.Pattern.DOTALL;
import static java.util.regex.Pattern.MULTILINE;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

import fish.payara.arquillian.container.payara.process.BufferingConsumer;
import fish.payara.arquillian.container.payara.process.ConsoleReader;
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

    // Regexps parse the server log for config info and deployment info of Payara Micro.
    // This should be in sync with fish.payara.appserver.micro.services.data.InstanceDescriptorImpl#toString

    private static final Pattern instanceConfigPattern = Pattern
            .compile("Instance Configuration.*Host: (?<host>.*)$.*HTTP Port\\(s\\):(?<ports>.*)$.*HTTPS", DOTALL | MULTILINE);

    // The following regexps parse a line such as this one:
    // Deployed: 1284c7bc-4733-4b44-8d50-a0ad8a42a1a4 ( 1284c7bc-4733-4b44-8d50-a0ad8a42a1a4 war
    // /1284c7bc-4733-4b44-8d50-a0ad8a42a1a4 [ < jsp *.jspx >< ArquillianServletRunner /ArquillianServletRunner >< jsp *.jsp
    // >< default / > ] )

    private static final Pattern appPattern = Pattern.compile(
            "Deployed: (?<appName>.*) \\( (?<modules>.*) \\)");
    private static final Pattern modulePattern = Pattern.compile(
            "(?<modName>.*?) (war) (?<modContextRoot>.*?) \\[ (?<servletMappings>.*?) \\]");
    private static final Pattern servletMappingsPattern = Pattern.compile(
            "\\< (?<servletName>.*?) (?<servletMapping>.*?) \\>");

    private PayaraMicroContainerConfiguration configuration;
    private Process payaraMicroProcess;
    private Thread shutdownHook;

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

        try {
            // The main directory from which we'll conduct our business
            Path arquillianMicroDir = createTempDirectory("arquillian-payara-micro");

            // Create paths for the directories we'll be using
            Path targetLogDir = arquillianMicroDir.resolve("logs/");
            Path deploymentDir = arquillianMicroDir.resolve("deployments/");
            Path targetZipDir = arquillianMicroDir.resolve("zipped-logs/");

            // Create the directories
            targetLogDir.toFile().mkdir();
            deploymentDir.toFile().mkdir();
            targetZipDir.toFile().mkdir();

            // Create path for the commands.txt file, which holds the asadmin post boot command
            Path commandFile = arquillianMicroDir.resolve("commands.text");

            // Create the commands.txt file - write a single command to zip up the log files
            // and store this zip into the /zipped-logs/ directory.
            write(
                commandFile,
                ("collect-log-files --retrieve true " + targetZipDir.toAbsolutePath().resolve("log.zip").toString()).getBytes());

            // Create the path for the deployment archive (e.g. the application war or ear)
            Path deploymentFile = deploymentDir.resolve(archive.getName());

            // Create the deployment file itself
            archive.as(ZipExporter.class).exportTo(deploymentFile.toFile());

            // Create the list of commands to start Payara Micro
            List<String> cmd = asList(
                    "java", 
                    "-jar", configuration.getMicroJarFile().getAbsolutePath(), 
                    "--nocluster", 
                    "--logtofile", targetLogDir.toAbsolutePath().resolve("log.txt").toString(), 
                    "--postdeploycommandfile", commandFile.toAbsolutePath().toString(), 
                    "--deploy", deploymentFile.toAbsolutePath().toString());

            WatchService watcher = FileSystems.getDefault().newWatchService();

            targetZipDir.register(watcher, ENTRY_CREATE);

            logger.info("Starting Payara Micro using cmd: " + cmd);

            registerShutdownHook();
            payaraMicroProcess = new ProcessBuilder(cmd).redirectErrorStream(true).start();
            BufferingConsumer consumer = new BufferingConsumer(createProcessOutputConsumer());

            ConsoleReader consoleReader = new ConsoleReader(payaraMicroProcess, consumer);
            new Thread(consoleReader).start();

            WatchKey key = null;
            try {
                key = watcher.poll(180, SECONDS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            }

            if (key == null) {
                // TODO: make timeout config
                logger.info("Timeout: Payara Micro did not start after 180 seconds");
            }

            for (WatchEvent<?> event : key.pollEvents()) {

                String context = event.context() != null ? event.context().toString().trim() : "";

                if ("log.zip".equals(context)) {

                    String log = consumer.getBuffer().toString();

                    Matcher matcher = instanceConfigPattern.matcher(log);
                    if (matcher.find()) {

                        String host = matcher.group("host").trim();
                        String[] ports = matcher.group("ports").trim().split(" ");
                        int firstPort = Integer.parseInt(ports[0].trim());

                        logger.info("Payara Micro running on host:" + host + " port: " + firstPort);

                        HTTPContext httpContext = new HTTPContext(host, firstPort);

                        // For all application that are deployed
                        Matcher appMatcher = appPattern.matcher(log.substring(matcher.start()));
                        while (appMatcher.find()) {

                            logger.info("Deployed application detected. Name: " + appMatcher.group("appName"));

                            // For all modules within a single application
                            Matcher moduleMatcher = modulePattern.matcher(appMatcher.group("modules"));
                            while (moduleMatcher.find()) {

                                logger.info("\tModule name: " + moduleMatcher.group("modName") + " root: "
                                        + moduleMatcher.group("modContextRoot"));

                                Matcher servletMappingsMatcher = servletMappingsPattern.matcher(moduleMatcher.group("servletMappings"));

                                // For all Servlet mappings within a single module
                                while (servletMappingsMatcher.find()) {
                                    logger.info("\t\tServlet name:" + servletMappingsMatcher.group("servletName"));
                                    httpContext.add(
                                            new Servlet(servletMappingsMatcher.group("servletName"), moduleMatcher.group("modContextRoot")));
                                }
                            }

                        }

                        ProtocolMetaData protocolMetaData = new ProtocolMetaData();
                        protocolMetaData.addContext(httpContext);

                        return protocolMetaData;
                    }

                }
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public void undeploy(Archive<?> archive) throws DeploymentException {
        // Nothing to do here, we start and deploy to Payara Micro in one step and thus don't need undeployments
    }

    @Override
    public void stop() throws LifecycleException {
        removeShutdownHook();
        try {
            stopContainer();
        } catch (LifecycleException failedStoppingContainer) {
            logger.log(SEVERE, "Failed stopping container.", failedStoppingContainer);
        }
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
