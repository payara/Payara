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
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 * 
 * JBoss, Home of Professional Open Source
 * Copyright 2010, Red Hat Middleware LLC, and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package fish.payara.arquillian.container.payara.embedded;

import static com.sun.enterprise.util.SystemPropertyConstants.INSTANCE_ROOT_PROPERTY;
import static fish.payara.arquillian.container.payara.embedded.ShrinkWrapUtil.toURL;
import static java.lang.Runtime.getRuntime;
import static java.lang.System.getProperty;
import static org.jboss.shrinkwrap.api.Filters.include;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletRegistration;

import org.apache.catalina.Container;
import org.glassfish.embeddable.BootstrapProperties;
import org.glassfish.embeddable.CommandResult;
import org.glassfish.embeddable.CommandRunner;
import org.glassfish.embeddable.GlassFish;
import org.glassfish.embeddable.GlassFishException;
import org.glassfish.embeddable.GlassFishProperties;
import org.glassfish.embeddable.GlassFishRuntime;
import org.glassfish.embeddable.web.Context;
import org.glassfish.embeddable.web.VirtualServer;
import org.glassfish.embeddable.web.WebContainer;
import org.jboss.arquillian.container.spi.client.container.DeployableContainer;
import org.jboss.arquillian.container.spi.client.container.DeploymentException;
import org.jboss.arquillian.container.spi.client.container.LifecycleException;
import org.jboss.arquillian.container.spi.client.protocol.ProtocolDescription;
import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.ProtocolMetaData;
import org.jboss.arquillian.container.spi.client.protocol.metadata.Servlet;
import org.jboss.shrinkwrap.api.Archive;
import org.jboss.shrinkwrap.api.ArchivePath;
import org.jboss.shrinkwrap.api.spec.EnterpriseArchive;
import org.jboss.shrinkwrap.api.spec.WebArchive;
import org.jboss.shrinkwrap.descriptor.api.Descriptor;

import com.sun.enterprise.web.WebModule;

/**
 *
 * @author <a href="mailto:aslak@redhat.com">Aslak Knutsen</a>
 * @version $Revision: $
 */
public class PayaraContainer implements DeployableContainer<PayaraConfiguration> {
    private static final Logger log = Logger.getLogger(PayaraContainer.class.getName());

    private static final String SYSTEM_PROPERTY_REGEX = "\\$\\{(.*)\\}";
    private static final String COMMAND_ADD_RESOURCES = "add-resources";

    // TODO: open configuration up for bind address
    private static final String ADDRESS = "localhost";

    private PayaraConfiguration configuration;
    private GlassFishRuntime glassfishRuntime;
    private GlassFish glassfish;

    private boolean shouldSetPort = true;
    private int bindHttpPort;
    private int bindHttpsPort;

    @Override
    public Class<PayaraConfiguration> getConfigurationClass() {
        return PayaraConfiguration.class;
    }

    @Override
    public ProtocolDescription getDefaultProtocol() {
        return new ProtocolDescription("Servlet 3.0");
    }

    @Override
    public void setup(PayaraConfiguration configuration) {
        this.configuration = configuration;
        
        BootstrapProperties bootstrapProps = new BootstrapProperties();
        if (configuration.getInstallRoot() != null) {
            bootstrapProps.setInstallRoot(configuration.getInstallRoot());
        }
        
        try {
            glassfishRuntime = GlassFishRuntime.bootstrap(bootstrapProps);
        } catch (Exception e) {
            throw new RuntimeException("Could not setup GlassFish Embedded Bootstrap", e);
        }

        boolean cleanup = configuration.getCleanup();
        GlassFishProperties serverProps = new GlassFishProperties();

        if (configuration.getInstanceRoot() != null) {
            File instanceRoot = new File(configuration.getInstanceRoot());
            if (instanceRoot.exists()) {
                cleanup = false;
            }
            serverProps.setInstanceRoot(configuration.getInstanceRoot());
            shouldSetPort = false;
        }
        
        if (configuration.getConfigurationXml() != null) {
            serverProps.setConfigFileURI(configuration.getConfigurationXml());
            shouldSetPort = false;
        }
        
        serverProps.setConfigFileReadOnly(configuration.isConfigurationReadOnly());
        if (shouldSetPort) {
            bindHttpPort = configuration.getBindHttpPort();
            serverProps.setPort("http-listener", bindHttpPort);
            bindHttpsPort = configuration.getBindHttpsPort();
            serverProps.setPort("https-listener", bindHttpsPort);
        }

        try {
            glassfish = glassfishRuntime.newGlassFish(serverProps);
        } catch (Exception e) {
            throw new RuntimeException("Could not setup GlassFish Embedded Runtime", e);
        }

        if (cleanup) {
            getRuntime().addShutdownHook(new Thread() {
                @Override
                public void run() {
                    deleteRecursive(new File(getProperty(INSTANCE_ROOT_PROPERTY)));
                }
            });
        }
    }

