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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
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
 */

// Portions Copyright [2017-2023] [Payara Foundation and/or its affiliates]

package com.sun.enterprise.config.serverbeans;

import com.sun.enterprise.config.util.ConfigApiLoggerInfo;
import com.sun.enterprise.util.StringUtils;
import fish.payara.enterprise.config.serverbeans.DGServerRef;
import fish.payara.enterprise.config.serverbeans.DeploymentGroup;
import fish.payara.enterprise.config.serverbeans.DeploymentGroups;
import java.beans.PropertyVetoException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import jakarta.validation.constraints.NotNull;
import org.glassfish.api.admin.config.ApplicationName;
import org.glassfish.api.admin.config.PropertiesDesc;
import org.glassfish.api.admin.config.PropertyDesc;
import org.glassfish.api.admin.config.ReferenceContainer;
import org.glassfish.quality.ToDo;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.ConfigExtensionMethod;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.DuckTyped;
import org.jvnet.hk2.config.Element;
import org.jvnet.hk2.config.types.Property;
import org.jvnet.hk2.config.types.PropertyBag;


/**
 * Top level Domain Element that includes applications, resources, configs,
 * servers, clusters and node-agents, load balancer configurations and load
 * balancers. node-agents and load balancers are SE/EE related entities only.
 *
 */
@Configured
public interface Domain extends ConfigBeanProxy, PropertyBag, SystemPropertyBag, ConfigLoader  {

    public static final String DOMAIN_NAME_PROPERTY = "administrative.domain.name";


    /**
     * Gets the value of the applicationRoot property.
     *
     * For PE this defines the location where applications are deployed
     *
     * @return possible object is {@link String }
     */
    @Attribute
    String getApplicationRoot();

    /**
     * Sets the value of the applicationRoot property.
     *
     * @param value allowed object is {@link String }
     * @throws PropertyVetoException
     */
    void setApplicationRoot(String value) throws PropertyVetoException;

    /**
     * Gets the value of the logRoot property.
     *
     * Specifies where the server instance's log files are kept, including
     * HTTP access logs, server logs, and transaction logs.
     * Default is $INSTANCE-ROOT/logs
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    String getLogRoot();

    /**
     * Sets the value of the logRoot property.
     *
     * @param value allowed object is {@link String }
     * @throws PropertyVetoException
     */
    void setLogRoot(String value) throws PropertyVetoException;

    /**
     * Gets the value of the locale property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    String getLocale();

    /**
     * Sets the value of the locale property.
     *
     * @param value allowed object is {@link String }
     * @throws PropertyVetoException
     */
    void setLocale(String value) throws PropertyVetoException;

    /**
     * Gets the value of the version property. It is read-only.
     *
     * Tools are not to depend on this property. It is only for reference.
     *
     * @return String representing version of the Domain.
     */
    @Attribute
    String getVersion();

    /**
     * Gets the SecureAdmin value defined in the domain.
     * @return {@link SecureAdmin }
     */
    @Element
    SecureAdmin getSecureAdmin();

    /**
     * Sets the SecureAdmin value.
     * @param secureAdmin
     */
    void setSecureAdmin(SecureAdmin secureAdmin);

    /**
     * Gets the value of the applications property.
     *
     * @return possible object is
     *         {@link Applications }
     */
    @Element
    @NotNull
    Applications getApplications();

    /**
     * Sets the value of the system-applications property.
     *
     * @param value allowed object is {@link Applications }
     * @throws PropertyVetoException
     */
    void setApplications(Applications value) throws PropertyVetoException;

    @Element
    @NotNull
    SystemApplications getSystemApplications();

    /**
     * Sets the value of the system-applications property.
     *
     * @param value allowed object is {@link Applications }
     * @throws PropertyVetoException
     */
    void setSystemApplications(SystemApplications value) throws PropertyVetoException;
    /**
     * Gets the value of the resources property.
     *
     * @return possible object is
     *         {@link Resources }
     */
    @Element
    @NotNull
    Resources getResources();

    /**
     * Sets the value of the resources property.
     *
     * @param value allowed object is {@link Resources }
     * @throws PropertyVetoException
     */
    void setResources(Resources value) throws PropertyVetoException;

    /**
     * Gets the value of the configs property.
     *
     * @return possible object is
     *         {@link Configs }
     */
    @Element(required=true)
    @NotNull
    Configs getConfigs();

