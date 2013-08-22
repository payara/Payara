/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.connectors.jms.system;

import java.util.logging.Logger;
import java.util.logging.Level;
import java.util.*;
import java.io.File;
import java.io.IOException;
import java.io.FileInputStream;
import java.beans.PropertyVetoException;

import com.sun.enterprise.connectors.jms.config.JmsHost;
import com.sun.enterprise.connectors.jms.config.JmsService;
import com.sun.enterprise.util.SystemPropertyConstants;
import com.sun.enterprise.config.serverbeans.*;
import com.sun.enterprise.connectors.jms.util.JmsRaUtil;
import com.sun.enterprise.admin.util.AdminConstants;
import com.sun.appserv.connectors.internal.api.ConnectorRuntimeException;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.internal.api.Globals;
import org.glassfish.api.ActionReport;
import com.sun.enterprise.util.StringUtils;
import com.sun.enterprise.universal.glassfish.ASenvPropertyReader;
import org.jvnet.hk2.config.ConfigSupport;
import org.jvnet.hk2.config.SingleConfigCode;
import org.jvnet.hk2.config.TransactionFailure;

/**
 * Defines an MQ addressList.
 *
 * @author Binod P.G
 */
public class MQAddressList {

    private static final Logger logger = Logger.getLogger(ActiveJmsResourceAdapter.JMS_MAIN_LOGGER);
    private String myName =
               System.getProperty(SystemPropertyConstants.SERVER_NAME);

    private List<MQUrl> urlList = new ArrayList<MQUrl>();

    private JmsService jmsService = null;
    //private AppserverClusterViewFromCacheRepository rep = null;
    private static String nodeHost = null;
    private String targetName = null;

    /**
     * Create an empty address list
     */
    public MQAddressList() {
        this(null);
    }

    /**
     * Use the provided <code>JmsService</code> to create an addresslist
     */
    public MQAddressList(JmsService service) {
        //use the server instance this is being run in as the target
        this(service, getServerName());
    }

    /**
     * Creates an instance from jmsService and resolves
     * values using the provided target name
     * @param targetName Represents the target for which the addresslist
     * needs to be created
     * @param service <code>JmsService</code> instance.
     */
    public MQAddressList(JmsService service, String targetName) {
        if (logger.isLoggable(Level.FINE))
            logFine(" init" + service + "target " + targetName);
        this.jmsService = service;
        this.targetName = targetName;
    }

    public void setJmsService (JmsService jmsService){
        this.jmsService = jmsService;
    }
    public void setTargetName(String targetName){
        this.targetName = targetName;
    }
    public void setInstanceName(String instanceName){
        myName = instanceName;
    }
    public void setup()throws Exception
    {
       if (isClustered() && (!this.jmsService.getType().equals(ActiveJmsResourceAdapter.REMOTE)) )
                    setup(true);
        else setup(false);
    }
    /**
     * Sets up the addresslist.
     */
    public void setup(boolean isClustered) throws Exception {
        try {
            if (isClustered) {
                //setup for LOCAL/EMBEDDED clusters.
                if (logger.isLoggable(Level.FINE))
                    logFine("setting up for cluster " +  this.targetName);
                setupClusterViewFromRepository();
                setupForCluster();
            } else {
                if (logger.isLoggable(Level.FINE))
                    logFine("setting up for SI/DAS " + this.targetName);
                if (isAConfig(targetName) || isDAS(targetName)) {
                    if (logger.isLoggable(Level.FINE))
                        logFine("performing default setup for DAS/remote clusters/PE instance" + targetName);
                    defaultSetup();
                } else {
                    logFine("configuring for Standalone EE server instance");
                    //resolve and add.
                    setupClusterViewFromRepository();
                    setupForStandaloneServerInstance();
                }
            }
        } catch (ConnectorRuntimeException ce) {
            throw new Exception(ce);
        }
    }

