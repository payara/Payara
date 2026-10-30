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
// Portions Copyright [2019-2024] [Payara Foundation and/or its affiliates]

package com.sun.ejb.containers;

import com.sun.ejb.Container;
import com.sun.ejb.EjbInvocation;
import com.sun.ejb.InvocationInfo;
import com.sun.ejb.containers.util.MethodMap;
import com.sun.enterprise.container.common.spi.util.IndirectlySerializable;
import com.sun.enterprise.deployment.EjbDescriptor;
import com.sun.enterprise.deployment.EjbSessionDescriptor;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.enterprise.util.Utility;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.logging.Logger;

import jakarta.ejb.EJBException;
import jakarta.ejb.EJBLocalHome;

/**
 * Handler for EJBLocalHome invocations through EJBLocalHome proxy.
 *
 * @author Kenneth Saks
 */
public class EJBLocalHomeInvocationHandler extends EJBLocalHomeImpl implements InvocationHandler {

    private static final Logger LOG = Logger.getLogger(EJBLocalHomeInvocationHandler.class.getName());

    private final boolean isStatelessSession;

    // Our associated proxy object.  Used when a caller needs EJBLocalObject
    // but only has InvocationHandler.
    private EJBLocalHome proxy;

    private final Class localHomeIntfClass;

    // Cache reference to invocation info.  There is one of these per
    // container.  It's populated during container initialization and
    // passed in when the InvocationHandler is created.  This avoids the
    // overhead of building the method info each time a LocalHome proxy
    // is created.
    private MethodMap invocationInfoMap;


    protected EJBLocalHomeInvocationHandler(EjbDescriptor ejbDescriptor, Class localHomeIntf) throws Exception {

        if (ejbDescriptor instanceof EjbSessionDescriptor) {
            this.isStatelessSession = ((EjbSessionDescriptor) ejbDescriptor).isStateless();
        } else {
            this.isStatelessSession = false;
        }

        this.localHomeIntfClass = localHomeIntf;

        // NOTE : Container is not set on super-class until after
        // constructor is called.
    }

    public void setMethodMap(MethodMap map) {
        this.invocationInfoMap = map;
    }

    public void setProxy(EJBLocalHome proxy) {
        this.proxy = proxy;
    }

    @Override
    protected EJBLocalHome getEJBLocalHome() {
        return proxy;
    }