    /**
     * Sets the value of the configs property.
     *
     * @param value allowed object is {@link Configs }
     * @throws java.beans.PropertyVetoException
     */
    void setConfigs(Configs value) throws PropertyVetoException;

    /**
     * Gets the value of the servers property.
     *
     * @return possible object is {@link Servers }
     */
    @Element(required=true)
    @NotNull
    Servers getServers();

    /**
     * Sets the value of the servers property.
     *
     * @param value allowed object is {@link Servers }
     * @throws PropertyVetoException
     */
    void setServers(Servers value) throws PropertyVetoException;

    /**
     * Gets the value of the clusters property.
     *
     * @return possible object is
     *         {@link Clusters }
     */
    @Element
    @NotNull
    Clusters getClusters();

    /**
     * Sets the value of the clusters property.
     *
     * @param value allowed object is {@link Clusters }
     * @throws PropertyVetoException
     */
    void setClusters(Clusters value) throws PropertyVetoException;
    
    /**
     * Gets the value of the deployment groups property.
     *
     * @return possible object is {@link DeploymentGroups }
     */
    @Element
    @NotNull
    DeploymentGroups getDeploymentGroups();

    /**
     * Sets the value of the deployment groups property.
     *
     * @param value allowed object is {@link DeploymentGroups }
     * @throws PropertyVetoException
     */
    void setDeploymentGroups(DeploymentGroups value) throws PropertyVetoException;
    
    

    /**
     * Gets the value of the nodes property.
     *
     * @return possible object is
     *         {@link Nodes }
     */
    @Element
    Nodes getNodes();

    /**
     * Sets the value of the nodes property.
     *
     * @param value allowed object is {@link Nodes }
     * @throws PropertyVetoException
     */
    void setNodes(Nodes value) throws PropertyVetoException;

    /**
     * Gets the value of the nodeAgents property.
     *
     * @return possible object is
     *         {@link NodeAgents }
     */
    @Element
    NodeAgents getNodeAgents();

    /**
     * Sets the value of the nodeAgents property.
     *
     * @param value allowed object is {@link NodeAgents }
     * @throws PropertyVetoException
     */
    void setNodeAgents(NodeAgents value) throws PropertyVetoException;

    /**
     * Gets the value of the systemProperty property.
     * <p/>
     * <p/>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the systemProperty property.
     * <p/>
     * <p/>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getSystemProperty().add(newItem);
     * </pre>
     * <p/>
     * <p/>
     * <p/>
     * Objects of the following type(s) are allowed in the list
     * {@link SystemProperty }
     * @return 
     */
    @ToDo(priority=ToDo.Priority.IMPORTANT, details="Any more legal system properties?" )
@PropertiesDesc(
    systemProperties=true,
    props={
        @PropertyDesc(name="com.sun.aas.installRoot",
            description="Operating system dependent. Path to the directory where the server is installed"),

        @PropertyDesc(name="com.sun.aas.instanceRoot",
            description="Operating system dependent. Path to the top level directory for a server instance"),

        @PropertyDesc(name="com.sun.aas.hostName",
            description="Operating system dependent. Path to the name of the host (machine)"),

        @PropertyDesc(name="com.sun.aas.javaRoot",
            description="Operating system dependent. Path to the library directory for the Sun GlassFish Message Queue software"),

        @PropertyDesc(name="com.sun.aas.imqLib",
            description="Operating system dependent. Path to the installation directory for the Java runtime"),

        @PropertyDesc(name="com.sun.aas.imqLib",
            description="Operating system dependent. Path to the installation directory for the Java runtime"),

        @PropertyDesc(name="com.sun.aas.configName", defaultValue="server-config",
            description="Name of the <config> used by a server instance"),

        @PropertyDesc(name="com.sun.aas.instanceName", defaultValue="server1",
            description="Name of the server instance. Not used in the default configuration, but can be used to customize configuration"),

        @PropertyDesc(name="com.sun.aas.domainName", defaultValue="domain1",
            description="Name of the domain. Not used in the default configuration, but can be used to customize configuration")
    }
    )
    @Element
    @Override
    List<SystemProperty> getSystemProperty();

