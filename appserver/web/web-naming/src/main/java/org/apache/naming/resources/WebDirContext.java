/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2017 Oracle and/or its affiliates. All rights reserved.
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
 *
 *
 * This file incorporates work covered by the following copyright and
 * permission notice:
 *
 * Copyright 2004 The Apache Software Foundation
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.naming.resources;

import java.io.File;
import java.io.InputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.text.MessageFormat;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

import javax.naming.Binding;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attributes;

import org.apache.naming.LogFacade;
import org.apache.naming.NamingEntry;
import org.apache.naming.NamingContextBindingsEnumeration;
import org.apache.naming.NamingContextEnumeration;

/**
 * Filesystem Directory Web Context implementation helper class.
 *
 * @author Shing Wai Chan
 */

public class WebDirContext extends FileDirContext {

    // -------------------------------------------------------------- Constants

    protected static final String META_INF_RESOURCES = "META-INF/resources/";


    // ----------------------------------------------------------- Constructors


    /**
     * Builds a file directory context using the given environment.
     */
    public WebDirContext() {
        super();
    }


    /**
     * Builds a file directory context using the given environment.
     */
    public WebDirContext(Hashtable<String, Object> env) {
        super(env);
    }


    // ----------------------------------------------------- Instance Variables

    protected JarFileResourcesProvider jarFileResourcesProvider = null;

    protected String jarResourceBase = META_INF_RESOURCES;


    // ------------------------------------------------------------- Properties

    public void setJarFileResourcesProvider(
            JarFileResourcesProvider jarFileResourcesProvider) {
        this.jarFileResourcesProvider = jarFileResourcesProvider;
    }

    void setJarResourceBase(String jarResourceBase) {
        this.jarResourceBase = jarResourceBase;
    }

    // --------------------------------------------------------- Public Methods


    /**
     * Release any resources allocated for this directory context.
     */
    @Override
    public void release() {

        jarFileResourcesProvider = null;
        jarResourceBase = null;
        super.release();

    }


    // -------------------------------------------------------- Context Methods


    /**
     * Retrieves the named object.
     * 
     * @param name the name of the object to look up
     * @return the object bound to name
     * @exception NamingException if a naming exception is encountered
     */
    @Override
    public Object lookup(String name)
        throws NamingException {
        Object result = null;
        File file = file(name);
        JarFileEntry jfEntry = null;
        
        if (file == null) {
            jfEntry = lookupFromJars(name);
            if (jfEntry == null) {
                throw new NamingException
                    (MessageFormat.format(rb.getString(LogFacade.RESOURCES_NOT_FOUND), name));
            }
        }

        if ((file != null && file.isDirectory()) ||
                (jfEntry != null && jfEntry.getJarEntry().isDirectory())) {
            WebDirContext tempContext = new WebDirContext(env);
            tempContext.docBase = name;
            tempContext.setAllowLinking(getAllowLinking());
            tempContext.setCaseSensitive(isCaseSensitive());
            tempContext.setJarFileResourcesProvider(jarFileResourcesProvider);
            tempContext.setJarResourceBase(getAbsoluteJarResourceName(name));
            result = tempContext;
        } else if (file != null) {
            result = new FileResource(file);
        } else if (jfEntry != null) {
            result = new JarResource(jfEntry.jarFile, jfEntry.jarEntry);
        }
        
        return result;
        
    }


