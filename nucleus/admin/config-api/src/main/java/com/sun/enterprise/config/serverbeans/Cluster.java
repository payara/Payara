/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2016 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.config.serverbeans;

import com.sun.enterprise.config.serverbeans.customvalidators.NotTargetKeyword;
import com.sun.enterprise.config.serverbeans.customvalidators.NotDuplicateTargetName;
import com.sun.enterprise.config.serverbeans.customvalidators.ConfigRefConstraint;
import com.sun.enterprise.config.serverbeans.customvalidators.ConfigRefValidator;
import com.sun.enterprise.config.serverbeans.customvalidators.ReferenceConstraint;
import com.sun.enterprise.config.util.ConfigApiLoggerInfo;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.io.FileUtils;
import com.sun.logging.LogDomains;
import java.io.*;
import org.glassfish.api.ActionReport;
import org.glassfish.api.I18n;
import org.glassfish.api.Param;
import org.glassfish.api.admin.*;
import org.glassfish.config.support.*;
import static org.glassfish.config.support.Constants.NAME_SERVER_REGEX;


import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceLocator;
import org.jvnet.hk2.config.*;
import org.glassfish.api.admin.config.Named;
import org.glassfish.api.admin.config.PropertyDesc;
import org.glassfish.api.admin.config.ReferenceContainer;
// import org.glassfish.virtualization.util.RuntimeContext;

import java.beans.PropertyVetoException;
import java.util.List;
import java.util.ArrayList;
import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.api.admin.config.PropertiesDesc;
import org.jvnet.hk2.config.types.Property;
import org.jvnet.hk2.config.types.PropertyBag;

import org.glassfish.quality.ToDo;

import javax.inject.Inject;
import javax.validation.Payload;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/**
 * A cluster defines a homogeneous set of server instances that share the same
 * applications, resources, and configuration.
 */
@Configured
@SuppressWarnings("unused")
@ConfigRefConstraint(message="{configref.invalid}", payload= ConfigRefValidator.class)
@NotDuplicateTargetName(message="{cluster.duplicate.name}", payload=Cluster.class)
@ReferenceConstraint(skipDuringCreation=true, payload=Cluster.class)
public interface Cluster extends ConfigBeanProxy, PropertyBag, Named, SystemPropertyBag, ReferenceContainer, RefContainer, Payload {

    /**
     * Sets the cluster name
     * @param value cluster name
     * @throws PropertyVetoException if a listener vetoes the change
     */
    @Param(name="name", primary = true)
    @Override
    public void setName(String value) throws PropertyVetoException;

    @NotTargetKeyword(message="{cluster.reserved.name}", payload=Cluster.class)
    @Pattern(regexp=NAME_SERVER_REGEX, message="{cluster.invalid.name}", payload=Cluster.class)
    @Override
    public String getName();

    /**
     * points to a named config. All server instances in the cluster
     * will share this config.
     *
     * @return a named config name
     */
    @Attribute
    @NotNull
    @Pattern(regexp=NAME_SERVER_REGEX)
    @ReferenceConstraint.RemoteKey(message="{resourceref.invalid.configref}", type=Config.class)
    String getConfigRef();

    /**
     * Sets the value of the configRef property.
     *
     * @param value allowed object is
     *              {@link String }
     * @throws PropertyVetoException if a listener vetoes the change
     */
    @Param(name="config", optional=true)
    @I18n("generic.config")
    void setConfigRef(String value) throws PropertyVetoException;

    /**
     * Gets the value of the gmsEnabled property.
     *
     * When "gms-enabled" is set to "true", the GMS services will be
     * started as a lifecycle module in each the application server in the
     * cluster.
     *
     * @return true | false as a string, null means false
     */
    @Attribute (defaultValue="true", dataType=Boolean.class, required=true)
    @NotNull
    String getGmsEnabled();

    /**
     * Sets the value of the gmsEnabled property.
     *
     * @param value allowed object is
     *              {@link String }
     * @throws PropertyVetoException if a listener vetoes the change
     */
    @Param(name="gmsenabled", optional=true)
    void setGmsEnabled(String value) throws PropertyVetoException;

