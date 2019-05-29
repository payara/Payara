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

package org.glassfish.appclient.client.acc;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.api.deployment.archive.ReadableArchive;
import com.sun.enterprise.deploy.shared.AbstractReadableArchive;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;

/**
 * Implements ReadableArchive for the http (and https) protocol to support
 * launches of app clients using Java Web Start.
 * <p>
 * Although the JARs are stored as JARs in the Java Web Start cache,
 * Java Web Start hides the actual location where the cached JAR resides.
 * So this implementation does not rely on the JARs location but uses
 * URLs to access the archive itself and its elements.
 *
 * @author tjquinn
 */
@Service(name="http")
@PerLookup
public class HTTPInputArchive extends AbstractReadableArchive {

    private URI archiveURI = null;
    private URL archiveURL = null;
    
    /** caches the manifest so we read if from the JAR at most once */
    private Manifest cachedManifest = null;

    /** caches the archive size to avoid opening connections to it multiple times. The real cached value will never be -2 */
    private int cachedArchiveSize = -2;

    /** caches the list of all entries; reused for subsequent calls to the entries methods */
    private Collection<String> cachedEntryNames = null;

    /** caches whether or not this archive exists */
    private Boolean exists;


    @Override
    public InputStream getEntry(String name) throws IOException {
        try {
            return entryURL(name).openStream();
        } catch (FileNotFoundException e) {
            return null;
        }
        
    }
    
    private URL entryURL(String name) throws MalformedURLException {
        if (! (name.charAt(0) == '/')) {
            name = "/" + name;
        }
        return new URL("jar:" + archiveURI.toASCIIString() + "!" + name);
    }

    @Override
    public synchronized boolean exists(String name) throws IOException {
        if (cachedEntryNames != null) {
            return cachedEntryNames.contains(name);
        }
        return getEntry(name) != null;
    }

    @Override
    public long getEntrySize(String name) {
        try {
            URLConnection cnx = entryURL(name).openConnection();
            return cnx.getContentLength();
        } catch (Exception ex) {
            Logger.getLogger(HTTPInputArchive.class.getName()).log(Level.SEVERE, null, ex);
            return -1;
        }
    }

    @Override
    public void open(URI uri) throws IOException {
        archiveURI = uri;
        archiveURL = uri.toURL();
    }

    @Override
    public ReadableArchive getSubArchive(String name) throws IOException {
        throw new UnsupportedOperationException("Nested archives not supported in ACC");
    }

    @Override
    public boolean exists() {
        if (exists != null) {
            return exists.booleanValue();
        }
        InputStream is = null;
        exists = Boolean.FALSE;
        try {
            is = archiveURL.openStream();
            exists = Boolean.TRUE;
            is.close();
        } finally {
            return exists.booleanValue();
        }
    }

    @Override
    public boolean delete() {
        throw new UnsupportedOperationException("delete not supported");
    }

    @Override
    public boolean renameTo(String name) {
        throw new UnsupportedOperationException("renameTo supported");
    }

    @Override
    public void close() throws IOException {
//        archiveURI = null;
//        archiveURL = null;
    }

    @Override
    public Enumeration<String> entries() {
        /*
         * This case is easy - just wrap an Enumerator interface around
         * an iterator we can get directly from the names cache.
         */
        try {
            final Iterator<String> it = entryNames().iterator();
            return new Enumeration<String>() {

                @Override
                public boolean hasMoreElements() {
                    return it.hasNext();
                }

                @Override
                public String nextElement() {
                    return it.next();
                }
            };
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns a collection containing the names of all entries in the
     * archive.
     *
     * @return
     * @throws IOException
     */
    private synchronized Collection<String> entryNames() throws IOException {
        if (cachedEntryNames == null) {
            /*
             * We have to build the cache first.
             */
            cachedEntryNames = new ArrayList<String>();
            final JarInputStream jis = new JarInputStream(archiveURL.openStream());
            JarEntry entry;
            try {
                while ((entry = jis.getNextJarEntry()) != null) {
                    cachedEntryNames.add(entry.getName());
                }
            } finally {
                jis.close();
            }
        }
        return cachedEntryNames;
    }

    @Override
    public Enumeration<String> entries(final String prefix) {
        try {
            /*
             * This is trickier than the non-prefix case because we have to
             * look ahead in the iterator of all the names.
             */
            final Iterator<String> it = entryNames().iterator();
            return new Enumeration<String>() {

                /** preloaded with the first match, if any. */
                private String nextName = nextMatch();

                @Override
                public boolean hasMoreElements() {

                    return nextName != null;
                }

                @Override
                public String nextElement() {
                    final String result = nextName;
                    nextName = nextMatch();
                    return result;
                }

                /**
                 * Returns the next entry name from the entry name cache which
                 * matches the specified prefix.
                 *
                 * @return next matching name if there is one; null otherwise
                 */
                private String nextMatch() {
                    String nextMatch = null;
                    while (it.hasNext() && ( ! (nextMatch = it.next()).startsWith(prefix))) {
                    }
                    return nextMatch;
                }
            };
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public Collection<String> getDirectories() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isDirectory(String name) {
        JarInputStream jis = null;
        try {
            jis = new JarInputStream(entryURL(name).openStream());
            JarEntry entry = jis.getNextJarEntry();
            return entry.isDirectory();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        } finally {
            if (jis != null) {
                try {
                    jis.close();
                } catch (IOException ignore) {

                }
            }
        }
    }

    @Override
    public synchronized Manifest getManifest() throws IOException {
        if (cachedManifest != null) {
            return cachedManifest;
        }
        final InputStream manifestIS = getEntry(JarFile.MANIFEST_NAME);
        if (manifestIS == null) {
            return null;
        }
        return (cachedManifest = new Manifest(manifestIS));
    }

    @Override
    public URI getURI() {
        return archiveURI;
    }

    @Override
    public long getArchiveSize() throws SecurityException {
        if (cachedArchiveSize != -2) {
            return cachedArchiveSize;
        }
        try {
            URLConnection cnx = archiveURL.openConnection();
            return (cachedArchiveSize = cnx.getContentLength());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getName() {
        return archiveURI.getPath();
    }
}
