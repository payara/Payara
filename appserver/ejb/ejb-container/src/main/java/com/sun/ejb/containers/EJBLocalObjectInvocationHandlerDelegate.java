/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2014 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.enterprise.container.common.spi.util.IndirectlySerializable;
import com.sun.enterprise.container.common.spi.util.SerializableObjectFactory;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

/**
 * This class is used as a "proxy" or adapter between the business interface
 *  proxy and the EJBLocalObjectInvocationHandler. An instance of this class
 *  is created for each business interface of a bean. All java.lang.Object
 *  methods and mthods of IndirectlySerializable are handled by this 
 *  InvocationHandler itself while the business interface methods are delegated
 *  to the delegate (which is the EJBLocalObjectInvocaionHandler). 
 *   
 * @author Mahesh Kannan
 *
 */
public class EJBLocalObjectInvocationHandlerDelegate
    implements InvocationHandler {

    private Class intfClass;
    private long containerId;
    private EJBLocalObjectInvocationHandler delegate;
    private boolean isOptionalLocalBusinessView;
    
    EJBLocalObjectInvocationHandlerDelegate(Class intfClass, long containerId,
            EJBLocalObjectInvocationHandler delegate) {
        this.intfClass = intfClass;
        this.containerId = containerId;
        this.delegate = delegate;
        this.isOptionalLocalBusinessView = delegate.isOptionalLocalBusinessView();
    }
    
    public Object invoke(Object proxy, Method method, Object[] args) 
        throws Throwable {
        
        Class methodClass = method.getDeclaringClass();
        Object result = null;
        if( methodClass == java.lang.Object.class ) {
            result = InvocationHandlerUtil.invokeJavaObjectMethod
                (this, method, args);
        } else if( methodClass == IndirectlySerializable.class ) {
            result = this.getSerializableObjectFactory();
        }else {
            result = delegate.invoke(intfClass, method, args);
        }
        
        return result;
    }

    EJBLocalObjectInvocationHandler getDelegate() {
        return delegate;
    }
    
    public int hashCode() {
        return (int) containerId;
    }
    
    public boolean equals(Object other) {
        boolean result = false;
        
        if ((other != null)
        && (other instanceof EJBLocalObjectInvocationHandlerDelegate)) {
            EJBLocalObjectInvocationHandlerDelegate otherDelegate
                    = (EJBLocalObjectInvocationHandlerDelegate) other;
            if ((containerId == otherDelegate.containerId)
            && (intfClass == otherDelegate.intfClass)) {
                EJBLocalObjectInvocationHandler otherHandler
                    = otherDelegate.delegate;
                result = (delegate.getKey() != null)
                    ? delegate.getKey().equals(otherHandler.getKey())
                    : (otherHandler.getKey() == null);
            }
        }
        
        return result;
    }

    public String toString() {
        return intfClass.getName() + "_" + System.identityHashCode(this);
    }
    
    public SerializableObjectFactory getSerializableObjectFactory() {
        // Note: for stateful SessionBeans, the EJBLocalObjectImpl contains
        // a pointer to the EJBContext. We should not serialize it here.
        
        return new SerializableLocalObjectDelegate(
            containerId, intfClass.getName(), delegate.getKey(),
            isOptionalLocalBusinessView,
            delegate.getSfsbClientVersion());
    }
    
    private static final class SerializableLocalObjectDelegate
        implements SerializableObjectFactory
    {
        private long containerId;
        private String intfClassName;
        private Object primaryKey;
        private boolean isOptionalLocalBusinessView;
        private long version = 0L; //Used only for SFSBs
        
        SerializableLocalObjectDelegate(long containerId, 
                String intfClassName, Object primaryKey, boolean isOptionalLocalBusView, long version) {
            this.containerId = containerId;
            this.intfClassName = intfClassName;
            this.primaryKey = primaryKey;
            this.isOptionalLocalBusinessView = isOptionalLocalBusView;
            this.version = version;
        }
        
        public Object createObject()
            throws IOException
        {
            BaseContainer container = EjbContainerUtilImpl.getInstance().getContainer(containerId);
            EJBLocalObjectImpl ejbLocalBusinessObjectImpl = isOptionalLocalBusinessView ?
                container.getOptionalEJBLocalBusinessObjectImpl(primaryKey) :
                container.getEJBLocalBusinessObjectImpl(primaryKey);
            ejbLocalBusinessObjectImpl.setSfsbClientVersion(version);
            // Return the client EJBLocalObject.

            return isOptionalLocalBusinessView ?
                ejbLocalBusinessObjectImpl.getOptionalLocalBusinessClientObject() :
                ejbLocalBusinessObjectImpl.getClientObject(intfClassName);
        }
    }
}
