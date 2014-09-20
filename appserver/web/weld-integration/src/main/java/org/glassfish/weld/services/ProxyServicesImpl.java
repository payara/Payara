/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.weld.services;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;

import org.glassfish.internal.api.ClassLoaderHierarchy;
import org.jboss.weld.serialization.spi.ProxyServices;
import org.glassfish.hk2.api.ServiceLocator;

/**
 * An implementation of the <code>ProxyServices</code> Service.
 * 
 * This implementation uses the thread context classloader (the application 
 * classloader) as the classloader for loading the bean proxies. The classloader
 * that loaded the Bean must be used to load and define the bean proxy to handle
 * Beans with package-private constructor as discussed in WELD-737. 
 *  
 * Weld proxies today have references to some internal weld implementation classes
 * such as javassist and org.jboss.weld.proxy.* packages. These classes are 
 * temporarily re-exported through the weld-integration-fragment bundle so that
 * when the bean proxies when loaded using the application classloader will have
 * visibility to these internal implementation classes.
 * 
 * As a fix for WELD-737, Weld may use the Bean's classloader rather than asking
 * the ProxyServices service implementation. Weld also plans to remove the 
 * dependencies of the bean proxy on internal implementation classes. When that
 * happens we can remove the weld-integration-fragment workaround and the 
 * ProxyServices implementation
 *  
 * @author Sivakumar Thyagarajan
 */
public class ProxyServicesImpl implements ProxyServices {
    
    ClassLoaderHierarchy clh;
    
    public ProxyServicesImpl(ServiceLocator services) {
        clh = services.getService(ClassLoaderHierarchy.class);
    }

    
    @Override
    public ClassLoader getClassLoader(final Class<?> proxiedBeanType) {
        SecurityManager sm = System.getSecurityManager();
        if (sm != null) {
            return AccessController
                    .doPrivileged(new PrivilegedAction<ClassLoader>() {
                        public ClassLoader run() {
                            return getClassLoaderforBean(proxiedBeanType);
                        }
                    });
        } else {
            return getClassLoaderforBean(proxiedBeanType);
        }
    }
    
    /**
     * Gets the ClassLoader associated with the Bean. Weld generates Proxies
     * for Beans from an application/BDA and for certain API artifacts such as
     * <code>UserTransaction</code>.
     * 
     * @param proxiedBeanType
     * @return
     */
    private ClassLoader getClassLoaderforBean(Class<?> proxiedBeanType) {
        //Get the ClassLoader that loaded the Bean. For Beans in an application,
        //this would be the application/module classloader. For other API
        //Bean classes, such as UserTransaction, this would be a non-application
        //classloader
        ClassLoader prxCL = proxiedBeanType.getClassLoader();
        //Check if this is an application classloader
        boolean isAppCL = isApplicationClassLoader(prxCL);
        if (!isAppCL) {
            prxCL = _getClassLoader(); 
            //fall back to the old behaviour of using TCL to get the application
            //or module classloader. We return this classloader for non-application
            //Beans, as Weld Proxies requires other Weld support classes (such as
            //JBoss Reflection API) that is exported through the weld-osgi-bundle.
        }
        return prxCL;
    }

    /**
     * Check if the ClassLoader of the Bean type being proxied is a 
     * GlassFish application ClassLoader. The current logic checks if
     * the common classloader appears as a parent in the classloader hierarchy
     * of the Bean's classloader.
     */
    private boolean isApplicationClassLoader(ClassLoader prxCL) {
        boolean isAppCL = false;
        while (prxCL != null) {
            if (prxCL.equals(clh.getCommonClassLoader())) {
                isAppCL = true;
                break;
            }
            prxCL = prxCL.getParent();
        }
        return isAppCL;
    }


    private ClassLoader _getClassLoader() {
        ClassLoader tcl = Thread.currentThread().getContextClassLoader();
        return tcl;
    }

    @Override
    public Class<?> loadBeanClass(final String className) {
        try {
            SecurityManager sm = System.getSecurityManager();
            if (sm != null) {
                return (Class<?>) AccessController
                        .doPrivileged(new PrivilegedExceptionAction<Object>() {
                            public Object run() throws Exception {
                                ClassLoader cl = _getClassLoader();
                                return Class.forName(className, true, cl);
                            }
                        });
            } else {
                ClassLoader cl = _getClassLoader();
                return Class.forName(className, true, cl);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }

    @Override
    public void cleanup() {
        // nothing to cleanup in this implementation.
    }

}
