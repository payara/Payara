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

package javax.resource.spi;

import javax.resource.ResourceException;

/**
 * This is a mix-in interface that may be optionally implemented by a 
 * <code>ConnectionManager</code> implementation. An implementation of
 * this interface must support the lazy connection association optimization.
 *
 * @version 1.0
 * @author  Ram Jeyaraman
 */
public interface LazyAssociatableConnectionManager {

    /**
     * This method is called by a resource adapter (that is capable of
     * lazy connection association optimization) in order to lazily associate
     * a connection object with a <code>ManagedConnection</code> instance. 
     *
     * @param connection the connection object that is to be associated.
     *
     * @param mcf The <code>ManagedConnectionFactory</code> instance that was
     * originally used to create the connection object.
     *
     * @param cxReqInfo connection request information. This information must
     * be the same as that used to originally create the connection object.
     *
     * @throws  ResourceException     Generic exception.
     *
     * @throws  ApplicationServerInternalException 
     *                              Application server specific exception.
     *
     * @throws  SecurityException     Security related error.
     *
     * @throws  ResourceAllocationException
     *                              Failed to allocate system resources for
     *                              connection request.
     * @throws  ResourceAdapterInternalException
     *                              Resource adapter related error condition.
     */
    void associateConnection(Object connection, ManagedConnectionFactory mcf,
	ConnectionRequestInfo cxReqInfo) throws ResourceException;

    /**
     * This method is called by the resource adapter (that is capable of
     * lazy connection association optimization) in order to notify the
     * application server that a disassociated connection handle is closed.
     * <p>The application server can then perform any cleanup operations 
     * related to the disassociated connection handle in its connection pool.
     *
     * @param connection the disassociated connection object handle that 
     * has been closed
     *
     * @param mcf The <code>ManagedConnectionFactory</code> instance that was
     * originally used to create the connection object.
     *
     * @since 1.6
     */
    void inactiveConnectionClosed(Object connection, 
                                     ManagedConnectionFactory mcf);
}

