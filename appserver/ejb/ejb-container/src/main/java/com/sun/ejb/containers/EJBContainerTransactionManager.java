/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
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
 */
// Portions Copyright [2016-2021] [Payara Foundation and/or its affiliates.]
package com.sun.ejb.containers;

import com.sun.ejb.Container;
import com.sun.ejb.EjbInvocation;
import com.sun.enterprise.deployment.MethodDescriptor;
import com.sun.enterprise.transaction.api.JavaEETransaction;
import com.sun.enterprise.transaction.api.JavaEETransactionManager;
import com.sun.enterprise.util.LocalStringManagerImpl;

import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.ejb.EJBException;
import jakarta.ejb.NoSuchEntityException;
import jakarta.ejb.NoSuchObjectLocalException;
import jakarta.ejb.TransactionRequiredLocalException;
import jakarta.ejb.TransactionRolledbackLocalException;
import jakarta.transaction.RollbackException;
import jakarta.transaction.Status;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.UserTransaction;

import org.glassfish.ejb.deployment.descriptor.ContainerTransaction;
import org.glassfish.ejb.deployment.descriptor.EjbApplicationExceptionInfo;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.runtime.IASEjbExtraDescriptors;

/**
 * Container support for handling transactions
 *
 * @author mvatkina
 */
public class EJBContainerTransactionManager {

    private static final Logger LOG = EjbContainerUtilImpl.getLogger();
    private static final String USER_TX = "java:comp/UserTransaction";

    private final EjbContainerUtil ejbContainerUtilImpl = EjbContainerUtilImpl.getInstance();
    private final JavaEETransactionManager transactionManager;
    private final BaseContainer container;
    private final EjbDescriptor ejbDescriptor;
    private final int cmtTimeoutInSeconds;

    /**
     * Construct new instance and set basic references
     */
    EJBContainerTransactionManager(Container c, EjbDescriptor ejbDesc) {
        container = (BaseContainer)c;
        ejbDescriptor = ejbDesc;
        transactionManager = ejbContainerUtilImpl.getTransactionManager();
        final IASEjbExtraDescriptors iased = ejbDesc.getIASEjbExtraDescriptors();
        cmtTimeoutInSeconds = iased.getCmtTimeoutInSeconds();
    }

    /**
     * Calculate for the transaction attribute for a method.
     * This is only used during container initialization.  After that,
     * tx attributes can be looked up with variations of getTxAttr()
     */
    int findTxAttr(MethodDescriptor md) {
        if (container.isBeanManagedTran) {
            return Container.TX_BEAN_MANAGED;
        }
        final int txAttr = getTransactionAttribute(md);
        if (txAttr < 0) {
            throw new EJBException("Transaction Attribute not found for method " + md.prettyPrint());
        }
        container.validateTxAttr(md, txAttr);
        return txAttr;
    }

    private int getTransactionAttribute(final MethodDescriptor md) {
        final ContainerTransaction containerTx = ejbDescriptor.getContainerTransactionFor(md);
        if (containerTx == null) {
            return -1;
        }
        final String attr = containerTx.getTransactionAttribute();
        if (attr.equals(ContainerTransaction.NOT_SUPPORTED)) {
            return Container.TX_NOT_SUPPORTED;
        } else if (attr.equals(ContainerTransaction.SUPPORTS)) {
            return Container.TX_SUPPORTS;
        } else if (attr.equals(ContainerTransaction.REQUIRED)) {
            return Container.TX_REQUIRED;
        } else if (attr.equals(ContainerTransaction.REQUIRES_NEW)) {
            return Container.TX_REQUIRES_NEW;
        } else if (attr.equals(ContainerTransaction.MANDATORY)) {
            return Container.TX_MANDATORY;
        } else if (attr.equals(ContainerTransaction.NEVER)) {
            return Container.TX_NEVER;
        } else {
            return -2;
        }
    }

