/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2018-2020] Payara Foundation and/or affiliates

package org.glassfish.apf.impl;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.security.PrivilegedAction;
import java.util.Set;
import java.util.HashSet;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.logging.Level;

import org.glassfish.apf.Scanner;

/**
 * Implementation of the Scanner interface for a directory
 *
 * @author Jerome Dochez
 */
public class DirectoryScanner extends JavaEEScanner implements Scanner<Object> {
    
    private File directory;
    private Set<String> entries = new HashSet<>();
    private ClassLoader classLoader = null;

    @Override
    public void process(File directory, Object bundleDesc, ClassLoader classLoader)
            throws IOException {
        AnnotationUtils.getLogger().log(Level.FINER, "dir is {0}", directory);
        AnnotationUtils.getLogger().log(Level.FINER, "classLoader is {0}", classLoader);
        this.directory = directory;
        this.classLoader = classLoader;
        init(directory);
    }       
    
    private void init(File directory) throws java.io.IOException {
        init(directory, directory);
        initTypes(directory);
    } 
    
    private void init(File top, File directory) throws java.io.IOException {
        
        File[] dirFiles = directory.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.getAbsolutePath().endsWith(".class");
                }
        });
        for (File file : dirFiles) {
            entries.add(file.getPath().substring(top.getPath().length()+1));
        }
        
        File[] subDirs = directory.listFiles(new FileFilter() {
                @Override
                public boolean accept(File pathname) {
                    return pathname.isDirectory();
                }
        });
        for (File subDir : subDirs) {
            init(top, subDir);
        }
    }
    
    protected Set<String> getEntries() {
        return entries;
    }

    @Override
    public ClassLoader getClassLoader() {
        if (classLoader==null) {
            final URL[] urls = new URL[1];
            try {
                if (directory == null) throw new IllegalStateException("directory must first be set by calling the process method.");
                urls[0] = directory.getAbsoluteFile().toURL();
                classLoader = new PrivilegedAction<URLClassLoader>() {
                  @Override
                  public URLClassLoader run() {
                    return new URLClassLoader(urls);
                  }
                }.run();
            } catch(Exception e) {
                AnnotationUtils.getLogger().log(Level.SEVERE, null, e);
            }
        }
        return classLoader;
    }

    @Override
    public Set<Class> getElements() {
        return getElements(entries);
    }

    @Override
    public Set<Class> getElements(Set<String> classNames) {

        Set<Class> elements = new HashSet<>();
        if (getClassLoader() == null) {
            AnnotationUtils.getLogger().severe("Class loader null");
            return elements;
        }
        for (String fileName : classNames) {
            // convert to a class name...
            String className = fileName.replace(File.separatorChar, '.');
            className = className.substring(0, className.length() - 6);
            AnnotationUtils.getLogger().log(Level.FINE, "Getting {0}", className);
            try {
                elements.add(classLoader.loadClass(className));
            } catch (Throwable cnfe) {
                AnnotationUtils.getLogger().log(Level.SEVERE, "cannot load {0} reason : {1}", new Object[]{className, cnfe.getMessage()});
            }
        }
        return elements;
    }

}