    /**
     * Gets the value of the broadcast property.
     *
     * When "broadcast" is set to default of "udpmulticast" and GmsMulticastPort
     * GMSMulticastAddress are not set, then their values are generated.
     * When "broadcast" is set to implied unicast using udp or tcp protocol,
     * then the VIRUTAL_MUTLICAST_URI_LIST is generated
     * for virtual broadcast over unicast mode.
     *
     * @return true | false as a string, null means false
     */
    @Attribute (defaultValue="udpmulticast", dataType=String.class, required=true)
    @NotNull
    String getBroadcast();

    /**
     * Sets the value of the broadcast property.
     *
     * @param value allowed object is
     *              {@link String }
     * @throws PropertyVetoException if a listener vetoes the change
     */
    @Param(name="gmsbroadcast", optional=true)
    void setBroadcast(String value) throws PropertyVetoException;


    /**
     * Gets the value of the gmsMulticastPort property.
     *
     * This is the communication port GMS uses to listen for group  events.
     * This should be a valid port number.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    @Min(value=2048)
    @Max(value=49151)  // fix bug 13475586
    String getGmsMulticastPort();

    /**
     * Sets the value of the gmsMulticastPort property.
     *
     * @param value allowed object is
     *              {@link String }
     * @throws PropertyVetoException if a listener vetoes the change
     */
    @Param(name="multicastport", optional=true, alias="heartbeatport")
    void setGmsMulticastPort(String value) throws PropertyVetoException;

    /**
     * Gets the value of the gmsMulticastAddress property.
     *
     * This is the address (only multicast supported) at which GMS will
     * listen for group events. Must be unique for each cluster.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    String getGmsMulticastAddress();

    /**
     * Sets the value of the gmsMulticastAddress property.
     *
     * @param value allowed object is
     *              {@link String }
     * @throws PropertyVetoException if a listener vetoes the change
     */
    @Param(name="multicastaddress", optional=true, alias="heartbeataddress")
    void setGmsMulticastAddress(String value) throws PropertyVetoException;

    /**
     * Gets the value of the gmsBindInterfaceAddress property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    String getGmsBindInterfaceAddress();

    /**
     * Sets the value of the gmsBindInterfaceAddress property.
     *
     * @param value allowed object is
     *              {@link String }
     * @throws PropertyVetoException if a listener vetoes the change
     */
    @Param(name="bindaddress", optional=true)
    void setGmsBindInterfaceAddress(String value) throws PropertyVetoException;

    /**
     * Gets the value of the heartbeatEnabled property.
     *
     * When "heartbeat-enabled" is set to "true", the GMS services will be
     * started as a lifecycle module in each the application server in the
     * cluster.When "heartbeat-enabled" is set to "false", GMS will not be
     * started and its services will be unavailable. Clusters should function
     * albeit with reduced functionality.
     *
     * @return true | false as a string, null means false
     */
    @Deprecated
    @Attribute
    String getHeartbeatEnabled();

    /**
     * Sets the value of the heartbeatEnabled property.
     *
     * @param value allowed object is
     *              {@link String }
     * @throws PropertyVetoException if a listener vetoes the change
     */
    @Deprecated
    void setHeartbeatEnabled(String value) throws PropertyVetoException;

    /**
     * Gets the value of the heartbeatPort property.
     *
     * This is the communication port GMS uses to listen for group  events.
     * This should be a valid port number.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    //@Min(value=2048)
    //@Max(value=49151)
    @Deprecated
    String getHeartbeatPort();

    /**
     * Sets the value of the heartbeatPort property.
     *
     * @param value allowed object is
     *              {@link String }
     * @throws PropertyVetoException if a listener vetoes the change
     */
    @Deprecated
    void setHeartbeatPort(String value) throws PropertyVetoException;

    /**
     * Gets the value of the heartbeatAddress property.
     *
     * This is the address (only multicast supported) at which GMS will
     * listen for group events.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    @Deprecated
    String getHeartbeatAddress();

    /**
     * Sets the value of the heartbeatAddress property.
     *
     * @param value allowed object is
     *              {@link String }
     * @throws PropertyVetoException if a listener vetoes the change
     */
    @Deprecated
    void setHeartbeatAddress(String value) throws PropertyVetoException;

