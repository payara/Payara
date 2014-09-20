/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;


/**
 * failure-detection enables its members to periodically monitor other
 * group members to determine their availability in the group.
 * group-discovery is used for discovery of group &
 * its members. failure-detection.verify-failure-timeout-in-millis
 * verifies suspect instances by adding a verification
 * layer to mark a failure suspicion as a confirmed failure.
 *
 * @since glassfish v3.1
 */
@Configured
@SuppressWarnings("unused")
public interface FailureDetection extends ConfigBeanProxy {
    /**
     * Gets the value of the maxMissedHeartbeats property.
     * <p/>
     * Maximum number of attempts to try before GMS confirms that a failure is
     * suspected in the group. Must be a positive integer.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute(defaultValue = "3")
    @Min(value = 1)
    String getMaxMissedHeartbeats();

    /**
     * Sets the value of the maxMissedHeartbeats property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setMaxMissedHeartbeats(String value) throws PropertyVetoException;

    /**
     * Gets the value of the heartbeatFrequencyInMillis property.
     * <p/>
     * Must be a positive integer.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute(defaultValue = "2000")
    @Min(value = 1000)
    @Max(value = 120000)
    String getHeartbeatFrequencyInMillis();

    /**
     * Sets the value of heartbeatFrequencyInMillis property.
     * <p/>
     * @param value allowed is {@link String }
     */
    void setHeartbeatFrequencyInMillis(String value) throws PropertyVetoException;

    /**
     * Sets the value of the verifyFailureWaittimeInMillis property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setVerifyFailureWaittimeInMillis(String value) throws PropertyVetoException;


    /**
     * Gets the value of the verifyFailureWaittimeInMillis property.
     * <p/>
     * After this timeout a suspected failure is marked as verified.
     * Must be a positive integer.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute(defaultValue = "1500")
    @Min(value = 1500)
    @Max(value = 120000)
    String getVerifyFailureWaittimeInMillis();

    /**
     * sets the value of the verifyFailureConnectTimeoutInMillis.
     * <p/>
     * @param value allowed object is {@link String}
     * @since glassfish v3.1
     */
    void setVerifyFailureConnectTimeoutInMillis(String value) throws PropertyVetoException;

    /**
     * Gets the value of the verifyFailureConnectTimeoutInMillis.
     * @since glassfish v3.1
     */
    @Attribute(defaultValue = "10000")
    @Min(value = 3000)
    @Max(value = 120000)
    String getVerifyFailureConnectTimeoutInMillis();
}
