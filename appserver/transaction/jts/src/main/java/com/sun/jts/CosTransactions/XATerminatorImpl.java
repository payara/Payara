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

package com.sun.jts.CosTransactions;

import javax.transaction.xa.Xid;
import javax.transaction.xa.XAException;
import javax.transaction.xa.XAResource;

import javax.resource.spi.XATerminator;

import org.omg.CosTransactions.Vote;
import org.omg.CosTransactions.HeuristicMixed;

import com.sun.enterprise.transaction.api.TransactionImport;

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

/** 
 * This is used for transaction completion and crash recovery flows.
 *
 * @version 1.0
 * @author  Ram Jeyaraman
 */
public class XATerminatorImpl implements XATerminator {

    private static void check(Xid xid) throws XAException {
        // check if xid is valid
        if (xid == null || xid.getFormatId() == 0 ||
                xid.getBranchQualifier() == null || 
                xid.getGlobalTransactionId() == null) {
            throw new XAException(XAException.XAER_NOTA);
        }    
    }
    
    /**	
     * Commits the global transaction specified by xid.
     *
     * @param xid A global transaction identifier
     *
     * @param onePhase If true, the resource manager should use a one-phase
     * commit protocol to commit the work done on behalf of xid.
     *
     * @exception XAException An error has occurred. Possible XAExceptions
     * are XA_HEURHAZ, XA_HEURCOM, XA_HEURRB, XA_HEURMIX, XAER_RMERR,
     * XAER_RMFAIL, XAER_NOTA, XAER_INVAL, or XAER_PROTO.
     *
     * <P>If the resource manager did not commit the transaction and the
     *  parameter onePhase is set to true, the resource manager may throw
     *  one of the XA_RB* exceptions. Upon return, the resource manager has
     *  rolled back the branch's work and has released all held resources.
     */
    public void commit(Xid xid, boolean onePhase) throws XAException {
        
        check(xid); // check if xid is valid
        
        GlobalTID tid = new GlobalTID(xid);   

        if (onePhase) {
            // Synchronizers invoked by coord.beforeCompletion must be
            // executed with the transaction context of the transaction 
            // that is being committed as specified in 
            // in method javax.Synchronization.beforeCompletion
           try {
                recreate(xid, 0);
            } catch (Throwable e) {
                // failed check for concurrent activity
                XAException xaExc = new XAException(XAException.XAER_PROTO); 
                xaExc.initCause(e);
                throw xaExc;
            }
        } else {
	     // check for concurrrent activity
             if (RecoveryManager.readAndUpdateTxMap(tid) == false) {
                throw new XAException(XAException.XAER_PROTO);
             }
        }
        
        boolean exceptionFlag = false;
        int errorCode = XAException.XAER_PROTO;
        try {
            // First of all make sure it has been recovered if necessary
            RecoveryManager.waitForRecovery();
    
            // Look up the Coordinator for the transaction.
            TopCoordinator coord = (TopCoordinator) 
                                    RecoveryManager.getCoordinator(tid);
        
            if (coord == null) { // error to receive commit more than once
                errorCode = XAException.XAER_PROTO;
                throw new XAException(errorCode);
            }
            
            // If there is a Coordinator, lock it for the duration of this 
            // operation. Tell the Coordinator to commit.
            synchronized (coord) {
                if (onePhase) {
                    coord.beforeCompletion();
                    if (coord.getParticipantCount() == 1) {
                        coord.commitOnePhase();
                    } else {
                        Vote vote;
                        try {
                            vote = coord.prepare();
                        } catch (HeuristicMixed exc) {
                            errorCode = XAException.XA_HEURHAZ;
                            throw new XAException(errorCode);
                        }
                        if (vote == Vote.VoteCommit) {
                            coord.commit();
                        } else if (vote == Vote.VoteRollback) {
                            coord.rollback(true);
                        }                   
                    }    
                } else {
                    coord.commit();
                }
            }        
        } catch (Throwable exc) {
            exceptionFlag = true;
            XAException xaExc = new XAException(errorCode); 
            xaExc.initCause(exc);
            throw xaExc;
        } finally {
            if (onePhase) {
                // Complete full transactional context created for 
		// beforeCompletion calls in prepare phase of one phase commit.
                try {
                    release(xid);
                } catch (Throwable t) {  
                    if (!exceptionFlag) {
                        XAException xaExc = new XAException(XAException.XAER_PROTO);
                        xaExc.initCause(t);
                       throw xaExc;
                    }
                }
            } else {
                Thread thread = RecoveryManager.removeFromTxMap(tid);
                if (thread == null || (thread != Thread.currentThread())) { // error
                    if (!exceptionFlag) { // no exception yet
                        throw new XAException(XAException.XAER_RMERR);
                    }
                }     
            }
        }
    }

