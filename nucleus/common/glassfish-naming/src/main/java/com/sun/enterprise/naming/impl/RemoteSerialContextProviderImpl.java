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

package com.sun.enterprise.naming.impl;


import java.rmi.*;
import javax.naming.*;


import org.omg.CORBA.ORB;
import org.glassfish.api.naming.NamingObjectProxy;

import java.util.logging.*;
import java.util.Hashtable;

import com.sun.enterprise.util.Utility;


/**
 * This class is the implementation of the Remote SerialContextProvider
 *
 * @author Sheetal Vartak
 */

public class RemoteSerialContextProviderImpl 
    extends SerialContextProviderImpl {

    static public final String SERIAL_CONTEXT_PROVIDER_NAME =
        "SerialContextProvider";

    private ORB orb;

    private RemoteSerialContextProviderImpl(ORB orb, TransientContext rootContext)
	    throws RemoteException {

	    super(rootContext);

        this.orb = orb;


    }


   /**
     * Create the remote object and publish it in the CosNaming name service.
     */
    static public Remote initSerialContextProvider(ORB orb, TransientContext rootContext)
	    throws RemoteException {
       
       return new RemoteSerialContextProviderImpl(orb, rootContext);

    }
        
    public Object lookup(String name) throws NamingException, RemoteException {

        Object obj = super.lookup(name);

        // If CORBA object, resolve here in server to prevent a
	    // another round-trip to CosNaming.

        ClassLoader originalClassLoader = null;

	    try {
	        if( obj instanceof Reference ) {
		        Reference ref = (Reference) obj;
                         
		        if( ref.getFactoryClassName().equals(GlassfishNamingManagerImpl.IIOPOBJECT_FACTORY) ) {

                    // Set CCL to this CL so it's guaranteed to be able to find IIOPObjectFactory
                    originalClassLoader = Utility.setContextClassLoader(getClass().getClassLoader());

                    Hashtable env = new Hashtable();
                    env.put("java.naming.corba.orb", orb);

                    obj = javax.naming.spi.NamingManager.getObjectInstance
                            (obj, new CompositeName(name), null, env);
                }

		    } else if (obj instanceof NamingObjectProxy) {

                NamingObjectProxy namingProxy = (NamingObjectProxy) obj;

                //this call will make sure that the actual object is initialized
                obj  = ((NamingObjectProxy) obj).create(new InitialContext());

		// If it's an InitialNamingProxy, ignore the result of the
		// create() call and re-lookup the name.
                if( namingProxy instanceof NamingObjectProxy.InitializationNamingObjectProxy ) {
                    return super.lookup(name);
                }
            }
	    } catch(Exception e) {
	        RemoteException re = new RemoteException("", e);
            throw re;
        }  finally {
            if( originalClassLoader != null ) {
                Utility.setContextClassLoader(originalClassLoader);
            }
        }

        return obj;
   }
}
