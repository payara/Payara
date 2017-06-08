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
 * Copyright 2011, Red Hat Middleware LLC, and individual contributors
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
package fish.payara.arquillian.container.payara.clientutils;

import static fish.payara.arquillian.container.payara.clientutils.NodeAddress.getHttpProtocolPrefix;
import static java.lang.Boolean.parseBoolean;
import static java.util.regex.Pattern.compile;

import java.net.ConnectException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.regex.Matcher;

import javax.ws.rs.ProcessingException;

import org.glassfish.jersey.media.multipart.FormDataMultiPart;
import org.jboss.arquillian.container.spi.client.protocol.metadata.HTTPContext;
import org.jboss.arquillian.container.spi.client.protocol.metadata.Servlet;

import fish.payara.arquillian.container.payara.CommonPayaraConfiguration;

/**
 * @author Z.Paulovics
 */
public class PayaraClientService implements PayaraClient {
    
    private static final Logger log = Logger.getLogger(PayaraClientService.class.getName());

    private static final String WEBMODULE = "WebModule";
    private static final String SERVLET = "Servlet";
    private static final String RUNNING_STATUS = "RUNNING";
    
    /**
     * the REST resource path template to retrieve the version of the server
     */
    private static final String PAYARA_VERSION              = "/version";
    
    /**
     * the REST resource path template to retrieve the list of server instances
     */
    private static final String INSTANCE_LIST               = "/list-instances";
    
    /**
     * the REST resource path template to retrieve the list of applications
     */
    private static final String APPLICATION                 = "/applications/application";
    private static final String APPLICATION_RESOURCE        = "/applications/application/{name}";
    
    private static final String APPLICATION_COMPONENTS      = "/applications/application/{application}/list-sub-components";
    
    private static final String APPLICATION_SERVLETS        = "/applications/application/{application}/list-sub-components?appname={application}&id={module}&type=servlets";
    
    private static final String CLUSTERED_SERVER_INSTANCES  = "/clusters/cluster";
    
    private static final String MEMBER_SERVERS_RESOURCE     = "/clusters/cluster/{target}/server-ref";
    
    /**
     * the REST resource path template for cluster attributes object
     */
    private static final String CLUSTER_RESOURCE            = "/clusters/cluster/{cluster}";
    
    private static final String STANDALONE_SERVER_INSTANCES = "/servers/server";
    
    /**
     * the REST resource path template for server attributes object
     */
    private static final String SERVER_RESOURCE             = "/servers/server/{server}";
    
    private static final String SERVER_PROPERTY             = "/servers/server/{server}/system-property/{system-property}";
    
    /**
     *  the REST resource path template for the Servers instance http-listener object
     */
    private static final String HTTP_LISTENER_INS           = "/servers/server/{server}/system-property/{http-listener}";
    
    private static final String NODE_RESOURCE               = "/nodes/node/{node}";
    
    private static final String SYSTEM_PROPERTY             = "/configs/config/{config}/system-property/{system-property}";
    
    private static final String VIRTUAL_SERVERS             = "/configs/config/{config}/http-service/list-virtual-servers?target={target}";
    
    private static final String VIRTUAL_SERVER              = "/configs/config/{config}/http-service/virtual-server/{virtualServer}";

    private static final String LISTENER                    = "/configs/config/{config}/network-config/network-listeners/network-listener/{listener}";
    
    private static final String PROTOCOL                    = "/configs/config/{config}/network-config/protocols/protocol/{protocol}";
    
    private static final String SYSTEM_PROPERTY_REGEX = "\\$\\{(.*)\\}";
    
    private String target = ADMINSERVER;
    private String adminBaseUrl;
    private String DASUrl;

    private ServerStartegy serverInstance;
    private PayaraClientUtil clientUtil;
    private NodeAddress nodeAddress;

    private final CommonPayaraConfiguration configuration;

    // GlassFish client service constructor
    public PayaraClientService(CommonPayaraConfiguration configuration) {
        this.configuration = configuration;
        target = configuration.getTarget();

        final StringBuilder adminUrlBuilder = new StringBuilder()
            .append(getHttpProtocolPrefix(configuration.isAdminHttps()))
            .append(configuration.getAdminHost())
            .append(":")
            .append(configuration.getAdminPort());
        
        DASUrl = adminUrlBuilder.toString();
        
        adminBaseUrl = adminUrlBuilder.append("/management/domain").toString();

        // Start up the jersey client layer
        clientUtil = new PayaraClientUtil(configuration, adminBaseUrl);
    }

