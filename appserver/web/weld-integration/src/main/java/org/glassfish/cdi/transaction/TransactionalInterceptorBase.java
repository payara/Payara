/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/main/LICENSE.txt
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

// Portions Copyright [2014-2021] [Payara Foundation and/or its affiliates]

package org.glassfish.cdi.transaction;

import static java.util.logging.Level.FINE;
import static java.util.logging.Level.SEVERE;
import static java.util.logging.Level.WARNING;
import static org.glassfish.cdi.transaction.TransactionScopedCDIUtil.getAnnotation;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.inject.spi.BeanManager;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.interceptor.InvocationContext;
import javax.naming.InitialContext;
import javax.naming.NameNotFoundException;
import javax.naming.NamingException;
import jakarta.transaction.SystemException;
import jakarta.transaction.Transaction;
import jakarta.transaction.TransactionManager;
import jakarta.transaction.Transactional;

import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;
import org.glassfish.logging.annotation.LogMessageInfo;
import org.glassfish.logging.annotation.LogMessagesResourceBundle;
import org.glassfish.logging.annotation.LoggerInfo;

import com.sun.enterprise.transaction.spi.TransactionOperationsManager;

/**
 * Base class for all interceptors providing common logic for exception handling, etc.
 *
 * @author Paul Parkinson
 */
public class TransactionalInterceptorBase implements Serializable {

    private static final long serialVersionUID = 1L;

    @LogMessagesResourceBundle
    public static final String SHARED_LOGMESSAGE_RESOURCE = "org.glassfish.cdi.LogMessages";

    @LoggerInfo(subsystem = "AS-CDI-JTA", description = "CDI-JTA", publish = true)
    public static final String CDI_JTA_LOGGER_SUBSYSTEM_NAME = "javax.enterprise.resource.jta";
    private static final Logger _logger = Logger.getLogger(CDI_JTA_LOGGER_SUBSYSTEM_NAME, SHARED_LOGMESSAGE_RESOURCE);

    @LogMessageInfo(message = "Encountered NamingException while attempting to acquire "
            + "transaction manager for Transactional annotation interceptors {0}", action = "Fix the issue for the Naming exception", cause = "Transaction annotation processing for the Naming", level = "SEVERE")
    public static final String CDI_JTA_NAME_EXCEPTION = "AS-JTA-00001";

    @LogMessageInfo(message = "About to setRollbackOnly from @Transactional interceptor on " + "transaction: {0}", level = "FINE")
    public static final String CDI_JTA_SETROLLBACK = "AS-JTA-00002";

    @LogMessageInfo(message = "No ComponentInvocation present for @Transactional annotation "
            + "processing. Restriction on use of UserTransaction will not be enforced.", level = "WARNING")
    public static final String CDI_JTA_NOCOMPONENT = "AS-JTA-00003";

    @LogMessageInfo(message = "In MANDATORY TransactionalInterceptor", level = "FINE")
    public static final String CDI_JTA_MANDATORY = "AS-JTA-00004";

    @LogMessageInfo(message = "In NEVER TransactionalInterceptor", level = "FINE")
    public static final String CDI_JTA_NEVER = "AS-JTA-00005";

    @LogMessageInfo(message = "In NOT_SUPPORTED TransactionalInterceptor", level = "FINE")
    public static final String CDI_JTA_NOTSUPPORTED = "AS-JTA-00006";

    @LogMessageInfo(message = "Managed bean with Transactional annotation and TxType of NOT_SUPPORTED "
            + "called inside a transaction context. Suspending transaction...", level = "FINE")
    public static final String CDI_JTA_MBNOTSUPPORTED = "AS-JTA-00007";

    @LogMessageInfo(message = "Managed bean with Transactional annotation and TxType of NOT_SUPPORTED "
            + "called inside a transaction context.  Suspending transaction failed due to {0}", level = "FINE")
    public static final String CDI_JTA_MBNOTSUPPORTEDTX = "AS-JTA-00008";

    @LogMessageInfo(message = "In REQUIRED TransactionalInterceptor", level = "FINE")
    public static final String CDI_JTA_REQUIRED = "AS-JTA-00009";

