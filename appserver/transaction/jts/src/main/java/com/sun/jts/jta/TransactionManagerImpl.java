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

package com.sun.jts.jta;

import java.util.*;
import javax.transaction.*;
import java.io.File;
import org.omg.CosTransactions.*;
import org.omg.CORBA.*;
import org.omg.CORBA.ORBPackage.InvalidName;

import com.sun.jts.CosTransactions.*;
import com.sun.jts.codegen.otsidl.*;

import javax.transaction.SystemException;
import javax.transaction.Synchronization;
import org.omg.CosTransactions.Status;
import org.omg.CosTransactions.Current;
import org.omg.CosTransactions.NoTransaction;
import org.omg.CosTransactions.HeuristicMixed;
import org.omg.CosTransactions.HeuristicHazard;
import com.sun.jts.CosTransactions.GlobalTID;

import javax.transaction.xa.Xid;
import javax.resource.spi.work.WorkException;
import javax.resource.spi.work.WorkCompletedException;

import javax.transaction.xa.XAException;

import java.util.logging.Logger;
import java.util.logging.Level;
import com.sun.logging.LogDomains;
import com.sun.jts.utils.LogFormatter;
import org.glassfish.internal.api.Globals;

/**
 * An implementation of javax.transaction.TransactionManager using JTA.
 *
 * This is a singleton object
 *
 * @author Tony Ng
 */
public class TransactionManagerImpl implements TransactionManager {

    /**
     * the singleton object
     */
    static private TransactionManagerImpl tm = null;

    /**
     * store the current psuedo object
     */
    private Current current;

    /**
     * a mapping of GlobalTID -> TransactionState
     */
    // private Hashtable transactionStates;

    /**
     * mapping between CosTransaction status -> JTA status
     */
    static private HashMap statusMap;
    static private int[] directLookup;
    static final int maxStatus;

	/*
		Logger to log transaction messages
	*/  
    static Logger _logger = LogDomains.getLogger(TransactionManagerImpl.class, LogDomains.TRANSACTION_LOGGER);
	
  	//START IASRI 4706150 

	/**
	* store XAResource Timeout 
	*/
	static private int xaTimeOut = 0;
  	//END IASRI 4706150 

	static private Status CosTransactionStatus[] =
    {
        org.omg.CosTransactions.Status.StatusActive,
        org.omg.CosTransactions.Status.StatusMarkedRollback,
        org.omg.CosTransactions.Status.StatusPrepared,
        org.omg.CosTransactions.Status.StatusCommitted,
        org.omg.CosTransactions.Status.StatusRolledBack,
        org.omg.CosTransactions.Status.StatusUnknown,
        org.omg.CosTransactions.Status.StatusNoTransaction,
        org.omg.CosTransactions.Status.StatusPreparing,
        org.omg.CosTransactions.Status.StatusCommitting,
        org.omg.CosTransactions.Status.StatusRollingBack
    };

    static private int JTAStatus[] =
    {
        javax.transaction.Status.STATUS_ACTIVE,
        javax.transaction.Status.STATUS_MARKED_ROLLBACK,
        javax.transaction.Status.STATUS_PREPARED,
        javax.transaction.Status.STATUS_COMMITTED,
        javax.transaction.Status.STATUS_ROLLEDBACK,
        javax.transaction.Status.STATUS_UNKNOWN,
        javax.transaction.Status.STATUS_NO_TRANSACTION,
        javax.transaction.Status.STATUS_PREPARING,
        javax.transaction.Status.STATUS_COMMITTING,
        javax.transaction.Status.STATUS_ROLLING_BACK
    };

    // static block to initialize statusMap
    static {
        statusMap = new HashMap();
        int calcMaxStatus = 0;
        for (int i=0; i<CosTransactionStatus.length; i++) {
            statusMap.put(CosTransactionStatus[i], JTAStatus[i]);
            calcMaxStatus = Math.max(calcMaxStatus, CosTransactionStatus[i].value());
        }
        maxStatus = calcMaxStatus;
        directLookup = new int[maxStatus + 1];
        for (int i=0; i < directLookup.length; i++) {
            // initialize so that any unused slots point to 'unkown'.
            directLookup[i] = javax.transaction.Status.STATUS_UNKNOWN;
        }
        for (int i=0; i < CosTransactionStatus.length; i++) {
            int statusVal = CosTransactionStatus[i].value();
            if (statusVal < 0) {
                _logger.log(Level.SEVERE, "A negative CosTransaction Status value was detected.");
            } else {
                directLookup[statusVal] = JTAStatus[i];
            }
        }

    }

