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

import java.beans.PropertyVetoException;
import java.util.List;
import javax.validation.constraints.Max;
import org.glassfish.api.admin.config.PropertiesDesc;
import org.glassfish.quality.ToDo;
import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.Element;
import org.jvnet.hk2.config.Configured;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.types.Property;
import org.jvnet.hk2.config.types.PropertyBag;

/**
 *
 */

/* @XmlType(name = "", propOrder = {
    "property"
}) */

@Configured
public interface ManagerProperties extends ConfigBeanProxy, PropertyBag {


    /**
     * Gets the value of the sessionFileName property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    public String getSessionFileName();

    /**
     * Sets the value of the sessionFileName property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setSessionFileName(String value) throws PropertyVetoException;

    /**
     * Gets the value of the reapIntervalInSeconds property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute(defaultValue="60",dataType=Integer.class)
    public String getReapIntervalInSeconds();

    /**
     * Sets the value of the reapIntervalInSeconds property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setReapIntervalInSeconds(String value) throws PropertyVetoException;

    /**
     * Gets the value of the maxSessions property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute(defaultValue="-1")
    @Max(value=Integer.MAX_VALUE)
    public String getMaxSessions();

    /**
     * Sets the value of the maxSessions property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setMaxSessions(String value) throws PropertyVetoException;

    /**
     * Gets the value of the sessionIdGeneratorClassname property.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    public String getSessionIdGeneratorClassname();

    /**
     * Sets the value of the sessionIdGeneratorClassname property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setSessionIdGeneratorClassname(String value) throws PropertyVetoException;
    
    
    /**
    	Properties as per {@link org.jvnet.hk2.config.types.PropertyBag}
     */
    @ToDo(priority=ToDo.Priority.IMPORTANT, details="Provide PropertyDesc for legal props" )
    @PropertiesDesc(props={})
    @Element
    List<Property> getProperty();
}