    /**
    	Properties as per {@link PropertyBag}
     * @return 
     */
    @ToDo(priority=ToDo.Priority.IMPORTANT, details="Provide PropertyDesc for legal props" )
    @PropertiesDesc(props={})
    @Element
    @Override
    List<Property> getProperty();


    @Element("*")
    List<DomainExtension> getExtensions();

    @DuckTyped
    String getName();

    @DuckTyped
    List<Application> getAllDefinedSystemApplications();

    @DuckTyped
    ApplicationRef getApplicationRefInServer(String sn, String name);

    @DuckTyped
    List<ApplicationRef> getApplicationRefsInServer(String sn);

    /**
     * Returns the list of system-applications that are referenced from the given server.
     * A server references an application, if the server has an element named
     * &lt;application-ref> in it that points to given application. The given server
     * is a &lt;server> element inside domain.
     *
     * @param sn the string denoting name of the server
     * @return List of system-applications for that server, an empty list in case there is none
     */
    @DuckTyped
    List<Application> getSystemApplicationsReferencedFrom(String sn);

    @DuckTyped
    Application getSystemApplicationReferencedFrom(String sn, String appName);

    @DuckTyped
    boolean isNamedSystemApplicationReferencedFrom(String appName, String serverName);

    @DuckTyped
    Server getServerNamed(String name);

    @DuckTyped
    boolean isServer(String name);

    @DuckTyped
    Config getConfigNamed(String name);

    @DuckTyped
    Cluster getClusterNamed(String name);

    @DuckTyped
    Node getNodeNamed(String name);

    @DuckTyped
    boolean isCurrentInstanceMatchingTarget(String target, String appName,
        String currentInstance, List<String> referencedTargets);

    @DuckTyped
    List<Server> getServersInTarget(String target);

    @DuckTyped
    List<ApplicationRef> getApplicationRefsInTarget(String target);

    @DuckTyped
    ApplicationRef getApplicationRefInTarget(String appName, String target);

    @DuckTyped
    ApplicationRef getApplicationRefInTarget(String appName, String target, boolean includeInstances);

    @DuckTyped
    boolean isAppRefEnabledInTarget(String appName, String target);

    @DuckTyped
    boolean isAppEnabledInTarget(String appName, String target);

    @DuckTyped
    boolean isAppReferencedByPaaSTarget(String appName);

    @DuckTyped
    List<String> getAllReferencedTargetsForApplication(String appName);

    @DuckTyped
    List<String> getAllTargets();
    
    @DuckTyped
    public List<String> getTargets(final String tgt);

    @DuckTyped
    List<Application> getApplicationsInTarget(String target);

    @DuckTyped
    String getVirtualServersForApplication(String target, String appName);

    @DuckTyped
    String getEnabledForApplication(String target, String appName);

    @DuckTyped
    ReferenceContainer getReferenceContainerNamed(String name);

    @DuckTyped
    Cluster getClusterForInstance(String instanceName);

    @DuckTyped
    List<ReferenceContainer> getAllReferenceContainers();

    @DuckTyped
    List<ReferenceContainer> getReferenceContainersOf(Config config);

    @DuckTyped
    List<Server> getInstancesOnNode(String nodeName);

    @DuckTyped
    List<Cluster> getClustersOnNode(String nodeName);

    @ConfigExtensionMethod
    <T extends DomainExtension> T getExtensionByType(Class<T> type);

    /**
     * @param configBeanType The config bean type we want to check whether the configuration exists for it or not.
     * @param <P>            Type that extends the   ConfigBeanProxy which is the type of class we accept as parameter
     * @return true if configuration for the type exists in the target area of domain.xml and false if not.
     */
    @DuckTyped
    <P extends ConfigBeanProxy> boolean checkIfExtensionExists(Class<P> configBeanType);

    @DuckTyped
    public DeploymentGroup getDeploymentGroupNamed(String target);
    
    @DuckTyped 
    public List<DeploymentGroup> getDeploymentGroupsForInstance(String namedInstance);

