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

package org.glassfish.internal.embedded;

import org.glassfish.api.deployment.archive.ReadableArchiveAdapter;

import java.io.*;
import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.jar.Manifest;
import java.util.jar.JarFile;
import java.util.zip.ZipEntry;
import java.util.jar.JarEntry;

/**
 * Abstraction for a scattered archive (parts disseminated in various directories)
 *
 * @author Jerome Dochez
 */
public class ScatteredArchive extends ReadableArchiveAdapter {


    public static class Builder {

        final String name;
        File topDir = null;
        File resources = null;
        final List<URL> urls = new ArrayList<URL>();
        final Map<String, File> metadata = new HashMap<String, File>();


        /**
         * Construct a new scattered archive builder with the minimum information
         * By default, a scattered archive is not different from any other
         * archive where all the files are located under a top level
         * directory (topDir).
         * Some files can then be scattered in different locations and be specified
         * through the appropriate setters.
         * Alternatively, topDir can be null to specify a truely scattered archive
         * and all the locations must be specified.
         *
         * @param name   archive name
         * @param topDir top level directory
         */
        public Builder(String name, File topDir) {
            this.name = name;
            this.topDir = topDir;
        }

        /**
         * Construct a new scattered archive builder with a set of URLs as repository
         * for locating archive resources (like .class files).
         * @param name archive name
         * @param urls set of resources repository
         */
        public Builder(String name, Collection<URL> urls) {
            this.name = name;
            for (URL u : urls) {
                this.urls.add(u);
            }
        }

        /**
         * Sets the location of resources files
         *
         * @param resources the resources directory
         * @return itself
         */
        public Builder resources(File resources) {
            if (!resources.exists()) {
                throw new IllegalArgumentException(resources.getAbsolutePath() + " not found");
            }
            this.resources = resources;
            return this;
        }

        /**
         * Add a new metadata locator for this scattered archive. A metadata is identified
         * by its name (like WEB-INF/web.xml) and the location of the metadata is used when
         * the embedded server is requesting the metadata file.
         * The name for this metadata will be obtained by doing metadata.getName()
         *
         * @param metadata the metadata file location
         *
         * @return itself
         */
        public Builder addMetadata(File metadata) {
            if (!metadata.exists()) {
                throw new IllegalArgumentException(metadata.getAbsolutePath() + " not found");
            }
            return addMetadata(metadata.getName(), metadata);
        }

        /**
         * Add a new metadata locator for this scattered archive. A metadata is identified
         * by its name (like WEB-INF/web.xml) and the location of the metadata is used when
         * the embedded server is requesting the metadata file.
         *
         * @param name name of the metadata (eg WEB-INF/web.xml or web.xml or META-INF/ejb.xml
         * or ejb.xml).
         *
         * @param metadata the metadata file location
         *
         * @return itself
         */
        public Builder addMetadata(String name, File metadata) {
            if (!metadata.exists()) {
                throw new IllegalArgumentException(metadata.getAbsolutePath() + " not found");
            }
            this.metadata.put(name, metadata);
            return this;
        }

        /**
         * Adds a directory to the classes classpath. Will be used to retrieve requested .class
         * files.
         *
         * @param location must be a directory location
         * @return itself
         */
        public Builder addClassPath(File location) {
            if (!location.isDirectory()) {
                throw new IllegalArgumentException("location is not a directory");
            }
            try {
                this.urls.add(location.toURI().toURL());
            } catch (MalformedURLException e) {
                throw new IllegalArgumentException(e);
            }
            return this;
        }

        /**
         * Adds a URL for the classes classpath. Will be used to retrieve requested .class files
         *
         * @param classpath the new classpath element.
         * @return itself
         */

        public Builder addClassPath(URL classpath) {
            this.urls.add(classpath);
            return this;
        }

        /**
         * Creates a new scattered jar file using this builder instance configuration.
         * The resulting instance will behave like a jar file when introspected by the
         * embedded instance.
         *
         * @return new scattered instance jar file
         */
        public ScatteredArchive buildJar() {
            return new ScatteredArchive(this, Builder.type.jar);
        }

