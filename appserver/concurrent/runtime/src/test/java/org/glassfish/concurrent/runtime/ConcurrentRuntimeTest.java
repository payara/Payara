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

package org.glassfish.concurrent.runtime;

import org.easymock.EasyMock;
import org.glassfish.concurrent.runtime.deployer.ContextServiceConfig;
import org.glassfish.concurrent.runtime.deployer.ManagedExecutorServiceConfig;
import org.glassfish.concurrent.runtime.deployer.ManagedThreadFactoryConfig;
import org.glassfish.enterprise.concurrent.ContextServiceImpl;
import org.glassfish.enterprise.concurrent.ManagedExecutorServiceImpl;
import org.glassfish.enterprise.concurrent.ManagedThreadFactoryImpl;
import org.glassfish.enterprise.concurrent.internal.ManagedThreadPoolExecutor;
import org.glassfish.resourcebase.resources.api.ResourceInfo;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.util.concurrent.TimeUnit;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.*;

public class ConcurrentRuntimeTest {

    private ContextServiceConfig contextServiceConfig;
    private ManagedThreadFactoryConfig managedThreadFactoryConfig;
    private ManagedExecutorServiceConfig managedExecutorServiceConfig;

    @Before
    public void before() {
        contextServiceConfig = createMock(ContextServiceConfig.class);
        managedThreadFactoryConfig = createMock(ManagedThreadFactoryConfig.class);
        managedExecutorServiceConfig = createMock(ManagedExecutorServiceConfig.class);
    }

    @Test
    public void testParseContextInfo() throws Exception {
        expect(contextServiceConfig.getJndiName()).andReturn("concurrent/ctxSrv").anyTimes();
        expect(contextServiceConfig.getContextInfo()).andReturn("Classloader, JNDI, Security, WorkArea").anyTimes();
        expect(contextServiceConfig.getContextInfoEnabled()).andReturn("true");
        replay(contextServiceConfig);

        ConcurrentRuntime concurrentRuntime = new ConcurrentRuntime();

        ResourceInfo resource = new ResourceInfo("test");
        ContextServiceImpl contextService = concurrentRuntime.getContextService(resource, contextServiceConfig);
        ContextSetupProviderImpl contextSetupProvider = (ContextSetupProviderImpl) contextService.getContextSetupProvider();
        assertTrue((Boolean) Util.getdFieldValue(contextSetupProvider, "classloading"));
        assertTrue((Boolean) Util.getdFieldValue(contextSetupProvider, "naming"));
        assertTrue((Boolean) Util.getdFieldValue(contextSetupProvider, "security"));
        assertTrue((Boolean) Util.getdFieldValue(contextSetupProvider, "workArea"));
    }

    @Test
    public void testParseContextInfo_lowerCase() throws Exception {
        expect(contextServiceConfig.getJndiName()).andReturn("concurrent/ctxSrv").anyTimes();
        expect(contextServiceConfig.getContextInfo()).andReturn("classloader, jndi, security, workarea").anyTimes();
        expect(contextServiceConfig.getContextInfoEnabled()).andReturn("true");
        replay(contextServiceConfig);

        ConcurrentRuntime concurrentRuntime = new ConcurrentRuntime();

        ResourceInfo resource = new ResourceInfo("test");
        ContextServiceImpl contextService = concurrentRuntime.getContextService(resource, contextServiceConfig);
        ContextSetupProviderImpl contextSetupProvider = (ContextSetupProviderImpl) contextService.getContextSetupProvider();
        assertTrue((Boolean) Util.getdFieldValue(contextSetupProvider, "classloading"));
        assertTrue((Boolean) Util.getdFieldValue(contextSetupProvider, "naming"));
        assertTrue((Boolean) Util.getdFieldValue(contextSetupProvider, "security"));
        assertTrue((Boolean) Util.getdFieldValue(contextSetupProvider, "workArea"));
    }

