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
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.Serializable;
import org.glassfish.internal.api.JavaEEContextUtil;
import java.util.HashMap;
import javax.annotation.PostConstruct;
import javax.enterprise.inject.spi.CDI;
import lombok.AccessLevel;
import lombok.Getter;
import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.internal.api.Globals;
import org.glassfish.internal.api.ServerContext;
import org.jboss.weld.context.bound.BoundRequestContext;
import org.jvnet.hk2.annotations.Service;

/**
 * utility to create / push Java EE thread context
 * 
 * @author lprimak
 */
@Service
@PerLookup
public class JavaEEContextUtilImpl implements JavaEEContextUtil, Serializable {
    @PostConstruct
    void init() {
        serverContext = Globals.getDefaultHabitat().getService(ServerContext.class);
        compEnvMgr = Globals.getDefaultHabitat().getService(ComponentEnvManager.class);

        doSetInstanceContext();
    }

    private void readObject(ObjectInputStream in) throws IOException, ClassNotFoundException {
        in.defaultReadObject();
        init();
    }

    /**
     * pushes Java EE invocation context
     *
     * @return the new context
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
        return new ContextImpl.Context(oldClassLoader, invocationCreated? invMgr.getCurrentInvocation() : null, invMgr);
    }

    /**
     * pushes invocation context onto the stack
     * Also creates Request scope
     *
     * @return new context that was created
     */
    @Override
    public Context pushRequestContext() {
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
     * set context class loader by component ID
     */
    @Override
    public void setApplicationClassLoader() {
        ClassLoader cl = null;
        if(capturedInvocation != null && capturedInvocation.getJNDIEnvironment() != null) {
            cl = getClassLoaderForEnvironment((JndiNameEnvironment)capturedInvocation.getJNDIEnvironment());
        }
        else if(componentId != null) {
            cl = getClassLoaderForEnvironment(compEnvMgr.getJndiNameEnvironment(componentId));
        }
        if(cl != null) {
            Utility.setContextClassLoader(cl);
        }
    }

    @Override
    public ClassLoader getInvocationClassLoader() {
        JndiNameEnvironment componentEnv = compEnvMgr.getCurrentJndiNameEnvironment();
        return getClassLoaderForEnvironment(componentEnv);
    }

    @Override
    public void setInstanceContext() {
        componentId = null;
        doSetInstanceContext();
    }

    @Override
    public String getInvocationComponentId() {
        ComponentInvocation inv = serverContext.getInvocationManager().getCurrentInvocation();
        return inv != null? inv.getComponentId() : null;
    }

    @Override
    public JavaEEContextUtil setInstanceComponentId(String componentId) {
        this.componentId = componentId;
        if(componentId != null) {
            createInvocationContext();
        }
        return this;
    }

    private void doSetInstanceContext() {
        capturedInvocation = serverContext.getInvocationManager().getCurrentInvocation();
        if(capturedInvocation != null) {
            capturedInvocation = capturedInvocation.clone();
            componentId = capturedInvocation.getComponentId();
        }
        else if(componentId != null) {
            // deserialized version
            createInvocationContext();
        }
    }

    private ClassLoader getClassLoaderForEnvironment(JndiNameEnvironment componentEnv) {
        if(componentEnv instanceof BundleDescriptor) {
            BundleDescriptor bd = (BundleDescriptor)componentEnv;
            return bd.getClassLoader();
        } else if (componentEnv instanceof EjbDescriptor) {
            EjbDescriptor ed = (EjbDescriptor)componentEnv;
            return ed.getEjbBundleDescriptor().getClassLoader();
        }
        return null;
    }

    private void createInvocationContext() {
        capturedInvocation = new ComponentInvocation();
        capturedInvocation.componentId = componentId;
        capturedInvocation.setJNDIEnvironment(compEnvMgr.getJndiNameEnvironment(componentId));
        capturedInvocation.setComponentInvocationType(ComponentInvocation.ComponentInvocationType.SERVLET_INVOCATION);
    }


    private transient @Getter(AccessLevel.PACKAGE) ServerContext serverContext;
    private transient ComponentEnvManager compEnvMgr;
    private transient ComponentInvocation capturedInvocation;
    private String componentId;
    private static final long serialVersionUID = 1L;
}
