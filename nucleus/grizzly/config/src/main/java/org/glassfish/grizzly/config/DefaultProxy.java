/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.grizzly.config;

import org.jvnet.hk2.config.Attribute;
import org.jvnet.hk2.config.ConfigBeanProxy;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.Map;

public class DefaultProxy implements InvocationHandler {
    private final Map<String, Method> methods;
    private final ConfigBeanProxy parent;

    DefaultProxy(ConfigBeanProxy parent, final Class<? extends ConfigBeanProxy> sslClass) {
        this.parent = parent;
        methods = new HashMap<String, Method>();
        final Method[] list = sslClass.getDeclaredMethods();
        for (Method method : list) {
            if (method.getName().startsWith("get") || method.getName().startsWith("is")) {
                methods.put(method.getName(), method);
            }
        }
    }

    public ConfigBeanProxy getParent() {
        return parent;
    }

    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        Object value = null;
        if (methods.get(method.getName()) != null) {
            final Attribute annotation = method.getAnnotation(Attribute.class);
            if (annotation != null) {
                String defValue = annotation.defaultValue().trim();
                if (defValue.length() > 0) {
                    value = defValue;
                }
            }
        } else if ("getParent".equals(method.getName())) {
            value = getParent();
        } else {
            throw new GrizzlyConfigException(String.format("Method not implemented for a %s: %s", getClass().getName(),
                    method.getName()));
        }
        return value;
    }

    public static ConfigBeanProxy createDummyProxy(ConfigBeanProxy parent,
            final Class<? extends ConfigBeanProxy> type) {
        return (ConfigBeanProxy) Proxy.newProxyInstance(type.getClassLoader(), new Class[]{type},
                new DefaultProxy(parent, type));
    }
}
