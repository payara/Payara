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

package org.glassfish.enterprise.iiop.impl;

import com.sun.corba.ee.spi.ior.IOR;
import com.sun.corba.ee.spi.ior.ObjectKey;
import com.sun.corba.ee.spi.ior.TaggedProfile;
import com.sun.corba.ee.spi.oa.rfm.ReferenceFactory;
import java.rmi.Remote;
import java.rmi.RemoteException;


import org.glassfish.enterprise.iiop.api.ProtocolManager;
import org.glassfish.enterprise.iiop.api.RemoteReferenceFactory;

import org.glassfish.enterprise.iiop.spi.EjbContainerFacade;
import org.glassfish.enterprise.iiop.spi.EjbService;

import com.sun.enterprise.deployment.EjbDescriptor;

import com.sun.enterprise.util.Utility;

import javax.ejb.NoSuchObjectLocalException;
import javax.ejb.TransactionRolledbackLocalException;
import javax.ejb.TransactionRequiredLocalException;

import org.omg.PortableServer.POA;
import org.omg.PortableServer.Servant;
import org.omg.CosNaming.NamingContext;
import org.omg.CosNaming.NamingContextHelper;
import org.omg.CosNaming.NameComponent;

import com.sun.corba.ee.spi.oa.rfm.ReferenceFactoryManager ;
import com.sun.corba.ee.spi.orb.ORB;
import com.sun.corba.ee.spi.presentation.rmi.PresentationManager;
import com.sun.corba.ee.spi.presentation.rmi.StubAdapter;

import com.sun.corba.ee.spi.misc.ORBConstants;
import com.sun.logging.LogDomains;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.inject.Provider;
import javax.rmi.CORBA.Tie;

import org.jvnet.hk2.annotations.Service;
import org.glassfish.hk2.api.ServiceLocator;
import javax.inject.Inject;
import org.omg.CORBA.CompletionStatus;
import org.omg.CORBA.INVALID_TRANSACTION;
import org.omg.CORBA.LocalObject;
import org.omg.CORBA.MARSHAL;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.OBJECT_NOT_EXIST;
import org.omg.CORBA.Policy;
import org.omg.CORBA.TRANSACTION_REQUIRED;
import org.omg.CORBA.TRANSACTION_ROLLEDBACK;
import org.omg.PortableServer.ForwardRequest;
import org.omg.PortableServer.ServantLocator;
import org.omg.PortableServer.ServantLocatorPackage.CookieHolder;

/**
 * This class implements the ProtocolManager interface for the
 * RMI/IIOP ORB with POA (Portable Object Adapter). 
 * Note that the POA is now accessed only through the 
 * ReferenceFactoryManager for EJB.
 * 
 * @author Vivek Nagar
 */