    /** 
     * Tells the resource manager to forget about a heuristically
     * completed transaction branch.
     *
     * @param xid A global transaction identifier.
     *
     * @exception XAException An error has occurred. Possible exception
     * values are XAER_RMERR, XAER_RMFAIL, XAER_NOTA, XAER_INVAL, or
     * XAER_PROTO.
     */
    public void forget(Xid xid) throws XAException {}
    
    /** 
     * Ask the resource manager to prepare for a transaction commit
     * of the transaction specified in xid.
     *
     * @param xid A global transaction identifier.
     *
     * @exception XAException An error has occurred. Possible exception
     * values are: XA_RB*, XAER_RMERR, XAER_RMFAIL, XAER_NOTA, XAER_INVAL,
     * or XAER_PROTO.
     *
     * @return A value indicating the resource manager's vote on the
     * outcome of the transaction. The possible values are: XA_RDONLY
     * or XA_OK. These constants are defined in 
     * <code> javax.transaction.xa.XATerminator</code> interface. 
     * If the resource manager wants to roll back the
     * transaction, it should do so by raising an appropriate XAException
     * in the prepare method.
     */
    public int prepare(Xid xid) throws XAException {
        
        check(xid); // check if xid is valid
        
        GlobalTID tid = new GlobalTID(xid);   
        
        try {
            // Synchronizers invoked by coord.beforeCompletion must be
            // executed with the transaction context of the transaction 
            // that is being committed as specified in 
            // in method javax.Synchronization.beforeCompletion
            recreate(xid, 0);
        } catch (Throwable e) {
	    // failed check for concurrent activity for transaction tid.
            XAException xaExc = new XAException(XAException.XAER_PROTO);
            xaExc.initCause(e);
            throw xaExc;
        }

        boolean exceptionFlag = false;
        int errorCode = XAException.XAER_PROTO;
        try {
            // First of all make sure it has been recovered if necessary
            RecoveryManager.waitForRecovery();
    
            // Look up the Coordinator for the transaction.
            TopCoordinator coord = (TopCoordinator) 
                                    RecoveryManager.getCoordinator(tid);
        
            if (coord == null) { // error to receive prepare more than once
                errorCode = XAException.XAER_PROTO;
                throw new XAException(errorCode);
            }
            
            // If there is a Coordinator, lock it for the duration of this 
            // operation. Tell the Coordinator to commit.
            synchronized (coord) {
                coord.beforeCompletion();
                Vote vote = coord.prepare();
                if (vote == Vote.VoteRollback) {
                    errorCode = XAException.XA_RBROLLBACK;
                } else if (vote == Vote.VoteCommit) {
                    return XAResource.XA_OK;
                } else if (vote == Vote.VoteReadOnly) {
                    return XAResource.XA_RDONLY;
                }
                throw new XAException(errorCode);
            }        
        } catch (Throwable exc) {
            exceptionFlag = true;           
            XAException xaExc = new XAException(errorCode); 
            xaExc.initCause(exc);
            throw xaExc;
        } finally {
            try {
                release(xid);
            } catch (Throwable t) {
                if (!exceptionFlag) {
                    errorCode = XAException.XAER_PROTO;
                    XAException xaExc = new XAException(errorCode);
                    xaExc.initCause(t);
                    throw xaExc;
                }
                // else allow original exception to be thrown
            }
        }       
    }