    @Test
    public void testParseContextInfo_upperCase() throws Exception {
        expect(contextServiceConfig.getJndiName()).andReturn("concurrent/ctxSrv").anyTimes();
        expect(contextServiceConfig.getContextInfo()).andReturn("CLASSLOADER, JNDI, SECURITY, WORKAREA").anyTimes();
        expect(contextServiceConfig.getContextInfoEnabled()).andReturn("true");
        replay(contextServiceConfig);

        ConcurrentRuntime concurrentRuntime = new ConcurrentRuntime();

        ResourceInfo resource = new ResourceInfo("test");
        ContextServiceImpl contextService = concurrentRuntime.getContextService(resource, contextServiceConfig);
        ContextSetupProviderImpl contextSetupProvider = (ContextSetupProviderImpl) contextService.getContextSetupProvider();
        assertTrue((Boolean) Util.getdFieldValue(contextSetupProvider, "classloading"));
        assertTrue((Boolean) Util.getdFieldValue(contextSetupProvider, "naming"));
        assertTrue((Boolean) Util.getdFieldValue(contextSetupProvider, "security"));
        assertTrue((Boolean) Util.getdFieldValue(contextSetupProvider, "workArea"));
    }

    @Test
    public void testParseContextInfo_disabled() throws Exception {
        expect(contextServiceConfig.getJndiName()).andReturn("concurrent/ctxSrv").anyTimes();
        expect(contextServiceConfig.getContextInfo()).andReturn("Classloader, JNDI, Security, WorkArea").anyTimes();
        expect(contextServiceConfig.getContextInfoEnabled()).andReturn("false");
        replay(contextServiceConfig);

        ConcurrentRuntime concurrentRuntime = new ConcurrentRuntime();

        ResourceInfo resource = new ResourceInfo("test");
        ContextServiceImpl contextService = concurrentRuntime.getContextService(resource, contextServiceConfig);
        ContextSetupProviderImpl contextSetupProvider = (ContextSetupProviderImpl) contextService.getContextSetupProvider();
        assertFalse((Boolean) Util.getdFieldValue(contextSetupProvider, "classloading"));
        assertFalse((Boolean) Util.getdFieldValue(contextSetupProvider, "naming"));
        assertFalse((Boolean) Util.getdFieldValue(contextSetupProvider, "security"));
        assertFalse((Boolean) Util.getdFieldValue(contextSetupProvider, "workArea"));
    }

    @Test
    public void testParseContextInfo_invalid() throws Exception {
        expect(contextServiceConfig.getJndiName()).andReturn("concurrent/ctxSrv").anyTimes();
        expect(contextServiceConfig.getContextInfo()).andReturn("JNDI, blah, beh, JNDI, WorkArea, WorkArea, ").anyTimes();
        expect(contextServiceConfig.getContextInfoEnabled()).andReturn("true");
        replay(contextServiceConfig);

        ConcurrentRuntime concurrentRuntime = new ConcurrentRuntime();

        ResourceInfo resource = new ResourceInfo("test");
        ContextServiceImpl contextService = concurrentRuntime.getContextService(resource, contextServiceConfig);
        ContextSetupProviderImpl contextSetupProvider = (ContextSetupProviderImpl) contextService.getContextSetupProvider();
        assertFalse((Boolean) Util.getdFieldValue(contextSetupProvider, "classloading"));
        assertTrue((Boolean) Util.getdFieldValue(contextSetupProvider, "naming"));
        assertFalse((Boolean) Util.getdFieldValue(contextSetupProvider, "security"));
        assertTrue((Boolean) Util.getdFieldValue(contextSetupProvider, "workArea"));
    }

