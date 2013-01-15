/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.xml.ws.api.server.ResourceInjector;
import com.sun.xml.ws.api.server.WSWebServiceContext;
import com.sun.enterprise.deployment.WebServiceEndpoint;
import com.sun.enterprise.deployment.WebBundleDescriptor;
import com.sun.enterprise.deployment.ResourceReferenceDescriptor;
import com.sun.enterprise.deployment.InjectionTarget;

import com.sun.enterprise.container.common.spi.util.InjectionException;
import com.sun.enterprise.container.common.spi.util.InjectionManager;

import java.util.Iterator;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.ws.WebServiceException;

import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.api.invocation.ComponentInvocation;


/**
 * JAXWS Container call back to inject servlet endpoints
 */

public class ResourceInjectorImpl extends ResourceInjector {
    
    private WebServiceEndpoint endpoint;
    private ComponentInvocation inv;
    private InvocationManager invMgr;
    private static final Logger logger = LogUtils.getLogger();

    public ResourceInjectorImpl(WebServiceEndpoint ep) {

        WebServiceContractImpl    wscImpl = WebServiceContractImpl.getInstance();
        invMgr =  wscImpl.getInvocationManager();
        inv = invMgr.getCurrentInvocation();      
        endpoint = ep;

    }
    
    public void inject(WSWebServiceContext context, Object instance)
                    throws WebServiceException {

        try {
            // Set proper component context
            invMgr.preInvoke(inv);
            // Injection first
            InjectionManager injManager = WebServiceContractImpl.getInstance().getInjectionManager();
            injManager.injectInstance(instance);

            // Set webservice context here
            // If the endpoint has a WebServiceContext with @Resource then
            // that has to be used
            WebServiceContextImpl wsc = null;
            WebBundleDescriptor bundle = (WebBundleDescriptor)endpoint.getBundleDescriptor();
            Iterator<ResourceReferenceDescriptor> it = bundle.getResourceReferenceDescriptors().iterator();
            while(it.hasNext()) {
                ResourceReferenceDescriptor r = it.next();
                if(r.isWebServiceContext()) {
                    Iterator<InjectionTarget> iter = r.getInjectionTargets().iterator();
                    boolean matchingClassFound = false;
                    while(iter.hasNext()) {
                        InjectionTarget target = iter.next();
                        if(endpoint.getServletImplClass().equals(target.getClassName())) {
                            matchingClassFound = true;
                            break;
                        }
                    }
                    if(!matchingClassFound) {
                        continue;
                    }
                    try {
                        javax.naming.InitialContext ic = new javax.naming.InitialContext();
                        wsc = (WebServiceContextImpl) ic.lookup("java:comp/env/" + r.getName());
                    } catch (Throwable t) {
                        // Do something here
                        if (logger.isLoggable(Level.FINE)) {
                            logger.log(Level.FINE, LogUtils.EXCEPTION_THROWN, t);
                        }
                    }
                    if(wsc != null) {
                        wsc.setContextDelegate(context);
                        //needed to support isUserInRole() on WSC;
                        wsc.setServletName(bundle.getWebComponentDescriptors());

                    }
                }
            }
        } catch (InjectionException ie) {
            throw new WebServiceException(ie);
        } finally {
            invMgr.postInvoke(inv);
        }
    }
}
