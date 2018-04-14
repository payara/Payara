/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2016 Oracle and/or its affiliates. All rights reserved.
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
// Portions Copyright [2017] [Payara Foundation and/or its affiliates]

package org.apache.naming.resources;

import java.text.MessageFormat;
import java.util.*;
import java.util.logging.Logger;
import java.util.logging.Level;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.naming.Binding;
import javax.naming.NameClassPair;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.NameNotFoundException;
import javax.naming.OperationNotSupportedException;
import javax.naming.NameAlreadyBoundException;
import javax.naming.directory.DirContext;
import javax.naming.directory.Attributes;
import javax.naming.directory.ModificationItem;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.naming.LogFacade;
import org.apache.naming.NamingEntry;
import org.apache.naming.NamingContextBindingsEnumeration;
import org.apache.naming.NamingContextEnumeration;
import org.apache.naming.Util;

/**
 * Filesystem Directory Context implementation helper class.
 *
 * @author Remy Maucherat
 * @version $Revision: 1.7 $ $Date: 2007/05/05 05:33:00 $
 */

public class FileDirContext extends BaseDirContext {
    private static final Logger logger = LogFacade.getLogger();

    protected static final ResourceBundle rb = logger.getResourceBundle();


    // -------------------------------------------------------------- Constants


    /**
     * The descriptive information string for this implementation.
     */
    protected static final int BUFFER_SIZE = 2048;


    // ----------------------------------------------------------- Constructors


    /**
     * Builds a file directory context using the given environment.
     */
    public FileDirContext() {
        super();
    }


    /**
     * Builds a file directory context using the given environment.
     */
    public FileDirContext(Hashtable<String, Object> env) {
        super(env);
    }


    // ----------------------------------------------------- Instance Variables


    /**
     * The document base directory.
     */
    protected File base = null;

    // START S1AS8PE 4965170
    /**
     * File cache.
     */
    // map x --> File(x)
    protected Map<String, File> docBaseFileCache =
        Collections.synchronizedMap(new WeakHashMap<String, File>());

    // map x --> File(base, x) 
    protected Map<String, File> fileCache =
        Collections.synchronizedMap(new WeakHashMap<String, File>());

    // map file.getPath() + '/' + x --> File(file, x)
    protected Map<String, File> listFileCache =
        Collections.synchronizedMap(new WeakHashMap<String, File>());
    // END S1AS8PE 4965170

    /**
     * Absolute normalized filename of the base.
     */
    protected String absoluteBase = null;


    /**
     * Case sensitivity.
     */
    protected boolean caseSensitive = true;


    /**
     * Allow linking.
     */
    protected boolean allowLinking = false;


    // ------------------------------------------------------------- Properties


    /**
     * Set the document root.
     * 
     * @param docBase The new document root
     * 
     * @exception IllegalArgumentException if the specified value is not
     *  supported by this implementation
     * @exception IllegalArgumentException if this would create a
     *  malformed URL
     */
    public void setDocBase(String docBase) {

        // Validate the format of the proposed document root
        if (docBase == null)
            throw new IllegalArgumentException
            (rb.getString(LogFacade.RESOURCES_NULL));

        // START S1AS8PE 4965170
        base = docBaseFileCache.get(docBase);
        if (base == null){
            // Calculate a File object referencing this document base directory
            base = new File(docBase);
            docBaseFileCache.put(docBase,base);
        }
        // END S1AS8PE 4965170


        try {
            base = base.getCanonicalFile();
        } catch (IOException e) {
            // Ignore
        }

        // Validate that the document base is an existing directory
        if (!base.exists() || !base.isDirectory() || !base.canRead())
            throw new IllegalArgumentException
                    (MessageFormat.format(rb.getString(LogFacade.FILE_RESOURCES_BASE), docBase));
        this.absoluteBase = base.getAbsolutePath();
        super.setDocBase(docBase);

    }


    /**
     * Set case sensitivity.
     */
    public void setCaseSensitive(boolean caseSensitive) {
        this.caseSensitive = caseSensitive;
    }


    /**
     * Is case sensitive ?
     */
    public boolean isCaseSensitive() {
        return caseSensitive;
    }


    /**
     * Set allow linking.
     */
    public void setAllowLinking(boolean allowLinking) {
        this.allowLinking = allowLinking;
    }


