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

package org.glassfish.webservices.config;

import com.sun.enterprise.config.serverbeans.ApplicationExtension;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.Element;

import java.beans.PropertyVetoException;
import java.util.List;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * This specifies configuration for a web service end point. This web service
 * end point could be JAXRPC or JSR-109 web service. It contains configuration
 * about Monitoring, Transformation rules and Monitoring Log
 */

/* @XmlType(name = "", propOrder = {
    "registryLocation",
    "transformationRule"
}) */

@Configured
public interface WebServiceEndpoint extends ApplicationExtension {

    /**
     * Gets the value of the name property.
     *
     * fully qualified web service name. Format:
     * |ModuleName|#|EndpointName|, if the web service endpoint belongs to an
     * application. (Parent of this element is j2ee-application).
     * |EndpointName|, if the web service endpoint belongs to stand alone
     * ejb-module or web-module (Parent of this element is either ejb-module
     * or web-module).
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute(key=true)
    @NotNull
    public String getName();

    /**
     * Sets the value of the name property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setName(String value) throws PropertyVetoException;

    /**
     * Gets the value of the monitoring property.
     *
     * Monitoring level for this web service.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="OFF")
    public String getMonitoring();

    /**
     * Sets the value of the monitoring property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setMonitoring(String value) throws PropertyVetoException;

    /**
     * Gets the value of the maxHistorySize property.
     *
     * Maximum number of monitoring records stored in history for this end point
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="25")
    @Min(value=1)
    public String getMaxHistorySize();

    /**
     * Sets the value of the maxHistorySize property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setMaxHistorySize(String value) throws PropertyVetoException;

    /**
     * Gets the value of the jbiEnabled property.
     *
     * If true, it enables the visibility of this endoint as a service in JBI.
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="false",dataType=Boolean.class)
    public String getJbiEnabled();

    /**
     * Sets the value of the jbiEnabled property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setJbiEnabled(String value) throws PropertyVetoException;

    /**
     * Gets the value of the registryLocation property.
     * <p/>
     * <p/>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the registryLocation property.
     * <p/>
     * <p/>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getRegistryLocation().add(newItem);
     * </pre>
     * <p/>
     * <p/>
     * <p/>
     * Objects of the following type(s) are allowed in the list
     * {@link RegistryLocation }
     */
    @Element
    public List<RegistryLocation> getRegistryLocation();

    /**
     * Gets the value of the transformationRule property.
     * <p/>
     * <p/>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the transformationRule property.
     * <p/>
     * <p/>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getTransformationRule().add(newItem);
     * </pre>
     * <p/>
     * <p/>
     * <p/>
     * Objects of the following type(s) are allowed in the list
     * {@link TransformationRule }
     */
    @Element
    public List<TransformationRule> getTransformationRule();

}
