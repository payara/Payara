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

package org.glassfish.appclient.server.core;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.ListIterator;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;
import java.util.List;
import java.util.logging.Level;
import java.util.Set;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.zip.ZipException;

import com.sun.enterprise.deployment.Application;
import com.sun.enterprise.deployment.archivist.Archivist;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.api.deployment.archive.WritableArchive;
import com.sun.enterprise.deployment.util.DOLUtils;
import com.sun.enterprise.util.shared.ArchivistUtils;
import com.sun.enterprise.util.zip.ZipItem;

/**
 * This class is responsible for creating an appclient jar file that
 * will be used by the appclient container to run the appclients for
 * the deployed application.
 *
 * @author deployment dev team
 */
class ClientJarMakerUtils {

    private static final Logger logger = DOLUtils.getDefaultLogger();

    static void populateStubs(WritableArchive target, ZipItem[] stubs)
            throws IOException {
        Set elements = new HashSet();
        for (ZipItem item : stubs) {
            if (elements.contains(item.getName())) {
                continue;
            }
            elements.add(item.getName());
            OutputStream os = null;
            InputStream is = null;
            try {
                os = target.putNextEntry(item.getName());
                is = new BufferedInputStream(new FileInputStream(item.getFile()));
                ArchivistUtils.copyWithoutClose(is, os);
            } finally {
                if (is != null) {
                    is.close();
                }
                if (os != null) {
                    target.closeEntry();
                }
            }
        }
    }

    /**
     * Populates a module archive using the contents of the original and
     * generated archives. 
     *<p>
     *Intended for use in building a top-level module archive.
     *@param original the ReadableArchive containing the input
     *@param generated the ReadableArchive for the generated bits for this application
     *@param target the archive to be created
     */
    static void populateModuleJar(ReadableArchive original,
            ReadableArchive generated, WritableArchive target)
            throws IOException {
        /*
         * Record library JARs and directories that must be included in the
         * app client archive.
         */
        ArrayList<String> libraries = new ArrayList<String>();

        /*
         * Class-Path entries in this module's manifest will be relative to
         * the module's parent.
         */
        File appArchive = new File(original.getURI().getSchemeSpecificPart());
        URI uri = appArchive.toURI();
        URI parentURI = null;
        try {
            parentURI = getParent(uri);
        } catch (URISyntaxException ex) {
            throw new IOException(ex);
        }
        populateModuleJar(original, generated, target, libraries, parentURI, parentURI);

        // Go through the JARs to be added to the classpath.  For each one check
        // to see if it has a Manifest Class-Path entry.  If so, add each
        // part of the Class-Path entry to the list of libraries.  Note that the
        // entries can be either JARs or directories.  Either will work.
        addClassPathElementsFromManifestClassPaths(appArchive, libraries);

        /*
         * Copy any libraries we need.
         */
        copyLibraries(original, target, libraries);
    }

    /**
     * Populates a module archive.
     *<p>
     * Typically intended for use in processing a submodule contained withint
     * a parent archive.
     *@param original the ReadableArchive containing the input
     *@param generated the ReadableArchive for the generated bits for this application
     *@param target the archive to be created
     *@param libraries List of Strings (possibly added to) refering to JARs or directories
     *to be included in the generated app client JAR
     */
    static void populateModuleJar(ReadableArchive original,
            ReadableArchive generated, WritableArchive target,
            List<String> libraries, URI containingAppURI,
            URI moduleParentURI) throws IOException {

        // Exclude the client jar file we are trying to generate
        String excludeFileName =
                target.getURI().getSchemeSpecificPart().substring(
                target.getURI().getSchemeSpecificPart().lastIndexOf(
                File.separatorChar) + 1);
        Set excludeList = new HashSet();
        excludeList.add(excludeFileName);

        // Copy all the generated content first.  Note that this
        // needs to be done before copying the original content
        // as any duplicates are otherwise ignored.
        // Also the ClientJarMaker does not have special knowledge
        // on what's modified.  that burden is on the deployment process to
        // make sure all generated content are available in the generated/xml
        if (generated != null) {
            copyArchive(generated, target, excludeList);
        }

        // preserve all entries in the original appclient jar
        copyArchive(original, target, excludeList);

        // copy manifest file since it does not appear in the list of files
        copy(original, target, JarFile.MANIFEST_NAME);

        // Add the manifest Class-Path entries (if any) from the submodule's
        // archive to the accumulated library list
        Manifest mf = original.getManifest();
        addClassPathElementsFromManifest(mf, containingAppURI, moduleParentURI, libraries);
    }

    static void copyDeploymentDescriptors(
            Archivist archivist, ReadableArchive original,
            ReadableArchive generated, WritableArchive target)
            throws IOException {

        ReadableArchive source = (generated == null) ? original : generated;
        //standard dd
        copy(source, target,
                archivist.getStandardDDFile().getDeploymentDescriptorPath());
        //runtime dd
        copy(source, target,
                archivist.getConfigurationDDFile(original).getDeploymentDescriptorPath());
    }

