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

import com.sun.enterprise.config.serverbeans.customvalidators.ReferenceConstraint;
import com.sun.enterprise.util.LocalStringManagerImpl;

import java.beans.PropertyVetoException;
import java.util.List;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.Payload;

import org.jvnet.hk2.config.types.Property;
import org.jvnet.hk2.config.types.PropertyBag;
import org.glassfish.api.admin.config.Named;
import org.glassfish.grizzly.config.dom.Ssl;
import static org.glassfish.config.support.Constants.NAME_REGEX;
import org.glassfish.api.admin.config.PropertiesDesc;
import org.glassfish.quality.ToDo;
import org.jvnet.hk2.config.*;

/**
 * The jmx-connector element defines the configuration of a JSR 160 compliant
 * remote JMX Connector.                                   
 */

/* @XmlType(name = "", propOrder = {
    "ssl",
    "property"
}) */

@Configured
@ReferenceConstraint(skipDuringCreation=true, payload=JmxConnector.class)
public interface JmxConnector extends ConfigBeanProxy, Named, PropertyBag, Payload {
    final static String PORT_PATTERN = "\\$\\{[\\p{L}\\p{N}_][\\p{L}\\p{N}\\-_./;#]*\\}"
            + "|[1-9]|[1-9][0-9]|[1-9][0-9][0-9]|[1-9][0-9][0-9][0-9]"
            + "|[1-5][0-9][0-9][0-9][0-9]|6[0-4][0-9][0-9][0-9]"
            + "|65[0-4][0-9][0-9]|655[0-2][0-9]|6553[0-5]";
    
    /**
     * Gets the value of the enabled property.
     *
     * Defines if this connector is enabled. For EE this must be enabled
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="true", dataType=Boolean.class)
    String getEnabled();

    /**
     * Sets the value of the enabled property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setEnabled(String value) throws PropertyVetoException;

    /**
     * Gets the value of the protocol property.
     *
     * Defines the protocol that this jmx-connector should support.
     * Supported protocols are defined by Entity rjmx-protocol. 
     * Other protocols can be used by user applications independently.
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="rmi_jrmp")
    String getProtocol();

    /**
     * Sets the value of the protocol property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setProtocol(String value) throws PropertyVetoException;

    /**
     * Gets the value of the address property.
     *
     * Specifies the IP address or host-name.
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    @NotNull
    String getAddress();

    /**
     * Sets the value of the address property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setAddress(String value) throws PropertyVetoException;

    /**
     * Gets the value of the port property.
     *
     * Specifies the port of the jmx-connector-server. Note that jmx-service-url
     * is a function of protocol, port and address as defined by the JSR 160 1.0
     * Specification.
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    @Pattern(regexp=PORT_PATTERN,
             message="{port-pattern}",
             payload=JmxConnector.class)
    String getPort();

    /**
     * Sets the value of the port property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setPort(String value) throws PropertyVetoException;

    /**
     * Gets the value of the acceptAll property.
     *
     * Determines whether the connection can be made on all the network
     * interfaces. A value of false implies that the connections only for this
     * specific address will be selected.
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="false", dataType=Boolean.class)
    String getAcceptAll();

    /**
     * Sets the value of the acceptAll property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setAcceptAll(String value) throws PropertyVetoException;

    /**
     * Gets the value of the auth-realm-name property.
     *
     * The name of the auth-realm in this config element that represents the
     * special administrative realm. All authentication (from administraive GUI
     * and CLI) will be handled by this realm.
     *
     * Note: This is deprecated since GlassFish v3 FCS. Use similarly named attribute on admin-service. This will
     * be used only when the admin-service attribute in missing.
     * @return String representing the name of auth realm
     */
    @Deprecated
    @Attribute
    @NotNull
    @Pattern(regexp=NAME_REGEX)
    @ReferenceConstraint.RemoteKey(message="{resourceref.invalid.auth-realm-name}", type=AuthRealm.class)
    String getAuthRealmName();

    /**
     * Sets the value of the authRealmName property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setAuthRealmName(String value) throws PropertyVetoException;

    /**
     * Gets the value of the securityEnabled property.
     *
     * Decides whether the transport layer security be used in jmx-connector.
     * If true, configure the ssl element
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="true", dataType=Boolean.class)
    String getSecurityEnabled();

    /**
     * Sets the value of the securityEnabled property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setSecurityEnabled(String value) throws PropertyVetoException;

    /**
     * Gets the value of the ssl property.
     *
     * @return possible object is
     *         {@link Ssl }
     */
    @Element
    Ssl getSsl();

    /**
     * Sets the value of the ssl property.
     *
     * @param value allowed object is
     *              {@link Ssl }
     */
    void setSsl(Ssl value) throws PropertyVetoException;
    
    /**
    	Properties as per {@link PropertyBag}
     */
    @ToDo(priority=ToDo.Priority.IMPORTANT, details="Provide PropertyDesc for legal props" )
    @PropertiesDesc(props={})
    @Element
    List<Property> getProperty();
}
