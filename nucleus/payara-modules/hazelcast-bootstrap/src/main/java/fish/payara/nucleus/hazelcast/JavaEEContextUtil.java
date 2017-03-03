/*

 DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.

 Copyright (c) 2014,2015,2016,2017 Payara Foundation. All rights reserved.

 The contents of this file are subject to the terms of the Common Development
 and Distribution License("CDDL") (collectively, the "License").  You
 may not use this file except in compliance with the License.  You can
 obtain a copy of the License at
 https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 or packager/legal/LICENSE.txt.  See the License for the specific
 language governing permissions and limitations under the License.

 When distributing the software, include this License Header Notice in each
 file and include the License file at packager/legal/LICENSE.txt.
 */
package fish.payara.nucleus.hazelcast;

import com.sun.enterprise.container.common.spi.util.ComponentEnvManager;
import com.sun.enterprise.deployment.BundleDescriptor;
import com.sun.enterprise.deployment.JndiNameEnvironment;
import com.sun.enterprise.util.Utility;
import java.util.HashMap;
import java.util.Map;
import javax.enterprise.inject.spi.CDI;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.internal.api.ServerContext;
import org.jboss.weld.context.bound.BoundRequestContext;

/**
 * utility to create / push Java EE thread context
 * 
 * @author lprimak
 */
public class JavaEEContextUtil {
    public JavaEEContextUtil(ServerContext serverContext) {
        this.serverContext = serverContext;
        capturedInvocation = serverContext.getInvocationManager().getCurrentInvocation();
        compEnvMgr = serverContext.getDefaultServices().getService(ComponentEnvManager.class);
    }

    /**
     * pushes Java EE invocation context
     *
     * @return old ClassLoader, or null if no invocation has been created
     */
    public Context preInvoke() {
        ClassLoader oldClassLoader = Utility.getClassLoader();
        InvocationManager invMgr = serverContext.getInvocationManager();
        boolean invocationCreated = false;
        if(invMgr.getCurrentInvocation() == null) {
            invMgr.preInvoke(new ComponentInvocation(capturedInvocation.getComponentId(), ComponentInvocation.ComponentInvocationType.SERVLET_INVOCATION,
                    capturedInvocation.getContainer(), capturedInvocation.getAppName(), capturedInvocation.getModuleName()));
            invocationCreated = true;
        }
        JndiNameEnvironment componentEnv = compEnvMgr.getCurrentJndiNameEnvironment();
        if(invocationCreated && componentEnv instanceof BundleDescriptor) {
            BundleDescriptor bd = (BundleDescriptor)componentEnv;
            Utility.setContextClassLoader(bd.getClassLoader());
        }
        return new Context(oldClassLoader, invocationCreated? invMgr.getCurrentInvocation() : null);
    }

    public void postInvoke(Context ctx) {
        if (ctx.invocation != null) {
            serverContext.getInvocationManager().postInvoke(ctx.invocation);
            Utility.setContextClassLoader(ctx.classLoader);
        }
    }

    /**
     * pushes invocation context onto the stack
     * Also creates Request scope
     *
     * @return new context that was created
     */
    public RequestContext preInvokeRequestContext() {
        Context rootCtx = preInvoke();
        BoundRequestContext brc = CDI.current().select(BoundRequestContext.class).get();
        RequestContext context = new RequestContext(rootCtx, brc.isActive()? null : brc, new HashMap<String, Object>());
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
    public void postInvokeRequestContext(RequestContext context) {
        if (context.ctx != null) {
            context.ctx.deactivate();
            context.ctx.dissociate(context.storage);
        }
        postInvoke(context.rootCtx);
    }

    public static class Context {
        public Context(ClassLoader classLoader, ComponentInvocation invocation) {
            this.classLoader = classLoader;
            this.invocation = invocation;
        }

        final ClassLoader classLoader;
        final ComponentInvocation invocation;
    }

    public static class RequestContext {
        public RequestContext(Context rootCtx, BoundRequestContext ctx, Map<String, Object> storage) {
            this.rootCtx = rootCtx;
            this.ctx = ctx;
            this.storage = storage;
        }

        final Context rootCtx;
        final BoundRequestContext ctx;
        final Map<String, Object> storage;
    }


    private final ServerContext serverContext;
    private final ComponentInvocation capturedInvocation;
    private final ComponentEnvManager compEnvMgr;
}
