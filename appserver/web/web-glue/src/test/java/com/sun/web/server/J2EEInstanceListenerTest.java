/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2026 Payara Foundation and/or its affiliates. All rights reserved.
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
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
 * Version 2] license."  If you not indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package com.sun.web.server;

import com.sun.enterprise.container.common.spi.util.InjectionManager;
import com.sun.enterprise.security.SecurityContextProxy;
import com.sun.enterprise.security.integration.AppServSecurityContext;
import com.sun.enterprise.web.WebModule;
import org.apache.catalina.InstanceEvent;
import org.apache.catalina.Realm;
import org.apache.catalina.Wrapper;
import org.glassfish.api.invocation.InvocationManager;
import org.junit.Before;
import org.junit.Test;

import jakarta.servlet.Servlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import java.lang.reflect.Field;
import java.security.Principal;

import static org.mockito.Mockito.*;

/**
 * Unit tests for the principal-selection logic in {@link J2EEInstanceListener#handleBeforeEvent}.
 *
 * <p>The listener runs inside the HK2 container in production, so {@code init()} is bypassed here
 * by injecting the required fields via reflection. Only the security-context selection path is
 * exercised; lifecycle (preInvoke/postInvoke) is stubbed out.
 */
public class J2EEInstanceListenerTest {

    private J2EEInstanceListener listener;
    private AppServSecurityContext securityContext;
    private WebModule webModule;
    private Wrapper wrapper;

    @Before
    public void setUp() throws Exception {
        listener = new J2EEInstanceListener();
        securityContext = mock(AppServSecurityContext.class);

        // Bypass init(): inject the fields that handleBeforeEvent needs.
        inject("initialized", true);
        inject("securityContext", securityContext);
        inject("im", mock(InvocationManager.class));
        inject("injectionMgr", mock(InjectionManager.class));
        // tm stays null — enlistComponentResources() is guarded by (tm != null).

        webModule = mock(WebModule.class, RETURNS_DEEP_STUBS);
        when(webModule.getRealm()).thenReturn(mock(Realm.class));

        wrapper = mock(Wrapper.class);
        when(wrapper.getParent()).thenReturn(webModule);
    }

    /**
     * A request wrapper that overrides {@code getUserPrincipal()} must take priority over the
     * container's own principal.  Before FISH-991 this worked correctly; the fix introduced a
     * regression where the overriding principal was silently ignored.
     */
    @Test
    public void wrapperOverridingGetUserPrincipal_honorsWrapper() throws Exception {
        // Inner (base) request: container-authenticated as alice.
        HttpServletRequest inner = mock(HttpServletRequest.class);
        Principal alicePrincipal = getMockedSecurityContextProxyPrincipal();
        when(inner.getUserPrincipal()).thenReturn(alicePrincipal);

        // A filter wraps the request and switches the identity to bob.
        Principal bobPrincipal = mock(Principal.class);
        HttpServletRequest wrappedRequest = new HttpServletRequestWrapper(inner) {
            @Override
            public Principal getUserPrincipal() {
                return bobPrincipal;
            }
        };

        listener.instanceEvent(buildBeforeServiceEvent(wrappedRequest));

        verify(securityContext).setSecurityContextWithPrincipal(bobPrincipal);
        verify(securityContext, never()).setSecurityContextWithPrincipal(alicePrincipal);
    }

    /**
     * Without a wrapper, a {@link SecurityContextProxy} principal must be propagated directly to
     * the security context so that group membership is preserved (FISH-991).
     */
    @Test
    public void noWrapper_securityContextProxyPrincipal_propagatesContext() throws Exception {
        Principal webPrincipal = getMockedSecurityContextProxyPrincipal();
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getUserPrincipal()).thenReturn(webPrincipal);

        listener.instanceEvent(buildBeforeServiceEvent(request));

        verify(securityContext).setSecurityContextWithPrincipal(webPrincipal);
    }

    /**
     * A non-wrapping request with a plain (non-{@link SecurityContextProxy}) principal should not
     * modify the security context — the request is either unauthenticated or goes through a realm
     * that manages its own context separately.
     */
    @Test
    public void noWrapper_plainPrincipal_doesNotSetSecurityContext() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getUserPrincipal()).thenReturn(mock(Principal.class));

        listener.instanceEvent(buildBeforeServiceEvent(request));

        verify(securityContext, never()).setSecurityContextWithPrincipal(any());
    }

    /**
     * An unauthenticated request (null principal) should not touch the security context at all.
     */
    @Test
    public void nullPrincipal_doesNotSetSecurityContext() throws Exception {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getUserPrincipal()).thenReturn(null);

        listener.instanceEvent(buildBeforeServiceEvent(request));

        verify(securityContext, never()).setSecurityContextWithPrincipal(any());
    }

    private InstanceEvent buildBeforeServiceEvent(HttpServletRequest request) {
        return new InstanceEvent(
                wrapper,
                mock(Servlet.class),
                InstanceEvent.EventType.BEFORE_SERVICE_EVENT,
                request,
                mock(HttpServletResponse.class));
    }

    private static Principal getMockedSecurityContextProxyPrincipal() {
        return mock(Principal.class, withSettings().extraInterfaces(SecurityContextProxy.class));
    }

    private void inject(String fieldName, Object value) throws Exception {
        Field f = J2EEInstanceListener.class.getDeclaredField(fieldName);
        f.setAccessible(true);
        f.set(listener, value);
    }
}
