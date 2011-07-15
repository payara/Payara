/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

import org.jvnet.hk2.component.Injectable;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Configured;

import java.beans.PropertyVetoException;



@Configured
@Deprecated

/**
 * HTTP Protocol related settings
 */
public interface HttpProtocol extends ConfigBeanProxy, Injectable  {

    /**
     * Gets the value of the version property.
     *
     * The version of the HTTP protocol used by the HTTP Service
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="HTTP/1.1")
    String getVersion();

    /**
     * Sets the value of the version property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setVersion(String value) throws PropertyVetoException;

    /**
     * Gets the value of the dnsLookupEnabled property.
     *
     * If the DNS name for a particular ip address from which the request
     * originates needs to be looked up.
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute  (defaultValue="false",dataType=Boolean.class)
    String getDnsLookupEnabled();

    /**
     * Sets the value of the dnsLookupEnabled property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setDnsLookupEnabled(String value) throws PropertyVetoException;

    /**
     * Gets the value of the forcedType property.
     *
     * The response type to be forced if the content served cannot be matched by
     * any of the MIME mappings for extensions. Specified as a semi-colon
     * delimited string consisting of content-type, encoding, language, charset
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="text/html; charset=iso-8859-1")
    String getForcedType();

    /**
     * Sets the value of the forcedType property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setForcedType(String value) throws PropertyVetoException;

    /**
     * Gets the value of the defaultType property.
     *
     * Setting the default response-type. Specified as a semi-colon delimited
     * string consisting of content-type, encoding, language, charset
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="text/html; charset=iso-8859-1")
    String getDefaultType();

    /**
     * Sets the value of the defaultType property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setDefaultType(String value) throws PropertyVetoException;

    /**
     * Gets the value of the forcedResponseType property.
     *
     * This attribute is deprecated. Use forced-type instead
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    String getForcedResponseType();

    /**
     * Sets the value of the forcedResponseType property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setForcedResponseType(String value) throws PropertyVetoException;

    /**
     * Gets the value of the defaultResponseType property.
     *
     * This attribute is deprecated. Use default-type instead
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    String getDefaultResponseType();

    /**
     * Sets the value of the defaultResponseType property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setDefaultResponseType(String value) throws PropertyVetoException;

    /**
     * Gets the value of the sslEnabled property.
     *
     * Globally enables SSL across the server
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="true",dataType=Boolean.class)
    String getSslEnabled();

    /**
     * Sets the value of the sslEnabled property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setSslEnabled(String value) throws PropertyVetoException;



}
