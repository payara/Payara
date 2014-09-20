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

import java.net.URL;
import java.net.URLConnection;
import java.net.URLStreamHandler;
import java.io.IOException;
import java.util.Hashtable;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;

/**
 * Stream handler to a JNDI directory context.
 * 
 * @author <a href="mailto:remm@apache.org">Remy Maucherat</a>
 * @version $Revision: 1.3 $
 */
public class DirContextURLStreamHandler 
    extends URLStreamHandler {
    
    
    // ----------------------------------------------------------- Constructors
    
    
    public DirContextURLStreamHandler() {
    }
    
    
    public DirContextURLStreamHandler(DirContext context) {
        this.context = context;
    }
    
    
    // -------------------------------------------------------------- Variables
    
    
    /**
     * Bindings class loader - directory context. Keyed by CL id.
     */
    private static Hashtable<ClassLoader, DirContext> clBindings =
        new Hashtable<ClassLoader, DirContext>();
    
    
    /**
     * Bindings thread - directory context. Keyed by thread id.
     */
    private static Hashtable<Thread, DirContext> threadBindings =
        new Hashtable<Thread, DirContext>();
    
    
    // ----------------------------------------------------- Instance Variables
    
    
    /**
     * Directory context.
     */
    protected DirContext context = null;
    
    
    // ------------------------------------------------------------- Properties
    
    
    // ----------------------------------------------- URLStreamHandler Methods
    
    
    /**
     * Opens a connection to the object referenced by the <code>URL</code> 
     * argument.
     */
    protected URLConnection openConnection(URL u) 
        throws IOException {
        DirContext currentContext = this.context;
        if (currentContext == null)
            currentContext = get();
        return new DirContextURLConnection(currentContext, u);
    }
    
    
    // --------------------------------------------------------- Public Methods
    
    
    /**
     * Set the java.protocol.handler.pkgs system property.
     */
    public static void setProtocolHandler() {
        String value = System.getProperty(Constants.PROTOCOL_HANDLER_VARIABLE);
        if (value == null) {
            value = Constants.Package;
            System.setProperty(Constants.PROTOCOL_HANDLER_VARIABLE, value);
        } else if (value.indexOf(Constants.Package) == -1) {
            value += "|" + Constants.Package;
            System.setProperty(Constants.PROTOCOL_HANDLER_VARIABLE, value);
        }
    }
    
    
    /**
     * Returns true if the thread or the context class loader of the current 
     * thread is bound.
     */
    public static boolean isBound() {
        return (clBindings.containsKey
                (Thread.currentThread().getContextClassLoader()))
            || (threadBindings.containsKey(Thread.currentThread()));
    }
    
    
    /**
     * Binds a directory context to a class loader.
     */
    public static void bind(DirContext dirContext) {
        ClassLoader currentCL = 
            Thread.currentThread().getContextClassLoader();
        if (currentCL != null)
            clBindings.put(currentCL, dirContext);
    }
    
    
    /**
     * Unbinds a directory context to a class loader.
     */
    public static void unbind() {
        ClassLoader currentCL = 
            Thread.currentThread().getContextClassLoader();
        if (currentCL != null)
            clBindings.remove(currentCL);
    }
    
    
    /**
     * Binds a directory context to a thread.
     */
    public static void bindThread(DirContext dirContext) {
        threadBindings.put(Thread.currentThread(), dirContext);
    }
    
    
    /**
     * Unbinds a directory context to a thread.
     */
    public static void unbindThread() {
        threadBindings.remove(Thread.currentThread());
    }
    
    
    /**
     * Get the bound context.
     */
    public static DirContext get() {

        DirContext result = null;

        Thread currentThread = Thread.currentThread();
        ClassLoader currentCL = currentThread.getContextClassLoader();

        // Checking CL binding
        result = clBindings.get(currentCL);
        if (result != null)
            return result;

        // Checking thread biding
        result = threadBindings.get(currentThread);

        // Checking parent CL binding
        currentCL = currentCL.getParent();
        while (currentCL != null) {
            result = clBindings.get(currentCL);
            if (result != null)
                return result;
            currentCL = currentCL.getParent();
        }

        if (result == null)
            throw new IllegalStateException("Illegal class loader binding");

        return result;

    }
    
    
    /**
     * Binds a directory context to a class loader.
     */
    public static void bind(ClassLoader cl, DirContext dirContext) {
        clBindings.put(cl, dirContext);
    }
    
    
    /**
     * Unbinds a directory context to a class loader.
     */
    public static void unbind(ClassLoader cl) {
        clBindings.remove(cl);
    }
    
    
    /**
     * Get the bound context.
     */
    public static DirContext get(ClassLoader cl) {
        return clBindings.get(cl);
    }
    
    
    /**
     * Get the bound context.
     */
    public static DirContext get(Thread thread) {
        return threadBindings.get(thread);
    }
    

    // START SJSAS 6318494
    /**
     * Converts a <code>URL</code> of a specific protocol to a
     * <code>String</code>.
     *
     * The impl of this method is almost identical to that of the
     * java.net.URLStreamHandler superclass, except that it omits the
     * URL's authority field from the URL's String representation.
     *
     * @param   u   the URL.
     * @return  a string representation of the <code>URL</code> argument.
     */
    protected String toExternalForm(URL u) {

        // pre-compute length of StringBuilder
        int len = u.getProtocol().length() + 1;
        if (u.getPath() != null) {
            len += u.getPath().length();
        }
        if (u.getQuery() != null) {
            len += 1 + u.getQuery().length();
        }
        if (u.getRef() != null) 
            len += 1 + u.getRef().length();

        StringBuilder result = new StringBuilder(len);
        result.append(u.getProtocol());
        result.append(":");
        if (u.getPath() != null) {
            result.append(u.getPath());
        }
        if (u.getQuery() != null) {
            result.append('?');
            result.append(u.getQuery());
        }
        if (u.getRef() != null) {
            result.append("#");
            result.append(u.getRef());
        }
        return result.toString();
    }
    // END SJSAS 6318494

}