    @Override
    public void start() throws LifecycleException {
        try {
            glassfish.start();
            if (!shouldSetPort) {
                readBindingHttpPort();
            }
            bindCommandRunner();
        } catch (Exception e) {
            throw new LifecycleException("Could not start GlassFish Embedded", e);
        }
        
        // Server needs to be started before we can deploy resources
        for (String resource : configuration.getResourcesXml()) {
            try {
                executeCommand(COMMAND_ADD_RESOURCES, resource);
            } catch (Throwable e) {
                throw new RuntimeException("Could not deploy sun-reosurces file: " + resource, e);
            }
        }
    }

    @Override
    public void stop() throws LifecycleException {
        try {
            unbindCommandRunner();
            glassfish.stop();
        } catch (Exception e) {
            throw new LifecycleException("Could not stop GlassFish Embedded", e);
        }
    }

    @Override
    public ProtocolMetaData deploy(Archive<?> archive) throws DeploymentException {
        String deploymentName = createDeploymentName(archive.getName());
        try {
            glassfish.getDeployer().deploy(toURL(archive).toURI(), "--name", deploymentName);
        } catch (Exception e) {
            throw new DeploymentException("Could not deploy " + archive.getName(), e);
        }

        try {
            HTTPContext httpContext = new HTTPContext(ADDRESS, bindHttpPort);

            findServlets(httpContext, resolveWebArchiveNames(archive));

            return new ProtocolMetaData()
                        .addContext(httpContext);
        } catch (GlassFishException e) {
            throw new DeploymentException("Could not probe Payara embedded for environment", e);
        }
    }

    /**
     * @param archive
     * @return
     */
    private String[] resolveWebArchiveNames(Archive<?> archive) {
        if (archive instanceof WebArchive) {
            return new String[] {createDeploymentName(archive.getName())};
        } else if (archive instanceof EnterpriseArchive) {
            List<String> deploymentNames = new ArrayList<String>();
            for (ArchivePath path : archive.getContent(include(".*\\.war")).keySet()) {
                deploymentNames.add(createDeploymentName(path.get()));
            }
            
            return deploymentNames.toArray(new String[0]);
        }
        
        return new String[0];
    }

