/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.jvnet.hk2.config.test;

import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.Element;
import org.jvnet.hk2.config.ConfigBeanProxy;
import org.jvnet.hk2.config.Configured;

import java.beans.PropertyVetoException;
import java.util.List;


/**
 * Configuration of EJB Container
 */

/* @XmlType(name = "", propOrder = {
    "ejbTimerService",
    "property"
}) */

@Configured
public interface SimpleConnector extends ConfigBeanProxy {
    public static final int DEFAULT_THREAD_CORE_POOL_SIZE = 16;
    public static final int DEFAULT_THREAD_MAX_POOL_SIZE = 32;
    public static final long DEFAULT_THREAD_KEEP_ALIVE_SECONDS = 60;
    public static final int DEFAULT_THREAD_QUEUE_CAPACITY = Integer.MAX_VALUE;
    public static final boolean DEFAULT_ALLOW_CORE_THREAD_TIMEOUT = false;
    public static final boolean DEFAULT_PRESTART_ALL_CORE_THREADS = false;
 
    /**
     * Gets the value of the steadyPoolSize property.
     *
     * (slsb,eb) number of bean instances normally maintained in pool.
     * When a pool is first created, it will be populated with size equal to
     * steady-pool-size. When an instance is removed from the pool, it is
     * replenished asynchronously, so that the pool size is at or above the
     * steady-pool-size. This addition will be in multiples of
     * pool-resize-quantity. When a bean is disassociated from a method
     * invocation, it is put back in the pool, subject to max-pool-size limit.
     * If the max pool size is exceeded the bean id destroyed immediately.
     * A pool cleaning thread, executes at an interval defined by
     * pool-idle-timeout-in-seconds. This thread reduces the pool size to
     * steady-pool-size, in steps defined by pool-resize-quantity. If the pool
     * is empty, the required object will be created and returned immediately.
     * This prevents threads from blocking till the pool is replenished by the
     * background thread. steady-pool-size must be greater than 1 and at most
     * equal to the max-pool-size.
     * 
     * @return possible object is
     *         {@link String }
     */
    @Attribute (defaultValue="8080")
    String getPort();

    /**
     * Sets the value of the steadyPoolSize property
     *
     * @param value allowed object is
     *              {@link String }
     */
    void setPort(String value) throws PropertyVetoException;
    
    @Element
    public EjbContainerAvailability getEjbContainerAvailability();
    
    void setEjbContainerAvailability(EjbContainerAvailability v);

    @Element
    public WebContainerAvailability getWebContainerAvailability();

    void setWebContainerAvailability(WebContainerAvailability v);

    @Element("*")
    List<GenericContainer> getExtensions();

}



