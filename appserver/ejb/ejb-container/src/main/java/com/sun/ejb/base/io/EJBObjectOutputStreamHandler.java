/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2012 Oracle and/or its affiliates. All rights reserved.
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

import com.sun.ejb.containers.BaseContainer;
import com.sun.ejb.EJBUtils;
import com.sun.ejb.containers.EjbContainerUtilImpl;
import com.sun.ejb.containers.RemoteBusinessWrapperBase;
import com.sun.enterprise.container.common.spi.util.JavaEEIOUtils;
import com.sun.logging.LogDomains;


import com.sun.enterprise.util.Utility;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import java.io.IOException;
import java.io.Serializable;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.enterprise.iiop.api.ProtocolManager;
import org.glassfish.enterprise.iiop.api.GlassFishORBHelper;
import org.glassfish.internal.api.Globals;
import org.glassfish.api.naming.GlassfishNamingManager;

import com.sun.enterprise.container.common.spi.util.GlassFishOutputStreamHandler;
import com.sun.enterprise.container.common.spi.util.IndirectlySerializable;
import com.sun.enterprise.container.common.spi.util.SerializableObjectFactory;

/**
 * A class that is used to passivate SFSB conversational state
 *
 * @author Mahesh Kannan
 */
public class EJBObjectOutputStreamHandler
    implements GlassFishOutputStreamHandler
{
    static JavaEEIOUtils _javaEEIOUtils;

    protected static final Logger _ejbLogger =
            LogDomains.getLogger(EJBObjectOutputStreamHandler.class, LogDomains.EJB_LOGGER);

    static final int EJBID_OFFSET = 0;
    static final int INSTANCEKEYLEN_OFFSET = 8;
    static final int INSTANCEKEY_OFFSET = 12;

    private static final byte HOME_KEY = (byte)0xff;

    //Ugly,
    public static final void setJavaEEIOUtils(JavaEEIOUtils javaEEIOUtils) {
        _javaEEIOUtils = javaEEIOUtils;
    }

    /**
     * This code is needed to serialize non-Serializable objects that
     * can be part of a bean's state. See EJB2.0 section 7.4.1.
     */
    public Object replaceObject(Object obj)
            throws IOException {
        Object result = obj;

        // Until we've identified a remote object, we can't assume the orb is
        // available in the container.  If the orb is not present, this will be null.
        ProtocolManager protocolMgr = getProtocolManager();

        if (obj instanceof RemoteBusinessWrapperBase) {
            result = getRemoteBusinessObjectFactory
                    ((RemoteBusinessWrapperBase) obj);
        } else if ((protocolMgr != null) && protocolMgr.isStub(obj) && protocolMgr.isLocal(obj)) {
            org.omg.CORBA.Object target = (org.omg.CORBA.Object) obj;
            // If we're here, it's always for the 2.x RemoteHome view.
            // There is no remote business wrapper class.
            result = getSerializableEJBReference(target, protocolMgr, null);
        }

        return result;
    }


    /**
     * Do all ProtocolManager access lazily and only request orb if it has already been
     * initialized so that code doesn't make the assumption that an orb is available in
     * this runtime.
     */
    private ProtocolManager getProtocolManager() {
	GlassFishORBHelper orbHelper = Globals.getDefaultHabitat().getService(GlassFishORBHelper.class);
	return orbHelper.isORBInitialized() ? orbHelper.getProtocolManager() : null;
    }

    private Serializable getRemoteBusinessObjectFactory
        (RemoteBusinessWrapperBase remoteBusinessWrapper) 
        throws IOException {
        // Create a serializable object with the remote delegate and
        // the name of the client wrapper class.
        org.omg.CORBA.Object target = (org.omg.CORBA.Object) 
            remoteBusinessWrapper.getStub();
        return getSerializableEJBReference(target, 
					   getProtocolManager(),
                      remoteBusinessWrapper.getBusinessInterfaceName());
    }

    private Serializable getSerializableEJBReference(org.omg.CORBA.Object obj,
						     ProtocolManager protocolMgr,
                             String remoteBusinessInterface)
	throws IOException
    {
        Serializable result = (Serializable) obj;
        try {


            byte[] oid = protocolMgr.getObjectID(obj);


            if ((oid != null) && (oid.length > INSTANCEKEY_OFFSET)) {
                long containerId = Utility.bytesToLong(oid, EJBID_OFFSET);
                //To be really sure that is indeed a ref generated
                //  by our container we do the following checks
                int keyLength = Utility.bytesToInt(oid, INSTANCEKEYLEN_OFFSET);
                if (oid.length == keyLength + INSTANCEKEY_OFFSET) {
                    boolean isHomeReference =
                        ((keyLength == 1) && (oid[INSTANCEKEY_OFFSET] == HOME_KEY));
                    if (isHomeReference) {
                        result = new SerializableS1ASEJBHomeReference(containerId);
                    } else {
                        SerializableS1ASEJBObjectReference serRef =
                            new SerializableS1ASEJBObjectReference(containerId,
                            oid, keyLength, remoteBusinessInterface);
                        result = serRef;
                        /* TODO
                        if (serRef.isHAEnabled()) {
                            SimpleKeyGenerator gen = new SimpleKeyGenerator();
                            Object key = gen.byteArrayToKey(oid, INSTANCEKEY_OFFSET, 20);
                            long version = SFSBClientVersionManager.getClientVersion(
                                    containerId, key);
                            serRef.setSFSBClientVersion(key, version);
                        } */
                    }
                }
            }
	    } catch (Exception ex) {
	        _ejbLogger.log(Level.WARNING, "Exception while getting serializable object", ex);
	        IOException ioEx = new IOException("Exception during extraction of instance key");
	        ioEx.initCause(ex);
	        throw ioEx;
	    }
	    return result;
    }

}

