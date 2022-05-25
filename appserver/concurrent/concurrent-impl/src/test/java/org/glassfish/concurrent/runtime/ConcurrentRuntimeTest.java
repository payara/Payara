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
// Portions Copyright [2022] Payara Foundation and/or affiliates

package org.glassfish.concurrent.runtime;

import org.glassfish.concurrent.config.ContextService;
import org.glassfish.concurrent.runtime.deployer.ContextServiceConfig;
import org.glassfish.enterprise.concurrent.ContextServiceImpl;
import org.glassfish.resourcebase.resources.api.ResourceInfo;
import org.junit.Before;
import org.junit.Test;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.*;

public class ConcurrentRuntimeTest {

    private ContextService configContextService;

    @Before
    public void before() {
        configContextService = createMock(ContextService.class);
    }

    @Test
    public void testParseContextInfo() throws Exception {
        expect(configContextService.getJndiName()).andReturn("concurrent/ctxSrv").anyTimes();
        expect(configContextService.getContextInfo()).andReturn("Classloader, JNDI, Security, WorkArea").anyTimes();
        expect(configContextService.getContextInfoEnabled()).andReturn("true");
        replay(configContextService);
        ContextServiceConfig contextServiceConfig = new ContextServiceConfig(configContextService);

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
        expect(configContextService.getJndiName()).andReturn("concurrent/ctxSrv").anyTimes();
        expect(configContextService.getContextInfo()).andReturn("classloader, jndi, security, workarea").anyTimes();
        expect(configContextService.getContextInfoEnabled()).andReturn("true");
        replay(configContextService);
        ContextServiceConfig contextServiceConfig = new ContextServiceConfig(configContextService);

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
        expect(configContextService.getJndiName()).andReturn("concurrent/ctxSrv").anyTimes();
        expect(configContextService.getContextInfo()).andReturn("CLASSLOADER, JNDI, SECURITY, WORKAREA").anyTimes();
        expect(configContextService.getContextInfoEnabled()).andReturn("true");
        replay(configContextService);
        ContextServiceConfig contextServiceConfig = new ContextServiceConfig(configContextService);

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
        expect(configContextService.getJndiName()).andReturn("concurrent/ctxSrv").anyTimes();
        expect(configContextService.getContextInfo()).andReturn("Classloader, JNDI, Security, WorkArea").anyTimes();
        expect(configContextService.getContextInfoEnabled()).andReturn("false");
        replay(configContextService);
        ContextServiceConfig contextServiceConfig = new ContextServiceConfig(configContextService);

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
        expect(configContextService.getJndiName()).andReturn("concurrent/ctxSrv").anyTimes();
        expect(configContextService.getContextInfo()).andReturn("JNDI, blah, beh, JNDI, WorkArea, WorkArea, ").anyTimes();
        expect(configContextService.getContextInfoEnabled()).andReturn("true");
        replay(configContextService);
        ContextServiceConfig contextServiceConfig = new ContextServiceConfig(configContextService);

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
        expect(configContextService.getJndiName()).andReturn("concurrent/ctxSrv").anyTimes();
        expect(configContextService.getContextInfo()).andReturn(null).anyTimes();
        expect(configContextService.getContextInfoEnabled()).andReturn("true");
        replay(configContextService);
        ContextServiceConfig contextServiceConfig = new ContextServiceConfig(configContextService);

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
        expect(configContextService.getJndiName()).andReturn("concurrent/ctxSrv").anyTimes();
        expect(configContextService.getContextInfo()).andReturn("").anyTimes();
        expect(configContextService.getContextInfoEnabled()).andReturn("true");
        replay(configContextService);
        ContextServiceConfig contextServiceConfig = new ContextServiceConfig(configContextService);

        ConcurrentRuntime concurrentRuntime = new ConcurrentRuntime();

        ResourceInfo resource = new ResourceInfo("test");
        ContextServiceImpl contextService = concurrentRuntime.getContextService(resource, contextServiceConfig);
        ContextSetupProviderImpl contextSetupProvider = (ContextSetupProviderImpl) contextService.getContextSetupProvider();
        assertFalse((Boolean) Util.getdFieldValue(contextSetupProvider, "classloading"));
        assertFalse((Boolean) Util.getdFieldValue(contextSetupProvider, "naming"));
        assertFalse((Boolean) Util.getdFieldValue(contextSetupProvider, "security"));
        assertFalse((Boolean) Util.getdFieldValue(contextSetupProvider, "workArea"));
    }
}
