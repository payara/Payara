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

package com.sun.enterprise.deployment.deploy.shared;

import com.sun.enterprise.util.i18n.StringManager;
import com.sun.enterprise.util.io.FileUtils;
import java.net.MalformedURLException;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.glassfish.deployment.common.DeploymentUtils;
import org.jvnet.hk2.annotations.Service;

import org.glassfish.hk2.api.PerLookup;

import java.io.*;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.net.URI;
import java.net.URISyntaxException;

import org.glassfish.logging.annotation.LogMessageInfo;

/**
 * This implementation of the Archive deal with reading
 * jar files either from a JarFile or from a JarInputStream
 *
 * @author Jerome Dochez
 */
@Service(name="jar")
@PerLookup
public class InputJarArchive extends JarArchive implements ReadableArchive {
    
    public static final Logger deplLogger = org.glassfish.deployment.common.DeploymentContextImpl.deplLogger;

    @LogMessageInfo(message = " file open failure; file = {0}", level="WARNING")
    private static final String FILE_OPEN_FAILURE = "NCLS-DEPLOYMENT-00019";

    @LogMessageInfo(message = "exception message:  {0} -- invalid zip file: {1}", level="WARNING")
    private static final String INVALID_ZIP_FILE = "NCLS-DEPLOYMENT-00020";

    // the file we are currently mapped to 
    volatile protected JarFile jarFile=null;
    
    // in case this abstraction is dealing with a jar file
    // within a jar file, the jarFile will be null and this
    // JarInputStream will contain the 
    volatile protected JarInputStream jarIS=null;
    
    // the archive Uri
    volatile private URI uri;

    // parent jar file for embedded jar
    private InputJarArchive parentArchive=null;

    private static StringManager localStrings = StringManager.getManager(InputJarArchive.class);

    // track entry enumerations to close them if needed when the archive is closed
    private final WeakHashMap<EntryEnumeration,Object> entryEnumerations =
            new WeakHashMap<EntryEnumeration,Object>();

    /**
     * Get the size of the archive
     * @return tje the size of this archive or -1 on error
     */
    public long getArchiveSize() throws NullPointerException, SecurityException {
        if(uri == null) {
            return -1;
        }
        File tmpFile = new File(uri);
        return(tmpFile.length());
    }
    
    /** @return an @see java.io.OutputStream for a new entry in this
     * current abstract archive.
     * @param name the entry name
     */
    public OutputStream addEntry(String name) throws IOException {
        throw new UnsupportedOperationException("Cannot write to an JAR archive open for reading");        
    }
    
    /** 
     * close the abstract archive
     */
    public synchronized void close() throws IOException {
        for (EntryEnumeration e : entryEnumerations.keySet()) {
            e.closeNoRemove();
        }
        entryEnumerations.clear();
        if (jarFile!=null) {
            jarFile.close();
            jarFile=null;
        }
        if (jarIS!=null) {
            jarIS.close();
            jarIS=null;
        }
    }

    private synchronized EntryEnumeration recordEntryEnumeration(final EntryEnumeration e) {
        entryEnumerations.put(e, null);
        return e;
    }

    /**
     * Returns the collection of first level directories in this
     * archive.
     * <p>
     * Avoid having to fetch all the entries if we can avoid it.  The only time
     * we must do that is if size() is invoked on the collection.  Use
     * the CollectionWrappedEnumeration for this optimization which will
     * create a full in-memory list of the entries only if and when needed
     * to satisfy the size() method.
     *
     * @return collection of directories under the root of this archive
     */
    @Override
    public Collection<String> getDirectories() throws IOException {
        return new CollectionWrappedEnumeration<String>(
                new CollectionWrappedEnumeration.EnumerationFactory<String>() {

            @Override
            public Enumeration<String> enumeration() {
                return entries(true);
            }
        });
    }

    /** 
     * creates a new abstract archive with the given path
     *
     * @param uri the path to create the archive
     */
    public void create(URI uri) throws IOException {
        throw new UnsupportedOperationException("Cannot write to an JAR archive open for reading");        
    }