    @LogMessageInfo(message = "Managed bean with Transactional annotation and TxType of REQUIRED "
            + "called outside a transaction context.  Beginning a transaction...", level = "FINE")
    public static final String CDI_JTA_MBREQUIRED = "AS-JTA-00010";

    @LogMessageInfo(message = "Managed bean with Transactional annotation and TxType of REQUIRED "
            + "encountered exception during begin {0}", level = "WARNING")
    public static final String CDI_JTA_MBREQUIREDBT = "AS-JTA-00011";

    @LogMessageInfo(message = "Managed bean with Transactional annotation and TxType of REQUIRED "
            + "encountered exception during commit {0}", level = "WARNING")
    public static final String CDI_JTA_MBREQUIREDCT = "AS-JTA-00012";

    @LogMessageInfo(message = "In REQUIRES_NEW TransactionalInterceptor", level = "FINE")
    public static final String CDI_JTA_REQNEW = "AS-JTA-00013";

    @LogMessageInfo(message = "Managed bean with Transactional annotation and TxType of REQUIRES_NEW "
            + "called inside a transaction context.  Suspending before beginning a transaction...", level = "FINE")
    public static final String CDI_JTA_MBREQNEW = "AS-JTA-00014";

    @LogMessageInfo(message = "Managed bean with Transactional annotation and TxType of REQUIRES_NEW "
            + "encountered exception during begin {0}", level = "WARNING")
    public static final String CDI_JTA_MBREQNEWBT = "AS-JTA-00015";

    @LogMessageInfo(message = "Managed bean with Transactional annotation and TxType of REQUIRES_NEW "
            + "encountered exception during commit {0}", level = "WARNING")
    public static final String CDI_JTA_MBREQNEWCT = "AS-JTA-00016";

    @LogMessageInfo(message = "Managed bean with Transactional annotation and TxType of REQUIRED "
            + "encountered exception during resume {0}", level = "WARNING")
    public static final String CDI_JTA_MBREQNEWRT = "AS-JTA-00017";

    @LogMessageInfo(message = "In SUPPORTS TransactionalInterceptor", level = "FINE")
    public static final String CDI_JTA_SUPPORTS = "AS-JTA-00018";

    private static TransactionManager testTransactionManager;
    volatile private static TransactionManager transactionManager;
    transient private TransactionOperationsManager preexistingTransactionOperationsManager;
    private static final TransactionOperationsManager transactionalTransactionOperationsManagerTransactionMethodsAllowed = new TransactionalTransactionOperationsManagerTransactionMethodsAllowed();
    private static final TransactionOperationsManager transactionalTransactionOperationsManagerTransactionMethodsNotAllowed = new TransactionalTransactionOperationsManagerTransactionMethodsNotAllowed();

    /**
     * Must not return null
     *
     * @return TransactionManager
     */
    public TransactionManager getTransactionManager() {
        if (testTransactionManager != null) {
            return testTransactionManager;
        }

        if (transactionManager == null) {
            try {
                synchronized (TransactionalInterceptorBase.class) {
                    if (transactionManager == null) {
                        transactionManager = (TransactionManager)
                            new InitialContext().lookup("java:appserver/TransactionManager");
                    }
                }
            } catch (NamingException e) {
                _logger.log(SEVERE, CDI_JTA_NAME_EXCEPTION, e);

                throw new RuntimeException("Unable to obtain TransactionManager for Transactional Interceptor", e);
            }
        }
        return transactionManager;
    }

    static void setTestTransactionManager(TransactionManager transactionManager) {
        testTransactionManager = transactionManager;
    }

    boolean isLifeCycleMethod(InvocationContext ctx) {
        return ctx.getMethod().getAnnotation(PostConstruct.class) != null
            || ctx.getMethod().getAnnotation(PreDestroy.class) != null;
    }

