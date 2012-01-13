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

import com.sun.ejb.spi.io.SerializableObjectFactory;

import com.sun.enterprise.naming.util.ObjectInputOutputStreamFactoryFactory;

import com.sun.enterprise.naming.util.ObjectInputOutputStreamFactory;

import com.sun.logging.LogDomains;

import java.io.*;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.rmi.Remote;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.enterprise.iiop.api.GlassFishORBHelper;
import org.glassfish.enterprise.iiop.api.ProtocolManager;
import org.glassfish.internal.api.Globals;

/**
 * A class that is used to restore SFSB conversational state
 *
 * @author Mahesh Kannan
 */
class EJBObjectInputStream extends ObjectInputStream
{
    private ClassLoader appLoader;

    private static final Logger _ejbLogger =
       LogDomains.getLogger(EJBObjectInputStream.class, LogDomains.EJB_LOGGER);

    private ObjectInputOutputStreamFactory inputStreamHelper;

    EJBObjectInputStream(InputStream in, ClassLoader appCl, boolean resolve)
        throws IOException, StreamCorruptedException
    {
        super(in);
        
        appLoader = appCl;
        
        if (resolve == true) {
            enableResolveObject(resolve);

        }

        inputStreamHelper = ObjectInputOutputStreamFactoryFactory.getFactory();
    }

    @Override
    protected Object resolveObject(Object obj)
        throws IOException
    {

	// Until we've identified a remote object, we can't assume the orb is
	// available in the container.  If the orb is not present, this will be null.
        ProtocolManager protocolMgr = getProtocolManager();

        try {
            if ( (protocolMgr != null) && protocolMgr.isStub(obj) ) {
                protocolMgr.connectObject((Remote)obj);
                return obj;
            } else if (obj instanceof SerializableObjectFactory) {
                return ((SerializableObjectFactory) obj).createObject();
            } else {
                return obj;
            }
        } catch (IOException ioEx ) {
            _ejbLogger.log(Level.SEVERE, "ejb.resolve_object_exception", ioEx);
            throw ioEx;
        } catch (Exception ex) {
            _ejbLogger.log(Level.SEVERE, "ejb.resolve_object_exception", ex);
            IOException ioe = new IOException();
            ioe.initCause(ex);
            throw ioe;
        }
    }

    /**
     * Do all ProtocolManager access lazily and only request orb if it has already been
     * initialized so that code doesn't make the assumption that an orb is available in
     * this runtime.
     */
    private ProtocolManager getProtocolManager() {
	GlassFishORBHelper orbHelper = Globals.getDefaultHabitat().byType(GlassFishORBHelper.class).get();
	return orbHelper.isORBInitialized() ? orbHelper.getProtocolManager() : null;
    }

    @Override
    protected Class resolveProxyClass(String[] interfaces)
        throws IOException, ClassNotFoundException
    {
        Class[] classObjs = new Class[interfaces.length];
        for (int i = 0; i < interfaces.length; i++) {
            Class cl = Class.forName(interfaces[i], false, appLoader);
            // If any non-public interfaces, delegate to JDK's
            // implementation of resolveProxyClass.
            if ((cl.getModifiers() & Modifier.PUBLIC) == 0) {
                return super.resolveProxyClass(interfaces);
            } else {
                classObjs[i] = cl;
            }
        }
        try {
            return Proxy.getProxyClass(appLoader, classObjs);
        } catch (IllegalArgumentException e) {
            throw new ClassNotFoundException(null, e);
        }
    }

    @Override
    protected Class<?> resolveClass(ObjectStreamClass desc)
                throws IOException, ClassNotFoundException
    {
        Class clazz = inputStreamHelper.resolveClass(this, desc);
        if( clazz == null ) {
            try {
                // First try app class loader
                clazz = appLoader.loadClass(desc.getName());
            }  catch (ClassNotFoundException e) {

                clazz = super.resolveClass(desc);               
            }

        }

        return clazz;
    }


}
