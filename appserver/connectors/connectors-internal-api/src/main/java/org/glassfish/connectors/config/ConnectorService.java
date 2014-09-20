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

package org.glassfish.connectors.config;

import org.glassfish.api.admin.config.ConfigExtension;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Element;
import org.jvnet.hk2.config.types.Property;
import org.glassfish.api.admin.config.PropertiesDesc;
import org.jvnet.hk2.config.types.PropertyBag;

import java.beans.PropertyVetoException;
import java.util.List;

import javax.validation.constraints.Min;


/**
 *
 */

/* @XmlType(name = "") */

@Configured
public interface ConnectorService extends ConfigExtension, ConfigBeanProxy, PropertyBag {

    /**
     * Gets the value of the shutdownTimeoutInSeconds property.
         *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="30")
    @Min(value=1)
    public String getShutdownTimeoutInSeconds();

    /**
     * Sets the value of the shutdownTimeoutInSeconds property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setShutdownTimeoutInSeconds(String value) throws PropertyVetoException;

    /**
     * Gets the value of the connector-classloading-policy.<br>
     * Valid values are <i>derived</i> or <i>global</i><br>
     * <i>derived</i> indicates that the resource-adapters are provided according the the
     * references of resource-adapters in application's deployment-descriptors<br>
     * <i>global</i> indicates that all resource-adapters will be visible to all applications.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="derived")
    public String getClassLoadingPolicy();

    /**
     * Sets the value of the connector-classloading-policy.<br>
     * Valid values are <i>derived</i> or <i>global</i><br>
     * <i>derived</i> indicates that the resource-adapters are provided according the the
     * references of resource-adapters in application's deployment-descriptors<br>
     * <i>global</i> indicates that all resource-adapters will be visible to all applications.
     * @param value allowed object is
     *              {@link String }
     */
    public void setClassLoadingPolicy(String value) throws PropertyVetoException;

    /**
     *	Properties as per {@link org.jvnet.hk2.config.types.PropertyBag}
     *
     *  Properties are used to override the ManagedConnectionFactory  javabean
     * configuration settings. When one or more of these properties are
     * specified, they are passed as is using set<Name>(<Value>) methods to the
     * Resource Adapter's ManagedConnectionfactory class (specified in ra.xml).
     *
     */
    @PropertiesDesc(props={})
    @Element
    List<Property> getProperty();

}
