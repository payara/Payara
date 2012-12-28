/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.ejb.containers;

import java.rmi.UnmarshalException;
import javax.ejb.EJBException;
import javax.ejb.AccessLocalException;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.lang.reflect.Method;

import java.util.Map;
import java.util.logging.Logger;

import com.sun.ejb.EjbInvocation;
import com.sun.ejb.ComponentContext;
import com.sun.ejb.InvocationInfo;
import org.glassfish.api.invocation.InvocationManager;
import org.glassfish.ejb.api.EjbEndpointFacade;
import com.sun.ejb.Container;
import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.deployment.WebServiceEndpoint;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.logging.LogDomains;

/** 
 * This is a proxy invocation handler for web service ejb invocations.
 * A single instance of this invocation handler is used for all
 * web service invocations to a particular ejb endpoint, so it must support
 * concurrent use.
 *
 * @author Kenneth Saks
 */    

public final class WebServiceInvocationHandler extends EJBLocalRemoteObject 
    implements InvocationHandler {

    private WebServiceEndpoint endpoint_;
    private Class ejbClass_;
    private Class serviceEndpointIntfClass_;
    private InvocationManager invManager_;
    private boolean hasHandlers_;
    private Map invocationInfoMap_;

    private static final LocalStringManagerImpl localStrings =
        new LocalStringManagerImpl(WebServiceInvocationHandler.class);


    public WebServiceInvocationHandler(Class ejbClass, 
                                       WebServiceEndpoint endpoint,
                                       Class serviceEndpointIntfClass,
                                       EjbContainerUtil contUtil,
                                       Map invocationInfoMap) {
        ejbClass_ = ejbClass;
        serviceEndpointIntfClass_ = serviceEndpointIntfClass;
        endpoint_ = endpoint;
        hasHandlers_ = endpoint.hasHandlers();
        invManager_ = contUtil.getInvocationManager();
        invocationInfoMap_ = invocationInfoMap;
    }

    public Object invoke(Object proxy, Method method, Object[] args) 
        throws Throwable {
        try {
        container.onEnteringContainer();
        // NOTE : be careful with "args" parameter.  It is null
        //        if method signature has 0 arguments.

        Class methodClass = method.getDeclaringClass();
        if( methodClass == java.lang.Object.class )  {
            return InvocationHandlerUtil.
                invokeJavaObjectMethod(this, method, args);    
        }

        Object returnValue = null;
        // Invocation was created earlier in the web service dispatching
        EjbInvocation inv = (EjbInvocation) invManager_.getCurrentInvocation();

        try {
            inv.ejbObject = this;
            
            // things can become hairy here. This handler may have been created 
            // with a dummy SEI to satisfy the EJB container. In such cases, we must
            // find the right method object on the SIB.
            if (endpoint_.getServiceEndpointInterface().equals(ejbClass_.getName())) {
                // we need to substiture the method object
                method = ejbClass_.getMethod(method.getName(), method.getParameterTypes());
            }
            inv.method = method;
            inv.clientInterface = serviceEndpointIntfClass_;

            inv.invocationInfo = (InvocationInfo)
                invocationInfoMap_.get(inv.method);

            if( inv.invocationInfo == null ) {
                throw new EJBException
                    ("Web service Invocation Info lookup failed for " +
                     "method " + inv.method);
            }

            inv.transactionAttribute = inv.invocationInfo.txAttr;

	        // special handling of jaxrpc endpoints (identfied by mapping file)
            if(endpoint_.getWebService().hasMappingFile()) {
		
		        if( hasHandlers_ ) {
		            // Handler performed method authorization already
		        } else {

		            boolean authorized = container.authorize(inv);
		            if( !authorized ) {
			            throw new AccessLocalException
			                ("Client not authorized to access " + inv.method);
		            }
		        }
	        } else if ( hasHandlers_ ) {

		        // jaxws enpoint
		        // authorization was done in security pipe
                // Now that application handlers have run, do 
                // another method lookup and compare the results 
                // with the original one. This ensures that the 
                // application handlers have not changed
                // which method is invoked.

                Method methodBefore = inv.getWebServiceMethod();
                
                if (methodBefore != null && !methodBefore.equals(inv.method)) {
                    inv.exception = new UnmarshalException
			    (localStrings.getLocalString
			    ("enterprise.webservice.postHandlerMethodMismatch",
			    "Original Method {0} does not match post-handler method {1}",
			    new Object[] { methodBefore, inv.method }));
		        throw inv.exception;
		        }
            }


            ComponentContext ctx = container.getContext(inv);
            inv.context  = ctx;
            inv.ejb      = ctx.getEJB();
            inv.instance = inv.ejb;

            container.preInvokeTx(inv);

            // Enterprise Bean class doesn't necessarily implement
            // web service endpoint interface, so we can't directly
            // dispatch through the given method object.
            Method beanClassMethod = ejbClass_.getMethod
                (method.getName(), method.getParameterTypes());
            inv.beanMethod = beanClassMethod;
            inv.methodParams = args;
            returnValue = container.intercept(inv);
        } catch(NoSuchMethodException nsme) {
            inv.exception = nsme;
        } catch(InvocationTargetException ite) {
            inv.exception = ite.getCause();
        } catch(Throwable c) {
            inv.exception = c;
        } finally {
            
            if( inv.ejb != null ) {
                // Do post invoke tx processing so that a commit failure
                // will be visible to web service client.
                container.postInvokeTx(inv);
            }
        }
        if (inv.exception != null) {
            if(inv.exception instanceof java.lang.RuntimeException) {
                throw (java.lang.RuntimeException)inv.exception; 
            } else if (inv.exception instanceof Exception) {
                throw inv.exception;
            } else {
                EJBException ejbEx = new EJBException();
                ejbEx.initCause(inv.exception);
                throw ejbEx;
            }
        }
        return returnValue;
        } finally {
            container.onLeavingContainer();
        }
    }
}
