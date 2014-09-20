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
import org.jvnet.hk2.config.ConfigBeanProxy;

import java.beans.PropertyVetoException;
import java.io.Serializable;

import javax.validation.constraints.Digits;
import javax.validation.constraints.Min;

/**
 * Each cluster would be configured for a ping based health check mechanism
 */

/* @XmlType(name = "") */

@Configured
public interface HealthChecker extends ConfigBeanProxy {

    /**
     * Gets the value of the url property.
     *
     * URL to ping so as to determine the health state of a listener.
     * This must be a relative URL.
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute
    public String getUrl();

    /**
     * Sets the value of the url property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setUrl(String value) throws PropertyVetoException;

    /**
     * Gets the value of the intervalInSeconds property.
     *
     * Interval, in seconds, between health checks. A value of "0"  means that
     * the health check is disabled. Default is 30 seconds. Must be 0 or greater.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="30")
    @Min(value=0)
    public String getIntervalInSeconds();

    /**
     * Sets the value of the intervalInSeconds property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setIntervalInSeconds(String value) throws PropertyVetoException;

    /**
     * Gets the value of the timeoutInSeconds property.
     *
     * Maximum time, in seconds, that a server must respond to a health check
     * request to be considered healthy. Default is 10 seconds. Must be greater
     * than 0.
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="10")
    @Min(value=1)
    public String getTimeoutInSeconds();

    /**
     * Sets the value of the timeoutInSeconds property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setTimeoutInSeconds(String value) throws PropertyVetoException;
}