    /**
     * Start-up the server
     * <p>
     * - Get the node addresses list associated with the target - Pull the server instances status
     * form mgm API - In case of cluster tries to fund an instance which has RUNNING status
     *
     * @return none
     */
    public void startUp() throws PayaraClientException {

        Map<String, String> standaloneServers = new HashMap<>();
        Map<String, String> clusters = new HashMap<>();

        try {
            standaloneServers = getServersList();
        } catch (ProcessingException ch) {
            throw new PayaraClientException("Could not connect to DAS on: " + getDASUrl() + " | " + ch.getCause().getMessage());
        }

        if (ADMINSERVER.equals(getTarget())) {

            // The "target" is the Admin Server Instance
            serverInstance = new AdminServer();
        } else if (standaloneServers.containsKey(getTarget())) {

            // The "target" is an Standalone Server Instance
            serverInstance = new StandaloneServer();
        } else {

            // The "target" shall be clustered instance(s)
            clusters = getClustersList();

            if (clusters != null && clusters.containsKey(getTarget())) {

                // Now we have found the cluster specified by the Target attribute
                serverInstance = new ClusterServer();
            } else {
                // The "target" attribute can be a domain or misspelled, but neither can be accepted
                throw new PayaraClientException("The target property: " + getTarget() + " is not a valid target");
            }
        }

        // Fetch the HOST address & HTTP port info from the DAS server
        List<NodeAddress> nodeAddressList = serverInstance.getNodeAddressList();

        if (ADMINSERVER.equals(configuration.getTarget())) {
            // Admin Server must be running, otherwise we can not be here
            nodeAddress = nodeAddressList.get(0);
        } else {
            // Returns the nodeAddress if the target instance status is RUNNING
            // In case of cluster, returns the first RUNNING instance (if any) from the list
            nodeAddress = runningInstanceFilter(nodeAddressList);
        }
    }

    public Integer getPayaraVersion() {
        
        Map<String, Object> extraProperties = clientUtil.getExtraProperties(clientUtil.GETRequest(PAYARA_VERSION));
        if (extraProperties != null) {
            Object versionNumberObj = extraProperties.get("version-number");
            if (versionNumberObj instanceof String) {
                String version = (String) versionNumberObj;
                StringTokenizer tokenizer = new StringTokenizer(version, ".");
                
                if (tokenizer.hasMoreElements()) {
                    try {
                        return Integer.valueOf(tokenizer.nextToken());
                    } catch (NumberFormatException ignore) {
                        log.info("Exception getting major version for: " + version);
                    }
                }
                
            }
        }
        
        return null;
    }

    /**
     * Filtering on the status of the instances
     * -	If the standalone server instance status is RUNNING, returns the nodeAddress,
     * but throws an exception otherwise.
     * -	In case of cluster, returns the first RUNNING instance from the list,
     * but throws an exception if can not find any.
     *
     * @param nodeAddressList
     * @return nodeAddress - if any has RUNNING status
     */
    private NodeAddress runningInstanceFilter(List<NodeAddress> nodeAddressList) {

        String instanceStatus = null;
        for (Map<String, Object> instance : getClientUtil().getInstancesList(INSTANCE_LIST)) {
            for (NodeAddress node : nodeAddressList) {
                if (instance.get("name").equals(node.getServerName())) {
                    instanceStatus = (String) instance.get("status");
                    if (RUNNING_STATUS.equals(instanceStatus)) {
                        return node;
                    }
                }
            }
        }

        if (nodeAddressList.size() == 1) {
            throw new PayaraClientException(
                "The " + nodeAddressList.get(0).getServerName() + 
                " server-instance status is: " + instanceStatus);
        } else {
            throw new PayaraClientException(
                "Could not find any instance with RUNNING status in cluster: " + getTarget());
        }
    }

