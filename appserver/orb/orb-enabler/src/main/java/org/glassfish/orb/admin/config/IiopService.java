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

import com.sun.enterprise.config.serverbeans.SslClientConfig;
import org.glassfish.api.admin.config.ConfigExtension;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.Element;
import org.jvnet.hk2.config.ConfigBeanProxy;

import java.beans.PropertyVetoException;
import java.util.List;


/* @XmlType(name = "", propOrder = {
    "orb",
    "sslClientConfig",
    "iiopListener"
}) */

@Configured
//@CustomConfiguration(baseConfigurationFileName = "iiop-module-conf.xml")
//@HasCustomizationTokens
public interface IiopService extends ConfigBeanProxy, ConfigExtension   {


    /**
     * Gets the value of the clientAuthenticationRequired property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="false",dataType=Boolean.class)
    public String getClientAuthenticationRequired();

    /**
     * Sets the value of the clientAuthenticationRequired property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setClientAuthenticationRequired(String value) throws PropertyVetoException;

    /**
     * Gets the value of the orb property.
     *
     * @return possible object is
     *         {@link Orb }
     */
    @Element(required=true)
    public Orb getOrb();

    /**
     * Sets the value of the orb property.
     *
     * @param value allowed object is
     *              {@link Orb }
     */
    public void setOrb(Orb value) throws PropertyVetoException;

    /**
     * Gets the value of the sslClientConfig property.
     *
     * @return possible object is
     *         {@link SslClientConfig }
     */
    @Element
    public SslClientConfig getSslClientConfig();

    /**
     * Sets the value of the sslClientConfig property.
     *
     * @param value allowed object is
     *              {@link SslClientConfig }
     */
    public void setSslClientConfig(SslClientConfig value) throws PropertyVetoException;

    /**
     * Gets the value of the iiopListener property.
     * <p/>
     * <p/>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the iiopListener property.
     * <p/>
     * <p/>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getIiopListener().add(newItem);
     * </pre>
     * <p/>
     * <p/>
     * <p/>
     * Objects of the following type(s) are allowed in the list
     * {@link IiopListener }
     */
    @Element
    public List<IiopListener> getIiopListener();



}
