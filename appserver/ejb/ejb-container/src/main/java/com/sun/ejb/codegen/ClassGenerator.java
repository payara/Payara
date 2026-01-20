/*
 * Copyright (c) 2022, 2024 Eclipse Foundation and/or its affiliates. All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0, which is available at
 * http://www.eclipse.org/legal/epl-2.0.
 *
 * This Source Code may also be made available under the following Secondary
 * Licenses when the conditions for such availability set forth in the
 * Eclipse Public License v. 2.0 are satisfied: GNU General Public License,
 * version 2 with the GNU Classpath Exception, which is available at
 * https://www.gnu.org/software/classpath/license.html.
 *
 * SPDX-License-Identifier: EPL-2.0 OR GPL-2.0 WITH Classpath-exception-2.0
 */
// Portions Copyright [2025] [Payara Foundation and/or its affiliates]
// Payara Foundation and/or its affiliates elects to include this software in this distribution under the GPL Version 2 license

package com.sun.ejb.codegen;

import com.sun.enterprise.loader.ASURLClassLoader;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.security.ProtectionDomain;
import java.util.Objects;
import java.util.logging.Logger;

import static java.util.logging.Level.CONFIG;

/**
 * This class serves to generate classes, because ...
 * <p>
 * {@link Lookup} class restricts the generated class to use
 * an already existing class as a source of the package and {@link ProtectionDomain}.
 * <p>
 * {@link Proxy#newProxyInstance(ClassLoader, Class[], java.lang.reflect.InvocationHandler)}
 * has another requirements, ie. all referred classes must be loadable by used classloader.
 *
 * @author David Matejcek
 */
public final class ClassGenerator {

    private static final Logger LOG = Logger.getLogger(ClassGenerator.class.getName());

    private static Method defineClassMethod;
    private static Method defineClassMethodSM;

    static {
        try {
            final Class<?> cl = Class.forName("java.lang.ClassLoader");
            final String name = "defineClass";

            defineClassMethod = cl.getDeclaredMethod(name, String.class, byte[].class, int.class, int.class);
            defineClassMethod.setAccessible(true);
            defineClassMethodSM = cl.getDeclaredMethod(
                name, String.class, byte[].class, int.class, int.class, ProtectionDomain.class);
            defineClassMethodSM.setAccessible(true);

            LOG.config("ClassLoader methods capable of generating classes were successfully detected.");
        } catch (final Exception e) {
            throw new Error("Could not initialize access to ClassLoader.defineClass method.", e);
        }
    }


    private ClassGenerator() {
        // hidden
    }


    /**
     * Decides which method is suitable to define the new class and then uses it.
     *
     * @param loader the classloader instance used to generate the class
     * @param anchorClass the class used as an "orientation" class. See the {@link Lookup} class for
     *            more info.
     * @param targetPackageName the name is used in decision logic; if the anchor class is from
     *            a different package, the {@link Lookup}'s method is not usable.
     * @param className expected binary name or null
     * @param classData the valid bytes that make up the class data.
     * @return the new generated class
     * @throws ClassDefinitionException invalid data, missing dependency, or another error related
     *             to the class generation
     */
    public static Class<?> defineClass(final ClassLoader loader, final Class<?> anchorClass,
        final String targetPackageName, final String className,  final byte[] classData) {
        return defineClass(loader, className, classData, anchorClass.getProtectionDomain());
    }


    /**
     * Calls the {@link Lookup}'s defineClass method to create a new class.
     *
     * @param anchorClass the class used as an "orientation" class. See the {@link Lookup} class for more info.
     * @param className expected binary name or null
     * @param classData the valid bytes that make up the class data.
     * @return the new generated class
     * @throws ClassDefinitionException invalid data, missing dependency, or another error related
     *             to the class generation
     */
    public static Class<?> defineClass(final Class<?> anchorClass, final String className, final byte[] classData) {
        LOG.log(CONFIG, "Defining class: {0} with anchorClass: {1}", new Object[] {className, anchorClass});

        try {
            return MethodHandles.privateLookupIn(anchorClass, MethodHandles.lookup())
                                .defineClass(classData);
        } catch (IllegalAccessException e) {
            throw new ClassDefinitionException(className,anchorClass, e);
        }
    }


