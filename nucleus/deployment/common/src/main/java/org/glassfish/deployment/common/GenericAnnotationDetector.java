/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2009-2012 Oracle and/or its affiliates. All rights reserved.
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
//Portions Copyright [2016-2017] [Payara Foundation]
package org.glassfish.deployment.common;


import java.io.FileNotFoundException;
import java.util.List;
import java.util.ArrayList;

import java.io.InputStream;
import java.io.File;
import java.io.IOException;
import java.util.Enumeration;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.util.logging.Level;
import java.net.URI;

import org.glassfish.api.deployment.archive.ReadableArchive;
import com.sun.enterprise.deploy.shared.ArchiveFactory;
import org.glassfish.hk2.external.org.objectweb.asm.AnnotationVisitor;
import org.glassfish.hk2.external.org.objectweb.asm.ClassReader;
import org.glassfish.hk2.external.org.objectweb.asm.Type;
import org.glassfish.internal.api.Globals;

import org.glassfish.logging.annotation.LogMessageInfo;

/**
 * This class will detect whether an archive contains specified annotations.
 */
public class GenericAnnotationDetector extends AnnotationScanner {

    public static final Logger deplLogger = org.glassfish.deployment.common.DeploymentContextImpl.deplLogger;

    @LogMessageInfo(message = "Cannot find archive {0} referenced from archive {1}, it will be ignored for annotation scanning", level="WARNING")
    private static final String ARCHIVE_NOT_FOUND = "NCLS-DEPLOYMENT-00006";

    @LogMessageInfo(message = "Exception caught {0}", level="WARNING")
    private static final String EXCEPTION_CAUGHT = "NCLS-DEPLOYMENT-00007";

    @LogMessageInfo(message = "Error in jar entry {0}:  {1}", level="WARNING")
    private static final String JAR_ENTRY_ERROR = "NCLS-DEPLOYMENT-00008";

    @LogMessageInfo(message = "Failed to scan archive for annotations: {0}", level="WARNING")
    private static final String FAILED_ANNOTATION_SCAN = "NCLS-DEPLOYMENT-00009";
    boolean found = false;
    List<String> annotations = new ArrayList<String>();; 

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
            archiveFactory = Globals.getDefaultHabitat().getService(ArchiveFactory.class);
        }

        if (archiveFactory != null) {
            List<URI> externalLibs = DeploymentUtils.getExternalLibraries(archive);
            for (URI externalLib : externalLibs) {
                try {
                    scanArchive(archiveFactory.openArchive(new File(externalLib.getPath())));
                } catch(FileNotFoundException fnfe) {
                    Object args[] = { externalLib.getPath(), archive.getName() };
                    deplLogger.log(Level.WARNING, ARCHIVE_NOT_FOUND, args);
                } catch (Exception e) {
                    LogRecord lr = new LogRecord(Level.WARNING, EXCEPTION_CAUGHT);
                    Object args[] = { e.getMessage() };
                    lr.setParameters(args);
                    lr.setThrown(e);
                    deplLogger.log(lr);
                }
            }
        }
        return found;
    }

    @Override
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
                        if (found) {
                            return;
                        } 
                    } finally {
                        is.close();
                    }
                } else if ((entryName.endsWith(".jar") || entryName.endsWith(".rar") || entryName.endsWith(".war") || entryName.endsWith(".ear")) && !entryName.contains("/")) {
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
                                    try (InputStream is = jarSubArchive.getEntry(jarEntryName)) {
                                        ClassReader cr = new ClassReader(is);
                                        cr.accept(this, crFlags);
                                        if (found) {
                                            return;
                                        } 
                                    }
                                }
                            }
                        } finally {
                            jarSubArchive.close();
                        }
                    } catch (Exception ioe) {
                        Object args[] = { entryName, ioe };
                        deplLogger.log(Level.WARNING, JAR_ENTRY_ERROR, args);
                    }
                }
            }
        } catch (Exception e) {
          deplLogger.log(Level.WARNING, FAILED_ANNOTATION_SCAN, e);
        }
    }
}
