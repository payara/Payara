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

import org.glassfish.config.support.datatypes.NonNegativeInteger;
import org.glassfish.api.admin.config.ConfigExtension;
import org.glassfish.api.admin.config.PropertiesDesc;
import org.glassfish.api.admin.config.PropertyDesc;

import javax.validation.constraints.Min;

/* @XmlType(name = "", propOrder = {
    "property"
}) */

@Configured
public interface MdbContainer extends ConfigBeanProxy, PropertyBag, ConfigExtension {


    /**
     * Gets the value of the steadyPoolSize property.
     * Minimum and initial number of message driven beans in pool.
     * An integer in the range [0, max-pool-size].
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="0")
    @Min(value=0)
    String getSteadyPoolSize();

    /**
     * Sets the value of the steadyPoolSize property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setSteadyPoolSize(String value) throws PropertyVetoException;

    /**
     * Gets the value of the poolResizeQuantity property.
     *
     * Quantum of increase/decrease, when the size of pool grows/shrinks.
     * An integer in the range [0, max-pool-size].
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="8")
    @Min(value=0)
    String getPoolResizeQuantity();

    /**
     * Sets the value of the poolResizeQuantity property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setPoolResizeQuantity(String value) throws PropertyVetoException;

    /**
     * Gets the value of the maxPoolSize property.
     * maximum size, pool can grow to. A non-negative integer.
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="32")
    @Min(value=0)
    String getMaxPoolSize();

    /**
     * Sets the value of the maxPoolSize property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setMaxPoolSize(String value) throws PropertyVetoException;

    /**
     * Gets the value of the idleTimeoutInSeconds property.
     *
     * Idle bean instance in pool becomes a candidate for deletion, when this
     * timeout expires
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="600")
    @Min(value=0)
    String getIdleTimeoutInSeconds();

    /**
     * Sets the value of the idleTimeoutInSeconds property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setIdleTimeoutInSeconds(String value) throws PropertyVetoException;
    
    
    /**
       Properties.
     */
@PropertiesDesc(
    props={
        @PropertyDesc(name="cmt-max-runtime-exceptions", defaultValue="1", dataType=NonNegativeInteger.class,
            description="Deprecated. Specifies the maximum number of RuntimeException occurrences allowed from a message-driven bean's " +
                "method when container-managed transactions are used")
    }
    )
    @Element
    List<Property> getProperty();
}
