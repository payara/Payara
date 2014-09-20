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

package com.sun.enterprise.naming.impl;

import javax.naming.Context;
import javax.naming.NamingException;
import java.rmi.RemoteException;
import java.util.Hashtable;

public class SerialContextProviderImpl implements SerialContextProvider {
    private TransientContext rootContext;

    protected SerialContextProviderImpl(TransientContext rootContext)
            throws RemoteException {
        this.rootContext = rootContext;
    }

    /**
     * Lookup the specified name.
     *
     * @return the object orK context bound to the name.
     * @throws NamingException if there is a naming exception.
     * @throws if              there is an RMI exception.
     */

    public Object lookup(String name) throws NamingException, RemoteException {
        try {
            return rootContext.lookup(name);
        } catch (NamingException ne) {
            throw ne;
        } catch (Exception e) {
            RemoteException re = new RemoteException("", e);
            throw re;
        }
    }

    /**
     * Bind the object to the specified name.
     *
     * @throws NamingException if there is a naming exception.
     * @throws if              there is an RMI exception.
     */

    public void bind(String name, Object obj)
            throws NamingException, RemoteException {

        rootContext.bind(name, obj);
    }

    /**
     * Rebind the object to the specified name.
     *
     * @throws NamingException if there is a naming exception.
     * @throws if              there is an RMI exception.
     */

    public void rebind(String name, Object obj)
            throws NamingException, RemoteException {

        rootContext.rebind(name, obj);
    }

    /**
     * Unbind the specified object.
     *
     * @throws NamingException if there is a naming exception.
     * @throws if              there is an RMI exception.
     */

    public void unbind(String name)
            throws NamingException, RemoteException {

        rootContext.unbind(name);
    }

    /**
     * Rename the bound object.
     *
     * @throws NamingException if there is a naming exception.
     * @throws if              there is an RMI exception.
     */

    public void rename(String oldname, String newname)
            throws NamingException, RemoteException {

        rootContext.rename(oldname, newname);
    }

    public Hashtable list() throws RemoteException {

        return rootContext.list();
    }

    /**
     * List the contents of the specified context.
     *
     * @throws NamingException if there is a naming exception.
     * @throws if              there is an RMI exception.
     */

    public Hashtable list(String name) throws NamingException, RemoteException {
        Hashtable ne = rootContext.listContext(name);
        return ne;
    }

    /**
     * Create a subcontext with the specified name.
     *
     * @return the created subcontext.
     * @throws NamingException if there is a naming exception.
     * @throws if              there is an RMI exception.
     */

    public Context createSubcontext(String name)
            throws NamingException, RemoteException {

        Context ctx = rootContext.createSubcontext(name);
        return ctx;
    }

    /**
     * Destroy the subcontext with the specified name.
     *
     * @throws NamingException if there is a naming exception.
     * @throws if              there is an RMI exception.
     */

    public void destroySubcontext(String name)
            throws NamingException, RemoteException {

        rootContext.destroySubcontext(name);
    }

}






