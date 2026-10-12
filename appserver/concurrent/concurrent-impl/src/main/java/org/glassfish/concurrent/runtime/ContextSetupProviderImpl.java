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
// Portions Copyright [2016-2026] [Payara Foundation and/or its affiliates]

package org.glassfish.concurrent.runtime;

import com.sun.enterprise.deployment.JndiNameEnvironment;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.security.SecurityContext;
import com.sun.enterprise.util.Utility;


import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.concurrent.LogFacade;
import org.glassfish.concurro.spi.ContextHandle;
import org.glassfish.concurro.spi.ContextSetupProvider;

import jakarta.enterprise.concurrent.ContextService;
import jakarta.enterprise.concurrent.ManagedTask;
import jakarta.enterprise.concurrent.spi.ThreadContextProvider;
import jakarta.enterprise.concurrent.spi.ThreadContextRestorer;
import jakarta.enterprise.concurrent.spi.ThreadContextSnapshot;
import java.io.IOException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import jakarta.enterprise.concurrent.ContextServiceDefinition;

public class ContextSetupProviderImpl implements ContextSetupProvider {


    private static final Logger logger  = LogFacade.getLogger();

    /** Bumped when the serialized form changes; same-JVM same-version round-trips only. */
    static final long serialVersionUID = 1L;

    // Predefined handlers for context propagation
    // TODO: replace with ConcurrentRuntime.CONTEXT_INFO_* ?
    public static final String CONTEXT_TYPE_CLASSLOADING = "CLASSLOADING"; // Concurrency 3.0: N/A
    public static final String CONTEXT_TYPE_SECURITY = "SECURITY"; // Concurrency 3.0: SECURITY
    public static final String CONTEXT_TYPE_NAMING = "NAMING"; // Concurrency 3.0: APPLICATION
    public static final String CONTEXT_TYPE_WORKAREA = "WORKAREA"; // Concurrency 3.0: TRANSACTION


    // TODO: do we need these booleans if we have sets?
    private boolean classloading, security, naming, workArea;
    private final Set<String> contextPropagate;
    private final Set<String> contextClear;
    private final Set<String> contextUnchanged;
    private Map<String, ThreadContextProvider> allThreadContextProviders = null;
    /**
     * Points to the context, which contains ALL_REMAINING.
     */
    private final Set<String> allRemaining;
    private transient InvocationFacade invocationFacade;
    private transient MonitoringFacade monitoringFacade;
    /** JNDI name of the owning executor/context-service; used as the OTel pool name. */
    private final String poolName;
    /**
     * Built-in OTel waiting-span provider — always active, not user-configurable.
     * Transient because {@link MonitoringFacade} is not serializable; restored in
     * {@link #readObject} from {@link ConcurrentRuntime} after passivation round-trip.
     */
    private transient OtelContextProvider otelContextProvider;

