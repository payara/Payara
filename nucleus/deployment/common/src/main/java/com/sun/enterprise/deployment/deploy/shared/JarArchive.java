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


import java.net.URI;
import org.glassfish.api.deployment.archive.Archive;
import org.glassfish.api.deployment.archive.ReadableArchive;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Map;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.jar.JarEntry;
import java.io.IOException;

/**
 * This abstract class contains all common implementation of the
 * Archive/WritableArchive interfaces for Jar files
 *
 * @author Jerome Dochez
 */
public abstract class JarArchive implements Archive {

    protected ReadableArchive parentArchive;

    protected Map<Class<?>, Object> extraData=new HashMap<Class<?>, Object>();

    protected Map<String, Object> archiveMetaData = new HashMap<String, Object>();

    /**
     * Returns an enumeration of the module file entries with the
     * specified prefix.  All elements in the enumeration are of
     * type String.  Each String represents a file name relative
     * to the root of the module.
     *
     * @param prefix the prefix of entries to be included
     * @return an enumeration of the archive file entries.
     */
    public Enumeration<String> entries(String prefix) {
        Enumeration<String> allEntries = entries();
        Vector<String> entries = new Vector<String>();
        while (allEntries.hasMoreElements()) {
            String name = allEntries.nextElement();
            if (name != null && name.startsWith(prefix)) {
                entries.add(name);
            }
        }
        return entries.elements();
    } 
    
   /**
     * Returns the name portion of the archive's URI.
     * <p>
     * For JarArhive the name is all of the path that follows
     * the last slash up to but not including the last dot.
     * <p>
     * Here are some example archive names for the specified JarArchive paths:
     * <ul>
     * <li>/a/b/c/d.jar -> d
     * <li>/a/b/c/d  -> d
     * <li>/x/y/z.html -> z
     * </ul>
     * @return the name of the archive
     * 
     */
    public String getName() {
         return JarArchive.getName(getURI());
    }

    abstract protected JarEntry getJarEntry(String entryName);

    /**
     * Returns the existence of the given entry name
     * The file name must be relative to the root of the module.
     *
     * @param name the file name relative to the root of the module.          * @return the existence the given entry name.
     */
    public boolean exists(String name) throws IOException {
        return getJarEntry(name)!=null;
    }

    /**
     * Returns true if the entry is a directory or a plain file
     * @param name name is one of the entries returned by {@link #entries()}
     * @return true if the entry denoted by the passed name is a directory
     */
    public boolean isDirectory(String name) {
        JarEntry entry = getJarEntry(name);
        if (entry==null) {
            throw new IllegalArgumentException(name);
        }
        return entry.isDirectory();
    }

    static String getName(URI uri) {
        String path = Util.getURIName(uri);
        int lastDot = path.lastIndexOf('.');
        int endOfName = (lastDot != -1) ? lastDot : path.length();
        String name = path.substring(0, endOfName);
        return name;
    }

    /**
     * set the parent archive for this archive
     *
     * @param parentArchive the parent archive
     */
    public void setParentArchive(ReadableArchive parentArchive) {
        this.parentArchive = parentArchive;
    }

    /**
     * get the parent archive of this archive
     *
     * @return the parent archive
     */
    public ReadableArchive getParentArchive() {
        return parentArchive;
    }

    /**
     * Returns any data that could have been calculated as part of
     * the descriptor loading.
     *
     * @param dataType the type of the extra data
     * @return the extra data or null if there are not an instance of
     * type dataType registered.
     */
    public synchronized <U> U getExtraData(Class<U> dataType) {
        return dataType.cast(extraData.get(dataType));
    }

    public synchronized <U> void setExtraData(Class<U> dataType, U instance) {
        extraData.put(dataType, instance);
    }

    public synchronized <U> void removeExtraData(Class<U> dataType) {
        extraData.remove(dataType);
    }

    public void addArchiveMetaData(String metaDataKey, Object metaData) {
        if (metaData!=null) {
            archiveMetaData.put(metaDataKey, metaData);
        }
    }

    public <T> T getArchiveMetaData(String metaDataKey, Class<T> metadataType) {
        Object metaData = archiveMetaData.get(metaDataKey);
        if (metaData != null) {
            return metadataType.cast(metaData);
        }
        return null;
    }

    public void removeArchiveMetaData(String metaDataKey) {
        archiveMetaData.remove(metaDataKey);
    }
}
