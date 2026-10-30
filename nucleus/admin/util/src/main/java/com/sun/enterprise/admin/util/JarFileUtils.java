/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015-2021 Payara Foundation and/or its affiliates. All rights reserved.
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
package com.sun.enterprise.admin.util;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ConcurrentModificationException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utilities class for JarFiles
 *
 * @author Sven Diedrichsen
 * @since 13.06.18
 */
public class JarFileUtils {

    private static final Logger LOG = Logger.getLogger(JarFileUtils.class.getName());

    /**
     * Ensures that all cached JarFile instances are closed.
     */
    public static void closeCachedJarFiles() {
        try {
            Map<String, JarFile> files = null;
            Object jarFileFactoryInstance = null;
            Class clazz = Class.forName("sun.net.www.protocol.jar.JarFileFactory", true, URL.class.getClassLoader());
            Field fields[] = clazz.getDeclaredFields();
            for (Field field : fields) {
                if ("fileCache".equals(field.getName())) {
                    field.setAccessible(true);
                    files = (Map<String, JarFile>) field.get(null);
                }
                if ("instance".equals(field.getName())) {
                    field.setAccessible(true);
                    jarFileFactoryInstance = field.get(null);
                }
            }
            if (files != null && !files.isEmpty()) {
                Map<String, JarFile> jarFiles = new HashMap<>();
                if(jarFileFactoryInstance != null) {
                    synchronized (jarFileFactoryInstance) {
                        jarFiles.putAll(files);
                    }
                } else {
                    jarFiles.putAll(files);
                }
                // Close "file:" JAR files so they can be overwritten on Windows
                // Leave others open, but remove them from the cache so they
                // Can be closed when garbage collected.  This prevents
                // closing JarFile instances opened by URLClassLoader
                // NOTE: This still may close JarFile instances not opened by the calling process.
                Method closeMethod = clazz.getMethod("close", JarFile.class);
                closeMethod.setAccessible(true);
                for (Map.Entry<String, JarFile> file : jarFiles.entrySet()) {
                    if (file.getKey().startsWith("file:")) {
                        file.getValue().close();
                    } else {
                        closeMethod.invoke(jarFileFactoryInstance, file.getValue());
                    }
                }
            }
        } catch (ClassNotFoundException | IllegalAccessException | SecurityException | IllegalArgumentException | IOException | ConcurrentModificationException | InvocationTargetException | NoSuchMethodException ex) {
            LOG.log(Level.SEVERE, null, ex);
        }
    }

}
