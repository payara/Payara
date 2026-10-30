/*
 * Copyright (c) 2022, 2022 Contributors to the Eclipse Foundation.
 * Copyright (c) 1997, 2021 Oracle and/or its affiliates. All rights reserved.
 * Copyright (c) [2022-2024] Payara Foundation and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.security;

import java.lang.reflect.Method;
import java.security.Permission;
import jakarta.security.jacc.Policy;

import java.security.Principal;
import java.util.Arrays;
import java.util.Set;

import javassist.util.proxy.MethodHandler;

public class AuthenticationProxyHandler implements MethodHandler {

    public final static Method impliesMethod = getMethod(
            Policy.class, "implies", Permission.class, Set.class);

    private final Policy javaSePolicy;

    public AuthenticationProxyHandler(Policy javaSePolicy) {
        this.javaSePolicy = javaSePolicy;
    }

    @Override
    public Object invoke(Object self, Method overridden, Method forwarder, Object[] args) throws Throwable {
        if (isImplementationOf(overridden, impliesMethod)) {
            Permission permission = (Permission) args[1];
            Set<Principal> principals = (Set<Principal>) args[0];
            if (!permission.getClass().getName().startsWith("jakarta.")) {
                return javaSePolicy.implies(permission, principals);
            }
        }
        return forwarder.invoke(self, args);
    }

    public static boolean isImplementationOf(Method implementationMethod, Method interfaceMethod) {
        return interfaceMethod.getDeclaringClass().isAssignableFrom(implementationMethod.getDeclaringClass())
                && interfaceMethod.getName().equals(implementationMethod.getName())
                && Arrays.equals(interfaceMethod.getParameterTypes(), implementationMethod.getParameterTypes());
    }

    public static Method getMethod(Class<?> base, String name, Class<?>... parameterTypes) {
        try {
            return base.getMethod(name, parameterTypes);
        } catch (NoSuchMethodException | SecurityException e) {
            throw new IllegalStateException(e);
        }
    }

}
