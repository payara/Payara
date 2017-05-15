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

package org.glassfish.loadbalancer.config;

import org.glassfish.loadbalancer.config.customvalidators.RefConstraint;
import org.glassfish.loadbalancer.config.customvalidators.RefValidator;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import java.beans.PropertyVetoException;

import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.config.serverbeans.Domain;
import com.sun.enterprise.config.serverbeans.Ref;
import com.sun.enterprise.config.serverbeans.ServerRef;
import com.sun.enterprise.config.serverbeans.ClusterRef;
import com.sun.logging.LogDomains;
import java.util.Date;

import org.glassfish.config.support.*;
import org.glassfish.quality.ToDo;


import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;
import org.jvnet.hk2.config.*;
import org.jvnet.hk2.config.types.Property;
import org.jvnet.hk2.config.types.PropertyBag;

import org.glassfish.api.Param;
import org.glassfish.api.admin.AdminCommandContext;
import org.glassfish.api.admin.config.PropertiesDesc;

import javax.inject.Inject;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import static org.glassfish.config.support.Constants.*;
import javax.validation.Payload;

/**
 *
 */

/* @XmlType(name = "", propOrder = {
    "clusterRefOrServerRef",
    "property"
}) */

@Configured
@RefConstraint(message="{ref.invalid}", payload= RefValidator.class)
public interface LbConfig extends ConfigBeanProxy, PropertyBag, Payload {

    String LAST_APPLIED_PROPERTY = "last-applied";
    String LAST_EXPORTED_PROPERTY = "last-exported";

    /**
     * Gets the value of the name property.
     *
     * Name of the load balancer configuration
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute(key=true)
    @Pattern(regexp=NAME_REGEX, message="{lbconfig.invalid.name}", payload=LbConfig.class)
    @NotNull
    String getName();

    /**
     * Sets the value of the name property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    @Param (name = "name", primary=true)
    void setName(String value) throws PropertyVetoException;

    /**
     * Gets the value of the responseTimeoutInSeconds property.
     *
     * Period within which a server must return a response or otherwise it will
     * be considered unhealthy. Default value is 60 seconds. Must be greater
     * than or equal to 0. A value of 0 effectively turns off this check
     * functionality, meaning the server will always be considered healthy
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="60")
    @Min(value=0)
    String getResponseTimeoutInSeconds();

    /**
     * Sets the value of the responseTimeoutInSeconds property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    @Param(name = "responsetimeout", optional=true)
    void setResponseTimeoutInSeconds(String value) throws PropertyVetoException;

    /**
     * Gets the value of the httpsRouting property.
     *
     * Boolean flag indicating how load-balancer will route https requests.
     * If true then an https request to the load-balancer will result in an
     * https request to the server; if false then https requests to the
     * load-balancer result in http requests to the server.
     * Default is to use http (i.e. value of false)
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="false",dataType=Boolean.class)
    String getHttpsRouting();

    /**
     * Sets the value of the httpsRouting property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    @Param(name = "httpsrouting", optional=true)
    void setHttpsRouting(String value) throws PropertyVetoException;

    /**
     * Gets the value of the reloadPollIntervalInSeconds property.
     *
     * Maximum period, in seconds, that a change to the load balancer
     * configuration file takes before it is detected by the load balancer and
     * the file reloaded. A value of 0 indicates that reloading is disabled.
     * Default period is 1 minute (60 sec)
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="60")
    String getReloadPollIntervalInSeconds();

    /**
     * Sets the value of the reloadPollIntervalInSeconds property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    @Param(name = "reloadinterval", optional=true)
    void setReloadPollIntervalInSeconds(String value) throws PropertyVetoException;

    /**
     * Gets the value of the monitoringEnabled property.
     *
     * Boolean flag that determines whether monitoring is switched on or not.
     * Default is that monitoring is switched off (false) 
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="false",dataType=Boolean.class)
    String getMonitoringEnabled();

    /**
     * Sets the value of the monitoringEnabled property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    @Param(name = "monitor", optional=true)
    void setMonitoringEnabled(String value) throws PropertyVetoException;

    /**
     * Gets the value of the routeCookieEnabled property.
     *
     * Boolean flag that determines whether a route cookie is or is not enabled.
     * Default is enabled (true).
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="true",dataType=Boolean.class)
    String getRouteCookieEnabled();

    /**
     * Sets the value of the routeCookieEnabled property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    @Param(name = "routecookie", optional=true)
    void setRouteCookieEnabled(String value) throws PropertyVetoException;

    /**
     * Gets the value of the clusterRefOrServerRef property.
     * <p/>
     * <p/>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the clusterRefOrServerRef property.
     * <p/>
     * <p/>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getClusterRefOrServerRef().add(newItem);
     * </pre>
     * <p/>
     * <p/>
     * <p/>
     * Objects of the following type(s) are allowed in the list
     * {@link ClusterRef }
     * {@link ServerRef }
     */
    @Element("*")
    List<Ref> getClusterRefOrServerRef();