    @Test
    public void testParseContextInfo_null() throws Exception {
        expect(contextServiceConfig.getJndiName()).andReturn("concurrent/ctxSrv").anyTimes();
        expect(contextServiceConfig.getContextInfo()).andReturn(null).anyTimes();
        expect(contextServiceConfig.getContextInfoEnabled()).andReturn("true");
        replay(contextServiceConfig);

        ConcurrentRuntime concurrentRuntime = new ConcurrentRuntime();

        ResourceInfo resource = new ResourceInfo("test");
        ContextServiceImpl contextService = concurrentRuntime.getContextService(resource, contextServiceConfig);
        ContextSetupProviderImpl contextSetupProvider = (ContextSetupProviderImpl) contextService.getContextSetupProvider();
        assertTrue((Boolean) Util.getdFieldValue(contextSetupProvider, "classloading"));
        assertTrue((Boolean) Util.getdFieldValue(contextSetupProvider, "naming"));
        assertTrue((Boolean) Util.getdFieldValue(contextSetupProvider, "security"));
        assertTrue((Boolean) Util.getdFieldValue(contextSetupProvider, "workArea"));
    }

    @Test
    public void testParseContextInfo_empty() throws Exception {
        expect(contextServiceConfig.getJndiName()).andReturn("concurrent/ctxSrv").anyTimes();
        expect(contextServiceConfig.getContextInfo()).andReturn("").anyTimes();
        expect(contextServiceConfig.getContextInfoEnabled()).andReturn("true");
        replay(contextServiceConfig);

        ConcurrentRuntime concurrentRuntime = new ConcurrentRuntime();

        ResourceInfo resource = new ResourceInfo("test");
        ContextServiceImpl contextService = concurrentRuntime.getContextService(resource, contextServiceConfig);
        ContextSetupProviderImpl contextSetupProvider = (ContextSetupProviderImpl) contextService.getContextSetupProvider();
        assertFalse((Boolean) Util.getdFieldValue(contextSetupProvider, "classloading"));
        assertFalse((Boolean) Util.getdFieldValue(contextSetupProvider, "naming"));
        assertFalse((Boolean) Util.getdFieldValue(contextSetupProvider, "security"));
        assertFalse((Boolean) Util.getdFieldValue(contextSetupProvider, "workArea"));
    }

    @Ignore   // re-enable when API added to ManagedThreadFactoryImpl to retrieve ContextService and threadPriority
    @Test
    public void testCreateManagedThreadFactory() throws Exception {
        final int THREAD_PRIORITY = 8;

        expect(managedThreadFactoryConfig.getJndiName()).andReturn("concurrent/mtf").anyTimes();
        expect(managedThreadFactoryConfig.getContextInfo()).andReturn("Classloader, JNDI, Security").anyTimes();
        expect(managedThreadFactoryConfig.getContextInfoEnabled()).andReturn("true").anyTimes();
        expect(managedThreadFactoryConfig.getThreadPriority()).andReturn(THREAD_PRIORITY).anyTimes();
        replay(managedThreadFactoryConfig);

        ConcurrentRuntime concurrentRuntime = new ConcurrentRuntime();

        ResourceInfo resource = new ResourceInfo("test");
        ManagedThreadFactoryImpl managedThreadFactory = concurrentRuntime.getManagedThreadFactory(resource, managedThreadFactoryConfig);
        ContextServiceImpl contextService = (ContextServiceImpl) Util.getdFieldValue(managedThreadFactory, "contextService");
        ContextSetupProviderImpl contextSetupProvider = (ContextSetupProviderImpl) contextService.getContextSetupProvider();
        assertTrue((Boolean) Util.getdFieldValue(contextSetupProvider, "classloading"));
        assertTrue((Boolean) Util.getdFieldValue(contextSetupProvider, "naming"));
        assertTrue((Boolean) Util.getdFieldValue(contextSetupProvider, "security"));
        assertFalse((Boolean) Util.getdFieldValue(contextSetupProvider, "workArea"));

        int threadPriority = (Integer)Util.getdFieldValue(managedThreadFactory, "priority");
        assertEquals(THREAD_PRIORITY, threadPriority);
    }

