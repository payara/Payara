/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2024] Payara Foundation and/or its affiliates. All rights reserved.
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
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package fish.payara.appserver.context;

import com.sun.enterprise.container.common.spi.util.ComponentEnvManager;
import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.deployment.JndiNameEnvironment;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.util.Utility;
import java.io.Serializable;
import java.util.Collection;
import org.glassfish.internal.api.JavaEEContextUtil;
import java.util.HashMap;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Inject;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.deployment.versioning.VersioningUtils;
import org.glassfish.grizzly.utils.Holder;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.data.ApplicationRegistry;
import org.glassfish.internal.data.ModuleInfo;
import org.jboss.weld.context.bound.BoundRequestContext;
import org.jvnet.hk2.annotations.Service;

/**
 * utility to create / push Java EE thread context
 *
 * @author lprimak
 */
@Service
public class JavaEEContextUtilImpl implements JavaEEContextUtil, Serializable {
    private static final long serialVersionUID = 2L;

    @Inject
    private transient ComponentEnvManager compEnvMgr;
    @Inject
    private transient ApplicationRegistry appRegistry;
    @Inject
    private transient InvocationManager invocationManager;


    @Override
    public Instance empty() {
        return new InstanceImpl();
    }

    @Override
    public Instance currentInvocation() {
        ComponentInvocation currentInvocation = invocationManager.getCurrentInvocation();
        if (currentInvocation == null) {
            throw new IllegalStateException("No Current Invocation present");
        }
        return new InstanceImpl(currentInvocation);
    }

    @Override
    public Instance fromComponentId(String componentId) throws IllegalArgumentException {
        if (componentId == null) {
            throw new IllegalArgumentException("componentId cannot be null");
        }
        return new InstanceImpl(componentId);
    }

    @Override
    public ClassLoader getInvocationClassLoader() {
        JndiNameEnvironment componentEnv = compEnvMgr.getCurrentJndiNameEnvironment();
        return getClassLoaderForEnvironment(componentEnv);
    }

    @Override
    public String getInvocationComponentId() {
        ComponentInvocation inv = invocationManager.getCurrentInvocation();
        return inv != null? inv.getComponentId() : null;
    }

    @Override
    public boolean isInvocationLoaded() {
        ComponentInvocation inv = invocationManager.getCurrentInvocation();
        return inv != null ? isLoaded(inv.getComponentId(), inv) : false;
    }

    @Override
    public boolean moduleMatches(ModuleInfo moduleInfo, String modulNameToMatch) {
        return VersioningUtils.getUntaggedName(moduleInfo.getName()).equals(modulNameToMatch);
    }

    private static ClassLoader getClassLoaderForEnvironment(JndiNameEnvironment componentEnv) {
        if (componentEnv instanceof BundleDescriptor) {
            BundleDescriptor bd = (BundleDescriptor) componentEnv;
            return bd.getClassLoader();
        } else if (componentEnv instanceof EjbDescriptor) {
            EjbDescriptor ed = (EjbDescriptor) componentEnv;
            return ed.getEjbBundleDescriptor().getClassLoader();
        }
        return null;
    }

    private ComponentInvocation createInvocation(JndiNameEnvironment jndiEnv, String componentId) {
        ComponentInvocation newInvocation = new ComponentInvocation();
        newInvocation.componentId = componentId;
        newInvocation.setJNDIEnvironment(jndiEnv);
        newInvocation.setComponentInvocationType(ComponentInvocation.ComponentInvocationType.SERVLET_INVOCATION);
        return newInvocation;
    }

    private boolean isLoaded(String componentId, ComponentInvocation invocation) {
        if (componentId == null) {
            // empty components are always loaded
            return true;
        }
        JndiNameEnvironment env = invocation != null ? ((JndiNameEnvironment) invocation.getJNDIEnvironment())
                : compEnvMgr.getJndiNameEnvironment(componentId);
        if (env != null) {
            ApplicationInfo appInfo = null;
            try {
                appInfo = appRegistry.get(DOLUtils.getApplicationFromEnv(env).getRegistrationName());
            } catch (IllegalArgumentException e) {
                // empty environment, not associated with any app
            }
            if (appInfo != null && appInfo.getModuleInfos().stream()
                    .filter(mod -> DOLUtils.isEarApplication(env) ? true
                    // Check if deployed vs. Payara internal application
                    : mod.getName().equals(DOLUtils.getModuleName(env)))
                    .anyMatch(moduleInfo -> !moduleInfo.isLoaded())) {
                return false;
            }
        }
        return env != null;
    }

    static boolean isLeaked(ComponentEnvManager compEnvMgr, ComponentInvocation cachedInvocation, String componentId) {
        if (cachedInvocation != null) {
            if (getClassLoaderForEnvironment((JndiNameEnvironment) cachedInvocation
                    .getJNDIEnvironment()) != getClassLoaderForEnvironment(compEnvMgr.
                            getJndiNameEnvironment(componentId))) {
                // referencing old class loader / environment, remove it
                return true;
            }
        }
        return false;
    }

    // deals with @Injected transient fields correctly
    private Object readResolve() {
        return Globals.getDefaultHabitat().getService(JavaEEContextUtil.class);
    }


    public class InstanceImpl implements Instance {
        private static final long serialVersionUID = 1L;
        private final String componentId;
        private volatile transient ComponentInvocation cachedInvocation;
        private volatile transient boolean loaded;