    /**
    	Properties as per {@link PropertyBag}
     */
    @Override
    @ToDo(priority=ToDo.Priority.IMPORTANT, details="Provide PropertyDesc for legal props" )
    @PropertiesDesc(props={})
    @Element
    List<Property> getProperty();

    @DuckTyped
    <T> List<T> getRefs(Class<T> type);

    @DuckTyped
    <T> T getRefByRef(Class<T> type, String ref);   

    @DuckTyped
    Date getLastExported();

    @DuckTyped
    Date getLastApplied();

    @DuckTyped
    boolean setLastExported();

    @DuckTyped
    boolean setLastApplied();

    public class Duck {
        public static <T> List<T> getRefs(LbConfig lc, Class<T> type) {
            List<T> refs = new ArrayList<T>();
            for (Object r : lc.getClusterRefOrServerRef()) {
                if (type.isInstance(r)) {
                    refs.add(type.cast(r));
                }
            }
            // you have to return an umodifiable list since this list
            // is not the real list of elements as maintained by this config bean
            return Collections.unmodifiableList(refs);
        }

        public static <T> T getRefByRef(LbConfig lc, Class<T> type, String ref) {
            if (ref == null) {
                return null;
            }

            for (Ref r : lc.getClusterRefOrServerRef())
                if (type.isInstance(r) && r.getRef().equals(ref))
                    return type.cast(r);

            return null;
        }

        public static Date getLastExported(LbConfig lc) {
            return getInternalPropertyValue(lc, LAST_EXPORTED_PROPERTY);
        }

        public static Date getLastApplied(LbConfig lc) {
            return getInternalPropertyValue(lc, LAST_APPLIED_PROPERTY);
        }

        private static Date getInternalPropertyValue(LbConfig lc,
                String propertyName) {
            String propertyValue = lc.getPropertyValue(propertyName);
            if(propertyValue == null){
                return null;
            }
            return new Date(Long.parseLong(propertyValue));
        }

        public static boolean setLastExported(LbConfig lc) {
            return setInternalProperty(lc, LAST_EXPORTED_PROPERTY);
        }

        public static boolean setLastApplied(LbConfig lc) {
            return setInternalProperty(lc, LAST_APPLIED_PROPERTY);
        }

        private static boolean setInternalProperty(LbConfig lc,
                String propertyName) {
            Property property = lc.getProperty(propertyName);
            Transaction transaction = new Transaction();
            try {
                if (property == null) {
                    ConfigBeanProxy lcProxy = transaction.enroll(lc);
                    property = lcProxy.createChild(Property.class);
                    property.setName(propertyName);
                    property.setValue(String.valueOf((new Date()).getTime()));
                    ((LbConfig)lcProxy).getProperty().add(property);
                } else {
                    ConfigBeanProxy propertyProxy = transaction.enroll(property);
                    ((Property)propertyProxy).setValue(String.valueOf(
                            (new Date()).getTime()));
                }
                transaction.commit();
            } catch (Exception ex) {
                transaction.rollback();
                Logger logger = LogDomains.getLogger(LbConfig.class,
                        LogDomains.ADMIN_LOGGER);
                LocalStringManagerImpl localStrings =
                        new LocalStringManagerImpl(LbConfig.class);
                String msg = localStrings.getLocalString(
                        "UnableToSetPropertyInLbconfig",
                        "Unable to set property {0} in lbconfig with name {1}",
                        new String[]{propertyName, lc.getName()});
                logger.log(Level.SEVERE, msg);
                logger.log(Level.FINE, "Exception when trying to set property "
                        + propertyName + " in lbconfig " + lc.getName(), ex);
                return false;
            }
            return true;
        }

    }
    
    @Service
    @PerLookup
    class Decorator implements CreationDecorator<LbConfig> {

        @Param (name = "name", optional=true)
        String config_name;

        @Param(optional=true)
        String target;        

        @Param (optional=true, defaultValue="60")
        String responsetimeout;

        @Param (optional=true, defaultValue="false")
        Boolean httpsrouting;

        @Param (optional=true, defaultValue="60")
        String reloadinterval;

        @Param (optional=true, defaultValue="false")
        Boolean monitor;

        @Param (optional=true, defaultValue="true")
        Boolean routecookie;

        @Param(optional=true, name="property", separator=':')
        Properties properties;