        /**
         * Creates a new scattered war file using this builder instance configuration.
         * The resulting instance will behave like a war file when introspected by the
         * embedded instance.
         *
         * @return the scattered instance war file
         */
        public ScatteredArchive buildWar() {
            return new ScatteredArchive(this, Builder.type.war);
        }

        /**
         * Supported types of scattered archives.
         */
        public enum type {
            jar, war
        }
    }

    final String name;
    final File topDir;
    final File resources;
    final List<URL> urls = new ArrayList<URL>();
    final Map<String, File> metadata = new HashMap<String, File>();
    final Builder.type type;
    final String prefix;

    private ScatteredArchive(Builder builder, Builder.type type) {
        name = builder.name;
        topDir = builder.topDir;
        resources = builder.resources;
        urls.addAll(builder.urls);
        if (topDir!=null && type!=Builder.type.war) {
            try {
                urls.add(topDir.toURI().toURL());
            } catch (MalformedURLException ignore) {

            }
        }
        metadata.putAll(builder.metadata);
        this.type = type;
        prefix = type==Builder.type.war?"WEB-INF/classes":null;
    }

    // todo : look at Open(URI), is it ok ?

    /**
     * Get the classpath URLs
     *
     * @return A read-only copy of the classpath URL Collection
     */
    public Iterable<URL> getClassPath() {
        return Collections.unmodifiableCollection(urls);
    }

    /**
     * @return The resources directory
     */
    public File getResourcesDir() {
        return resources;
    }

    /**
     * Returns the InputStream for the given entry name
     * The file name must be relative to the root of the module.
     *
     * @param arg the file name relative to the root of the module.
     * @return the InputStream for the given entry name or null if not found.
     */

    public InputStream getEntry(String arg) throws IOException {
        File f = getFile(arg);
        if (f!=null && f.exists()) return new FileInputStream(f);
        JarFile jar = getJarWithEntry(arg);
        if (jar != null) {
            ZipEntry entry = jar.getEntry(arg);
            if (entry != null) {
                return jar.getInputStream(entry);
            }
        }
        return null;
    }

    @Override
    public long getEntrySize(String arg) {
        File f = getFile(arg);
        if (f!=null && f.exists()) return f.length();
        JarFile jar = getJarWithEntry(arg);
        if (jar != null) {
            ZipEntry entry = jar.getEntry(arg);
            if (entry != null) {
                return jar.getEntry(arg).getSize();
            }
        }
        return 0L;
    }

    /**
     * Returns whether or not a file by that name exists
     * The file name must be relative to the root of the module.
     *
     * @param name the file name relative to the root of the module.
     * @return does the file exist?
     */


    public boolean exists(String name) throws IOException {
        if ("WEB-INF".equals(name) && type == Builder.type.war) {
            return true;
        }
        return getEntry(name) != null;
    }

