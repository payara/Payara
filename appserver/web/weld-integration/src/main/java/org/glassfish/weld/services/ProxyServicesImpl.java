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
// Portions Copyright [2022] [Payara Foundation and/or its affiliates]
package org.glassfish.weld.services;

import org.glassfish.internal.api.ClassLoaderHierarchy;
import org.jboss.weld.serialization.spi.ProxyServices;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.weld.ClassGenerator;
import org.glassfish.weld.WeldProxyException;
import java.security.ProtectionDomain;

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

    public Class<?> loadClass(Class<?> originalClass, String classBinaryName) throws ClassNotFoundException {
        return _getClassLoader().loadClass(classBinaryName);
    }

    @Override
    public void cleanup() {
        // nothing to cleanup in this implementation.
    }

    @Override
    public Class<?> defineClass(Class<?> originalClass, String className, byte[] classBytes, int off, int len,
                                ProtectionDomain protectionDomain) throws ClassFormatError {
        ClassLoader cl = getClassLoaderforBean(originalClass);
        if(protectionDomain == null) {
            return defineClass(cl, className, classBytes, off, len);
        }
        return defineClass(cl, className, classBytes, off, len, protectionDomain);
    }

    @Override
    public Class<?> defineClass(Class<?> originalClass, String className, byte[] classBytes, int off, int len) throws ClassFormatError {
        return defineClass(originalClass, className, classBytes, off, len, null);
    }

    private Class<?> defineClass(final ClassLoader loader, final String className,
                                 final byte[] classBytes, final int off, final int len) {
        try {
            return ClassGenerator.defineClass(loader, className, classBytes, 0, len);
        } catch(final Exception e) {
            throw new WeldProxyException("Could not define class" +  className, e);
        }
    }

    private Class<?> defineClass(final ClassLoader loader, final String className,
                                 final byte[] classBytes, final int off, final int len,
                                 ProtectionDomain protectionDomain) {
        try {
            return ClassGenerator.defineClass(loader, className, classBytes, 0, len, protectionDomain);
        } catch(final Exception e) {
            throw new WeldProxyException("Could not define class" +  className, e);
        }
    }
}
