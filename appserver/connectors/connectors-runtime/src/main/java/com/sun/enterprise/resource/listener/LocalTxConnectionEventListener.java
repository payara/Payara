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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
 * 
 * Portions Copyright [2018-2021] [Payara Foundation and/or its affiliates]
 */

package com.sun.enterprise.resource.listener;

import static java.util.logging.Level.FINE;

import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Logger;

import jakarta.resource.ResourceException;
import jakarta.resource.spi.ConnectionEvent;
import jakarta.resource.spi.ManagedConnection;

import com.sun.enterprise.connectors.ConnectorRuntime;
import com.sun.enterprise.resource.ResourceHandle;
import com.sun.enterprise.resource.pool.PoolManager;
import com.sun.logging.LogDomains;

/**
 * @author Binod P.G
 */
public class LocalTxConnectionEventListener extends ConnectionEventListener {

    private static final Logger LOGGER = LogDomains.getLogger(LocalTxConnectionEventListener.class,
            LogDomains.RSR_LOGGER);

    private final PoolManager poolMgr;

    // connectionHandle -> ResourceHandle
    // Whenever a connection is associated with a ManagedConnection,
    // that connection and the resourcehandle associated with its
    // original ManagedConnection will be put in this table.
    private final Map<Object, ResourceHandle> associatedHandles;

    private final ResourceHandle resource;

    public LocalTxConnectionEventListener(ResourceHandle resource) {
        this.resource = resource;
        this.associatedHandles = new IdentityHashMap<>(10);
        this.poolMgr = ConnectorRuntime.getRuntime().getPoolManager();
    }

    @Override
    public void connectionClosed(ConnectionEvent evt) {
        // Get the resource handle from the event
        final Object connectionHandle = evt.getConnectionHandle();

        synchronized (this) {
            ResourceHandle handle = getResourceHandle(connectionHandle);

            if (handle != null) {
                // close the resource
                poolMgr.resourceClosed(handle);
            }
        }
    }

    @Override
    public synchronized void connectionErrorOccurred(ConnectionEvent evt) {
        resource.setConnectionErrorOccurred();
        ManagedConnection mc = (ManagedConnection) evt.getSource();
        mc.removeConnectionEventListener(this);
        poolMgr.resourceErrorOccurred(resource);
    }

    /**
     * Resource adapters will signal that the connection being closed is bad.
     * 
     * @param evt ConnectionEvent
     */
    @Override
    public void badConnectionClosed(ConnectionEvent evt) {
        Object connectionHandle = evt.getConnectionHandle();

        synchronized (this) {
            ResourceHandle handle = getResourceHandle(connectionHandle);

            if (handle != null) {
                poolMgr.badResourceClosed(handle);

                ManagedConnection mc = (ManagedConnection) evt.getSource();
                mc.removeConnectionEventListener(this);
            }
        }
    }

    @Override
    public void localTransactionStarted(ConnectionEvent evt) {
        // no-op
    }

    @Override
    public void localTransactionCommitted(ConnectionEvent evt) {
        // no-op
    }

    @Override
    public void localTransactionRolledback(ConnectionEvent evt) {
        // no-op
    }

    public synchronized void associateHandle(Object c, ResourceHandle h) {
        associatedHandles.put(c, h);
    }

    public synchronized ResourceHandle removeAssociation(Object c) {
        return associatedHandles.remove(c);
    }

    public synchronized void resetAssociations() throws ResourceException {
        for (Entry<Object, ResourceHandle> userHandleEntry : associatedHandles.entrySet()) {
            Object connectionHandle = userHandleEntry.getKey();
            ResourceHandle associatedHandle = userHandleEntry.getValue();
            ManagedConnection associatedConnection = (ManagedConnection) associatedHandle.getResource();
            associatedConnection.associateConnection(connectionHandle);
            LOGGER.log(FINE, "connection_sharing_reset_association", connectionHandle);
        }
        // all associated handles are mapped back to their actual Managed Connection.
        // Clear the associations.
        associatedHandles.clear();
    }

    private ResourceHandle getResourceHandle(Object connectionHandle) {
        ResourceHandle handle = associatedHandles.get(connectionHandle);
        if (handle != null) {
            return handle;
        }
        return resource;
    }

}
