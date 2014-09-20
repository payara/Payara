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
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.Element;
import org.jvnet.hk2.config.types.Property;
import org.jvnet.hk2.config.types.PropertyBag;

import java.beans.PropertyVetoException;
import java.util.List;

import org.glassfish.api.admin.config.PropertiesDesc;

import org.glassfish.quality.ToDo;

import javax.validation.constraints.Min;


/**
 * Configuration for ejb timer service
 */

/* @XmlType(name = "", propOrder = {
    "property"
}) */

@Configured
public interface EjbTimerService extends ConfigBeanProxy, PropertyBag {

    /**
     * Gets the value of the minimumDeliveryIntervalInMillis property.
     *
     * It is the minimum number of milliseconds allowed before the next timer
     * expiration for a particular timer can occur. It guards  against extremely
     * small timer increments that can overload the server.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="1000")
    @Min(value=1)
    String getMinimumDeliveryIntervalInMillis();

    /**
     * Sets the value of the minimumDeliveryIntervalInMillis property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setMinimumDeliveryIntervalInMillis(String value) throws PropertyVetoException;

    /**
     * Gets the value of the maxRedeliveries property.
     *
     * It is the maximum number of times the ejb timer service will attempt to
     * redeliver a timer expiration due to exception or rollback.
     * The minimum value is 1, per the ejb specification.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="1")
    @Min(value=1)
    String getMaxRedeliveries();

    /**
     * Sets the value of the maxRedeliveries property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setMaxRedeliveries(String value) throws PropertyVetoException;

    /**
     * Gets the value of the timerDatasource property.
     *
     * overrides cmp-resource (jdbc/__TimerPool) specified in sun-ejb-jar.xml of
     * (__ejb_container_timer_app) of the timer service system application.
     * By default this is set to jdbc/__TimerPool, but can be overridden for the
     * cluster or server instance, if they choose to.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    String getTimerDatasource();

    /**
     * Sets the value of the timerDatasource property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setTimerDatasource(String value) throws PropertyVetoException;

    /**
     * Gets the value of the redeliveryIntervalInternalInMillis property.
     *
     * It is the number of milliseconds the ejb timer service will wait after a
     * failed ejbTimeout delivery before attempting a redelivery.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="5000")
    @Min(value=1)
    String getRedeliveryIntervalInternalInMillis();

    /**
     * Sets the value of the redeliveryIntervalInternalInMillis property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setRedeliveryIntervalInternalInMillis(String value) throws PropertyVetoException;
    
    /**
    	Properties as per {@link PropertyBag}
     */
    @ToDo(priority=ToDo.Priority.IMPORTANT, details="Provide PropertyDesc for legal props" )
    @PropertiesDesc(props={})
    @Element
    List<Property> getProperty();
}
