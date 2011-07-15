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

import java.util.logging.*;

import javax.transaction.*;

import com.sun.logging.*;
import com.sun.enterprise.transaction.api.JavaEETransactionManager;
import com.sun.enterprise.connectors.ConnectorRuntime;
import com.sun.appserv.connectors.internal.api.PoolingException;
import com.sun.enterprise.resource.*;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.api.invocation.InvocationException;

/**
 * Resource Manager for any resource request from a component.
 *
 * @author Binod PG
 */
public class ResourceManagerImpl implements ResourceManager {
    
    
    static Logger _logger = null;
    static {
        _logger = LogDomains.getLogger(ResourceManagerImpl.class, LogDomains.RSR_LOGGER);
    }
    
    /**
     * Returns the transaction component is participating.
     *
     * @return Handle to the <code>Transaction</code> object.
     * @exception <code>PoolingException<code>
     */
    public Transaction getTransaction() throws PoolingException {
        InvocationManager invmgr = ConnectorRuntime.getRuntime().getInvocationManager();

        ComponentInvocation inv = invmgr.getCurrentInvocation();
        if (inv == null) {
	    try {
                return ConnectorRuntime.getRuntime().getTransaction();
            } catch (Exception ex) {
	        return null;
	    }
        }
        return (Transaction) inv.getTransaction();
    }
    
    /**
     * Returns the component invoking resource request.
     *
     * @return Handle to the component.
     */
    public Object getComponent(){
        
        InvocationManager invmgr = ConnectorRuntime.getRuntime().getInvocationManager();
        ComponentInvocation inv = invmgr.getCurrentInvocation();
        if (inv == null) {
            return null;
        }
        
        return inv.getInstance();
    }

    /**
     * Enlist the <code>ResourceHandle</code> in the transaction
     *
     * @param h	<code>ResourceHandle</code> object
     * @exception <code>PoolingException</code>
     */
    public void enlistResource(ResourceHandle h) throws PoolingException{
        registerResource(h);
    }
    
    /**
     * Register the <code>ResourceHandle</code> in the transaction
     *
     * @param handle	<code>ResourceHandle</code> object
     * @exception <code>PoolingException</code>
     */
    public void registerResource(ResourceHandle handle)
            throws PoolingException {
        try {
            Transaction tran = null;
            JavaEETransactionManager tm = ConnectorRuntime.getRuntime().getTransactionManager();

            // enlist if necessary
            if (handle.isTransactional()) {
                InvocationManager invmgr = ConnectorRuntime.getRuntime().getInvocationManager();
                ComponentInvocation inv = invmgr.getCurrentInvocation();
                              
                if (inv == null) {
                    //throw new InvocationException();
                    
                    //Go to the tm and get the transaction
		    //This is mimicking the current behavior of
		    //the SystemResourceManagerImpl registerResource method
		    //in that, you return the transaction from the TxManager
		    try {
                        tran = tm.getTransaction(); 	
                    } catch( Exception e ) {
		        tran = null;
			_logger.log(Level.INFO, e.getMessage());
		    }
                } else {
                    tran = (Transaction) inv.getTransaction();
                    tm.registerComponentResource(handle);
                }
                
                if (tran != null) {
 	 	    try{            
 	 	        tm.enlistResource(tran, handle);
 	 	    } catch (Exception ex) {
                        if(_logger.isLoggable(Level.FINE)) {
 	 	            _logger.fine("Exception whle trying to enlist resource " + ex.getMessage());
                        }
 	 	        //If transactional, remove the connection handle from the
 	 	        //component's resource list as there has been exception attempting
 	 	        //to enlist the resource
 	 	        if(inv != null) {
                            if(_logger.isLoggable(Level.FINE)) {
 	 	                _logger.fine("Attempting to unregister component resource");
                            }
 	 	            tm.unregisterComponentResource(handle);
 	 	        }
 	 	        throw ex;
 	 	    }
                }
            }
                        
        } catch (Exception ex) {
            _logger.log(Level.SEVERE,"poolmgr.component_register_exception",ex);
            throw new PoolingException(ex.toString(), ex);
        }
    }
   