    /**
     * Do deploy an application defined by a multipart form's fileds to a target server or a cluster
     * of GlassFish 3.1
     *
     * @param name - name of the appliacation form - a form of MediaType.MULTIPART_FORM_DATA_TYPE
     * @return subComponents - a map of SubComponents of the application
     */
    public HTTPContext doDeploy(String name, FormDataMultiPart form) {
        // Deploy the application on the Payara server
        getClientUtil().POSTMultiPartRequest(APPLICATION, form);

        // Fetch the list of SubComponents of the application
        Map<String, Object> subComponentsResponse = getClientUtil().GETRequest(APPLICATION_COMPONENTS.replace("{application}", name));
        
        @SuppressWarnings("unchecked")
        Map<String, String> subComponents = (Map<String, String>) subComponentsResponse.get("properties");

        // Build up the HTTPContext object using the nodeAddress information
        HTTPContext httpContext = new HTTPContext(nodeAddress.getHost(), nodeAddress.getHttpPort());

        // Add the servlets to the HTTPContext
        String contextRoot = getApplicationContextRoot(name);

        if (subComponents != null) {
            for (Entry<String, String> subComponent : subComponents.entrySet()) {
                String componentName = subComponent.getKey().toString();
                
                if (WEBMODULE.equals(subComponent.getValue())) {

                    @SuppressWarnings("unchecked")
                    List<Map<String, Object>> children = (List<Map<String, Object>>) subComponentsResponse.get("children");
                    
                    // Override the application contextRoot by the webmodule's contextRoot
                    contextRoot = resolveWebModuleContextRoot(componentName, children);
                    resolveWebModuleSubComponents(name, componentName, contextRoot, httpContext);
                } else if (SERVLET.equals(subComponent.getValue())) {
                    httpContext.add(new Servlet(componentName, contextRoot));
                }
            }
        }

        return httpContext;
    }

    /**
     * Undeploy the component
     *
     * @param name - application name form - form that include the target & operation fields
     *
     * @return resultMap
     */
    public Map<String, Object> doUndeploy(String name, FormDataMultiPart form) {
        return getClientUtil().POSTMultiPartRequest(APPLICATION_RESOURCE.replace("{name}", name), form);
    }

    /**
     * Verify if the DAS is running or not.
     */
    public boolean isDASRunning() {
        try {
            getClientUtil().GETRequest("");
        } catch (ProcessingException clientEx) {
            if (clientEx.getCause().getClass().equals(ConnectException.class)) {
                // We were unable to connect to the DAS through Jersey
                return false;
            }
        }
        
        return true;
    }

    /**
     * Get the standalone servers list associated with the DAS
     *
     * @return map of standalone servers
     */
    private Map<String, String> getServersList() {
        return getClientUtil().getChildResources(STANDALONE_SERVER_INSTANCES);
    }

    /**
     * Get the list of clusters
     *
     * @param none
     * @return map of clusters
     */
    private Map<String, String> getClustersList() {
        return getClientUtil().getChildResources(CLUSTERED_SERVER_INSTANCES);
    }

    /**
     * Get the context root associated with the application
     *
     * @param name - application name
     *
     * @return contextRoot attribute of the application
     */
    private String getApplicationContextRoot(String name) {
        // Pull the contextRoot from the application's attributes
        return getClientUtil().getAttributes(APPLICATION_RESOURCE.replace("{name}", name))
                              .get("contextRoot");
    }

    private String resolveWebModuleContextRoot(String componentName, List<Map<String, Object>> modules) {
        for (Map<String, Object> module : modules) {
            
            @SuppressWarnings("unchecked")
            Map<String, String> moduleProperties = (Map<String, String>) module.get("properties");
            
            if (moduleProperties != null && !moduleProperties.isEmpty()) {
                String moduleInfo = moduleProperties.get("moduleInfo");
                if (moduleInfo.startsWith(componentName)) {
                    
                    // Get the webmodule's contextRoot
                    // The moduleInfo property has the format - moduleArchiveURI:moduleType:contextRoot
                    // The contextRoot is extracted, and removed of any prefixed slash.
                    String contextRoot = moduleInfo.split(":")[2];
                    
                    return contextRoot.indexOf("/") > -1 ? contextRoot.substring(contextRoot.indexOf("/"))
                        : contextRoot;
                }
            } else {
                throw new PayaraClientException("Could not resolve the web-module contextRoot");
            }
        }
        
        return null;
    }

