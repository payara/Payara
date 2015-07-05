/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2015 Oracle and/or its affiliates. All rights reserved.
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

package com.sun.ejb.portable;

import java.io.*;
import javax.ejb.*;
import javax.rmi.PortableRemoteObject;

/**
 * A portable, Serializable implementation of EJBMetaData.
 * This class can potentially be instantiated in another vendor's container
 * so it must not refer to any non-portable RI-specific classes.
 *
 */

public final class EJBMetaDataImpl implements javax.ejb.EJBMetaData, Serializable
{
    // for backward compatibility
    private static final long serialVersionUID = 5777657175353026918L;

    private Class keyClass;
    private Class homeClass;
    private Class remoteClass;
    private boolean isSessionBean;
    private boolean isStatelessSessionBean;
    private HomeHandle homeHandle;

    // Dont serialize the EJBHome ref directly, use the HomeHandle
    transient private EJBHome ejbHomeStub;


    // this constructor is only called by the EntityContainer
    public EJBMetaDataImpl(EJBHome ejbHomeStub, Class homeClass, 
		   Class remoteClass, Class keyClass)
    {
        this(ejbHomeStub, homeClass, remoteClass, keyClass, false, false);
    }

    // this constructor is only called by non-entity-bean containers
    public EJBMetaDataImpl(EJBHome ejbHomeStub, Class homeClass, 
		   Class remoteClass, 
		   boolean isSessionBean, boolean isStatelessSessionBean)
    {
        this(ejbHomeStub, homeClass, remoteClass, null, isSessionBean, isStatelessSessionBean);
    }

    // this constructor is only called in the RI's EJB container
    public EJBMetaDataImpl(EJBHome ejbHomeStub, Class homeClass, 
		   Class remoteClass, Class keyClass, 
		   boolean isSessionBean, boolean isStatelessSessionBean)
    {
	this.ejbHomeStub = ejbHomeStub;
	this.homeHandle = new HomeHandleImpl(ejbHomeStub);
	this.keyClass  = keyClass;
	this.homeClass  = homeClass;
	this.remoteClass  = remoteClass;
	this.isSessionBean = isSessionBean;
	this.isStatelessSessionBean = isStatelessSessionBean;
    }


    /**
     * 
     */
    public Class getHomeInterfaceClass() 
    {
	return homeClass;
    }

    /**
     * 
     */
    public Class getRemoteInterfaceClass() 
    {
	return remoteClass;
    }

    /**
     *
     */
    public EJBHome getEJBHome() 
    {
	return ejbHomeStub;
    }

    /**
     *
     */
    public Class getPrimaryKeyClass() 
    {
	if ( keyClass == null ) {
	    // for SessionBeans there is no primary key
	    throw new RuntimeException("SessionBeans do not have a primary key");
	}
	return keyClass;
    }

    /**
     * 
     */
    public boolean isSession() 
    {
	return isSessionBean;
    }


    public boolean isStatelessSession()
    {
	return isStatelessSessionBean;
    }


    private void readObject(ObjectInputStream in)
	throws IOException, ClassNotFoundException
    {
	isSessionBean = in.readBoolean();
	isStatelessSessionBean = in.readBoolean();

	// Use thread context classloader to load home/remote/primarykey classes
	// See EJB2.0 spec section 18.4.4
	ClassLoader loader = Thread.currentThread().getContextClassLoader();
	remoteClass = loader.loadClass(in.readUTF());
	homeClass = loader.loadClass(in.readUTF());
	if ( !isSessionBean )
	    keyClass = loader.loadClass(in.readUTF());

	homeHandle = (HomeHandle)in.readObject();
	ejbHomeStub = homeHandle.getEJBHome();
	// narrow the home so that the application doesnt have to do
	// a narrow after EJBMetaData.getEJBHome().
	ejbHomeStub = (EJBHome)PortableRemoteObject.narrow(ejbHomeStub, homeClass);
    }

    private void writeObject(ObjectOutputStream out)
	throws IOException
    {
	out.writeBoolean(isSessionBean);
	out.writeBoolean(isStatelessSessionBean);

	// Write the String names of the Class objects, 
	// since Class objects cant be serialized unless the classes
	// they represent are Serializable.
	out.writeUTF(remoteClass.getName());
	out.writeUTF(homeClass.getName());
	if ( !isSessionBean )
	    out.writeUTF(keyClass.getName());

	out.writeObject(homeHandle);
    }   
}