        @Inject
        Domain domain;

        /**
         * Create lb-config entries
         * tasks :
         *      - ensures that it references an existing cluster

         * @param context administration command context
         * @param instance newly created configuration element
         * @throws TransactionFailure
         * @throws PropertyVetoException
         *
         */
        @Override
        public void decorate(AdminCommandContext context, final LbConfig instance) throws TransactionFailure, PropertyVetoException {
            Logger logger = LogDomains.getLogger(LbConfig.class, LogDomains.ADMIN_LOGGER);
            LocalStringManagerImpl localStrings = new LocalStringManagerImpl(LbConfig.class);            

            if (config_name == null && target == null) {
                String msg = localStrings.getLocalString("RequiredTargetOrConfig", "Neither LB config name nor target specified");
                throw new TransactionFailure(msg);
            }

            // generate lb config name if not specified
            if (config_name == null) {
                config_name = target + "_LB_CONFIG";
            }
            
            LbConfigs lbconfigs = domain.getExtensionByType(LbConfigs.class);
            //create load-balancers parent element if it does not exist
            if (lbconfigs == null) {
                Transaction transaction = new Transaction();
                try {
                    ConfigBeanProxy domainProxy = transaction.enroll(domain);
                    lbconfigs = domainProxy.createChild(LbConfigs.class);
                    ((Domain) domainProxy).getExtensions().add(lbconfigs);
                    transaction.commit();
                } catch (TransactionFailure ex) {
                    transaction.rollback();
                    String msg = localStrings.getLocalString("LbConfigsCreationFailed", "Creation of parent element lb-configs failed");
                    throw new TransactionFailure(msg, ex);
                } catch (RetryableException ex) {
                    transaction.rollback();
                    String msg = localStrings.getLocalString("LbConfigsCreationFailed", "Creation of parent element lb-configs failed");
                    throw new TransactionFailure(msg, ex);
                }
            }

            if (lbconfigs.getLbConfig(config_name) != null) {
                String msg = localStrings.getLocalString("LbConfigExists", config_name);
                throw new TransactionFailure(msg);
            }

            instance.setName(config_name);
            instance.setResponseTimeoutInSeconds(responsetimeout);
            instance.setReloadPollIntervalInSeconds(reloadinterval);
            instance.setMonitoringEnabled(monitor==null ? null : monitor.toString());
            instance.setRouteCookieEnabled(routecookie==null ? null : routecookie.toString());
            instance.setHttpsRouting(httpsrouting==null ? null : httpsrouting.toString());

            // creates a reference to the target
            if (target != null) {                
                if (domain.getClusterNamed(target) != null) {
                   ClusterRef cRef = instance.createChild(ClusterRef.class);
                   cRef.setRef(target);
                   instance.getClusterRefOrServerRef().add(cRef);
                } else if (domain.isServer(target)) {
                    ServerRef sRef = instance.createChild(ServerRef.class);
                    sRef.setRef(target);
                    instance.getClusterRefOrServerRef().add(sRef);
                } else {
                    String msg = localStrings.getLocalString("InvalidTarget", target);
                    throw new TransactionFailure(msg);
                }
            }

            // add properties
            if (properties != null) {
                for (Object propname: properties.keySet()) {
                    Property newprop = instance.createChild(Property.class);
                    newprop.setName((String) propname);
                    newprop.setValue(properties.getProperty((String) propname));
                    instance.getProperty().add(newprop);
                }
            }
            logger.info(localStrings.getLocalString("http_lb_admin.LbConfigCreated",
                    "Load balancer configuration {0} created.", config_name));
        }
    }

    @Service
    @PerLookup
    class DeleteDecorator implements DeletionDecorator<LbConfigs, LbConfig> {
        @Inject
        private Domain domain;

        @Override
        public void decorate(AdminCommandContext context, LbConfigs parent, LbConfig child)
                throws PropertyVetoException, TransactionFailure {
            Logger logger = LogDomains.getLogger(LbConfig.class, LogDomains.ADMIN_LOGGER);
            LocalStringManagerImpl localStrings = new LocalStringManagerImpl(LbConfig.class);

            String lbConfigName = child.getName();
            LbConfig lbConfig = domain.getExtensionByType(LbConfigs.class).getLbConfig(lbConfigName);

            //Ensure there are no refs 
            if ( (lbConfig.getClusterRefOrServerRef().size() != 0 ) ) {
                String msg = localStrings.getLocalString("LbConfigNotEmpty", lbConfigName);
                throw new TransactionFailure(msg);
            }
            logger.info(localStrings.getLocalString("http_lb_admin.LbConfigDeleted", lbConfigName));
        }
   }
}