    public Object proceed(InvocationContext ctx) throws Exception {
        Transactional transactionalAnnotation = getTransactionalAnnotation(ctx);

        Class<?>[] rollbackOn = transactionalAnnotation.rollbackOn();
        Class<?>[] dontRollbackOn = transactionalAnnotation.dontRollbackOn();

        Object object;

        try {
            object = ctx.proceed();
        } catch (RuntimeException runtimeException) {
            _logger.log(FINE, "Error during transaction processing", runtimeException);

            Class<?> dontRollbackOnClass = getClassInArrayClosestToClassOrNull(dontRollbackOn, runtimeException.getClass());
            if (dontRollbackOnClass == null) {
                // Proceed as default...
                markRollbackIfActiveTransaction();
                throw runtimeException;
            }

            // Spec states "if both elements are specified, dontRollbackOn takes precedence."
            if (dontRollbackOnClass.equals(runtimeException.getClass()) || dontRollbackOnClass.isAssignableFrom(runtimeException.getClass())) {
                throw runtimeException;
            }

            Class<?> rollbackOnClass = getClassInArrayClosestToClassOrNull(rollbackOn, runtimeException.getClass());
            if (rollbackOnClass != null) {

                // Both rollback and dontrollback are isAssignableFrom exception.
                // check if one isAssignableFrom the other, dontRollbackOn takes precedence if not
                if (rollbackOnClass.isAssignableFrom(dontRollbackOnClass)) {
                    throw runtimeException;
                }

                if (dontRollbackOnClass.isAssignableFrom(rollbackOnClass)) {
                    markRollbackIfActiveTransaction();
                    throw runtimeException;
                }
            }
            // This means dontRollbackOnClass is "not null" and rollbackOnClass is "null"
            // Default for un-checked exception is to mark transaction for rollback
            markRollbackIfActiveTransaction();

            throw runtimeException;

        } catch (Exception checkedException) {
            _logger.log(FINE, "Error during transaction processing", checkedException);

            Class<?> rollbackOnClass = getClassInArrayClosestToClassOrNull(rollbackOn, checkedException.getClass());
            if (rollbackOnClass == null) { // proceed as default...
                throw checkedException;
            }

            // Spec states "if both elements are specified, dontRollbackOn takes precedence."
            Class<?> dontRollbackOnClass = getClassInArrayClosestToClassOrNull(dontRollbackOn, checkedException.getClass());
            if (dontRollbackOnClass != null) {

                // Both rollback and dontrollback are isAssignableFrom exception.
                // check if one isAssignableFrom the other, dontRollbackOn takes precedence if not
                if (rollbackOnClass.isAssignableFrom(dontRollbackOnClass)) {
                    throw checkedException;
                }

                if (dontRollbackOnClass.isAssignableFrom(rollbackOnClass)) {
                    markRollbackIfActiveTransaction();
                    throw checkedException;
                }
            }

            if (rollbackOnClass.equals(checkedException.getClass()) || rollbackOnClass.isAssignableFrom(checkedException.getClass())) {
                markRollbackIfActiveTransaction();
                throw checkedException;
            }

            // This means dontRollbackOnClass is null but rollbackOnClass is "not null"
            // Default for checked exception is to "not" mark transaction for rollback
            throw checkedException;
        }

        return object;
    }

    private Transactional getTransactionalAnnotation(InvocationContext invocationContext) {

        Optional<Transactional> optionalTransactional;

        // Try the Weld bindings first. This gives us the *exact* binding which caused this interceptor being called
        @SuppressWarnings("unchecked")
        Set<Annotation> bindings = (Set<Annotation>) invocationContext.getContextData().get("org.jboss.weld.interceptor.bindings");
        if (bindings != null) {
            optionalTransactional = bindings.stream()
                    .filter(annotation -> annotation.annotationType().equals(Transactional.class))
                    .findAny()
                    .map(annotation -> Transactional.class.cast(annotation));

            if (optionalTransactional.isPresent()) {
                return optionalTransactional.get();
            }
        }

        BeanManager beanManager = CDI.current().getBeanManager();

        // Failing the Weld binding, check the method first
        optionalTransactional = getAnnotation(beanManager, invocationContext.getMethod(), Transactional.class);
        if (optionalTransactional.isPresent()) {
            return optionalTransactional.get();
        }

        // If nothing found on the method, check the the bean class
        optionalTransactional = getAnnotation(beanManager, invocationContext.getTarget().getClass(), Transactional.class);
        if (optionalTransactional.isPresent()) {
            return optionalTransactional.get();
        }

        // If still not found; throw. Since we're being called the annotation has to be there. Failing to
        // find it signals a critical error.
        throw new IllegalStateException("@Transactional not found on " + invocationContext.getTarget().getClass());
    }



