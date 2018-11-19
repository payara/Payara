/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2016-2018] [Payara Foundation and/or its affiliates]

package org.glassfish.concurrent.runtime;

import com.sun.enterprise.config.serverbeans.Application;
import com.sun.enterprise.config.serverbeans.Applications;
import com.sun.enterprise.container.common.spi.util.ComponentEnvManager;
import com.sun.enterprise.deployment.JndiNameEnvironment;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.security.SecurityContext;
import com.sun.enterprise.transaction.api.JavaEETransactionManager;
import com.sun.enterprise.util.Utility;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.concurrent.LogFacade;
import org.glassfish.enterprise.concurrent.spi.ContextHandle;
import org.glassfish.enterprise.concurrent.spi.ContextSetupProvider;
import org.glassfish.internal.deployment.Deployment;

import javax.enterprise.concurrent.ContextService;
import javax.enterprise.concurrent.ManagedTask;
import javax.transaction.*;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import fish.payara.nucleus.requesttracing.RequestTracingService;
import fish.payara.nucleus.healthcheck.stuck.StuckThreadsStore;
import fish.payara.opentracing.propagation.MapToTextMap;
import fish.payara.notification.requesttracing.RequestTraceSpanContext;
import fish.payara.opentracing.OpenTracingService;
import io.opentracing.SpanContext;
import io.opentracing.Tracer;
import io.opentracing.Tracer.SpanBuilder;
import io.opentracing.propagation.Format;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.data.ApplicationRegistry;

public class ContextSetupProviderImpl implements ContextSetupProvider {

    private transient InvocationManager invocationManager;
    private transient Deployment deployment;
    private transient ComponentEnvManager compEnvMgr;
    private transient ApplicationRegistry applicationRegistry;
    private transient Applications applications;
    // transactionManager should be null for ContextService since it uses TransactionSetupProviderImpl
    private transient JavaEETransactionManager transactionManager;

    private static final Logger logger = LogFacade.getLogger();

    static final long serialVersionUID = -1095988075917755802L;

    static enum CONTEXT_TYPE {
        CLASSLOADING, SECURITY, NAMING, WORKAREA
    }

    private boolean classloading, security, naming, workArea;

    private RequestTracingService requestTracing;
    private OpenTracingService openTracing;
    private StuckThreadsStore stuckThreads;

    public ContextSetupProviderImpl(InvocationManager invocationManager,
            Deployment deployment,
            ComponentEnvManager compEnvMgr,
            ApplicationRegistry applicationRegistry,
            Applications applications,
            JavaEETransactionManager transactionManager,
            CONTEXT_TYPE... contextTypes) {
        this.invocationManager = invocationManager;
        this.deployment = deployment;
        this.compEnvMgr = compEnvMgr;
        this.applicationRegistry = applicationRegistry;
        this.applications = applications;
        this.transactionManager = transactionManager;

        try {
            this.requestTracing = Globals.getDefaultHabitat().getService(RequestTracingService.class);
        } catch (NullPointerException ex) {
            logger.log(Level.INFO, "Error retrieving Request Tracing service "
                    + "during initialisation of Concurrent Context - NullPointerException");
        }
        try {
            this.stuckThreads = Globals.getDefaultHabitat().getService(StuckThreadsStore.class);
        } catch (NullPointerException ex) {
            logger.log(Level.INFO, "Error retrieving Stuck Threads Sore Healthcheck service "
                    + "during initialisation of Concurrent Context - NullPointerException");
        }
        try {
            this.openTracing = Globals.getDefaultHabitat().getService(OpenTracingService.class);
        } catch (NullPointerException ex) {
            logger.log(Level.INFO, "Error retrieving OpenTracing service "
                    + "during initialisation of Concurrent Context - NullPointerException");
        }
        
        for (CONTEXT_TYPE contextType : contextTypes) {
            switch (contextType) {
                case CLASSLOADING:
                    classloading = true;
                    break;
                case SECURITY:
                    security = true;
                    break;
                case NAMING:
                    naming = true;
                    break;
                case WORKAREA:;
                    workArea = true;
                    break;
            }
        }
    }

    @Override
    public ContextHandle saveContext(ContextService contextService) {
        return saveContext(contextService, null);
    }

    @Override
    public ContextHandle saveContext(ContextService contextService, Map<String, String> contextObjectProperties) {
        // Capture the current thread context
        ClassLoader contextClassloader = null;
        SecurityContext currentSecurityContext = null;
        ComponentInvocation savedInvocation = null;
        if (classloading) {
            contextClassloader = Utility.getClassLoader();
        }
        if (security) {
            currentSecurityContext = SecurityContext.getCurrent();
        }
        ComponentInvocation currentInvocation = invocationManager.getCurrentInvocation();
        if (currentInvocation != null) {
            savedInvocation = createComponentInvocation(currentInvocation);
        }
        boolean useTransactionOfExecutionThread = transactionManager == null && useTransactionOfExecutionThread(contextObjectProperties);
        // TODO - support workarea propagation
        return new InvocationContext(savedInvocation, contextClassloader, currentSecurityContext, useTransactionOfExecutionThread);
    }

