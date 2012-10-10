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

package com.sun.enterprise.connectors.jms.config;

import com.sun.enterprise.config.serverbeans.AvailabilityServiceExtension;
import org.glassfish.api.admin.config.ConfigExtension;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.Element;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.types.Property;

import java.beans.PropertyVetoException;
import java.util.List;

import org.glassfish.api.admin.config.PropertiesDesc;
import org.jvnet.hk2.config.types.PropertyBag;
import javax.validation.constraints.Pattern;

import org.glassfish.quality.ToDo;

/**
 *
 */

/* @XmlType(name = "", propOrder = {
    "property"
}) */

@Configured
public interface JmsAvailability extends ConfigExtension, PropertyBag, AvailabilityServiceExtension {

    /**
     * Gets the value of the availabilityEnabled property.
     *
     * This boolean flag controls whether the MQ cluster associated with the
     * application server cluster is HA enabled or not. If this attribute is
     * "false", then the MQ cluster pointed to by the jms-service element is
     * considered non-HA (Conventional MQ cluster). JMS Messages are not
     * persisted to a highly availablestore. If this attribute is "true" the
     * MQ cluster pointed to by the jms-service element is a HA (enhanced)
     * cluster and the MQ cluster uses the database pointed to by jdbcurl to save persistent JMS messages and
     * other broker cluster configuration information. Individual applications
     * will not be able to control or override MQ cluster availability levels.
     * They inherit the availability attribute defined in this element.
     * If this attribute is missing, availability is turned off by default
     * [i.e. the MQ cluster associated with the AS cluster would behave as a
     * non-HA cluster]
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="false",dataType=Boolean.class)
    String getAvailabilityEnabled();

    /**
     * Sets the value of the availabilityEnabled property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setAvailabilityEnabled(String value) throws PropertyVetoException;

    /**
      * Gets the value of the Config Store type property
      *
      * This attribute specifies whether to use a master broker or a Shared Database
      * for conventional MQ clusters
      * This is a no-op for Enhanced clusters
      *
      * @return possible object is
      *         {@link String }
      */

    @Attribute (defaultValue="masterbroker")
    @Pattern(regexp="(masterbroker|shareddb)")
    String getConfigStoreType();


    /**
     * Sets the value of the Config Store type property.
     *
     * @param value allowed object is
     *              {@link String }
     */

    void setConfigStoreType(String value);

    /**
      * Gets the value of the Message Store type property
      *
      * This attribute specifies where messages need to be stored by MQ.
      * The options are file based or Database based storage
      * This is only relevent for conventional MQ clusters
      * This is a no-op for enhanced clusters
      *
      * @return possible object is
      *         {@link String }
      */

    @Attribute (defaultValue="file")
    @Pattern(regexp="(file|jdbc)")
    String getMessageStoreType();


    /**
     * Sets the value of the Message store type property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setMessageStoreType(String value);



    /**
     * Gets the value of the DB Vendor property.
     *
     * This is the DB Vendor Name for the DB used by the MQ broker cluster
     * for use in saving persistent JMS messages and other broker
     * cluster configuration information.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    String getDbVendor();

    /**
     * Sets the value of the DB Vendor property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setDbVendor(String value) throws PropertyVetoException;

    /**
         * Gets the value of the DB User Name property.
         *
         * This is the DB user Name for the DB used by the MQ broker cluster
         * for use in saving persistent JMS messages and other broker
         * cluster configuration information.
         *
         * @return possible object is
         *         {@link String }
      */
    @Attribute
    String getDbUsername();

      /**
         * Sets the value of the DB UserName property.
         *
         * @param value allowed object is
         *              {@link String }
       */
    void setDbUsername(String value) throws PropertyVetoException;

    /**
     * Gets the value of the DB Password property.
     *
     * This is the DB Password for the DB used by the MQ broker cluster
     * for use in saving persistent JMS messages and other broker
     * cluster configuration information.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    String getDbPassword();

    /**
     * Sets the value of the DB password property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setDbPassword(String value) throws PropertyVetoException;

    /**
     * Gets the value of the JDBC URL property.
     *
     * This is the JDBC URL used by the MQ broker
     * cluster for use in saving persistent JMS messages and other broker
     * cluster configuration information.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    String getDbUrl();

    /**
     * Sets the value of the JDBC URL property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setDbUrl(String value) throws PropertyVetoException;

    /**
     * Gets the value of the MQ Store pool name property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    String getMqStorePoolName();
    
    /**
     * Sets the value of the MQ store pool name property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setMqStorePoolName(String value) throws PropertyVetoException;    
    /**
    	Properties as per {@link PropertyBag}
     */
    @ToDo(priority=ToDo.Priority.IMPORTANT, details="Provide PropertyDesc for legal props" )
    @PropertiesDesc(props={})
    @Element
    List<Property> getProperty();
}

