/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

import java.beans.PropertyVetoException;
import java.io.IOException;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.sun.common.util.logging.LoggingConfigImpl;
import com.sun.enterprise.config.serverbeans.customvalidators.NotDuplicateTargetName;
import com.sun.enterprise.config.serverbeans.customvalidators.NotTargetKeyword;
import com.sun.enterprise.config.util.ServerHelper;
import com.sun.hk2.component.ExistingSingletonInhabitant;
import static org.glassfish.config.support.Constants.NAME_SERVER_REGEX;

import org.jvnet.hk2.config.*;
import org.jvnet.hk2.config.types.Property;
import org.jvnet.hk2.config.types.PropertyBag;
import org.glassfish.config.support.datatypes.Port;
import org.glassfish.grizzly.config.dom.NetworkConfig;
import org.glassfish.grizzly.config.dom.NetworkListener;
import org.glassfish.quality.ToDo;
import org.glassfish.server.ServerEnvironmentImpl;
import org.glassfish.api.admin.config.*;
import org.jvnet.hk2.component.Habitat;
import org.jvnet.hk2.component.Injectable;

import javax.validation.Payload;
import org.glassfish.api.admin.ServerEnvironment;

/**
 * The configuration defines the configuration of a server instance that can be
 * shared by other server instances. The availability-service and are SE/EE only
 */

/* @XmlType(name = "", propOrder = {
    "httpService",
    "adminService",
    "logService",
    "securityService",
    "monitoringService",
    "diagnosticService",
    "javaConfig",
    "availabilityService",
    "threadPools",
    "groupManagementService",
    "systemProperty",
    "property"
}) */

@Configured
@NotDuplicateTargetName(message="{config.duplicate.name}", payload=Config.class)
public interface Config extends ConfigBeanProxy, Injectable, Named, PropertyBag, SystemPropertyBag, Payload {

    /**
     *  Name of the configured object
     *
     * @return name of the configured object
     FIXME: should set 'key=true'.  See bugs 6039, 6040
     */
    @NotNull
    @NotTargetKeyword(message="{config.reserved.name}", payload=Config.class)
    @Pattern(regexp=NAME_SERVER_REGEX, message="{config.invalid.name}", payload=Config.class)
    @Override
    String getName();

    @Override
    void setName(String value) throws PropertyVetoException;

    /**
     * Gets the value of the dynamicReconfigurationEnabled property.
     *
     * When set to "true" then any changes to the system (e.g. applications
     * deployed, resources created) will be automatically applied to the
     * affected servers without a restart being required. When set to
     * "false" such changes will only be picked up by the affected servers
     * when each server restarts.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="true",dataType=Boolean.class)
    String getDynamicReconfigurationEnabled();

    /**
     * Sets the value of the dynamicReconfigurationEnabled property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setDynamicReconfigurationEnabled(String value) throws PropertyVetoException;

    /**
     * Gets the value of the networkConfig property.
     *
     * @return possible object is {@link NetworkConfig }
     */
    @Element(required=true)
    NetworkConfig getNetworkConfig();

    /**
     * Sets the value of the networkConfig property.
     *
     * @param value allowed object is {@link NetworkConfig }
     */
    void setNetworkConfig(NetworkConfig value) throws PropertyVetoException;

    /**
     * Gets the value of the httpService property.
     *
     * @return possible object is
     *         {@link HttpService }
     */
    @Element(required=true)
    HttpService getHttpService();

    /**
     * Sets the value of the httpService property.
     *
     * @param value allowed object is
     *              {@link HttpService }
     */
    void setHttpService(HttpService value) throws PropertyVetoException;

    /**
     * Gets the value of the adminService property.
     *
     * @return possible object is
     *         {@link AdminService }
     */
    @Element(required=true)
    AdminService getAdminService();

    /**
     * Sets the value of the adminService property.
     *
     * @param value allowed object is
     *              {@link AdminService }
     */
    void setAdminService(AdminService value) throws PropertyVetoException;

    /**
     * Gets the value of the logService property.
     *
     * @return possible object is
     *         {@link LogService }
     */
    @Element(required=true)
    LogService getLogService();

    /**
     * Sets the value of the logService property.
     *
     * @param value allowed object is
     *              {@link LogService }
     */
    void setLogService(LogService value) throws PropertyVetoException;

    /**
     * Gets the value of the securityService property.
     *
     * @return possible object is
     *         {@link SecurityService }
     */
    @Element(required=true)
    SecurityService getSecurityService();

    /**
     * Sets the value of the securityService property.
     *
     * @param value allowed object is
     *              {@link SecurityService }
     */
    void setSecurityService(SecurityService value) throws PropertyVetoException;