    private void setupClusterViewFromRepository() throws Exception {
        ServerContext context = Globals.get(ServerContext.class);
        Domain domain = Globals.get(Domain.class);
        String serverName = context.getInstanceName();
        Server server = domain.getServerNamed(serverName); //context.getConfigBean();
        //String domainurl = context.getServerConfigURL();
        //rep = new AppserverClusterViewFromCacheRepository(domainurl);
        try {
            nodeHost = getNodeHostName(server);
            logFine("na host" + nodeHost);
        } catch (Exception e) {
            if (logger.isLoggable(Level.FINE))
                logger.log(Level.FINE,"Exception while attempting to get nodeagentHost", e.getMessage());
            if (logger.isLoggable(Level.FINER))
                logger.log(Level.FINER, e.getMessage(), e);
        }
    }

    public String getNodeHostName(final Server as) throws Exception{
        String nodeRef = as.getNodeRef();
        Nodes nodes = Globals.get(Nodes.class);
        Node node = nodes.getNode(nodeRef);

         if (node != null)
         {
            if (node.getNodeHost() != null) return node.getNodeHost();
            //localhostNode
            else if (node.isDefaultLocalNode())
            {
                 String hostName = getHostNameFromDasProperties();
                 if ("localhost".equals(hostName))
                 //instance is co-located on same machine as DAS. Hence read host name from system property
                 return System.getProperty(SystemPropertyConstants.HOST_NAME_PROPERTY);
            }
         }


        return null;
    }