    /**
     * get the singleton TransactionManagerImpl
     */
    static synchronized
    public TransactionManagerImpl getTransactionManagerImpl() {
        if (tm == null) {
            tm = new TransactionManagerImpl();
        }
        return tm;
    }

    /**
     * Create a transaction manager instance
     */
    private TransactionManagerImpl() {
        try {
            ORB orb = Configuration.getORB();
            if (orb != null) {
                current = org.omg.CosTransactions.CurrentHelper.
                    narrow(orb.resolve_initial_references("TransactionCurrent"/*#Frozen*/));
            } else {
                DefaultTransactionService dts = new DefaultTransactionService();
                Properties p = TransactionServiceProperties.getJTSProperties(Globals.getDefaultHabitat(), false);
                if (!Configuration.isFileLoggingDisabled()) {
                    String logdir = p.getProperty(Configuration.LOG_DIRECTORY);
                    _logger.fine("======= logdir ======= " + logdir);
                    if (logdir != null) {
                        (new File(logdir)).mkdirs();
                    }
                }

                dts.identify_ORB(null, null, p);
                current = dts.get_current();
            }

            // This will release locks in RecoveryManager which were created
            // by RecoveryManager.initialize() call in the TransactionFactoryImpl constructor
            // if startup recovery didn't happen yet.
            TransactionServiceProperties.initRecovery(true);

            // V2-commented-out transactionStates = new Hashtable();
        } catch (InvalidName inex) { 
			_logger.log(Level.SEVERE,
					"jts.unexpected_error_in_create_transaction_manager",inex);
        } catch (Exception ex) {
			_logger.log(Level.SEVERE,
					"jts.unexpected_error_in_create_transaction_manager",ex);
		}			
    }

    /**
     * extends props with the JTS-related properties
     * based on the specified parameters.
     * The properties will be used as part of ORB.init() call.
     *
     * @param prop the properties that will be extended
     * @param logDir directory for the log, current directory if null
     * @param trace enable JTS tracing
     * @param traceDir directory for tracing, current directory if null
     *
     */
    static public void initJTSProperties(Properties props, String logDir,
                                         boolean trace, String traceDir) {
        if (traceDir == null) traceDir = "."/*#Frozen*/;
        if (logDir == null) logDir = "."/*#Frozen*/;

        props.put("com.sun.corba.se.CosTransactions.ORBJTSClass"/*#Frozen*/,
                  "com.sun.jts.CosTransactions.DefaultTransactionService"/*#Frozen*/);
        props.put("com.sun.jts.traceDirectory"/*#Frozen*/, traceDir);
        props.put("com.sun.jts.logDirectory"/*#Frozen*/, logDir);
        if (trace) {
            props.put("com.sun.jts.trace"/*#Frozen*/, "true"/*#Frozen*/);
        }
    }

    /**
     * given a CosTransactions Status, return
     * the equivalent JTA Status
     */
    static public int mapStatus(Status status) {
        int statusVal = status.value();
        if (statusVal < 0 || statusVal > maxStatus) {
            return javax.transaction.Status.STATUS_UNKNOWN;
        } else {
            return directLookup[statusVal];
        }
    }

    /**
     * Create a new transaction and associate it with the current thread.
     *
     * @exception NotSupportedException Thrown if the thread is already
     *    associated with a transaction.
     */
    public void begin()
        throws NotSupportedException, SystemException {

        try {
            // does not support nested transaction
            if (current.get_control() != null) {
                throw new NotSupportedException();
            }
            current.begin();
        } catch (TRANSACTION_ROLLEDBACK ex) {
            throw new NotSupportedException();
        } catch (SubtransactionsUnavailable ex) {
            throw new SystemException();
        }
    }

