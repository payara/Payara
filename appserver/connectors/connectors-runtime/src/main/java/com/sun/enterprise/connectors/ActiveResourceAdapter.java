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

package com.sun.enterprise.connectors;

import com.sun.appserv.connectors.internal.api.ConnectorRuntimeException;
import com.sun.enterprise.deployment.ConnectorDescriptor;

import javax.resource.spi.ManagedConnectionFactory;
import javax.resource.spi.ResourceAdapter;

import org.jvnet.hk2.annotations.Contract;


/**
 * Interface class for different types (1.0 and 1.5 complient) resource
 * adapter abstraction classes.
 * Contains methods for setup(initialization), destroy and creation of MCF.
 *
 * @author Srikanth P and Binod PG
 */

@Contract
public interface ActiveResourceAdapter {

    /**
     * initializes the active (runtime) RAR
     * @param ra resource-adapter bean
     * @param cd connector-descriptor
     * @param moduleName rar-name
     * @param loader classloader for the RAR
     * @throws ConnectorRuntimeException when unable to initialize the runtime RA
     */
    public void init(ResourceAdapter ra, ConnectorDescriptor cd, String moduleName, ClassLoader loader)
            throws ConnectorRuntimeException;

    /**
     * initializes the resource adapter bean and the resources, pools
     *
     * @throws ConnectorRuntimeException This exception is thrown if the
     *                                   setup/initialization fails.
     */
    public void setup() throws ConnectorRuntimeException;

    /**
     * uninitializes the resource adapter.
     */
    public void destroy();

    /**
     * Returns the Connector descriptor which represents/holds ra.xml
     *
     * @return ConnectorDescriptor Representation of ra.xml.
     */
    public ConnectorDescriptor getDescriptor();

    /**
     * Indicates whether a particular implementation of ActiveRA can handle the RAR in question.
     * @param desc ConnectorDescriptor
     * @param moduleName resource adapter name
     * @return boolean indiating whether a ActiveRA can handle the RAR
     */
    public boolean handles(ConnectorDescriptor desc, String moduleName);

    /**
     * Creates managed Connection factories corresponding to one pool.
     * This should be implemented in the ActiveJmsResourceAdapter, for
     * jms resources, has been implemented to perform xa resource recovery
     * in mq clusters, not supported for any other code path.
     *
     * @param ccp Connector connection pool which contains the pool properties
     *            and ra.xml values pertaining to managed connection factory
     *            class. These values are used in MCF creation.
     * @param loader Classloader used to managed connection factory class.
     * @return ManagedConnectionFactory created managed connection factories
     */
    public ManagedConnectionFactory[] createManagedConnectionFactories
            (ConnectorConnectionPool ccp, ClassLoader loader);

    /**
     * Creates managed Connection factory instance.
     *
     * @param ccp    Connector connection pool which contains the pool properties
     *               and ra.xml values pertaining to managed connection factory
     *               class. These values are used in MCF creation.
     * @param loader Classloader used to managed connection factory class.
     * @return ManagedConnectionFactory created managed connection factory
     *         instance
     */
    public ManagedConnectionFactory createManagedConnectionFactory
            (ConnectorConnectionPool ccp, ClassLoader loader);

    /**
     * Returns the class loader that is used to load the RAR.
     *
     * @return <code>ClassLoader</code> object.
     */
    public ClassLoader getClassLoader();

    /**
     * Returns the module Name of the RAR
     *
     * @return A <code>String</code> representing the name of the
     *         connector module
     */
    public String getModuleName();

    /**
     * returns the resource-adapter bean
     * @return resource-adapter bean
     */
    ResourceAdapter getResourceAdapter();
}