    /**
     * Calls the {@link ClassLoader}'s protected defineClass method to create a new class.
     *
     * @param loader the classloader instance used to generate the class
     * @param className expected binary name or null
     * @param classData the valid bytes that make up the class data.
     * @return the new generated class
     * @throws ClassDefinitionException invalid data, missing dependency, or another error related
     *             to the class generation
     */
    public static Class<?> defineClass(final ClassLoader loader, final String className, final byte[] classData)
        throws ClassDefinitionException {
        return defineClass(loader, className, classData, 0, classData.length);
    }


    /**
     * Calls the {@link ClassLoader}'s protected defineClass method to create a new class.
     *
     * @param loader the classloader instance used to generate the class
     * @param className expected binary name or null
     * @param classData the valid bytes that make up the class data.
     * @param offset The start offset in {@code b} of the class data
     * @param length The length of the class data
     * @return the new generated class
     * @throws ClassDefinitionException invalid data, missing dependency, or another error related
     *             to the class generation
     */
    public static Class<?> defineClass(final ClassLoader loader, final String className, final byte[] classData,
        final int offset, final int length) throws ClassDefinitionException {
        LOG.log(CONFIG, "Defining class: {0} by loader: {1}", new Object[] {className, loader});

        try {
            return (Class<?>) defineClassMethod.invoke(loader, className, classData, 0, length);
        } catch (final Exception | NoClassDefFoundError | ClassFormatError e) {
            throw new ClassDefinitionException(className, loader, e);
        }
    }


    /**
     * Calls the {@link ClassLoader}'s protected defineClass method to create a new class.
     *
     * @param loader the classloader instance used to generate the class
     * @param className expected binary name or null
     * @param classData the valid bytes that make up the class data.
     * @param protectionDomain The {@code ProtectionDomain} of the class
     * @return the new generated class
     * @throws ClassDefinitionException invalid data, missing dependency, or another error related
     *             to the class generation
     */
    public static Class<?> defineClass(final ClassLoader loader, final String className, final byte[] classData,
        final ProtectionDomain protectionDomain) throws ClassDefinitionException {
        return defineClass(loader, className, classData, 0, classData.length, protectionDomain);
    }


    /**
     * Calls the {@link ClassLoader}'s protected defineClass method to create a new class.
     *
     * @param loader the classloader instance used to generate the class
     * @param className expected binary name or null
     * @param classData the valid bytes that make up the class data.
     * @param offset The start offset in {@code b} of the class data
     * @param length The length of the class data
     * @param protectionDomain The {@code ProtectionDomain} of the class
     * @return the new generated class
     * @throws ClassDefinitionException invalid data, missing dependency, or another error related
     *             to the class generation
     */
    public static Class<?> defineClass(
        final ClassLoader loader, final String className,
        final byte[] classData, final int offset, final int length,
        final ProtectionDomain protectionDomain) throws ClassDefinitionException {
        LOG.log(CONFIG, "Defining class: {0} by loader: {1}", new Object[] {className, loader});

        try {
            return (Class<?>) defineClassMethodSM.invoke(loader, className, classData, 0, length, protectionDomain);
        } catch (final Exception | NoClassDefFoundError | ClassFormatError e) {
            throw new ClassDefinitionException(className, loader, e);
        }
    }


    /**
     * The class wasn't generated. See the message and cause to see what happened.
     */
    public static class ClassDefinitionException extends RuntimeException {
        private static final long serialVersionUID = -8955780830818904365L;

        ClassDefinitionException(final String className, final ClassLoader loader, final Throwable cause) {
            super("Could not define class '" + className + "' by the class loader: " + loader, cause);
        }

        ClassDefinitionException(final String className, final Class<?> anchorClass, final Throwable cause) {
            super("Could not define class '" + className + "' using the anchor " + anchorClass, cause);
        }

    }


    private static boolean useMethodHandles(final ClassLoader loader, final Class<?> anchorClass,
        final String targetPackageName) {
        if (loader == null) {
            return true;
        }
        // The bootstrap CL used by embedded glassfish doesn't remember generated classes
        // Further ClassLoader.findClass calls will fail.
        if (anchorClass == null || loader.getParent() == null || loader.getClass() == ASURLClassLoader.class) {
            return false;
        }
        // Use MethodHandles.Lookup only if the anchor class has the same package.
        return Objects.equals(anchorClass.getPackageName(), targetPackageName);
    }
}
