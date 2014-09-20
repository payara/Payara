/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.enterprise.iiop.impl;

import java.io.*;

import javax.ejb.*;
import javax.ejb.spi.HandleDelegate;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import javax.rmi.PortableRemoteObject;

import org.omg.CORBA.portable.Delegate;
import org.omg.CORBA.ORB;
import com.sun.corba.ee.spi.presentation.rmi.StubAdapter;

/**
 * An implementation of HandleDelegate for the IIOP Protocol.
 *
 */

public final class IIOPHandleDelegate
    implements HandleDelegate
{

    public static HandleDelegate getHandleDelegate() {
        HandleDelegate handleDelegate =
            (HandleDelegate) java.security.AccessController.doPrivileged(
                new java.security.PrivilegedAction() {
                    public Object run() {
                        try {
                            ClassLoader cl = new HandleDelegateClassLoader();
                            Class c = cl.loadClass(
                                "org.glassfish.enterprise.iiop.impl.IIOPHandleDelegate");
                            return c.newInstance();
                        } catch ( Exception ex ) {
                            throw new RuntimeException("Error creating HandleDelegate", ex);
                        }
                    }
                }
            );
        return handleDelegate;
    }
    
    
    public void writeEJBObject(javax.ejb.EJBObject ejbObject,
            java.io.ObjectOutputStream ostream)
        throws java.io.IOException
    {
        ostream.writeObject(ejbObject); // IIOP stubs are Serializable
    }
    
    public javax.ejb.EJBObject readEJBObject(java.io.ObjectInputStream istream)
        throws java.io.IOException, ClassNotFoundException
    {
        return (EJBObject)getStub(istream, EJBObject.class);
    }
    
    public void writeEJBHome(javax.ejb.EJBHome ejbHome,
            java.io.ObjectOutputStream ostream)
        throws java.io.IOException
    {
        ostream.writeObject(ejbHome); // IIOP stubs are Serializable
    }
    
    public javax.ejb.EJBHome readEJBHome(java.io.ObjectInputStream istream)
        throws java.io.IOException, ClassNotFoundException
    {
        return (EJBHome)getStub(istream, EJBHome.class);
    }
    
    private Object getStub(java.io.ObjectInputStream istream, Class stubClass)
        throws IOException, ClassNotFoundException
    {
        // deserialize obj
        Object obj = istream.readObject();

        if( StubAdapter.isStub(obj) ) {

            try {

                // Check if it is already connected to the ORB by getting
                // the delegate.  If BAD_OPERATION is not thrown, then the 
                // stub is connected.  This will happen if istream is an 
                // IIOP input stream.
                StubAdapter.getDelegate(obj);

            } catch(org.omg.CORBA.BAD_OPERATION bo) {

                // TODO Temporary way to get the ORB.  Will need to
                // replace with an approach that goes through the habitat
                ORB orb = null;
                try {

                    orb = (ORB) new InitialContext().lookup("java:comp/ORB");

                } catch(NamingException ne) {

                    throw new IOException("Error acquiring orb", ne);
                }

                // Stub is not connected. This can happen if istream is
		        // not an IIOP input stream (e.g. it's a File stream).
                StubAdapter.connect(obj, (com.sun.corba.ee.spi.orb.ORB) orb);
            }

        } else {
            throw new IOException("Unable to create stub for class " + 
                stubClass.getName() + 
                ", object deserialized is not a CORBA object, it's type is " +
                obj.getClass().getName());
        }

        // narrow it
        Object stub = PortableRemoteObject.narrow(obj, stubClass);
     
        return stub;
    }
    
}
