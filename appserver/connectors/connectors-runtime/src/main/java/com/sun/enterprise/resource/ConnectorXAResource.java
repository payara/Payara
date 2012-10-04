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

package com.sun.enterprise.resource;

import com.sun.appserv.connectors.internal.api.PoolingException;
import com.sun.enterprise.connectors.ConnectorRuntime;
import com.sun.enterprise.resource.allocator.ResourceAllocator;
import com.sun.enterprise.resource.listener.ConnectionEventListener;
import com.sun.enterprise.resource.listener.LocalTxConnectionEventListener;
import com.sun.enterprise.resource.pool.PoolManager;
import com.sun.enterprise.transaction.api.JavaEETransaction;
import com.sun.enterprise.transaction.api.JavaEETransactionManager;
import com.sun.enterprise.transaction.api.TransactionConstants;
import com.sun.logging.LogDomains;
import org.glassfish.resourcebase.resources.api.PoolInfo;

import javax.resource.spi.ManagedConnection;
import javax.resource.spi.ResourceAllocationException;
import javax.transaction.SystemException;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;
import javax.transaction.xa.Xid;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Tony Ng, Jagadish Ramu
 *
 */
public class ConnectorXAResource implements XAResource {

    private Object userHandle;
    private ResourceSpec spec;
    private PoolInfo poolInfo;
    private ResourceAllocator alloc;
    private PoolManager poolMgr;
    private ManagedConnection localConnection;
    private ClientSecurityInfo info;
    private ConnectionEventListener listener;
    private ResourceHandle localHandle_;
    private JavaEETransaction associatedTransaction;

    private static Hashtable listenerTable = new Hashtable();


    // Create logger object per Java SDK 1.4 to log messages
    // introduced Santanu De, Sun Microsystems, March 2002

    static Logger _logger = LogDomains.getLogger(ConnectorXAResource.class, LogDomains.RSR_LOGGER);

    public ConnectorXAResource(ResourceHandle handle,
                               ResourceSpec spec,
                               com.sun.enterprise.resource.allocator.ResourceAllocator alloc,
                               ClientSecurityInfo info ) {

        // initially userHandle is associated with mc
        this.poolMgr = ConnectorRuntime.getRuntime().getPoolManager();
        this.userHandle = null;
        this.spec = spec;
	    this.poolInfo = spec.getPoolInfo();
        this.alloc = alloc;
        this.info = info;
        localConnection = (ManagedConnection) handle.getResource();
        localHandle_ = handle;
    }

    public void setUserHandle(Object userHandle) {
        this.userHandle = userHandle;
    }

    private void handleResourceException(Exception ex)
        throws XAException {
        _logger.log(Level.SEVERE,"poolmgr.system_exception",ex);
        XAException xae = new XAException(ex.toString());
        xae.errorCode = XAException.XAER_RMERR;
        throw xae;
    }

    public void commit(Xid xid, boolean onePhase) throws XAException {
        try {
            ResourceHandle handle = getResourceHandle();
            ManagedConnection mc = (ManagedConnection) handle.getResource();
            mc.getLocalTransaction().commit();
        } catch (Exception ex) {
            handleResourceException(ex);
        }finally{
            resetAssociation();
            resetAssociatedTransaction();
        }
    }


    public void start(Xid xid, int flags) throws XAException {
        try {
            ResourceHandle handle = getResourceHandle();
            if (!localHandle_.equals(handle)) {
                ManagedConnection mc = (ManagedConnection) handle.getResource();
                mc.associateConnection(userHandle);
                LocalTxConnectionEventListener l =
                        (com.sun.enterprise.resource.listener.LocalTxConnectionEventListener) handle.getListener();
                if(_logger.isLoggable(Level.FINE)){
                    _logger.log(Level.FINE, "connection_sharing_start",  userHandle);
                }
                l.associateHandle(userHandle, localHandle_);
            } else{
                 associatedTransaction = getCurrentTransaction();
            }
        } catch (Exception ex) {
            handleResourceException(ex);
        }
    }