    /**
     * Lookup the Servlets of the WebModule and put them in the httpContext associated with the
     * application
     *
     * @param name - application name
     * @param module - webmodule name
     * @param context - contextRoot of the web-module
     * @param httpContext - httpContext to be updated
     */
    private void resolveWebModuleSubComponents(String name, String module, String context, HTTPContext httpContext) {
        
        // Fetch the list of SubComponents of the application

        Set<Entry<String, String>> subComponents = 
            getProperties(
                clientUtil
                    .GETRequest(APPLICATION_SERVLETS.replace("{application}", name).replace("{module}", module))
            ).entrySet();
        
        for (Entry<String, String> subComponent : subComponents) {
            httpContext.add(new Servlet(subComponent.getKey(), context));
        }
    }
    
    @SuppressWarnings("unchecked")
    private Map<String, String> getProperties(Map<String, Object> responseMap) {
        return (Map<String, String>) responseMap.get("properties");
    }

    /**
     * Get the list of server instances of the cluster
     *
     * @param target
     * @return server instances map
     */
    protected Map<String, String> getServerInstances(String target) {
        return clientUtil.getChildResources(MEMBER_SERVERS_RESOURCE.replace("{target}", target));
    }

    /**
     * Get the serverAttributes map of a server
     *
     * @param name of the server
     * @return serverAttributes map
     * nodeRef:		- reference to the node object
     * configRef:		- reference to the server's configuration object
     * ...
     */
    protected Map<String, String> getServerAttributes(String server) {
        return clientUtil.getAttributes(SERVER_RESOURCE.replace("{server}", server));
    }

    /**
     * Get the clusterAttributes map of a cluster
     *
     * @param name of the cluster
     * @return serverAttributes map
     * configRef:      - reference to the cluster's configuration object
     * ...
     */
    protected Map<String, String> getClusterAttributes(String cluster) {
        return clientUtil.getAttributes(CLUSTER_RESOURCE.replace("{cluster}", cluster));
    }

    /**
     * Get the HOST address (IP or name) of the node associated with the server
     *
     * @param serverAttributes name
     * @return nodeAttributes map
     */
    protected String getHostAddress(Map<String, String> serverAttributes) {
        String nodeHost = clientUtil.getAttributes(NODE_RESOURCE.replace("{node}", serverAttributes.get("nodeRef"))).get("nodeHost");

        // If the host address returned by DAS was "localhost", it could be "localhost" in the context of DAS, but not Arquillian.
        // This would result in Arquillian connecting to localhost, even though the DAS (and it's localhost) is on a separate machine.
        // This is the case when the Glassfish installer or asadmin creates a localhost node with node-host set to "localhost" instead of a FQDN.
        // Variants of "localhost" like "127.0.0.1" or ::1 are not addressed, as the installer/asadmin does not appear to set the node-host to such values.
        // In such a scenario, the adminHost (DAS) known to Arquillian (from arquillian.xml) will be used as the nodeHost.
        // All conditions are addressed:
        // 1. If adminHost is "localhost", and the node-host registered in DAS is "localhost", then the node-host is set to "localhost". No harm done.
        // 2. If adminHost is not "localhost", and the node-host registered in DAS is "localhost", then the node-host value will be set to the same as adminHost.
        // Prevents Arquillian from connecting to a wrong address (localhost) to run the tests via ArquillianTestRunner.
        // 3. If adminHost is "localhost" and the node-host registered in DAS is not "localhost", then the value from DAS will be used.
        // 4. If adminHost is not "localhost" and the node-host registered in DAS is not "localhost", then the value from DAS will be used.
        if (nodeHost.equals("localhost")) {
            nodeHost = configuration.getAdminHost();
        }
        
        return nodeHost;
    }
    

    /**
     * Get the port number defined as a system property in a configuration.
     *
     * @param attributes
     *     The attributes which references the configuration (server or
     *     cluster configuration)
     * @param propertyName
     *     The name of the system property to resolve
     *
     * @return The port number stored in the system property
     */
    private int getSystemProperty(Map<String, String> attributes, String propertyName) {
        return Integer.parseInt(
            clientUtil.getAttributes(SYSTEM_PROPERTY.replace("{config}", attributes.get("configRef")).replace("{system-property}", propertyName))
                      .get("value"));
    }
    