    /**
     * Is linking allowed.
     */
    public boolean getAllowLinking() {
        return allowLinking;
    }


    // --------------------------------------------------------- Public Methods


    /**
     * Release any resources allocated for this directory context.
     */
    public void release() {

        caseSensitive = true;
        allowLinking = false;
        absoluteBase = null;
        base = null;
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
    public Object lookup(String name)
        throws NamingException {
        Object result = null;
        File file = file(name, true);

        if (file == null)
            throw new NamingException
                    (MessageFormat.format(rb.getString(LogFacade.RESOURCES_NOT_FOUND), name));
        if (file.isDirectory()) {
            FileDirContext tempContext = new FileDirContext(env);
            tempContext.setDocBase(file.getPath());
            tempContext.setAllowLinking(getAllowLinking());
            tempContext.setCaseSensitive(isCaseSensitive());
            result = tempContext;
        } else {
            result = new FileResource(file);
        }

        return result;
        
    }


    /**
     * Unbinds the named object. Removes the terminal atomic name in name 
     * from the target context--that named by all but the terminal atomic 
     * part of name.
     * <p>
     * This method is idempotent. It succeeds even if the terminal atomic 
     * name is not bound in the target context, but throws 
     * NameNotFoundException if any of the intermediate contexts do not exist. 
     * 
     * @param name the name to bind; may not be empty
     * @exception NameNotFoundException if an intermediate context does not
     * exist
     * @exception NamingException if a naming exception is encountered
     */
    public void unbind(String name)
        throws NamingException {

        File file = file(name, true);

        if (file == null)
            throw new NameNotFoundException
                    (MessageFormat.format(rb.getString(LogFacade.RESOURCES_NOT_FOUND), name));

        // START S1AS8PE 4965170
        fileCache.remove(name);
        // END S1AS8PE 4965170
        if (!file.delete())
            throw new NamingException
                    (MessageFormat.format(rb.getString(LogFacade.RESOURCES_NOT_FOUND), name));

    }


    /**
     * Binds a new name to the object bound to an old name, and unbinds the 
     * old name. Both names are relative to this context. Any attributes 
     * associated with the old name become associated with the new name. 
     * Intermediate contexts of the old name are not changed.
     * 
     * @param oldName the name of the existing binding; may not be empty
     * @param newName the name of the new binding; may not be empty
     * @exception NameAlreadyBoundException if newName is already bound
     * @exception NamingException if a naming exception is encountered
     */
    public void rename(String oldName, String newName)
        throws NamingException {
        
        File file = file(oldName, true);

        if (file == null)
            throw new NamingException
                    (MessageFormat.format(rb.getString(LogFacade.RESOURCES_NOT_FOUND), oldName));

        // START S1AS8PE 4965170
        File newFile = fileCache.get(newName);
        if (newFile == null) {
            newFile = new File(base, newName);
        }
        // END S1AS8PE 4965170
        
        if (!file.renameTo(newFile)) {
            throw new NamingException(
                    MessageFormat.format(rb.getString(LogFacade.RESOURCES_RENAME_FAIL), oldName, newName));
        }
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
    public NamingEnumeration<NameClassPair> list(String name)
        throws NamingException {
        
        File file = file(name, true);

        if (file == null)
            throw new NamingException
                    (MessageFormat.format(rb.getString(LogFacade.RESOURCES_NOT_FOUND), name));

        return new NamingContextEnumeration(list(file).iterator());

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
    public NamingEnumeration<Binding> listBindings(String name)
        throws NamingException {

        File file = file(name, true);

        if (file == null)
            throw new NamingException
                    (MessageFormat.format(rb.getString(LogFacade.RESOURCES_NOT_FOUND), name));

        return new NamingContextBindingsEnumeration(list(file).iterator(),
                this);

    }


    /**
     * Destroys the named context and removes it from the namespace. Any 
     * attributes associated with the name are also removed. Intermediate 
     * contexts are not destroyed.
     * <p>
     * This method is idempotent. It succeeds even if the terminal atomic 
     * name is not bound in the target context, but throws 
     * NameNotFoundException if any of the intermediate contexts do not exist.
     * 
     * In a federated naming system, a context from one naming system may be 
     * bound to a name in another. One can subsequently look up and perform 
     * operations on the foreign context using a composite name. However, an 
     * attempt destroy the context using this composite name will fail with 
     * NotContextException, because the foreign context is not a "subcontext" 
     * of the context in which it is bound. Instead, use unbind() to remove 
     * the binding of the foreign context. Destroying the foreign context 
     * requires that the destroySubcontext() be performed on a context from 
     * the foreign context's "native" naming system.
     * 
     * @param name the name of the context to be destroyed; may not be empty
     * @exception NameNotFoundException if an intermediate context does not 
     * exist
     * @exception javax.naming.NotContextException if the name is bound but does
     * not name a context, or does not name a context of the appropriate type
     */
    public void destroySubcontext(String name)
        throws NamingException {
        unbind(name);
    }


    /**
     * Retrieves the named object, following links except for the terminal 
     * atomic component of the name. If the object bound to name is not a 
     * link, returns the object itself.
     * 
     * @param name the name of the object to look up
     * @return the object bound to name, not following the terminal link 
     * (if any).
     * @exception NamingException if a naming exception is encountered
     */
    public Object lookupLink(String name)
        throws NamingException {
        // Note : Links are not supported
        return lookup(name);
    }


    /**
     * Retrieves the full name of this context within its own namespace.
     * <p>
     * Many naming services have a notion of a "full name" for objects in 
     * their respective namespaces. For example, an LDAP entry has a 
     * distinguished name, and a DNS record has a fully qualified name. This 
     * method allows the client application to retrieve this name. The string 
     * returned by this method is not a JNDI composite name and should not be 
     * passed directly to context methods. In naming systems for which the 
     * notion of full name does not make sense, 
     * OperationNotSupportedException is thrown.
     * 
     * @return this context's name in its own namespace; never null
     * @exception OperationNotSupportedException if the naming system does 
     * not have the notion of a full name
     * @exception NamingException if a naming exception is encountered
     */
    public String getNameInNamespace()
        throws NamingException {
        return docBase;
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
    public Attributes getAttributes(String name, String[] attrIds)
        throws NamingException {

        // Building attribute list
        File file = file(name, true);

        if (file == null)
            throw new NamingException
                    (MessageFormat.format(rb.getString(LogFacade.RESOURCES_NOT_FOUND), name));

        return new FileResourceAttributes(file);

    }


    /**
     * Modifies the attributes associated with a named object. The order of 
     * the modifications is not specified. Where possible, the modifications 
     * are performed atomically.
     * 
     * @param name the name of the object whose attributes will be updated
     * @param mod_op the modification operation, one of: ADD_ATTRIBUTE, 
     * REPLACE_ATTRIBUTE, REMOVE_ATTRIBUTE
     * @param attrs the attributes to be used for the modification; may not 
     * be null
     * @exception javax.naming.directory.AttributeModificationException if the
     * modification cannot be completed successfully
     * @exception NamingException if a naming exception is encountered
     */
    public void modifyAttributes(String name, int mod_op, Attributes attrs)
        throws NamingException {
        
    }


    /**
     * Modifies the attributes associated with a named object using an an 
     * ordered list of modifications. The modifications are performed in the 
     * order specified. Each modification specifies a modification operation 
     * code and an attribute on which to operate. Where possible, the 
     * modifications are performed atomically.
     * 
     * @param name the name of the object whose attributes will be updated
     * @param mods an ordered sequence of modifications to be performed; may 
     * not be null
     * @exception javax.naming.directory.AttributeModificationException if the
     * modification cannot be completed successfully
     * @exception NamingException if a naming exception is encountered
     */
    public void modifyAttributes(String name, ModificationItem[] mods)
        throws NamingException {
        
    }


    /**
     * Binds a name to an object, along with associated attributes. If attrs 
     * is null, the resulting binding will have the attributes associated 
     * with obj if obj is a DirContext, and no attributes otherwise. If attrs 
     * is non-null, the resulting binding will have attrs as its attributes; 
     * any attributes associated with obj are ignored.
     * 
     * @param name the name to bind; may not be empty
     * @param obj the object to bind; possibly null
     * @param attrs the attributes to associate with the binding
     * @exception NameAlreadyBoundException if name is already bound
     * @exception javax.naming.directory.InvalidAttributesException if some
     * "mandatory" attributes of the binding are not supplied
     * @exception NamingException if a naming exception is encountered
     */
    public void bind(String name, Object obj, Attributes attrs)
        throws NamingException {
        
        // Note: No custom attributes allowed
         
        File file = new File(base, name);
        if (file.exists())
            throw new NameAlreadyBoundException
                    (MessageFormat.format(rb.getString(LogFacade.RESOURCES_ALREADY_BOUND), name));

        rebind(file, obj, attrs);
        
    }


    /**
     * Binds a name to an object, along with associated attributes, 
     * overwriting any existing binding. If attrs is null and obj is a 
     * DirContext, the attributes from obj are used. If attrs is null and obj 
     * is not a DirContext, any existing attributes associated with the object
     * already bound in the directory remain unchanged. If attrs is non-null, 
     * any existing attributes associated with the object already bound in 
     * the directory are removed and attrs is associated with the named 
     * object. If obj is a DirContext and attrs is non-null, the attributes 
     * of obj are ignored.
     * 
     * @param name the name to bind; may not be empty
     * @param obj the object to bind; possibly null
     * @param attrs the attributes to associate with the binding
     * @exception javax.naming.directory.InvalidAttributesException if some
     * "mandatory" attributes of the binding are not supplied
     * @exception NamingException if a naming exception is encountered
     */
    public void rebind(String name, Object obj, Attributes attrs)
        throws NamingException {
        
        // Note: No custom attributes allowed
        // Check obj type
        
        File file = new File(base, name);
        rebind(file,obj,attrs);    
    }


    public void rebind(File file, Object obj, Attributes attrs)
        throws NamingException {
        InputStream is = null;
        String name = file.getName();
        if (obj instanceof Resource) {
            try {
                is = ((Resource) obj).streamContent();
            } catch (IOException e) {
            }
        } else if (obj instanceof InputStream) {
            is = (InputStream) obj;
        } else if (obj instanceof DirContext) {
            if (file.exists()) {
                if (!file.delete())
                    throw new NamingException
                            (MessageFormat.format(rb.getString(LogFacade.RESOURCES_BIND_FAILED), name));
            }
            if (!file.mkdir())
                throw new NamingException
                        (MessageFormat.format(rb.getString(LogFacade.RESOURCES_BIND_FAILED), name));
        }
        if (is == null)
            throw new NamingException
                    (MessageFormat.format(rb.getString(LogFacade.RESOURCES_BIND_FAILED), name));

        // Open os
        
        try {
            FileOutputStream os = null;
            byte buffer[] = new byte[BUFFER_SIZE];
            int len = -1;
            try {
                os = new FileOutputStream(file);
                while (true) {
                    len = is.read(buffer);
                    if (len == -1)
                        break;
                    os.write(buffer, 0, len);
                }
            } finally {
                if (os != null)
                    os.close();
                is.close();
            }
        } catch (IOException e) {
            throw new NamingException
                    (MessageFormat.format(rb.getString(LogFacade.RESOURCES_BIND_FAILED), e));
        }
        
    }


    /**
     * Creates and binds a new context, along with associated attributes. 
     * This method creates a new subcontext with the given name, binds it in 
     * the target context (that named by all but terminal atomic component of 
     * the name), and associates the supplied attributes with the newly 
     * created object. All intermediate and target contexts must already 
     * exist. If attrs is null, this method is equivalent to 
     * Context.createSubcontext().
     * 
     * @param name the name of the context to create; may not be empty
     * @param attrs the attributes to associate with the newly created context
     * @return the newly created context
     * @exception NameAlreadyBoundException if the name is already bound
     * @exception javax.naming.directory.InvalidAttributesException if attrs
     * does not contain all the mandatory attributes required for creation
     * @exception NamingException if a naming exception is encountered
     */
    public DirContext createSubcontext(String name, Attributes attrs)
        throws NamingException {
        
        File file = new File(base, name);
        if (file.exists())
            throw new NameAlreadyBoundException
                    (MessageFormat.format(rb.getString(LogFacade.RESOURCES_ALREADY_BOUND), name));
        if (!file.mkdir())
            throw new NamingException
                    (MessageFormat.format(rb.getString(LogFacade.RESOURCES_BIND_FAILED), name));
        return (DirContext) lookup(name);
        
    }


    /**
     * Retrieves the schema associated with the named object. The schema 
     * describes rules regarding the structure of the namespace and the 
     * attributes stored within it. The schema specifies what types of 
     * objects can be added to the directory and where they can be added; 
     * what mandatory and optional attributes an object can have. The range 
     * of support for schemas is directory-specific.
     * 
     * @param name the name of the object whose schema is to be retrieved
     * @return the schema associated with the context; never null
     * @exception OperationNotSupportedException if schema not supported
     * @exception NamingException if a naming exception is encountered
     */
    public DirContext getSchema(String name)
        throws NamingException {
        throw new OperationNotSupportedException();
    }


    /**
     * Retrieves a context containing the schema objects of the named 
     * object's class definitions.
     * 
     * @param name the name of the object whose object class definition is to 
     * be retrieved
     * @return the DirContext containing the named object's class 
     * definitions; never null
     * @exception OperationNotSupportedException if schema not supported
     * @exception NamingException if a naming exception is encountered
     */
    public DirContext getSchemaClassDefinition(String name)
        throws NamingException {
        throw new OperationNotSupportedException();
    }


    /**
     * Searches in a single context for objects that contain a specified set 
     * of attributes, and retrieves selected attributes. The search is 
     * performed using the default SearchControls settings.
     * 
     * @param name the name of the context to search
     * @param matchingAttributes the attributes to search for. If empty or 
     * null, all objects in the target context are returned.
     * @param attributesToReturn the attributes to return. null indicates 
     * that all attributes are to be returned; an empty array indicates that 
     * none are to be returned.
     * @return a non-null enumeration of SearchResult objects. Each 
     * SearchResult contains the attributes identified by attributesToReturn 
     * and the name of the corresponding object, named relative to the 
     * context named by name.
     * @exception NamingException if a naming exception is encountered
     */
    public NamingEnumeration<SearchResult> search(String name,
            Attributes matchingAttributes, String[] attributesToReturn)
        throws NamingException {
        return null;
    }


    /**
     * Searches in a single context for objects that contain a specified set 
     * of attributes. This method returns all the attributes of such objects. 
     * It is equivalent to supplying null as the atributesToReturn parameter 
     * to the method search(Name, Attributes, String[]).
     * 
     * @param name the name of the context to search
     * @param matchingAttributes the attributes to search for. If empty or 
     * null, all objects in the target context are returned.
     * @return a non-null enumeration of SearchResult objects. Each 
     * SearchResult contains the attributes identified by attributesToReturn 
     * and the name of the corresponding object, named relative to the 
     * context named by name.
     * @exception NamingException if a naming exception is encountered
     */
    public NamingEnumeration<SearchResult> search(String name,
           Attributes matchingAttributes) throws NamingException {
        return null;
    }


    /**
     * Searches in the named context or object for entries that satisfy the 
     * given search filter. Performs the search as specified by the search 
     * controls.
     * 
     * @param name the name of the context or object to search
     * @param filter the filter expression to use for the search; may not be 
     * null
     * @param cons the search controls that control the search. If null, 
     * the default search controls are used (equivalent to 
     * (new SearchControls())).
     * @return an enumeration of SearchResults of the objects that satisfy 
     * the filter; never null
     * @exception javax.naming.directory.InvalidSearchFilterException if the
     * search filter specified is not supported or understood by the underlying
     * directory
     * @exception javax.naming.directory.InvalidSearchControlsException if the
     * search controls contain invalid settings
     * @exception NamingException if a naming exception is encountered
     */
    public NamingEnumeration<SearchResult> search(String name, String filter, 
                                    SearchControls cons)
        throws NamingException {
        return null;
    }


    /**
     * Searches in the named context or object for entries that satisfy the 
     * given search filter. Performs the search as specified by the search 
     * controls.
     * 
     * @param name the name of the context or object to search
     * @param filterExpr the filter expression to use for the search. 
     * The expression may contain variables of the form "{i}" where i is a 
     * nonnegative integer. May not be null.
     * @param filterArgs the array of arguments to substitute for the 
     * variables in filterExpr. The value of filterArgs[i] will replace each 
     * occurrence of "{i}". If null, equivalent to an empty array.
     * @param cons the search controls that control the search. If null, the 
     * default search controls are used (equivalent to (new SearchControls())).
     * @return an enumeration of SearchResults of the objects that satisy the 
     * filter; never null
     * @exception ArrayIndexOutOfBoundsException if filterExpr contains {i} 
     * expressions where i is outside the bounds of the array filterArgs
     * @exception javax.naming.directory.InvalidSearchControlsException if cons
     * contains invalid settings
     * @exception javax.naming.directory.InvalidSearchFilterException if
     * filterExpr with filterArgs represents an invalid search filter
     * @exception NamingException if a naming exception is encountered
     */
    public NamingEnumeration<SearchResult> search(String name, String filterExpr, 
                                    Object[] filterArgs, SearchControls cons)
        throws NamingException {
        return null;
    }


    // ------------------------------------------------------ Protected Methods


    /**
     * Return a context-relative path, beginning with a "/", that represents
     * the canonical version of the specified path after ".." and "." elements
     * are resolved out.  If the specified path attempts to go outside the
     * boundaries of the current context (i.e. too many ".." path elements
     * are present), return <code>null</code> instead.
     *
     * @param path Path to be normalized
     */
    protected String normalize(String path) {
        return Util.normalize(path, File.separatorChar == '\\');
    }

    /**
     * Return a File object representing the specified normalized
     * context-relative path if it exists and is readable.  Otherwise,
     * return <code>null</code>.
     *
     * @param name Normalized context-relative path (with leading '/')
     * @param fileMustExist Must the required file exist (i.e. preexisting files vs. injected) CVE-2017-12615
     * @return the validated java.io.File
     */
    protected File file(String name, boolean fileMustExist) {
        return validate(base, name, name, fileCache, fileMustExist);
    }

    /*
     * Check that the file is valid for this context
     */
    private File validate(File baseFile, String name, String keyName, Map<String, File> fCache, boolean fileMustExist) {

        // START S1AS8PE 4965170
        File file = fCache.get(keyName);
        if (file == null){
            file = new File(baseFile, name);
        }
        // END S1AS8PE 4965170
        
        if (!fileMustExist || file.exists() && file.canRead()) {

            // START S1AS 6200277
            if (!caseSensitive && allowLinking) {
                fCache.put(keyName,file);
                return file;
            }
            // END S1AS 6200277

            // Check that this file belongs to our root path
            String canPath = null;
            try {
                canPath = file.toPath().toRealPath().toString();
            } catch (IOException e) {
            }
            if (canPath == null) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, LogFacade.FILE_RESOURCES_NULL_CANONICAL_PATH);
                }
                return null;
            }

            // Check to see if going outside of the web application root
            if ((!allowLinking) && (!canPath.startsWith(absoluteBase))) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, LogFacade.FILE_RESOURCES_NOT_ALLOWED, new Object[]{allowLinking,canPath,absoluteBase});
                }
                return null;
            }

