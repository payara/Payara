package org.glassfish.concurrent.runtime;

import com.sun.enterprise.deployment.JndiNameEnvironment;
import jakarta.transaction.InvalidTransactionException;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import org.glassfish.api.invocation.ComponentInvocation;

interface InvocationFacade {
    ComponentInvocation getCurrentInvocation();
    JndiNameEnvironment getJndiNameEnvironment(String componentId);
    void preInvoke(ComponentInvocation invocation);
    void postInvoke(ComponentInvocation invocation);
    void cleanupTransaction(boolean canCommitOrRollback);
    boolean isApplicationEnabled(String appName);
    boolean isContextService();
    ClassLoader getContextClassLoader(String appName);

    Transaction suspend() throws SystemException;

    void resume(Transaction suspendedTxn) throws SystemException, InvalidTransactionException;
}
