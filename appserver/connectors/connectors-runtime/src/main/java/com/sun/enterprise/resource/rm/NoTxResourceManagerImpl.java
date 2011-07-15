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

package com.sun.enterprise.resource.rm;

import com.sun.enterprise.resource.ResourceHandle;
import com.sun.appserv.connectors.internal.api.PoolingException;
import com.sun.logging.LogDomains;

import javax.transaction.Transaction;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Resource Manager for a resource request from a component
 * that is not to be associated with a transaction.
 *
 * @author Aditya Gore
 */
public class NoTxResourceManagerImpl implements ResourceManager {


    private static Logger _logger;

    static {
        _logger = LogDomains.getLogger(NoTxResourceManagerImpl.class, LogDomains.RSR_LOGGER);
    }

    /**
     * Returns null since this connection is outside any tx context
     *
     * @throws PoolingException when unable to get current transaction
     */
    public Transaction getTransaction() throws PoolingException {
        return null;
    }

    /**
     * Returns the component invoking resource request.
     *
     * @return Handle to the component
     */
    public Object getComponent() {
        return null;
    }

    /**
     * Enlist the <code>ResourceHandle</code> in the transaction
     * This implementation of the method is expected to be a no-op
     *
     * @param h <code>ResourceHandle</code> object
     * @throws PoolingException when unable to enlist resource
     */
    public void enlistResource(ResourceHandle h) throws PoolingException {
        logFine("NoTxResourceManagerImpl :: enlistResource called");
    }

    /**
     * Register the <code>ResourceHandle</code> in the transaction
     * This implementation of the method is expected to be a no-op
     *
     * @param handle <code>ResourceHandle</code> object
     * @throws PoolingException when unable to register resource
     */
    public void registerResource(ResourceHandle handle)
            throws PoolingException {
        logFine("NoTxResourceManagerImpl :: registerResource called");
    }

    /**
     * Get's the component's transaction and marks it for rolling back.
     * This implementation of the method is expected to be a no-op
     */
    public void rollBackTransaction() {
        logFine("rollBackTransaction called in NoTxResourceManagerImpl");
    }

    /**
     * delist the <code>ResourceHandle</code> from the transaction
     * This implementation of the method is expected to be a no-op
     *
     * @param resource  <code>ResourceHandle</code> object
     * @param xaresFlag flag indicating transaction success. This can
     *                  be XAResource.TMSUCCESS or XAResource.TMFAIL
     */
    public void delistResource(ResourceHandle resource, int xaresFlag) {
        logFine("NoTxResourceManagerImpl :: delistResource called");
    }

    /**
     * Unregister the <code>ResourceHandle</code> from the transaction
     * This implementation of the method is expected to be a no-op
     *
     * @param resource  <code>ResourceHandle</code> object
     * @param xaresFlag flag indicating transaction success. This can
     *                  be XAResource.TMSUCCESS or XAResource.TMFAIL
     */
    public void unregisterResource(ResourceHandle resource, int xaresFlag) {
        logFine("NoTxResourceManagerImpl :: unregisterResource called");
    }

    public void logFine(String message) {
        if (_logger.isLoggable(Level.FINE)) {
            _logger.fine(message);
        }
    }
}