    /**
     * Enumerates the names bound in the named context, along with the class 
     * names of objects bound to them. The contents of any subcontexts are 
     * not included.
     * <p>
     * If a binding is added to or removed from this context, its effect on 
     * an enumeration previously returned is undefined.
     * 
     * @param name the name of the context to list
     * @return an enumeration of the names and class names of the bindings in 
     * this context. Each element of the enumeration is of type NameClassPair.
     * @exception NamingException if a naming exception is encountered
     */
    @Override
    public NamingEnumeration<NameClassPair> list(String name)
        throws NamingException {

        List<NamingEntry> namingEntries = null;
        File file = file(name);
        if (file != null) {
            namingEntries = list(file);
        }
        List<JarFileEntry> jfeEntries = lookupAllFromJars(name);
        for (JarFileEntry jfeEntry : jfeEntries) {
            List<NamingEntry> jfList = list(jfeEntry);
            if (namingEntries != null) {
                namingEntries.addAll(jfList);
            } else {
                namingEntries = jfList;
            }
        }

        if (file == null && jfeEntries.size() == 0) {
            throw new NamingException
                    (MessageFormat.format(rb.getString(LogFacade.RESOURCES_NOT_FOUND), name));
        }
        return new NamingContextEnumeration(namingEntries.iterator());
    }


    /**
     * Enumerates the names bound in the named context, along with the 
     * objects bound to them. The contents of any subcontexts are not 
     * included.
     * <p>
     * If a binding is added to or removed from this context, its effect on 
     * an enumeration previously returned is undefined.
     * 
     * @param name the name of the context to list
     * @return an enumeration of the bindings in this context. 
     * Each element of the enumeration is of type Binding.
     * @exception NamingException if a naming exception is encountered
     */
    @Override
    public NamingEnumeration<Binding> listBindings(String name)
        throws NamingException {

        List<NamingEntry> namingEntries = null;
        File file = file(name);
        if (file != null) {
            namingEntries = list(file);
        }
        List<JarFileEntry> jfeEntries = lookupAllFromJars(name);
        for (JarFileEntry jfeEntry : jfeEntries) {
            List<NamingEntry> jfeList = list(jfeEntry);
            if (namingEntries != null) {
                namingEntries.addAll(jfeList);
            } else {
                namingEntries = jfeList;
            }
        }

        if (file == null && jfeEntries.size() == 0) {
            throw new NamingException
                    (MessageFormat.format(rb.getString(LogFacade.RESOURCES_NOT_FOUND), name));
        }

        return new NamingContextBindingsEnumeration(namingEntries.iterator(),
                this);

    }


    // ----------------------------------------------------- DirContext Methods


    /**
     * Retrieves selected attributes associated with a named object. 
     * See the class description regarding attribute models, attribute type 
     * names, and operational attributes.
     * 
     * @return the requested attributes; never null
     * @param name the name of the object from which to retrieve attributes
     * @param attrIds the identifiers of the attributes to retrieve. null 
     * indicates that all attributes should be retrieved; an empty array 
     * indicates that none should be retrieved
     * @exception NamingException if a naming exception is encountered
     */
    @Override
    public Attributes getAttributes(String name, String[] attrIds)
        throws NamingException {

        // Building attribute list
        File file = file(name);

        if (file == null) {
            JarFileEntry jfEntry = lookupFromJars(name);
            if (jfEntry == null) {
                throw new NamingException
                    (MessageFormat.format(rb.getString(LogFacade.RESOURCES_NOT_FOUND), name));
            } else {
                return new JarResourceAttributes(jfEntry.getJarEntry());
            }
        } else {
            return new FileResourceAttributes(file);
        }

    }


    // ------------------------------------------------------ Protected Methods

    protected JarFileEntry lookupFromJars(String name) {
        JarFileEntry result = null;
        JarFile[] jarFiles = null;
        if (jarFileResourcesProvider != null) {
            jarFiles = jarFileResourcesProvider.getJarFiles();
        }
        if (jarFiles != null) {
            String jeName = getAbsoluteJarResourceName(name);
            for (JarFile jarFile : jarFiles) {
                JarEntry jarEntry = null;
                if (jeName.charAt(jeName.length() - 1) != '/') {
                    jarEntry = jarFile.getJarEntry(jeName + '/');
                    if (jarEntry != null) {
                        result = new JarFileEntry(jarFile, jarEntry);
                        break;
                    }
                }
                jarEntry = jarFile.getJarEntry(jeName);
                if (jarEntry != null) {
                    result = new JarFileEntry(jarFile, jarEntry);
                    break;
                }
            }
        }

        return result;
    }