    /**
     * Handle transaction requirements, if any, before invoking bean method
     */
    final void preInvokeTx(EjbInvocation inv) throws Exception {
        LOG.finest(() -> String.format("preInvokeTx(inv=%s)", inv));
        // Get existing Tx status: this tells us if the client
        // started a transaction which was propagated on this invocation.
        final Integer preInvokeTxStatus = inv.getPreInvokeTxStatus();
        final int status = preInvokeTxStatus == null ? transactionManager.getStatus() : preInvokeTxStatus.intValue();

        // For MessageDrivenBeans,ejbCreate/ejbRemove must be called without a Tx.
        // For StatelessSessionBeans, ejbCreate/ejbRemove must be called without a Tx.
        // For StatefullSessionBeans ejbCreate/ejbRemove/ejbFind can be called with or without a Tx.
        // For EntityBeans, ejbCreate/ejbRemove/ejbFind must be called with a Tx so no special work needed.
        if (container.suspendTransaction(inv)) {
            // EJB2.0 section 7.5.7 says that ejbCreate/ejbRemove etc are called
            // without a Tx. So suspend the client's Tx if any.

            // Note: ejbRemove cannot be called when EJB is associated with
            // a Tx, according to EJB2.0 section 7.6.4. This check is done in
            // the container's implementation of removeBean().

            if (status != Status.STATUS_NO_TRANSACTION) {
                // client request is associated with a Tx
                try {
                    inv.clientTx = transactionManager.suspend();
                } catch (SystemException ex) {
                    throw new EJBException(ex);
                }
            }
            return;
        }

        // isNullTx is true if the client sent a null tx context
        // (i.e. a tx context with a null Coordinator objref)
        // or if this server's tx interop mode flag is false.
        // Follow the tables in EJB2.0 sections 19.6.2.2.1 and 19.6.2.2.2.
        final boolean isNullTx = inv.isRemote ? transactionManager.isNullTransaction() : false;
        final int txAttr = container.getTxAttr(inv);
        final EJBContextImpl context = (EJBContextImpl)inv.context;

        // Note: in the code below, inv.clientTx is set ONLY if the
        // client's Tx is actually suspended.

        // get the Tx associated with the EJB from previous invocation,
        // if any.
        final Transaction prevTx = context.getTransaction();

        switch (txAttr) {
            case Container.TX_BEAN_MANAGED:
                // TX_BEAN_MANAGED rules from EJB2.0 Section 17.6.1, Table 13
                // Note: only MDBs and SessionBeans can be TX_BEAN_MANAGED
                if (status != Status.STATUS_NO_TRANSACTION) {
                    // client request associated with a Tx, always suspend
                    inv.clientTx = transactionManager.suspend();
                }
                if (container.isStatefulSession && prevTx != null
                    && prevTx.getStatus() != Status.STATUS_NO_TRANSACTION ) {
                    // Note: if prevTx != null , then it means
                    // afterCompletion was not called yet for the
                    // previous transaction on the EJB.

                    // The EJB was previously associated with a Tx which was
                    // begun by the EJB itself in a previous invocation.
                    // This is only possible for stateful SessionBeans
                    // not for StatelessSession or Entity.
                    transactionManager.resume(prevTx);

                    // This allows the TM to enlist resources
                    // used by the EJB with the transaction
                    transactionManager.enlistComponentResources();
                }

                break;

            case Container.TX_NOT_SUPPORTED:
                if (status != Status.STATUS_NO_TRANSACTION) {
                    inv.clientTx = transactionManager.suspend();
                }
                container.checkUnfinishedTx(prevTx, inv);
                container.preInvokeNoTx(inv);
                break;

            case Container.TX_MANDATORY:
                if (isNullTx || status == Status.STATUS_NO_TRANSACTION) {
                    throw new TransactionRequiredLocalException();
                }

                useClientTx(prevTx, inv);
                break;

            case Container.TX_REQUIRED:
                if (isNullTx) {
                    throw new TransactionRequiredLocalException();
                }

                if (status == Status.STATUS_NO_TRANSACTION) {
                    inv.clientTx = null;
                    startNewTx(prevTx, inv);
                } else { // There is a client Tx
                    inv.clientTx = transactionManager.getTransaction();
                    useClientTx(prevTx, inv);
                }
                break;

            case Container.TX_REQUIRES_NEW:
                if (status != Status.STATUS_NO_TRANSACTION) {
                    inv.clientTx = transactionManager.suspend();
                }
                startNewTx(prevTx, inv);
                break;

            case Container.TX_SUPPORTS:
                if (isNullTx) {
                    throw new TransactionRequiredLocalException();
                }

                if (status != Status.STATUS_NO_TRANSACTION) {
                    useClientTx(prevTx, inv);
                } else { // we need to invoke the EJB with no Tx.
                    container.checkUnfinishedTx(prevTx, inv);
                    container.preInvokeNoTx(inv);
                }
                break;

            case Container.TX_NEVER:
                if (isNullTx || status != Status.STATUS_NO_TRANSACTION) {
                    throw new EJBException("EJB cannot be invoked in global transaction");
                }
                container.checkUnfinishedTx(prevTx, inv);
                container.preInvokeNoTx(inv);
                break;

            default:
                throw new EJBException("Bad transaction attribute");
        }
    }