	//START IASRI PERFIMPROVEMNT
    /**
     * Create a new transaction with the given timeout and associate it 
	 *		with the current thread.
     *
     * @exception NotSupportedException Thrown if the thread is already
     *    associated with a transaction.
     */
    public void begin(int timeout)
        throws NotSupportedException, SystemException {
        try {
            // does not support nested transaction
            if (current.get_control() != null) {
                throw new NotSupportedException();
            }
            ((com.sun.jts.CosTransactions.CurrentImpl)current).begin(timeout);
        } catch (TRANSACTION_ROLLEDBACK ex) {
            throw new NotSupportedException();
        } catch (SubtransactionsUnavailable ex) {
            throw new SystemException();
        }
    }
	//END IASRI PERFIMPROVEMNT

    /**
     * Complete the transaction associated with the current thread. When this
     * method completes, the thread becomes associated with no transaction.
     *
     * @exception RollbackException Thrown to indicate that
     *    the transaction has been rolled back rather than committed.
     *
     * @exception HeuristicMixedException Thrown to indicate that a heuristic
     *    decision was made and that some relevant updates have been committed
     *    while others have been rolled back.
     *
     * @exception HeuristicRollbackException Thrown to indicate that a
     *    heuristic decision was made and that all relevant updates have been
     *    rolled back.
     *
     * @exception SecurityException Thrown to indicate that the thread is
     *    not allowed to commit the transaction.
     *
     * @exception IllegalStateException Thrown if the current thread is
     *    not associated with a transaction.
     */
    public void commit() throws RollbackException,
	HeuristicMixedException, HeuristicRollbackException, SecurityException,
	IllegalStateException, SystemException {

        try {
            current.commit(true);
        } catch (TRANSACTION_ROLLEDBACK ex) {
            RollbackException rbe = new RollbackException();
            Throwable cause = ex.getCause();
            if (cause != null) {
                rbe.initCause(cause);
            }
            throw rbe;
        } catch (NoTransaction ex) {
            throw new IllegalStateException();
        } catch (NO_PERMISSION ex) {
            throw new SecurityException();
        } catch (HeuristicMixed ex) {
            throw new HeuristicMixedException();
        } catch (HeuristicHazard ex) {
            throw new HeuristicRollbackException();
        } catch (Exception ex) {
            // ex.printStackTrace();
            throw new SystemException(ex.toString());
        }
        /***
        Transaction tran = getTransaction();
        if (tran == null) throw new IllegalStateException();
        tran.commit();
        ***/
    }

    /**
     * Roll back the transaction associated with the current thread. When this
     * method completes, the thread becomes associated with no transaction.
     *
     * @exception SecurityException Thrown to indicate that the thread is
     *    not allowed to roll back the transaction.
     *
     * @exception IllegalStateException Thrown if the current thread is
     *    not associated with a transaction.
     */
    public void rollback()
        throws IllegalStateException, SecurityException, SystemException {

        try {
            current.rollback();
        } catch (NoTransaction ex) {
            throw new IllegalStateException();
        } catch (NO_PERMISSION ex) {
            throw new SecurityException();
        } catch (Exception ex) {
            throw new SystemException(ex.toString());
        }

        /***
        Transaction tran = getTransaction();
        if (tran == null) throw new IllegalStateException();
        tran.rollback();
        ***/
    }

    /**
     * Modify the transaction associated with the current thread such that
     * the only possible outcome of the transaction is to roll back the
     * transaction.
     *
     * @exception IllegalStateException Thrown if the current thread is
     *    not associated with a transaction.
     */
    public void setRollbackOnly()
        throws IllegalStateException, SystemException {

        try {
            current.rollback_only();
        } catch (NoTransaction ex) {
            throw new IllegalStateException();
        } catch (Exception ex) {
            throw new SystemException(ex.toString());
        }
    }

    /**
     * Obtain the status of the transaction associated with the current thread.
     *
     * @return The transaction status. If no transaction is associated with
     *    the current thread, this method returns the Status.NoTransaction
     *    value.
     */
    public int getStatus() throws SystemException {
        try {
            Status status = current.get_status();
            return mapStatus(status);
        } catch (Exception ex) {
            throw new SystemException(ex.toString());
        }
    }

