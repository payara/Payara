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

package org.glassfish.connectors.config;

import com.sun.enterprise.config.serverbeans.BindableResource;
import com.sun.enterprise.config.serverbeans.Resource;
import org.glassfish.resourcebase.resources.ResourceDeploymentOrder;
import org.glassfish.resourcebase.resources.ResourceTypeOrder;
import org.jvnet.hk2.config.*;
import org.jvnet.hk2.config.types.Property;
import org.jvnet.hk2.config.types.PropertyBag;

import java.beans.PropertyVetoException;
import java.util.List;

import org.glassfish.api.admin.config.PropertiesDesc;

import org.glassfish.quality.ToDo;


/**
 * Persistence Manager runtime configuration
 */

/* @XmlType(name = "", propOrder = {
    "description",
    "property"
}) */
@Configured
@ResourceTypeOrder(deploymentOrder= ResourceDeploymentOrder.PERSISTENCE_RESOURCE)
public interface PersistenceManagerFactoryResource extends ConfigBeanProxy, Resource, PropertyBag, BindableResource {

    /**
     * Gets the value of the factoryClass property.
     *
     * Class that creates persistence manager instance
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="com.sun.jdo.spi.persistence.support.sqlstore.impl.PersistenceManagerFactoryImpl")
    public String getFactoryClass();

    /**
     * Sets the value of the factoryClass property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setFactoryClass(String value) throws PropertyVetoException;

    /**
     * Gets the value of the jdbcResourceJndiName property.
     *
     * jdbc resource with which database connections are obtained
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    public String getJdbcResourceJndiName();

    /**
     * Sets the value of the jdbcResourceJndiName property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setJdbcResourceJndiName(String value) throws PropertyVetoException;

    /**
     * Gets the value of the enabled property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="true",dataType=Boolean.class)
    public String getEnabled();

    /**
     * Sets the value of the enabled property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setEnabled(String value) throws PropertyVetoException;

    /**
     * Gets the value of the description property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    public String getDescription();

    /**
     * Sets the value of the description property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setDescription(String value) throws PropertyVetoException;
    
    /**
    	Properties as per {@link org.glassfish.api.admin.config.PropertyBag}
     */
    @ToDo(priority=ToDo.Priority.IMPORTANT, details="Provide PropertyDesc for legal props" )
    @PropertiesDesc(props={})
    @Element
    List<Property> getProperty();

    @DuckTyped
    String getIdentity();

    class Duck {
        public static String getIdentity(PersistenceManagerFactoryResource resource){
            return resource.getJndiName();
        }
    }

}
