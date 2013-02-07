/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2013 Oracle and/or its affiliates. All rights reserved.
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


import org.glassfish.deployment.common.DeploymentException;
import org.glassfish.api.deployment.archive.Archive;
import java.io.*;
import java.util.Enumeration;
import java.util.Locale;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;
import java.util.logging.Logger;
import java.util.logging.LogRecord;
import java.util.logging.Level;

import com.sun.enterprise.deploy.shared.FileArchive;
import com.sun.enterprise.util.zip.ZipFile;
import com.sun.enterprise.util.zip.ZipFileException;
import com.sun.enterprise.util.i18n.StringManager;
import com.sun.enterprise.util.io.FileUtils;

import org.glassfish.logging.annotation.LogMessageInfo;

/**
 * Simple Module exploder
 *
 * @author Jerome Dochez
 *
 */
public class ModuleExploder {

    public static final Logger deplLogger = org.glassfish.deployment.common.DeploymentContextImpl.deplLogger;

    @LogMessageInfo(message = "Could not expand entry {0} into destination {1}", cause="An exception was caught when the entry was expanded", action="See the exception to determine how to fix the error", level="SEVERE")
    private static final String COULD_NOT_EXPAND_ENTRY = "NCLS-DEPLOYMENT-00005";

    protected static final StringManager localStrings =
            StringManager.getManager(ModuleExploder.class );

    protected static final String PRESERVED_MANIFEST_NAME = java.util.jar.JarFile.MANIFEST_NAME + ".preserved";

    protected static final String WEB_INF_PREFIX = "WEB-INF/";


    public static void explodeJar(File source, File destination) throws IOException {
        JarFile jarFile = null;
        String fileSystemName = null; // declared outside the try block so it's available in the catch block
        try {
            jarFile = new JarFile(source);
            Enumeration<JarEntry> e = jarFile.entries();
            while (e.hasMoreElements()) {
                JarEntry entry = e.nextElement();
                fileSystemName = entry.getName().replace('/', File.separatorChar);
                File out = new File(destination, fileSystemName);

                if (entry.isDirectory()) {
                    if (!out.exists() && !out.mkdirs()) {
                      throw new IOException("Unable to create directories " + out.getAbsolutePath());
                    }
                } else {
                    if (!out.getParentFile().exists()) {
                        out.getParentFile().mkdirs();
                    }
                    InputStream is = new BufferedInputStream(jarFile.getInputStream(entry));
                    FileOutputStream fos = FileUtils.openFileOutputStream(out);
                    FileUtils.copy(is, fos, entry.getSize());
                }
            }
        } catch(Throwable e) {
            /*
             *Use the logger here, even though we rethrow the exception.  In
             *at least some cases the caller does not propagate this exception
             *further, instead replacing it with a serializable
             *IASDeployException.  The added information is then lost.
             *By logging the exception here, we make sure the log file at least
             *displays as much as we know about the problem even though the
             *exception sent to the client may not.
             */
            String msg0 = localStrings.getString(
                    "enterprise.deployment.backend.error_expanding",
                    new Object[] {source.getAbsolutePath()});
            IOException ioe = new IOException(msg0);
            ioe.initCause(e);
            LogRecord lr = new LogRecord(Level.SEVERE, COULD_NOT_EXPAND_ENTRY);
            Object args[] = { fileSystemName, destination.getAbsolutePath() };
            lr.setParameters(args);
            lr.setThrown(ioe);
            deplLogger.log(lr);
            throw ioe;
        } finally {
            if (jarFile != null) {
                jarFile.close();
            }
        }
    }
    

    public static void explodeModule(Archive source, File directory, boolean preserveManifest)
    throws IOException, DeploymentException {

        File explodedManifest = null;
        File preservedManifestFromArchive = null;

        FileArchive target = new FileArchive();
        target.create(directory.toURI());

        explodeJar(new File(source.getURI()), directory);

        if (preserveManifest) {
            explodedManifest = new File(directory, java.util.jar.JarFile.MANIFEST_NAME);
            if (explodedManifest.exists()) {
                /* Rename the manifest so it can be restored later. */
                preservedManifestFromArchive = new File(directory, PRESERVED_MANIFEST_NAME);
                if ( ! explodedManifest.renameTo(preservedManifestFromArchive)) {
                    throw new RuntimeException(localStrings.getString(
                            "enterprise.deployment.backend.error_saving_manifest",
                            new Object[]
                    { explodedManifest.getAbsolutePath(),
                              preservedManifestFromArchive.getAbsolutePath()
                    } ) ) ;
                }
            }
        }
        // now explode all top level jar files and delete them.
        // this cannot be done before since the optionalPkgDependency
        // require access to the manifest file of each .jar file.
        for (Enumeration itr = source.entries();itr.hasMoreElements();) {
            String fileName = (String) itr.nextElement();


            // check for optional packages depencies
            // XXX : JEROME look if this is still done
            // resolveDependencies(new File(directory, fileName));

             /*
              *Expand the file only if it is a jar and only if it does not lie in WEB-INF/lib.
              */
            if (fileName.toLowerCase(Locale.US).endsWith(".jar") && 
                ( ! fileName.replace('\\', '/').toUpperCase(Locale.getDefault()).startsWith(WEB_INF_PREFIX)) ) { 

                try {
                    File f = new File(directory, fileName);

                    ZipFile zip = new ZipFile(f, directory);
                    zip.explode();
                } catch(ZipFileException e) {
                    IOException ioe = new IOException(e.getMessage());
                    ioe.initCause(e);
                    throw ioe;
                }
            }
        }
         /*
          *If the archive's manifest was renamed to protect it from being overwritten by manifests from
          *jar files, then rename it back.  Delete an existing manifest file first if needed.
          */
        if (preservedManifestFromArchive != null) {
            if (explodedManifest.exists()) {
                if ( ! explodedManifest.delete()) {
                    throw new RuntimeException(localStrings.getString(
                            "enterprise.deployment.backend.error_deleting_manifest",
                            new Object []
                    { explodedManifest.getAbsolutePath(),
                              preservedManifestFromArchive.getAbsolutePath()
                    }
                    ) );
                }
            }

            if ( ! preservedManifestFromArchive.renameTo(explodedManifest)) {
                throw new RuntimeException(localStrings.getString(
                        "enterprise.deployment.backend.error_restoring_manifest",
                        new Object []
                { preservedManifestFromArchive.getAbsolutePath(),
                          explodedManifest.getAbsolutePath()
                }
                ) );
            }
        }

        source.close();
        target.close();
    }
}
