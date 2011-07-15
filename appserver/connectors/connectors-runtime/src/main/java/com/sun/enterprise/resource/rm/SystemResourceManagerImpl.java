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

import javax.transaction.Transaction;
import javax.transaction.SystemException;

import com.sun.logging.*;
import com.sun.enterprise.transaction.api.JavaEETransactionManager;
import com.sun.enterprise.connectors.ConnectorRuntime;
import com.sun.appserv.connectors.internal.api.PoolingException;
import com.sun.enterprise.resource.*;

/**
 * SystemResourceManagerImpl manages the resource requests from system
 *
 * @author Binod PG
 */ 
public class SystemResourceManagerImpl implements ResourceManager {


    private static Logger _logger ;
    static {
        _logger = LogDomains.getLogger(SystemResourceManagerImpl.class, LogDomains.RSR_LOGGER);
    }


    /**
     * Returns the transaction component is participating.
     *
     * @return Handle to the <code>Transaction</code> object.
     * @exception <code>PoolingException<code> If exception is thrown
     *         while getting the transaction.
     */
    public Transaction getTransaction() throws PoolingException {
        try {
            return ConnectorRuntime.getRuntime().getTransaction();
        } catch (Exception ex) {
            _logger.log(Level.SEVERE,"poolmgr.unexpected_exception",ex);
            throw new PoolingException(ex.toString(), ex);
        }
    }

    /**
     * Return null for System Resource.
     */
    public Object getComponent() {        
        return null;
    }
    
    /**
     * Register the <code>ResourceHandle</code> in the transaction
     *
     * @param handle	<code>ResourceHandle</code> object
     * @exception <code>PoolingException</code> If there is any error while 
     *        enlisting.
     */    
    public void enlistResource(ResourceHandle handle) throws PoolingException{
        try {
            JavaEETransactionManager tm = ConnectorRuntime.getRuntime().getTransactionManager();
            Transaction tran = tm.getTransaction();
	    if (tran != null) {
                tm.enlistResource(tran, handle);
            }
	} catch (Exception ex) {
            _logger.log(Level.SEVERE,"poolmgr.unexpected_exception",ex);
            throw new PoolingException(ex.toString(), ex);
        }           
    }                

    /**
     * Dont do any thing for System Resource.
     */
    public void registerResource(ResourceHandle handle) 
         throws PoolingException {
    }
    
    public void rollBackTransaction() {
        try {
            JavaEETransactionManager tm = ConnectorRuntime.getRuntime().getTransactionManager();
            Transaction tran = tm.getTransaction();
	    if ( tran != null ) {
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
     * @param h	<code>ResourceHandle</code> object
     * @param xaresFlag flag indicating transaction success. This can
     *        be XAResource.TMSUCCESS or XAResource.TMFAIL     
     * @exception <code>PoolingException</code>
     */       
    public void delistResource(ResourceHandle h, int xaresFlag) {
        try {
        JavaEETransactionManager tm = ConnectorRuntime.getRuntime().getTransactionManager();
            Transaction tran = tm.getTransaction();
	    if (tran != null) {
                tm.delistResource(tran, h, xaresFlag);
            }		
        } catch (SystemException ex) {
            _logger.log(Level.WARNING,"poolmgr.system_exception",ex);
        } catch (IllegalStateException ex) {
            // ignore
        }            
    }   
    
    /**
     * Dont do any thing for System Resource.
     */
    public void unregisterResource(ResourceHandle resource,
                                   int xaresFlag) {        
    }
}