    /**
     * Modify the timeout value that is associated with transactions started
     * by subsequent invocations of the begin method.
     *
     * <p> If an application has not called this method, the transaction
     * service uses some default value for the transaction timeout.
     *
     * @param seconds The value of the timeout in seconds. If the value is zero,
     *        the transaction service restores the default value. If the value
     *        is negative a SystemException is thrown.
     *
     * @exception SystemException Thrown if the transaction manager
     *    encounters an unexpected error condition.
     *
     */
    public synchronized void setTransactionTimeout(int seconds)
        throws SystemException {

        try {
            if (seconds < 0) {
				String msg = LogFormatter.getLocalizedMessage(_logger,
							 "jts.invalid_timeout");
                throw new SystemException(msg);
            }
            current.set_timeout(seconds);
        } catch (Exception ex) {
            throw new SystemException(ex.toString());
        }
    }

    /**
     * Get the transaction object that represents the transaction
     * context of the calling thread
     */
    public Transaction getTransaction()
        throws SystemException {

        try {
            Control control = current.get_control();
            if (control == null) {
                return null;
            } else {
                return createTransactionImpl(control);
            }
        } catch (Unavailable uex) {
            throw new SystemException(uex.toString());
        } catch (Exception ex) {
            throw new SystemException(ex.toString());
        }
    }

    /**
     * Resume the transaction context association of the calling thread
     * with the transaction represented by the supplied Transaction object.
     * When this method returns, the calling thread is associated with the
     * transaction context specified.
     */
    public void resume(Transaction suspended) throws
        InvalidTransactionException, IllegalStateException, SystemException {
        // thread is already associated with a transaction?
        if (getTransaction() != null) throw new IllegalStateException();
        // check for invalid Transaction object
        if (suspended == null) throw new InvalidTransactionException();
        if ((suspended instanceof TransactionImpl) == false) {
            throw new InvalidTransactionException();
        }
        Control control = ((TransactionImpl) suspended).getControl();
        try {
            current.resume(control);
        } catch (InvalidControl ex) {
			//_logger.log(Level.FINE,"Invalid Control Exception in resume",ex);
            throw new InvalidTransactionException();
        } catch (Exception ex) {
            throw new SystemException(ex.toString());
        }
    }


    /**
     * Suspend the transaction currently associated with the calling
     * thread and return a Transaction object that represents the
     * transaction context being suspended. If the calling thread is
     * not associated with a transaction, the method returns a null
     * object reference. When this method returns, the calling thread
     * is associated with no transaction.
     */
    public Transaction suspend() throws SystemException {
        try {
            Control control = current.suspend();
            if (control == null) return null;
            return createTransactionImpl(control);
        } catch (Unavailable uex) {
            throw new SystemException(uex.toString());
        } catch (Exception ex) {
            throw new SystemException(ex.toString());
        }
    }

    /**
    TransactionState getOrCreateTransactionState(GlobalTID gtid,
                                                 Transaction tran)
        throws SystemException {

        synchronized (transactionStates) {
            TransactionState result =
                (TransactionState) transactionStates.get(gtid);
            if (result == null) {
                result = new TransactionState(gtid);
                transactionStates.put(gtid, result);
                try {
                    // remove Transaction State on transaction completion
                    Synchronization sync =
                        new SynchronizationListener(gtid, result);
                    tran.registerSynchronization(sync);
                } catch (Exception ex) {
					_logger.log(Level.WARNING,
							"jts.unexpected_error_in_get_or_create_transaction_state",ex);
                    throw new SystemException();
                }
            }
            return result;
        }
    }

    TransactionState getTransactionState(GlobalTID gtid,
                                         Transaction tran)
        throws SystemException {

        synchronized (transactionStates) {
            return (TransactionState) transactionStates.get(gtid);
        }
    }

   **/

    private Transaction createTransactionImpl(Control control)
        throws Unavailable, SystemException
    {
        GlobalTID gtid = null;
        if (Configuration.isLocalFactory()) {
            gtid = ((ControlImpl) control).getGlobalTID();
        } else {
            ControlImpl cntrlImpl = ControlImpl.servant(JControlHelper.narrow(control));
            gtid = cntrlImpl.getGlobalTID();
        }

        // return new TransactionImpl(this, control, gtid);
        return new TransactionImpl(control, gtid);
    }