final class SerializableJNDIContext
    implements SerializableObjectFactory
{
    private String name;
    
    SerializableJNDIContext(Context ctx)
        throws IOException
    {
        try {
            // Serialize state for a jndi context.  The spec only requires
            // support for serializing contexts pointing to java:comp/env
            // or one of its subcontexts.  We also support serializing the
            // references to the the default no-arg InitialContext, as well
            // as references to the the contexts java: and java:comp. All
            // other contexts will either not serialize correctly or will
            // throw an exception during deserialization.
            this.name = ctx.getNameInNamespace();
        } catch (NamingException ex) {
            IOException ioe = new IOException();
            ioe.initCause(ex);
            throw ioe;
        }
    }

    public Object createObject()
        throws IOException
    {
        try {
            if ((name == null) || (name.length() == 0)) {
                return new InitialContext();
            } else {
                return Globals.getDefaultHabitat().<GlassfishNamingManager>getService(GlassfishNamingManager.class).restoreJavaCompEnvContext(name);
            }
        } catch (NamingException namEx) {
            IOException ioe = new IOException();
            ioe.initCause(namEx);
            throw ioe;
	}
    }

}

abstract class AbstractSerializableS1ASEJBReference
    implements SerializableObjectFactory
{
    protected long containerId;
    protected String debugStr;	//used for loggin purpose only

    
    protected static Logger _ejbLogger =
       LogDomains.getLogger(AbstractSerializableS1ASEJBReference.class, LogDomains.EJB_LOGGER);

    AbstractSerializableS1ASEJBReference(long containerId) {
	this.containerId = containerId;
	BaseContainer container = EjbContainerUtilImpl.getInstance().getContainer(containerId);
    
	//container can be null if the app has been undeployed
	//  after this was serialized
	if (container == null) {
	    _ejbLogger.log(Level.WARNING, "ejb.base.io.EJBOutputStream.null_container: "
		+ containerId);
	    debugStr = "" + containerId;
	} else {
	    debugStr = container.toString();
	}
    }


    protected static java.rmi.Remote doRemoteRefClassLoaderConversion
        (java.rmi.Remote reference) throws IOException {

        Thread currentThread = Thread.currentThread();
        ClassLoader contextClassLoader =
            currentThread.getContextClassLoader();
        
        java.rmi.Remote returnReference = reference;

        if( reference.getClass().getClassLoader() !=
            contextClassLoader) {
            try {
                byte[] serializedRef = EJBObjectOutputStreamHandler._javaEEIOUtils.serializeObject
                    (reference, false);
                returnReference = (java.rmi.Remote)
                        EJBObjectOutputStreamHandler._javaEEIOUtils.deserializeObject(serializedRef, false,
                                             contextClassLoader);
                GlassFishORBHelper orbHelper = EjbContainerUtilImpl.getInstance().getORBHelper();
                ProtocolManager protocolMgr = orbHelper.getProtocolManager();

               protocolMgr.connectObject(returnReference); 

            } catch(IOException ioe) {
                throw ioe;
            } catch(Exception e) {
                IOException ioEx = new IOException(e.getMessage());
                ioEx.initCause(e);
                throw ioEx;
            }
        }

        return returnReference;
    }
}

