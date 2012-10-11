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

import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.ConfigBeanProxy;

import java.beans.PropertyVetoException;

import javax.validation.constraints.NotNull;

/**
 * Specifies configuration for a XSLT transformation rule
 */

/* @XmlType(name = "") */

@Configured
public interface TransformationRule extends ConfigBeanProxy  {

    /**
     * Gets the value of the name property.
     *
     * Name of the transformation rule
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
     * Gets the value of the enabled property.
     *
     * If false, this transformation rule is disabled
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="true",dataType=Boolean.class)
    public String getEnabled();

    /**
     * Sets the value of the enabled property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setEnabled(String value) throws PropertyVetoException;

    /**
     * Gets the value of the applyTo property.
     *
     * - "request": transformations are applied to request in the order
     *   in which they are specified.
     * - "response": transformation is applied to response in the order in
         which they are specified.
     * - "both": transformation rule is applied to request and response. The
     *   order is reversed for response.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="request")
    public String getApplyTo();

    /**
     * Sets the value of the applyTo property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setApplyTo(String value) throws PropertyVetoException;

    /**
     * Gets the value of the ruleFileLocation property.
     *
     * Location of rule file to do transformation. Only XSLT files are allowed.
     * Default is:
     * ${com.sun.aas.instanceRoot}/generated/xml/<appOrModule>/<xslt-ilename>/
     * Absolute paths can also be specified
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    @NotNull
    public String getRuleFileLocation();

    /**
     * Sets the value of the ruleFileLocation property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setRuleFileLocation(String value) throws PropertyVetoException;
    
}
