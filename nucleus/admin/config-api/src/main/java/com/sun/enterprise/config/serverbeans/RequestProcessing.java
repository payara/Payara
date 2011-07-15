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


/**
 * Provides attributes to configure the request processing subsystem in the
 * HTTP service
 */

@Configured
@Deprecated
public interface RequestProcessing extends ConfigBeanProxy, Injectable  {

    /**
     * Gets the value of the threadCount property.
     *
     * Max no of request processing threads.
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="128")
    String getThreadCount();

    /**
     * Sets the value of the threadCount property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setThreadCount(String value) throws PropertyVetoException;

    /**
     * Gets the value of the initialThreadCount property.
     *
     * The no of request processing threads when the http service is initialized
     *
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="48")
    String getInitialThreadCount();

    /**
     * Sets the value of the initialThreadCount property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setInitialThreadCount(String value) throws PropertyVetoException;

    /**
     * Gets the value of the threadIncrement property.
     *
     * The increment in the number of request processing threads when the number
     * of requests reaches the number specified by request-threads-init
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="10")
    String getThreadIncrement();

    /**
     * Sets the value of the threadIncrement property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setThreadIncrement(String value) throws PropertyVetoException;

    /**
     * Gets the value of the requestTimeoutInSeconds property.
     *
     * Time after which the request times out
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="30")
    String getRequestTimeoutInSeconds();

    /**
     * Sets the value of the requestTimeoutInSeconds property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setRequestTimeoutInSeconds(String value) throws PropertyVetoException;

    /**
     * Gets the value of the headerBufferLengthInBytes property.
     *
     * The size of the buffer used by the request processing threads for reading
     * the request data
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="4096")
    String getHeaderBufferLengthInBytes();

    /**
     * Sets the value of the headerBufferLengthInBytes property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setHeaderBufferLengthInBytes(String value) throws PropertyVetoException;



}
