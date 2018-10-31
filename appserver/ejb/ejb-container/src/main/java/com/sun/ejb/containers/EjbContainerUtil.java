/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2012 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2016-2017] [Payara Foundation and/or its affiliates]

package com.sun.ejb.containers;

import org.glassfish.ejb.config.EjbTimerService;
import com.sun.enterprise.container.common.spi.util.JavaEEIOUtils;
import com.sun.enterprise.transaction.api.JavaEETransactionManager;
import com.sun.enterprise.container.common.spi.util.ComponentEnvManager;
import com.sun.enterprise.container.common.spi.util.InjectionManager;
import com.sun.enterprise.deployment.EjbDescriptor;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.internal.deployment.Deployment;
import org.glassfish.server.ServerEnvironmentImpl;
import com.sun.enterprise.admin.monitor.callflow.Agent;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.api.naming.GlassfishNamingManager;
import org.jvnet.hk2.annotations.Contract;

import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import javax.transaction.Transaction;
import javax.transaction.Synchronization;
import java.util.List;
import java.util.Timer;
import java.util.concurrent.ThreadPoolExecutor;
import org.glassfish.ejb.config.EjbContainer;

import org.glassfish.enterprise.iiop.api.GlassFishORBHelper;
import org.glassfish.flashlight.provider.ProbeProviderFactory;

/**
 * @author Mahesh Kannan
 *         Date: Feb 10, 2008
 */
@Contract
public interface EjbContainerUtil {

	// FIXME temporary constant for EJB Container's name - should get
	// removed once Deployment teams changes to add ContainerType are complete
	String EJB_CONTAINER_NAME = "ejb";

    String DEFAULT_THREAD_POOL_NAME = "__ejb-thread-pool";

    // Used by the TimerService upgrade
    long MINIMUM_TIMER_DELIVERY_INTERVAL = 1000;

    // Used by the TimerService upgrade
    String TIMER_SERVICE_UPGRADED = "ejb-timer-service-upgraded";

    GlassFishORBHelper getORBHelper();

    ServiceLocator getServices();

    EjbTimerService getEjbTimerService(String target);

    void registerContainer(BaseContainer container);

    void unregisterContainer(BaseContainer container);

    BaseContainer getContainer(long id);
    BaseContainer getContainer(long id, long appUniqueId);

    EjbDescriptor getDescriptor(long id);

    ClassLoader getClassLoader(long id);

    Timer getTimer();

    void setInsideContainer(boolean bool);

    boolean isInsideContainer();

    InvocationManager getInvocationManager();

    InjectionManager getInjectionManager();

    GlassfishNamingManager getGlassfishNamingManager();

    ComponentEnvManager getComponentEnvManager();

    ComponentInvocation getCurrentInvocation();

    JavaEETransactionManager getTransactionManager();

    ServerContext getServerContext();

    ContainerSynchronization getContainerSync(Transaction jtx)
        throws RollbackException, SystemException;

    void removeContainerSync(Transaction tx);

    void registerPMSync(Transaction jtx, Synchronization sync)
        throws RollbackException, SystemException;

    EjbContainer getEjbContainer();

    ServerEnvironmentImpl getServerEnvironment();

    Agent getCallFlowAgent();

    List getBeans(Transaction jtx);

    Object getActiveTxCache(Transaction jtx);

    void setActiveTxCache(Transaction jtx, Object cache);

    void addWork(Runnable task);

    EjbDescriptor ejbIdToDescriptor(long ejbId);

    boolean isEJBLite();

    boolean isEmbeddedServer();

    ProbeProviderFactory getProbeProviderFactory();

    boolean isDas();

    ThreadPoolExecutor getThreadPoolExecutor(String poolName);

    JavaEEIOUtils getJavaEEIOUtils();

    Deployment getDeployment();
}
