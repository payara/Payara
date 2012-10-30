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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Proxy;
import java.lang.reflect.Method;

import java.util.Iterator;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;
import java.util.logging.Level;

import java.rmi.RemoteException;

import javax.ejb.EJBException;
import javax.ejb.EJBObject;

import com.sun.ejb.EjbInvocation;
import com.sun.ejb.InvocationInfo;
import com.sun.ejb.Container;
import com.sun.ejb.containers.util.MethodMap;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.Utility;
import com.sun.logging.LogDomains;

/** 
 * Handler for EJBObject invocations through EJBObject proxy.
 * 
 *
 * @author Kenneth Saks
 */    

public final class EJBObjectInvocationHandler 
    extends EJBObjectImpl implements InvocationHandler {

    private static LocalStringManagerImpl localStrings =
        new LocalStringManagerImpl(EJBObjectInvocationHandler.class);

    // Cache reference to invocation info populated during container
    // initialization. This avoids the overhead of building the method 
    // info each time a proxy is created.  
    private MethodMap invocationInfoMap_;

    private Class remoteIntf_;

    /**
     * Constructor used for Remote Home view.
     */
    public EJBObjectInvocationHandler(MethodMap invocationInfoMap,
                                      Class remoteIntf)
        throws Exception {

        invocationInfoMap_ = invocationInfoMap;

        remoteIntf_ = remoteIntf;
        setIsRemoteHomeView(true);

        // NOTE : Container is not set on super-class until after 
        // constructor is called.
    }

    /**
     * Constructor used for Remote Business view.
     */
    public EJBObjectInvocationHandler(MethodMap invocationInfoMap)
        throws Exception {

        invocationInfoMap_ = invocationInfoMap;

        setIsRemoteHomeView(false);

        // NOTE : Container is not set on super-class until after 
        // constructor is called.
    }

    /**
     * This entry point is only used for the Remote Home view.
     */
    public Object invoke(Object proxy, Method method, Object[] args) 
        throws Throwable {

        return invoke(remoteIntf_, method, args);
    }

    Object invoke(Class clientInterface, Method method, Object[] args) 
        throws Throwable {

        ClassLoader originalClassLoader = null;

        // NOTE : be careful with "args" parameter.  It is null
        //        if method signature has 0 arguments.
        try {
            container.onEnteringContainer();

            // In some cases(e.g. if the Home/Remote interfaces appear in
            // a parent of the application's classloader), 
            // ServantLocator.preinvoke() will not be called by the
            // ORB, and therefore BaseContainer.externalPreInvoke will not have
            // been called for this invocation.  In those cases we need to set 
            // the context classloader to the application's classloader before 
            // proceeding. Otherwise, the context classloader could still 
            // reflect the caller's class loader.  
            
            if( Thread.currentThread().getContextClassLoader() != 
                getContainer().getClassLoader() ) {
                originalClassLoader = Utility.setContextClassLoader
                    (getContainer().getClassLoader());
            }
            
            Class methodClass = method.getDeclaringClass();
            if( methodClass == java.lang.Object.class ) {
                return InvocationHandlerUtil.invokeJavaObjectMethod
                    (this, method, args);    
            } 
            
            // Use optimized version of get that takes param count as an 
            // argument.
            InvocationInfo invInfo = (InvocationInfo)
                invocationInfoMap_.get(method, 
                                       ((args != null) ? args.length : 0) );
            
            if( invInfo == null ) {
                throw new RemoteException("Unknown Remote interface method :" 
                                          + method);
            }
            
            if( (methodClass == javax.ejb.EJBObject.class) ||
                invInfo.ejbIntfOverride ) {
                return invokeEJBObjectMethod(method.getName(), args);
            } else if( invInfo.targetMethod1 == null ) {
                Object [] params = new Object[] 
                    { invInfo.ejbName, "Remote", invInfo.method.toString() };
                String errorMsg = localStrings.getLocalString
                    ("ejb.bean_class_method_not_found", "", params);
              
                _logger.log(Level.SEVERE, "ejb.bean_class_method_not_found",
                       params);                                   
                throw new RemoteException(errorMsg);           
            }
            
            // Process application-specific method.
            
            Object returnValue = null;
            
            EjbInvocation inv = container.createEjbInvocation();
            
            inv.isRemote  = true;
            inv.isHome    = false;
            inv.isBusinessInterface = !isRemoteHomeView();
            inv.ejbObject = this;
            inv.method    = method;

            inv.clientInterface = clientInterface;

            // Set cached invocation params.  This will save additional lookups
            // in BaseContainer.
            inv.transactionAttribute = invInfo.txAttr;
            inv.invocationInfo = invInfo;
            inv.beanMethod = invInfo.targetMethod1;
            inv.methodParams = args;
            
            try {
                container.preInvoke(inv);
                returnValue = container.intercept(inv);
            } catch(InvocationTargetException ite) {
                inv.exception = ite.getCause();
                inv.exceptionFromBeanMethod = inv.exception;
            } catch(Throwable t) {
                inv.exception = t;
            } finally {
                container.postInvoke(inv);
                //purge ThreadLocals before the thread is returned to pool
                if (container.getSecurityManager() != null) {
                    container.getSecurityManager().resetPolicyContext();
                }
            }
            
            if (inv.exception != null) {
                InvocationHandlerUtil.throwRemoteException
                    (inv.exception, method.getExceptionTypes());
            }
            
            return returnValue;
        } finally {
            
            if( originalClassLoader != null ) {
                Utility.setContextClassLoader(originalClassLoader);
            }

            container.onLeavingContainer();
        }
    }


    private Object invokeEJBObjectMethod(String methodName, Object[] args)
        throws Exception
    {
        // Return value is null if target method returns void.
        Object returnValue = null;


        // NOTE : Might be worth optimizing this method check if it
        // turns out to be a bottleneck.  I don't think these methods
        // are called with the frequency that this would be an issue,
        // but it's worth considering.
        int methodIndex = -1;
        Exception caughtException = null;

        try {
            if( methodName.equals("getEJBHome") ) {
    
                methodIndex = container.EJBObject_getEJBHome;
                container.onEjbMethodStart(methodIndex);
                returnValue = super.getEJBHome();
    
            } else if( methodName.equals("getHandle") ) {
    
                methodIndex = container.EJBObject_getHandle;
                container.onEjbMethodStart(methodIndex);
                returnValue = super.getHandle();
    
            } else if( methodName.equals("getPrimaryKey") ) {
    
                methodIndex = container.EJBObject_getPrimaryKey;
                container.onEjbMethodStart(methodIndex);
                returnValue = super.getPrimaryKey();
    
            } else if( methodName.equals("isIdentical") ) {
    
                // boolean isIdentical(EJBObject)
                // Convert the param into an EJBObject.           
                EJBObject other = (EJBObject) args[0];
    
                methodIndex = container.EJBObject_isIdentical;
                container.onEjbMethodStart(methodIndex);
                returnValue = super.isIdentical(other);
    
            } else if( methodName.equals("remove") ) {
    
                methodIndex = container.EJBObject_remove;
                container.onEjbMethodStart(methodIndex);
                super.remove();
    
            } else {
    
                throw new RemoteException("unknown EJBObject method = " 
                                          + methodName);
            }

        } catch (Exception ex) {
            caughtException = ex;
            throw ex;
        } finally {
            if (methodIndex != -1) {
                container.onEjbMethodEnd(methodIndex, caughtException);
            }
        }

        return returnValue;
    }

}