    /**
     * Gets the value of the serverRef property.
     *
     * List of servers in the cluster
     *
     * @return list of configured {@link ServerRef }
     */
    @Element
    List<ServerRef> getServerRef();

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
     */
    @Element
    @ToDo(priority=ToDo.Priority.IMPORTANT, details="Provide PropertyDesc for legal system props" )
    @Param(name="systemproperties",optional=true)
    @Override
    List<SystemProperty> getSystemProperty();

    /**
     *	Properties as per {@link org.jvnet.hk2.config.types.PropertyBag}
     */
    @ToDo(priority=ToDo.Priority.IMPORTANT, details="Complete PropertyDesc for legal props" )
    @PropertiesDesc(props={
        @PropertyDesc(name="GMS_LISTENER_PORT", defaultValue = "9090",
            description = "GMS listener port")
    })
    @Element
    @Param(name="properties", optional=true)
    @Override
    List<Property> getProperty();

    @Element("*")
    List<ClusterExtension> getExtensions();

    /**
     * Returns the cluster configuration reference
     * @return the config-ref attribute
     */
    @DuckTyped
    @Override
    String getReference();

    @DuckTyped
    List<Server> getInstances();

    @DuckTyped
    public ServerRef getServerRefByRef(String ref);

    // four trivial methods that ReferenceContainer's need to implement
    @DuckTyped
    @Override
    boolean isCluster();

    @DuckTyped
    @Override
    boolean isServer();

    @DuckTyped
    @Override
    boolean isDas();

    @DuckTyped
    @Override
    boolean isInstance();

    @DuckTyped
    boolean isVirtual();

    @DuckTyped
    ApplicationRef getApplicationRef(String appName);

    @DuckTyped
    ResourceRef getResourceRef(String refName);

    @DuckTyped
    boolean isResourceRefExists(String refName);

    @DuckTyped
    void createResourceRef(String enabled, String refName) throws TransactionFailure;

    @DuckTyped
    void deleteResourceRef(String refName) throws TransactionFailure;

    @DuckTyped
    <T extends ClusterExtension> List<T> getExtensionsByType(Class<T> type);

    @DuckTyped
    <T extends ClusterExtension> T getExtensionsByTypeAndName(Class<T> type, String name);

    class Duck {
        public static boolean isCluster(Cluster me) { return true; }
        public static boolean isServer(Cluster me)  { return false; }
        public static boolean isInstance(Cluster me) { return false; }
        public static boolean isDas(Cluster me) { return false; }

        public static String getReference(Cluster cluster) {
            return cluster.getConfigRef();
        }

        public static boolean isVirtual(Cluster me) {
            return !me.getExtensionsByType(VirtualMachineExtension.class).isEmpty();
        }

        public static List<Server> getInstances(Cluster cluster) {

            Dom clusterDom = Dom.unwrap(cluster);
            Domain domain =
                    clusterDom.getHabitat().getService(Domain.class);

            ArrayList<Server> instances = new ArrayList<Server>();
            for (ServerRef sRef : cluster.getServerRef()) {
                Server svr =  domain.getServerNamed(sRef.getRef());
                // the instance's domain.xml only has its own server 
                // element and not other server elements in the cluster 
                if (svr != null) {
                    instances.add(domain.getServerNamed(sRef.getRef()));
                }
            }
            return instances;
        }

        public static ServerRef getServerRefByRef(Cluster c, String name) {
            for (ServerRef ref : c.getServerRef()) {
                if (ref.getRef().equals(name)) {
                    return ref;
                }
            }
            return null;
        }

        public static ApplicationRef getApplicationRef(Cluster cluster,
                String appName) {
            for (ApplicationRef appRef : cluster.getApplicationRef()) {
                if (appRef.getRef().equals(appName)) {
                    return appRef;
                }
            }
            return null;
        }

        public static ResourceRef getResourceRef(Cluster cluster, String refName) {
            for (ResourceRef ref : cluster.getResourceRef()) {
                if (ref.getRef().equals(refName)) {
                    return ref;
                }
            }
            return null;
        }


