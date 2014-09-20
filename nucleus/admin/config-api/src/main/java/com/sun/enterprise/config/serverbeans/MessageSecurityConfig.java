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

import java.beans.PropertyVetoException;
import java.io.Serializable;
import java.util.List;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.glassfish.api.admin.RestRedirects;
import org.glassfish.api.admin.RestRedirect;
import static org.glassfish.config.support.Constants.NAME_REGEX;

/**
 * Defines the message layer specific provider configurations of the application
 * server. All of the providers within a message-security-config element must be
 * able to perform authentication processing at the message layer defined by the
 * value of the auth-layer attribute.                                         
 */

/* @XmlType(name = "", propOrder = {
    "providerConfig"
}) */

@Configured
 @RestRedirects({
 @RestRedirect(opType = RestRedirect.OpType.POST, commandName = "create-message-security-provider"),
 @RestRedirect(opType = RestRedirect.OpType.DELETE, commandName = "delete-message-security-provider")
})
public interface MessageSecurityConfig extends ConfigBeanProxy  {

    /**
     * Gets the value of the authLayer property. Values: "SOAP" or "HttpServlet".
     *
     * All of the providers within a message-security-config element must be
     * able to perform authentication processing at the message layer defined
     * by the value of the auth-layer attribute.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute(key=true)
    @NotNull
    public String getAuthLayer();

    /**
     * Sets the value of the authLayer property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setAuthLayer(String value) throws PropertyVetoException;

    /**
     * Gets the value of the defaultProvider property.
     *
     * Used to identify the server provider to be invoked for any application
     * for which a specific server provider has not been bound.
     *
     * When a default provider of a type is not defined for a message layer,
     * the container will only invoke a provider of the type (at the layer)
     * for those applications for which a specific provider has been bound.   
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    @Pattern(regexp=NAME_REGEX)
    public String getDefaultProvider();

    /**
     * Sets the value of the defaultProvider property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setDefaultProvider(String value) throws PropertyVetoException;

    /**
     * Gets the value of the defaultClientProvider property.
     *
     * Used to identify the client provider to be invoked for any application
     * for which a specific client provider has not been bound
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    @Pattern(regexp=NAME_REGEX)
    public String getDefaultClientProvider();

    /**
     * Sets the value of the defaultClientProvider property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setDefaultClientProvider(String value) throws PropertyVetoException;

    /**
     * Gets the value of the providerConfig property.
     * <p/>
     * <p/>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the providerConfig property.
     * <p/>
     * <p/>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getProviderConfig().add(newItem);
     * </pre>
     * <p/>
     * <p/>
     * <p/>
     * Objects of the following type(s) are allowed in the list
     * {@link ProviderConfig }
     */
    @Element(required=true)
    public List<ProviderConfig> getProviderConfig();
}
