/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2016-2017] Payara Foundation and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at glassfish/legal/LICENSE.txt.
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
import com.sun.enterprise.util.Utility;
import org.glassfish.internal.api.JavaEEContextUtil;
import java.util.HashMap;
import javax.annotation.PostConstruct;
import javax.enterprise.inject.spi.CDI;
import javax.inject.Inject;
import lombok.AccessLevel;
import lombok.Getter;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.internal.api.ServerContext;
import org.glassfish.internal.data.ApplicationInfo;
import org.glassfish.internal.data.ApplicationRegistry;
import org.jboss.weld.context.bound.BoundRequestContext;
import org.jvnet.hk2.annotations.Service;

/**
 * utility to create / push Java EE thread context
 * 
 * @author lprimak
 */
@Service
@PerLookup
public class JavaEEContextUtilImpl implements JavaEEContextUtil {
    @PostConstruct
    void init() {
        capturedInvocation = serverContext.getInvocationManager().getCurrentInvocation();
        if(capturedInvocation != null) {
            capturedInvocation = capturedInvocation.clone();
        }
    }

    /**
     * pushes Java EE invocation context
     *
     * @return old ClassLoader, or null if no invocation has been created
     */
    @Override
    public Context pushContext() {
        ClassLoader oldClassLoader = Utility.getClassLoader();
        InvocationManager invMgr = serverContext.getInvocationManager();
        boolean invocationCreated = false;
        if(invMgr.getCurrentInvocation() == null && capturedInvocation != null) {
            ComponentInvocation newInvocation = capturedInvocation.clone();
            newInvocation.clearRegistry();
            invMgr.preInvoke(newInvocation);
            invocationCreated = true;
        }
        if(invocationCreated) {
            Utility.setContextClassLoader(getInvocationClassLoader());
        }
        return new ContextImpl.Context(oldClassLoader, invocationCreated? invMgr.getCurrentInvocation() : null);
    }

    @Override
    public void popContext(Context _ctx) {
        ContextImpl.Context ctx = (ContextImpl.Context)_ctx;
        if (ctx.invocation != null) {
            getServerContext().getInvocationManager().postInvoke(ctx.invocation);
            Utility.setContextClassLoader(ctx.classLoader);
        }
    }

    /**
     * pushes invocation context onto the stack
     * Also creates Request scope
     *
     * @return new context that was created
     */
    @Override
    public RequestContext pushRequestContext() {
        Context rootCtx = pushContext();
        BoundRequestContext brc = CDI.current().select(BoundRequestContext.class).get();
        ContextImpl.RequestContext context = new ContextImpl.RequestContext(rootCtx, brc.isActive()? null : brc, new HashMap<String, Object>());
        if(context.ctx != null) {
            context.ctx.associate(context.storage);
            context.ctx.activate();
        }
        return context;
    }

    /**
     * context to pop from the stack
     *
     * @param context to be popped
     */
    @Override
    public void popRequestContext(RequestContext context) {
        ContextImpl.RequestContext ctx = (ContextImpl.RequestContext)context;
        if (ctx.ctx!= null) {
            ctx.ctx.deactivate();
            ctx.ctx.dissociate(ctx.storage);
        }
        popContext(ctx.rootCtx);
    }

    /**
     * @return application name or null if there is no invocation context
     */
    @Override
    public String getApplicationName() {
        ComponentInvocation ci = serverContext.getInvocationManager().getCurrentInvocation();
        return ci != null? ci.getModuleName() : null;
    }

    /**
     * set context class loader by appName
     *
     * @param appName
     */
    @Override
    public void setApplicationContext(String appName) {
        if(appName == null) {
            return;
        }
        ApplicationInfo appInfo = appRegistry.get(appName);

        // try plain non-versioned app first
        if(appInfo == null) {
            appName = stripVersionSuffix(appName);
            appInfo = appRegistry.get(appName);
        }
        // for versioned applications, search app registry and strip out the version number
        if(appInfo == null) {
            for(String regAppName : appRegistry.getAllApplicationNames()) {
                if(stripVersionSuffix(regAppName).equals(appName)) {
                    appInfo = appRegistry.get(regAppName);
                    break;
                }
            }
        }
        if(appInfo != null) {
            Utility.setContextClassLoader(appInfo.getAppClassLoader());
        }
    }

    @Override
    public ClassLoader getInvocationClassLoader() {
        JndiNameEnvironment componentEnv = compEnvMgr.getCurrentJndiNameEnvironment();
        if(componentEnv instanceof BundleDescriptor) {
            BundleDescriptor bd = (BundleDescriptor)componentEnv;
            return bd.getClassLoader();
        } else if (componentEnv instanceof EjbDescriptor) {
            EjbDescriptor ed = (EjbDescriptor)componentEnv;
            return ed.getEjbBundleDescriptor().getClassLoader();
        }
        return null;
    }

    private static String stripVersionSuffix(String name) {
        int delimiterIndex = name.lastIndexOf(':');
        return delimiterIndex != -1? name.substring(0, delimiterIndex) : name;
    }


    private @Inject @Getter(AccessLevel.PACKAGE) ServerContext serverContext;
    private ComponentInvocation capturedInvocation;
    private @Inject ComponentEnvManager compEnvMgr;
    private @Inject ApplicationRegistry appRegistry;
}