        public static boolean isResourceRefExists(Cluster cluster, String refName) {
            return getResourceRef(cluster, refName) != null;
        }

        public static void deleteResourceRef(Cluster cluster, String refName) throws TransactionFailure {
            final ResourceRef ref = getResourceRef(cluster, refName);
            if (ref != null) {
                ConfigSupport.apply(new SingleConfigCode<Cluster>() {

                    public Object run(Cluster param) {
                        return param.getResourceRef().remove(ref);
                    }
                }, cluster);
            }
        }

        public static void createResourceRef(Cluster cluster, final String enabled, final String refName) throws TransactionFailure {

            ConfigSupport.apply(new SingleConfigCode<Cluster>() {

                public Object run(Cluster param) throws PropertyVetoException, TransactionFailure {

                    ResourceRef newResourceRef = param.createChild(ResourceRef.class);
                    newResourceRef.setEnabled(enabled);
                    newResourceRef.setRef(refName);
                    param.getResourceRef().add(newResourceRef);
                    return newResourceRef;
                }
            }, cluster);
        }

        public static <T extends ClusterExtension> List<T> getExtensionsByType(Cluster cluster, Class<T> type) {
            List<T> extensions = new ArrayList<T>();
            for (ClusterExtension ce : cluster.getExtensions()) {
                try {
                    type.cast(ce);
                    extensions.add((T) ce);
                } catch (ClassCastException e) {
                    // ignore, not the right type
                }
            }
            return extensions;
        }

        public static <T extends ClusterExtension> T getExtensionsByTypeAndName(Cluster cluster, Class<T> type, String name) {
            for (ClusterExtension ce : cluster.getExtensions()) {
                try {
                    type.cast(ce);
                    if (ce.getName().equals(name)) {
                        return type.cast(ce);
                    }
                } catch (ClassCastException e) {
                    // ignore, not the right type
                }
            }
            return null;
        }
    }

    @Service
    @PerLookup
    class Decorator implements CreationDecorator<Cluster> {

        @Param(name="config", optional=true)
        String configRef=null;

        @Param(optional = true,obsolete=true)
        String hosts=null;

        @Param(optional = true,obsolete=true)
        String haagentport;

        @Param(optional = true,obsolete=true)
        String haadminpassword=null;

        @Param(optional = true,obsolete=true)
        String haadminpasswordfile=null;

        @Param(optional = true,obsolete=true)
        String devicesize=null;

        @Param(optional = true,obsolete=true)
        String haproperty=null;

        @Param(optional = true,obsolete=true)
        String autohadb=null;

        @Param(optional = true,obsolete=true)
        String portbase=null;

        @Inject
        ServiceLocator habitat;

        @Inject
        ServerEnvironment env;

        @Inject
        Domain domain;

        @Inject
        CommandRunner runner;

//        @Inject
//        RuntimeContext rtContext;