    /**
     * We want the exception in the array that is closest/lowest in hierarchy to the exception So if c
     * extends b which extends a the return of getClassInArrayClosestToClassOrNull( {a,b} , c} will be b
     *
     * @param exceptionArray
     *            rollbackOn or dontRollbackOn exception array
     * @param exception
     *            actual exception thrown for comparison
     * @return exception in the array that is closest/lowest in hierarchy to the exception or null if
     *         non exists
     */
    private Class<?> getClassInArrayClosestToClassOrNull(Class<?>[] exceptionArray, Class<?> exception) {
        if (exceptionArray == null || exception == null) {
            return null;
        }

        Class<?> closestMatch = null;
        for (Class<?> exceptionArrayElement : exceptionArray) {
            if (exceptionArrayElement.equals(exception)) {
                return exceptionArrayElement;
            }

            if (exceptionArrayElement.isAssignableFrom(exception)) {
                if (closestMatch == null || closestMatch.isAssignableFrom(exceptionArrayElement)) {
                    closestMatch = exceptionArrayElement;
                }
            }
        }

        return closestMatch;
    }

    private void markRollbackIfActiveTransaction() throws SystemException {
        Transaction transaction = getTransactionManager().getTransaction();

        if (transaction != null) {
            _logger.log(Level.FINE, CDI_JTA_SETROLLBACK, transaction);
            getTransactionManager().setRollbackOnly();
        }
    }

    void setTransactionalTransactionOperationsManger(boolean userTransactionMethodsAllowed) {
        if (testTransactionManager != null) {
            return; // test
        }

        ComponentInvocation currentInvocation = getCurrentInvocation();
        if (currentInvocation == null) {
            _logger.log(WARNING, CDI_JTA_NOCOMPONENT);

            return;
        }

        preexistingTransactionOperationsManager = (TransactionOperationsManager) currentInvocation.getTransactionOperationsManager();

        currentInvocation.setTransactionOperationsManager(
                userTransactionMethodsAllowed ?
                        transactionalTransactionOperationsManagerTransactionMethodsAllowed :
                        transactionalTransactionOperationsManagerTransactionMethodsNotAllowed);
    }

    void resetTransactionOperationsManager() {
        if (testTransactionManager != null) {
            return; // test
        }

        ComponentInvocation currentInvocation = getCurrentInvocation();
        if (currentInvocation == null) {

            // There should always be a currentInvocation and so this would seem a bug
            // but not a fatal one as app should not be relying on this, so log warning only

            _logger.log(WARNING, "TransactionalInterceptorBase.markThreadAsTransactional currentInvocation==null");

            return;
        }

        currentInvocation.setTransactionOperationsManager(preexistingTransactionOperationsManager);
    }

    ComponentInvocation getCurrentInvocation() {
        ServiceLocator serviceLocator = Globals.getDefaultHabitat();
        InvocationManager invocationManager = serviceLocator == null ? null : serviceLocator.getService(InvocationManager.class);

        return invocationManager == null ? null : invocationManager.getCurrentInvocation();
    }

    private static final class TransactionalTransactionOperationsManagerTransactionMethodsAllowed implements TransactionOperationsManager {

        @Override
        public boolean userTransactionMethodsAllowed() {
            return true;
        }

        @Override
        public void userTransactionLookupAllowed() throws NameNotFoundException {
        }

        @Override
        public void doAfterUtxBegin() {
        }
    }

    private static final class TransactionalTransactionOperationsManagerTransactionMethodsNotAllowed implements TransactionOperationsManager {

        @Override
        public boolean userTransactionMethodsAllowed() {
            return false;
        }

        @Override
        public void userTransactionLookupAllowed() throws NameNotFoundException {
        }

        @Override
        public void doAfterUtxBegin() {
        }
    }
}