    @Override
    public void undeploy(Archive<?> archive) throws DeploymentException {
        try {
            glassfish.getDeployer().undeploy(createDeploymentName(archive.getName()));
        } catch (Exception e) {
            throw new DeploymentException("Could not undeploy " + archive.getName(), e);
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

    private String createDeploymentName(String archiveName) {
        String correctedName = archiveName;
        if (correctedName.startsWith("/")) {
            correctedName = correctedName.substring(1);
        }
        if (correctedName.indexOf(".") != -1) {
            correctedName = correctedName.substring(0, correctedName.lastIndexOf("."));
        }
        return correctedName;
    }

    public void findServlets(HTTPContext httpContext, String[] webArchiveNames) throws GlassFishException {
        WebContainer webContainer = glassfish.getService(WebContainer.class);
        for (String deploymentName : webArchiveNames) {
            for (VirtualServer server : webContainer.getVirtualServers()) {
                WebModule webModule = null;

                for (Context serverContext : server.getContexts()) {
                    if (serverContext instanceof WebModule) {
                        if (((WebModule) serverContext).getID().startsWith(deploymentName)) {
                            webModule = (WebModule) serverContext;
                        }
                    }
                }
                
                if (webModule == null) {
                    if (server instanceof com.sun.enterprise.web.VirtualServer) {
                        Container child = ((com.sun.enterprise.web.VirtualServer) server).findChild("/" + deploymentName);
                        if (child instanceof WebModule) {
                            webModule = (WebModule) child;
                        }
                    }
                }
                
                if (webModule != null) {
                    for (Map.Entry<String, ? extends ServletRegistration> servletRegistration : webModule.getServletRegistrations()
                        .entrySet()) {
                        httpContext.add(new Servlet(servletRegistration.getKey(), webModule.getContextPath()));
                    }
                }
            }
        }
    }

    private String executeCommand(String command, String... parameterList) throws Throwable {
        CommandRunner runner = glassfish.getCommandRunner();
        CommandResult result = runner.run(command, parameterList);

        String output = null;
        switch (result.getExitStatus()) {
            case FAILURE:
            case WARNING:
                throw result.getFailureCause();
            case SUCCESS:
                output = result.getOutput();
                log.info("command " + command + " parameters" + parameterList + " result: " + output);
                break;
        }
        return output;
    }

    private void bindCommandRunner() throws NamingException, GlassFishException {
        new InitialContext().bind("org.glassfish.embeddable.CommandRunner", glassfish.getCommandRunner());
    }

    private void unbindCommandRunner() throws NamingException {
        new InitialContext().unbind("org.glassfish.embeddable.CommandRunner");
    }

    private void deleteRecursive(File dir) {
        if (dir == null) {
            throw new IllegalArgumentException("directory cannot be null");
        }
        
        if (!dir.isDirectory()) {
            throw new IllegalArgumentException("directory must be a directory");
        }
        
        if (dir.exists()) {
            for (File file : dir.listFiles()) {
                if (file.isDirectory()) {
                    deleteRecursive(file);
                    file.delete();
                } else {
                    file.delete();
                }
            }
            dir.delete();
        }
    }

    /**
     * Populates the bindHttpPort value by reading the GlassFish configuration.
     *
     * @throws LifecycleException
     *     When there is failure reading the contents of the GlassFish configuration
     */
    private void readBindingHttpPort() throws LifecycleException {
        // Fetch the name of the configuration ref element of the Default Admin Server
        String config = fetchAttribute("servers.server.server.config-ref");

        // Fetch the virtual servers in the configuration element
        List<String> virtualServers = fetchAttributes("configs.config." + config + ".http-service.virtual-server.*.id");
        for (String virtualServer : virtualServers) {
            // Ignore the __asadmin virtual server as no deployments will occur against this
            if (virtualServer.equals("__asadmin")) {
                continue;
            } else {
                // Fetch the network listeners of the virtual server
                String networkListenerList = fetchAttribute("configs.config." + config + ".http-service.virtual-server."
                    + virtualServer + ".network-listeners");

                // Iterate through the network listeners, resolve the port number of the first one. Ignore the rest.
                for (String networkListener : networkListenerList.split(",")) {
                    Integer listenPort = readNetworkListenerPort(config, networkListener);
                    if (listenPort != null) {
                        bindHttpPort = listenPort;
                    }
                }
            }
        }
    }

    /**
     * @throws LifecycleException
     */
    private Integer readNetworkListenerPort(String config, String networkListener) throws LifecycleException {
        boolean enabled = Boolean.parseBoolean(fetchAttribute("configs.config." + config
            + ".network-config.network-listeners.network-listener." + networkListener + ".enabled"));
        
        // Is the listener enabled?
        if (enabled) {
            String protocol =
                fetchAttribute("configs.config." + config + ".network-config.network-listeners.network-listener."
                    + networkListener + ".protocol");
            boolean securityEnabled = Boolean.parseBoolean(fetchAttribute("configs.config." + config
                + ".network-config.protocols.protocol." + protocol + ".security-enabled"));
            
            // Is the protocol secure? If yes, then ignore since Arquillian will not make HTTPS connections (yet) 
            if (!securityEnabled) {
                String portNum = fetchAttribute("configs.config." + config
                    + ".network-config.network-listeners.network-listener." + networkListener + ".port");
                Pattern sysProp = Pattern.compile(SYSTEM_PROPERTY_REGEX);
                Matcher matcher = sysProp.matcher(portNum);
                if (matcher.matches()) {
                    // If the port number is stored as a system property, then resolve the property value
                    String propertyName = matcher.group(1);
                    portNum = fetchAttribute("configs.config." + config + ".system-property." + propertyName + ".value");
                    return Integer.parseInt(portNum);
                } else {
                    return Integer.parseInt(portNum);
                }
            }
        }
        
        return null;
    }

    /**
     * Executes the 'get' command and parses the output for multiple values. Use this method if you're not sure of the
     * number of
     * values returned in the command output.
     *
     * @param parameterList
     *     The list of parameters to be provided for the execution of the 'get' command.
     *
     * @return The list of values of the attribute returned in the command output.
     *
     * @throws LifecycleException
     *     When there is a failure executing the 'get' command
     */
    private List<String> fetchAttributes(String... parameterList) throws LifecycleException {
        try {
            List<String> result = new ArrayList<String>();
            
            String[] lines = executeCommand("get", parameterList).split("\\n");
            
            // Omit the first line and parse the rest
            for (int ctr = 1; ctr < lines.length; ctr++) {
                // Obtain the value in the K=V pair
                result.add(lines[ctr].split("=")[1]);
            }
            
            return result;
        } catch (Throwable ex) {
            throw new LifecycleException("Failed to read the HTTP listener configuration.", ex);
        }
    }

    /**
     * Executes the 'get' command and parses the output for a single value. Multiple values are not
     * recognized. Use this only if you are sure of a single return value.
     *
     * @param parameterList The list of parameters to be provided for the execution of the 'get'
     *            command.
     *
     * @return The value of the attribute returned in the command output.
     *
     * @throws LifecycleException When there is a failure executing the 'get' command
     */
    private String fetchAttribute(String... parameterList) throws LifecycleException {
        try {
            return executeCommand("get", parameterList)
                        .split("\\n")[1]  // Omit the first line and parse only the second
                        .split("=")[1];   // Obtain the value in the K=V pair
        } catch (Throwable ex) {
            throw new LifecycleException("Failed to read the HTTP listener configuration.", ex);
        }
    }
}