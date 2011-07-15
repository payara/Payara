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

import java.util.Hashtable;
import java.util.Iterator;

import java.lang.reflect.Proxy;
import java.lang.reflect.InvocationHandler;

import java.rmi.Remote;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;

import javax.xml.rpc.server.ServiceLifecycle;

import org.glassfish.api.invocation.ComponentInvocation;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.ejb.api.EJBInvocation;

// JAXRPC-RI classes
import com.sun.xml.rpc.spi.JaxRpcObjectFactory;
import com.sun.xml.rpc.spi.runtime.Implementor;
import com.sun.xml.rpc.spi.runtime.ImplementorCache;
import com.sun.xml.rpc.spi.runtime.ImplementorCacheDelegate;
import com.sun.xml.rpc.spi.runtime.RuntimeEndpointInfo;
import com.sun.xml.rpc.spi.runtime.Tie;

/**
 * This class extends the behavior of ImplementorCache in order to
 * interpose on lifecycle events for the creation and destruction of 
 * ties/servants for servlet web service endpoints.
 *
 * @author Kenneth Saks
 */
public class ImplementorCacheDelegateImpl extends ImplementorCacheDelegate {

    private Hashtable implementorCache_;
    private ServletContext servletContext_;
    private JaxRpcObjectFactory rpcFactory_;

    public ImplementorCacheDelegateImpl(ServletConfig servletConfig) {
        servletContext_ = servletConfig.getServletContext();
        implementorCache_ = new Hashtable();
        rpcFactory_ = JaxRpcObjectFactory.newInstance();
    }

    public Implementor getImplementorFor(RuntimeEndpointInfo targetEndpoint) {

        Implementor implementor = null;
        try {
            synchronized(targetEndpoint) {
                implementor = (Implementor) 
                    implementorCache_.get(targetEndpoint);
                if( implementor == null ) {
                    implementor = createImplementor(targetEndpoint);
                    implementorCache_.put(targetEndpoint, implementor);
                }
            }

            WebServiceContractImpl wscImpl = WebServiceContractImpl.getInstance();
            InvocationManager invManager = wscImpl.getInvocationManager();
            ComponentInvocation inv = invManager.getCurrentInvocation();
            if (inv instanceof EJBInvocation)
                ((EJBInvocation)inv).setWebServiceTie(implementor.getTie());
        } catch(Throwable t) {
            RuntimeException re = new RuntimeException();
            re.initCause(t);
            throw re;
        }

        return implementor;
    }

    public void releaseImplementor(RuntimeEndpointInfo targetEndpoint, 
                                   Implementor implementor) {
        // do nothing
    }

    public void destroy() {
        for (Iterator iter = implementorCache_.values().iterator(); 
             iter.hasNext();) {
            Implementor implementor = (Implementor) iter.next();
            try {
                implementor.destroy();
            } catch(Throwable t) {
                // @@@ log
            }
        }
        implementorCache_.clear();
    }

    private Implementor createImplementor(RuntimeEndpointInfo targetEndpoint) 
        throws Exception {

        Tie tie = (Tie) targetEndpoint.getTieClass().newInstance();

        Class seiClass  = targetEndpoint.getRemoteInterface();
        Class implClass = targetEndpoint.getImplementationClass();

        Remote servant  = null;
        if( seiClass.isAssignableFrom(implClass) ) {
            // if servlet endpoint impl is a subtype of SEI, use an
            // instance as the servant.
            servant = (Remote) implClass.newInstance();
        } else {
            // Create a dynamic proxy that implements SEI (and optionally
            // ServiceLifecycle) and delegates to an instance of the 
            // endpoint impl.
            Object implInstance = implClass.newInstance();

            InvocationHandler handler = 
                new ServletImplInvocationHandler(implInstance);
            boolean implementsLifecycle = 
                ServiceLifecycle.class.isAssignableFrom(implClass);
            Class[] proxyInterfaces = implementsLifecycle ?
                new Class[] { seiClass, ServiceLifecycle.class } :
                new Class[] { seiClass };

            servant = (Remote) Proxy.newProxyInstance
                (implClass.getClassLoader(), proxyInterfaces, handler);
        }
        tie.setTarget(servant);
        
        Implementor implementor = rpcFactory_.createImplementor(servletContext_, tie);
        implementor.init();

        return implementor;
    }
}
