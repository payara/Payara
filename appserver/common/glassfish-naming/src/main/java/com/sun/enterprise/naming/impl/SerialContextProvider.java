/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2006-2010 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.enterprise.naming.impl;

import java.util.*;
import javax.naming.*;
import java.rmi.*;

public interface SerialContextProvider extends Remote {

    /**
     * Lookup the specified name.
     *
     * @return the object or context bound to the name.
     * @throws NamingException if there is a naming exception.
     * @throws if              there is an RMI exception.
     */
    public Object lookup(String name)
            throws NamingException, RemoteException;

    /**
     * Bind the object to the specified name.
     *
     * @throws NamingException if there is a naming exception.
     * @throws if              there is an RMI exception.
     */
    public void bind(String name, Object obj)
            throws NamingException, RemoteException;

    /**
     * Rebind the object to the specified name.
     *
     * @throws NamingException if there is a naming exception.
     * @throws if              there is an RMI exception.
     */
    public void rebind(String name, Object obj)
            throws NamingException, RemoteException;

    /**
     * Unbind the specified object.
     *
     * @throws NamingException if there is a naming exception.
     * @throws if              there is an RMI exception.
     */
    public void unbind(String name)
            throws NamingException, RemoteException;

    /**
     * Rename the bound object.
     *
     * @throws NamingException if there is a naming exception.
     * @throws if              there is an RMI exception.
     */
    public void rename(String oldname, String newname)
            throws NamingException, RemoteException;

    public Hashtable list() throws RemoteException;

    /**
     * List the contents of the specified context.
     *
     * @throws NamingException if there is a naming exception.
     * @throws if              there is an RMI exception.
     */
    public Hashtable list(String name) throws NamingException, RemoteException;

    /**
     * Create a subcontext with the specified name.
     *
     * @return the created subcontext.
     * @throws NamingException if there is a naming exception.
     * @throws if              there is an RMI exception.
     */
    public Context createSubcontext(String name)
            throws NamingException, RemoteException;

    /**
     * Destroy the subcontext with the specified name.
     *
     * @throws NamingException if there is a naming exception.
     * @throws if              there is an RMI exception.
     */
    public void destroySubcontext(String name)
            throws NamingException, RemoteException;
}


