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

package com.sun.ejb.base.io;

import java.io.*;
import java.security.*;

import com.sun.ejb.spi.io.J2EEObjectStreamFactory;

/**
 * A Factory class for creating EJBObject input/output Stream
 *
 * @author Mahesh Kannan
 */
public class J2EEObjectStreamFactoryImpl
    implements J2EEObjectStreamFactory
{

    public J2EEObjectStreamFactoryImpl() {
    }

    /**
     *
     * Obtain an ObjectOutputStream that allows serialization
     *  of a graph of objects. The objects can be plain Serializable objects
     *  or can be converted into Serializable objects using
     *  the handler
     *
     *@throws IOException when the serialziation fails
     *@return an ObjectOutputStream that can be used to serialize objects
     */
    public ObjectOutputStream createObjectOutputStream(
            final OutputStream os,
            final boolean replaceObject)
        throws IOException
    {
        // Need privileged block here because EJBObjectOutputStream
        // does enableReplaceObject
        ObjectOutputStream oos = null;
        if(System.getSecurityManager() == null) {
            oos = new EJBObjectOutputStream(os, replaceObject);
        } else {
            try {
                oos = (ObjectOutputStream)AccessController.doPrivileged(
                        new PrivilegedExceptionAction() {
                    public java.lang.Object run()
                    throws Exception {
                        return new EJBObjectOutputStream(
                                os, replaceObject);
                    }
                });
            } catch ( PrivilegedActionException ex ) {
                throw (IOException) ex.getException();
            }
        }
        return oos; 
    }

    /**
     *
     * Obtain an ObjectInputStream that allows de-serialization
     *  of a graph of objects.
     *
     *@throws IOException when the de-serialziation fails
     *@return an ObjectInputStream that can be used to deserialize objects
     */
	public ObjectInputStream createObjectInputStream(
            final InputStream is,
            final boolean resolveObject,
            final ClassLoader appClassLoader)
        throws Exception
    {
        ObjectInputStream ois = null;
        if ( appClassLoader != null ) {
            // Need privileged block here because EJBObjectInputStream
            // does enableResolveObject
            if(System.getSecurityManager() == null) {
                ois = new EJBObjectInputStream(is, appClassLoader, resolveObject);
            } else {
                try {
                    ois = (ObjectInputStream)AccessController.doPrivileged(
                            new PrivilegedExceptionAction() {
                        public java.lang.Object run()
                        throws Exception {
                            return new EJBObjectInputStream(
                                    is, appClassLoader, resolveObject);
                        }
                    });
                } catch ( PrivilegedActionException ex ) {
                    throw (IOException) ex.getException();
                }
            }
        } else {
            ois = new ObjectInputStream(is);
        }

        return ois;
    }

}
