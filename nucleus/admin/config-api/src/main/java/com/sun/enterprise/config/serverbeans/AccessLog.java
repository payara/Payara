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
import org.jvnet.hk2.config.types.PropertyBag;
import java.beans.PropertyVetoException;

import javax.validation.constraints.Min;
import javax.validation.constraints.Max;
import javax.validation.constraints.NotNull;

/**
 * Access log configuration
 */
@Configured
@SuppressWarnings("unused")
public interface AccessLog extends ConfigBeanProxy, PropertyBag {

    /**
     * Gets the value of the format attribute, which specifies the format
     * of the access log.
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="%client.name% %auth-user-name% %datetime% %request% %status% %response.length%")
    String getFormat();

    /**
     * Sets the value of the format attribute.
     *
     * @param value allowed object is
     *              {@link String }
     * @throws PropertyVetoException if a listener vetoes the change
     */
    void setFormat(String value) throws PropertyVetoException;

    /**
     * Gets the value of the rotation-policy attribute.
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="time")
    String getRotationPolicy();

    /**
     * Sets the value of the rotation-policy attribute.
     *
     * @param value allowed object is
     *              {@link String }
     * @throws PropertyVetoException if a listener vetoes the change
     * 
     */
    void setRotationPolicy(String value) throws PropertyVetoException;

    /**
     * Gets the value of the rotation-interval-in-minutes attribute.
     * The time interval in minutes between two successive rotations of the
     * access logs.
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="1440")
    @Min(value=1)
    @Max(value=Integer.MAX_VALUE)
    String getRotationIntervalInMinutes();

    /**
     * Sets the value of the rotation-interval-in-minutes attribute.
     *
     * @param value allowed object is
     *              {@link String }
     * @throws PropertyVetoException if a listener vetoes the change
     */
    void setRotationIntervalInMinutes(String value) throws PropertyVetoException;

    /**
     * Gets the value of the rotation-suffix attribute.
     * The suffix to be added to the access-log name after rotation.
     * Acceptable values include those supported by
     * java.text.SimpleDateFormat and "%YYYY;%MM;%DD;-%hh;h%mm;m%ss;s".
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="yyyy-MM-dd")
    String getRotationSuffix();

    /**
     * Sets the value of the rotation-suffix attribute.
     *
     * @param value allowed object is
     *              {@link String }
     * @throws PropertyVetoException if a listener vetoes the change
     */
    void setRotationSuffix(String value) throws PropertyVetoException;

    /**
     * Gets the value of the rotation-enabled attribute.
     * The flag for enabling the access-log rotation
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="true", dataType=Boolean.class)
    String getRotationEnabled();

    /**
     * Sets the value of the rotation-enabled attribute.
     *
     * @param value allowed object is
     *              {@link String }
     * @throws PropertyVetoException if a listener vetoes the change
     */
    void setRotationEnabled(String value) throws PropertyVetoException;

    /**
     * Size in bytes of the buffer where access log calls are stored. If the value is less than 5120, a warning
     *  message is issued, and the value is set to 5120
     * @return the buffer-size
     */
    @Attribute(defaultValue = "32768")
    String getBufferSizeBytes();

    void setBufferSizeBytes(String value);

    /**
     * Number of seconds before the log is written to the disk. The access log is written when the buffer is
     * full or when the interval expires. If the value is 0, the buffer is always written even if it is not full.
     * This means that each time the server is accessed, the log message is stored directly to the file
     * @return   the write interval in seconds
     */
    @Attribute(defaultValue = "300")
    String getWriteIntervalSeconds();
    
    void setWriteIntervalSeconds(String value);


    /**
     * Gets the maximum number of rotated access log files that are to be
     * kept.
     * 
     * <p>A negative value must be interpreted as no limit.
     * @return the max number of log files
     */
    @NotNull
    @Attribute(defaultValue = "-1", dataType=Integer.class)
    String getMaxHistoryFiles();

    /**
     * Sets the maximum number of rotated access log files that are to be
     * kept.
     *
     * @param value the maximum number of log files
     * @throws PropertyVetoException if a listener vetoes the change
     */
    void setMaxHistoryFiles(String value) throws PropertyVetoException;

}