    protected List<JarFileEntry> lookupAllFromJars(String name) {
        List<JarFileEntry> results = new ArrayList<JarFileEntry>();
        JarFile[] jarFiles = null;
        if (jarFileResourcesProvider != null) {
            jarFiles = jarFileResourcesProvider.getJarFiles();
        }
        if (jarFiles != null) {
            String jeName = getAbsoluteJarResourceName(name);
            for (JarFile jarFile : jarFiles) {
                JarEntry jarEntry = null;
                if (jeName.charAt(jeName.length() - 1) != '/') {
                    jarEntry = jarFile.getJarEntry(jeName + '/');
                    if (jarEntry != null) {
                        results.add(new JarFileEntry(jarFile, jarEntry));
                    }
                }
                jarEntry = jarFile.getJarEntry(jeName);
                if (jarEntry != null) {
                    results.add(new JarFileEntry(jarFile, jarEntry));
                }
            }
        }

        return results;
    }

    protected List<NamingEntry> list(JarFileEntry jfeEntry) {
        List<NamingEntry> entries = new ArrayList<NamingEntry>();
        JarFile jarFile = jfeEntry.jarFile;
        JarEntry jarEntry = jfeEntry.jarEntry;
        if (!jarEntry.isDirectory()) {
            return entries;
        }

        String prefix = jarEntry.getName();
        int prefixLength = prefix.length();

        Enumeration<JarEntry> e = jarFile.entries();
        while (e.hasMoreElements()) {
            JarEntry je = e.nextElement();
            String name = je.getName();
            if (name.length() > prefixLength && name.startsWith(prefix)) {
                int endIndex = name.indexOf('/', prefixLength);
                if (endIndex != -1 && endIndex != name.length() - 1) {
                    // more levels
                    continue;
                }
                String subName = ((endIndex != -1)?
                        name.substring(prefixLength, endIndex) :
                        name.substring(prefixLength));

                Object object = null;
                if (je.isDirectory()) {
                    WebDirContext tempContext = new WebDirContext(env);
                    tempContext.docBase = name;
                    tempContext.setAllowLinking(getAllowLinking());
                    tempContext.setCaseSensitive(isCaseSensitive());
                    tempContext.setJarFileResourcesProvider(jarFileResourcesProvider);
                    tempContext.setJarResourceBase(name);
                    object = tempContext;
                } else {
                    object = new JarResource(jarFile, je);
                }

                entries.add(new NamingEntry(subName, object, NamingEntry.ENTRY));

            }
        }

        return entries;
    }

    protected String getAbsoluteJarResourceName(String name) {
        if (name.length() == 0) {
            return jarResourceBase;
        }
        boolean firstEndsWithSlash = (jarResourceBase.charAt(jarResourceBase.length() -1) == '/');
        // name has length > 0 here
        boolean secondStartWithSlash = (name.charAt(0) == '/');
        if (firstEndsWithSlash && secondStartWithSlash) {
            return jarResourceBase.substring(0, jarResourceBase.length() - 1) + name;
        } else if (!firstEndsWithSlash && !secondStartWithSlash) {
            return jarResourceBase + '/' + name;
        } else {
            return jarResourceBase + name;
        }
    }

    // ----------------------------------------------- FileResource Inner Class


    // ------------------------------------- JarFileEntry Inner Class
    protected static class JarFileEntry {
        private JarFile jarFile = null;
        private JarEntry jarEntry = null;

        protected JarFileEntry(JarFile jf, JarEntry je) {
            jarFile = jf;
            jarEntry = je;
        }

        JarFile getJarFile() {
            return jarFile;
        }

        JarEntry getJarEntry() {
            return jarEntry;
        }
    }


    // ------------------------------------- JarResource Inner Class
    
