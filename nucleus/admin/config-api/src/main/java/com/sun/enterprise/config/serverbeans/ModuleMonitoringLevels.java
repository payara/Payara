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

package com.sun.enterprise.config.serverbeans;

import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.Element;

import java.beans.PropertyVetoException;
import java.util.List;

import org.glassfish.api.admin.config.PropertiesDesc;
import org.jvnet.hk2.config.types.Property;
import org.jvnet.hk2.config.types.PropertyBag;

import org.glassfish.quality.ToDo;

import javax.validation.constraints.Pattern;

/**
 *
 */

/* @XmlType(name = "", propOrder = {
    "property"
}) */

@Configured
public interface ModuleMonitoringLevels extends ConfigBeanProxy, PropertyBag {

    /**
     * Gets the value of the threadPool property.
     *
     * All the thread-pools used by the run time
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="OFF")
    @Pattern(regexp="(OFF|LOW|HIGH)")
    public String getThreadPool();

    /**
     * Sets the value of the threadPool property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setThreadPool(String value) throws PropertyVetoException;

    /**
     * Gets the value of the orb property.
     *
     * Specifies the level for connection managers of the orb, which apply to
     * connections to the orb
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="OFF")
    @Pattern(regexp="(OFF|LOW|HIGH)")
    public String getOrb();

    /**
     * Sets the value of the orb property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setOrb(String value) throws PropertyVetoException;

    /**
     * Gets the value of the ejbContainer property.
     *
     * Various ejbs deployed to the server, ejb-pools, ejb-caches & ejb-methods
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="OFF")
    @Pattern(regexp="(OFF|LOW|HIGH)")
    public String getEjbContainer();

    /**
     * Sets the value of the ejbContainer property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setEjbContainer(String value) throws PropertyVetoException;

    /**
     * Gets the value of the webContainer property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="OFF")
    @Pattern(regexp="(OFF|LOW|HIGH)")
    public String getWebContainer();

    /**
     * Sets the value of the webContainer property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setWebContainer(String value) throws PropertyVetoException;

   /**
     * Gets the value of the deployment property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="OFF")
    @Pattern(regexp="(OFF|LOW|HIGH)")
    public String getDeployment();

    /**
     * Sets the value of the webContainer property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setDeployment(String value) throws PropertyVetoException;

    /**
     * Gets the value of the transactionService property.
     *
     * Transaction subsystem
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="OFF")
    @Pattern(regexp="(OFF|LOW|HIGH)")
    public String getTransactionService();

    /**
     * Sets the value of the transactionService property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setTransactionService(String value) throws PropertyVetoException;

    /**
     * Gets the value of the httpService property.
     *
     * http engine and the http listeners therein.
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="OFF")
    @Pattern(regexp="(OFF|LOW|HIGH)")
    public String getHttpService();

    /**
     * Sets the value of the httpService property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setHttpService(String value) throws PropertyVetoException;

    /**
     * Gets the value of the jdbcConnectionPool property.
     *
     * Monitoring level for all the jdbc-connection-pools used by the runtime.
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="OFF")
    @Pattern(regexp="(OFF|LOW|HIGH)")
    public String getJdbcConnectionPool();

    /**
     * Sets the value of the jdbcConnectionPool property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setJdbcConnectionPool(String value) throws PropertyVetoException;

    /**
     * Gets the value of the connectorConnectionPool property.
     *
     * Monitoring level for all the connector-connection-pools used by runtime.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="OFF")
    @Pattern(regexp="(OFF|LOW|HIGH)")
    public String getConnectorConnectionPool();

    /**
     * Sets the value of the connectorConnectionPool property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setConnectorConnectionPool(String value) throws PropertyVetoException;

    /**
     * Gets the value of the connectorService property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="OFF")
    @Pattern(regexp="(OFF|LOW|HIGH)")
    public String getConnectorService();

    /**
     * Sets the value of the connectorService property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setConnectorService(String value) throws PropertyVetoException;

    /**
     * Gets the value of the jmsService property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="OFF")
    @Pattern(regexp="(OFF|LOW|HIGH)")
    public String getJmsService();

    /**
     * Sets the value of the jmsService property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setJmsService(String value) throws PropertyVetoException;

    /**
     * Gets the value of the jvm property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="OFF")
    @Pattern(regexp="(OFF|LOW|HIGH)")
    public String getJvm();

    /**
     * Sets the value of the jvm property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setJvm(String value) throws PropertyVetoException;
    
    /**
     * Gets the value of the security property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="OFF")
    @Pattern(regexp="(OFF|LOW|HIGH)")
    public String getSecurity();

    /**
     * Sets the value of the security property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setSecurity(String value) throws PropertyVetoException;
    
    /**
     * Gets the value of the web-service-container property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="OFF")
    @Pattern(regexp="(OFF|LOW|HIGH)")
    public String getWebServicesContainer();

    /**
     * Sets the value of the web-service-container property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setWebServicesContainer(String value) throws PropertyVetoException;
    

    /**
     * Gets the value of the jpa property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="OFF")
    @Pattern(regexp="(OFF|LOW|HIGH)")
    public String getJpa();

    /**
     * Sets the value of the jpa property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setJpa(String value) throws PropertyVetoException;
    

    /**
     * Gets the value of the jax-ra property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="OFF")
    @Pattern(regexp="(OFF|LOW|HIGH)")
    public String getJersey();

    /**
     * Sets the value of the jax-ra property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setJersey(String value) throws PropertyVetoException;

    /**
     * Gets the value of the cloudTenantManager property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="OFF")
    @Pattern(regexp="(OFF|LOW|HIGH)")
    public String getCloudTenantManager();

    /**
     * Sets the value of the cloudTenantManager property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setCloudTenantManager(String value) throws PropertyVetoException;

    /**
     * Gets the value of the cloud property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="OFF")
    @Pattern(regexp="(OFF|LOW|HIGH)")
    public String getCloud();

    /**
     * Sets the value of the cloud property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setCloud(String value) throws PropertyVetoException;


    /**
     * Gets the value of the cloud Orchestrator property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="OFF")
    @Pattern(regexp="(OFF|LOW|HIGH)")
    public String getCloudOrchestrator();

    /**
     * Sets the value of the cloud Orchestrator property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setCloudOrchestrator(String value) throws PropertyVetoException;

    /**
     * Gets the value of the cloud Elasticity property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="OFF")
    @Pattern(regexp="(OFF|LOW|HIGH)")
    public String getCloudElasticity();

    /**
     * Sets the value of the cloud elasticity property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setCloudElasticity(String value) throws PropertyVetoException;

    /**
    	Properties as per {@link PropertyBag}
     */

   /**
     * Gets the value of the cloud IMS property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="OFF")
    @Pattern(regexp="(OFF|LOW|HIGH)")
    public String getCloudVirtAssemblyService();

    /**
     * Sets the value of the cloud IMS property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setCloudVirtAssemblyService(String value) throws PropertyVetoException;
    /**
        Properties as per {@link PropertyBag}
     */

    @ToDo(priority=ToDo.Priority.IMPORTANT, details="Provide PropertyDesc for legal props" )
    @PropertiesDesc(props={})
    @Element
    List<Property> getProperty();
}
