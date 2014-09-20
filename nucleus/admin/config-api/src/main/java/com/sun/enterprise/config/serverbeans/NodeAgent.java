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
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.Element;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.types.Property;
import org.jvnet.hk2.config.types.PropertyBag;

import java.beans.PropertyVetoException;
import java.util.List;

import org.glassfish.api.admin.config.PropertiesDesc;
import org.glassfish.quality.ToDo;

import javax.validation.constraints.NotNull;

/**
 * SE/EE Node Controller. The node agent is an agent that manages server
 * instances on a host machine.
 */

/* @XmlType(name = "", propOrder = {
    "jmxConnector",
    "authRealm",
    "logService",
    "property"
}) */

@Configured
public interface NodeAgent extends ConfigBeanProxy, PropertyBag {

    /**
     * Gets the value of the name property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute(key=true)
    @NotNull
    public String getName();

    /**
     * Sets the value of the name property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setName(String value) throws PropertyVetoException;

    /**
     * Gets the value of the systemJmxConnectorName property.
     *
     * The name of the internal jmx connector
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    public String getSystemJmxConnectorName();

    /**
     * Sets the value of the systemJmxConnectorName property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setSystemJmxConnectorName(String value) throws PropertyVetoException;

    /**
     * Gets the value of the startServersInStartup property.
     *
     * If true, starts all managed server instances when the
     * Node Controller is started.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="true",dataType=Boolean.class)
    public String getStartServersInStartup();

    /**
     * Sets the value of the startServersInStartup property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setStartServersInStartup(String value) throws PropertyVetoException;

    /**
     * Gets the value of the jmxConnector property.
     *
     * @return possible object is
     *         {@link JmxConnector }
     */
    @Element
    public JmxConnector getJmxConnector();

    /**
     * Sets the value of the jmxConnector property.
     *
     * @param value allowed object is
     *              {@link JmxConnector }
     */
    public void setJmxConnector(JmxConnector value) throws PropertyVetoException;

    /**
     * Gets the value of the authRealm property.
     *
     * @return possible object is
     *         {@link AuthRealm }
     */
    @Element
    public AuthRealm getAuthRealm();

    /**
     * Sets the value of the authRealm property.
     *
     * @param value allowed object is
     *              {@link AuthRealm }
     */
    public void setAuthRealm(AuthRealm value) throws PropertyVetoException;

    /**
     * Gets the value of the logService property.
     *
     * @return possible object is
     *         {@link LogService }
     */
    @Element(required=true)
    public LogService getLogService();

    /**
     * Sets the value of the logService property.
     *
     * @param value allowed object is
     *              {@link LogService }
     */
    public void setLogService(LogService value) throws PropertyVetoException;
    
    /**
    	Properties as per {@link org.jvnet.hk2.config.types.PropertyBag}
     */
    @ToDo(priority=ToDo.Priority.IMPORTANT, details="Provide PropertyDesc for legal props" )
    @PropertiesDesc(props={})
    @Element
    List<Property> getProperty();
}
