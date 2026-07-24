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
// Portions Copyright [2018-2026] [Payara Foundation and/or its affiliates.]

package org.glassfish.concurrent.runtime;

import com.sun.enterprise.security.SecurityContext;
import jakarta.enterprise.concurrent.spi.ThreadContextRestorer;
import jakarta.enterprise.concurrent.spi.ThreadContextSnapshot;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.concurro.spi.ContextHandle;

import javax.security.auth.Subject;
import java.io.IOException;
import java.util.List;

/**
 * Captured context handle used by {@link ContextSetupProviderImpl} to propagate
 * thread context across managed-executor boundaries.
 *
 * <h3>Serialization</h3>
 * <p>This class is {@link java.io.Serializable} for two distinct purposes, both
 * confined to the <em>same JVM</em> with the <em>same Payara version</em>:
 *
 * <ol>
 *   <li><strong>Duplication / contextual-proxy cloning</strong> — when
 *       {@link jakarta.enterprise.concurrent.ContextService#createContextualProxy} wraps
 *       an object with a captured context, the underlying
 *       {@code ContextProxyInvocationHandler} holds a reference to this handle. The
 *       handler may be serialized to produce a deep copy of the captured context, for
 *       example when the proxy itself is passed across component boundaries within the
 *       same application.</li>
 *
 *   <li><strong>EJB passivation</strong> — a contextual proxy held inside a
 *       {@code @Stateful} EJB may be serialized to disk by the container and restored
 *       later in the same JVM.</li>
 * </ol>
 *
 * <p>Remote calls and rolling upgrades are not a concern: serialized instances are
 * always read back by the same JVM and the same Payara version. The
 * {@code serialVersionUID} is intentionally bumped when the serialized form changes
 * because same-JVM same-version round-trips are always safe to break.
 */
public class InvocationContext implements ContextHandle {

    /** Bumped when the serialized form changes; same-JVM same-version round-trips only. */
    static final long serialVersionUID = 1L;

    private transient ComponentInvocation invocation;
    private transient ClassLoader contextClassLoader;
    private transient SecurityContext securityContext;
    private boolean useTransactionOfExecutionThread;

    private List<ThreadContextSnapshot> threadContextSnapshots;
    private List<ThreadContextRestorer> threadContextRestorers;

    public InvocationContext(ComponentInvocation invocation, ClassLoader contextClassLoader,
            SecurityContext securityContext, boolean useTransactionOfExecutionThread,
            List<ThreadContextSnapshot> threadContextSnapshots,
            List<ThreadContextRestorer> threadContextRestorers) {
        this.invocation = invocation;
        this.contextClassLoader = contextClassLoader;
        this.securityContext = securityContext;
        this.useTransactionOfExecutionThread = useTransactionOfExecutionThread;
        this.threadContextSnapshots = threadContextSnapshots;
        this.threadContextRestorers = threadContextRestorers;
    }

    public ComponentInvocation getInvocation() { return invocation; }
    public ClassLoader getContextClassLoader() { return contextClassLoader; }
    public SecurityContext getSecurityContext() { return securityContext; }
    public boolean isUseTransactionOfExecutionThread() { return useTransactionOfExecutionThread; }
    public List<ThreadContextSnapshot> getThreadContextSnapshots() { return threadContextSnapshots; }
    public List<ThreadContextRestorer> getThreadContextRestorers() { return threadContextRestorers; }

    private void writeObject(java.io.ObjectOutputStream out) throws IOException {
        out.writeBoolean(useTransactionOfExecutionThread);
        String componentId = null, appName = null, moduleName = null;
        if (invocation != null) {
            componentId = invocation.getComponentId();
            appName     = invocation.getAppName();
            moduleName  = invocation.getModuleName();
        }
        out.writeObject(componentId);
        out.writeObject(appName);
        out.writeObject(moduleName);

        String principalName = null;
        boolean defaultSecurityContext = false;
        Subject subject = null;
        if (securityContext != null) {
            if (securityContext.getCallerPrincipal() != null) {
                principalName = securityContext.getCallerPrincipal().getName();
                subject = securityContext.getSubject();
                // Clear principal set to avoid ClassNotFoundException during deserialization.
                subject.getPrincipals().clear();
            }
            defaultSecurityContext = (securityContext == SecurityContext.getDefaultSecurityContext());
        }
        out.writeObject(principalName);
        out.writeBoolean(defaultSecurityContext);
        out.writeObject(subject);
        out.writeObject(threadContextSnapshots);
        out.writeObject(threadContextRestorers);
    }

    private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
        useTransactionOfExecutionThread = in.readBoolean();
        String componentId = (String) in.readObject();
        String appName     = (String) in.readObject();
        String moduleName  = (String) in.readObject();
        invocation = createComponentInvocation(componentId, appName, moduleName);

        String  principalName        = (String)  in.readObject();
        boolean defaultSecurityCtx   = in.readBoolean();
        Subject subject              = (Subject) in.readObject();
        if (principalName != null) {
            securityContext = defaultSecurityCtx
                    ? SecurityContext.getDefaultSecurityContext()
                    : new SecurityContext(principalName, subject, null);
        }
        contextClassLoader = ConcurrentRuntime.getRuntime().getInvocationFacade().getContextClassLoader(appName);
        threadContextSnapshots  = (List<ThreadContextSnapshot>)  in.readObject();
        threadContextRestorers  = (List<ThreadContextRestorer>)  in.readObject();
    }

    private ComponentInvocation createComponentInvocation(String componentId, String appName, String moduleName) {
        if (componentId == null && appName == null && moduleName == null) {
            return null;
        }
        return new ComponentInvocation(
                componentId,
                ComponentInvocation.ComponentInvocationType.SERVLET_INVOCATION,
                null, appName, moduleName, appName);
    }
}
