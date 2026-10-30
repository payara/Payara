/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2022] Payara Foundation and/or its affiliates. All rights reserved.
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
 * The Payara Foundation designates this particular file as subject to the "Classpath"
 * exception as provided by the Payara Foundation in the GPL Version 2 section of the License
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
package org.glassfish.weld;

import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
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

    public static Class<?> defineClass(final ClassLoader loader, final String className, final byte[] b, final int off,
                                       final int len, final ProtectionDomain protectionDomain) {
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
