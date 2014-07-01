/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2014 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jms.injection;

import java.io.Serializable;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.PreDestroy;
import javax.enterprise.inject.Instance;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.inject.Inject;
import javax.jms.ConnectionFactory;
import javax.jms.JMSConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSPasswordCredential;
import javax.jms.JMSSessionMode;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.transaction.Transaction;
import javax.transaction.SystemException;
import com.sun.appserv.connectors.internal.api.ConnectorsUtil;
import com.sun.enterprise.transaction.api.JavaEETransactionManager;
import com.sun.enterprise.util.LocalStringManagerImpl;
import org.jboss.weld.context.ContextNotActiveException;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.Globals;
import org.glassfish.logging.annotation.LoggerInfo;

/**
 * This bean is the JMSContext wrapper which user gets by injection.
 * It can read metadata of injection point for it is dependent scoped.
 * It delegates all business methods of JMSContext interface to the 
 * JMSContext API via request scopd JMSContextManager bean.
 */
public class InjectableJMSContext extends ForwardingJMSContext implements Serializable {
    // Note: since this bean is dependent-scoped, instances are liable to be passivated
    // All fields are therefore either serializable or transient

    @LoggerInfo(subsystem="JMS_INJECTION", description="JMS Injection Logger", publish=true)
    public static final String JMS_INJECTION_LOGGER = "javax.enterprise.resource.jms.injection";

    private static final Logger logger = Logger.getLogger(JMS_INJECTION_LOGGER);
    private final static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(InjectableJMSContext.class);

    private final String ipId;  // id per injection point
    private final String id;    // id per scope

    // Make it transient for FindBugs
    @Inject
    private transient Instance<TransactedJMSContextManager> tm;

    // CDI proxy so serializable
    private final RequestedJMSContextManager requestedManager;
    private TransactedJMSContextManager transactedManager;

    // We need to ensure this is serialiable
    private final JMSContextMetadata metadata;

    /*
     * We cache the ConnectionFactory here to avoid repeated JNDI lookup
     * If the bean is passivated/activated the field will be set to null 
     * and re-initialised lazily. (Though as a ConnectionFactory is required
     * to be Serializable this may not be needed)
     */
    private transient ConnectionFactory connectionFactory;
    private transient ConnectionFactory connectionFactoryPM;
    private transient JavaEETransactionManager transactionManager;

    private static final boolean usePMResourceInTransaction = Boolean.getBoolean("org.glassfish.jms.skip-resource-registration-in-transaction");

