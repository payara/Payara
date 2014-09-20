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

/*
 * MemoryMappedArchive.java
 *
 * Created on September 6, 2002, 2:58 PM
 */

package com.sun.enterprise.deployment.deploy.shared;

import com.sun.enterprise.util.shared.ArchivistUtils;
import org.glassfish.api.deployment.archive.ReadableArchive;
import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.PerLookup;


import java.io.*;
import java.net.URI;
import java.util.Enumeration;
import java.util.Vector;
import java.util.Collection;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.jar.JarEntry;
import java.util.zip.ZipEntry;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author  Jerome Dochez
 */
@Service
@PerLookup
public class MemoryMappedArchive extends JarArchive implements ReadableArchive {

    private URI uri;

    byte[] file;
    
    /** Creates a new instance of MemoryMappedArchive */
    protected MemoryMappedArchive() {
	// for use by subclasses
    }

    /** Creates a new instance of MemoryMappedArchive */
    public MemoryMappedArchive(InputStream is) throws IOException {
        read(is);
    }

    public MemoryMappedArchive(byte[] bits) {
        file = bits;
    }

    public byte[] getByteArray() {
        return file;
    }
    
    private void read(InputStream is) throws IOException{
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ArchivistUtils.copy(is,baos);
        file = baos.toByteArray();
        
    }
    
    public void open(URI uri) throws IOException {
        File in = new File(uri);
        if (!in.exists()) {
            throw new FileNotFoundException(uri.getSchemeSpecificPart());
        }
        FileInputStream is = null;
        try {
            is = new FileInputStream(in);
            read(is);
        } finally {
            if (is != null) {
                is.close();
            }
        }
    }
    
    // copy constructor
    public MemoryMappedArchive(ReadableArchive source) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        JarOutputStream jos = new JarOutputStream(baos);
        for (Enumeration elements = source.entries();elements.hasMoreElements();) {
            String elementName = (String) elements.nextElement();
            InputStream is = source.getEntry(elementName);
            jos.putNextEntry(new ZipEntry(elementName));
            ArchivistUtils.copyWithoutClose(is, jos);            
            is.close();
            jos.flush();
            jos.closeEntry();
        }
        jos.close();
        file = baos.toByteArray();            
    }
    
    /**
     * close the abstract archive
     */
    public void close() throws IOException {
    }
        
    /**
     * delete the archive
     */
    public boolean delete() {
        return false;
    }
    
    /**
     * @return an @see java.util.Enumeration of entries in this abstract
     * archive
     */
    public Enumeration entries() {
        return entries(false).elements();
    }


    public Collection<String> getDirectories() throws IOException {
        return entries(true);
    }

    private Vector<String> entries(boolean directory) {

        Vector entries = new Vector();
        try {
            JarInputStream jis = new JarInputStream(new ByteArrayInputStream(file));
            ZipEntry ze;
            while ((ze=jis.getNextEntry())!=null) {
                if (ze.isDirectory()==directory) {
                    entries.add(ze.getName());
                }
            }
            jis.close();
        } catch(IOException ioe) {
            Logger.getAnonymousLogger().log(Level.WARNING, 
                ioe.getMessage(), ioe);  
        }
        return entries;        
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
    
    /**
     * @return true if this archive exists
     */
    public boolean exists() {
        return false;
    }
    
    /**
     * @return the archive uri
     */
    public String getPath() {
        return null;
    }
    
    /**
     * Get the size of the archive
     * @return tje the size of this archive or -1 on error
     */
    public long getArchiveSize() throws NullPointerException, SecurityException {
        return(file.length);
    }
    
    public URI getURI() {
        return uri;
    }

    public void setURI(final URI uri) {
        this.uri = uri;
    }
    
    /**
     * create or obtain an embedded archive within this abstraction.
     *
     * @param name the name of the embedded archive.
     */
    public ReadableArchive getSubArchive(String name) throws IOException {
        InputStream is = getEntry(name);
        if (is!=null) {
            ReadableArchive archive = new MemoryMappedArchive(is);
            is.close();
            return archive;
        }
        return null;
    }

    /**
     * Returns the existence of the given entry name
     * The file name must be relative to the root of the module.
     *
     * @param name the file name relative to the root of the module.
     * @return the existence the given entry name.
     */
    public boolean exists(String name) throws IOException {
        return (getEntry(name) != null);
    }
    
    /**
     * @return a @see java.io.InputStream for an existing entry in
     * the current abstract archive
     * @param name the entry name
     */
    public InputStream getEntry(String name) throws IOException {
        JarInputStream jis = new JarInputStream(new ByteArrayInputStream(file));
        ZipEntry ze;
        while ((ze=jis.getNextEntry())!=null) {
            if (ze.getName().equals(name)) 
                return new BufferedInputStream(jis);
        }
        return null;        
    }

    public JarEntry getJarEntry(String name) {
        try {
            JarInputStream jis = new JarInputStream(new ByteArrayInputStream(file));
            JarEntry ze;
            while ((ze=jis.getNextJarEntry())!=null) {
                if (ze.getName().equals(name)) {
                    return ze;
                }
            }
        } catch(IOException e) {
            return null;
        }
        return null;
    }

    /**
     * Returns the entry size for a given entry name or 0 if not known
     *
     * @param name the entry name
     * @return the entry size
     */
    public long getEntrySize(String name) {
        try {
            JarInputStream jis = new JarInputStream(new ByteArrayInputStream(file));
            ZipEntry ze;
            while ((ze=jis.getNextEntry())!=null) {
                if (ze.getName().equals(name)) {
                    return ze.getSize();
                }
            }
        } catch(IOException e) {
            return 0;
        }
        return 0;
    }
    
    /**
     * @return the manifest information for this abstract archive
     */
    public Manifest getManifest() throws IOException {
        JarInputStream jis = new JarInputStream(new ByteArrayInputStream(file));
        Manifest m = jis.getManifest();
        jis.close();
        return m;
    }
    
    /**
     * rename the archive
     *
     * @param name the archive name
     */
    public boolean renameTo(String name) {
        return false;
    }        
    
    /**
     * Returns the name for the archive.
     * <p>
     * For a MemoryMappedArhive there is no name, so an empty string is returned.
     * @return the name of the archive
     * 
     */
    @Override
    public String getName() {
        return "";
    }
}