    @Override
    public Enumeration<String> entries() {
        return entries(false);
    }

    /**
     * Returns an enumeration of the entry names in the archive.
     *
     * @param topLevelDirectoriesOnly whether to report directories only or non-directories only
     * @return enumeration of the matching entry names, excluding the manifest
     */
    private Enumeration<String> entries(final boolean topLevelDirectoriesOnly) {
        try {
            /*
             * We have two decisions to make:
             *
             * 1. whether the caller wants top-level directory entries or all
             * non-directory entries enumerated, and
             *
             * 2. how to obtain the sequence of JarEntry objects which we filter
             * before returning their names.
             *
             */
            return recordEntryEnumeration(createEntryEnumeration(topLevelDirectoriesOnly));
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    /**
     *  @return an @see java.util.Enumeration of entries in this abstract
     * archive, providing the list of embedded archive to not count their 
     * entries as part of this archive
     */
     public Enumeration entries(Enumeration embeddedArchives) {
	// jar file are not recursive    
  	return entries();
    }

    public JarEntry getJarEntry(String name) {
        if (jarFile!=null) {
            return jarFile.getJarEntry(name);
        }
        return null;
    }
    
    /**
     * Returns the existence of the given entry name
     * The file name must be relative to the root of the module.
     *
     * @param name the file name relative to the root of the module.          * @return the existence the given entry name.
     */
    public boolean exists(String name) throws IOException {
        if (jarFile!=null) {
            ZipEntry ze = jarFile.getEntry(name);
            if (ze!=null) {
                return true;
            }
        }
        return false;
    }    

    /**
     * @return a @see java.io.InputStream for an existing entry in
     * the current abstract archive
     * @param entryName entry name
     */
    public InputStream getEntry(String entryName) throws IOException {
        if (jarFile!=null) {
            ZipEntry ze = jarFile.getEntry(entryName);
            if (ze!=null) {
                return new BufferedInputStream(jarFile.getInputStream(ze));
            } else {
                return null;
            }            
        } else
	if ((parentArchive != null) && (parentArchive.jarFile != null)) {
            JarEntry je;
            // close the current input stream
            if (jarIS!=null) {
                jarIS.close();
            }
            
            // reopen the embedded archive and position the input stream
            // at the beginning of the desired element
	    JarEntry archiveJarEntry = (uri != null)? parentArchive.jarFile.getJarEntry(uri.getSchemeSpecificPart()) : null;
	    if (archiveJarEntry == null) {
		return null;
	    }
            jarIS = new JarInputStream(parentArchive.jarFile.getInputStream(archiveJarEntry));
            do {
                je = jarIS.getNextJarEntry();
            } while (je!=null && !je.getName().equals(entryName));
            if (je!=null) {
                return new BufferedInputStream(jarIS);
            } else {
                return null;
            }
        } else {
	    return null;
	}
    }

    /**
     * Returns the entry size for a given entry name or 0 if not known
     *
     * @param name the entry name
     * @return the entry size
     */
    public long getEntrySize(String name) {
        if (jarFile!=null) {
            ZipEntry ze = jarFile.getEntry(name);
            if (ze!=null) {
                return ze.getSize();
            }
        }
        return 0;
    }

    /** Open an abstract archive
     * @param uri the path to the archive
     */
    public void open(URI uri) throws IOException {
       this.uri = uri;
       jarFile = getJarFile(uri);
    }
    
    /**
     * @return a JarFile instance for a file path
     */
    protected static JarFile getJarFile(URI uri) throws IOException {
        JarFile jf = null;
        try {
            File file = new File(uri);
            if (file.exists()) {
                jf = new JarFile(file);
            }
        } catch(IOException e) {
            deplLogger.log(Level.WARNING,
                           FILE_OPEN_FAILURE,
                           new Object[]{uri});
            // add the additional information about the path
            // since the IOException from jdk doesn't include that info
            String additionalInfo = localStrings.getString(
                "enterprise.deployment.invalid_zip_file", uri);
            deplLogger.log(Level.WARNING,
                           INVALID_ZIP_FILE,
                           new Object[] { e.getLocalizedMessage(), additionalInfo } );
        }
        return jf;
    }       
    
    
    /** 
     * @return the manifest information for this abstract archive
     */
    public Manifest getManifest() throws IOException {
        if (jarFile!=null) {
            return jarFile.getManifest();
        } 
        if (parentArchive!=null) {    
            // close the current input stream
            if (jarIS!=null) {
                jarIS.close();
            }
            // reopen the embedded archive and position the input stream
            // at the beginning of the desired element
            if (jarIS==null) {
                jarIS = new JarInputStream(parentArchive.jarFile.getInputStream(parentArchive.jarFile.getJarEntry(uri.getSchemeSpecificPart())));
            }
            Manifest m = jarIS.getManifest();
            if (m==null) {
               java.io.InputStream is = getEntry(java.util.jar.JarFile.MANIFEST_NAME);
               if (is!=null) {
                    m = new Manifest();
                    m.read(is);
                    is.close();
               }
            }
            return m;
        }                        
        return null;
    }

    /**
     * Returns the path used to create or open the underlying archive
     *
     * @return the path for this archive.
     */
    public URI getURI() {
        return uri;
    }

    /**
     * @return true if this abstract archive maps to an existing 
     * jar file
     */
    public boolean exists() {
        return jarFile!=null;
    }
    
    /**
     * deletes the underlying jar file
     */
    public boolean delete() {
        if (jarFile==null) {
            return false;
        }
        try {
            jarFile.close();
            jarFile = null;
        } catch (IOException ioe) {
            return false;
        }
        return FileUtils.deleteFile(new File(uri));
    }
    
    /**
     * rename the underlying jar file
     */
    public boolean renameTo(String name) {
        if (jarFile==null) {
            return false;
        }
        try {
            jarFile.close();
            jarFile = null;
        } catch (IOException ioe) {
            return false;
        }        
        return FileUtils.renameFile(new File(uri), new File(name));
    }
    
    /**
     * @return an Archive for an embedded archive indentified with
     * the name parameter
     */
    public ReadableArchive getSubArchive(String name) throws IOException {
        if (jarFile!=null) {
            // for now, I only support one level down embedded archives
            InputJarArchive ija = new InputJarArchive();
            JarEntry je = jarFile.getJarEntry(name);
            if (je!=null) {
                JarInputStream jis = new JarInputStream(new BufferedInputStream(jarFile.getInputStream(je)));
                try {
                    ija.uri = new URI("jar",name, null);
                } catch(URISyntaxException e) {
                    // do nothing
                }
                ija.jarIS = jis;
                ija.parentArchive = this;
                return ija;
            }
        }
        return null;
    }

    /**
     * Creates the correct type of entry enumeration, depending on whether the
     * current archive is nested or not and depending on whether the caller
     * requested top-level directory entries or all non-directory entries be returned
     * in the enumeration.
     *
     * @param uriToReadForEntries
     * @param topLevelDirectoriesOnly
     * @return
     * @throws FileNotFoundException
     * @throws IOException
     */
    private EntryEnumeration createEntryEnumeration(
            final boolean topLevelDirectoriesOnly) throws FileNotFoundException, IOException {
        final JarEntrySource source = (parentArchive == null ?
            new ArchiveJarEntrySource(uri) :
            new SubarchiveJarEntrySource(parentArchive.jarFile, uri));
        if (topLevelDirectoriesOnly) {
            return new TopLevelDirectoryEntryEnumeration(source);
        } else {
            return new NonDirectoryEntryEnumeration(source);
        }
    }

    /**
     * Logic for enumerations of the entry names that is common between the
     * top-level directory entry enumeration and the full non-directory
     * enumeration.
     * <p>
     * The goal is to wrap an Enumeration around the underlying entries 
     * available in the archive.  This avoids collecting all
     * the entry names first and then returning an enumeration of the collection;
     * that can be very costly for large JARs.
     * <p>
     * But, the trade-off is that we need to be careful because we leave a stream
     * opened to the JAR.  So, even though the finalizer is not guaranteed to be
     * invoked, we still provide one to close up the JarFile.  This should help
     * reduce the chance for locked JARs on Windows due to open streams.
     */
    private abstract class EntryEnumeration implements Enumeration<String> {

        /* look-ahead of one entry */
        private JarEntry nextMatchingEntry;

        /* source of JarEntry objects for building the enumeration values */
        private final JarEntrySource jarEntrySource;

        private EntryEnumeration(final JarEntrySource jarEntrySource) {
            this.jarEntrySource = jarEntrySource;
        }

        /**
         * Finishes the initialization for the enumeration; MUST be invoked
         * from the subclass constructor after super(...).
         */
        protected void completeInit() {
            nextMatchingEntry = skipToNextMatchingEntry();
        }

        @Override
        public boolean hasMoreElements() {
            return nextMatchingEntry != null;
        }

        @Override
        public String nextElement() {
            if (nextMatchingEntry == null) {
                throw new NoSuchElementException();
            }
            final String answer = nextMatchingEntry.getName();
            nextMatchingEntry = skipToNextMatchingEntry();
            return answer;
        }

        protected JarEntry getNextJarEntry() throws IOException {
            return jarEntrySource.getNextJarEntry();
        }

        /**
         * Returns the next JarEntry available from the archive.
         * <p>
         * Different concrete subclasses implement this differently so as to
         * enumerate the correct sequence of entry names.
         *
         * @return the next available JarEntry; null if no more are available
         */
        protected abstract JarEntry skipToNextMatchingEntry();

        private void closeNoRemove() {
            try {
                jarEntrySource.close();
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }

        void close() {
            closeNoRemove();
            entryEnumerations.remove(this);
        }

        @Override
        protected void finalize() throws Throwable {
            super.finalize();
            close();
        }
    }

    /**
     * Defines behavior for sources of JarEntry objects for EntryEnumeration
     * implementations.
     * <p>
     * The implementation must be different for top-level archives vs. 
     * subarchives.
     */
    private interface JarEntrySource {

        /**
         * Returns the next JarEntry from the raw entries() enumeration
         * of the archive or subarchive.
         * @return JarEntry for the next entry in the JarArchive
         * @throws IOException
         */
        JarEntry getNextJarEntry() throws IOException;

        /**
         * Closes the source of the JarEntry objects.
         * @throws IOException
         */
        void close() throws IOException;
    }

    /**
     * Source of JarEntry objects for a top-level archive (as opposed to a
     * subarchive).
     */
    private static class ArchiveJarEntrySource implements JarEntrySource {

        private JarFile sourceJarFile;

        private Enumeration<JarEntry> jarEntries;

        private ArchiveJarEntrySource(final URI archiveURI) throws IOException {
            sourceJarFile = getJarFile(archiveURI);
            if (sourceJarFile == null){
                throw new IOException(localStrings.getString(
                        "enterprise.deployment.invalid_zip_file", archiveURI));
            }
            jarEntries = sourceJarFile.entries();
        }

        @Override
        public JarEntry getNextJarEntry() {
            return (jarEntries.hasMoreElements()) ? jarEntries.nextElement() : null;
        }

        @Override
        public void close() throws IOException {
            sourceJarFile.close();
        }
    }

    /**
     * Source of JarEntry objects for a subarchive.
     */
    private static class SubarchiveJarEntrySource implements JarEntrySource {

        private JarInputStream jis;

        private SubarchiveJarEntrySource(final JarFile jf, final URI uri) throws IOException {
            final JarEntry subarchiveJarEntry = jf.getJarEntry(uri.getSchemeSpecificPart());
            jis = new JarInputStream(jf.getInputStream(subarchiveJarEntry));
        }

        @Override
        public JarEntry getNextJarEntry() throws IOException {
            return jis.getNextJarEntry();
        }

        @Override
        public void close() throws IOException {
            jis.close();
        }

    }

    /**
     * Enumerates the top-level directory entries.
     * <p>
     * This implementation uses the enumeration of JarEntry objects from the
     * JarFile itself.
     */
    private class TopLevelDirectoryEntryEnumeration extends EntryEnumeration {

        private TopLevelDirectoryEntryEnumeration(final JarEntrySource jarEntrySource) throws FileNotFoundException, IOException {
            super(jarEntrySource);
            completeInit();
        }

        @Override
        protected JarEntry skipToNextMatchingEntry() {
            JarEntry candidateNextEntry;

            try {
                /*
                 * The next entry should be returned (and not skipped) if the entry is a
                 * directory entry and it contains only a single slash at the
                 * end of the entry name.
                 */
                while ((candidateNextEntry = getNextJarEntry()) != null) {
                    final String candidateNextEntryName = candidateNextEntry.getName();
                    if ( candidateNextEntry.isDirectory() &&
                           (candidateNextEntryName.indexOf('/') ==
                                candidateNextEntryName.lastIndexOf('/')) &&
                           (candidateNextEntryName.indexOf('/') ==
                                candidateNextEntryName.length() - 1)
                       ) {
                        break;
                    }
                }
                return candidateNextEntry;
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    /**
     * Enumerates entries in the archive that are not directories.
     */
    private class NonDirectoryEntryEnumeration extends EntryEnumeration {

        private NonDirectoryEntryEnumeration(final JarEntrySource jarEntrySource) throws IOException {
            super(jarEntrySource);
            completeInit();
        }

        @Override
        protected JarEntry skipToNextMatchingEntry() {
            JarEntry candidateNextEntry;
            try {
                /*
                 * The next entry should be returned (and not skipped) if the entry is
                 * not a directory entry and if it also not the manifest.
                 */
                while ((candidateNextEntry = getNextJarEntry()) != null) {
                    final String candidateNextEntryName = candidateNextEntry.getName();
                    if ( ! candidateNextEntry.isDirectory() &&
                              ! candidateNextEntryName.equals(JarFile.MANIFEST_NAME)
                       ) {
                        break;
                    }
                }
                return candidateNextEntry;
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }
    
    /**
     * A Collection which wraps an Enumeration.
     * <p>
     * Note that the nextSlot field is always updated, even if we are using
     * the original enumeration to return the next value from the iterator.  This
     * is so that, if the caller invokes size() which causes us to build the
     * ArrayList containing all the elements -- even if that invocation comes while
     * the iterator is being used to return values -- the subsequent invocations
     * of hasNext and next will use the correct place in the newly-constructed
     * ArrayList of values.
     *
     * @param <T>
     */
    static class CollectionWrappedEnumeration<T> extends AbstractCollection<T> {

        /** Used only if size is invoked */
        private ArrayList<T> entries = null;

        /** always updated, even if we use the enumeration */
        private int nextSlot = 0;

        private final EnumerationFactory<T> factory;

        private Enumeration<T> e;

        static interface EnumerationFactory<T> {
            public Enumeration<T> enumeration();
        }

        CollectionWrappedEnumeration(final EnumerationFactory<T> factory) {
            this.factory = factory;
            e = factory.enumeration();
        }

        @Override
        public Iterator<T> iterator() {
            return new Iterator<T>() {

                @Override
                public boolean hasNext() {
                    return (entries != null) ?
                        nextSlot < entries.size() :
                        e.hasMoreElements();
                }

                @Override
                public T next() {
                    T result = null;
                    if (entries != null) {
                        if (nextSlot >= entries.size()) {
                            throw new NoSuchElementException();
                        }
                        result = entries.get(nextSlot++);
                    } else {
                        result = e.nextElement();
                        nextSlot++;
                    }
                    return result;
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
            };
        }

        @Override
        public int size() {
            if (entries == null) {
                populateEntries();
            };
            return entries.size();
        }

        private void populateEntries() {
            entries = new ArrayList<T>();
            /*
             * Fill up the with data from
             * a new enumeration.
             */
            for (Enumeration<T> newE = factory.enumeration(); newE.hasMoreElements(); ) {
                entries.add(newE.nextElement());
            }
            e = null;
        }
    }
}