            // Case sensitivity check
            // START S1AS 6200277
            // if (!allowLinking && caseSensitive) {
            // END S1AS 6200277
            // START S1AS 6200277
            if (caseSensitive) {
            // END S1AS 6200277
                String fileAbsPath = file.getAbsolutePath();
                if (fileAbsPath.endsWith("."))
                    fileAbsPath = fileAbsPath + "/";
                String absPath = normalize(fileAbsPath);
                canPath = normalize(canPath);
                if ((canPath == null) || (absPath == null)) {
                    if (logger.isLoggable(Level.FINE)) {
                        logger.log(Level.FINE, LogFacade.FILE_RESOURCES_NULL_ABS_PATH,
                                new Object[]{canPath,absPath});
                    }
                    return null;
                }
                if ((absoluteBase.length() < absPath.length()) 
                    && (absoluteBase.length() < canPath.length())) {
                    absPath = absPath.substring(absoluteBase.length() + 1);
                    if (absPath.equals(""))
                        absPath = "/";
                    canPath = canPath.substring(absoluteBase.length() + 1);
                    if (canPath.equals(""))
                        canPath = "/";
                    if (!canPath.equals(absPath)) {
                    // START S1AS 6200277
                    //  return null;
                    // END S1AS 6200277
                    // START S1AS 6200277
                        if (canPath.equalsIgnoreCase(absPath)
                                || !allowLinking) {
                            if (logger.isLoggable(Level.FINE)) {
                                logger.log(Level.FINE, LogFacade.FILE_RESOURCES_PATH_EQUALS_ABS_PATH,
                                    new Object[]{canPath,absPath,allowLinking});
                            }
                            return null;
                        }
                    // END S1AS 6200277
                    }
                }
            }

        } else {
            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, LogFacade.FILE_RESOURCES_NOT_EXIST,
                        file.getAbsolutePath());
            }
            return null;
        }
        // START S1AS8PE 4965170
        fCache.put(keyName,file);
        // END S1AS8PE 4965170
        return file;

    }


    /**
     * List the resources which are members of a collection.
     * 
     * @param file Collection
     * @return ArrayList containg NamingEntry objects
     */
    protected ArrayList<NamingEntry> list(File file) {

        ArrayList<NamingEntry> entries = new ArrayList<NamingEntry>();
        if (!file.isDirectory())
            return entries;
        String[] names = file.list();
        if (names==null) {
            /* Some IO error occurred such as bad file permissions,
             * lack of file descriptors.
             * Prevent a NPE with Arrays.sort(names) */
            logger.log(Level.WARNING, LogFacade.FILE_RESOURCES_LISTING_NULL,
                                  file.getAbsolutePath());
            return entries;
        }

        Arrays.sort(names);             // Sort alphabetically
        NamingEntry entry = null;

        for (int i = 0; i < names.length; i++) {

            // START S1AS8PE 4965170
            String keyName = file.getPath() + '/' + names[i];
            File currentFile = validate(file, names[i], keyName, listFileCache, true);
            // END S1AS8PE 4965170

            Object object = null;
            if (currentFile!=null && currentFile.isDirectory()) {
                FileDirContext tempContext = new FileDirContext(env);
                tempContext.setDocBase(file.getPath());
                tempContext.setAllowLinking(getAllowLinking());
                tempContext.setCaseSensitive(isCaseSensitive());
                object = tempContext;
            } else {
                object = new FileResource(currentFile);
            }
            entry = new NamingEntry(names[i], object, NamingEntry.ENTRY);
            entries.add(entry);

        }

        return entries;

    }


    // ----------------------------------------------- FileResource Inner Class


    /**
     * This specialized resource implementation avoids opening the InputStream
     * to the file right away (which would put a lock on the file).
     */
    protected static class FileResource extends Resource {
        
        
        // -------------------------------------------------------- Constructor


        public FileResource(File file) {
            this.file = file;
        }
        
        
        // --------------------------------------------------- Member Variables
        
        
        /**
         * Associated file object.
         */
        protected File file;
        

        // --------------------------------------------------- Resource Methods
        
        
        /**
         * Content accessor.
         * 
         * @return InputStream
         */
        public InputStream streamContent()
            throws IOException {
            if (binaryContent == null) {
                FileInputStream fin = new FileInputStream(file);
                inputStream = fin;
                return fin;
            }
            return super.streamContent();
        }
        
        
    }


    // ------------------------------------- FileResourceAttributes Inner Class


    /**
     * This specialized resource attribute implementation does some lazy 
     * reading (to speed up simple checks, like checking the last modified 
     * date).
     */
    protected static class FileResourceAttributes extends ResourceAttributes {


        // -------------------------------------------------------- Constructor


        public FileResourceAttributes(File file) {
            this.file = file;
            getCreation();
            getLastModified();
        }
        
        // --------------------------------------------------- Member Variables
        
        
        protected File file;
        
        
        protected boolean accessed = false;


        protected String canonicalPath = null;
        
        
        // ----------------------------------------- ResourceAttributes Methods
        
        
        /**
         * Is collection.
         */
        public boolean isCollection() {
            if (!accessed) {
                collection = file.isDirectory();
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
            contentLength = file.length();
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
                creation = file.lastModified();
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
            lastModified = file.lastModified();
            return lastModified;
        }
        
        
        /**
         * Get lastModified date.
         * 
         * @return LastModified date value
         */
        public Date getLastModifiedDate() {
            if (lastModified == -1L) {
                lastModified = file.lastModified();
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
                name = file.getName();
            return name;
        }
        
        
        /**
         * Get resource type.
         * 
         * @return String resource type
         */
        public String getResourceType() {
            if (!accessed) {
                collection = file.isDirectory();
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
            if (canonicalPath == null) {
                try {
                    canonicalPath = file.getCanonicalPath();
                } catch (IOException e) {
                    // Ignore
                }
            }
            return canonicalPath;
        }


    }


}

