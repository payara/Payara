/*
 * Copyright (c) 2008, 2025 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.cdi.persistence;

import java.io.Serializable;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public class AnnotationInvocationHandler implements InvocationHandler, Serializable {

    private static final long serialVersionUID = 1L;

    private final Class<?> type;

    AnnotationInvocationHandler(Class<?> type) {
        this.type = type;
    }

    @Override
    public boolean equals(Object other) {
        return type.isInstance(other);
    }

    @Override
    public int hashCode() {
        return type.hashCode();
    }

    @Override
    public String toString() {
        return "@" + type.getName();
    }

    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        switch (method.getName()) {
            case "annotationType":
                return type;
            case "equals":
                return args.length > 0 && equals(args[0]);
            case "hashCode":
                return hashCode();
            case "toString":
                return toString();
            default:
                return null;
        }
    }
}