    class Duck {
        private static final Logger LOGGER=ConfigApiLoggerInfo.getLogger();
        public static String getName(Domain domain) {
            return domain.getPropertyValue(DOMAIN_NAME_PROPERTY);
        }
        /* return an empty list if given garbage -- or errors are encountered
         * or if no matches
         */
        public static List<Server> getInstancesOnNode(Domain domain, String nodeName) {
            List<Server> ret = new LinkedList<Server>();
            try {
                if(!StringUtils.ok(nodeName))
                    return ret;

                List<Server> servers = domain.getServers().getServer();

                for(Server server : servers) {
                    if(nodeName.equals(server.getNodeRef()))
                            ret.add(server);
                }
            }
            catch(Exception e) {
                 LOGGER.log(Level.WARNING,ConfigApiLoggerInfo.errorGettingServers , e.getLocalizedMessage());
            }
            return ret;
        }

        /* return an empty list if given garbage -- or errors are encountered
         * or if no matches
         */
        public static List<Cluster> getClustersOnNode(Domain domain, String nodeName) {

            HashMap<String,Cluster> clMap = new HashMap<String,Cluster>();
            List<Server> serverList = getInstancesOnNode(domain, nodeName);

            try {
                for(Server server : serverList) {
                    Cluster mycl = server.getCluster();
                    if(nodeName.equals(server.getNodeRef()) )   {
                        clMap.put(mycl.getName(),mycl);
                    }
                }
            }
            catch(Exception e) {
                LOGGER.log(Level.WARNING, ConfigApiLoggerInfo.errorGettingCluster, e.getLocalizedMessage());

            }
            return new ArrayList(clMap.values());
        }

        public static List<Application> getAllDefinedSystemApplications(Domain me) {
            List<Application> allSysApps = new ArrayList<Application>();
            SystemApplications sa = me.getSystemApplications();
            for (ApplicationName m : sa.getModules()) {
                if (m instanceof Application) {
                    allSysApps.add((Application) m);
                }
            }
            return Collections.unmodifiableList(allSysApps);
        }

        public static ApplicationRef getApplicationRefInServer(Domain me, String sn, String name) {
            Servers ss = me.getServers();
            List<Server> list = ss.getServer();
            Server theServer = null;
            for (Server s : list) {
                if (s.getName().equals(sn)) {
                    theServer = s;
                    break;
                }
            }
            ApplicationRef aref = null;
            if (theServer != null) {
                List <ApplicationRef> arefs = theServer.getApplicationRef();
                for (ApplicationRef ar : arefs) {
                    if (ar.getRef().equals(name)) {
                        aref = ar;
                        break;
                    }
                }
            }
            return aref;
        }

        public static List<ApplicationRef> getApplicationRefsInServer(Domain me, String sn) {
            Server server = getServerNamed(me, sn);
            if (server != null) {
                return server.getApplicationRef();
            } else {
                return Collections.emptyList();
            }
        }

        public static List<Application> getSystemApplicationsReferencedFrom(Domain d, String sn) {
            if (d == null || sn == null)
                throw new IllegalArgumentException("Null argument");
            List<Application> allApps = d.getAllDefinedSystemApplications();
            if (allApps.isEmpty())
                return allApps; //if there are no sys-apps, none can reference one :)
            //allApps now contains ALL the system applications
            Server s = getServerNamed(d,sn);
            List<Application> referencedApps = new ArrayList<Application>();
            List<ApplicationRef> appsReferenced = s.getApplicationRef();
            for (ApplicationRef ref : appsReferenced) {
                for (Application app : allApps) {
                    if (ref.getRef().equals(app.getName())) {
                        referencedApps.add(app);
                    }
                }
            }
            return Collections.unmodifiableList(referencedApps);
        }

        public static Application getSystemApplicationReferencedFrom(Domain d, String sn, String appName) {
            //returns null in case there is none
            List<Application> allApps = getSystemApplicationsReferencedFrom(d, sn);
            for (Application app : allApps) {
                if (app.getName().equals(appName)) {
                    return app;
                }
            }
            return null;
        }

        public static boolean isNamedSystemApplicationReferencedFrom(Domain d, String appName, String serverName) {
            List <Application> referencedApps = getSystemApplicationsReferencedFrom(d, serverName);
            for (Application app : referencedApps) {
                if (app.getName().equals(appName))
                    return true;
            }
            return false;
        }

        public static Server getServerNamed(Domain d, String name) {
            if (d.getServers() == null || name == null)
                throw new IllegalArgumentException ("no <servers> element");
            List<Server> servers = d.getServers().getServer();
            for (Server s : servers) {
                if (name.equals(s.getName().trim())) {
                    return s;
                }
            }
            return null;
        }