@Service
public final class POAProtocolMgr extends org.omg.CORBA.LocalObject 
			     implements ProtocolManager
{
    private static final Logger _logger =
        LogDomains.getLogger(POAProtocolMgr.class, LogDomains.CORBA_LOGGER);
    
    private static final int MAPEXCEPTION_CODE = 9998;

    private ORB orb;

    private ReferenceFactoryManager rfm = null ;

    private PresentationManager presentationMgr;

    @Inject
    private ServiceLocator services;

    public POAProtocolMgr() {}

    @Inject
    private Provider<EjbService> ejbServiceProvider;

    @Override
    public void initialize(org.omg.CORBA.ORB o) {
        this.orb = (ORB)o;

        this.presentationMgr = ORB.getPresentationManager();
    }


    // Called in all VMs, must be called only after InitialNaming is available
    @Override
    public void initializePOAs() throws Exception {
	    // NOTE:  The RootPOA manager used to be activated here.
            getRFM() ;
	    _logger.log(Level.FINE,
                "POAProtocolMgr.initializePOAs: RFM resolved and activated");
    }

    private static class RemoteNamingServantLocator extends LocalObject
        implements ServantLocator {

        private final ORB orb ;
        private final Servant servant ;

        public RemoteNamingServantLocator( ORB orb, Remote impl ) {
            this.orb = orb ;
            Tie tie = ORB.getPresentationManager().getTie() ;
            tie.setTarget( impl ) ;
            servant = Servant.class.cast( tie ) ;
        }

        @Override
	public synchronized Servant preinvoke( byte[] oid, POA adapter,
	    String operation, CookieHolder the_cookie ) throws ForwardRequest {
	    return servant ;
	}

        @Override
	public void postinvoke( byte[] oid, POA adapter,
	    String operation, Object the_cookie, Servant the_servant ) {
	}
    }

    private synchronized ReferenceFactoryManager getRFM() {
        if (rfm == null) {
            try {
                rfm = ReferenceFactoryManager.class.cast(
                    orb.resolve_initial_references( "ReferenceFactoryManager" )) ;
                    rfm.activate() ;
            } catch (Exception exc) {
                // do nothing
            }
        }

        return rfm ;
    }

    private org.omg.CORBA.Object getRemoteNamingReference( Remote remoteNamingProvider ) {
        final ServantLocator locator = new RemoteNamingServantLocator( orb, 
            remoteNamingProvider ) ;

        final PresentationManager pm = ORB.getPresentationManager() ;

        String repositoryId ;
        try {
            repositoryId = pm.getRepositoryId( remoteNamingProvider ) ;
        } catch (Exception exc) {
            throw new RuntimeException( exc ) ;
        }
    
        final List<Policy> policies = new ArrayList<Policy>() ;
        final ReferenceFactory rf = getRFM().create( "RemoteSerialContextProvider",
            repositoryId, policies, locator ) ;

        // arbitrary
        final byte[] oid = { 0, 3, 5, 7, 2, 37, 42 } ;

        final org.omg.CORBA.Object ref = rf.createReference( oid ) ;
        return ref ;
    }

    @Override
    public void initializeRemoteNaming(Remote remoteNamingProvider) 
        throws Exception {

        try {
            org.omg.CORBA.Object provider = getRemoteNamingReference( remoteNamingProvider ) ;

            // put object in NameService
            org.omg.CORBA.Object objRef = orb.resolve_initial_references("NameService");
            NamingContext ncRef = NamingContextHelper.narrow(objRef);
            // XXX use constant for SerialContextProvider name
            NameComponent nc = new NameComponent("SerialContextProvider", "");

            NameComponent path[] = {nc};
            ncRef.rebind(path, provider);
        } catch (Exception ex) {
            _logger.log(Level.SEVERE,
                 "enterprise_naming.excep_in_insertserialcontextprovider",ex);

            RemoteException re = new RemoteException("initSerialCtxProvider error", ex);
            throw re;
        }

    }

    // Called only in J2EE Server VM
    @Override
    public void initializeNaming() throws Exception {
        // NOTE: The TransientNameService reference is NOT HA.
        // new TransientNameService((com.sun.corba.ee.spi.orb.ORB)orb);
        // _logger.log(Level.FINE, "POAProtocolMgr.initializeNaming: complete");
    }


    /**     
     * Return a factory that can be used to create/destroy remote
     * references for a particular EJB type.
     * @param container The container to use
     * @param remoteHomeView The remote home view
     * @param id The object id
     * @return the ref factory
     */
    @Override
    public RemoteReferenceFactory getRemoteReferenceFactory(
        EjbContainerFacade container, boolean remoteHomeView, String id) {

        RemoteReferenceFactory factory = new POARemoteReferenceFactory
        (container, this, orb, remoteHomeView, id);

        return factory;
    }

    /**
     * Connect the RMI object to the protocol.
     */
    @Override
    public void connectObject(Remote remoteObj) throws RemoteException
    {
         StubAdapter.connect(remoteObj,  orb);    
    }	


    @Override
    public boolean isStub(Object obj) {
        return StubAdapter.isStub(obj);
    }

    @Override
    public boolean isLocal(Object obj) {
        return StubAdapter.isLocal(obj);
    }

    @Override
    public byte[] getObjectID(org.omg.CORBA.Object obj) {
        IOR ior = ((com.sun.corba.ee.spi.orb.ORB)orb).getIOR(obj, false);
	    java.util.Iterator iter = ior.iterator();

        byte[] oid = null;
        if (iter.hasNext()) {
            TaggedProfile profile = (TaggedProfile) iter.next();
            ObjectKey objKey = profile.getObjectKey();
            oid = objKey.getId().getId();
        }

        return oid;
    }

    /**
     * Return true if the two object references refer to the same
     * remote object.
     */
    @Override
    public boolean isIdentical(Remote obj1, Remote obj2) {
        if (obj1 instanceof org.omg.CORBA.Object && obj2 instanceof org.omg.CORBA.Object) { 
            org.omg.CORBA.Object corbaObj1 = (org.omg.CORBA.Object)obj1;
            org.omg.CORBA.Object corbaObj2 = (org.omg.CORBA.Object)obj2;

            return corbaObj1._is_equivalent(corbaObj2);
        } else {
            return false;  
        }       
    }

    @Override
    public void validateTargetObjectInterfaces(Remote targetObj) {
        if( targetObj != null ) {
            // All Remote interfaces implemented by targetObj will be
            // validated as a side-effect of calling setTarget().
            // A runtime exception will be propagated if validation fails.
            Tie tie = presentationMgr.getTie();
            tie.setTarget(targetObj);
        } else {
            throw new IllegalArgumentException
                ("null passed to validateTargetObjectInterfaces");
        }

    }
  
    /**
     * Map the EJB/RMI exception to a protocol-specific (e.g. CORBA) exception
     */
    @Override
    public Throwable mapException(Throwable exception) {

        boolean initCause = true;
        Throwable mappedException = exception;

        if ( exception instanceof java.rmi.NoSuchObjectException
            || exception instanceof NoSuchObjectLocalException )
        {
            mappedException = new OBJECT_NOT_EXIST(MAPEXCEPTION_CODE,
                CompletionStatus.COMPLETED_MAYBE);
        } else if ( exception instanceof java.rmi.AccessException
            || exception instanceof javax.ejb.AccessLocalException )
        {
            mappedException = new NO_PERMISSION(MAPEXCEPTION_CODE,
                CompletionStatus.COMPLETED_MAYBE);
        } else if ( exception instanceof java.rmi.MarshalException ) {
            mappedException = new MARSHAL(MAPEXCEPTION_CODE,
                CompletionStatus.COMPLETED_MAYBE);
        } else if ( exception instanceof javax.transaction.TransactionRolledbackException
            || exception instanceof TransactionRolledbackLocalException )
        {
            mappedException = new TRANSACTION_ROLLEDBACK(MAPEXCEPTION_CODE,
                CompletionStatus.COMPLETED_MAYBE);
        } else if ( exception instanceof javax.transaction.TransactionRequiredException
            || exception instanceof TransactionRequiredLocalException )
        {
            mappedException = new TRANSACTION_REQUIRED(MAPEXCEPTION_CODE,
                CompletionStatus.COMPLETED_MAYBE);
        } else if ( exception instanceof javax.transaction.InvalidTransactionException ) {
            mappedException = new INVALID_TRANSACTION(MAPEXCEPTION_CODE,
                CompletionStatus.COMPLETED_MAYBE);
        } else {
            initCause = false;
        }

        if( initCause ) {
            mappedException.initCause(exception);
        }
        
        return mappedException;
    }


    /**
     * Called from SecurityMechanismSelector for each objref creation
     */
    @Override
    public EjbDescriptor getEjbDescriptor(byte[] ejbKey) {
        EjbDescriptor result = null;

        try {
            if(_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE,
                    "POAProtocolMgr.getEjbDescriptor->: {0}", ejbKey);
            }

            if ( ejbKey.length < POARemoteReferenceFactory.EJBID_OFFSET + 8 ) {
                if(_logger.isLoggable(Level.FINE)) {
                    _logger.log(Level.FINE,
                        "POAProtocolMgr.getEjbDescriptor: {0}: {1} < {2}{3}",
                    new Object[]{ejbKey, ejbKey.length,
                        POARemoteReferenceFactory.EJBID_OFFSET, 8});
                }

                return null;
            }

            long ejbId = Utility.bytesToLong(ejbKey,
                POARemoteReferenceFactory.EJBID_OFFSET);

            if(_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE,
                    "POAProtocolMgr.getEjbDescriptor: {0}: ejbId: {1}",
                    new Object[]{ejbKey, ejbId});
            }

            EjbService ejbService = ejbServiceProvider.get();

            result = ejbService.ejbIdToDescriptor(ejbId);
        } finally {
            if(_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE,
                    "POAProtocolMgr.getEjbDescriptor<-: {0}: {1}",
                    new Object[]{ejbKey, result});
            }
        }

        return result;
   }
}
