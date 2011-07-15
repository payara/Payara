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

package com.sun.enterprise.resource.allocator;

import com.sun.enterprise.deployment.ConnectorDescriptor;
import com.sun.enterprise.resource.ClientSecurityInfo;
import com.sun.enterprise.resource.ResourceHandle;
import com.sun.enterprise.resource.ResourceSpec;
import com.sun.enterprise.resource.listener.ConnectionEventListener;
import com.sun.enterprise.resource.pool.PoolManager;
import com.sun.appserv.connectors.internal.api.PoolingException;

import javax.resource.ResourceException;
import javax.resource.spi.ConnectionEvent;
import javax.resource.spi.ConnectionRequestInfo;
import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ManagedConnectionFactory;
import javax.security.auth.Subject;
import java.util.logging.Level;

/**
 * @author Tony Ng
 */
public class NoTxConnectorAllocator extends AbstractConnectorAllocator {

    class ConnectionListenerImpl extends ConnectionEventListener {
        private ResourceHandle resource;

        public ConnectionListenerImpl(ResourceHandle resource) {
            this.resource = resource;
        }

        public void connectionClosed(ConnectionEvent evt) {
            poolMgr.putbackResourceToPool(resource, false);
        }

        /**
         * Resource adapters will signal that the connection being closed is bad.
         *
         * @param evt ConnectionEvent
         */
        public void badConnectionClosed(ConnectionEvent evt) {
            ManagedConnection mc = (ManagedConnection) evt.getSource();
            mc.removeConnectionEventListener(this);
            poolMgr.badResourceClosed(resource);
        }

        public void connectionErrorOccurred(ConnectionEvent evt) {
            ManagedConnection mc = (ManagedConnection) evt.getSource();
            mc.removeConnectionEventListener(this);
            poolMgr.putbackResourceToPool(resource, true);
/*
            try {
                mc.destroy();
            } catch (Exception ex) {
                // ignore exception
            }
*/
            //GJCINT
            resource.setConnectionErrorOccurred();
        }

        public void localTransactionStarted(ConnectionEvent evt) {
            throw new IllegalStateException("local transaction not supported");
        }

        public void localTransactionCommitted(ConnectionEvent evt) {
            throw new IllegalStateException("local transaction not supported");
        }

        public void localTransactionRolledback(ConnectionEvent evt) {
            throw new IllegalStateException("local transaction not supported");
        }
    }

    public NoTxConnectorAllocator(PoolManager poolMgr,
                                  ManagedConnectionFactory mcf,
                                  ResourceSpec spec,
                                  Subject subject,
                                  ConnectionRequestInfo reqInfo,
                                  ClientSecurityInfo info,
                                  ConnectorDescriptor desc) {
        super(poolMgr, mcf, spec, subject, reqInfo, info, desc);
    }


    public ResourceHandle createResource()
            throws PoolingException {
        try {
            ManagedConnection mc =
                    mcf.createManagedConnection(subject, reqInfo);
            ResourceHandle resource =
                    createResourceHandle(mc, spec, this, info);
            ConnectionEventListener l =
                    new ConnectionListenerImpl(resource);
            mc.addConnectionEventListener(l);
            return resource;
        } catch (ResourceException ex) {
            Object[] params = new Object[]{spec.getPoolInfo(), ex.toString()};
            _logger.log(Level.WARNING,"poolmgr.create_resource_error",params);
            if(_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE,"Resource Exception while creating resource",ex);
            }

            if (ex.getLinkedException() != null) {
                _logger.log(Level.WARNING,
                        "poolmgr.create_resource_linked_error", ex
                                .getLinkedException().toString());
            }
            throw new PoolingException(ex);
        }
    }

    public void fillInResourceObjects(ResourceHandle resource)
            throws PoolingException {
        try {
            ManagedConnection mc = (ManagedConnection) resource.getResource();
            Object con = mc.getConnection(subject, reqInfo);
            resource.fillInResourceObjects(con, null);
        } catch (ResourceException ex) {
            throw new PoolingException(ex);
        }
    }

    public void destroyResource(ResourceHandle resource)
            throws PoolingException {

        try {
            ManagedConnection mc = (ManagedConnection) resource.getResource();
            mc.destroy();
        } catch (Exception ex) {
            throw new PoolingException(ex);
        }
    }

    public boolean isTransactional() {
        return false;
    }
}
