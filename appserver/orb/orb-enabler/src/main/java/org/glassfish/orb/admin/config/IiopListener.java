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
import org.jvnet.hk2.config.Element;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.ConfigBeanProxy;

import java.beans.PropertyVetoException;
import java.util.List;

import org.glassfish.api.admin.config.PropertiesDesc;
import org.jvnet.hk2.config.types.Property;
import org.jvnet.hk2.config.types.PropertyBag;
import org.glassfish.api.admin.RestRedirects;
import org.glassfish.api.admin.RestRedirect;
import org.glassfish.grizzly.config.dom.Ssl;
import static org.glassfish.config.support.Constants.NAME_REGEX;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.glassfish.quality.ToDo;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.Payload;

/**
 *
 */

/* @XmlType(name = "", propOrder = {
    "ssl",
    "property"
}) */

@Configured
@RestRedirects({
 @RestRedirect(opType = RestRedirect.OpType.POST, commandName = "create-iiop-listener"),
 @RestRedirect(opType = RestRedirect.OpType.DELETE, commandName = "delete-iiop-listener")
})
public interface IiopListener extends ConfigBeanProxy, PropertyBag, Payload {

    final static String PORT_PATTERN = "\\$\\{[\\p{L}\\p{N}_][\\p{L}\\p{N}\\-_./;#]*\\}"
            + "|[1-9]|[1-9][0-9]|[1-9][0-9][0-9]|[1-9][0-9][0-9][0-9]"
            + "|[1-5][0-9][0-9][0-9][0-9]|6[0-4][0-9][0-9][0-9]"
            + "|65[0-4][0-9][0-9]|655[0-2][0-9]|6553[0-5]";


    /**
     * Gets the value of the id property.
     *
     * if false, a configured listener, is disabled
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute(key=true)
    @NotNull
    @Pattern(regexp=NAME_REGEX)
    String getId();

    /**
     * Sets the value of the id property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setId(String value) throws PropertyVetoException;

    /**
     * Gets the value of the address property.
     *
     * ip V6 or V4 address or hostname
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
     * Port number
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="1072")
    @Pattern(regexp=PORT_PATTERN,
            message="{port-pattern}",
            payload=IiopListener.class)
    String getPort();

    /**
     * Sets the value of the port property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setPort(String value) throws PropertyVetoException;

    /**
     * Gets the value of the securityEnabled property.
     *
     * Determines whether the iiop listener runs SSL. You can turn 
     * SSL2 or SSL3 on or off and set ciphers using an ssl element
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="false")
    String getSecurityEnabled();

    /**
     * Sets the value of the securityEnabled property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setSecurityEnabled(String value) throws PropertyVetoException;

    /**
     * Gets the value of the enabled property.
     *
     * if false, a configured listener, is disabled
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="true",dataType=Boolean.class)
    String getEnabled();

    /**
     * Sets the value of the enabled property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setEnabled(String value) throws PropertyVetoException;

    /**
     * Gets the value of the ssl property.
     *
     * Specifies optional SSL configuration. Note that the ssl2 ciphers are not
     * supported for iiop, and therefore must be disabled
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
     * Gets the value of lazyInit property
     *
     * if false, this listener is started during server startup
     *
     * @return true or false
     */
    @Attribute(defaultValue="false", dataType=Boolean.class)
    String getLazyInit();

    /**
     * Sets the value of lazyInit property
     *
     * Specify is this listener should be started as part of server startup or not
     *
     * @param value true if the listener is to be started lazily; false otherwise
     */
    void String(boolean value);
    
    /**
    	Properties as per {@link org.jvnet.hk2.config.types.PropertyBag}
     */
    @ToDo(priority=ToDo.Priority.IMPORTANT, details="Provide PropertyDesc for legal props" )
    @PropertiesDesc(props={})
    @Element
    List<Property> getProperty();
}
