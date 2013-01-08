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

package org.glassfish.resources.config;

import com.sun.enterprise.config.serverbeans.BindableResource;
import com.sun.enterprise.config.serverbeans.Resource;
import org.glassfish.admin.cli.resources.ResourceConfigCreator;
import org.glassfish.api.admin.config.PropertiesDesc;
import org.glassfish.quality.ToDo;
import org.glassfish.admin.cli.resources.UniqueResourceNameConstraint;
import org.glassfish.resourcebase.resources.ResourceDeploymentOrder;
import org.glassfish.resourcebase.resources.ResourceTypeOrder;
import org.jvnet.hk2.config.*;
import org.jvnet.hk2.config.types.Property;
import org.jvnet.hk2.config.types.PropertyBag;

import javax.validation.constraints.NotNull;
import java.beans.PropertyVetoException;
import java.util.List;

/**
 * Resource residing in an external JNDI repository
 */

/* @XmlType(name = "", propOrder = {
    "description",
    "property"
}) */

@Configured
@ResourceConfigCreator(commandName="create-jndi-resource")
@UniqueResourceNameConstraint(message="{resourcename.isnot.unique}", payload=ExternalJndiResource.class)
@ResourceTypeOrder(deploymentOrder= ResourceDeploymentOrder.EXTERNALJNDI_RESOURCE)
public interface ExternalJndiResource extends ConfigBeanProxy, Resource,
        PropertyBag, BindableResource {


    /**
     * Gets the value of the jndiLookupName property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    @NotNull
    String getJndiLookupName();

    /**
     * Sets the value of the jndiLookupName property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setJndiLookupName(String value) throws PropertyVetoException;

    /**
     * Gets the value of the resType property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    @NotNull
    String getResType();

    /**
     * Sets the value of the resType property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setResType(String value) throws PropertyVetoException;

    /**
     * Gets the value of the factoryClass property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    @NotNull
    String getFactoryClass();

    /**
     * Sets the value of the factoryClass property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setFactoryClass(String value) throws PropertyVetoException;

    /**
     * Gets the value of the enabled property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="true",dataType=Boolean.class)
    String getEnabled();

    /**
     * Sets the value of the enabled property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setEnabled(String value) throws PropertyVetoException;

    /**
     * Gets the value of the description property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    String getDescription();

    /**
     * Sets the value of the description property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setDescription(String value) throws PropertyVetoException;
    
    /**
    	Properties as per {@link PropertyBag}
     */
    @ToDo(priority=ToDo.Priority.IMPORTANT, details="Provide PropertyDesc for legal props" )
    @PropertiesDesc(props={})
    @Element
    List<Property> getProperty();

    @DuckTyped
    String getIdentity();

    class Duck {
        public static String getIdentity(ExternalJndiResource resource){
            return resource.getJndiName();
        }
    }
}