    /**
     * Start a CMT transaction, enlist resources, and call afterBegin, which is a
     * no-op in those containers that do not need this callback
     */
    private void startNewTx(Transaction prevTx, EjbInvocation inv) throws Exception {
        LOG.finest(() -> String.format("startNewTx(prevTx=%s, inv=%s)", prevTx, inv));
        container.checkUnfinishedTx(prevTx, inv);

        if (cmtTimeoutInSeconds > 0) {
            transactionManager.begin(cmtTimeoutInSeconds);
        } else {
            transactionManager.begin();
        }

        EJBContextImpl context = (EJBContextImpl) inv.context;
        Transaction tx = transactionManager.getTransaction();
        if (!container.isSingleton) {
            context.setTransaction(tx);
        }

        // This allows the TM to enlist resources used by the EJB
        // with the transaction
        transactionManager.enlistComponentResources();

        // register synchronization for methods other than finders/home methods
        if (!inv.invocationInfo.isHomeFinder) {
            // Register for Synchronization notification
            ejbContainerUtilImpl.getContainerSync(tx).addBean(context);
        }

        // Call afterBegin/ejbLoad. If ejbLoad throws exceptions,
        // the completeNewTx machinery called by postInvokeTx
        // will rollback the tx. Since we have already registered
        // a Synchronization object with the TM, the afterCompletion
        // will get called.
        container.afterBegin(context);
    }

    /**
     * Use caller transaction to execute a bean method
     */
    protected void useClientTx(Transaction prevTx, EjbInvocation inv) {
        LOG.finest(() -> String.format("useClientTx(prevTx=%s, inv=%s)", prevTx, inv));
        final Transaction clientTx;
        final int status;
        final int prevStatus;
        try {
            // Note: inv.clientTx will not be set at this point.
            clientTx = transactionManager.getTransaction();
            status = clientTx.getStatus();  // clientTx cant be null
            if (prevTx == null) {
                prevStatus = -1;
            } else {
                prevStatus = prevTx.getStatus();
            }
        } catch (Exception ex) {
            final TransactionRolledbackLocalException toThrow = new TransactionRolledbackLocalException(ex.getMessage(), ex);
            try {
                transactionManager.setRollbackOnly();
            } catch (Exception e) {
                toThrow.addSuppressed(ex);
            }
            throw toThrow;
        }

        // If the client's tx is going to rollback, it is fruitless
        // to invoke the EJB, so throw an exception back to client.
        if (status == Status.STATUS_MARKED_ROLLBACK //
            || status == Status.STATUS_ROLLEDBACK //
            || status == Status.STATUS_ROLLING_BACK) {
            throw new TransactionRolledbackLocalException("Client's transaction aborted");
        }

        container.validateEMForClientTx(inv, (JavaEETransaction) clientTx);

        if (prevTx == null || prevStatus == Status.STATUS_NO_TRANSACTION) {
            // First time the bean is running in this new client Tx
            EJBContextImpl context = (EJBContextImpl)inv.context;

            //Must change this for singleton
            if (!container.isSingleton) {
                context.setTransaction(clientTx);
            }
            try {
                transactionManager.enlistComponentResources();

                if ( !container.isStatelessSession && !container.isMessageDriven && !container.isSingleton) {
                    // Create a Synchronization object.

                    // Not needed for stateless beans or message-driven beans
                    // or singletons because they cant have Synchronization callbacks,
                    // and they cant be associated with a tx across
                    // invocations.
                    // Register sync for methods other than finders/home methods
                    if ( !inv.invocationInfo.isHomeFinder ) {
                        ejbContainerUtilImpl.getContainerSync(clientTx).addBean(
                        context);
                    }

                    container.afterBegin(context);
                }
            } catch (Exception ex) {
                final TransactionRolledbackLocalException toThrow = new TransactionRolledbackLocalException("", ex);
                try {
                    transactionManager.setRollbackOnly();
                } catch (Exception e) {
                    toThrow.addSuppressed(e);
				}
                throw toThrow;
            }
        } else { // Bean already has a transaction associated with it.
            if (!prevTx.equals(clientTx)) {
                // There is already a different Tx in progress !!
                // Note: this can only happen for stateful SessionBeans.
                // EntityBeans will get a different context for every Tx.
                if (container.isSession) {
                    // Row 2 in Table E
                    throw new IllegalStateException("EJB is already associated with an incomplete transaction");
                }
            } else { // Bean was invoked again with the same transaction
                // This allows the TM to enlist resources used by the EJB
                // with the transaction
                try {
                    transactionManager.enlistComponentResources();
                } catch (Exception ex) {
                    final TransactionRolledbackLocalException toThrow = new TransactionRolledbackLocalException("", ex);
                    try {
                        transactionManager.setRollbackOnly();
                    } catch (Exception e) {
                        toThrow.addSuppressed(e);
					}
                    throw toThrow;
                }
            }
        }
     }