    /**
     * Get the port number defined as a system property in a configuration, and
     * overridden at the level of the server instance.
     *
     * @param server
     *     The name of the server instance
     * @param propertyName
     *     The name of the system property to resolve
     * @param defaultValue
     *     The default port number to be used, in case the system
     *     property is not overridden
     *
     * @return The port number stored in the system property
     */
    private int getServerSystemProperty(String server, String propertyName, int defaultValue) {
        String value = clientUtil.getAttributes(SERVER_PROPERTY.replace("{server}", server).replace("{system-property}", propertyName))
                                 .get("value");

        return value != null ? Integer.parseInt(value) : defaultValue;
    }

    /**
     * Get the http/https port number of the server instance
     * <p>
     * The attribute is optional, It is generated by the Glassfish server
     * if we have more then one server instance on the same node.
     *
     * @param server name
     * secure: false - http port number, true - https port number
     * @return http/https port number. If the attribute is not defined, gives back the default port
     */
    protected int getServerInstanceHttpPort(String server, int default_port, boolean secure) {
        String httpListener = (!secure) ? "HTTP_LISTENER_PORT" : "HTTP_SSL_LISTENER_PORT";

        String value = getClientUtil().getAttributes(HTTP_LISTENER_INS.replace("{server}", server).replace("{http-listener}", httpListener))
                                      .get("value");

        return value != null ? Integer.parseInt(value) : default_port;
    }

    /**
     * Obtains the list of virtual servers associated with the deployment
     * target. This method omits '__asadmin' in the result, as no deployments
     * can target this virtual server.
     *
     * @param attributes
     *     The attributes which references the configuration (server or
     *     cluster configuration)
     *
     * @return A list of virtual server names that have been found in the
     * server/cluster configuration
     */
    private List<String> getVirtualServers(Map<String, String> attributes) {
        
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> virtualServers = (List<Map<String, Object>>)
            clientUtil.GETRequest(
                        VIRTUAL_SERVERS.replace("{config}", attributes.get("configRef"))
                                       .replace("{target}", attributes.get("name")))
                      .get("children");
        
        List<String> virtualServerNames = new ArrayList<String>();
        for (Map<String, Object> virtualServer : virtualServers) {
            String virtualServerName = (String) virtualServer.get("message");
            if (!virtualServerName.equals("__asadmin")) {
                virtualServerNames.add(virtualServerName);
            }
        }
        
        return virtualServerNames;
    }

    /**
     * Obtains the list of all network listeners associated with the list of provided virtual
     * servers.
     *
     * @param attributes The attributes which references the configuration (server or cluster
     *            configuration)
     * @param virtualServers The {@link List} of all virtual servers whose the listeners must be
     *            retrieved
     *
     * @return The list of all listener names associated with the provided list of virtual servers
     */
    private List<String> getNetworkListeners(Map<String, String> attributes, List<String> virtualServers) {
        List<String> networkListeners = new ArrayList<String>();
        
        for (String virtualServer : virtualServers) {
            String[] listeners = 
                clientUtil.getAttributes(
                            VIRTUAL_SERVER.replace("{config}", attributes.get("configRef"))
                                          .replace("{virtualServer}", virtualServer))
                          .get("networkListeners")
                          .split(",");
            
            for (String listener : listeners) {
                networkListeners.add(listener.trim());
            }
        }
        
        return networkListeners;
    }

    /**
     * Obtains the value of a HTTP/HTTPS network listener, as stored in the Payara configuration.
     *
     * @param attributes The attributes which references the configuration (server or cluster
     *            configuration)
     * @param networkListeners The {@link List} of network listeners among which one will be chosen
     * @param secure Should a listener with a secure protocol be chosen?
     *
     * @return The value of the port number stored in the chosen listener configuration. This may be
     *         parseable as a number, but not necessarily so. Sometimes a system property might be
     *         returned.
     */
    private String getActiveHttpPort(Map<String, String> attributes, List<String> networkListeners, boolean secure) {
        for (String networkListener : networkListeners) {
            Map<String, String> listenerAttributes = clientUtil.getAttributes(LISTENER.replace("{config}", attributes.get("configRef"))
                                                                                      .replace("{listener}", networkListener));
            
            if (!parseBoolean(listenerAttributes.get("enabled"))) {
                continue;
            }
            
            String port = listenerAttributes.get("port");
            String protocolName = listenerAttributes.get("protocol");
            boolean secureProtocol = isSecureProtocol(attributes, protocolName);
            
            if (secure && secureProtocol) {
                return port;
            } else if (!secure && !secureProtocol) {
                return port;
            }
        }
        
        return null;
    }