    @Override
    public ContextHandle setup(ContextHandle contextHandle) throws IllegalStateException {
        if (!(contextHandle instanceof InvocationContext)) {
            logger.log(Level.SEVERE, LogFacade.UNKNOWN_CONTEXT_HANDLE);
            return null;
        }
        InvocationContext handle = (InvocationContext) contextHandle;
        String appName = null;

        ClassLoader backupClassLoader = null;
        if (handle.getInvocation() != null) {
            appName = handle.getInvocation().getAppName();
            if (appName == null && handle.getInvocation().getJNDIEnvironment() != null) {
                appName = DOLUtils.getApplicationFromEnv((JndiNameEnvironment) handle.getInvocation().getJNDIEnvironment()).getName();
            }
            if (appName == null) {
                // try to get environment from component ID
                if (handle.getInvocation().getComponentId() != null && compEnvMgr != null) {
                    JndiNameEnvironment currJndiEnv = compEnvMgr.getJndiNameEnvironment(handle.getInvocation().getComponentId());
                    if (currJndiEnv != null) {
                        com.sun.enterprise.deployment.Application appInfo = DOLUtils.getApplicationFromEnv(currJndiEnv);
                        if (appInfo != null) {
                            appName = appInfo.getName();
                            // cache JNDI environment
                            handle.getInvocation().setJNDIEnvironment(currJndiEnv);
                            backupClassLoader = appInfo.getClassLoader();
                        }
                    }
                }
            }
        }

        // Check whether the application component submitting the task is still running. Throw IllegalStateException if not.
        if (appName != null && !isApplicationEnabled(appName)) { // appName == null in case of the server context
            throw new IllegalStateException("Module " + appName + " is disabled");
        }

        ClassLoader resetClassLoader = null;
        SecurityContext resetSecurityContext = null;
        if (handle.getContextClassLoader() != null) {
            resetClassLoader = Utility.setContextClassLoader(handle.getContextClassLoader());
        }
        else if(backupClassLoader != null) {
            resetClassLoader = Utility.setContextClassLoader(backupClassLoader);
        }
        if (handle.getSecurityContext() != null) {
            resetSecurityContext = SecurityContext.getCurrent();
            SecurityContext.setCurrent(handle.getSecurityContext());
        }
        ComponentInvocation invocation = handle.getInvocation();
        if (invocation != null && !handle.isUseTransactionOfExecutionThread()) {
            // Each invocation needs a ResourceTableKey that returns a unique hashCode for TransactionManager
            invocation.setResourceTableKey(new PairKey(invocation.getInstance(), Thread.currentThread()));
            invocationManager.preInvoke(invocation);
        }
        // Ensure that there is no existing transaction in the current thread
        if (transactionManager != null) {
            transactionManager.clearThreadTx();
        }

        if (requestTracing != null && requestTracing.isRequestTracingEnabled()) {
            startConcurrentContextSpan(invocation, handle);
        }

        if (stuckThreads != null) {
            stuckThreads.registerThread(Thread.currentThread().getId());
        }

        return new InvocationContext(invocation, resetClassLoader, resetSecurityContext, handle.isUseTransactionOfExecutionThread());
    }
    
    private void startConcurrentContextSpan(ComponentInvocation invocation, InvocationContext handle) {
        Tracer tracer = openTracing.getTracer(openTracing.getApplicationName(
                Globals.getDefaultBaseServiceLocator().getService(InvocationManager.class)));
        
        SpanContext spanContext = tracer.extract(Format.Builtin.TEXT_MAP, new MapToTextMap(handle.getSpanContextMap()));
        
        if (spanContext != null) {
            // Start a trace in the request tracing system
            SpanBuilder builder = tracer.buildSpan("executeConcurrentContext")
                    .asChildOf(spanContext)
                    .withTag("App Name", invocation.getAppName())
                    .withTag("Component ID", invocation.getComponentId())
                    .withTag("Module Name", invocation.getModuleName());

            Object instance = invocation.getInstance();
            if (instance != null) {
                builder.withTag("Class Name", instance.getClass().getName());
            }
            
            builder.withTag("Thread Name", Thread.currentThread().getName());
            
            // Check for the presence of a propagated parent operation name
            try {
                String operationName = ((RequestTraceSpanContext) spanContext).getBaggageItems().get("operation.name");
                if (operationName != null) {
                    builder.withTag("Parent Operation Name", operationName);
                }
            } catch (ClassCastException cce) {
                logger.log(Level.FINE, "ClassCastException caught converting Span Context", cce);
            }
            
            builder.startActive(true);
        }
    }