    /**
     * This specialized resource implementation avoids opening the InputStream
     * to the jar entry right away (which would put a lock on the jar file).
     */
    protected static class JarResource extends Resource {
        
        
        // -------------------------------------------------------- Constructor


        public JarResource(JarFile jarFile, JarEntry jarEntry) {
            this.jarFile = jarFile;
            this.jarEntry = jarEntry;
        }
        
        
        // --------------------------------------------------- Member Variables
        
        
        /**
         * Associated JarFile object.
         */
        protected JarFile jarFile;
        

        /**
         * Associated JarEntry object.
         */
        protected JarEntry jarEntry;
        

        // --------------------------------------------------- Resource Methods
        
        
        /**
         * Content accessor.
         * 
         * @return InputStream
         */
        public InputStream streamContent()
            throws IOException {
            if (binaryContent == null) {
                InputStream jin = jarFile.getInputStream(jarEntry);
                inputStream = jin;
                return jin;
            }
            return super.streamContent();
        }
        
        
    }

    // ------------------------------------- JarResourceAttributes Inner Class


    /**
     * This specialized resource attribute implementation does some lazy 
     * reading (to speed up simple checks, like checking the last modified 
     * date).
     */
    protected static class JarResourceAttributes extends ResourceAttributes {


        // -------------------------------------------------------- Constructor


        public JarResourceAttributes(JarEntry jarEntry) {
            this.jarEntry = jarEntry;
            getCreation();
            getLastModified();
        }
        
        // --------------------------------------------------- Member Variables
        
        
        protected transient JarEntry jarEntry;
        
        
        protected boolean accessed = false;
        
        
        // ----------------------------------------- ResourceAttributes Methods
        
        
        /**
         * Is collection.
         */
        public boolean isCollection() {
            if (!accessed) {
                collection = jarEntry.isDirectory();
                accessed = true;
            }
            return super.isCollection();
        }
        
        
        /**
         * Get content length.
         * 
         * @return content length value
         */
        public long getContentLength() {
            if (contentLength != -1L)
                return contentLength;
            contentLength = jarEntry.getSize();
            return contentLength;
        }
        
        
        /**
         * Get creation time.
         * 
         * @return creation time value
         */
        public long getCreation() {
            if (creation != -1L)
                return creation;
            creation = getLastModified();
            return creation;
        }
        
        
        /**
         * Get creation date.
         * 
         * @return Creation date value
         */
        public Date getCreationDate() {
            if (creation == -1L) {
                creation = jarEntry.getTime();
            }
            return super.getCreationDate();
        }
        
        
        /**
         * Get last modified time.
         * 
         * @return lastModified time value
         */
        public long getLastModified() {
            if (lastModified != -1L)
                return lastModified;
            lastModified = jarEntry.getTime();
            return lastModified;
        }
        
        
        /**
         * Get lastModified date.
         * 
         * @return LastModified date value
         */
        public Date getLastModifiedDate() {
            if (lastModified == -1L) {
                lastModified = jarEntry.getTime();
            }
            return super.getLastModifiedDate();
        }
        
        
        /**
         * Get name.
         * 
         * @return Name value
         */
        public String getName() {
            if (name == null)
                name = jarEntry.getName();
            return name;
        }
        
        
        /**
         * Get resource type.
         * 
         * @return String resource type
         */
        public String getResourceType() {
            if (!accessed) {
                collection = jarEntry.isDirectory();
                accessed = true;
            }
            return super.getResourceType();
        }
        
        
        /**
         * Get canonical path.
         *
         * @return String the file's canonical path
         */
        public String getCanonicalPath() {
            return null;
        }

        // ----------------------------------------- Serializables Methods

        private void readObject(ObjectInputStream ois) throws IOException {
            throw new UnsupportedOperationException();
        }

        private void writeObject(ObjectOutputStream oos) throws IOException {
            throw new UnsupportedOperationException();
        }
    }
}