    /**
     * Gets the value of the monitoringService property.
     *
     * @return possible object is
     *         {@link MonitoringService }
     */
    @Element(required=true)
    @NotNull
    MonitoringService getMonitoringService();

    /**
     * Sets the value of the monitoringService property.
     *
     * @param value allowed object is
     *              {@link MonitoringService }
     */
    void setMonitoringService(MonitoringService value) throws PropertyVetoException;

    /**
     * Gets the value of the diagnosticService property.
     *
     * @return possible object is
     *         {@link DiagnosticService }
     */
    @Element
    DiagnosticService getDiagnosticService();

    /**
     * Sets the value of the diagnosticService property.
     *
     * @param value allowed object is
     *              {@link DiagnosticService }
     */
    void setDiagnosticService(DiagnosticService value) throws PropertyVetoException;

    /**
     * Gets the value of the javaConfig property.
     *
     * @return possible object is
     *         {@link JavaConfig }
     */
    @Element(required=true)
    JavaConfig getJavaConfig();

    /**
     * Sets the value of the javaConfig property.
     *
     * @param value allowed object is
     *              {@link JavaConfig }
     */
    void setJavaConfig(JavaConfig value) throws PropertyVetoException;

    /**
     * Gets the value of the availabilityService property.
     *
     * @return possible object is
     *         {@link AvailabilityService }
     */
    @Element
    @NotNull
    AvailabilityService getAvailabilityService();

    /**
     * Sets the value of the availabilityService property.
     *
     * @param value allowed object is
     *              {@link AvailabilityService }
     */
    void setAvailabilityService(AvailabilityService value) throws PropertyVetoException;

    /**
     * Gets the value of the threadPools property.
     *
     * @return possible object is
     *         {@link ThreadPools }
     */
    @Element(required=true)
    ThreadPools getThreadPools();

    /**
     * Sets the value of the threadPools property.
     *
     * @param value allowed object is
     *              {@link ThreadPools }
     */
    void setThreadPools(ThreadPools value) throws PropertyVetoException;

    /**
     * Gets the value of the groupManagementService property.
     *
     * @return possible object is
     *         {@link GroupManagementService }
     */
    @Element
    @NotNull
    GroupManagementService getGroupManagementService();

    /**
     * Sets the value of the groupManagementService property.
     *
     * @param value allowed object is
     *              {@link GroupManagementService }
     */
    void setGroupManagementService(GroupManagementService value) throws PropertyVetoException;

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
    @ToDo(priority=ToDo.Priority.IMPORTANT, details="Any more legal system properties?" )
@PropertiesDesc(
    systemProperties=true,
    props={
        @PropertyDesc(name="HTTP_LISTENER_PORT", defaultValue="8080", dataType=Port.class),
        @PropertyDesc(name="HTTP_SSL_LISTENER_PORT", defaultValue="1043", dataType=Port.class),
        @PropertyDesc(name="HTTP_ADMIN_LISTENER_PORT", defaultValue="4848", dataType=Port.class),
        @PropertyDesc(name="IIOP_LISTENER_PORT", defaultValue="3700", dataType=Port.class),
        @PropertyDesc(name="IIOP_SSL_LISTENER_PORT", defaultValue="1060", dataType=Port.class),
        @PropertyDesc(name="IIOP_SSL_MUTUALAUTH_PORT", defaultValue="1061", dataType=Port.class),
        @PropertyDesc(name="JMX_SYSTEM_CONNECTOR_PORT", defaultValue="8686", dataType=Port.class)
    }
    )
    @Element
    @Override
    List<SystemProperty> getSystemProperty();


    //DuckTyped for accessing the logging.properties file

    @DuckTyped
    Map<String, String> getLoggingProperties();

    @DuckTyped
    String setLoggingProperty(String property, String value);

    @DuckTyped
    Map<String, String> updateLoggingProperties( Map<String, String> properties);

    @DuckTyped
    NetworkListener getAdminListener();

    @DuckTyped
    <T extends ConfigExtension> T createDefaultChildByType(Class<T> type);




    /**
     * Return an extension configuration given the extension type.
     *
     * @param type type of the requested extension configuration
     * @param <T> interface subclassing the ConfigExtension type
     * @return a configuration proxy of type T or null if there is no such
     * configuration with that type.
     */
    @DuckTyped
    <T extends ConfigExtension> T getExtensionByType(Class<T> type);
    
    /**
     * Add name as an index key for this Config and for the objects that are
     * directly referenced by this Config.  This includes all of the Config 
     * extensions.
     * 
     * @param habitat Habitat that contains this Config
     * @param name name to use to identify the objects
     */
    @DuckTyped
    void addIndex(Habitat habitat, String name);

    class Duck {