    @Override
    public void reset(ContextHandle contextHandle) {
        if (!(contextHandle instanceof InvocationContext)) {
            logger.log(Level.SEVERE, LogFacade.UNKNOWN_CONTEXT_HANDLE);
            return;
        }
        InvocationContext handle = (InvocationContext) contextHandle;
        if (handle.getContextClassLoader() != null) {
            Utility.setContextClassLoader(handle.getContextClassLoader());
        }
        if (handle.getSecurityContext() != null) {
            SecurityContext.setCurrent(handle.getSecurityContext());
        }
        if (handle.getInvocation() != null && !handle.isUseTransactionOfExecutionThread()) {
            invocationManager.postInvoke(((InvocationContext) contextHandle).getInvocation());
        }
        if (transactionManager != null) {
            // clean up after user if a transaction is still active
            // This is not required by the Concurrency spec
            Transaction transaction = transactionManager.getCurrentTransaction();
            if (transaction != null) {
                try {
                    int status = transaction.getStatus();
                    if (status == Status.STATUS_ACTIVE) {
                        transactionManager.commit();
                    } else if (status == Status.STATUS_MARKED_ROLLBACK) {
                        transactionManager.rollback();
                    }
                } catch (Exception ex) {
                    Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, ex.toString());
                }
            }
            transactionManager.clearThreadTx();
        }

        if (requestTracing != null && requestTracing.isRequestTracingEnabled()) {
            requestTracing.endTrace();
        }
        if (stuckThreads != null) {
            stuckThreads.deregisterThread(Thread.currentThread().getId());
        }
    }

    private boolean isApplicationEnabled(String appId) {
        boolean result = false;
        if (appId != null) {
            Application app = applications.getApplication(appId);
            if (app != null) {
                result = deployment.isAppEnabled(app);
            } else {
                // if app is null then it is likely that appId is still deploying
                // and its enabled status has not been written to the domain.xml yet
                // this can happen for example with a Startup EJB submitting something
                // it its startup method. Reference Payara GitHub issue 204
                if (applicationRegistry.get(appId) != null) {
                    logger.log(Level.INFO, "Job submitted for {0} likely during deployment. Continuing...", appId);
                    result = true;
                }
            }
        }
        return result;
    }

    private ComponentInvocation createComponentInvocation(ComponentInvocation currInv) {
//        ComponentInvocation newInv = new ComponentInvocation(
//                currInv.getComponentId(),
//                ComponentInvocation.ComponentInvocationType.SERVLET_INVOCATION,
//                currInv.getContainer(),
//                currInv.getAppName(),
//                currInv.getModuleName()
//        );
        ComponentInvocation newInv = currInv.clone();
        newInv.setResourceTableKey(null);
        newInv.instance = currInv.getInstance();
//        if (naming) {
//            newInv.setJNDIEnvironment(currInv.getJNDIEnvironment());
//        }
        if (!naming) {
            newInv.setJNDIEnvironment(null);
        }
        return newInv;
    }

    private boolean useTransactionOfExecutionThread(Map<String, String> executionProperties) {
        return (ManagedTask.USE_TRANSACTION_OF_EXECUTION_THREAD.equals(getTransactionExecutionProperty(executionProperties)));
    }

    private String getTransactionExecutionProperty(Map<String, String> executionProperties) {
        if (executionProperties != null && executionProperties.get(ManagedTask.TRANSACTION) != null) {
            return executionProperties.get(ManagedTask.TRANSACTION);
        }
        return ManagedTask.SUSPEND;
    }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.defaultWriteObject();
        out.writeBoolean(transactionManager == null);
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        boolean nullTransactionManager = in.readBoolean();
        ConcurrentRuntime concurrentRuntime = ConcurrentRuntime.getRuntime();
        // re-initialize these fields
        invocationManager = concurrentRuntime.getInvocationManager();
        deployment = concurrentRuntime.getDeployment();
        compEnvMgr = concurrentRuntime.getCompEnvMgr();
        applications = concurrentRuntime.getApplications();
        if (!nullTransactionManager) {
            transactionManager = concurrentRuntime.getTransactionManager();
        }
    }

    private static class PairKey {

        private Object instance = null;
        private Thread thread = null;
        int hCode = 0;

        private PairKey(Object inst, Thread thr) {
            instance = inst;
            thread = thr;
            if (inst != null) {
                hCode = 7 * inst.hashCode();
            }
            if (thr != null) {
                hCode += thr.hashCode();
            }
        }

        @Override
        public int hashCode() {
            return hCode;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            }

            boolean eq = false;
            if (obj != null && obj instanceof PairKey) {
                PairKey p = (PairKey) obj;
                if (instance != null) {
                    eq = (instance.equals(p.instance));
                } else {
                    eq = (p.instance == null);
                }

                if (eq) {
                    if (thread != null) {
                        eq = (thread.equals(p.thread));
                    } else {
                        eq = (p.thread == null);
                    }
                }
            }
            return eq;
        }
    }
}