    @Inject
    public InjectableJMSContext(InjectionPoint ip, RequestedJMSContextManager rm) {
        getTransactionManager();

        JMSConnectionFactory jmsConnectionFactoryAnnot = ip.getAnnotated().getAnnotation(JMSConnectionFactory.class);
        JMSSessionMode                sessionModeAnnot = ip.getAnnotated().getAnnotation(JMSSessionMode.class);
        JMSPasswordCredential          credentialAnnot = ip.getAnnotated().getAnnotation(JMSPasswordCredential.class);

        ipId = UUID.randomUUID().toString();
        this.requestedManager = rm;
        metadata = new JMSContextMetadata(jmsConnectionFactoryAnnot, sessionModeAnnot, credentialAnnot);
        id = metadata.getFingerPrint();
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, localStrings.getLocalString("JMSContext.injection.initialization", 
                       "Injecting JMSContext wrapper with id {0} and metadata [{1}].", ipId, metadata));
        }
    }

    private synchronized TransactedJMSContextManager getTransactedManager() {
        if (transactedManager == null)
            transactedManager = tm.get();
        return transactedManager;
    }

    @Override
    protected JMSContext delegate() {
        AbstractJMSContextManager manager = requestedManager;
        boolean isInTransaction = isInTransaction();
        if (isInTransaction)
            manager = getTransactedManager();

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, localStrings.getLocalString("JMSContext.delegation.type", 
                       "JMSContext wrapper with id {0} is delegating to {1} instance.", ipId, manager.getType()));
        }
        try {
            return manager.getContext(ipId, id, metadata, getConnectionFactory(isInTransaction));
        } catch (ContextNotActiveException e) {
            String message = localStrings.getLocalString("ContextNotActiveException.msg",
                             "An injected JMSContext cannot be used when there is neither a transaction or a valid request scope.");
            throw new RuntimeException(message, e);
        }
    }

    @Override
    public String toString() {
        JMSContext rContext = null;
        JMSContext tContext = null;
        try {
            boolean isInTransaction = isInTransaction();
            if (isInTransaction) {
                TransactedJMSContextManager manager = getTransactedManager();
                tContext = manager.getContext(id);
                if (tContext == null)
                    tContext = manager.getContext(ipId, id, metadata, getConnectionFactory(isInTransaction));
            } else {
                rContext = requestedManager.getContext(id);
                if (rContext == null)
                    rContext = requestedManager.getContext(ipId, id, metadata, getConnectionFactory(isInTransaction));
            }
        } catch (ContextNotActiveException cnae) {
            // if toString() is called in an env which doesn't have valid CDI request/transaction scope,
            // then we don't call the CDI proxy for creating a new JMSContext bean.
        }

        StringBuffer sb = new StringBuffer();
        sb.append("JMSContext Wrapper ").append(ipId).append(" with metadata [").append(metadata).append("]");
        if (tContext != null)
            sb.append(", around ").append(getTransactedManager().getType()).append(" [").append(tContext).append("]");
        else if (rContext != null)
            sb.append(", around ").append(requestedManager.getType()).append(" [").append(rContext).append("]");
        else
            sb.append(", there is neither a transaction or a valid request scope.");
        return sb.toString();
    }

    @PreDestroy
    public void cleanup() {
        cleanupManager(requestedManager);
        cleanupManager(getTransactedManager());
    }

    private void cleanupManager(AbstractJMSContextManager manager) {
        try {
            manager.cleanup();
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, localStrings.getLocalString("JMSContext.injection.cleanup", 
                           "Cleaning up {0} JMSContext wrapper with id {1} and metadata [{2}].", 
                           manager.getType(), ipId, metadata.getLookup()));
            }
        } catch (ContextNotActiveException cnae) {
            // ignore the ContextNotActiveException when the application is undeployed.
        } catch (Throwable t) {
            logger.log(Level.SEVERE, localStrings.getLocalString("JMSContext.injection.cleanup.failure", 
                       "Failed to cleaning up {0} JMSContext wrapper with id {1} and metadata [{2}]. Reason: {3}.", 
                        manager.getType(), ipId, metadata.getLookup(), t.toString()));
        }
    }

    private JavaEETransactionManager getTransactionManager() {
        if (transactionManager == null) {
            ServiceLocator serviceLocator = Globals.get(ServiceLocator.class);
            if (serviceLocator != null)
                transactionManager = serviceLocator.getService(JavaEETransactionManager.class);
            if (transactionManager == null) {
                throw new RuntimeException(localStrings.getLocalString("txn.mgr.failure", "Unable to retrieve transaction manager."));
            }
        }
        return transactionManager;
    }

    private ConnectionFactory getConnectionFactory(boolean isInTransaction) {
        ConnectionFactory cachedCF = null;
        boolean usePMResource = isInTransaction && (usePMResourceInTransaction || connectionFactoryPM != null);

        if (usePMResource) {
            cachedCF = connectionFactoryPM;
        } else {
            cachedCF = connectionFactory;
        }

        if (cachedCF == null) {
            String jndiName;
            if (metadata.getLookup() == null) {
                // Use platform default connection factory
                // Java EE 7: see http://java.net/jira/browse/JAVAEE_SPEC-2
                jndiName = JMSContextMetadata.DEFAULT_CONNECTION_FACTORY;
            } else {
                jndiName = metadata.getLookup();
            }

            InitialContext initialContext = null;
            try {
                initialContext = new InitialContext();
            } catch (NamingException ne) {
                throw new RuntimeException(localStrings.getLocalString("initialContext.init.exception", 
                                           "Cannot create InitialContext."), ne);
            }

            try {

                boolean isPMName = jndiName.endsWith("__pm");
                if (isPMName) {
                    int l = jndiName.length();
                    jndiName = jndiName.substring(0, l-4);
                }
                cachedCF = (ConnectionFactory) initialContext.lookup(jndiName);

                if (isInTransaction && (usePMResourceInTransaction || isPMName)) {
                    // append __PM to jndi name to work around GLASSFISH-19872
                    // it needs double jndi lookup for __PM resource
                    jndiName = ConnectorsUtil.getPMJndiName(jndiName);
                    cachedCF = (ConnectionFactory) initialContext.lookup(jndiName);
                    usePMResource = true;
                }
            } catch (NamingException ne) {
                throw new RuntimeException(localStrings.getLocalString("connectionFactory.not.found", 
                                           "ConnectionFactory not found with lookup {0}.", 
                                           jndiName), ne);
            } finally {
                if (initialContext != null) {
                    try {
                        initialContext.close();
                    } catch (NamingException ne) {}
                }
            }

            if (usePMResource)
                connectionFactoryPM = cachedCF;
            else
                connectionFactory = cachedCF;
        }
        return cachedCF;
    }

    private boolean isInTransaction() {
        boolean isInTransaction = false;
        try {
            Transaction txn = getTransactionManager().getTransaction();
            if (txn != null)
                isInTransaction = true;
        } catch (SystemException e) {
            throw new RuntimeException(localStrings.getLocalString("txn.detection.failure", 
                                       "Failed to detect transaction status of current thread."), e);
        }
        return isInTransaction;
    }
}
