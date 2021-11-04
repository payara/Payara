/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 * Copyright (c) 2010-2021 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright 2021 Payara Foundation and/or its affiliates.

package org.glassfish.weld.services;

import java.lang.reflect.Method;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.PrivilegedExceptionAction;
import java.security.ProtectionDomain;
import java.util.concurrent.atomic.AtomicBoolean;

import org.glassfish.internal.api.ClassLoaderHierarchy;
import org.jboss.weld.serialization.spi.ProxyServices;
import org.glassfish.hk2.api.ServiceLocator;

/**
 * An implementation of the {@link ProxyServices}.
 * <p>
 * This implementation respects the classloader hierarchy used to load original beans.
 * If it is not an application classloader, uses the current thread classloader.
 * If it wasn't possible to detect any usable classloader, throws a {@link WeldProxyException}
 * <p>
 * Context: Weld generates proxies for beans from an application and for certain API artifacts
 * such as <code>UserTransaction</code>.
 *
 * @author Sivakumar Thyagarajan
 * @author David Matějček
 */
public class ProxyServicesImpl implements ProxyServices {

    private static Method defineClassMethod;
    private static Method defineClassMethodSM;
    private static final AtomicBoolean CL_METHODS_INITIALIZATION_FINISHED = new AtomicBoolean(false);

    private final ClassLoaderHierarchy classLoaderHierarchy;


    /**
     * @param services immediately used to find a {@link ClassLoaderHierarchy} service
     */
    public ProxyServicesImpl(final ServiceLocator services) {
        classLoaderHierarchy = services.getService(ClassLoaderHierarchy.class);
    }


    @Deprecated
    @Override
    public boolean supportsClassDefining() {
        // true is mandatory since Weld 4.0.1.SP1, because default method impl returns false
        // and cdi_all tests then fail
        return true;
    }


    @Deprecated
    @Override
    public ClassLoader getClassLoader(final Class<?> proxiedBeanType) {
        if (System.getSecurityManager() == null) {
            return getClassLoaderforBean(proxiedBeanType);
        }
        final PrivilegedAction<ClassLoader> action = () -> getClassLoaderforBean(proxiedBeanType);
        return AccessController.doPrivileged(action);
    }


    @Deprecated
    @Override
    public Class<?> loadBeanClass(final String className) {
        try {
            if (System.getSecurityManager() == null) {
                return loadClassByThreadCL(className);
            }
            final PrivilegedExceptionAction<Class<?>> action = () -> loadClassByThreadCL(className);
            return AccessController.doPrivileged(action);
        } catch (final Exception ex) {
            throw new WeldProxyException("Failed to load the bean class: " + className, ex);
        }
    }


    @Override
    public Class<?> defineClass(final Class<?> originalClass, final String className, final byte[] classBytes,
        final int off, final int len) throws ClassFormatError {
        return defineClass(originalClass, className, classBytes, off, len, null);
    }


    @Override
    public Class<?> defineClass(final Class<?> originalClass, final String className, final byte[] classBytes,
        final int off, final int len, final ProtectionDomain protectionDomain) throws ClassFormatError {
        checkClassDefinitionFeature();
        final ClassLoader loader = getClassLoaderforBean(originalClass);
        if (protectionDomain == null) {
            return defineClass(loader, className, classBytes, off, len);
        }
        return defineClass(loader, className, classBytes, off, len, protectionDomain);
    }


    @Override
    public Class<?> loadClass(final Class<?> originalClass, final String classBinaryName)
        throws ClassNotFoundException {
        return getClassLoaderforBean(originalClass).loadClass(classBinaryName);
    }


    @Override
    public void cleanup() {
        // nothing to cleanup in this implementation.
    }


    /**
     * Initialization of access to protected methods of the {@link ClassLoader} class.
     */
    private static void checkClassDefinitionFeature() {
        if (CL_METHODS_INITIALIZATION_FINISHED.compareAndSet(false, true)) {
            try {
                final PrivilegedExceptionAction<Void> action = () -> {
                    final Class<?> cl = Class.forName("java.lang.ClassLoader");
                    final String name = "defineClass";
                    defineClassMethod = cl.getDeclaredMethod(name, String.class, byte[].class, int.class, int.class);
                    defineClassMethod.setAccessible(true);
                    defineClassMethodSM = cl.getDeclaredMethod(
                        name, String.class, byte[].class, int.class, int.class, ProtectionDomain.class);
                    defineClassMethodSM.setAccessible(true);
                    return null;
                };
                AccessController.doPrivileged(action);
            } catch (final Exception e) {
                throw new WeldProxyException("Could not initialize access to ClassLoader.defineClass method.", e);
            }
        }
    }


    /**
     * @param originalClass
     * @return ClassLoader probably usable with the bean.
     */
    private ClassLoader getClassLoaderforBean(final Class<?> originalClass) {
        // Get the ClassLoader that loaded the Bean. For Beans in an application,
        // this would be the application/module classloader. For other API
        // Bean classes, such as UserTransaction, this would be a non-application
        // classloader
        final ClassLoader originalClassLoader = originalClass.getClassLoader();
        if (isApplicationClassLoader(originalClassLoader)) {
            return originalClassLoader;
        }
        // fall back to the old behaviour of using thread class loader to get the application
        // or module classloader. We return this classloader for non-application
        // Beans, as Weld Proxies requires other Weld support classes (such as
        // JBoss Reflection API) that is exported through the weld-osgi-bundle.
        final ClassLoader threadCL = Thread.currentThread().getContextClassLoader();
        if (threadCL != null) {
            return threadCL;
        }
        throw new WeldProxyException("Could not determine classloader for " + originalClass);
    }


    /**
     * Check if the ClassLoader of the Bean type being proxied is a GlassFish application
     * ClassLoader. The current logic checks if the common classloader appears as a parent in
     * the classloader hierarchy of the Bean's classloader.
     */
    private boolean isApplicationClassLoader(ClassLoader classLoader) {
        while (classLoader != null) {
            if (classLoader.equals(classLoaderHierarchy.getCommonClassLoader())) {
                return true;
            }
            classLoader = classLoader.getParent();
        }
        return false;
    }


    private Class<?> loadClassByThreadCL(final String className) throws ClassNotFoundException {
        return Class.forName(className, true, Thread.currentThread().getContextClassLoader());
    }


    private Class<?> defineClass(
        final ClassLoader loader, final String className,
        final byte[] b, final int off, final int len,
        final ProtectionDomain protectionDomain) {
        try {
            return (Class<?>) defineClassMethodSM.invoke(loader, className, b, 0, len, protectionDomain);
        } catch (final Exception e) {
            throw new WeldProxyException("Could not define class " + className, e);
        }
    }


    private Class<?> defineClass(
        final ClassLoader loader, final String className,
        final byte[] b, final int off, final int len) {
        try {
            return (Class<?>) defineClassMethod.invoke(loader, className, b, 0, len);
        } catch (final Exception e) {
            throw new WeldProxyException("Could not define class " + className, e);
        }
    }
}