    /**
     * Handle transaction requirements, if any, after invoking bean method
     */
    protected void postInvokeTx(EjbInvocation inv) throws Exception {
        LOG.finest(() -> String.format("postInvokeTx(inv=%s)", inv));

        // For StatelessSessionBeans, ejbCreate/ejbRemove was called without a Tx,
        // so resume client's Tx if needed.
        // For StatefulSessionBeans ejbCreate/ejbRemove was called with or without a Tx,
        // so resume client's Tx if needed.
        // For EntityBeans, ejbCreate/ejbRemove/ejbFind must be called with a Tx
        // so no special processing needed.
        if (container.resumeTransaction(inv)) {
           // check if there was a suspended client Tx
            if (inv.clientTx != null) {
                transactionManager.resume(inv.clientTx);
            }
            if (inv.exception != null && inv.exception instanceof BaseContainer.PreInvokeException) {
                inv.exception = inv.exception.getCause();
            }
            return;
        }

        EJBContextImpl context = (EJBContextImpl)inv.context;

        final int status = transactionManager.getStatus();
        final int txAttr = inv.invocationInfo.txAttr;
        final Throwable oriException = inv.exception;

        // Note: inv.exception may have been thrown by the container
        // during preInvoke (i.e. bean may never have been invoked).

        // Exception and Tx handling rules. See EJB2.0 Sections 17.6, 18.3.
        final Throwable newException;
        switch (txAttr) {
            case Container.TX_BEAN_MANAGED:
                // EJB2.0 section 18.3.1, Table 16
                // Note: only SessionBeans can be TX_BEAN_MANAGED
                newException = checkExceptionBeanMgTx(context, oriException, status);
                if (inv.clientTx != null) {
                    // there was a client Tx which was suspended
                    transactionManager.resume(inv.clientTx);
                }
                break;

            case Container.TX_NOT_SUPPORTED:
            case Container.TX_NEVER:
                // NotSupported and Never are handled in the same way
                // EJB2.0 sections 17.6.2.1, 17.6.2.6.
                // EJB executed in no Tx
                newException = checkExceptionNoTx(context, oriException);
                container.postInvokeNoTx(inv);

                if (inv.clientTx != null) {
                    // there was a client Tx which was suspended
                    transactionManager.resume(inv.clientTx);
                }

                break;

            case Container.TX_MANDATORY:
                // EJB2.0 section 18.3.1, Table 15
                // EJB executed in client's Tx
                newException = checkExceptionClientTx(context, oriException);
                break;

            case Container.TX_REQUIRED:
                // EJB2.0 section 18.3.1, Table 15
                if (inv.clientTx == null) {
                    // EJB executed in new Tx started in preInvokeTx
                    newException = completeNewTx(context, oriException, status);
                } else {
                    // EJB executed in client's tx
                    newException = checkExceptionClientTx(context, oriException);
                }
                break;

            case Container.TX_REQUIRES_NEW:
                // EJB2.0 section 18.3.1, Table 15
                // EJB executed in new Tx started in preInvokeTx
                newException = completeNewTx(context, oriException, status);

                if (inv.clientTx != null) {
                    // there was a client Tx which was suspended
                    transactionManager.resume(inv.clientTx);
                }
                break;

            case Container.TX_SUPPORTS:
                // EJB2.0 section 18.3.1, Table 15
                if (status != Status.STATUS_NO_TRANSACTION) {
                    // EJB executed in client's tx
                    newException = checkExceptionClientTx(context, oriException);
                } else {
                    // EJB executed in no Tx
                    newException = checkExceptionNoTx(context, oriException);
                    container.postInvokeNoTx(inv);
                }
                break;

            default:
                newException = oriException;
        }

        inv.exception = newException;

        // XXX If any of the TM commit/rollback/suspend calls throws an
        // exception, should the transaction be rolled back if not already so ?
     }

