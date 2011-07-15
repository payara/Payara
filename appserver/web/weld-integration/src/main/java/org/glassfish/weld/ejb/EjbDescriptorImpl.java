/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2011 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.weld.ejb;

import org.jboss.weld.ejb.spi.BusinessInterfaceDescriptor;
import org.jboss.weld.ejb.SessionBeanInterceptor;

import javax.ejb.Local;
import javax.ejb.Remote;
import javax.interceptor.InvocationContext;

import com.sun.enterprise.deployment.*;

import java.lang.reflect.Method;
import java.lang.annotation.Annotation;

import java.util.Collection;
import java.util.Set;
import java.util.HashSet;

/**
 */
public class EjbDescriptorImpl<T> implements org.jboss.weld.ejb.spi.EjbDescriptor<T>
{

    private EjbDescriptor ejbDesc;

    public EjbDescriptorImpl(EjbDescriptor e) {
        ejbDesc = e;

        // Before handling application-level interceptors that are defined using 299 metadata,
        // add the CDI impl-specific system-level interceptor that needs to be registered for ALL
        // EJB components.  At runtime, this sets up the appropriate request context for invocations that
        // do not originate via the web tier.  This interceptor must be registered *before*
        // any application-level interceptors in the chain, so add it in the framework interceptor
        // category within the ejb descriptor.
        EjbInterceptor systemLevelCDIInterceptor = createSystemLevelCDIInterceptor();
        ejbDesc.addFrameworkInterceptor(systemLevelCDIInterceptor);
    }


    public String getEjbName() {
        return ejbDesc.getName();
    }

    public EjbDescriptor getEjbDescriptor() {
        return ejbDesc;
    }

    public Class<T> getBeanClass() {
        Class beanClassType = null;
	    try {

            beanClassType =
              ejbDesc.getEjbBundleDescriptor().getClassLoader().loadClass(ejbDesc.getEjbClassName());

        } catch(ClassNotFoundException e) {
            throw new IllegalStateException(e);
        }

        return beanClassType;
    }