    //Overridden by the LazyEnlistableResourceManager to be a No-Op
    protected void enlist( JavaEETransactionManager tm, Transaction tran,
        ResourceHandle h ) throws PoolingException {
        try {
            tm.enlistResource( tran, h );
        } catch( Exception e ) {
            PoolingException pe = new PoolingException( e.getMessage() );
            pe.initCause( e );
            throw pe;
        }
    }
    
    /**
     * Get's the component's transaction and marks it for rolling back.
     */
    public void rollBackTransaction() {
        InvocationManager invmgr = ConnectorRuntime.getRuntime().getInvocationManager();

	JavaEETransactionManager tm = ConnectorRuntime.getRuntime().getTransactionManager();
	Transaction tran = null;
        try {
            ComponentInvocation inv = invmgr.getCurrentInvocation();
            if (inv == null) {
                //throw new InvocationException();
                    
               //Go to the tm and get the transaction
               //This is mimicking the current behavior of
               //the SystemResourceManagerImpl registerResource method
               //in that, you return the transaction from the TxManager
	       try {
                   tran = tm.getTransaction(); 	
               } catch( Exception e ) {
	           tran = null;
                   _logger.log(Level.INFO, e.getMessage());
               }

            } else {
                tran = (Transaction) inv.getTransaction();
	    }
            if (tran != null) {
                tran.setRollbackOnly();
            }
        } catch (SystemException ex) {
            _logger.log(Level.WARNING,"poolmgr.system_exception",ex);
        } catch (IllegalStateException ex) {
            // ignore
        }
    }
    
    /**
     * delist the <code>ResourceHandle</code> from the transaction
     *
     * @param resource	<code>ResourceHandle</code> object
     * @param xaresFlag flag indicating transaction success. This can
     *        be XAResource.TMSUCCESS or XAResource.TMFAIL
     * @exception <code>PoolingException</code>
     */
    public void delistResource(ResourceHandle resource, int xaresFlag) {
        unregisterResource(resource,xaresFlag);
    }
    
    /**
     * Unregister the <code>ResourceHandle</code> from the transaction
     *
     * @param resource	<code>ResourceHandle</code> object
     * @param xaresFlag flag indicating transaction success. This can
     *        be XAResource.TMSUCCESS or XAResource.TMFAIL
     * @exception <code>PoolingException</code>
     */
    public void unregisterResource(ResourceHandle resource,
                                   int xaresFlag) {

        JavaEETransactionManager tm = ConnectorRuntime.getRuntime().getTransactionManager();

        Transaction tran = null;

        try {
            // delist with TMSUCCESS if necessary
            if (resource.isTransactional()) {
                InvocationManager invmgr = ConnectorRuntime.getRuntime().getInvocationManager();

                ComponentInvocation inv = invmgr.getCurrentInvocation();
                if (inv == null) {
                    //throw new InvocationException();

                    //Go to the tm and get the transaction
                    //This is mimicking the current behavior of
                    //the SystemResourceManagerImpl registerResource method
                    //in that, you return the transaction from the TxManager
                    try {
                        tran = tm.getTransaction();
                    } catch (Exception e) {
                        tran = null;
                        _logger.log(Level.INFO, e.getMessage());
                    }
                } else {
                    tran = (Transaction) inv.getTransaction();
                    tm.unregisterComponentResource(resource);
                }
                if (tran != null && resource.isEnlisted()) {
                    tm.delistResource(tran, resource, xaresFlag);
                }
            }
        } catch (SystemException ex) {
            _logger.log(Level.WARNING, "poolmgr.system_exception", ex);
        } catch (IllegalStateException ex) {
            // transaction aborted. Do nothing
        } catch (InvocationException ex) {
            // unregisterResource is called outside of component context
            // likely to be container-forced destroy. Do nothing
        }
    }
}