    /**
     * This method finds all the library entries - JARs and directories - needed 
     * by the appclient.  Do not optimize by gathering all non-submodule JARs in
     * the archive because that (1) ignores directories in JAR Class-Path entries
     * and (2) may gather JARs that should be excluded.
     */
    static List<String> getLibraryEntries(
            Application app, ReadableArchive appSource)
            throws IOException {

        File appArchive = new File(appSource.getURI().getSchemeSpecificPart());

        Vector<String> libraries = new Vector();

        // Process any JARs in the <library-directory>.
        String libraryDirectoryPath = app.getLibraryDirectory();
        File libraryDirectory = null;
        if (libraryDirectoryPath != null) {
            libraryDirectory = new File(appArchive, libraryDirectoryPath);
            addJARClassPathElementsFromDirectory(libraryDirectory, appArchive, libraries);
        }

        // For backward compatibility process any JARs at the top level of the 
        // archive, unless that is also the <library-directory> which we don't
        // want to process twice.
        if (libraryDirectory == null || !libraryDirectory.equals(appArchive)) {
            addJARClassPathElementsFromDirectory(appArchive, appArchive, libraries);
        }

        if (DOLUtils.getDefaultLogger().isLoggable(Level.FINEST)) {
            for (String lib : libraries) {
                DOLUtils.getDefaultLogger().log(
                        Level.FINE, "Adding to the appclient jar, library [{0}]", lib);
            }
        }
        return libraries;
    }

    static void copyLibraries(ReadableArchive source, WritableArchive target,
            List<String> libraries) throws IOException {
        for (String library : libraries) {
            copy(source, target, library);
        }
    }

    /**
     * Adds entries to the collection of class path elements for each JAR in
     * the specified directory.  The entries are paths relative to the archive
     * root.
     *@param directory the directory to search for JARs
     *@param archiveRoot the root directory of the archive in which the directory resides
     *@param classPathEntries a Collection<String> to which class path entries are
     *added
     */
    private static void addJARClassPathElementsFromDirectory(File directory, File archiveRoot, Collection<String> classPathEntries) {
        // Make sure the directory exists and is a directory.  For example, the 
        // default library-directory might not be present, or the developer might
        // have specified a non-existent directory for the library directory or
        // in the Class-Path entry of a JAR's manifest.
        boolean isFine = logger.isLoggable(Level.FINE);
        StringBuilder jarsAdded = null;
        URI relativeDirURI = null;
        String relativeDirURIPath = null;
        if (isFine) {
            jarsAdded = new StringBuilder();
            relativeDirURI = archiveRoot.toURI().relativize(directory.toURI());
            // If the directory and the archive root are the same, the relatived
            // path is an empty string.  In that case use a "." instead for
            // clarity in the log messages.

            if (directory.equals(archiveRoot)) {
                relativeDirURIPath = ".";
            } else {
                relativeDirURIPath = relativeDirURI.getPath();
            }
        }

        if (directory.exists() && directory.isDirectory()) {
            for (File candidateJAR : directory.listFiles()) {
                // Include only JARs.  Make sure it is not a directory, because
                // directories can be arbitrarily named and can end with .jar.
                if (candidateJAR.getName().endsWith(".jar") && !candidateJAR.isDirectory()) {
                    URI jarRelativeURI = archiveRoot.toURI().relativize(candidateJAR.toURI());
                    classPathEntries.add(jarRelativeURI.getPath());
                    if (isFine) {
                        jarsAdded.append(relativeDirURI.relativize(jarRelativeURI)).append(" ");
                    }
                }
            }
        }
        if (isFine) {
            logger.log(Level.FINE, "Adding these JARs from directory {0} to app client JAR classpath: [{1}]", 
                    new Object[]{relativeDirURIPath, jarsAdded.toString()});
        }
    }

    /**
     * Adds class path elements to the list of libraries based on the 
     * Class-Path entries from the manifests of JARs already in the list.
     *@param appArchive the file where the application resides
     *@param libraries the List which contains JARs already and will be modified
     */
    static void addClassPathElementsFromManifestClassPaths(File appArchive, List<String> libraries) throws IOException {
        boolean isFine = logger.isLoggable(Level.FINE);
        URI appArchiveURI = appArchive.toURI();

        // Use a ListIterator so we can add elements during the iteration.
        for (ListIterator<String> it = libraries.listIterator(); it.hasNext();) {
            String entry = it.next();
            if (entry.endsWith(".jar")) {
                // Record class path elements added from this JAR
                StringBuilder elementsAdded = isFine ? new StringBuilder() : null;

                File jarFile = new File(appArchive, entry);
                /*
                 * Make sure this library exists before trying to open it to 
                 * process its manifest Class-Path entries.
                 */
                if (!jarFile.exists()) {
                    if (isFine) {
                        logger.log(Level.FINE, "Skipping manifest Class-Path processing for non-existent library {0}", 
                                jarFile.getAbsolutePath());
                    }
                    continue;
                }
                JarFile jar = new JarFile(jarFile);
                try {
                    Manifest mf = jar.getManifest();
                    List<String> classPathLibs = getClassPathElementsFromManifest(
                            mf, appArchiveURI, appArchiveURI.resolve(jarFile.toURI()));
                    /*
                     * Add each class path element from the manifest to the list of 
                     * libraries if it does not already exist.
                     */
                    for (String element : classPathLibs) {
                        if (!libraries.contains(element)) {
                            it.add(element);
                            if (isFine) {
                                elementsAdded.append(element).append(" ");
                            }
                        }
                    }
                    if (elementsAdded != null) {
                        if (elementsAdded.length() > 0) {
                            logger.log(Level.FINE, "Added following entries from {0} Class-Path to client JAR classpath: [ {1}]", 
                                    new Object[]{entry, elementsAdded.toString()});
                        } else {
                            logger.log(Level.FINE, "No Class-Path entries to add to client JAR classpath from manifest of {0}", entry);
                        }
                    }
                } finally {
                    if (jar != null) {
                        jar.close();
                    }
                }
            }
        }
    }