    final UserTransaction getUserTransaction() {
        // Only session beans with bean-managed transactions
        // or message-driven beans with bean-managed transactions
        // can programmatically demarcate transactions.
        if ((container.isSession || container.isMessageDriven) && container.isBeanManagedTran) {
            try {
                return (UserTransaction) container.namingManager.getInitialContext().lookup(USER_TX);
            } catch (Exception ex) {
                final String message = LOG.getResourceBundle().getString("ejb.user_transaction_exception");
                throw new EJBException(message, ex);
            }
        }
        final LocalStringManagerImpl localStrings = new LocalStringManagerImpl(EJBContainerTransactionManager.class);
        throw new IllegalStateException( //
            localStrings.getLocalString( //
                "ejb.ut_only_for_bmt", "Only session beans with bean-managed transactions can obtain UserTransaction"));
    }

    private Throwable checkExceptionBeanMgTx(EJBContextImpl context, Throwable exception, int status) throws Exception {
        LOG.finest(() -> String.format("checkExceptionBeanMgTx(context=%s, exception=%s, status=%s)", //
            context, exception, status));
        // EJB2.0 section 18.3.1, Table 16
        if (exception != null && exception instanceof BaseContainer.PreInvokeException) {
            // A PreInvokeException was thrown, so bean was not invoked
            return exception.getCause();
        } else if (status == Status.STATUS_NO_TRANSACTION) {
            // EJB was invoked, EJB's Tx is complete.
            return checkExceptionNoTx(context, exception);
        } else {
            // EJB was invoked, EJB's Tx is incomplete.
            // See EJB2.0 Section 17.6.1
            if (container.isStatefulSession) {
                if (container.isSystemUncheckedException(exception)) {
                    // system/unchecked exception was thrown by EJB
                    destroyBeanAndRollback(context, null);
                    return processSystemException(exception);
                }
                if (isAppExceptionRequiringRollback(exception)) {
                    rollback();
                } else {
                    transactionManager.suspend();
                }
                return exception;
            } else if (container.isStatelessSession) { // stateless SessionBean
                return destroyBeanAndRollback(context, "Stateless SessionBean");
            } else if (container.isSingleton) {
                return destroyBeanAndRollback(context, "Singleton SessionBean");
            } else {
                return destroyBeanAndRollback(context, "MessageDrivenBean");
            }
        }
    }

    private Throwable checkExceptionNoTx(EJBContextImpl context, Throwable exception) throws EJBException {
        LOG.finest(() -> String.format("checkExceptionNoTx(context=%s, exception=%s)", context, exception));
        if (exception == null) {
            return null;
        }

        if (exception instanceof BaseContainer.PreInvokeException) {
            // A PreInvokeException was thrown, so bean was not invoked
            return exception.getCause();
        }

        // If PreInvokeException was not thrown, EJB was invoked with no Tx
        if (container.isSystemUncheckedException(exception)) {
            // Table 15, EJB2.0
            final Throwable newException = processSystemException(exception);
            container.forceDestroyBean(context);
            return newException;
        }
        return exception;
    }

    // Can be called by the BaseContainer - do not make it private
    Throwable checkExceptionClientTx(EJBContextImpl context, Throwable exception) throws SystemException {
        LOG.finest(() -> String.format("checkExceptionClientTx(context=%s, exception=%s)", context, exception));
        if (exception == null) {
            return null;
        }
        if (exception instanceof BaseContainer.PreInvokeException) {
            // A PreInvokeException was thrown, so bean was not invoked
            return exception.getCause();
        }

        // If PreInvokeException wasn't thrown, EJB was invoked with client's Tx
        if (container.isSystemUncheckedException(exception)) {
            // Table 15, EJB2.0
            try {
                container.forceDestroyBean(context);
            } finally {
                transactionManager.setRollbackOnly();
            }
            final TransactionRolledbackLocalException newException = new TransactionRolledbackLocalException(
                "Exception thrown from bean: " + exception);
            newException.initCause(exception);
            return newException;
        }
        if (isAppExceptionRequiringRollback(exception)) {
            transactionManager.setRollbackOnly();
        }
        return exception;
     }

