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

package org.glassfish.deployapi.config;

import java.io.*;
import java.util.Enumeration;
import java.util.jar.Manifest;
import java.util.Collection;
import javax.enterprise.deploy.model.DeployableObject;
import java.net.URI;

import org.glassfish.api.deployment.archive.ReadableArchive;
import com.sun.enterprise.deploy.shared.AbstractReadableArchive;
import com.sun.enterprise.util.LocalStringManagerImpl;

/**
 * This class act as an ReadableArchive implementation, delegating all possible
 * APIs to the JSR88 DeployObject object.
 *
 * @author  Jerome Dochez
 */
public class ConfigBeanArchive extends AbstractReadableArchive {
    
    private DeployableObject deployObject;
    
    private static LocalStringManagerImpl localStrings =
	  new LocalStringManagerImpl(ConfigBeanArchive.class);
    
    /** Creates a new instance of ConfigBeanArchive */
    public ConfigBeanArchive(DeployableObject deployObject) {
        this.deployObject = deployObject;
    }
    
    /**
     * close the abstract archive
     */
    public void close() throws IOException {
        // nothing to do here
    }
    
    /**
     * close a previously returned @see java.io.OutputStream returned
     * by an addEntry call
     *
     * @param the output stream to close
     */
    public void closeEntry(ReadableArchive os) throws IOException {
        throw new IOException(localStrings.getLocalString(
		    	    "enterprise.deployapi.config.configbeanarchive.notimplemented", 
		    	    "Operation not implemented"));
        
    }
    
    /**
     * close a previously returned @see java.io.OutputStream returned
     * by an addEntry call
     * @param the output stream to close
     */
    public void closeEntry(OutputStream os) throws IOException {
        throw new IOException(localStrings.getLocalString(
		    	    "enterprise.deployapi.config.configbeanarchive.notimplemented", 
		    	    "Operation not implemented"));
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
        return deployObject.entries();
    }

    public Enumeration<String> entries(String prefix) {
        return null;
    }

    public Collection<String> getDirectories() throws IOException {
        return null;
    }

    public boolean isDirectory(String name) {
        return false;
    }

    public String getName() {
        return "";
    }

    
    /**
     * @return true if this archive exists
     */
    public boolean exists() {
        return false;
    }    
    
    public boolean exists(String name) throws IOException {
        if (getEntry(name) == null) {
            return false;
        } else {
            return true;
        }
    }

    /**
     * @return the archive uri
     */
    public String getArchiveUri() {
        return null;
    }
    
    public long getEntrySize(String name) {
        return 0;
    }

    public void open(URI uri) throws IOException {
    }

    /**
     * @return the archive size
     */
    public long getArchiveSize() throws NullPointerException, SecurityException {
        return -1;
    }

    /**
     * create or obtain a subarchive within this abstraction.
     *
     * @param the name of the subarchive.
     */
    public ReadableArchive getSubArchive(String name) throws IOException {
        throw new IOException(localStrings.getLocalString(
		    	    "enterprise.deployapi.config.configbeanarchive.notimplemented", 
		    	    "Operation not implemented"));
        
    }
    
    /**
     * @return a @see java.io.InputStream for an existing entry in
     * the current abstract archive
     * @param the entry name
     */
    public InputStream getEntry(String name) throws IOException {
        return deployObject.getEntry(name);
    }
    
    /**
     * @return the manifest information for this abstract archive
     */
    public Manifest getManifest() throws IOException {
        return null;
    }
    
    /**
     * rename the archive
     *
     * @param name the archive name
     */
    public boolean renameTo(String name) {
        return false;
    }
    
    /** @return true if this archive abstraction supports overwriting of elements
     *
     */
    public boolean supportsElementsOverwriting() {
        return false;
    }
    
    public void closeEntry() throws IOException {
        throw new IOException(localStrings.getLocalString(
		    	    "enterprise.deployapi.config.configbeanarchive.notimplemented", 
		    	    "Operation not implemented"));
    }
    
    public java.net.URI getURI() {
        return null;        
    }
}
