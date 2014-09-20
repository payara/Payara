/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.ejb.config;

import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.Element;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.types.Property;
import org.jvnet.hk2.config.types.PropertyBag;

import java.beans.PropertyVetoException;
import java.util.List;

import org.glassfish.api.admin.config.PropertiesDesc;
import org.glassfish.quality.ToDo;
import com.sun.enterprise.config.serverbeans.AvailabilityServiceExtension;


/* @XmlType(name = "", propOrder = {
    "property"
}) */

@Configured
public interface EjbContainerAvailability extends ConfigBeanProxy,
        PropertyBag, AvailabilityServiceExtension {
    
    /**
     * Gets the value of the availabilityEnabled property.
     *
     * This boolean flag controls whether availability is enabled for SFSB
     * checkpointing (and potentially passivation). If this is "false",
     * then all SFSB checkpointing is disabled for all j2ee apps and ejb modules
     * If it is "true" (and providing that the global availability-enabled in
     * availability-service is also "true", then j2ee apps and stand-alone ejb
     * modules may be ha enabled. Finer-grained control exists at lower levels.
     * If this attribute is missing, it inherits the value of the global
     * availability-enabled under availability-service.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute(defaultValue="true")
    String getAvailabilityEnabled();

    /**
     * Sets the value of the availabilityEnabled property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setAvailabilityEnabled(String value) throws PropertyVetoException;

    /**
     * Gets the value of the sfsbHaPersistenceType property.
     *
     * The persistence type used by the EJB Stateful Session Bean Container for
     * checkpointing and passivating availability-enabled beans' state.
     * Default is "ha".
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="replicated")
    String getSfsbHaPersistenceType();

    /**
     * Sets the value of the sfsbHaPersistenceType property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setSfsbHaPersistenceType(String value) throws PropertyVetoException;

    /**
     * Gets the value of the sfsbPersistenceType property.
     *
     * Specifies the passivation mechanism for stateful session beans that do
     * not have availability enabled. Default is "file".
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="file")
    String getSfsbPersistenceType();

    /**
     * Sets the value of the sfsbPersistenceType property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setSfsbPersistenceType(String value) throws PropertyVetoException;

    /**
     * Gets the value of the sfsbCheckpointEnabled property.
     *
     * This attribute is deprecated, replaced by availability-enabled and will
     * be ignored if present.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    String getSfsbCheckpointEnabled();

    /**
     * Sets the value of the sfsbCheckpointEnabled property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setSfsbCheckpointEnabled(String value) throws PropertyVetoException;

    /**
     * Gets the value of the sfsbQuickCheckpointEnabled property.
     *
     * This attribute is deprecated and will be ignored if present.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    String getSfsbQuickCheckpointEnabled();

    /**
     * Sets the value of the sfsbQuickCheckpointEnabled property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setSfsbQuickCheckpointEnabled(String value) throws PropertyVetoException;

    /**
     * Gets the value of the sfsbStorePoolName property.
     * This is the jndi-name for the JDBC Connection Pool used by the
     * EJB Stateful Session Bean Container for use in checkpointing/passivation
     * when persistence-type = "ha". See sfsb-ha-persistence-type and
     * sfsb-persistence-type for more details. It will default to value of
     * store-pool-name under availability-service (ultimately "jdbc/hastore").
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    String getSfsbStorePoolName();

    /**
     * Sets the value of the sfsbStorePoolName property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setSfsbStorePoolName(String value) throws PropertyVetoException;
    
    /**
    	Properties as per {@link PropertyBag}
     */
    @ToDo(priority=ToDo.Priority.IMPORTANT, details="Provide PropertyDesc for legal props" )
    @PropertiesDesc(props={})
    @Element
    List<Property> getProperty();
}
