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

import java.io.FileNotFoundException;
import java.util.List;
import java.util.ArrayList;

import java.io.InputStream;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.net.URI;

import org.glassfish.api.deployment.archive.ReadableArchive;
import com.sun.enterprise.deploy.shared.ArchiveFactory;
import org.glassfish.internal.api.Globals;

import com.sun.logging.LogDomains;

/**
 * This class will detect whether an archive contains specified annotations.
 */
public class GenericAnnotationDetector extends AnnotationScanner {

    boolean found = false;
    List<String> annotations = new ArrayList<String>();; 

    final static Logger logger = LogDomains.getLogger(DeploymentUtils.class, LogDomains.DPL_LOGGER);

    public GenericAnnotationDetector(Class[] annotationClasses) {
        if (annotationClasses != null) {
            for (Class annClass : annotationClasses) {
                annotations.add(Type.getDescriptor(annClass));
            }
        }
    }

    public boolean hasAnnotationInArchive(ReadableArchive archive) {
        scanArchive(archive);
        if (found) {
            return found;
        }      
        ArchiveFactory archiveFactory = null;
        if (Globals.getDefaultHabitat() != null) {
            archiveFactory = Globals.getDefaultHabitat().getComponent(ArchiveFactory.class);
        }

        if (archiveFactory != null) {
            List<URI> externalLibs = DeploymentUtils.getExternalLibraries(archive);
            for (URI externalLib : externalLibs) {
                try {
                    scanArchive(archiveFactory.openArchive(new File(externalLib.getPath())));
                } catch(FileNotFoundException fnfe) {
                    logger.log(Level.WARNING, "Cannot find archive " + externalLib.getPath()
                            + " referenced from archive " + archive.getName()
                            + ", it will be ignored for annotation scanning"); 
                } catch (Exception e) {
                    logger.log(Level.WARNING, e.getMessage(), e);
                }
            }
        }
        return found;
    }

    public AnnotationVisitor visitAnnotation(String s, boolean b) {
        if (annotations.contains(s)) {
            found = true;
        }
        return null;
    }

    @Override
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
                    } finally {
                        is.close();
                    }
                } else if (entryName.endsWith(".jar") && 
                    entryName.indexOf('/') == -1) {
                    // scan class files inside top level jar
                    try {
                        ReadableArchive jarSubArchive = null;
                        try {
                            jarSubArchive = archive.getSubArchive(entryName);
                            Enumeration<String> jarEntries =
                                jarSubArchive.entries();
                            while (jarEntries.hasMoreElements()) {
                                String jarEntryName = jarEntries.nextElement();
                                if (jarEntryName.endsWith(".class")) {
                                    InputStream is =
                                        jarSubArchive.getEntry(jarEntryName);
                                    try {
                                        ClassReader cr = new ClassReader(is);
                                        cr.accept(this, crFlags);
                                    } finally {
                                        is.close();
                                    }
                                }
                            }
                        } finally {
                            jarSubArchive.close();
                        }
                    } catch (IOException ioe) {
                        logger.warning("Error scan jar entry" + entryName +
                            ioe.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            logger.warning("Failed to scan archive for annotations" +
                e.getMessage());
        }
    }
}
