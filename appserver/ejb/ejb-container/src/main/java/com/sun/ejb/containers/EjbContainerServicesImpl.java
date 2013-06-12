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

package com.sun.ejb.containers;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Proxy;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ejb.EJBException;
import javax.ejb.NoSuchEJBException;

import org.glassfish.ejb.LogFacade;
import org.glassfish.ejb.api.EjbContainerServices;
import org.glassfish.ejb.deployment.descriptor.EjbDescriptor;
import org.glassfish.ejb.deployment.descriptor.EjbSessionDescriptor;
import org.jvnet.hk2.annotations.Service;

import com.sun.ejb.Container;
import com.sun.ejb.EJBUtils;
import com.sun.ejb.spi.container.OptionalLocalInterfaceProvider;
import com.sun.enterprise.deployment.EjbInterceptor;
import com.sun.logging.LogDomains;

/**
 *
 */
@Service
public class EjbContainerServicesImpl implements EjbContainerServices {



    public <S> S  getBusinessObject(Object ejbRef, java.lang.Class<S> businessInterface) {

        EJBLocalObjectImpl localObjectImpl = getEJBLocalObject(ejbRef);

        if( localObjectImpl == null ) {
            throw new IllegalStateException("Invalid ejb ref");
        }

        Container container = localObjectImpl.getContainer();
        EjbDescriptor ejbDesc = container.getEjbDescriptor();

        S businessObject = null;
        
        if (businessInterface != null) {
            String intfName = businessInterface.getName();
            if (ejbDesc.getLocalBusinessClassNames().contains(intfName)) {

                // Get proxy corresponding to this business interface.
                businessObject = (S) localObjectImpl.getClientObject(intfName);

            } else if( ejbDesc.isLocalBean()) {
                //If this is a no-interface view session bean, the bean
                //can be accessed through interfaces in its superclass as well
                boolean isValidBusinessInterface =
                    ejbDesc.getNoInterfaceLocalBeanClasses().contains(intfName);
                if ((intfName.equals(ejbDesc.getEjbClassName())) 
                        || isValidBusinessInterface) {  
                    businessObject = (S) localObjectImpl.getClientObject(ejbDesc.getEjbClassName());
                }
                
            }
        }

        if( businessObject == null ) {
            throw new IllegalStateException("Unable to convert ejbRef for ejb " +
            ejbDesc.getName() + " to a business object of type " + businessInterface);
        }        

        return businessObject;

    }

    public void remove(Object ejbRef) {

        EJBLocalObjectImpl localObjectImpl = getEJBLocalObject(ejbRef);

        if( localObjectImpl == null ) {
            throw new UnsupportedOperationException("Invalid ejb ref");
        }

        Container container = localObjectImpl.getContainer();
        EjbDescriptor ejbDesc = container.getEjbDescriptor();
        boolean isStatefulBean = false;

        if( ejbDesc.getType().equals(EjbSessionDescriptor.TYPE) ) {

            EjbSessionDescriptor sessionDesc = (EjbSessionDescriptor) ejbDesc;
            isStatefulBean = sessionDesc.isStateful();

        }

        if( !isStatefulBean ) {

             // TODO 299 impl may incorrectly call this for stateless/singleton
            // beans.  Until it's fixed just treat it as a no-op. Otherwise, any app acquiring
            // stateless/singleton references via 299 could fail until bug is fixed.
            return;

            // TODO reenable this after bug is fixed
            //throw new UnsupportedOperationException("ejbRef for ejb " +
             //       ejbDesc.getName() + " is not a stateful bean ");
        }

        try {
            localObjectImpl.remove();
        } catch(EJBException e) {
            LogFacade.getLogger().log(Level.FINE, "EJBException during remove. ", e);    
        } catch(javax.ejb.RemoveException re) {
            throw new NoSuchEJBException(re.getMessage(), re);
        }

    }


    public boolean isRemoved(Object ejbRef) {

        EJBLocalObjectImpl localObjectImpl = getEJBLocalObject(ejbRef);

        if( localObjectImpl == null ) {
            throw new UnsupportedOperationException("Invalid ejb ref");
        }

        Container container = localObjectImpl.getContainer();
        EjbDescriptor ejbDesc = container.getEjbDescriptor();
        boolean isStatefulBean = false;

        if( ejbDesc.getType().equals(EjbSessionDescriptor.TYPE) ) {

            EjbSessionDescriptor sessionDesc = (EjbSessionDescriptor) ejbDesc;
            isStatefulBean = sessionDesc.isStateful();

        }

        if( !isStatefulBean ) {

            // TODO 299 impl is incorrectly calling isRemoved for stateless/singleton
            // beans.  Until it's fixed just return false. Otherwise, any app acquiring
            // stateless/singleton references via 299 will fail until bug is fixed.
            return false;

            // TODO reenable this per SessionObjectReference.isRemoved SPI
            //throw new UnsupportedOperationException("ejbRef for ejb " +
             //   ejbDesc.getName() + " is not a stateful bean ");
        }

        boolean removed = false;

        try {
            ((BaseContainer)container).checkExists(localObjectImpl);    
        } catch(Exception e) {
            removed = true;
        }

        return removed;

    }

    private EJBLocalObjectImpl getEJBLocalObject(Object ejbRef) {

        // ejbRef is assumed to be either a local business view or
        // no-interface view

        EJBLocalObjectInvocationHandlerDelegate localObj = null;

        // First try to convert it as a local or remote business interface object
        try {

            localObj = (EJBLocalObjectInvocationHandlerDelegate) Proxy.getInvocationHandler(ejbRef);
            
        } catch(IllegalArgumentException iae) {

            Proxy proxy;

            if( ejbRef instanceof OptionalLocalInterfaceProvider ) {

                try {

                     Field proxyField = ejbRef.getClass().getDeclaredField("__ejb31_delegate");

                     final Field finalF = proxyField;
                        java.security.AccessController.doPrivileged(
                        new java.security.PrivilegedExceptionAction() {
                            public java.lang.Object run() throws Exception {
                                if( !finalF.isAccessible() ) {
                                    finalF.setAccessible(true);
                                }
                                return null;
                            }
                        });

                      proxy = (Proxy) proxyField.get(ejbRef);

                } catch(Exception e) {

                    throw new IllegalArgumentException("Invalid ejb ref", e);
                }

                              
                try {

                    localObj = (EJBLocalObjectInvocationHandlerDelegate)
                            Proxy.getInvocationHandler(proxy);

                } catch(IllegalArgumentException i) {}                      

            }
        }

        return (localObj != null) ?  localObj.getDelegate() : null;
    }

    public boolean isEjbManagedObject(Object desc, Class c) {

        String className = c.getName();

        EjbDescriptor ejbDesc = (EjbDescriptor) desc;

        Set<String> ejbManagedObjectClassNames = new HashSet<String>();
        ejbManagedObjectClassNames.add(ejbDesc.getEjbClassName());

        for(EjbInterceptor next : ejbDesc.getInterceptorClasses()) {
            if( !next.isCDIInterceptor() ) {
                ejbManagedObjectClassNames.add(next.getInterceptorClassName());
            }
        }

        Set<String> serializableClassNames = new HashSet<String>();

        for(String next : ejbManagedObjectClassNames) {
            // Add the serializable sub-class version of each name as well
            serializableClassNames.add(EJBUtils.getGeneratedSerializableClassName(next));
        }

        boolean isEjbManagedObject = ejbManagedObjectClassNames.contains(className) ||
                serializableClassNames.contains(className);

        return isEjbManagedObject;

    }

}
