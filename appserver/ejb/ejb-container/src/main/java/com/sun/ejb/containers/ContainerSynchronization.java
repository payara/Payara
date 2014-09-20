/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.ejb.containers;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJBException;
import javax.transaction.Synchronization;
import javax.transaction.SystemException;
import javax.transaction.Transaction;

import org.glassfish.ejb.config.EjbContainerAvailability;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;

import com.sun.logging.LogDomains;


/**
 * This class intercepts Synchronization notifications from
 * the TransactionManager and forwards them to the appropriate container.
 * There is one ContainerSynchronization instance per tx.
 * All bean instances (of all types) participating in the tx must be
 * added by the container to the ContainerSynchronization, so that
 * the beans can be called during before/afterCompletion.
 *
 * This class also provides special methods for PersistenceManager Sync and
 * Timer objects which must be called AFTER the containers during 
 * before/afterCompletion.
 *
 */
final class ContainerSynchronization implements Synchronization
{

    private static final Logger _logger =
        LogDomains.getLogger(ContainerSynchronization.class, LogDomains.EJB_LOGGER);

    private ArrayList beans = new ArrayList();
    private Vector pmSyncs = new Vector();

    private Hashtable timerSyncs = new Hashtable();

    private Transaction tx; // the tx with which this Sync was registered
    private EjbContainerUtil ejbContainerUtilImpl;

    SFSBTxCheckpointCoordinator sfsbTxCoordinator;

    // Note: this must be called only after a Tx is begun.
    ContainerSynchronization(Transaction tx, 
			     EjbContainerUtil ejbContainerUtilImpl)
    {
        this.tx = tx;
        this.ejbContainerUtilImpl = ejbContainerUtilImpl;
    }
    
    Vector  getBeanList(){
        Vector vec = new Vector();
        for (Iterator iter = beans.iterator(); iter.hasNext(); ) {
            vec.add(iter.next());
        }
        return vec;
    }
    
    void addBean(EJBContextImpl bean)
    {
        beans.add(bean);
    }
    
    void removeBean(EJBContextImpl bean)
    {
        beans.remove(bean);
    }
    
    void addPMSynchronization(Synchronization sync)
    {
        pmSyncs.add(sync);
    }

    // Set synchronization object for a particular timer.  
    void addTimerSynchronization(TimerPrimaryKey timerId, Synchronization sync)
    {
        timerSyncs.put(timerId, sync);
    }

    // Might be null if no timer synch object for this timerId in this tx
    Synchronization getTimerSynchronization(TimerPrimaryKey timerId) {
        return (Synchronization) timerSyncs.get(timerId);
    }

    void removeTimerSynchronization(TimerPrimaryKey timerId) {
        timerSyncs.remove(timerId);
    }

    public void beforeCompletion()
    {
        // first call beforeCompletion for each bean instance
        for ( int i=0; i<beans.size(); i++ ) {
            EJBContextImpl context = (EJBContextImpl)beans.get(i);
            BaseContainer container = (BaseContainer)context.getContainer();
            try {
                if( container != null ) {
                    boolean allowTxCompletion = true;
		    if (container.isUndeployed()) {
                        if (context instanceof SessionContextImpl) {
                            allowTxCompletion = ((SessionContextImpl) context).getInLifeCycleCallback();
                        } else {
                            allowTxCompletion = false;
		            _logger.log(Level.WARNING, "Marking Tx for rollback "
        		        + " because container for " + container
        		        + " is undeployed");
                        }
                    }

                    if (!allowTxCompletion) {
			try {
			    tx.setRollbackOnly();
			} catch (SystemException sysEx) {
			    _logger.log(Level.FINE, "Error while trying to "
				+ "mark for rollback", sysEx);
			}
		    } else {
			container.beforeCompletion(context);
		    }
                } else {
                    // Might be null if bean was removed.  Just skip it.
                    _logger.log(Level.FINE, "context with empty container in " +
                                " ContainerSynchronization.beforeCompletion");
                }
            } catch ( RuntimeException ex ) {
                logAndRollbackTransaction(ex);
                throw ex;
            } catch ( Exception ex ) {
                logAndRollbackTransaction(ex);
                // no need to call remaining beforeCompletions
                throw new EJBException("Error during beforeCompletion.", ex);
            }
        }

        // now call beforeCompletion for all pmSyncs
        for ( int i=0; i<pmSyncs.size(); i++ ) {
            Synchronization sync = (Synchronization)pmSyncs.elementAt(i);
            try {
                sync.beforeCompletion();
            } catch ( RuntimeException ex ) {
                logAndRollbackTransaction(ex);
                throw ex;
            } catch ( Exception ex ) {
                logAndRollbackTransaction(ex);
                // no need to call remaining beforeCompletions
                throw new EJBException("Error during beforeCompletion.", ex);
            }
        }
    }

    private void logAndRollbackTransaction(Exception ex)
    {
        // rollback the Tx. The client will get
        // a EJB/RemoteException or a TransactionRolledbackException.
        _logger.log(Level.SEVERE,"ejb.remote_or_txnrollback_exception",ex);
        try {
            tx.setRollbackOnly();
        } catch ( SystemException e ) {
            _logger.log(Level.FINE, "", ex);
        }
    }

    public void afterCompletion(int status)
    {	
        for ( int i=0; i<pmSyncs.size(); i++ ) {
            Synchronization sync = (Synchronization)pmSyncs.elementAt(i);
            try {
                sync.afterCompletion(status);
            } catch ( Exception ex ) {
                _logger.log(Level.SEVERE, "ejb.after_completion_error", ex);
            }
        }

        // call afterCompletion for each bean instance
        for ( int i=0; i<beans.size();i++  ) {
            EJBContextImpl context = (EJBContextImpl)beans.get(i);
            BaseContainer container = (BaseContainer)context.getContainer();
            try {
                if( container != null ) {
                    container.afterCompletion(context, status);
                } else {
                    // Might be null if bean was removed.  Just skip it.
                    _logger.log(Level.FINE, "context with empty container in "
                                +
                                " ContainerSynchronization.afterCompletion");
                }
            } catch ( Exception ex ) {
                _logger.log(Level.SEVERE, "ejb.after_completion_error", ex);
            }
        }

        if (sfsbTxCoordinator != null) {
            sfsbTxCoordinator.doTxCheckpoint();
        }

        for ( Iterator iter = timerSyncs.values().iterator(); 
              iter.hasNext(); ) {
            Synchronization timerSync = (Synchronization) iter.next();
            try {
                timerSync.afterCompletion(status);
            } catch ( Exception ex ) { 
                _logger.log(Level.SEVERE, "ejb.after_completion_error", ex);
            }
        }

        // tell ejbContainerUtilImpl to remove this tx/sync from its table
        ejbContainerUtilImpl.removeContainerSync(tx);
    }

    void registerForTxCheckpoint(SessionContextImpl sessionCtx) {
        //No need to synchronize
        if (sfsbTxCoordinator == null) {
            String sfsbHaPersistenceTypeFromConfig = "jdbc/hastore";
            EjbContainerAvailability ejbContainerAvailability = ejbContainerUtilImpl.getServices().
                    getService(EjbContainerAvailability.class);
            if (ejbContainerAvailability != null) {
                sfsbHaPersistenceTypeFromConfig = ejbContainerAvailability.getSfsbStorePoolName();
            }
            sfsbTxCoordinator = new SFSBTxCheckpointCoordinator(sfsbHaPersistenceTypeFromConfig);
        }

        sfsbTxCoordinator.registerContext(sessionCtx);
    }
}