    /**
     * Returns an enumeration of the module file entries.  All elements
     * in the enumeration are of type String.  Each String represents a
     * file name relative to the root of the module.
     * <p><strong>Currently under construction</strong>
     *
     * @return an enumeration of the archive file entries.
     */
    public Enumeration<String> entries() {
        // TODO: abstraction breakage. We need file-level abstraction for archive
        // and then more structured abstraction.

        Vector<String> entries = new Vector<String>();
        File localResources = resources;

        for (URL url : urls) {
            try {
                if (localResources!=null && localResources.toURI().toURL().sameFile(url)) {
                    localResources=null;
                }
                File f;
                try {
                    f = new File(url.toURI());
                } catch(URISyntaxException e) {
                    f = new File(url.getPath());
                }
                if (f.isFile()) {
                    JarFile jar = new JarFile(f);
                    Enumeration<JarEntry> jarEntries = jar.entries();
                    while (jarEntries.hasMoreElements()) {
                        JarEntry jarEntry = jarEntries.nextElement();
                        if (jarEntry.isDirectory()) {
                            continue;
                        }
                        entries.add(jarEntry.getName());
                    }
                } else {
                    getListOfFiles(f, prefix, entries);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (localResources!=null) {

            getListOfFiles(localResources, null, entries);
        }
        return entries.elements();
    }

    private void getListOfFiles(File directory, String prefix, List<String> list) {
        if (!directory.isDirectory())
            return;
        for (File f : directory.listFiles()) {
            String name = prefix==null?f.getName():prefix+"/"+f.getName();
            if (f.isDirectory()) {
                getListOfFiles(f, name ,list);
            } else {
                list.add(name);
            }
        }
    }

    /**
     * Returns the manifest information for this archive
     *
     * @return the manifest info
     */
    public Manifest getManifest() throws IOException {
        InputStream is = getEntry(JarFile.MANIFEST_NAME);
        if (is != null) {
            try {
                return new Manifest(is);
            } finally {
                is.close();
            }
        }
        return new Manifest();
    }

    /**
     * Returns the path used to create or open the underlying archive
     * <p/>
     * <p/>
     * TODO: abstraction breakage:
     * Several callers, most notably {@link org.glassfish.api.deployment.DeploymentContext#getSourceDir()}
     * implementation, assumes that this URI is an URL, and in fact file URL.
     * <p/>
     * <p/>
     * If this needs to be URL, use of {@link URI} is misleading. And furthermore,
     * if its needs to be a file URL, this should be {@link File}.
     *
     * @return the path for this archive.
     */
    public URI getURI() {
        if (topDir != null) {
            return topDir.toURI();
        }
        if (resources != null) {
            return resources.toURI();
        }
        try {
            //TODO : Fix this
            if (urls.size() > 0) {
                for (URL url : urls) {
                    File f = new File(url.toURI());
                    if (f.isFile())
                        return url.toURI();
                }
                return urls.get(0).toURI();
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Returns the name of the archive.
     * <p/>
     * Implementations should not return null.
     *
     * @return the name of the archive
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the archive type
     * @return the archive type
     */
    public Builder.type type() {
        return type;
    }

    /**
     * Returns an enumeration of the module file entries with the
     * specified prefix.  All elements in the enumeration are of
     * type String.  Each String represents a file name relative
     * to the root of the module.
     * <p><strong>Currently Not Supported</strong>
     *
     * @param s the prefix of entries to be included
     * @return an enumeration of the archive file entries.
     */

    public Enumeration<String> entries(String s) {
        Enumeration <String> entries = entries();
        Vector<String> prefixedEntries = new Vector();
        while (entries.hasMoreElements()) {
            String entry = (String)entries.nextElement();
            if (entry.startsWith(s))
                prefixedEntries.add(entry);
        }
        return prefixedEntries.elements();
    }

    @Override
    public Collection<String> getDirectories() throws IOException {
        return new ArrayList<String>();
    }

    public String toString() {
        return super.toString() + " located at " + (topDir == null ? resources : topDir);
    }


    public File getFile(String name) {
        if (metadata.containsKey(name)) {
            return metadata.get(name);
        }
        String shortName = (name.indexOf("/") != -1 ? name.substring(name.indexOf("/") + 1) : name);
        if (metadata.containsKey(shortName)) {
            return metadata.get(shortName);
        }
        if (resources != null) {
            File f = new File(resources, name);
            if (f.exists()) {
                return f;
            }
        }
        if (prefix!=null) {
            if (name.startsWith(prefix)) {
                name = name.substring(prefix.length()+1);
            }
        }
        for (URL url : urls) {
            File f = null;
            try {
                f = new File(url.toURI());
            } catch(URISyntaxException e) {
                f = new File(url.getPath());
            }
            f = new File(f, name);
            if (f.exists()) {
                return f;
            }
        }
        return null;
    }

    private JarFile  getJarWithEntry(String name) {
        for (URL url : urls) {
            File f = null;
            try {
                f = new File(url.toURI());
            } catch(URISyntaxException e) {
                f = new File(url.getPath());
            }
            try {
                if (f.getName().endsWith(".jar")) {
                    JarFile jar = new JarFile(f);
                    if (jar.getEntry(name) != null) {
                        return jar;
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                return null;
            }
        }
        return null;
    }

   @Override
   public void close() throws IOException {
   }

}
