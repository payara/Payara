/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.deployment.util;

import com.sun.enterprise.deployment.annotation.introspection.AnnotationScanner;
import com.sun.enterprise.deployment.annotation.introspection.ClassFile;
import com.sun.enterprise.deployment.annotation.introspection.ConstantPoolInfo;
import org.glassfish.api.deployment.archive.ReadableArchive;

import java.io.IOException;
import java.io.InputStream;
import java.io.File;
import java.io.FileFilter;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.util.Enumeration;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;

/**
 * Subclass for connector annotation detection.
 * Connector annotation detector need to scan for top level jars as well
 *
 */
public class ConnectorAnnotationDetector extends AnnotationDetector {

    public ConnectorAnnotationDetector(AnnotationScanner scanner) {
        super(scanner);
    }
    
    @Override
    public boolean hasAnnotationInArchive(ReadableArchive archive) throws IOException {

        Enumeration<String> entries = archive.entries();
        while (entries.hasMoreElements()) {
            String entryName = entries.nextElement();
            if (entryName.endsWith(".class")) {
                if (containsAnnotation(archive, entryName)) {
                    return true;
                }
            } 

            // scan classes in top level jars
            File archiveFile = new File(archive.getURI());
            File[] jarFiles = archiveFile.listFiles(new FileFilter() {
                 public boolean accept(File pathname) {
                     return (pathname.isFile() &&
                            pathname.getAbsolutePath().endsWith(".jar"));
                 }
            });

            if (jarFiles != null && jarFiles.length > 0) {
                for (File file : jarFiles) {
                    JarFile jarFile = null; 
                    try {
                        jarFile = new JarFile(file);
                        Enumeration<JarEntry> jarEntries = jarFile.entries();
                        while (jarEntries.hasMoreElements()) {
                            JarEntry jarEntry = jarEntries.nextElement();
                            if (jarEntry.getName().endsWith(".class")) {
                                if (containsAnnotation(jarFile.getInputStream(
                                    jarEntry), jarEntry.getSize())) {
                                    return true;
                                }
                            }
                        } 
                    } finally {
                        if (jarFile != null) {
                            jarFile.close();
                        }
                    }
                }
            }
        }
        return false;
    }
}