final class SerializableS1ASEJBHomeReference
    extends AbstractSerializableS1ASEJBReference
{
    
    SerializableS1ASEJBHomeReference(long containerId) {
	super(containerId);
    }

    public Object createObject()
        throws IOException
    {
	    Object result = null;
	    BaseContainer container = EjbContainerUtilImpl.getInstance().getContainer(containerId);
	    //container can be null if the app has been undeployed
	    //  after this was serialized
	    if (container == null) {
	        _ejbLogger.log(Level.WARNING, "ejb.base.io.EJBOutputStream.null_container "
		    + debugStr);
	        result = null;
	    } else {
            // Note that we can assume it's a RemoteHome stub because an
            // application never sees a reference to the internal 
            // Home for the Remote Business view.
	        result = AbstractSerializableS1ASEJBReference.
                doRemoteRefClassLoaderConversion(container.getEJBHomeStub());
	    }

	    return result;
    }
}

final class SerializableS1ASEJBObjectReference
    extends AbstractSerializableS1ASEJBReference
{
    private byte[] instanceKey;
    private Object sfsbKey;
    private long sfsbClientVersion;
    private boolean haEnabled;

    // If 3.0 Remote business view, the name of the remote business
    // interface to which this stub corresponds.
    private String remoteBusinessInterface;

    SerializableS1ASEJBObjectReference(long containerId, byte[] objKey,
            int keySize, String remoteBusinessInterfaceName) {
        super(containerId);
        BaseContainer container = EjbContainerUtilImpl.getInstance().getContainer(containerId);
        if (container != null) {
            this.haEnabled = container.isHAEnabled();
        }
        remoteBusinessInterface = remoteBusinessInterfaceName;
        instanceKey = new byte[keySize];
        System.arraycopy(objKey, EJBObjectOutputStreamHandler.INSTANCEKEY_OFFSET,
                instanceKey, 0, keySize);
    }
    
    void setSFSBClientVersion(Object key, long val) {
        this.sfsbKey = key;
        this.sfsbClientVersion = val;
    }
    
    boolean isHAEnabled() {
        return haEnabled;
    }
    
    public Object createObject()
        throws IOException
    {
        Object result = null;
        BaseContainer container = EjbContainerUtilImpl.getInstance().getContainer(containerId);
        //container can be null if the app has been undeployed
        //  after this was serialized
        if (container == null) {
            _ejbLogger.log(Level.WARNING,
                               "ejb.base.io.EJBOutputStream.null_container: "
                               + debugStr);
            result = null;
        } else {
                try {
                    if( remoteBusinessInterface == null ) {
                        java.rmi.Remote reference = container.
                            createRemoteReferenceWithId(instanceKey, null);
                        result = AbstractSerializableS1ASEJBReference.
                            doRemoteRefClassLoaderConversion(reference);

                    } else {

                        String generatedRemoteIntfName = EJBUtils.
                            getGeneratedRemoteIntfName(remoteBusinessInterface);

                        java.rmi.Remote remoteRef = container.
                            createRemoteReferenceWithId(instanceKey,
                                                        generatedRemoteIntfName);

                        java.rmi.Remote newRemoteRef =
                            AbstractSerializableS1ASEJBReference.
                                doRemoteRefClassLoaderConversion(remoteRef);


                        Thread currentThread = Thread.currentThread();
                        ClassLoader contextClassLoader =
                            currentThread.getContextClassLoader();

                        result = EJBUtils.createRemoteBusinessObject
                            (contextClassLoader, remoteBusinessInterface,
                             newRemoteRef);

                    }
                    /*TODO
                    if (haEnabled) {
                        SFSBClientVersionManager.setClientVersion(
                                containerId, sfsbKey, sfsbClientVersion);
                    }*/
                } catch(Exception e) {
                    IOException ioex = new IOException("remote ref create error");
                    ioex.initCause(e);
                    throw ioex;
                }
        }

	    return result;
    }
}