    @Ignore   // re-enable when API added to ManagedThreadFactoryImpl to retrieve ContextService and threadPriority
    @Test
    public void testCreateManagedExecutorService() throws Exception {
        final int THREAD_PRIORITY = 3;
        final int HUNG_AFTER_SECONDS = 100;
        final int CORE_POOL_SIZE = 1;
        final int MAXIMUM_POOL_SIZE = 5;
        final boolean LONG_RUNNING_TASKS = true;
        final long KEEP_ALIVE_SECONDS = 88L;
        final long THREAD_LIFE_TIME_SECONDS = 99L;
        final int TASK_QUEUE_CAPACITY = 12345;


        expect(managedExecutorServiceConfig.getJndiName()).andReturn("concurrent/mes").anyTimes();
        expect(managedExecutorServiceConfig.getContextInfo()).andReturn("Classloader, JNDI, Security").anyTimes();
        expect(managedExecutorServiceConfig.getContextInfoEnabled()).andReturn("true").anyTimes();
        expect(managedExecutorServiceConfig.getThreadPriority()).andReturn(THREAD_PRIORITY).anyTimes();
        expect(managedExecutorServiceConfig.getHungAfterSeconds()).andReturn(HUNG_AFTER_SECONDS).anyTimes();
        expect(managedExecutorServiceConfig.getCorePoolSize()).andReturn(CORE_POOL_SIZE).anyTimes();
        expect(managedExecutorServiceConfig.getMaximumPoolSize()).andReturn(MAXIMUM_POOL_SIZE).anyTimes();
        expect(managedExecutorServiceConfig.isLongRunningTasks()).andReturn(LONG_RUNNING_TASKS).anyTimes();
        expect(managedExecutorServiceConfig.getKeepAliveSeconds()).andReturn(KEEP_ALIVE_SECONDS).anyTimes();
        expect(managedExecutorServiceConfig.getThreadLifeTimeSeconds()).andReturn(THREAD_LIFE_TIME_SECONDS).anyTimes();
        expect(managedExecutorServiceConfig.getTaskQueueCapacity()).andReturn(TASK_QUEUE_CAPACITY).anyTimes();
        replay(managedExecutorServiceConfig);

        ConcurrentRuntime concurrentRuntime = new ConcurrentRuntime();

        ResourceInfo resource = new ResourceInfo("test");
        ManagedExecutorServiceImpl mes = concurrentRuntime.getManagedExecutorService(resource, managedExecutorServiceConfig);

        ManagedThreadFactoryImpl managedThreadFactory = mes.getManagedThreadFactory();

        assertEquals(HUNG_AFTER_SECONDS * 1000, managedThreadFactory.getHungTaskThreshold());

        ManagedThreadPoolExecutor executor = (ManagedThreadPoolExecutor) Util.getdFieldValue(mes, "threadPoolExecutor");
        assertEquals(CORE_POOL_SIZE, executor.getCorePoolSize());
        assertEquals(KEEP_ALIVE_SECONDS, executor.getKeepAliveTime(TimeUnit.SECONDS));
        assertEquals(MAXIMUM_POOL_SIZE, executor.getMaximumPoolSize());

        long threadLifeTime = (Long)Util.getdFieldValue(executor, "threadLifeTime");
        assertEquals(THREAD_LIFE_TIME_SECONDS, threadLifeTime);

        ContextSetupProviderImpl contextSetupProvider = (ContextSetupProviderImpl) mes.getContextSetupProvider();
        assertTrue((Boolean) Util.getdFieldValue(contextSetupProvider, "classloading"));
        assertTrue((Boolean) Util.getdFieldValue(contextSetupProvider, "naming"));
        assertTrue((Boolean) Util.getdFieldValue(contextSetupProvider, "security"));
        assertFalse((Boolean) Util.getdFieldValue(contextSetupProvider, "workArea"));

        int threadPriority = (Integer)Util.getdFieldValue(managedThreadFactory, "priority");
        assertEquals(THREAD_PRIORITY, threadPriority);
    }
}
