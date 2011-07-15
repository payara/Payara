/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.webservices;

import javax.xml.ws.Provider;
import javax.xml.ws.WebServiceContext;
import java.lang.reflect.Method;
import java.lang.reflect.InvocationTargetException;
import com.sun.xml.ws.api.message.Packet;
import com.sun.xml.ws.api.server.*;

/**
 * Implements JAXWS's Invoker interface to call the endpoint method
 */
public class InvokerImpl extends Invoker {
    protected final Invoker core;
    protected final Object invokeObject;
    protected final WebServiceContextImpl injectedWSCtxt;

    public InvokerImpl(Invoker core, Object inv, WebServiceContextImpl wsc) {
        this.core = core;
        this.injectedWSCtxt = wsc;
        this.invokeObject = inv;
    }

    private static final boolean jaxwsDirect=Boolean.getBoolean("com.sun.enterprise.webservice.jaxwsDirect");

    public void start(WSWebServiceContext wsc, WSEndpoint endpoint) {
        if(this.injectedWSCtxt != null) {
            injectedWSCtxt.setContextDelegate(wsc);
        }
        core.start(injectedWSCtxt, endpoint);
    }

    public void dispose() {
        core.dispose();
    }

    public Object invoke(Packet p, Method m, Object... args) throws InvocationTargetException, IllegalAccessException {
        if(jaxwsDirect)
            return core.invoke(p,m,args);
        Object ret = null;
        if(this.invokeObject != null) {
            ret = m.invoke(this.invokeObject, args);
        }
        return ret;
    }

    private static final Method invokeMethod;

    static {
        try {
            invokeMethod = Provider.class.getMethod("invoke",Object.class);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    public <T> T invokeProvider(Packet p, T arg) throws IllegalAccessException, InvocationTargetException {
        if(jaxwsDirect)
            return core.invokeProvider(p, arg);
        Object ret = null;
        if(this.invokeObject != null) {
            ret = invokeMethod.invoke(this.invokeObject, arg);
        }
        return (T)ret;

    }

    private static final Method asyncInvokeMethod;

    static {
        try {
            asyncInvokeMethod = AsyncProvider.class.getMethod("invoke",Object.class, AsyncProviderCallback.class, WebServiceContext.class);
        } catch (NoSuchMethodException e) {
            throw new AssertionError(e);
        }
    }

    public <T> void invokeAsyncProvider(Packet p, T arg, AsyncProviderCallback cbak, WebServiceContext ctxt) throws IllegalAccessException, InvocationTargetException {
        if(jaxwsDirect)
            core.invokeAsyncProvider(p, arg, cbak, ctxt);
        if(this.invokeObject != null) {
            asyncInvokeMethod.invoke(this.invokeObject, arg);
        }

    }
}
