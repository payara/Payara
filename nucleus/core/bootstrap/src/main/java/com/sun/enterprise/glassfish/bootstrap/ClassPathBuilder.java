/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2011 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.glassfish.bootstrap;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Builds up a {@link ClassLoader}.
 *
 * @author Kohsuke Kawaguchi
 */
public final class ClassPathBuilder {
    private final List<URL> urls = new ArrayList<URL>();
    private final ClassLoader parent;

    public ClassPathBuilder(ClassLoader parent) {
        this.parent = parent;
    }

    /**
     * Adds a single jar.
     */
    public void addJar(File jar) throws IOException {
        if(!jar.exists())
            throw new IOException("No such file: "+jar);
        urls.add(jar.toURI().toURL());
    }

    /**
     * Adds a single class folder.
     */
    public void addClassFolder(File classFolder) throws IOException {
        addJar(classFolder);
    }

    /**
     * Adds all jars in the given folder.
     *
     * @param folder
     *      A directory that contains a bunch of jar files.
     * @param excludes
     *      List of jars to be excluded
     */
    public void addJarFolder(File folder, final String... excludes) throws IOException {
        if(!folder.isDirectory())
            throw new IOException("Not a directory "+folder);

        File[] children = folder.listFiles(new FileFilter() {
            public boolean accept(File pathname) {
                for (String name : excludes) {
                    if(pathname.getName().equals(name))
                        return false;   // excluded
                }
                return pathname.getPath().endsWith(".jar");
            }
        });

        if(children==null)
            return; // in a very rare race condition, the directory can disappear after we checked.

        for (File child : children) {
            addJar(child);
        }
    }

    /**
     * Looks for the child files/directories in the given folder that matches the specified GLOB patterns
     * (like "foo-*.jar") and adds them to the classpath.
     */
    public void addGlob(File folder, String... masks) throws IOException {
        StringBuilder regexp = new StringBuilder();
        for (String mask : masks) {
            if(regexp.length()>0)   regexp.append('|');
            regexp.append("(\\Q");
            regexp.append(mask.replace("?","\\E.\\Q").replace("*","\\E.*\\Q"));
            regexp.append("\\E)");
        }
        Pattern p = Pattern.compile(regexp.toString());

        File[] children = folder.listFiles();
        if(children==null)  return;
        for (File child : children) {
            if(p.matcher(child.getName()).matches())
                addJar(child);
        }
    }

    public ClassLoader create() {
        return AccessController.doPrivileged(new PrivilegedAction<ClassLoader>() {
            @Override
            public ClassLoader run() {
                return new URLClassLoader(urls.toArray(new URL[urls.size()]),parent);
            }
        });
    }

}