    /**
     * The application server passes in the list of XAResource objects
     * to be recovered.
     *
     * @param xaResourceList list of XAResource objects.
     */
    public static void recover(Enumeration xaResourceList) {
        RecoveryManager.recoverXAResources(xaResourceList);
    }

    /**
     * Recreate a transaction based on the Xid. This call causes the calling
     * thread to be associated with the specified transaction.
     * 
     * @param xid the Xid object representing a transaction. 
     * @param timeout positive, non-zero value for transaction timeout.
     */
    public static void recreate(Xid xid, long timeout) throws WorkException {
                
        // check if xid is valid
        if (xid == null || xid.getFormatId() == 0 ||
                xid.getBranchQualifier() == null || 
                xid.getGlobalTransactionId() == null) {
            WorkException workExc = new WorkCompletedException("Invalid Xid");
            workExc.setErrorCode(WorkException.TX_RECREATE_FAILED);
            throw workExc;
        }

        // has TransactionService been initialized?
        if (!DefaultTransactionService.isActive()) {
            WorkException workExc = 
                new WorkCompletedException("Transaction Manager unavailable");
            workExc.setErrorCode(WorkException.TX_RECREATE_FAILED);
            throw workExc;
        }
        
        // recreate the transaction        
        GlobalTID tid = new GlobalTID(xid);
        try {        
            CurrentTransaction.recreate(
		tid, (int) ((timeout <= 0) ? 0 : timeout));
        } catch (Throwable exc) {
            String errorCode = WorkException.TX_RECREATE_FAILED;
            if (exc instanceof INVALID_TRANSACTION &&
                    (((INVALID_TRANSACTION) exc).minor == 
                        MinorCode.TX_CONCURRENT_WORK_DISALLOWED)) {
                errorCode = WorkException.TX_CONCURRENT_WORK_DISALLOWED;
            }
            WorkException workExc = new WorkCompletedException(exc);
            workExc.setErrorCode(errorCode);
            throw workExc;             
        }  
    }

    /**
     * Release a transaction. This call causes the calling thread to be 
     * dissociated from the specified transaction.
     * 
     * @param xid the Xid object representing a transaction. 
     */    
    public static void release(Xid xid) throws WorkException {
                
        GlobalTID tid = new GlobalTID(xid);
        try {        
            CurrentTransaction.release(tid);
        } catch (Throwable exc) {
            String errorCode = WorkException.UNDEFINED;
            if (exc instanceof INTERNAL) {
                errorCode = WorkException.INTERNAL;
            }
            WorkException workExc = new WorkCompletedException(exc);
            workExc.setErrorCode(errorCode);            
            throw workExc;             
        }
    }

    /**
     * Provides a handle to a <code>XATerminator</code> instance. The
     * <code>XATerminator</code> instance could be used by a resource adapter 
     * to flow-in transaction completion and crash recovery calls from an EIS.
     *
     * @return a <code>XATerminator</code> instance.
     */    
    public static javax.resource.spi.XATerminator getXATerminator() {
        return new XATerminatorImpl();
    }

  	//START IASRI 4706150 
	/**
	* used to set XAResource timeout
	*/
	public static void setXAResourceTimeOut(int value){
		xaTimeOut = value;
	}

	public static int getXAResourceTimeOut(){
		return xaTimeOut;
	}
  	//END IASRI 4706150 
  /**
    class SynchronizationListener implements Synchronization {

        private GlobalTID gtid;
        private TransactionState tranState;

        SynchronizationListener(GlobalTID gtid, TransactionState tranState) {
            this.gtid = gtid;
            this.tranState = tranState;
        }

        public void afterCompletion(int status) {
            tranState.cleanupTransactionStateMapping();
        }

        public void beforeCompletion() {
            try {
	      tranState.beforeCompletion();
	    }catch(XAException xaex){
	      _logger.log(Level.WARNING,"jts.unexpected_xa_error_in_beforecompletion", new java.lang.Object[] {xaex.errorCode, xaex.getMessage()});
	      _logger.log(Level.WARNING,"",xaex);
            } catch (Exception ex) {
				_logger.log(Level.WARNING,"jts.unexpected_error_in_beforecompletion",ex);
            }
        }
    }
  **/

    /**
    void cleanupTransactionState(GlobalTID gtid) {
        transactionStates.remove(gtid);
    }
    **/
}