    /**
     * Determines whether the protocol associated with the listener is a secure
     * protocol or not.
     *
     * @param attributes
     *     The attributes which references the configuration (server or
     *     cluster configuration)
     * @param protocolName
     *     The name of the protocol
     *
     * @return A boolean value indicating whether a protocol is secure or not
     */
    private boolean isSecureProtocol(Map<String, String> attributes, String protocolName) {
        return parseBoolean(
            clientUtil.getAttributes(PROTOCOL.replace("{config}", attributes.get("configRef"))
                                             .replace("{protocol}", protocolName))
                      .get("securityEnabled"));
    }

    /**
     * Get the port number of a network listener. Firstly, this method parses the provided String as
     * a number. If this fails, the provided String is parsed as a system property stored in the
     * format - <blockquote>${systemProperty}</blockquote>. The value of the referenced system
     * property is then read from the GlassFish configuration.
     *
     * @param attributes The attributes which references the configuration (server or cluster
     *            configuration)
     * @param serverName The name of the server instance
     * @param portNum The port number or a system property that stores the port number
     *
     * @return The port number as stored in the network listener configuration or in the system
     *         property
     */
    private int getPortValue(Map<String, String> attributes, String serverName, String portNum) {
        try {
            return Integer.parseInt(portNum);
        } catch (NumberFormatException formatEx) {
            Matcher matcher = compile(SYSTEM_PROPERTY_REGEX).matcher(portNum);
            if (matcher.find()) {
                String propertyName = matcher.group(1);
                return getServerSystemProperty(serverName, propertyName, getSystemProperty(attributes, propertyName));
            }
        }
        
        return -1;
    }

    private CommonPayaraConfiguration getConfiguration() {
        return configuration;
    }

    private String getTarget() {
        return target;
    }

    private PayaraClientUtil getClientUtil() {
        return clientUtil;
    }

    /**
     * Get the URL of the DAS server
     *
     * @return URL
     */
    private String getDASUrl() {
        return DASUrl;
    }

    /**
     * The GoF Strategy pattern is used to implement specific algorithm
     * by server type (Admin, Standalone or Clustered server)
     * <p>
     * The attribute is optional, It is generated by the Payara server
     * if we have more then one server instance on the same node or
     * explicitly defined by the create command parameter.
     *
     * @param server
     *     name
     *     secure: false - http port number, true - https port number
     *
     * @return http/https port number. If the attribute is not defined, gives back the default port
     */
    abstract class ServerStartegy {

        /**
         * Address list of the node(s) on GlassFish Appserver
         */
        private List<NodeAddress> nodes = new ArrayList<NodeAddress>();

        protected PayaraClientService glassFishClient;

        protected ServerStartegy() {
        }

        protected List<NodeAddress> getNodes() {
            return nodes;
        }

        protected void setNodes(List<NodeAddress> nodes) {
            this.nodes = nodes;
        }

        protected void addNode(NodeAddress node) {
            nodes.add(node);
        }

        protected PayaraClientService getGlassFishClient() {
            return glassFishClient;
        }

        /**
         * Get the the node address list associated with the target
         *
         * @return list of node address objects
         */
        protected abstract List<NodeAddress> getNodeAddressList();
    }

    class AdminServer extends ServerStartegy {

        public AdminServer() {
            super();
        }

        @Override
        public List<NodeAddress> getNodeAddressList() {
            String nodeHost = "localhost"; // default host
            setNodes(new ArrayList<NodeAddress>());

            // Getting the server attributes is happening too fast.  The admin server hasn't started yet.
            int count = 10;
            Map<String, String> serverAttributes = getServerAttributes(ADMINSERVER);
            while (serverAttributes.size() == 0 && count-- > 0) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ignore) {
                }
                serverAttributes = getServerAttributes(ADMINSERVER);
            }

            // Get the host address of the Admin Server
            nodeHost = (String) getConfiguration().getAdminHost();

            // Get the virtual servers and the associated network listeners for the DAS.
            // We'll not verify if the listeners are bound to private IP
            // addresses, or reachable from the Arquillian test client.
            List<String> virtualServers = getVirtualServers(serverAttributes);
            List<String> networkListeners = getNetworkListeners(serverAttributes, virtualServers);
            String httpPortNum = getActiveHttpPort(serverAttributes, networkListeners, false);
            String httpsPortNum = getActiveHttpPort(serverAttributes, networkListeners, true);

