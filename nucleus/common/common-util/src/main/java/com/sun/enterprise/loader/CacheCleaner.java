/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) [2024] Payara Foundation and/or its affiliates. All rights reserved.
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
 * file and include the License file at glassfish/legal/LICENSE.txt.
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
package com.sun.enterprise.loader;

import com.sun.enterprise.util.CULoggerInfo;
import java.lang.reflect.Field;
import java.util.logging.Level;
import java.util.logging.Logger;

public class CacheCleaner {
    private static final Logger logger = CULoggerInfo.getLogger();

    public static void clearCaches(ClassLoader classLoader) {
        clearOmniFacesCache(classLoader);
        clearJNACache(classLoader);
        while (classLoader != null) {
            clearJaxRSCache(classLoader);
            classLoader = classLoader.getParent();
        }
    }

    private static void clearJaxRSCache(ClassLoader classLoader) {
        try {
            Class<?> cdiComponentProvider = CachingReflectionUtil
                    .getClassFromCache("org.glassfish.jersey.ext.cdi1x.internal.CdiComponentProvider", classLoader);
            if (cdiComponentProvider != null) {
                Field runtimeSpecificsField = CachingReflectionUtil.getFieldFromCache(cdiComponentProvider,
                        "runtimeSpecifics", true);
                Object runtimeSpecifics = runtimeSpecificsField.get(null);
                CachingReflectionUtil.getMethodFromCache(runtimeSpecifics.getClass(),
                                "clearJaxRsResource", true, ClassLoader.class)
                        .invoke(runtimeSpecifics, classLoader);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error clearing Jax-Rs cache", e);
        }
    }

    private static void clearOmniFacesCache(ClassLoader classLoader) {
        try {
            Class<?> eagerBeans = CachingReflectionUtil
                    .getClassFromCache("org.omnifaces.cdi.eager.EagerBeansRepository", classLoader);
            if (eagerBeans != null && eagerBeans.getClassLoader() instanceof CurrentBeforeParentClassLoader) {
                Field instance = CachingReflectionUtil.getFieldFromCache(eagerBeans, "instance", true);
                instance.set(null, null);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error clearing OmniFaces cache", e);
        }
    }

    private static void clearJNACache(ClassLoader classLoader) {
        try {
            Class<?> cleanerClass = CachingReflectionUtil
                    .getClassFromCache("com.sun.jna.internal.Cleaner", classLoader);
            if (cleanerClass != null && cleanerClass.getClassLoader() instanceof CurrentBeforeParentClassLoader) {
                Field instanceField = CachingReflectionUtil.getFieldFromCache(cleanerClass, "INSTANCE", true);
                Object instance = instanceField.get(null);
                CachingReflectionUtil.getFieldFromCache(instance.getClass(), "cleanerThread", true)
                        .set(instance, null);
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Error clearing JNA cache", e);
        }
    }
}