        public static String setLoggingProperty(Config c, String property, String value){
            ConfigBean cb = (ConfigBean) ((ConfigView)Proxy.getInvocationHandler(c)).getMasterView();
            ServerEnvironmentImpl env = cb.getHabitat().getComponent(ServerEnvironmentImpl.class);
            LoggingConfigImpl loggingConfig = new LoggingConfigImpl();
            loggingConfig.setupConfigDir(env.getConfigDirPath(), env.getLibPath());

            String prop = null;
            try{
                   prop= loggingConfig.setLoggingProperty(property, value);
            } catch (IOException ex){
            }
            return prop;
        }

        public static Map<String, String>getLoggingProperties(Config c) {
            ConfigBean cb = (ConfigBean) ((ConfigView)Proxy.getInvocationHandler(c)).getMasterView();
            ServerEnvironmentImpl env = cb.getHabitat().getComponent(ServerEnvironmentImpl.class);
            LoggingConfigImpl loggingConfig = new LoggingConfigImpl();
            loggingConfig.setupConfigDir(env.getConfigDirPath(), env.getLibPath());

            Map <String, String> map = new HashMap<String, String>() ;
            try {
                map = loggingConfig.getLoggingProperties();
            } catch (IOException ex){
            }
            return map;
        }

        public static Map<String, String>updateLoggingProperties(Config c, Map<String, String>properties){
            ConfigBean cb = (ConfigBean) ((ConfigView)Proxy.getInvocationHandler(c)).getMasterView();
            ServerEnvironmentImpl env = cb.getHabitat().getComponent(ServerEnvironmentImpl.class);
            LoggingConfigImpl loggingConfig = new LoggingConfigImpl();
            loggingConfig.setupConfigDir(env.getConfigDirPath(), env.getLibPath());

            Map <String, String> map = new HashMap<String, String>() ;
            try {
                map = loggingConfig.updateLoggingProperties(properties);
            } catch (IOException ex){
            }
            return map;
        }

        public static <T extends ConfigExtension> T getExtensionByType(Config c, Class<T> type) throws ClassNotFoundException,TransactionFailure{
            T configExtension = null;
            for (Container extension : c.getContainers()) {
                try {
                    configExtension = type.cast(extension);
                    return configExtension ;


                } catch (Exception e) {
                    // ignore, not the right type.
                }
            }
            if (configExtension == null ) {

                return createDefaultChildByType(c ,type);

            }

            return null;
        }

        public static NetworkListener getAdminListener(Config c) {
            return ServerHelper.getAdminListener(c);
        }
        
        public static void addIndex(Config c, Habitat habitat, String name) {
            habitat.addIndex(new ExistingSingletonInhabitant<Config>(c),
                Config.class.getName(), ServerEnvironment.DEFAULT_INSTANCE_NAME);
            
            // directly referenced objects
            ConfigBeanProxy dirref[] = {
                c.getAdminService(),
                c.getAvailabilityService(),
                c.getDiagnosticService(),
                c.getGroupManagementService(),
                c.getHttpService(),
                c.getJavaConfig(),
                c.getLogService(),
                c.getMonitoringService(),
                c.getNetworkConfig(),
                c.getSecurityService(),
                c.getThreadPools(),
            };
            for (ConfigBeanProxy cbp : dirref) {
                if (cbp != null) {
                    habitat.addIndex(new ExistingSingletonInhabitant<ConfigBeanProxy>(cbp),
                            ConfigSupport.getImpl(cbp).getProxyType().getName(),
                            ServerEnvironment.DEFAULT_INSTANCE_NAME);
                }
            }
            
            // containers
            for (Container extension : c.getContainers()) {
                habitat.addIndex(new ExistingSingletonInhabitant<Container>(extension),
                        ConfigSupport.getImpl(extension).getProxyType().getName(), 
                        ServerEnvironment.DEFAULT_INSTANCE_NAME);
            }
        }

        public static  <U extends ConfigExtension>
                U createDefaultChildByType(Config config,Class<U> p)
                throws TransactionFailure {

            final Class<U> parentElem = p;
            ConfigSupport.apply(new SingleConfigCode<Config>() {

                @Override
                public Object run(Config parent) throws PropertyVetoException, TransactionFailure {
                    ConfigExtension child = parent.createChild(parentElem);
                    Dom.unwrap(child).addDefaultChildren();
                    parent.getContainers().add(child);
                    return child;
                }
            }, config);
            return config.getExtensionByType(p);

        }





    }
    /**
    	Properties as per {@link PropertyBag}
     */
    @ToDo(priority=ToDo.Priority.IMPORTANT, details="Provide PropertyDesc for legal props" )
    @PropertiesDesc(props={})
    @Element
    @Override
    List<Property> getProperty();

    /**
     * Get the configuration for other types of containers.
     * 
     * @return  list of containers configuration
     */
    @Element("*")
    List<Container> getContainers();
}