            int httpPort = getPortValue(serverAttributes, getTarget(), httpPortNum);
            // A HTTPS listener might not exist in the DAS config.
            // And Arquillian requires a HTTP port for now.
            // So, we'll parse the HTTPS config conditionally.
            int httpsPort = -1;
            if (httpsPortNum != null && !httpsPortNum.equals("")) {
                httpsPort = getPortValue(serverAttributes, getTarget(), httpsPortNum);
            }

            addNode(new NodeAddress(ADMINSERVER, nodeHost, httpPort, httpsPort));

            return getNodes();
        }
    }

    class StandaloneServer extends ServerStartegy {

        public StandaloneServer() {
            super();
        }

        @Override
        public List<NodeAddress> getNodeAddressList() {
            String nodeHost = "localhost"; // default host
            setNodes(new ArrayList<NodeAddress>());

            Map<String, String> serverAttributes = getServerAttributes(getTarget());

            // Get the host address of the Admin Server
            nodeHost = getHostAddress(serverAttributes);

            // Get the virtual servers and the associated network listeners for the DAS.
            // We'll not verify if the listeners are bound to private IP addresses,
            // or reachable from the Arquillian test client.
            List<String> virtualServers = getVirtualServers(serverAttributes);
            List<String> networkListeners = getNetworkListeners(serverAttributes, virtualServers);
            String httpPortNum = getActiveHttpPort(serverAttributes, networkListeners, false);
            String httpsPortNum = getActiveHttpPort(serverAttributes, networkListeners, true);

            int httpPort = getPortValue(serverAttributes, getTarget(), httpPortNum);
            // A HTTPS listener might not exist in the instance config.
            // And Arquillian requires a HTTP port for now.
            // So, we'll parse the HTTPS config conditionally.
            int httpsPort = -1;
            if (httpsPortNum != null && !httpsPortNum.equals("")) {
                httpsPort = getPortValue(serverAttributes, getTarget(), httpsPortNum);
            }

            addNode(new NodeAddress(getTarget(), nodeHost, httpPort, httpsPort));
            return getNodes();
        }
    }

    class ClusterServer extends ServerStartegy {

        public ClusterServer() {
            super();
        }

        @Override
        public List<NodeAddress> getNodeAddressList() {
            String nodeHost = "localhost"; // default host
            setNodes(new ArrayList<NodeAddress>());
            Map<String, String> serverAttributes;

            // Get the REST resource for the cluster attributes, to reference the config-ref later
            Map<String, String> clusterAttributes = getClusterAttributes(getTarget());
            // Fetch the list of server instances of the cluster
            Map<String, String> serverInstances = getServerInstances(getTarget());

            // Get the virtual servers and the associated network listeners for the cluster.
            // GlassFish clusters are homogeneous and the virtual servers and network listeners
            // will be present on every cluster instance; only port numbers for the listener may vary.
            // We'll not verify if the listeners are bound to private IP addresses,
            // or reachable from the Arquillian test client.
            List<String> virtualServers = getVirtualServers(clusterAttributes);
            List<String> networkListeners = getNetworkListeners(clusterAttributes, virtualServers);

            // Obtain a HTTP and a HTTPS port that have been enabled on the
            // virtual server.
            String httpPortNum = getActiveHttpPort(clusterAttributes, networkListeners, false);
            String httpsPortNum = getActiveHttpPort(clusterAttributes, networkListeners, true);

            for (Entry<String, String> serverInstance : serverInstances.entrySet()) {
                String serverName = serverInstance.getKey();

                serverAttributes = getServerAttributes(serverName);
                nodeHost = getHostAddress(serverAttributes);

                int httpPort = getPortValue(clusterAttributes, serverName, httpPortNum);
                
                // A HTTPS listener might not exist in the cluster config.
                // And Arquillian requires a HTTP port for now.
                // So, we'll parse the HTTPS config conditionally.
                int httpsPort = -1;
                if (httpsPortNum != null && !httpsPortNum.equals("")) {
                    httpsPort = getPortValue(clusterAttributes, serverName, httpsPortNum);
                }

                addNode(new NodeAddress(serverName, nodeHost, httpPort, httpsPort));
            }

            return getNodes();
        }
    }
}