        /**
         * empty invocation
         */
        private InstanceImpl() {
            // empty
            componentId = null;
            checkState();
        }

        /**
         * instance from  current invocation
         *
         * @param currentInvocation non-null current invocation
         */
        private InstanceImpl(ComponentInvocation currentInvocation) {
            boolean isApplicationComponent = false;
            if (currentInvocation.getComponentId() != null) {
                componentId = VersioningUtils.getUntaggedName(currentInvocation.getComponentId());
            } else if (currentInvocation.getJNDIEnvironment() instanceof JndiNameEnvironment) {
                componentId = DOLUtils.toEarComponentId(
                        DOLUtils.getApplicationName((JndiNameEnvironment)
                                currentInvocation.getJNDIEnvironment()));
                isApplicationComponent = true;
            } else {
                // checkState() later should error out due to this condition
                componentId = null;
            }
            cachedInvocation = currentInvocation.clone();
            if (isApplicationComponent) {
                // application components don't have component ID, just application name
                // here we set component ID on the cached object to application name so internal state
                // is set correctly
                cachedInvocation.setComponentId(componentId);
            }
            cachedInvocation.clearRegistry();
            checkState();
        }

        private InstanceImpl(String componentId) {
            this.componentId = componentId;
            checkState();
        }

        @Override
        public Context pushContext() {
            if (isEmpty()) {
                return new ContextImpl.EmptyContext(invocationManager);
            }
            if (!isValidAndNotEmpty()) {
                // same as invocation, or app not running
                return new ContextImpl.Context(null, invocationManager, compEnvMgr, null);
            }
            ComponentInvocation newInvocation = ensureCached(true).clone();
            invocationManager.preInvoke(newInvocation);
            return new ContextImpl.Context(newInvocation, invocationManager, compEnvMgr,
                    Utility.setContextClassLoader(getInvocationClassLoader()));
        }

        @Override
        public Context pushRequestContext() {
            Context rootCtx = pushContext();
            if (!isValidAndNotEmpty()) {
                return rootCtx;
            }
            BoundRequestContext brc;
            try {
                 brc = CDI.current().select(BoundRequestContext.class).get();
            } catch (Throwable ex) {
                return rootCtx;
            }
            ContextImpl.RequestContext context = new ContextImpl.RequestContext(rootCtx, brc.isActive() ? null : brc, new HashMap<>());
            if (context.ctx != null) {
                context.ctx.associate(context.storage);
                context.ctx.activate();
            }
            return context;
        }

        @Override
        public Context setApplicationClassLoader() {
            ClassLoader cl = null;
            ComponentInvocation localCachedInvocation = ensureCached(false);
            if (localCachedInvocation != null) {
                cl = getClassLoaderForEnvironment((JndiNameEnvironment) localCachedInvocation.getJNDIEnvironment());
            } else if (componentId != null) {
                cl = getClassLoaderForEnvironment(compEnvMgr.getJndiNameEnvironment(componentId));
            }
            if (cl != null) {
                return new ContextImpl.ClassLoaderContext(Utility.setContextClassLoader(cl), true);
            }
            return new ContextImpl.ClassLoaderContext(null, false);
        }

        @Override
        public String getInstanceComponentId() {
            return componentId;
        }

        private ComponentInvocation ensureCached(boolean failOnError) {
            ComponentInvocation localCachedInvocation = cachedInvocation;
            // empty objects not allowed
            if (localCachedInvocation != null || isEmpty()) {
                return localCachedInvocation;
            }
            JndiNameEnvironment jndiEnv = compEnvMgr.getJndiNameEnvironment(componentId);
            if (jndiEnv != null) { // create invocation only for valid JNDI environment
                    localCachedInvocation = createInvocation(jndiEnv, componentId);
                    cachedInvocation = localCachedInvocation;
            } else if (failOnError) {
                throw new IllegalStateException(String.format("Cannot cache invocation: %s", componentId));
            }
            checkState();
            return localCachedInvocation;
        }

        @Override
        public boolean isLoaded() {
            if (!loaded) {
                loaded = JavaEEContextUtilImpl.this.isLoaded(componentId, cachedInvocation);
            }
            return loaded;
        }

        @Override
        public boolean isEmpty() {
            return componentId == null;
        }

        @Override
        public void clearInstanceInvocation() {
            loaded = false;
            cachedInvocation = null;
        }

        private boolean isValidAndNotEmpty() {
            return isCurrentInvocationPresentAndSame() || isLoaded();
        }

        private boolean isCurrentInvocationPresentAndSame() {
            ComponentInvocation invocation = invocationManager.getCurrentInvocation();
            if (invocation != null) {
                return componentId != null && componentId.equals(invocation.getComponentId());
            } else {
                return false;
            }
        }

        private void checkState() {
            ComponentInvocation localCachedInvocation = cachedInvocation;
            if (componentId == null && localCachedInvocation != null) {
                // empty invocation
                throw new IllegalStateException("Cannot have non-null cached invocation for an empty component");
            }
            if (localCachedInvocation != null) {
                // check for validity of cached invocation
                if (localCachedInvocation.getComponentId() == null || localCachedInvocation.getJNDIEnvironment() == null) {
                    throw new IllegalStateException("Invalid Cached Invocation - either componentID or JNDIEnvironment is null");
                }
            }
            if (loaded && localCachedInvocation == null) {
                throw new IllegalStateException("running/loaded invocation with null cache");
            }
        }
    }
}
