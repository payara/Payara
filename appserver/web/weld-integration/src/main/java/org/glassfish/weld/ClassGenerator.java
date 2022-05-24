/*
 * Copyright (c) 2022 Eclipse Foundation and/or its affiliates. All rights reserved.
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
package org.glassfish.weld;

import java.lang.reflect.Method;
import java.security.ProtectionDomain;
import java.security.PrivilegedExceptionAction;

import static java.security.AccessController.doPrivileged;

public final class ClassGenerator {

    private static Method defineClassMethod;
    private static Method defineClassMethodSM;

    static {
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
            doPrivileged(action);
        } catch (final Exception e) {
            throw new Error("Could not initialize access to ClassLoader.defineClass method.", e);
        }
    }

    public static Class<?> defineClass(final ClassLoader loader, final String className, final byte[] b) {
        return defineClass(loader, className, b, 0, b.length);
    }

    public static Class<?> defineClass(
            final ClassLoader loader, final String className,
            final byte[] b, final int off, final int len,
            final ProtectionDomain protectionDomain) {
        try {
            return (Class<?>) defineClassMethodSM.invoke(loader, className, b, 0, len, protectionDomain);
        } catch (final Exception e) {
            throw new ClassDefinitionException(className, loader, e);
        }
    }

    public static Class<?> defineClass(final ClassLoader loader, final String className, final byte[] b,
                                       final ProtectionDomain protectionDomain) {
        return defineClass(loader, className, b, 0, b.length, protectionDomain);
    }

    public static Class<?> defineClass(final ClassLoader loader, final String className, final byte[] b, final int off,
                                       final int len) {
        try {
            return (Class<?>) defineClassMethod.invoke(loader, className, b, 0, len);
        } catch (final Exception e) {
            throw new ClassDefinitionException(className, loader, e);
        }
    }

    public static class ClassDefinitionException extends RuntimeException {
        private static final long serialVersionUID = -8955780830818904365L;

        ClassDefinitionException(final String className, final ClassLoader loader, final Exception cause) {
            super("Could not define class '" + className + "' by the class loader: " + loader, cause);
        }
    }

}