        public static Config getConfigNamed(Domain d, String name) {
            if (d.getConfigs() == null || name == null)
                throw new IllegalArgumentException ("no <config> element");
            List<Config> configs = d.getConfigs().getConfig();
            for (Config c : configs) {
                if (name.equals(c.getName().trim())) {
                    return c;
                }
            }
            return null;
        }


        public static DeploymentGroup getDeploymentGroupNamed(Domain d, String name) {
            if (d.getDeploymentGroups()== null || name == null)
                throw new IllegalArgumentException ("no <deploymentgroup> element");
            List<DeploymentGroup> dgs = d.getDeploymentGroups().getDeploymentGroup();
            for (DeploymentGroup dg : dgs) {
                if (name.equals(dg.getName().trim())) {
                    return dg;
                }
            }
            return null;
        }
        
        public static List<DeploymentGroup> getDeploymentGroupsForInstance(Domain d, String instanceName) {
            List<DeploymentGroup> result = new LinkedList<>();
            List<DeploymentGroup> dgs = d.getDeploymentGroups().getDeploymentGroup();
            if (dgs != null) {
                for (DeploymentGroup dg : dgs) {
                    DGServerRef ref = dg.getDGServerRefByRef(instanceName);
                    if (ref != null) {
                        result.add(dg);
                    }
                }
            }
            return result;
        }
        
         public static Cluster getClusterNamed(Domain d, String name) {
            if (name == null) {
                return null;
            }
            List<Cluster> clusters = d.getClusters().getCluster();
            for (Cluster c : clusters) {
                if (name.equals(c.getName().trim())) {
                    return c;
                }
            }
            return null;
        }

        public static Node getNodeNamed(Domain d, String name) {
            if (d.getNodes() == null || name == null) {
                return null;
            }
            List<Node> nodes = d.getNodes().getNode();
            for (Node n : nodes) {
                if (name.equals(n.getName().trim())) {
                    return n;
                }
            }
            return null;
        }