   /**
    * Gets the local business interfaces of the EJB
    * 
    * @return An iterator over the local business interfaces
    */
    public Collection<BusinessInterfaceDescriptor<?>> getLocalBusinessInterfaces() {
       
        Set<BusinessInterfaceDescriptor<?>> localBusIntfs = new HashSet<BusinessInterfaceDescriptor<?>>();

        if( ejbDesc.getType().equals(EjbSessionDescriptor.TYPE) ) {

            EjbSessionDescriptor sessionDesc = (EjbSessionDescriptor) ejbDesc;
            Set<String> localNames = sessionDesc.getLocalBusinessClassNames();

            // Add superinterfaces that are also marked as Local
            Set<String> extraNames = new HashSet<String>();
            for(String local : localNames) {
                try {
                    Class localClass = sessionDesc.getEjbBundleDescriptor().getClassLoader().loadClass(local);
                    addIfLocal(localClass.getInterfaces(), extraNames);
                } catch(ClassNotFoundException e) {
                    throw new IllegalStateException(e);
                }
            }

            localNames.addAll(extraNames);

            // Include the no-interface Local view
            if( sessionDesc.isLocalBean() ) {
                localNames.add(sessionDesc.getEjbClassName());
            }


            for(String local : localNames) {
                try {

                    Class localClass = sessionDesc.getEjbBundleDescriptor().getClassLoader().loadClass(local);
                    BusinessInterfaceDescriptor busIntfDesc =
                            new BusinessInterfaceDescriptorImpl(localClass);
                    localBusIntfs.add(busIntfDesc);

                } catch(ClassNotFoundException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
       
        return localBusIntfs;
    }
   
   /**
    * Get the remove methods of the EJB
    * 
    * @return An iterator over the remove methods
    */
    public Collection<Method> getRemoveMethods() {
        Set<Method> removeMethods = new HashSet<Method>();

        if( ejbDesc.getType().equals(EjbSessionDescriptor.TYPE) ) {
            EjbSessionDescriptor sessionDesc = (EjbSessionDescriptor) ejbDesc;
            if( sessionDesc.isStateful() && sessionDesc.hasRemoveMethods() ) {

                for(EjbRemovalInfo next : sessionDesc.getAllRemovalInfo()) {

                    MethodDescriptor mDesc = next.getRemoveMethod();
                    Method m = mDesc.getMethod(ejbDesc);
                    if( m == null ) {
                        throw new IllegalStateException("Can't resolve remove method " +
                        mDesc + " For EJB " + sessionDesc.getName());
                    }
                    removeMethods.add(m);

                }

            }
        }

       return removeMethods;

    }

   /**
    * Indicates if the bean is stateless
    * 
    * @return True if stateless, false otherwise
    */
    public boolean isStateless() {
	    return (ejbDesc.getType().equals(EjbSessionDescriptor.TYPE) &&
               ((EjbSessionDescriptor) ejbDesc).isStateless());
    }

   /**
    * Indicates if the bean is a EJB 3.1 Singleton
    * 
    * @return True if the bean is a singleton, false otherwise
    */
    public boolean isSingleton() {
       return (ejbDesc.getType().equals(EjbSessionDescriptor.TYPE) &&
               ((EjbSessionDescriptor) ejbDesc).isSingleton());
    }

   /**
    * Indicates if the EJB is stateful
    * 
    * @return True if the bean is stateful, false otherwise
    */
    public boolean isStateful() {
        return (ejbDesc.getType().equals(EjbSessionDescriptor.TYPE) &&
               ((EjbSessionDescriptor) ejbDesc).isStateful());
    }

   /**
    * Indicates if the EJB is and MDB
    * 
    * @return True if the bean is an MDB, false otherwise
    */
    public boolean isMessageDriven() {
	    return (ejbDesc.getType().equals(EjbMessageBeanDescriptor.TYPE));
    }

    /*  enabled for debugging 
    public int hashCode() {
        return getEjbName().hashCode();
    }

    public boolean equals(Object o) {

        boolean equal = false;

        if( (o != null) && (o instanceof EjbDescriptorImpl) ) {

            equal = getEjbName().equals( ((EjbDescriptorImpl)o).getEjbName() );

        }

        return equal;

    }
    */

    private void addIfLocal(Class[] interfaces, Set<String> names) {
        for(Class next : interfaces) {
            if( next.getAnnotation(Local.class) != null ) {
                names.add(next.getName());
            }
            addIfLocal(next.getInterfaces(), names);
        }
    }
    
    private void addIfRemote(Class[] interfaces, Set<String> names) {
        for(Class next : interfaces) {
            if( next.getAnnotation(Remote.class) != null ) {
                names.add(next.getName());
            }
            addIfRemote(next.getInterfaces(), names);
        }
    }
    

    private EjbInterceptor createSystemLevelCDIInterceptor() {

        EjbInterceptor interceptor = new EjbInterceptor();

        Class interceptorClass = SessionBeanInterceptor.class;

        String interceptorName = interceptorClass.getName();

        interceptor.setInterceptorClass(interceptorClass);

        // @@@ SessionBeanInterceptor.class no longer has @AroundInvoke annotation so
        // we have to look for method explicitly.
        try {
            Method aroundInvokeMethod = interceptorClass.getMethod("aroundInvoke", InvocationContext.class);

            if( aroundInvokeMethod != null ) {
                LifecycleCallbackDescriptor aroundInvokeDesc = new LifecycleCallbackDescriptor();
                aroundInvokeDesc.setLifecycleCallbackClass(interceptorName);
                aroundInvokeDesc.setLifecycleCallbackMethod(aroundInvokeMethod.getName());
                interceptor.addAroundInvokeDescriptor(aroundInvokeDesc);

                // TODO BUG -- Existing SessionBeanInterceptor does not define an @AroundTimeout method.
                // Until that's fixed, work around it by adding the method marked @AroundInvoke as an
                // @AroundTimeout.
                LifecycleCallbackDescriptor aroundTimeoutDesc = new LifecycleCallbackDescriptor();
                aroundTimeoutDesc.setLifecycleCallbackClass(interceptorName);
                aroundTimeoutDesc.setLifecycleCallbackMethod(aroundInvokeMethod.getName());
                interceptor.addAroundTimeoutDescriptor(aroundTimeoutDesc);
            }
        } catch(NoSuchMethodException nsme) {
            throw new RuntimeException("Cannot find weld EJB interceptor aroundInvoke method");
        }

        return interceptor;

    }

    private Method getMethodForMethodAnnotation(Class c, String annotationClassName) {
        for(Method m : c.getDeclaredMethods()) {
            for(Annotation next : m.getDeclaredAnnotations()) {

                if( next.annotationType().getName().equals(annotationClassName)) {
                    return m;
                }
            }
        }
        return null;
    }


    @Override
    public Collection<BusinessInterfaceDescriptor<?>> getRemoteBusinessInterfaces() {
        Set<BusinessInterfaceDescriptor<?>> remoteBusIntfs = new HashSet<BusinessInterfaceDescriptor<?>>();

        if( ejbDesc.getType().equals(EjbSessionDescriptor.TYPE) ) {

            EjbSessionDescriptor sessionDesc = (EjbSessionDescriptor) ejbDesc;
            Set<String> remoteNames = sessionDesc.getRemoteBusinessClassNames();

            // Add superinterfaces that are also marked as Local
            Set<String> extraNames = new HashSet<String>();
            for(String local : remoteNames) {
                try {
                    Class remoteClass = sessionDesc.getEjbBundleDescriptor().getClassLoader().loadClass(local);
                    addIfRemote(remoteClass.getInterfaces(), extraNames);
                } catch(ClassNotFoundException e) {
                    throw new IllegalStateException(e);
                }
            }

            remoteNames.addAll(extraNames);

            // Include the no-interface Local view
            if( sessionDesc.isLocalBean() ) {
                remoteNames.add(sessionDesc.getEjbClassName());
            }


            for(String remote : remoteNames) {
                try {

                    Class remoteClass = sessionDesc.getEjbBundleDescriptor().getClassLoader().loadClass(remote);
                    BusinessInterfaceDescriptor busIntfDesc =
                            new BusinessInterfaceDescriptorImpl(remoteClass);
                    remoteBusIntfs.add(busIntfDesc);

                } catch(ClassNotFoundException e) {
                    throw new IllegalStateException(e);
                }
            }
        }
       
        return remoteBusIntfs;
    }
    
    @Override
    public String toString(){
        return ejbDesc.getEjbClassName();
    }
}
