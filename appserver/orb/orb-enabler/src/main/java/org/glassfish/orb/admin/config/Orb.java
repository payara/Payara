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

package org.glassfish.orb.admin.config;

import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.Element;
import org.jvnet.hk2.config.types.Property;
import org.jvnet.hk2.config.types.PropertyBag;

import java.beans.PropertyVetoException;
import java.util.List;

import org.glassfish.api.admin.config.PropertiesDesc;

import org.glassfish.quality.ToDo;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Min;

/**
 * Orb Configuration properties
 */

/* @XmlType(name = "", propOrder = {
    "property"
}) */

@Configured
public interface Orb extends ConfigBeanProxy, PropertyBag {

    /**
     * Gets the value of the useThreadPoolIds property.
     * Specifies a comma-separated list of thread-pool ids.
     *
     * This would refer to the thread-pool-id(s) defined in the thread-pool
     * sub-element of thread-pool-config element in domain.xml. These would be
     * the threadpool(s) used by the ORB. More than one thread-pool-id(s) could
     * be specified by using commas to separate the names
     * e.g. orb-thread-pool-1, orb-thread-pool-2
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    @NotNull
    public String getUseThreadPoolIds();

    /**
     * Sets the value of the useThreadPoolIds property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setUseThreadPoolIds(String value) throws PropertyVetoException;

    /**
     * Gets the value of the messageFragmentSize property.
     *
     * GIOPv1.2 messages larger than this will get fragmented.
     * Minimum value is 128.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="1024")
    @Min(value=128)
    public String getMessageFragmentSize();

    /**
     * Sets the value of the messageFragmentSize property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setMessageFragmentSize(String value) throws PropertyVetoException;

    /**
     * Gets the value of the maxConnections property.
     *
     * Maximum number of incoming connections, on all listeners
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="1024",dataType=Integer.class)
    @Min(value=0)
    public String getMaxConnections();

    /**
     * Sets the value of the maxConnections property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setMaxConnections(String value) throws PropertyVetoException;
    
    /**
    	Properties as per {@link org.jvnet.hk2.config.types.PropertyBag}
     */
    @ToDo(priority=ToDo.Priority.IMPORTANT, details="Provide PropertyDesc for legal props" )
    @PropertiesDesc(props={})
    @Element
    List<Property> getProperty();
}