    public void end(Xid xid, int flags) throws XAException {
        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "connection_sharing_end");
        }
        try {
            ResourceHandle handleInTransaction = getResourceHandle();
            if (!localHandle_.equals(handleInTransaction)) {
                LocalTxConnectionEventListener l = (LocalTxConnectionEventListener) handleInTransaction.getListener();

                ResourceHandle handle = l.removeAssociation(userHandle);
                if (handle != null) { // not needed, just to be sure.
                    ManagedConnection associatedConnection = (ManagedConnection) handle.getResource();
                    associatedConnection.associateConnection(userHandle);
                    if (_logger.isLoggable(Level.FINE)) {
                        _logger.log(Level.FINE, "connection_sharing_reset_association",
                                userHandle);
                    }
                }
            }
        } catch (Exception e) {
            handleResourceException(e);
        }
    }
    
    public void forget(Xid xid) throws XAException {
        if(_logger.isLoggable(Level.FINE)) {
	    _logger.fine("Well, forget is called for xid :"+xid);
        }
        // no-op
    }

    public int getTransactionTimeout() throws XAException {
        return 0;
    }
    
    public boolean isSameRM(XAResource other) throws XAException {
        if (this == other) return true;
        if (other == null) return false;
        if (other instanceof ConnectorXAResource) {
            ConnectorXAResource obj = (ConnectorXAResource) other;
            return (this.spec.equals(obj.spec) &&
                    this.info.equals(obj.info));
        } else {
            return false;
        }
    }        

    public int prepare(Xid xid) throws XAException {
        return TransactionConstants.LAO_PREPARE_OK;
    }
    
    public Xid[] recover(int flag) throws XAException {
        return new Xid[0];
    }
    
    public void rollback(Xid xid) throws XAException {
        try {
	    ResourceHandle handle = getResourceHandle();
	    ManagedConnection mc = (ManagedConnection) handle.getResource();
            mc.getLocalTransaction().rollback();
        } catch (Exception ex) {
            handleResourceException(ex);
        }finally{
            resetAssociation();
            resetAssociatedTransaction();
        }
    }

    public boolean setTransactionTimeout(int seconds) throws XAException {
        return false;
    }

    public static void freeListener(ManagedConnection mc) {
        listenerTable.remove(mc);
    }

    private ResourceHandle getResourceHandle() throws PoolingException {
        try {
            ResourceHandle h = null;
            JavaEETransaction j2eetran = getCurrentTransaction();
            if (j2eetran == null) {      //Only if some thing is wrong with tx manager.
                h = localHandle_;        //Just return the local handle.
            } else {
                h = (ResourceHandle)j2eetran.getNonXAResource();
            //make sure that if local-tx resource is set as 'unshareable', only one resource
            //can be acquired. If the resource in question is not the one in transaction, fail
            if(!localHandle_.isShareable()){
                   if(h != localHandle_){
                        throw new ResourceAllocationException("Cannot use more than one local-tx resource in unshareable scope");
                    }
                }
            }
            if (h.getResourceState().isUnenlisted()) {
                ManagedConnection mc = (ManagedConnection) h.getResource();
                // begin the local transaction if first time
                // this ManagedConnection is used in this JTA transaction
                mc.getLocalTransaction().begin();
            }
            return h;
        } catch (Exception ex) {
            _logger.log(Level.SEVERE, "poolmgr.system_exception", ex);
            throw new PoolingException(ex.toString(), ex);
        }
    }

    private JavaEETransaction getCurrentTransaction() throws SystemException {
         JavaEETransactionManager txMgr = ConnectorRuntime.getRuntime().getTransactionManager();
         return (JavaEETransaction) txMgr.getTransaction();
    }

    private void resetAssociation() throws XAException{
        try {
        ResourceHandle handle = getResourceHandle();

            LocalTxConnectionEventListener l = (LocalTxConnectionEventListener)handle.getListener();
            //Get all associated Handles and reset their ManagedConnection association.
            Map associatedHandles = l.getAssociatedHandles();
            if(associatedHandles != null ){
                Set<Map.Entry> userHandles = associatedHandles.entrySet();
                for(Map.Entry userHandleEntry : userHandles ){
                    ResourceHandle associatedHandle = (ResourceHandle)userHandleEntry.getValue();
                    ManagedConnection associatedConnection = (ManagedConnection)associatedHandle.getResource();
                    associatedConnection.associateConnection(userHandleEntry.getKey());
                    if(_logger.isLoggable(Level.FINE)){
                        _logger.log(Level.FINE, "connection_sharing_reset_association",
                                userHandleEntry.getKey());
                    }
                }
                //all associated handles are mapped back to their actual Managed Connection. Clear the associations.
                associatedHandles.clear();
            }

        } catch (Exception ex) {
            handleResourceException(ex);
        }
    }
   
    public JavaEETransaction getAssociatedTransaction(){
         return associatedTransaction;
    }

    private void resetAssociatedTransaction(){
         associatedTransaction = null;
    }
}
