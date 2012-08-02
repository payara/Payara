/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2012 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.deployment.deploy.shared;

import org.glassfish.api.deployment.archive.WritableArchive;
import org.jvnet.hk2.annotations.Service;

import org.glassfish.hk2.api.PerLookup;

import java.io.*;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Collection;
import java.util.jar.Manifest;
import java.util.jar.JarEntry;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Provides an implementation of the Archive that maps to
 * a Jar file @see java.util.jar.JarFile
 *
 * @author Jerome Dochez
 */
@Service(name="jar")
@PerLookup
public class OutputJarArchive extends JarArchive implements WritableArchive {

    // the path
    private URI uri;

    // the file we are currently mapped to (if open for writing)
    protected ZipOutputStream jos = null;

    private Manifest manifest = null;

    // list of entries already written to this ouput
    private Vector entries = new Vector();

    /**
     * Get the size of the archive
     *
     * @return -1 because this is getting created
     */
    public long getArchiveSize() throws NullPointerException, SecurityException {
        return -1;
    }

    /**
     * close the abstract archive
     */
    public void close() throws IOException {
        if (jos != null) {
            jos.flush();
            jos.finish();
            jos.close();
            jos = null;
        }
    }

    protected JarEntry getJarEntry(String entryName) {
        return null; 
    }

    /**
     * creates a new abstract archive with the given path
     *
     * @param path the path to create the archive
     */
    public void create(URI path) throws IOException {
        this.uri = path;
        File file = new File(uri.getSchemeSpecificPart());
        // if teh file exists, we delete it first
        if (file.exists()) {
            boolean isDeleted = file.delete();
            if (!isDeleted) {
                Logger.getAnonymousLogger().log(Level.WARNING, "Error in deleting file " + file.getAbsolutePath());
            }
        }
        FileOutputStream fos = new FileOutputStream(file);
        BufferedOutputStream bos = new BufferedOutputStream(fos);
        jos = new ZipOutputStream(bos);
    }

    /**
     * @return an @see java.util.Enumeration of entries in this abstract
     *         archive
     */
    public Enumeration entries() {
        return entries.elements();
    }

    public Collection<String> getDirectories() throws IOException {
        return new Vector<String>();
    }

    /**
     * @return an @see java.util.Enumeration of entries in this abstract
     *         archive, providing the list of embedded archive to not count their
     *         entries as part of this archive
     */
    public Enumeration entries(Enumeration embeddedArchives) {
        return entries();
    }

    /**
     * @return the manifest information for this abstract archive
     */
    public Manifest getManifest() throws IOException {
        if (manifest == null) {
            manifest = new Manifest();
        }
        return manifest;
    }

    /**
     * Returns the path used to create or open the underlyong archive
     *
     * @return the path for this archive.
     */
    public URI getURI() {
        return uri;
    }

    public WritableArchive createSubArchive(String name) throws IOException {
        OutputStream os = putNextEntry(name);
        ZipOutputStream jos = new ZipOutputStream(os);
        OutputJarArchive ja = new OutputJarArchive();
        try {
            ja.uri = new URI("jar", name, null);
        } catch(URISyntaxException e) {

        }
        ja.jos = jos;
        return ja;
    }

    /**
     * Close a previously returned sub archive
     *
     * @param subArchive output stream to close
     * @link Archive.getSubArchive}
     */
    public void closeEntry(WritableArchive subArchive) throws IOException {
        if (subArchive instanceof OutputJarArchive) {
            ((OutputJarArchive) subArchive).jos.flush();
            ((OutputJarArchive) subArchive).jos.finish();
        }
        jos.closeEntry();
    }


    /**
     * @param name the entry name
     * @returns an @see java.io.OutputStream for a new entry in this
     * current abstract archive.
     */
    public OutputStream putNextEntry(String name) throws java.io.IOException {
        if (jos != null) {
            ZipEntry ze = new ZipEntry(name);
            jos.putNextEntry(ze);
            entries.add(name);
        }
        return jos;
    }


    /**
     * closes the current entry
     */
    public void closeEntry() throws IOException {
        if (jos != null) {
            jos.flush();
            jos.closeEntry();
        }
    }

}