    /**
     * Returns a list of URIs (relative to the application) for the 
     * class path entries (if any) from the specified manifest.
     *@param mf the Manifest to be inspected
     *@param referencingURI the URI (relative to the application's top-level) of 
     * module or JAR containing the manifest
     *@return List<String> containing URIs in string form for the libraries needed
     * by this manifest's Class-Path
     */
    private static List<String> getClassPathElementsFromManifest(Manifest mf, URI appURI, URI referencingURI) {
        ArrayList<String> classPathLibs = new ArrayList<String>();
        if (mf == null) {
            return classPathLibs;
        }

        Attributes mainAttrs = mf.getMainAttributes();
        if (mainAttrs == null) {
            return classPathLibs;
        }

        String classPathString = mainAttrs.getValue(Attributes.Name.CLASS_PATH);
        if (classPathString == null || classPathString.length() == 0) {
            return classPathLibs;
        }

        for (String classPathElement : classPathString.split(" ")) {
            URI elementURI = appURI.relativize(referencingURI.resolve(classPathElement));
            classPathLibs.add(elementURI.toString());
        }

        return classPathLibs;
    }

    private static void addClassPathElementsFromManifest(Manifest mf, URI appURI, URI moduleParentURI, List<String> libraries) {
        for (String element : getClassPathElementsFromManifest(mf, appURI, moduleParentURI)) {
            libraries.add(element);
        }
    }

    /**
     * copy the entryName element from the source abstract archive into
     * the target abstract archive
     */
    static void copy(
            ReadableArchive source, WritableArchive target, String entryName)
            throws IOException {

        InputStream is = null;
        OutputStream os = null;
        try {
            is = source.getEntry(entryName);
            if (is != null) {
                try {
                    os = target.putNextEntry(entryName);
                } catch (ZipException ze) {
                    // this is a duplicate...
                    return;
                }
                ArchivistUtils.copyWithoutClose(is, os);
            } else {
                // This may be a directory specification if there is no entry
                // in the source for it...for example, a directory expression
                // in the Class-Path entry from a JAR's manifest.  
                // 
                // Try to copy all entries from the source that have the 
                // entryName as a prefix.
                for (Enumeration e = source.entries(entryName); e.hasMoreElements();) {
                    copy(source, target, (String) e.nextElement());
                }
            }
        } catch (IOException ioe) {
            throw ioe;
        } finally {
            IOException closeEntryIOException = null;
            if (os != null) {
                try {
                    target.closeEntry();
                } catch (IOException ioe) {
                    closeEntryIOException = ioe;
                }
            }
            if (is != null) {
                is.close();
            }

            if (closeEntryIOException != null) {
                throw closeEntryIOException;
            }
        }
    }

    /**
     * Returns a URI representing the parent of the specified URI.
     *@param uri the URI whose parent is needed
     *@return the URI of the parent; could be the empty string
     */
    static URI getParent(URI uri) throws URISyntaxException {
        return getParent(uri.toString());
    }

    /**
     * Returns a URI representing the parent of the specified URI.
     *@param uriString the URI whose parent is needed
     *@return the URI of the parent; could be the empty string
     */
    static URI getParent(String uriString) throws URISyntaxException {
        int endOfParentURI = uriString.lastIndexOf("/") + 1;
        String parentURIString = endOfParentURI != 0 ? uriString.substring(0, endOfParentURI) : "";
        URI parentURI = new URI(parentURIString);
        return parentURI;
    }

    static void copyArchive(
            ReadableArchive source, WritableArchive target, Set excludeList) {
        for (Enumeration e = source.entries(); e.hasMoreElements();) {
            String entryName = String.class.cast(e.nextElement());
            if (excludeList.contains(entryName)) {
                continue;
            }
            try {
                copy(source, target, entryName);
            } catch (IOException ioe) {
                // duplicate, we ignore
            }
        }
    }

}
