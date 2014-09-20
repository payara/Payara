/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
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

/*
 * Archive.java
 *
 * Created on September 6, 2004, 9:10 AM
 */

package com.sun.enterprise.tools.verifier.apiscan.packaging;

import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * @author Sanjeeb.Sahoo@Sun.COM
 */
public class Archive {
    private Manifest manifest;
    private File path;
    private static String resourceBundleName = "com.sun.enterprise.tools.verifier.apiscan.LocalStrings";
    private static Logger logger = Logger.getLogger("apiscan.archive", resourceBundleName); // NOI18N
    private static final String myClassName = "Archive"; // NOI18N
    private static Archive[] allOptPkgsInstalledInJRE;//note this is per JVM. It is lazily instantiated
    private static String thisClassName = "com.sun.enterprise.tools.verifier.apiscan.packaging.Archive"; // NOI18N

    //returns a unmodifiable collection of installed optional packages.
    //once the method is called, for subsequent calls it retuns the same list
    //even if new pkg is installed in JVM lib ext dir. This is in line with JVM operations.
    public static Archive[] getAllOptPkgsInstalledInJRE() {
        if (allOptPkgsInstalledInJRE != null) return allOptPkgsInstalledInJRE;
        synchronized (Archive.class) {
            if (allOptPkgsInstalledInJRE == null) {//double if check to avoid race condition
                final ArrayList<Archive> allPkgs = new ArrayList<Archive>();
                List ext_dirs = listAllExtDirs();
                for (Iterator iter = ext_dirs.iterator(); iter.hasNext();) {
                    File ext_dir = new File((String) iter.next());
                    ext_dir.listFiles(new FileFilter() {
                        public boolean accept(File f) {
                            if (!f.isDirectory()) {
                                try {
                                    allPkgs.add(new Archive(new JarFile(f)));
                                    logger.logp(Level.FINE, myClassName,
                                            "getAllOptPkgsInstalledInJRE", // NOI18N
                                            "Found an installed opt pkg " + // NOI18N
                                            f.getAbsolutePath());
                                    return true;
                                } catch (Exception e) {
                                    logger.logp(Level.INFO, myClassName,
                                            "getAllOptPkgsInstalledInJRE", // NOI18N
                                            thisClassName + ".exception1", new Object[]{f.toString()});
                                    logger.log(Level.INFO, "", e);
                                }
                            }
                            return false;
                        }//accept()
                    });
                }
                // Store in a tmp and update allOptPkgsInstalledInJre in a single instruction.
                final Archive[] tmp = new Archive[allPkgs.size()];
                allPkgs.toArray(tmp);
                allOptPkgsInstalledInJRE = tmp;
            }//if null
        }//synchronized
        return allOptPkgsInstalledInJRE;
    }

    private static List listAllExtDirs() {
        String ext_dirStr = new String(System.getProperty("java.ext.dirs"));
        logger.fine("Extension Dir Path is " + ext_dirStr); // NOI18N
        ArrayList<String> ext_dirs = new ArrayList<String>();
        StringTokenizer st = new StringTokenizer(ext_dirStr,
                File.pathSeparator);
        while (st.hasMoreTokens()) {
            String next = st.nextToken();
            ext_dirs.add(next);
        }
        return ext_dirs;
    }

    /**
     * Creates a new instance of Archive
     */
    public Archive(JarFile jar) throws IOException {
        manifest = jar.getManifest();
        path = new File(jar.getName());
    }

    //path represnets either a dir or a jar file path
    public Archive(File path) throws IOException {
        this.path = path.getCanonicalFile();
    }

    public String getClassPath() throws IOException {
        String cp = getManifest().getMainAttributes().getValue(
                Attributes.Name.CLASS_PATH);
        if (cp != null)
            return cp;
        else
            return "";
    }

    //lazy initialisation
    public synchronized Manifest getManifest() throws IOException {
        if (manifest == null) {
            if (path.isDirectory()) {
                File file = new File(
                        path.getPath() + File.separator + JarFile.MANIFEST_NAME);
                if (file.exists()) {
                    InputStream mis = new FileInputStream(file);
                    manifest = new Manifest(mis);
                    mis.close();
                }
            } else {
                JarFile jar = new JarFile(path);
                try {
                    manifest = jar.getManifest();
                } finally {
                    jar.close();
                }
            }
            if (manifest == null)
                manifest = new Manifest();
        }
        return manifest;
    }

    /**
     * @return the absolute path of this package. Depdnding on whether it was
     *         constructed from a dir or a jar file, the returned path
     *         represents either a file or a dir.
     */
    public String getPath() {
        return path.getAbsolutePath();
    }

    public Archive[] getBundledArchives() throws IOException {
        ArrayList<Archive> list = new ArrayList<Archive>();
        String parent = path.getParent() + File.separator;
        for (StringTokenizer st = new StringTokenizer(getClassPath());
             st.hasMoreTokens();) {
            String nextEntry = st.nextToken();
            String entryPath = parent + nextEntry;
            if (!new File(entryPath).exists()) {
                logger.logp(Level.FINE, myClassName, "getBundledArchives", // NOI18N
                        entryPath +
                        " does not exist, will try to see if this is a module whose name has been changed when archive was exploded."); // NOI18N
                String newNextEntry;
                //account for the fact that Class-Path may be specified as ./a.jar
                if (nextEntry.startsWith("./") && nextEntry.length() > 2) // NOI18N
                    newNextEntry =
                            nextEntry.substring("./".length()).replaceAll( // NOI18N
                                    "\\.", "_"); // NOI18N
                else
                    newNextEntry = nextEntry.replaceAll("\\.", "_"); // NOI18N

                if (new File(parent, newNextEntry).exists()) {
                    logger.logp(Level.FINE, myClassName, "getBundledArchives", // NOI18N
                            "Using " + newNextEntry + " instead of " + nextEntry); // NOI18N
                    entryPath = parent + newNextEntry;
                    list.add(new Archive(new File(entryPath)));
                } else {
                    logger.logp(Level.WARNING, myClassName,
                            "getBundledArchives", // NOI18N
                            thisClassName + ".error1", new Object[]{getPath(), nextEntry});
                }
            }
            list.add(new Archive(new File(entryPath)));
        }
        return (Archive[]) list.toArray(new Archive[0]);
    }

    /**
     * @return the list of installed optional packages that this package depends
     *         on.
     */
    public ExtensionRef[] getExtensionRefs() throws IOException {
        ExtensionRef[] refs = new ExtensionRef[0];
        Manifest manifest = getManifest();
        String extensions = manifest.getMainAttributes().getValue(
                Attributes.Name.EXTENSION_LIST);
        ArrayList<ExtensionRef> extensionList = new ArrayList<ExtensionRef>();
        if (extensions != null) {
            for (StringTokenizer st = new StringTokenizer(extensions);
                 st.hasMoreTokens();) {
                String extName = st.nextToken();
                ExtensionRef ref = new ExtensionRef(manifest, extName);
                extensionList.add(ref);
            }
        }
        refs = (ExtensionRef[]) extensionList.toArray(refs);
        return refs;
    }

    public String toString() {
        return getPath();
    }
}
