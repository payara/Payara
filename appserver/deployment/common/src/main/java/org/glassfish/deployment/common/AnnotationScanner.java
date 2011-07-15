/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.deployment.common;

import org.objectweb.asm.*;

import java.io.InputStream;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import java.util.logging.Level;

import org.glassfish.api.deployment.archive.ReadableArchive;

import com.sun.logging.LogDomains;

public class AnnotationScanner implements ClassVisitor {

    protected String className;
    protected String signature;

    final static Logger logger = LogDomains.getLogger(DeploymentUtils.class, LogDomains.DPL_LOGGER);

    public void visit(int version,
           int access,
           String name,
           String signature,
           String superName,
           String[] interfaces) {

        this.className = name;
        this.signature = signature;
    }

    public void visitSource(String s, String s1) {}

    public void visitOuterClass(String s, String s1, String s2) {

    }

    public AnnotationVisitor visitAnnotation(String s, boolean b) {
        return null;
    }

    public void visitAttribute(Attribute attribute) {

    }

    public void visitInnerClass(String s, String s1, String s2, int i) {

    }

    public FieldVisitor visitField(int i, String s, String s1, String s2, Object o) {
        return null;
    }

    public MethodVisitor visitMethod(int i, String s, String s1, String s2, String[] strings) {
        return null;
    }

    public void visitEnd() {
        
    }

    public void scanArchive(ReadableArchive archive) {
        try {
            int crFlags = ClassReader.SKIP_CODE | ClassReader.SKIP_DEBUG
                | ClassReader.SKIP_FRAMES;
            Enumeration<String> entries = archive.entries();
            while (entries.hasMoreElements()) {
                String entryName = entries.nextElement();
                if (entryName.endsWith(".class")) {
                    // scan class files
                    InputStream is = archive.getEntry(entryName);
                    try {
                        ClassReader cr = new ClassReader(is);
                        cr.accept(this, crFlags);
                    } catch(Exception e) {
                        logger.log(Level.WARNING, "Exception while scanning " +
                                entryName, e);
                    } finally {
                        is.close();
                    }
                } else if (entryName.endsWith(".jar")) {
                    // scan class files inside jar
                    try {
                        File archiveRoot = new File(archive.getURI());
                        File file = new File(archiveRoot, entryName);
                        JarFile jarFile = new JarFile(file);
                        try {
                            Enumeration<JarEntry> jarEntries = jarFile.entries();
                            while (jarEntries.hasMoreElements()) {
                                JarEntry entry = jarEntries.nextElement();
                                String jarEntryName = entry.getName();
                                if (jarEntryName.endsWith(".class")) {
                                    InputStream is = jarFile.getInputStream(entry);
                                    try {
                                        ClassReader cr = new ClassReader(is);
                                        cr.accept(this, crFlags);
                                    } catch(Exception e) {
                                        logger.log(Level.FINE,
                                                "Exception while scanning " +
                                                        entryName, e);
                                    } finally {
                                        is.close();
                                    }
                                }
                            }
                        } finally {
                            jarFile.close();
                        }
                    } catch (IOException ioe) {
                        logger.warning("Error scan jar entry" + entryName +
                            ioe.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            logger.log(Level.WARNING, "Failed to scan archive for annotations", e);
        }
    }
}