        public static boolean isCurrentInstanceMatchingTarget(Domain d,
            String target, String appName, String currentInstance,
            List<String> referencedTargets) {

            if (target == null || currentInstance == null) {
                return false;
            }

            List<String> targets = new ArrayList<String>();
            if (!target.equals("domain")) {
                targets.add(target);
            } else {
                if (referencedTargets == null) {
                    referencedTargets =
                        d.getAllReferencedTargetsForApplication(appName);
                }
                targets = referencedTargets;
            }

            for  (String target2 : targets) {
                if (currentInstance.equals(target2)) {
                    // standalone instance case
                    return true;
                }

                Cluster cluster = getClusterNamed(d, target2);

                if (cluster != null) {
                    for (Server svr : cluster.getInstances() ) {
                        if (svr.getName().equals(currentInstance)) {
                            // cluster instance case
                            return true;
                        }
                    }
                }
                
                // check the deployment group
                DeploymentGroup dg = getDeploymentGroupNamed(d, target2);
                if (dg != null) {
                    for (Server svr : dg.getInstances()) {
                        if (svr.getName().equals(currentInstance)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }


        public static List<Server> getServersInTarget(
            Domain me, String target) {
            List<Server> servers = new ArrayList<>();
            Server server = me.getServerNamed(target);
            if (server != null) {
                servers.add(server);
            } else {
                Cluster cluster = getClusterNamed(me, target);
                if (cluster != null) {
                    servers.addAll(cluster.getInstances());
                } else {
                    DeploymentGroup dg = getDeploymentGroupNamed(me, target);
                    if (dg != null) {
                        servers.addAll(dg.getInstances());
                    }
                }
            }
            return servers;
        }

        public static List<ApplicationRef> getApplicationRefsInTarget(
            Domain me, String target) {
            return getApplicationRefsInTarget(me, target, false);
        }

        public static List<String> getTargets(final Domain me, final String tgt) {
            List<String> targets = new ArrayList<>();
            if (!tgt.equals("domain")) {
                targets.add(tgt);
            } else {
                targets = me.getAllTargets();
            }
            return targets;
        }
        
        public static List<ApplicationRef> getApplicationRefsInTarget(
            Domain me, String tgt, boolean includeInstances) {
            List<String> targets = getTargets(me, tgt);
            List<ApplicationRef> allAppRefs = new ArrayList<ApplicationRef>();

            for (String target : targets) {
                Server server = me.getServerNamed(target);
                if (server != null) {
                    allAppRefs.addAll(server.getApplicationRef());
                } else {
                    Cluster cluster = getClusterNamed(me, target);
                    if (cluster != null) {
                        allAppRefs.addAll(cluster.getApplicationRef());
                        if (includeInstances) {
                            for (Server svr : cluster.getInstances() ) {
                                allAppRefs.addAll(svr.getApplicationRef());
                            }
                        }
                    } else {
                        DeploymentGroup dg = getDeploymentGroupNamed(me, target);
                        if (dg != null) {
                            allAppRefs.addAll(dg.getApplicationRef());
                            if (includeInstances) {
                                for (Server svr: dg.getInstances()) {
                                    allAppRefs.addAll(svr.getApplicationRef());
                                }
                            }
                        }
                    }
                }
            }
            return allAppRefs;
        }

        public static ApplicationRef getApplicationRefInTarget(
            Domain me, String appName, String target) {
            for (ApplicationRef ref : getApplicationRefsInTarget(me, target)) {
                if (ref.getRef().equals(appName)) {
                    return ref;
                }
            }
            return null;
        }

        public static boolean isAppRefEnabledInTarget(
            Domain me, String appName, String target) {
            boolean found = false;

            Cluster containingCluster = getClusterForInstance(me, target);
            if (containingCluster != null) {
                // if this is a clustered instance, check the enable 
                // attribute of its enclosing cluster first
                // and return false if the cluster level enable attribute
                // is false
                ApplicationRef clusterRef = getApplicationRefInTarget(me, appName, containingCluster.getName());
                if (clusterRef == null ||
                    !Boolean.valueOf(clusterRef.getEnabled())) {
                    return false;
                }
            }
            
            List<DeploymentGroup> dgs = getDeploymentGroupsForInstance(me, target);
            if (dgs != null) {
                for (DeploymentGroup dg : dgs) {
                    ApplicationRef ref = dg.getApplicationRef(appName);
                    if (ref == null || !Boolean.valueOf(ref.getEnabled())) {
                        return false;
                    }
                }
            }
            

            for (ApplicationRef ref :
                getApplicationRefsInTarget(me, target, true)) {
                if (ref.getRef().equals(appName)) {
                    found = true;
                    if (!Boolean.valueOf(ref.getEnabled())) {
                        return false;
                    }
                }
            }
            // if we found the ref(s) and the enable attribute(s) is/are true
            return found;
        }

        public static boolean isAppEnabledInTarget(
            Domain me, String appName, String target) {
            Application application = me.getApplications().getApplication(
                appName);
            if (application != null &&
                Boolean.valueOf(application.getEnabled())) {
                List<String> targets = new ArrayList<String>();
                if (!target.equals("domain")) {
                    targets.add(target);
                } else {
                    targets = getAllReferencedTargetsForApplication(me, appName);
                }
                for (String tgt : targets) {
                    if (!isAppRefEnabledInTarget(me, appName, tgt)) {
                        return false;
                    }
                }
                return true;
            }
            return false;
        }

        public static List<String> getAllTargets(Domain d) {
            List<String> targets = new ArrayList<String>();
            // only add non-clustered servers as the cluster 
            // targets will be separately added
            for (Server server : d.getServers().getServer()) {
                if (server.getCluster() == null) {
                    targets.add(server.getName());
                }
            }
            for (Cluster cluster : d.getClusters().getCluster()) {
                targets.add(cluster.getName());
            }
            for (DeploymentGroup dg : d.getDeploymentGroups().getDeploymentGroup()) {
                targets.add(dg.getName());
            }
            return targets;
        }

        /**
         * @param me      the current domain
         * @param appName the name of the application
         * @return all of the targets that have application-refs for an application. If
         *         an instance is included by being in a deployment group or cluster it
         *         won't appear in this list.
         */
        public static List<String> getAllReferencedTargetsForApplication(
            Domain me, String appName) {
            List<String> referencedTargets = new ArrayList<String>();
            for (String target : me.getAllTargets()) {
                if (me.getApplicationRefInTarget(appName, target) != null) {
                    referencedTargets.add(target);
                }
            }

            // Remove any server targets if they are included by virtue of being in a
            // deployment group
            for (String reference : new ArrayList<>(referencedTargets)) {
                DeploymentGroup dg = me.getDeploymentGroupNamed(reference);
                if (dg != null) {
                    for (Server instance : dg.getInstances()) {
                        referencedTargets.remove(instance.getName());
                    }
                }
                Cluster cluster = me.getClusterNamed(reference);
                if (cluster != null) {
                    for (Server instance : cluster.getInstances()) {
                        referencedTargets.remove(instance.getName());
                    }
                }
            }
            return referencedTargets;
        }

        public static boolean isAppReferencedByPaaSTarget(Domain me, String appName) {
            List<String> referencedTargets = me.getAllReferencedTargetsForApplication(appName);
            for (String target : referencedTargets) {
                Cluster cluster = me.getClusterNamed(target);
                if (cluster != null && cluster.isVirtual()) {
                    return true;
                }
            }
            return false;
        }

        public static List<Application> getApplicationsInTarget(Domain me, String target) {
            if (target.equals("domain")) {
                // special target domain
                return me.getApplications().getApplications();
            }

            List<Application> apps = new ArrayList<Application>();

            List<ApplicationRef> applicationRefs = me.getApplicationRefsInTarget(target);
            for (ApplicationRef ref : applicationRefs) {
                Application app = me.getApplications().getApplication(ref.getRef());
                if (app != null) {
                    apps.add(app);
                }
            }
            return apps;
         }

         public static String getVirtualServersForApplication(Domain d,
             String target, String appName) {
             ApplicationRef appRef = d.getApplicationRefInTarget(
                 appName, target);
             if (appRef != null) {
                return appRef.getVirtualServers();
             } else {
                return null;
             }
         }

        public static String getEnabledForApplication(Domain d,
                                                      String target, String appName) {
            ApplicationRef appRef = d.getApplicationRefInTarget(
                    appName, target);
            if (appRef != null) {
                return appRef.getEnabled();
            } else {
                return null;
            }
        }

         public static ReferenceContainer getReferenceContainerNamed(Domain domain, String name) {
             Cluster cluster = getClusterNamed(domain, name);
             if (cluster != null) {
                 return cluster;
             }
             DeploymentGroup deploymentGroup = getDeploymentGroupNamed(domain, name);
             if (deploymentGroup != null) {
                 return deploymentGroup;
             }
             return getServerNamed(domain, name);
        }

        public static List<ReferenceContainer> getReferenceContainersOf(Domain d, Config config) {
            // Clusters and Servers are ReferenceContainers
            List<ReferenceContainer> sub = new LinkedList<ReferenceContainer>();

            // both the config and its name need to be sanity-checked
            String name = null;

            if(config != null)
                name = config.getName();

            if(!StringUtils.ok(name))  // we choose to make this not an error
                return sub;

            List<ReferenceContainer> all = getAllReferenceContainers(d);

            for(ReferenceContainer rc : all) {
                if(name.equals(rc.getReference()))
                    sub.add(rc);
            }
            return sub;
        }

        public static List<ReferenceContainer> getAllReferenceContainers(Domain d) {
            List<ReferenceContainer> referenceContainers = new LinkedList<ReferenceContainer>();
            referenceContainers.addAll(d.getServers().getServer());
            referenceContainers.addAll(d.getClusters().getCluster());
            referenceContainers.addAll(d.getDeploymentGroups().getDeploymentGroup());
            return referenceContainers;
        }

        public static Cluster getClusterForInstance(Domain d,String instanceName){
            List<Cluster> clusterList = d.getClusters().getCluster();
            for (Cluster cluster:clusterList) {
                List<ServerRef> serverRefs =cluster.getServerRef();
                for (ServerRef serverRef:serverRefs){
                    if (serverRef.getRef().equals(instanceName)) {
                        return cluster;
                    }
                }
            }
            return null;
        }

        public static boolean isServer(Domain d, String name) {
            final Server server = d.getServerNamed(name);
            return (server != null);
        }

        public static <P extends DomainExtension> boolean checkIfExtensionExists(Domain d, Class<P> configBeanType) {
             for (DomainExtension extension : d.getExtensions()) {
                 try {
                     configBeanType.cast(extension);
                     return true;
                 } catch (Exception e) {
                     // ignore, not the right type.
                 }
             }
             return false;
         }

    }
}