    // this is the counterpart of startNewTx
    private Throwable completeNewTx(EJBContextImpl context, Throwable exception, int status) throws Exception {
        LOG.finest(
            () -> String.format("completeNewTx(context=%s, exception=%s, status=%s)", context, exception, status));
        final Throwable newException;
        if (exception instanceof BaseContainer.PreInvokeException) {
            newException = exception.getCause();
        } else {
            newException = exception;
        }

        if (status == Status.STATUS_NO_TRANSACTION) {
            // no tx was started, probably an exception was thrown
            // before tm.begin() was called
            return newException;
        }

        if (container.isStatefulSession && (context instanceof SessionContextImpl)) {
            ((SessionContextImpl) context).setTxCompleting(true);
        }

        // A new tx was started, so we must commit/rollback
        if (container.isSystemUncheckedException(newException)) {
            // EJB2.0 section 18.3.1, Table 15
            // Rollback the Tx we started
            destroyBeanAndRollback(context, null);
            return processSystemException(newException);
        }
        try {
            if (status == Status.STATUS_MARKED_ROLLBACK) {
                // EJB2.0 section 18.3.1, Table 15, and 18.3.6:
                // rollback tx, no exception
                rollback();
            } else {
                if (newException != null && isAppExceptionRequiringRollback(newException)) {
                    rollback();
                } else {
                    // Note: if exception is an application exception
                    // we do a commit as in EJB2.0 Section 18.3.1,
                    // Table 15. Commit the Tx we started
                    transactionManager.commit();
                }
            }
            return newException;
        } catch (RollbackException ex) {
            LOG.log(Level.FINE, "ejb.transaction_abort_exception", ex);
            // EJB2.0 section 18.3.6
            final EJBException result = new EJBException("Transaction aborted", ex);
            if (newException != null) {
                result.addSuppressed(newException);
            }
            return result;
        } catch (Exception ex) {
            LOG.log(Level.FINE, "ejb.cmt_exception", ex);
            // Commit or rollback failed.
            // EJB2.0 section 18.3.6
            final EJBException result = new EJBException("Unable to complete container-managed transaction.", ex);
            if (newException != null) {
                result.addSuppressed(newException);
            }
            return result;
        }
    }

    /**
     * @return null if the type was null, {@link EJBException} otherwise.
     */
    private EJBException destroyBeanAndRollback(EJBContextImpl context, String type) throws SystemException {
        LOG.finest(() -> String.format("destroyBeanAndRollback(context=%s, type=%s)", context, type));
        try {
            container.forceDestroyBean(context);
        } finally {
            rollback();
        }
        if (type == null) {
            return null;
        }
        LOG.log(Level.FINE, "ejb.incomplete_sessionbean_txn_exception");
        return new EJBException(type + " method returned without completing transaction");
    }

    private void rollback() throws SystemException {
        if (transactionManager.isTimedOut()) {
            LOG.log(Level.WARNING, "ejb.tx_timeout",
                new Object[] {transactionManager.getTransaction(), ejbDescriptor.getName()});
        }
        transactionManager.rollback();
    }

    private Throwable processSystemException(Throwable sysEx) {
        if (sysEx instanceof EJBException) {
            return sysEx;
        }
        // EJB2.0 section 18.3.4
        if (sysEx instanceof NoSuchEntityException) { // for EntityBeans only
            return new NoSuchObjectLocalException("NoSuchEntityException thrown by EJB method.").initCause(sysEx);
        }
        return new EJBException(sysEx.getMessage()).initCause(sysEx);
    }


    /**
     * @return true if this exception is an Application Exception and
     * it requires rollback of the transaction in which it was thrown.
     */
    private boolean isAppExceptionRequiringRollback(Throwable exception) {
        if (exception == null) {
            return false;
        }
        final String exceptionClassName = exception.getClass().getName();
        final Map<String, EjbApplicationExceptionInfo> appExceptions = //
            ejbDescriptor.getEjbBundleDescriptor().getApplicationExceptions();

        Class<?> exceptionClass = exception.getClass();
        while (exceptionClass != null) {
            final String eClassName = exceptionClass.getName();
            if (appExceptions.containsKey(eClassName)) {
                final EjbApplicationExceptionInfo exceptionInfo = appExceptions.get(eClassName);
                if (exceptionClassName.equals(eClassName) || exceptionInfo.getInherited()) {
                    // Exact exception is specified as an ApplicationException
                    // or superclass exception is inherited
                    return exceptionInfo.getRollback();
                }
                break;
            }
            exceptionClass = exceptionClass.getSuperclass();
        }
        return false;
    }
}