    String dasPropertiesHostName= null;
    public String getHostNameFromDasProperties()
    {
        if (dasPropertiesHostName != null) return dasPropertiesHostName;

        String agentsDirPath = getSystemProperty(
                SystemPropertyConstants.AGENT_ROOT_PROPERTY);

        if(!StringUtils.ok(agentsDirPath))
            //return agentsDirPath;
        {
            String installRootPath = getSystemProperty(
                SystemPropertyConstants.INSTALL_ROOT_PROPERTY);

            if(!StringUtils.ok(installRootPath))
                installRootPath = System.getProperty(
                    SystemPropertyConstants.INSTALL_ROOT_PROPERTY);
            agentsDirPath = installRootPath + File.separator + "nodes";
        }
       // if(!StringUtils.ok(installRootPath))
         //   throw new CommandException("Agent.noInstallDirPath");

        String dasPropsFilePath = agentsDirPath + File.separator + "agent" + File.separator + "config";
        File dasPropsFile = new File(dasPropsFilePath, "das.properties");

        Properties dasprops = new Properties();
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(dasPropsFile);
            dasprops.load(fis);
            fis.close();
            fis = null;
            dasPropertiesHostName = dasprops.getProperty("agent.das.host");
            return dasPropertiesHostName;
         } catch (IOException ioex) {
            ;
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException cex) {
                    // ignore it
                }
            }
        }
        return null;
    }

    Map<String,String> systemProps = null;

    protected String getSystemProperty(String propertyName)
    {
       if (systemProps == null)
         systemProps = Collections.unmodifiableMap(new ASenvPropertyReader().getProps());

        return systemProps.get(propertyName);
    }

    public String getMasterBroker(String clustername) {
    String masterbrk = null;
    //if (rep != null) {
        try {
        JmsHost mb = getMasterJmsHostInCluster(clustername);
        JmsService js = getJmsServiceForMasterBroker
                                 (clustername);
        MQUrl url = createUrl(mb, js);
        masterbrk = url.toString();
        if (logger.isLoggable(Level.FINE))
            logger.log(Level.FINE, "Master broker obtained is " + masterbrk);
        }
        catch (Exception e) {
        logger.log(Level.SEVERE, "Cannot obtain master broker");
        logger.log(Level.SEVERE, e.getMessage(), e);
        }
    //}
    return masterbrk;
    }

      private JmsService getJmsServiceForMasterBroker(String clusterName)  {
         Domain domain = Globals.get(Domain.class);
         Cluster cluster = domain.getClusterNamed(clusterName);

        final Server[] buddies = getServersInCluster(cluster);
        final Config cfg =  getConfigForServer(buddies[0]);
        return cfg.getExtensionByType(JmsService.class);
	}

    private Config getConfigForServer(Server server){

         String cfgRef = server.getConfigRef();
         return getConfigByName(cfgRef);
    }
    private Config getConfigByName(String cfgRef){
         Domain domain = Globals.get(Domain.class);
         Configs configs = domain.getConfigs();
                List configList = configs.getConfig();
                for(int i=0; i < configList.size(); i++){
                    Config config = (Config)configList.get(i);
                    if (config.getName().equals(cfgRef))
                        return config;
        }
        return null;
    }
     private JmsHost getMasterJmsHostInCluster(String clusterName) throws Exception {
        Domain domain = Globals.get(Domain.class);
        Cluster cluster = domain.getClusterNamed(clusterName);

         /*
           Since GF 3.1 - Added a new way to configure the master broker
           Check if a master broker has been configured by looking at jmsService.getMasterBroker
           If it is configured, return th
           If not, use the first configured server in the cluster list as the master broker
         */
         Config config = domain.getConfigNamed(cluster.getConfigRef());
         JmsService jmsService = config.getExtensionByType(JmsService.class);
         Server masterBrokerInstance = null;

         String masterBrokerInstanceName = jmsService.getMasterBroker();
         if (masterBrokerInstanceName != null){
             masterBrokerInstance = domain.getServerNamed(masterBrokerInstanceName);
         }
         else{
            Server[] buddies = getServersInCluster(cluster);
            // return the first valid host
			// there may be hosts attached to an NA that is down
            if (buddies.length > 0){
                masterBrokerInstance = buddies[0];
            }
        }
        final JmsHost copy = getResolvedJmsHost(masterBrokerInstance);
	    if (copy != null)
            return copy;
        else
	        throw new RuntimeException("No JMS hosts available to select as Master");
    }

    public Cluster getClusterByName(String clusterName)
    {
         Domain domain = Globals.get(Domain.class);
         Clusters clusters = domain.getClusters();
         List clusterList = clusters.getCluster();

         for (int i =0; i < clusterList.size(); i++){
             Cluster cluster = (Cluster)clusterList.get(i);
             if (cluster.getName().equals(clusterName))
                         return cluster;
         }
        return null;
    }
    public Server[] getServersInCluster(String clusterName){
        Cluster cluster = getClusterByName(clusterName);
        return getServersInCluster(cluster);
    }
    public Server[] getServersInCluster(Cluster cluster)
    {
        //first ensure that the cluster exists
        //Cluster cluster = ClusterHelper.getClusterByName(configContext, clusterName);

        //Now fetch the server instances in the cluster.
        List servers = cluster.getInstances();


       // List serverRefs = cluster.getServerRef();

        Server[] result = new Server[servers.size()];
       // Domain domain = Globals.get(Domain.class);

        for (int i = 0; i <  servers.size(); i++) {
            result[i] = (Server) servers.get(i); //.getServerNamed(((ServerRef)serverRefs.get(i)).getRef());

          //  try {
            //} catch (ConfigException ex) {
              //  throw new ConfigException(_strMgr.getString("noSuchClusterInstance",
                //    clusterName, serverRefs[i].getRef()));
            //}
        }
        return result;
    }

    public boolean isDAS(String targetName)  {
        if (isAConfig(targetName)) {
            return false;
        }
        return getServerByName(targetName).isDas();
    }

    public boolean isAConfig(String targetName)  {
        //return ServerHelper.isAConfig(getAdminConfigContext(), targetName);
        final Config config = getConfigByName(targetName);
        return (config != null ? true : false);
    }


    /**
     * Gets the admin config context associated with this server instance
     * Usage Notice: Use this only for operations that are performed in DAS
     * and requires the admin config context
     */
   /* private ConfigContext getAdminConfigContext() {
        return com.sun.enterprise.admin.server.core.AdminService.
                   getAdminService().getAdminContext().getAdminConfigContext();
    } */

    /**
     * Setup addresslist for Standalone server instance in EE
     */
    private void setupForStandaloneServerInstance() throws Exception {
        if (jmsService.getType().equals(ActiveJmsResourceAdapter.REMOTE)) {
            logFine("REMOTE Standalone server instance and hence use default setup");
            defaultSetup();
        } else {
            //For LOCAL or EMBEDDED standalone server instances, we need to resolve
            //the JMSHost
            logFine("LOCAL/EMBEDDED Standalone server instance");
            JmsHost host = getResolvedJmsHostForStandaloneServerInstance(this.targetName);
            MQUrl url = createUrl(host);
            urlList.add(url);
        }
    }

    /**
     * Default setup concatanates all JMSHosts in a JMSService to create the address list
     */
    private void defaultSetup() throws Exception {
        logFine("performing defaultsetup");
        JmsService jmsService = Globals.get(JmsService.class);
        List hosts = jmsService.getJmsHost();
        for (int i=0; i < hosts.size(); i++) {
            MQUrl url = createUrl((JmsHost)hosts.get(i));
            urlList.add(url);
        }
    }

    /**
     * Setup the address list after calculating the JMS hosts
     * belonging to the local appserver cluster members.
     * For LOCAL/EMBEDDED clusters the MQ broker corresponding
     * to "this" server instance needs to be placed ahead
     * of the other brokers of the other siblings in the AS
     * cluter to enable sticky connection balancing by MQ.
     */
    private void setupForCluster() throws Exception {
        java.util.Map<String,JmsHost> hostMap =
            getResolvedLocalJmsHostsInMyCluster(true);
        //First add my jms host.
        JmsHost jmsHost = hostMap.get(myName);
        MQUrl myUrl = createUrl(jmsHost, nodeHost);
        urlList.add(myUrl);
        hostMap.remove(myName);

        // Add all buddies to URL.
        for (JmsHost host : hostMap.values() ) {
            MQUrl url = createUrl(host);
            urlList.add(url);
        }
    }

    public Map<String, JmsHost> getResolvedLocalJmsHostsInMyCluster(final boolean includeMe) {
        final Map<String, JmsHost> map = new HashMap<String, JmsHost> ();
         Cluster cluster = getClusterForServer( myName);
        if (cluster != null){
            final Server[] buddies = getServersInCluster(cluster);
            for (final Server as : buddies) {
                if (!includeMe && myName.equals(as.getName()))
                    continue;

                JmsHost copy = null;
                try {
                            copy  = getResolvedJmsHost(as);
                    } catch (Exception e) {
                      //e.printStackTrace();
                }
                map.put(as.getName(), copy);

            }
        }
        return ( map );
    }

    public Cluster getClusterForServer(String instanceName){
         Domain domain = Globals.get(Domain.class);
         Clusters clusters = domain.getClusters();
         List clusterList = clusters.getCluster();

         for (int i =0; i < clusterList.size(); i++){
             Cluster cluster = (Cluster)clusterList.get(i);
             if (isServerInCluster(cluster, instanceName))
                         return cluster;
         }
        return null;
    }

    private boolean isServerInCluster (Cluster cluster, String instanceName){
        List instances = cluster.getInstances();
        for (int i=0; i < instances.size(); i++){
            if(instanceName.equals(((Server)instances.get(i)).getName()))
                return true;
        }
        return false;
    }
    /**
     * Creates a String representation of address list from
     * array list. In short, it is a comma separated list.
     * Actual syntax of an MQ url is inside MQUrl class.
     *
     * @return AddressList String
     * @see MQUrl
     */
    public String toString() {
        StringBuilder builder = new StringBuilder();

        Iterator it = urlList.iterator();
        if (it.hasNext()) {
            builder.append(it.next().toString());
        }

        while (it.hasNext()) {
            builder.append(",").append(it.next().toString());
        }

        String s = builder.toString();
        if (logger.isLoggable(Level.FINE))
            logFine("toString returns :: " + s);
        return s;
    }

    /**
     * Creates an instance of MQUrl from JmsHost element in
     * the dtd and add it to the addresslist.
     *
     * @param host An instance of <code>JmsHost</code> object.
     */
    public void addMQUrl(JmsHost host) {
        MQUrl url = createUrl(host);
        urlList.add(url);
    }

    /**
     * Deletes the url represented by the JmsHost from the AddressList.
     *
     * @param host An instance of <code>JmsHost</code> object.
     */
    public void removeMQUrl(JmsHost host) {
        MQUrl url = createUrl(host);
        urlList.remove(url);
    }

    /**
     * Updates the information about the <code>JmsHost</code>
     * in the address list.
     *
     * @param host An instance of <code>JmsHost</code> object.
     */
    public void updateMQUrl(JmsHost host) {
        MQUrl url = createUrl(host);
        urlList.remove(url);
        urlList.add(url);
    }

    private MQUrl createUrl(JmsHost host) {
        return createUrl(host, this.jmsService);
    }

    private MQUrl createUrl(JmsHost host, String overridedHostName) {
        return createUrl(host, this.jmsService, overridedHostName);
    }

    public static MQUrl createUrl(JmsHost host, JmsService js) {
        return createUrl(host, js, null);
    }

    public static MQUrl createUrl(JmsHost host, JmsService js, String overridedHostName) {
        try {
        String name = host.getName();
        String hostName = host.getHost();
        // For LOCAL/EMBEDDED Clustered instances and
        // standalone server instances, use
        // their nodeagent's hostname as the jms host name.
        if (overridedHostName != null && !overridedHostName.trim().equals("")) {
           hostName = overridedHostName;
        }

        String port = host.getPort();
        MQUrl url = new MQUrl(name);
        url.setHost(hostName);
        url.setPort(port);
        if (js != null) {
            String scheme = js.getMqScheme();
            if (scheme != null && !scheme.trim().equals("")) {
                url.setScheme(scheme);
            }

            String service = js.getMqService();
            if (service != null && !service.trim().equals("")) {
                url.setService(service);
            }
        }
        return url;
        } catch (Exception ce) {
            ce.printStackTrace();
        }
        return null;
    }

     //Used to get resolved local JmsHost for a standalone server instance
    private JmsHost getResolvedJmsHostForStandaloneServerInstance(
                                         String serverName) throws Exception {
        if (logger.isLoggable(Level.FINE))
            logFine(" getresolved " + serverName);
       //ConfigContext con =  getAdminConfigContext();
       Server serverInstance = getServerByName(serverName);
       if (logger.isLoggable(Level.FINE))
           logFine("serverinstace " + serverInstance);
       JmsHost jmsHost = getResolvedJmsHost(serverInstance);
       return jmsHost;
    }

    private Server getServerByName(String serverName){
        Domain domain = Globals.get(Domain.class);
        Servers servers = domain.getServers();
        List serverList = servers.getServer();

        for (int i=0; i < serverList.size(); i++){
            Server server = (Server) serverList.get(i);
            if(serverName.equals(server.getName()))
                return server;
        }
           return null;
    }


    private JmsHost getResolvedJmsHost(Server as) throws Exception{
        if (as == null) {
            return null;
        }
        if (logger.isLoggable(Level.FINE))
            logFine("getResolvedJmsHost " + as);
//        final JmsService jmsService     = Globals.get(JmsService.class);
  //      JmsHost jmsHost                 = null;
    //    if (JMSServiceType.LOCAL.toString().equals(jmsService.getType())	|| JMSServiceType.EMBEDDED.toString().equals(jmsService.getType())) {
      //      jmsHost = getDefaultJmsHost(jmsService);
        //}
       // return ( jmsHost );

        JmsHost jmsHost   = getResolvedLocalJmsHostInServer(as);
        JmsHost copy      = createJmsHostCopy(jmsHost, as);

        String hostName = getNodeHostName(as);
        String port = JmsRaUtil.getJMSPropertyValue(as) ;//"JMS_PROVIDER_PORT", "7676");
        copy.setHost(hostName);
        copy.setPort(port);

        return copy;
    }

    private JmsHost createJmsHostCopy(final JmsHost jmsHost, final Server server)
        {
        JmsHost jmsHostCopy = new JmsHostWrapper();


        try {
            jmsHostCopy.setAdminPassword(jmsHost.getAdminPassword());
            jmsHostCopy.setAdminUserName(jmsHost.getAdminUserName());
            jmsHostCopy.setName(jmsHost.getName());
            jmsHostCopy.setHost(jmsHost.getHost());
            jmsHostCopy.setPort(jmsHost.getPort());
        } catch(Exception tfe) {
            tfe.printStackTrace();//todo: handle this exception
        }
        return jmsHostCopy;
     }
    /*static JmsHost jmsHostcopy = null;
     private JmsHost createJmsHostCopy(final JmsHost jmsHost, final Server server)
        {
        try {
            //jmsHost.deepCopy();
             ConfigSupport.apply(new SingleConfigCode<JmsService>() {
                public Object run(JmsService param) throws PropertyVetoException, TransactionFailure {

                    final JmsHost jmsHost = param.createChild(JmsHost.class); //TODO: need a way to create a JmsHost instance
                    jmsHost.setAdminPassword(jmsHost.getAdminPassword());
                    jmsHost.setAdminUserName(jmsHost.getAdminUserName());
                    jmsHost.setName(jmsHost.getName());
                    jmsHost.setHost(jmsHost.getHost());
                    jmsHost.setPort(jmsHost.getPort());
                    MQAddressList.jmsHostcopy = jmsHost;
                    return jmsHost;
                }
            }, getConfigForServer(server).getJmsService());
        } catch(TransactionFailure tfe) {
            //tfe.printStackTrace();//todo: handle this exception
        }
        return jmsHostcopy;
     }*/
     private JmsHost getResolvedLocalJmsHostInServer(final Server server)  {
        Config config = getConfigForServer(server);
        if (config != null)
        {
            JmsService jmsService = config.getExtensionByType(JmsService.class);
            JmsHost jmsHost = null;
             if (JMSServiceType.LOCAL.toString().equals(jmsService.getType())	|| JMSServiceType.EMBEDDED.toString().equals(jmsService.getType())) {
            jmsHost = getDefaultJmsHost(jmsService);
            }
            return ( jmsHost );

        }
         return null;
     }

    public JmsHost getDefaultJmsHost(JmsService jmsService){
        String defaultJmsHost=   jmsService.getDefaultJmsHost();
        List <JmsHost> jmsHosts = jmsService.getJmsHost();
        JmsHost jmsHost = null;
        if (defaultJmsHost != null && ! defaultJmsHost.equals("") && jmsHosts != null && jmsHosts.size()> 0){
            for (int i=0; i <jmsHosts.size(); i++)
                   if (defaultJmsHost.equals((jmsHosts.get(i)).getName()))
                          return (JmsHost)jmsHosts.get(i);
            }

             if(jmsHosts != null && jmsHosts.size() >0 )
                     jmsHost = jmsHosts.get(0);
             else
                jmsHost = Globals.get(JmsHost.class);

        return jmsHost;
    }

    public boolean isClustered()  {
        Domain domain = Globals.get(Domain.class);
        Clusters clusters = domain.getClusters();
        if (clusters == null) return false;

        List clusterList = clusters.getCluster();

        return JmsRaUtil.isClustered(clusterList, myName);
    }

    private static String getServerName() {
        String serverName=System.getProperty(SystemPropertyConstants.SERVER_NAME);
        return serverName;
    }

    private void logFine(String s) {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "MQAddressList :: " + s);
        }
    }

    public int getSize() {
        if (this.urlList != null) {
            return this.urlList.size();
        } else {
            return 0;
        }
    }
    enum JMSServiceType {
        LOCAL,
        REMOTE,
        EMBEDDED
    }


}

