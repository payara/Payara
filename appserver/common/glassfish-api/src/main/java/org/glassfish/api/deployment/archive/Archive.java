/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2008-2010 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.api.deployment.archive;

import org.glassfish.api.deployment.DeploymentContext;

import java.io.IOException;
import java.io.File;
import java.util.Enumeration;
import java.util.Collection;
import java.util.jar.Manifest;
import java.net.URI;


/**
 * This interface is an abstraction for accessing a module archive. 
 *
 * @author Jerome Dochez
 */
public interface Archive {
    
    /**
     * closes this archive and releases all resources
     */
    public void close() throws IOException;
    
    /** 
     * Returns an enumeration of the module file entries.  All elements 
     * in the enumeration are of type String.  Each String represents a 
     * file name relative to the root of the module. 
     * 
     * @return an enumeration of the archive file entries. 
     */ 
    public Enumeration<String> entries(); 

    /** 
     * Returns an enumeration of the module file entries with the
     * specified prefix.  All elements in the enumeration are of 
     * type String.  Each String represents a file name relative 
     * to the root of the module. 
     * 
     * @param prefix the prefix of entries to be included
     * @return an enumeration of the archive file entries. 
     */ 
    public Enumeration<String> entries(String prefix);

    /**
     * Returns the enumeration of first level directories in this
     * archive
     * @return enumeration of directories under the root of this archive
     */
    public Collection<String> getDirectories() throws IOException;
    
    /**
     * Returns true if the entry is a directory or a plain file
     * @param name name is one of the entries returned by {@link #entries()}
     * @return true if the entry denoted by the passed name is a directory
     */
    public boolean isDirectory(String name);
    
    /**
     * Returns the manifest information for this archive
     * @return the manifest info
     */
    public Manifest getManifest() throws IOException;
    
    /**
     * Returns the path used to create or open the underlying archive
     *
     * <p>
     * TODO: abstraction breakage:
     * Several callers, most notably {@link DeploymentContext#getSourceDir()}
     * implementation, assumes that this URI is an URL, and in fact file URL.
     *
     * <p>
     * If this needs to be URL, use of {@link URI} is misleading. And furthermore,
     * if its needs to be a file URL, this should be {@link File}.
     *
     * @return the path for this archive. 
     */
    public URI getURI();
    
    /**
     * Returns the size of the archive.
     * @return long indicating the size of the archive
     */
    public long getArchiveSize() throws SecurityException;
    
    /**
     * Returns the name of the archive.
     * <p>
     * Implementations should not return null.
     * @return the name of the archive
     */
    public String getName();
}
