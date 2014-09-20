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

import org.glassfish.api.admin.config.PropertiesDesc;
import org.jvnet.hk2.config.types.Property;
import org.jvnet.hk2.config.types.PropertyBag;
import org.glassfish.quality.ToDo;

import javax.validation.constraints.NotNull;

/**
 * Defines the standard JACC properties used for setting up the JACC provider.
 * It also allows optional properties which can be used by the provider
 * implementation for its configuration.
 */

/* @XmlType(name = "", propOrder = {
    "property"
}) */

@Configured
public interface JaccProvider extends ConfigBeanProxy, PropertyBag {

    /**
     * Gets the value of the name property.
     *
     * A name for this jacc-provider. Is always "default" for default provider.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute(key=true)
    @NotNull
    String getName();

    /**
     * Sets the value of the name property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setName(String value) throws PropertyVetoException;

    /**
     * Gets the value of the policyProvider property.
     *
     * Corresponds to (and can be overridden by) the system property
     * javax.security.jacc.policy.provider
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    @NotNull
    String getPolicyProvider();

    /**
     * Sets the value of the policyProvider property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setPolicyProvider(String value) throws PropertyVetoException;

    /**
     * Gets the value of the policyConfigurationFactoryProvider property.
     *
     * Corresponds to (and can be overridden by) the system property
     * javax.security.jacc.PolicyConfigurationFactory.provider       
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    String getPolicyConfigurationFactoryProvider();

    /**
     * Sets the value of the policyConfigurationFactoryProvider property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setPolicyConfigurationFactoryProvider(String value) throws PropertyVetoException;
    
    /**
    	Properties as per {@link PropertyBag}
     */
    @ToDo(priority=ToDo.Priority.IMPORTANT, details="Provide PropertyDesc for legal props" )
    @PropertiesDesc(props={})
    @Element
    List<Property> getProperty();
}
