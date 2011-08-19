/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

import javax.transaction.xa.XAResource;
import javax.transaction.Status;
import javax.transaction.SystemException;
import javax.resource.spi.*;
import javax.resource.ResourceException;
import javax.security.auth.Subject;
import java.util.logging.*;

import com.sun.enterprise.deployment.ConnectorDescriptor;
import com.sun.enterprise.resource.pool.PoolManager;
import com.sun.enterprise.transaction.api.JavaEETransaction;
import com.sun.enterprise.resource.*;
import com.sun.enterprise.resource.listener.ConnectionEventListener;
import com.sun.enterprise.resource.listener.LocalTxConnectionEventListener;
import com.sun.appserv.connectors.internal.api.PoolingException;

/**
 * @author Tony Ng
 */
public class LocalTxConnectorAllocator extends AbstractConnectorAllocator {

    protected boolean shareable = true;
    public LocalTxConnectorAllocator(PoolManager poolMgr,
                                     ManagedConnectionFactory mcf,
                                     ResourceSpec spec,
                                     Subject subject,
                                     ConnectionRequestInfo reqInfo,
                                     ClientSecurityInfo info,
                                     ConnectorDescriptor desc, boolean shareable) {
        super(poolMgr, mcf, spec, subject, reqInfo, info, desc);
        this.shareable = shareable;
    }

    private static String transactionCompletionMode;
    private static final String COMMIT = "COMMIT";
    private static final String ROLLBACK = "ROLLBACK";

    static{
        transactionCompletionMode = System.getProperty("com.sun.enterprise.in-progress-local-transaction.completion-mode");
    }

    public ResourceHandle createResource()
            throws PoolingException {
        try {
            ManagedConnection mc = mcf.createManagedConnection(subject, reqInfo);

            ResourceHandle resource = createResourceHandle(mc, spec, this, info);
            ConnectionEventListener l = new LocalTxConnectionEventListener(resource);
            mc.addConnectionEventListener(l);
            resource.setListener(l);

            XAResource xares = new ConnectorXAResource(resource, spec, this, info);
            resource.fillInResourceObjects(null, xares);

            return resource;
        } catch (ResourceException ex) {
            Object[] params = new Object[]{spec.getPoolInfo(), ex.toString()};
            _logger.log(Level.WARNING, "poolmgr.create_resource_error", params);
            if(_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "Resource Exception while creating resource", ex);
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

            ConnectorXAResource xares = (ConnectorXAResource) resource.getXAResource();
            xares.setUserHandle(con);
            resource.fillInResourceObjects(con, xares);
        } catch (ResourceException ex) {
            throw new PoolingException(ex);
        }
    }

    public void destroyResource(ResourceHandle resource)
            throws PoolingException {
        try {
            ManagedConnection mc = (ManagedConnection) resource.getResource();
            ConnectorXAResource.freeListener(mc);
            XAResource xares = resource.getXAResource();
            forceTransactionCompletion(xares);
            mc.destroy();
            if (_logger.isLoggable(Level.FINEST)) {
                _logger.finest("destroyResource for LocalTxConnectorAllocator done");
            }

        } catch (Exception ex) {
            throw new PoolingException(ex);
        }
    }

    private void forceTransactionCompletion(XAResource xares) throws SystemException {
        if(transactionCompletionMode != null){
            if(xares instanceof ConnectorXAResource){
                ConnectorXAResource connectorXARes = (ConnectorXAResource)xares;
                JavaEETransaction j2eetran = connectorXARes.getAssociatedTransaction();
                if(j2eetran != null && j2eetran.isLocalTx()){
                    if(j2eetran.getStatus() == (Status.STATUS_ACTIVE)){
                        try{
                            if(transactionCompletionMode.equalsIgnoreCase(COMMIT)){
                                if(_logger.isLoggable(Level.FINEST)){
                                    _logger.log(Level.FINEST,"Transaction Completion Mode for LocalTx resource is " +
                                            "set as COMMIT, committing transaction");
                                }
                                j2eetran.commit();
                            }else if(transactionCompletionMode.equalsIgnoreCase(ROLLBACK)){
                                if(_logger.isLoggable(Level.FINEST)){
                                    _logger.log(Level.FINEST,"Transaction Completion Mode for LocalTx resource is " +
                                        "set as ROLLBACK, rolling back transaction");
                                }
                                j2eetran.rollback();
                            }else{
                                _logger.log(Level.WARNING,"Unknown transaction completion mode, no action made");
                            }
                        }catch(Exception e){
                            _logger.log(Level.WARNING, "Failure while forcibily completing an incomplete, " +
                                    "local transaction ", e);
                        }
                    }
                }
            }
        }
    }

    public boolean shareableWithinComponent() {
        return shareable;
    }

}
