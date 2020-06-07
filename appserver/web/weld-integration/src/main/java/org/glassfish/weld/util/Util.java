/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2014 Oracle and/or its affiliates. All rights reserved.
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
 *
 * Portions Copyright [2017] Payara Foundation and/or affiliates
 */

package org.glassfish.weld.util;

import org.glassfish.weld.ACLSingletonProvider;
import org.jboss.weld.bootstrap.api.SingletonProvider;
import org.jboss.weld.bootstrap.api.helpers.TCCLSingletonProvider;

/**
 * Defines util methods for instantiating with weld
 */
public class Util {

    /**
     * Creates and returns a new instance of the specified class using a no-argument constructor
     * @param <T>
     * @param className
     * @throws InstantiationException
     * @throws IllegalAccessException
     * @return 
     */
    public static <T> T newInstance(String className) {
        try {
            return Util.<T>classForName(className).newInstance();
        } catch (InstantiationException e) {
            throw new IllegalArgumentException("Cannot instantiate instance of " + className + " with no-argument constructor", e);
       } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Cannot instantiate instance of " + className + " with no-argument constructor", e);
       }
    }

    /**
     * Returns the actual class with the specified name
     * @param <T>
     * @param name The full name of the class i.e. java.lang.String
     * @return 
     */
    public static <T> Class<T> classForName(String name) {
        try {
            if (Thread.currentThread().getContextClassLoader() != null) {
                Class<?> c = Thread.currentThread().getContextClassLoader().loadClass(name);
                @SuppressWarnings("unchecked")
                Class<T> clazz = (Class<T>)  c;
                return clazz;
            } else {
                Class<?> c = Class.forName(name);
                @SuppressWarnings("unchecked")
                Class<T> clazz = (Class<T>)  c;
                return clazz;
            }
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Cannot load class for " + name, e);
        } catch (NoClassDefFoundError e) {
            throw new IllegalArgumentException("Cannot load class for " + name, e);
        }
   }

    /**
     * Starts the singleton provider for weld.
     * <p>
     * This will be {@link ACLSingletonProvider}
     */
    public static void initializeWeldSingletonProvider() {
        SingletonProvider.initialize(new ACLSingletonProvider());
    }

}