    public ContextSetupProviderImpl(InvocationFacade invocationFacade,
            MonitoringFacade monitoringFacade,
            String poolName,
            Set<String> propagated,
            Set<String> cleared,
            Set<String> unchanged) {
        this.invocationFacade = invocationFacade;
        this.monitoringFacade = monitoringFacade;
        this.poolName = poolName;
        this.otelContextProvider = new OtelContextProvider(monitoringFacade, poolName);

        contextPropagate = new HashSet<>(propagated);
        contextClear = new HashSet<>(cleared);
        contextUnchanged = new HashSet<>(unchanged);

        // process ALL_REMAINING
        if (contextPropagate.contains(ContextServiceDefinition.ALL_REMAINING)) {
            allRemaining = contextPropagate;
        } else if (contextClear.contains(ContextServiceDefinition.ALL_REMAINING)) {
            allRemaining = contextClear;
        } else if (contextUnchanged.contains(ContextServiceDefinition.ALL_REMAINING)) {
            allRemaining = contextUnchanged;
        } else {
            allRemaining = contextPropagate; // By default, propagate contexts
        }

        // put standard "providers" to Remaining if not specified
        addToRemainingIfNotPresent(CONTEXT_TYPE_CLASSLOADING);
        addToRemainingIfNotPresent(CONTEXT_TYPE_SECURITY);
        addToRemainingIfNotPresent(CONTEXT_TYPE_NAMING);
        addToRemainingIfNotPresent(CONTEXT_TYPE_WORKAREA);

        for (String contextType : contextPropagate) {
            switch (contextType) {
                case CONTEXT_TYPE_CLASSLOADING:
                    classloading = true;
                    break;
                case CONTEXT_TYPE_SECURITY:
                    security = true;
                    break;
                case CONTEXT_TYPE_NAMING:
                    naming = true;
                    break;
                case CONTEXT_TYPE_WORKAREA:
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

        // TODO: put initialization of providers to better place; caching is a problem due to different classloaders
        allThreadContextProviders = new HashMap<>();
        for (ThreadContextProvider service : ServiceLoader.load(jakarta.enterprise.concurrent.spi.ThreadContextProvider.class, Utility.getClassLoader())) {
            String serviceName = service.getThreadContextType();
            if (contextPropagate.contains(serviceName) || contextClear.contains(serviceName) || contextUnchanged.contains(serviceName)) {
                allThreadContextProviders.put(serviceName, service);
            } else {
                if (allRemaining != null) {
                    allRemaining.add(serviceName);
                    allThreadContextProviders.put(serviceName, service);
                }
            }
        }
        // check, if there is no unexpected provider name
        Set<String> verifiedContextPropagate = filterVerifiedProviders(contextPropagate);
        Set<String> verifiedContextClear = filterVerifiedProviders(contextClear);
        Set<String> verifiedContextUnchanged = filterVerifiedProviders(contextUnchanged);

        ComponentInvocation currentInvocation = invocationFacade.getCurrentInvocation();
        if (currentInvocation != null) {
            if (verifiedContextPropagate.contains(CONTEXT_TYPE_NAMING)) {
                savedInvocation = createComponentInvocation(currentInvocation);
            }
            if (verifiedContextClear.contains(CONTEXT_TYPE_NAMING)) {
                savedInvocation = new ComponentInvocation();
            }
        }
        boolean useTransactionOfExecutionThread = (!invocationFacade.isContextService() && useTransactionOfExecutionThread(contextObjectProperties))
                || verifiedContextUnchanged.contains(CONTEXT_TYPE_WORKAREA);

        // store the snapshots of the current state
        List<ThreadContextSnapshot> threadContextSnapshots = new ArrayList<>();
        // remember values from propagate and clear lists
        verifiedContextPropagate.stream()
                .map((provider) -> allThreadContextProviders.get(provider))
                .filter(snapshot -> snapshot != null) // ignore standard providers like CONTEXT_TYPE_CLASSLOADING
                .map(snapshot -> snapshot.currentContext(contextObjectProperties))
                .forEach(snapshot -> threadContextSnapshots.add(snapshot));
        verifiedContextClear.stream()
                .map((provider) -> allThreadContextProviders.get(provider))
                .filter(snapshot -> snapshot != null)
                .map(snapshot -> snapshot.clearedContext(contextObjectProperties))
                .forEach(snapshot -> threadContextSnapshots.add(snapshot));

        // Built-in OTel waiting-span context: always captured at submit time,
        // regardless of the executor's context-type propagation configuration.
        threadContextSnapshots.add(otelContextProvider.currentContext(contextObjectProperties));

        return new InvocationContext(savedInvocation, contextClassloader, currentSecurityContext, useTransactionOfExecutionThread,
                threadContextSnapshots, Collections.EMPTY_LIST);
    }

    @Override
    public ContextHandle setup(ContextHandle contextHandle) throws IllegalStateException {
        if (! (contextHandle instanceof InvocationContext)) {
            logger.log(Level.SEVERE, LogFacade.UNKNOWN_CONTEXT_HANDLE);
            return null;
        }
        InvocationContext handle = (InvocationContext) contextHandle;
        String appName = null;

        ComponentInvocation invocation = handle.getInvocation();
        ClassLoader backupClassLoader = null;
        if (invocation != null) {
            appName = invocation.getRegistrationName();
            if (appName == null && invocation.getJNDIEnvironment() != null) {
                appName = DOLUtils.getApplicationFromEnv((JndiNameEnvironment) invocation.getJNDIEnvironment()).getRegistrationName();
            }
            if (appName == null) {
                // try to get environment from component ID
                if (invocation.getComponentId() != null) {
                    JndiNameEnvironment currJndiEnv = invocationFacade.getJndiNameEnvironment(invocation.getComponentId());
                    if (currJndiEnv != null) {
                        com.sun.enterprise.deployment.Application appInfo = DOLUtils.getApplicationFromEnv(currJndiEnv);
                        if (appInfo != null) {
                            appName = appInfo.getRegistrationName();
                            // cache JNDI environment
                            invocation.setJNDIEnvironment(currJndiEnv);
                            backupClassLoader = appInfo.getClassLoader();
                        }
                    }
                }
            }
        }

        // Check whether the application component submitting the task is still running. Throw IllegalStateException if not.
        if (appName != null && !invocationFacade.isApplicationEnabled(appName)) { // appName == null in case of the server context
            throw new IllegalStateException("Module " + appName + " is disabled");
        }

        ClassLoader resetClassLoader = null;
        if (handle.getContextClassLoader() != null) {
            resetClassLoader = Utility.setContextClassLoader(handle.getContextClassLoader());
        } else if (backupClassLoader != null) {
            resetClassLoader = Utility.setContextClassLoader(backupClassLoader);
        }

        SecurityContext resetSecurityContext = null;
        if (handle.getSecurityContext() != null && !contextUnchanged.contains(CONTEXT_TYPE_SECURITY)) {
            resetSecurityContext = SecurityContext.getCurrent();
            SecurityContext.setCurrent(handle.getSecurityContext());
        }

        if (invocation != null) {
            // Each invocation needs a ResourceTableKey that returns a unique hashCode for TransactionManager
            invocation.setResourceTableKey(new PairKey(invocation.getInstance(), Thread.currentThread()));
            invocationFacade.preInvoke(invocation);
        }
        // Ensure that there is no existing transaction in the current thread
        if (contextClear.contains(CONTEXT_TYPE_WORKAREA)) {
            invocationFacade.cleanupTransaction(false);
        }

        // execute thread contexts snapshots to begin
        List<ThreadContextRestorer> restorers = Collections.EMPTY_LIST;
        if (handle.getThreadContextSnapshots() != null) {
            restorers = handle.getThreadContextSnapshots().stream()
                    .map((ThreadContextSnapshot snapshot) -> snapshot.begin())
                    .collect(Collectors.toList());
        }

        InvocationContext restorerHandle = new InvocationContext(invocation, resetClassLoader, resetSecurityContext, handle.isUseTransactionOfExecutionThread(),
                Collections.EMPTY_LIST, restorers);

        monitoringFacade.registerStuckThread(Thread.currentThread().threadId());

        return restorerHandle;
    }

    @Override
    public void reset(ContextHandle contextHandle) {
        if (! (contextHandle instanceof InvocationContext)) {
            logger.log(Level.SEVERE, LogFacade.UNKNOWN_CONTEXT_HANDLE);
            return;
        }
        InvocationContext handle = (InvocationContext) contextHandle;

        // execute thread contexts restorers to end
        for (ThreadContextRestorer restorer : handle.getThreadContextRestorers()) {
            restorer.endContext();
        }

        Utility.setContextClassLoader(handle.getContextClassLoader());
        if (handle.getSecurityContext() != null) {
            SecurityContext.setCurrent(handle.getSecurityContext());
        }
        if (handle.getInvocation() != null) {
            invocationFacade.postInvoke(handle.getInvocation());
        }
        if (contextClear.contains(CONTEXT_TYPE_WORKAREA)) {
            invocationFacade.cleanupTransaction(true);
        }

        if (monitoringFacade.isRequestTracingEnabled()) {
            monitoringFacade.endTrace();
        }
        monitoringFacade.deregisterStuckThread(Thread.currentThread().threadId());
    }

    private ComponentInvocation createComponentInvocation(ComponentInvocation currInv) {
        ComponentInvocation newInv = currInv.clone();
        newInv.setResourceTableKey(null);
        newInv.clearRegistry();
        newInv.instance = currInv.getInstance();
        if (!naming) {
            newInv.setJNDIEnvironment(null);
        }
        return newInv;
    }

    private boolean useTransactionOfExecutionThread(Map<String, String> executionProperties) {
        return ManagedTask.USE_TRANSACTION_OF_EXECUTION_THREAD.equals(getTransactionExecutionProperty(executionProperties));
    }

    private String getTransactionExecutionProperty(Map<String, String> executionProperties) {
        if (executionProperties != null && executionProperties.get(ManagedTask.TRANSACTION) != null) {
            return executionProperties.get(ManagedTask.TRANSACTION);
        }
        return ManagedTask.SUSPEND;
    }

    private Set<String> filterVerifiedProviders(Set<String> providers) {
        HashSet<String> filtered = new HashSet<>();
        for (String provider : providers) {
            switch (provider) {
                case CONTEXT_TYPE_CLASSLOADING:
                case CONTEXT_TYPE_SECURITY:
                case CONTEXT_TYPE_NAMING:
                case CONTEXT_TYPE_WORKAREA:
                case ContextServiceDefinition.ALL_REMAINING:
                    filtered.add(provider);
                    break;
                default:
                    if (allThreadContextProviders.containsKey(provider)) {
                        filtered.add(provider);
                    } else {
                        logger.log(Level.SEVERE, "Thread context provider ''{0}'' is not registered in WEB-APP/services/jakarta.enterprise.concurrent.spi.ThreadContextProvider and will be ignored!", provider);
                    }
                    break;
            }
        }
         return filtered;
    }

    private void addToRemainingIfNotPresent(String contextType) {
        if (!(contextPropagate.contains(contextType) || contextClear.contains(contextType) || contextUnchanged.contains(contextType))) {
            // such context type is not present in any context
            allRemaining.add(contextType);
        }
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        // Re-initialize transient facade fields from the runtime singleton.
        ConcurrentRuntime concurrentRuntime = ConcurrentRuntime.getRuntime();
        invocationFacade     = concurrentRuntime.getInvocationFacade();
        monitoringFacade     = concurrentRuntime.getMonitoringFacade();
        otelContextProvider  = new OtelContextProvider(monitoringFacade, poolName);
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
                PairKey p = (PairKey)obj;
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

