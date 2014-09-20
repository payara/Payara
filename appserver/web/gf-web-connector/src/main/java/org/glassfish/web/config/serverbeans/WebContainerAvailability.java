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

package org.glassfish.web.config.serverbeans;

import com.sun.enterprise.config.serverbeans.AvailabilityServiceExtension;
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

/**
 * web-container-availability SE/EE only
 */

/* @XmlType(name = "", propOrder = {
    "property"
}) */

@Configured
public interface WebContainerAvailability extends ConfigBeanProxy,
        PropertyBag, AvailabilityServiceExtension {

    /**
     * Gets the value of the availabilityEnabled property.
     *
     * This boolean flag controls whether availability is enabled for HTTP
     * session persistence. If this is "false", then session persistence is
     * disabled for all web modules in j2ee apps and stand-alone web modules.
     * If it is "true" (and providing that the global availability-enabled in
     * availability-service is also "true", then j2ee apps and stand-alone web
     * modules may be ha enabled. Finer-grained control exists at lower levels.
     * If this attribute is missing, it "inherits" the value of the global
     * availability-enabled under availability-service.  Default is "true".
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="true")
    public String getAvailabilityEnabled();

    /**
     * Sets the value of the availabilityEnabled property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setAvailabilityEnabled(String value) throws PropertyVetoException;

    /**
     * Gets the value of the persistenceType property.
     *
     * Specifies the session persistence mechanism for web applications that
     * have availability enabled. Default is "replicated".
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="replicated")
    public String getPersistenceType();

    /**
     * Sets the value of the persistenceType property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setPersistenceType(String value) throws PropertyVetoException;

    /**
     * Gets the value of the persistenceFrequency property.
     *
     * The persistence frequency used by the session persistence framework,
     * when persistence-type = "ha". Values may be "time-based" or "web-event"
     * If it is missing, then the persistence-type will revert to "memory".
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="web-method")
    public String getPersistenceFrequency();

    /**
     * Sets the value of the persistenceFrequency property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setPersistenceFrequency(String value) throws PropertyVetoException;

    /**
     * Gets the value of the persistenceScope property.
     *
     * The persistence scope used by the session persistence framework, when
     * persistence-type = "ha". Values may be "session", "modified-session",
     * "modified-attribute". If it is missing, then the persistence-type will
     * revert to "memory".
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="session")
    public String getPersistenceScope();

    /**
     * Sets the value of the persistenceScope property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setPersistenceScope(String value) throws PropertyVetoException;

    /**
     * Gets the value of the persistenceStoreHealthCheckEnabled property.
     *
     * Deprecated. This attribute has no effect. If you wish to control
     * enabling/disabling HADB health check, refer to store-healthcheck-enabled
     * attribute in the availability-service element.
     * 
     * @return possible object is
     *         {@link String }
     */
    @Deprecated
    @Attribute (defaultValue="false",dataType=Boolean.class)
    public String getPersistenceStoreHealthCheckEnabled();

    /**
     * Sets the value of the persistenceStoreHealthCheckEnabled property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setPersistenceStoreHealthCheckEnabled(String value) throws PropertyVetoException;

    /**
     * Gets the value of the ssoFailoverEnabled property.
     *
     * Controls whether Single-Sign-On state will be made available for failover
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="false",dataType=Boolean.class)
    public String getSsoFailoverEnabled();

    /**
     * Sets the value of the ssoFailoverEnabled property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setSsoFailoverEnabled(String value) throws PropertyVetoException;

    /**
     * Gets the value of the httpSessionStorePoolName property.
     *
     * This is the jndi-name for the JDBC Connection Pool used by the HTTP
     * Session Persistence Framework. If missing, internal code will default it
     * to value of store-pool-name under availability-service
     * (ultimately "jdbc/hastore").
     * 
     * @return possible object is
     *         {@link String }
     */
    @Deprecated
    @Attribute
    public String getHttpSessionStorePoolName();

    /**
     * Sets the value of the httpSessionStorePoolName property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setHttpSessionStorePoolName(String value) throws PropertyVetoException;

    /**
     * Gets thevalue of disableJreplica property.
     *
     * This is the property used to disable setting the JREPLICA cookie
     *
     * @return returns the string representation of the boolean value
     */
    @Attribute (defaultValue="false",dataType=Boolean.class)
    public String getDisableJreplica();

    /**
     * Sets the disableJreplica property
     * @param value allowed object is {@link String}
     * @throws PropertyVetoException
     */
    public void setDisableJreplica(String value) throws PropertyVetoException;
    
    /**
    	Properties as per {@link PropertyBag}
     */
    @ToDo(priority=ToDo.Priority.IMPORTANT, details="Provide PropertyDesc for legal props" )
    @PropertiesDesc(props={})
    @Element
    List<Property> getProperty();
}
