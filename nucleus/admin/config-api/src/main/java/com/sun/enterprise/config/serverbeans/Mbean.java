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

package com.sun.enterprise.config.serverbeans;

import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.Element;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.ConfigBeanProxy;

import java.beans.PropertyVetoException;
import java.util.List;

import org.glassfish.api.admin.config.*;
import org.jvnet.hk2.config.types.Property;
import org.jvnet.hk2.config.types.PropertyBag;
import org.glassfish.quality.ToDo;

import javax.validation.constraints.NotNull;

/**
 * Note on the Name of the MBean :
 * It is a String that represents the name of the MBean. It is required that the
 * name is valid to represent a "value" of a property in the property-list of
 * MBean ObjectName. The name must be specified and is a primary key for an
 * MBean. An invalid name implies failure of operation.
 */

/* @XmlType(name = "", propOrder = {
    "description",
    "property"
}) */

@Configured
public interface Mbean extends ConfigBeanProxy, Named, PropertyBag {

    /**
     * Gets the value of the objectType property.
     * A String representing whether it is a user-defined MBean or System MBean.
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="user")
    public String getObjectType();

    /**
     * Sets the value of the objectType property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setObjectType(String value) throws PropertyVetoException;

    /**
     * Gets the value of the implClassName property.
     * A String that represents fully qualified class name of
     * MBean implementation. This is read-only.
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    @NotNull
    public String getImplClassName();

    /**
     * Sets the value of the implClassName property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setImplClassName(String value) throws PropertyVetoException;

    /**
     * Gets the value of the objectName property.
     *
     * A String that represents a system-generated Object Name for this MBean.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    public String getObjectName();

    /**
     * Sets the value of the objectName property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setObjectName(String value) throws PropertyVetoException;

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
    	Properties as per {@link org.jvnet.hk2.config.types.PropertyBag}
     */
    @ToDo(priority=ToDo.Priority.IMPORTANT, details="Provide PropertyDesc for legal props" )
    @PropertiesDesc(props={})
    @Element
    List<Property> getProperty();
}