    /** 
     * Obtains a list of prepared transaction branches from a resource
     * manager. The transaction manager calls this method during recovery
     * to obtain the list of transaction branches that are currently in
     * prepared or heuristically completed states.
     *
     * @param flag One of TMSTARTRSCAN, TMENDRSCAN, TMNOFLAGS. TMNOFLAGS
     * must be used when no other flags are set in the parameter. These
     * constants are defined in <code>javax.transaction.xa.XATerminator</code> 
     * interface.
     *
     * @exception XAException An error has occurred. Possible values are
     * XAER_RMERR, XAER_RMFAIL, XAER_INVAL, and XAER_PROTO.
     *
     * @return The resource manager returns zero or more XIDs of the
     * transaction branches that are currently in a prepared or
     * heuristically completed state. If an error occurs during the
     * operation, the resource manager should throw the appropriate
     * XAException.
     */
    public Xid[] recover(int flag) throws XAException {
        
        // wait for recovery to be completed.
        RecoveryManager.waitForResync();
        
        return (Xid[]) TimeoutManager.getInDoubtXids();    
    }

    /** 
     * Informs the resource manager to roll back work done on behalf
     * of a transaction branch.
     *
     * @param xid A global transaction identifier.
     *
     * @exception XAException An error has occurred. Possible XAExceptions are
     * XA_HEURHAZ, XA_HEURCOM, XA_HEURRB, XA_HEURMIX, XAER_RMERR, XAER_RMFAIL,
     * XAER_NOTA, XAER_INVAL, or XAER_PROTO.
     *
     * <p>If the transaction branch is already marked rollback-only the
     * resource manager may throw one of the XA_RB* exceptions. Upon return,
     * the resource manager has rolled back the branch's work and has released
     * all held resources.
     */
    public void rollback(Xid xid) throws XAException {
        
        check(xid); // check if xid is valid
        
        GlobalTID tid = new GlobalTID(xid);   

        // check for concurrent activity
        if (RecoveryManager.readAndUpdateTxMap(tid) == false) {
            throw new XAException(XAException.XAER_PROTO);      
        }
        
        boolean exceptionFlag = false;
        int errorCode = XAException.XAER_PROTO;
        try {
            // First of all make sure it has been recovered if necessary
            RecoveryManager.waitForRecovery();
    
            // Look up the Coordinator for the transaction.
            TopCoordinator coord = (TopCoordinator) 
                                    RecoveryManager.getCoordinator(tid);
        
            if (coord == null) { // error to receive rollback more than once
                errorCode = XAException.XAER_PROTO;
                throw new XAException(errorCode);
            }
            
            // If there is a Coordinator, lock it for the duration of this 
            // operation. Tell the Coordinator to commit.
            synchronized (coord) {
                coord.rollback(true);
            }        
        } catch (Throwable exc) {
            exceptionFlag = true;
            XAException xaExc = new XAException(errorCode); 
            xaExc.initCause(exc);
            throw xaExc; 
        } finally {
            Thread thread = RecoveryManager.removeFromTxMap(tid);
            if (thread == null || (thread != Thread.currentThread())) { // error            
                if (!exceptionFlag) {
                    throw new XAException(XAException.XAER_RMERR);            
                }
            } 
        }              
    }

    static private final TransactionImport tim = getTransactionImportManager();

   // no standardized JNDI name exists across as implementations for TM, this is Sun App Server specific.
    private static final String AS_TXN_MGR_JNDI_NAME = "java:appserver/TransactionManager";
    
    static private Object jndiLookup(final String jndiName) {
        Object result = null;
        try {
            final Context ctx = new InitialContext();
            result = ctx.lookup(jndiName);
        } catch (NamingException e) { }
        return result;
    }
    
    static private TransactionImport getTransactionImportManager() {
        return (TransactionImport)jndiLookup(AS_TXN_MGR_JNDI_NAME);
    }

    static private void recreate(Xid xid, int timeout) {
        if (tim != null) {
            tim.recreate(xid, timeout);
        }
    }

    static private void release(Xid xid) {
        if (tim != null) {
            tim.release(xid);
        }
    }
}