    /**
     * Called by EJBLocalHome proxy.
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        LOG.finest(() -> String.format("invoke(proxy=%s, method=%s, args=%s)", proxy, method, Arrays.toString(args)));
        ClassLoader originalClassLoader = null;

        // NOTE : be careful with "args" parameter. It is null
        // if method signature has 0 arguments.
        try {
            getContainer().onEnteringContainer();

            // In some cases(e.g. CDI + OSGi combination) ClassLoader
            // is accessed from the current Thread. In those cases we need to set
            // the context classloader to the application's classloader before
            // proceeding. Otherwise, the context classloader could still
            // reflect the caller's class loader.

            if (Thread.currentThread().getContextClassLoader() != getContainer().getClassLoader()) {
                originalClassLoader = Utility.setContextClassLoader(getContainer().getClassLoader());
            }

            final Class methodClass = method.getDeclaringClass();

            if (methodClass == java.lang.Object.class) {
                return InvocationHandlerUtil.invokeJavaObjectMethod(this, method, args);
            } else if (methodClass == IndirectlySerializable.class) {
                return this.getSerializableObjectFactory();
            } else if (handleSpecialEJBLocalHomeMethod(method, methodClass)) {
                return invokeSpecialEJBLocalHomeMethod(method, methodClass, args);
            }

            // Use optimized version of get that takes param count as an argument.
            final InvocationInfo invInfo = (InvocationInfo) invocationInfoMap.get(method,
                (args == null ? 0 : args.length));

            if (invInfo == null) {
                throw new IllegalStateException("Unknown method: " + method);
            }

            if ((methodClass == jakarta.ejb.EJBLocalHome.class) || invInfo.ejbIntfOverride) {
                // There is exactly one argument on jakarta.ejb.EJBLocalHome: primaryKey
                super.remove(args[0]);
                return null;

            } else if (methodClass == GenericEJBLocalHome.class) {

                // This is a creation request through the EJB 3.0
                // client view, so just create a local business object and
                // return it.
                final EJBLocalObjectImpl localImpl = createEJBLocalBusinessObjectImpl((String) args[0]);
                return localImpl.getClientObject((String) args[0]);

            }

            // Process finder, create method, or home method.
            EJBLocalObjectImpl localObjectImpl = null;
            if (invInfo.startsWithCreate) {
                localObjectImpl = createEJBLocalObjectImpl();
                if (localObjectImpl != null) {
                    if (isStatelessSession) {
                        return localObjectImpl.getClientObject();
                    }
                }
            }

            if (isStatelessSession) {
                return null;
            }

            if (invInfo.targetMethod1 == null) {
                final LocalStringManagerImpl msgs = new LocalStringManagerImpl(EJBLocalHomeInvocationHandler.class);
                final Object[] params = new Object[] {invInfo.ejbName, "LocalHome", invInfo.method.toString()};
                final String errorMsg = msgs.getLocalString("ejb.bean_class_method_not_found", "", params);
                throw new EJBException(errorMsg);
            }

            final EjbInvocation inv = getContainer().createEjbInvocation();

            inv.isLocal = true;
            inv.isHome = true;
            inv.method = method;

            inv.clientInterface = localHomeIntfClass;

            // Set cached invocation params. This will save additional lookups
            // in BaseContainer.
            inv.transactionAttribute = invInfo.txAttr;
            inv.invocationInfo = invInfo;

            if (localObjectImpl != null && invInfo.startsWithCreate) {
                inv.ejbObject = localObjectImpl;
            }

            final Object returnValue = invokeBean(args, invInfo, inv);
            if (inv.exception != null) {
                InvocationHandlerUtil.throwLocalException(inv.exception, method.getExceptionTypes());
            }
            return returnValue;
        } finally {
            if (originalClassLoader != null) {
                Utility.setContextClassLoader(originalClassLoader);
            }

            getContainer().onLeavingContainer();
        }
    }

    private Object invokeBean(Object[] args, final InvocationInfo invInfo, final EjbInvocation inv) {
        try {
            container.preInvoke(inv);
            if (invInfo.startsWithCreate) {
                final Object ejbCreateReturnValue = invokeTargetBeanMethod(container, invInfo.targetMethod1, inv,
                    inv.ejb, args);
                postCreate(container, inv, invInfo, ejbCreateReturnValue, args);
                if (inv.ejbObject == null) {
                    return null;
                }
                return ((EJBLocalObjectImpl) inv.ejbObject).getClientObject();
            } else if (invInfo.startsWithFindByPrimaryKey) {
                return container.invokeFindByPrimaryKey(invInfo.targetMethod1, inv, args);
            } else if (invInfo.startsWithFind) {
                final Object pKeys = invokeTargetBeanMethod(container, invInfo.targetMethod1, inv, inv.ejb, args);
                return container.postFind(inv, pKeys, null);
            } else {
                return invokeTargetBeanMethod(container, invInfo.targetMethod1, inv, inv.ejb, args);
            }
        } catch (final InvocationTargetException ite) {
            inv.exception = ite.getCause();
            return null;
        } catch (final Throwable c) {
            inv.exception = c;
            return null;
        } finally {
            container.postInvoke(inv);
        }
    }

    // default impl to be overridden in subclasses if special invoke is necessary
    protected boolean handleSpecialEJBLocalHomeMethod(Method method, Class methodClass) {
        return false;
    }

    // default impl to be overridden in subclasses if special invoke is necessary
    protected Object invokeSpecialEJBLocalHomeMethod(Method method, Class methodClass,
            Object[] args) throws Throwable {
        return null;
    }

    // default impl to be overridden in subclass if necessary
    protected void postCreate(Container container, EjbInvocation inv,
            InvocationInfo invInfo, Object primaryKey, Object[] args)
            throws Throwable {
    }

    // allow subclasses to execute a protected method in BaseContainer
    protected Object invokeTargetBeanMethod(BaseContainer container,
            Method beanClassMethod, EjbInvocation inv, Object target, Object[] params)
            throws Throwable {

        return container.invokeTargetBeanMethod(beanClassMethod, inv, target, params);
    }

}