        /**
         * Decorates the newly CRUD created cluster configuration instance.
         * tasks :
         *      - ensures that it references an existing configuration
         *      - creates a new config from the default-config if no config-ref
         *        was provided.
         *      - check for deprecated parameters.
         *
         * @param context administration command context
         * @param instance newly created configuration element
         * @throws TransactionFailure
         * @throws PropertyVetoException
         */
        @Override
        public void decorate(AdminCommandContext context, final Cluster instance) throws TransactionFailure, PropertyVetoException {
            Logger logger = ConfigApiLoggerInfo.getLogger();
            LocalStringManagerImpl localStrings = new LocalStringManagerImpl(Cluster.class);
            Transaction t = Transaction.getTransaction(instance);
            //check if cluster software is installed else fail , see issue 12023
            final CopyConfig command = (CopyConfig) runner
                    .getCommand("copy-config", context.getActionReport(), context.getLogger());
            if (command == null ) {
                throw new TransactionFailure(localStrings.getLocalString("cannot.execute.command",
                        "Cluster software is not installed"));
            }
            final String instanceName = instance.getName();
            if (instance.getGmsBindInterfaceAddress() == null) {
                instance.setGmsBindInterfaceAddress(String.format(
                    "${GMS-BIND-INTERFACE-ADDRESS-%s}",
                    instanceName));
            }

            if (configRef==null) {
                Config config = habitat.getService(Config.class, "default-config");
                if (config==null) {
                    config = habitat.<Config>getAllServices(Config.class).iterator().next();
                    logger.log(Level.WARNING,ConfigApiLoggerInfo.noDefaultConfigFound,
                            new Object[]{config.getName(), instance.getName()});
                }

                Configs configs = domain.getConfigs();
                Configs writableConfigs = t.enroll(configs);
                final String configName = instance.getName() + "-config";
                instance.setConfigRef(configName);
                command.copyConfig(writableConfigs,config,configName,logger);


            }  else {

                // cluster using specified config
                Config specifiedConfig = domain.getConfigs().getConfigByName(configRef);
                if (specifiedConfig == null) {
                    throw new TransactionFailure(localStrings.getLocalString(
                            "noSuchConfig", "Configuration {0} does not exist.", configRef));
                }
            }

            Property gmsListenerPort = instance.getProperty("GMS_LISTENER_PORT");
            boolean needToAddGmsListenerPort = false;
            if (gmsListenerPort == null) {
                needToAddGmsListenerPort = true;
                gmsListenerPort = instance.createChild(Property.class);
                gmsListenerPort.setName("GMS_LISTENER_PORT");
                gmsListenerPort.setValue(String.format("${GMS_LISTENER_PORT-%s}", instanceName));
                // do not add gmsListenerPort until know whether it needs to be fixed or symbolic.
                // for non-multicast with generate or list of ip addresses, port needs to be a fixed value
                // all members of cluster. for non-multicast with list of uri, the GMS_LISTENER_PORT is
                // set to symbolic system environment variable that is set different for each instance of cluster.
            }

            // handle generation of udp multicast and non-multicast mode for DAS managed cluster.
            // inspect cluster attribute broadcast and cluster property GMS_DISCOVERY_URI_LIST.
            String DEFAULT_BROADCAST = "udpmulticast";
            String broadcastProtocol = instance.getBroadcast();
            Property discoveryUriListProp = instance.getProperty("GMS_DISCOVERY_URI_LIST");
            String  discoveryUriList = discoveryUriListProp != null ? discoveryUriListProp.getValue() : null;
            if (discoveryUriList != null  && DEFAULT_BROADCAST.equals(broadcastProtocol)) {

                // override default broadcast protocol of udp multicast when GMS_DISCOVERY_URI_LIST has been set.
                instance.setBroadcast("tcp");
                broadcastProtocol = "tcp";
            }
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE,ConfigApiLoggerInfo.clusterGSMBroadCast, instance.getBroadcast());
                logger.log(Level.FINE, ConfigApiLoggerInfo.clusterGSMDeliveryURI , discoveryUriList);
            }
            if (DEFAULT_BROADCAST.equals(broadcastProtocol)) {

                // only generate these values when they are not set AND broadcastProtocol is set to enable UDP multicast.
                // Note: that this is the default for DAS controlled clusters.
                if (instance.getGmsMulticastAddress() == null) {
                    instance.setGmsMulticastAddress(generateHeartbeatAddress());
                }
                if (instance.getGmsMulticastPort() == null) {
                    instance.setGmsMulticastPort(generateHeartbeatPort());
                }

                if (needToAddGmsListenerPort) {
                    instance.getProperty().add(gmsListenerPort);
                }
            } else {

                final String GENERATE = "generate";

                // cover case that broadcast is set to non-multicast and no
                // cluster property GMS_DISCOVERY_URI_LIST exists.
                // create the property and set to "generate".
                // gms-adapter will handle generation of the list when needed
                if (discoveryUriListProp == null) {
                    discoveryUriListProp = instance.createChild(Property.class);
                    discoveryUriListProp.setName("GMS_DISCOVERY_URI_LIST");
                    discoveryUriListProp.setValue(GENERATE);
                    instance.getProperty().add(discoveryUriListProp);
                }

                String TCPPORT = gmsListenerPort.getValue();
                if (GENERATE.equals(discoveryUriListProp.getValue())) {

                    // TODO: implement UDP unicast.

                    // Only tcp mode is supported now.
                    // So either "udpunicast" or "tcp" for broadcast mode is treated the same.
                    if (TCPPORT == null || TCPPORT.trim().charAt(0) == '$') {

                        // generate a random port since user did not provide one.
                        // better fix in future would be to walk existing clusters and pick an unused port.
                        TCPPORT = Integer.toString(new Random(System.currentTimeMillis()).nextInt(9200 - 9090) + 9090);

                        // hardcode all instances to use same default port.
                        // generate mode does not support multiple instances on one machine.
                        gmsListenerPort.setValue(TCPPORT);
                        if (needToAddGmsListenerPort) {
                            instance.getProperty().add(gmsListenerPort);
                        }
                    }
                } else {
                     // lookup server-config and set environment property value
                    // GMS_LISTENER_PORT-clusterName to fixed value.
                    Config config = habitat.getService(Config.class, "server-config");
                    if (config != null) {
                        String propName = String.format("GMS_LISTENER_PORT-%s", instanceName);
                        if (config.getProperty(propName) == null ) {
                            Config writeableConfig = t.enroll(config);
                            SystemProperty gmsListenerPortSysProp = instance.createChild(SystemProperty.class);
                            gmsListenerPortSysProp.setName(propName);
                            if (TCPPORT == null || TCPPORT.trim().charAt(0) == '$') {
                                String generateGmsListenerPort = Integer.toString(
                                        new Random(System.currentTimeMillis()).nextInt(9200 - 9090) + 9090);
                                gmsListenerPortSysProp.setValue(generateGmsListenerPort);
                            } else {
                                gmsListenerPortSysProp.setValue(TCPPORT);
                            }
                            writeableConfig.getSystemProperty().add(gmsListenerPortSysProp);
                        }
                    }
                    if (needToAddGmsListenerPort) {
                        instance.getProperty().add(gmsListenerPort);
                    }
                }
            }

            Resources resources = domain.getResources();
            for (Resource resource : resources.getResources()) {
                if (Resource.Duck.copyToInstance(resource)) {
                    String name=null;
                    if (resource instanceof BindableResource) {
                        name = ((BindableResource) resource).getJndiName();
                    }
                    if (resource instanceof Named) {
                        name = ((Named) resource).getName();
                    }
                    if (name==null) {
                        throw new TransactionFailure("Cannot add un-named resources to the new server instance");
                    }
                    ResourceRef newResourceRef = instance.createChild(ResourceRef.class);
                    newResourceRef.setRef(name);
                    instance.getResourceRef().add(newResourceRef);
                }
            }
            for (Application application : domain.getApplications().getApplications()) {
                if (application.getObjectType().equals("system-all") || application.getObjectType().equals("system-instance")) {
                    ApplicationRef newAppRef = instance.createChild(ApplicationRef.class);
                    newAppRef.setRef(application.getName());
                    instance.getApplicationRef().add(newAppRef);
                }
            }

            if (hosts!=null ||
                    haagentport!=null ||
                    haadminpassword!=null ||
                    haadminpasswordfile!=null ||
                    devicesize!=null ||
                    haproperty!=null ||
                    autohadb!=null ||
                    portbase!=null
                    ) {
               context.getActionReport().setActionExitCode(ActionReport.ExitCode.WARNING);
               context.getActionReport().setMessage("Obsolete options used.");
            }

        }

    private String generateHeartbeatPort() {
        final int MIN_GMS_MULTICAST_PORT = 2048;
        final int MAX_GMS_MULTICAST_PORT = 32000; // be pessimistic for generation of random port and assume
                                                  // ephemeral port range between 32k to 64k.

        int portInterval = MAX_GMS_MULTICAST_PORT - MIN_GMS_MULTICAST_PORT;
        return Integer.toString(Math.round((float)(Math.random() * portInterval)) + MIN_GMS_MULTICAST_PORT);
    }

    private String generateHeartbeatAddress () {
            final int MAX_GMS_MULTICAST_ADDRESS_SUBRANGE = 255;

            final StringBuffer heartbeatAddressBfr = new StringBuffer( "228.9.");
            heartbeatAddressBfr.append(Math.round(Math.random()*MAX_GMS_MULTICAST_ADDRESS_SUBRANGE))
                            .append('.')
                            .append(Math.round(Math.random()*MAX_GMS_MULTICAST_ADDRESS_SUBRANGE));
            return heartbeatAddressBfr.toString();
        }
    }

    @Service
    @PerLookup
    class DeleteDecorator implements DeletionDecorator<Clusters, Cluster> {

        @Param(name="nodeagent", optional=true,obsolete=true)
        String nodeagent;

        // for backward compatibility, ignored.
        @Param(name="autohadboverride", optional=true,obsolete=true)
        String autohadboverride;

        @Inject
        private Domain domain;

        @Inject
        Configs configs;

        @Inject
        private ServerEnvironment env;
        
        @Inject
        CommandRunner runner;

        @Override
        public void decorate(AdminCommandContext context, Clusters parent, Cluster child) throws
                PropertyVetoException, TransactionFailure{
            
            Logger logger = ConfigApiLoggerInfo.getLogger();
            LocalStringManagerImpl localStrings = new LocalStringManagerImpl(Cluster.class);
            final ActionReport report = context.getActionReport();
            
            // check to see if the clustering software is installed
            AdminCommand command = runner.getCommand("copy-config", report, context.getLogger());
            if (command == null) {
                String msg = localStrings.getLocalString("cannot.execute.command",
                        "Cluster software is not installed");
                throw new TransactionFailure(msg);
            }
            
            String instanceConfig = child.getConfigRef();
            final Config config = configs.getConfigByName(instanceConfig);
            Transaction t = Transaction.getTransaction(parent);

            //check if the cluster contains instances throw error that cluster
            //cannot be deleted
            //issue 12172
            List<ServerRef> serverRefs = child.getServerRef();
            StringBuffer namesOfServers = new StringBuffer();
            if (serverRefs.size() > 0) {
                for (ServerRef serverRef: serverRefs){
                    namesOfServers.append(new StringBuffer( serverRef.getRef()).append( ','));
                }

                final String msg = localStrings.getLocalString(
                        "Cluster.hasInstances",
                        "Cluster {0} contains server instances {1} and must not contain any instances"
                        ,child.getName() ,namesOfServers.toString()
                );

                logger.log(Level.SEVERE, ConfigApiLoggerInfo.clusterMustNotContainInstance,new Object[]{child.getName() ,namesOfServers.toString()});
                throw new TransactionFailure(msg);
            }

            // remove GMS_LISTENER_PORT-clusterName prop from server config
            Config serverConfig = configs.getConfigByName("server-config");
            String propName = String.format(
                "GMS_LISTENER_PORT-%s", child.getName());
            SystemProperty gmsProp = serverConfig.getSystemProperty(propName);
            if (gmsProp != null && t != null) {
                Config c = t.enroll(serverConfig);
                List<SystemProperty> propList = c.getSystemProperty();
                propList.remove(gmsProp);
            }

            // check if the config is null or still in use by some other
            // ReferenceContainer or is not <cluster-name>-config -- if so just return...
            if(config == null || domain.getReferenceContainersOf(config).size() > 1 || !instanceConfig.equals(child.getName() + "-config"))
                return;


            try {
                File configConfigDir = new File(env.getConfigDirPath(), config.getName());
                FileUtils.whack(configConfigDir);
            }
            catch(Exception e) {
                // no big deal - just ignore
            }

            try {
                if (t != null) {
                    Configs c = t.enroll(configs);
                    List<Config> configList = c.getConfig();
                    configList.remove(config);
                }
            } catch (TransactionFailure ex) {
                logger.log(Level.SEVERE, ConfigApiLoggerInfo.deleteConfigFailed, new Object[]{instanceConfig, ex});
                String msg = ex.getMessage() != null ? ex.getMessage()
                        : localStrings.getLocalString("deleteConfigFailed",
                        "Unable to remove config {0}", instanceConfig);
                report.setMessage(msg);
                report.setActionExitCode(ActionReport.ExitCode.FAILURE);
                report.setFailureCause(ex);
                throw ex;
            }
        }
    }
}
