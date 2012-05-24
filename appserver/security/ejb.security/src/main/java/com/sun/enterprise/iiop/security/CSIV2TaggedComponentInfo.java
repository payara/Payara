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

package com.sun.enterprise.iiop.security;

import java.util.Iterator ;
import java.util.Set ;
import java.util.Properties ;
import java.util.List ;

import java.util.logging.Level ;
import java.util.logging.Logger ;

import java.io.IOException ;

import org.glassfish.security.common.Role;
import org.omg.CORBA.ORB;

import com.sun.enterprise.util.Utility;
import com.sun.enterprise.deployment.EjbIORConfigurationDescriptor;
import com.sun.enterprise.deployment.EjbDescriptor;

import com.sun.logging.LogDomains ;

import com.sun.corba.ee.spi.ior.TaggedComponent ;
import com.sun.corba.ee.spi.ior.IOR;

import com.sun.corba.ee.spi.ior.iiop.IIOPProfile;
import com.sun.corba.ee.spi.ior.iiop.IIOPProfileTemplate;

import org.glassfish.pfl.basic.func.UnaryFunction ;

import com.sun.corba.ee.spi.folb.SocketInfo ;

import com.sun.corba.ee.impl.encoding.CDRInputObject;
import com.sun.corba.ee.impl.encoding.CDROutputObject;
import com.sun.corba.ee.impl.encoding.EncapsInputStream;

// The following classes are generated from CSIIOP.idl
import com.sun.corba.ee.org.omg.CSIIOP.CompoundSecMech ;
import com.sun.corba.ee.org.omg.CSIIOP.CompoundSecMechList ;
import com.sun.corba.ee.org.omg.CSIIOP.CompoundSecMechListHelper ;
import com.sun.corba.ee.org.omg.CSIIOP.EstablishTrustInClient ;
import com.sun.corba.ee.org.omg.CSIIOP.EstablishTrustInTarget ;
import com.sun.corba.ee.org.omg.CSIIOP.IdentityAssertion ;
import com.sun.corba.ee.org.omg.CSIIOP.Integrity ;
import com.sun.corba.ee.org.omg.CSIIOP.Confidentiality ;
import com.sun.corba.ee.org.omg.CSIIOP.SAS_ContextSec ;
import com.sun.corba.ee.org.omg.CSIIOP.AS_ContextSec ;
import com.sun.corba.ee.org.omg.CSIIOP.ServiceConfiguration ;
import com.sun.corba.ee.org.omg.CSIIOP.TLS_SEC_TRANS ;
import com.sun.corba.ee.org.omg.CSIIOP.TLS_SEC_TRANSHelper ;
import com.sun.corba.ee.org.omg.CSIIOP.TAG_NULL_TAG ;
import com.sun.corba.ee.org.omg.CSIIOP.TAG_CSI_SEC_MECH_LIST ;
import com.sun.corba.ee.org.omg.CSIIOP.TAG_TLS_SEC_TRANS ;
import com.sun.corba.ee.org.omg.CSIIOP.TransportAddress ;
import org.glassfish.enterprise.iiop.api.GlassFishORBHelper;
import org.omg.CORBA.INV_POLICY;
import org.omg.PortableInterceptor.IORInfo;

import org.glassfish.enterprise.iiop.impl.CSIv2Policy;

/**
 * This is the class that manages the CSIV2 tagged component information
 * in the IORs.
 * Note: For supporting FLOB in a cluster/EE mode we need to register the CSIV2TaggedComponentHandlerImpl
 * with the GlassFishORBManager.
 * @author Vivek Nagar
 * @author Harpreet Singh
 * @author Ken Cavanaugh
 */

public final class CSIV2TaggedComponentInfo
{
    public static final int SUPPORTED_IDENTITY_TOKEN_TYPES = 15;

    private static final String DEFAULT_REALM = "default";
    private static final Logger _logger ;
    private static final org.omg.IOP.TaggedComponent NULL_TAGGED_COMPONENT ;

    static{
	byte[] b = {} ;
	NULL_TAGGED_COMPONENT = new org.omg.IOP.TaggedComponent(
	    TAG_NULL_TAG.value, b);
	_logger = LogDomains.getLogger(CSIV2TaggedComponentInfo.class,LogDomains.SECURITY_LOGGER);
    }

    // Realm name is first picked up from the application object.
    // If the realm is unpopulated here, then we query it from
    // the IORDescriptor(as in for a standalone ejb case).
    // The fallback is "default"
  //  private String _realm_name = null;
 //   private byte[] _realm_name_bytes = null;
    private ORB orb;
    private int sslMutualAuthPort;
    private GlassFishORBHelper orbHelper;
    
    
    public CSIV2TaggedComponentInfo(ORB orb) {
	this.orb = orb;	
        orbHelper = Lookups.getGlassFishORBHelper();
    }

    public CSIV2TaggedComponentInfo(ORB orb, int sslMutualAuthPort) {
	this( orb ) ;
	this.sslMutualAuthPort = sslMutualAuthPort ;
    }

    public  EjbDescriptor getEjbDescriptor( IORInfo iorInfo ) {
	CSIv2Policy csiv2Policy = null;
	try {
	    csiv2Policy = (CSIv2Policy)iorInfo.get_effective_policy(
                orbHelper.getCSIv2PolicyType());
	} catch ( INV_POLICY ex ) {
	    _logger.log(Level.FINE, 
			"CSIV2TaggedComponentInfo.getEjbDescriptor: CSIv2Policy not present");
	}
	    
	// Add CSIv2 tagged component for this EJB type.
	if(_logger.isLoggable(Level.FINE)) {
	    _logger.log(Level.FINE, 
			"TxSecIORInterceptor.establish_components: CSIv2Policy: " + csiv2Policy);
	}

	EjbDescriptor ejbDesc = null ;
	if (csiv2Policy != null)
	    ejbDesc = csiv2Policy.getEjbDescriptor() ;

	return ejbDesc ;
    }

    /**
     * Create the security mechanism list tagged component based 
     * on the deployer specified configuration information.
     * This method is on the server side for all ejbs in the
     * non-cluster app server case. 
     */
    public org.omg.IOP.TaggedComponent createSecurityTaggedComponent(
	int sslPort, EjbDescriptor desc) {

        org.omg.IOP.TaggedComponent tc = null;
	try {
            if (_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE, "IIOP: Creating a Security Tagged Component");
            }
	    
            // get the realm from the application object.
            //_realm_name = desc.getApplication().getRealm();
            CompoundSecMech[] mechList = createCompoundSecMechs(sslPort, desc);
            tc = createCompoundSecMechListComponent(mechList);
        } catch(Exception e) {
            _logger.log(Level.SEVERE,"iiop.createcompund_exception",e);
        }

        return tc;
    }

    /** Create the CSIv2 tagged component for a clustered app server.
     */
    public org.omg.IOP.TaggedComponent createSecurityTaggedComponent(
	List<SocketInfo> socketInfos, EjbDescriptor desc)
    {
        org.omg.IOP.TaggedComponent tc = null;
        if (desc != null) {
            try {
                if (_logger.isLoggable(Level.FINE)) {
                    _logger.log(Level.FINE, "IIOP: Creating a Security Tagged Component");
                }

                // get the realm from the application object.
                //  _realm_name = desc.getApplication().getRealm();
                CompoundSecMech[] mechList = createCompoundSecMechs( socketInfos, desc ) ;
                tc =  createCompoundSecMechListComponent(mechList);
            } catch(Exception e) {
                _logger.log(Level.SEVERE,"iiop.createcompund_exception",e);
            }
        }

        return tc;
    }

    private boolean getBooleanValue(Properties props, String name ) {
	String val = props.getProperty( name, "false" ) ;
	boolean result = val.equals( "true" ) ;
	return result ;
    }

    /**
     * This method is called on the server side for all non-EJB POAs. 
     */
    public org.omg.IOP.TaggedComponent createSecurityTaggedComponent(int sslPort ) {

        org.omg.IOP.TaggedComponent tc = null;

	try {
	    Properties props = orbHelper.getCSIv2Props();
	    boolean sslRequired = getBooleanValue(props,
		GlassFishORBHelper.ORB_SSL_SERVER_REQUIRED);
	    boolean clientAuthRequired = getBooleanValue(props,
		GlassFishORBHelper.ORB_CLIENT_AUTH_REQUIRED);

	    CompoundSecMech[] mechList = new CompoundSecMech[1]; 

	    org.omg.IOP.TaggedComponent transportMech = createSSLInfo(
		sslPort, null, sslRequired);
	    
	    // Create AS_Context
	    AS_ContextSec asContext = createASContextSec(null, DEFAULT_REALM);

	    // Create SAS_Context
	    SAS_ContextSec sasContext = createSASContextSec(null);

	    short targetRequires = 
		    (clientAuthRequired ? EstablishTrustInClient.value : 0);

	    // Convert Profile.TaggedComponent to org.omg.IOP.TaggedComponent
	    mechList[0] = new CompoundSecMech(targetRequires, 
		transportMech, asContext, sasContext);

	    tc =  createCompoundSecMechListComponent(mechList);
        } catch(Exception e) {
            _logger.log(Level.SEVERE,"iiop.createcompund_exception",e);
        }
        return tc;
    }

    private org.omg.IOP.TaggedComponent createCompoundSecMechListComponent(
	CompoundSecMech[] mechList) {

 	CDROutputObject out = (CDROutputObject) orb.create_output_stream();
	out.putEndian();

	boolean stateful = false; 
	CompoundSecMechList list = new CompoundSecMechList(
	    stateful, mechList);
	CompoundSecMechListHelper.write(out, list);
	byte[] buf = out.toByteArray();
	org.omg.IOP.TaggedComponent tc = new org.omg.IOP.TaggedComponent( 
	    TAG_CSI_SEC_MECH_LIST.value, buf ) ;
	return tc;
    }

    private Set<EjbIORConfigurationDescriptor> getIORConfigurationDescriptors( 
	EjbDescriptor desc ) {

        if (desc == null) {
            return null ;
        }

	Set<EjbIORConfigurationDescriptor> iorDescSet = desc.getIORConfigurationDescriptors();
	int size = iorDescSet.size();
	if (size == 0) {
	    // No IOR config descriptors:
	    // Either none were configured or 1.2.x app.

	    // Create an IOR config desc with SSL supported
	    EjbIORConfigurationDescriptor eDesc = 
		new EjbIORConfigurationDescriptor();
	    eDesc.setIntegrity(
		EjbIORConfigurationDescriptor.SUPPORTED);
	    eDesc.setConfidentiality(
		EjbIORConfigurationDescriptor.SUPPORTED);
	    eDesc.setEstablishTrustInClient(
		EjbIORConfigurationDescriptor.SUPPORTED);
	    iorDescSet.add(eDesc);
	    size = 1;

	    // Check if method permissions are set on the descriptor.
	    // If they are then enable username_password mechanism in as_context
	    Set<Role> permissions = desc.getPermissionedRoles();
	    if (permissions.size() > 0) {
                if(_logger.isLoggable(Level.FINE)){
                    _logger.log(Level.FINE,"IIOP:Application has protected methods");
                }

	        eDesc.setAuthMethodRequired(true);
                String realmName = DEFAULT_REALM;


                if (desc.getApplication() != null) {
                    realmName = desc.getApplication().getRealm();
                }
                
                if (realmName == null) {
                    realmName = DEFAULT_REALM;
                }
                eDesc.setRealmName(realmName);
	    }
	}

	return iorDescSet;
    }
    
    // Type of simple closure used for createCompoundSecMechs
    private interface DescriptorMaker extends
	UnaryFunction<EjbIORConfigurationDescriptor,org.omg.IOP.TaggedComponent> { } 

    /**
     * Create the security mechanisms. Only 1 such mechanism is created
     * although the spec allows multiple mechanisms (in decreasing order
     * of preference).  Note that creating more than one CompoundSecMech
     * here will cause getSecurityMechanisms to fail, as it supports
     * only one CompoundSecMech.
     */
    private CompoundSecMech[] createCompoundSecMechs(
	DescriptorMaker maker, EjbDescriptor desc) throws IOException {

        if(_logger.isLoggable(Level.FINE)){
            _logger.log(Level.FINE, "IIOP: Creating CompoundSecMech");
        }

        if (desc == null) {
            return null ;
        }

	Set iorDescSet = getIORConfigurationDescriptors( desc ) ;

        CompoundSecMech[] mechList = new CompoundSecMech[iorDescSet.size()]; 
	Iterator<EjbIORConfigurationDescriptor> itr = iorDescSet.iterator();
        if(_logger.isLoggable(Level.FINE)){
	    	_logger.log(Level.FINE,"IORDescSet SIZE:" + iorDescSet.size());
	}
        String realmName = DEFAULT_REALM;

	for(int i = 0; i < iorDescSet.size(); i++) {
	    EjbIORConfigurationDescriptor iorDesc = itr.next();
            int target_requires = getTargetRequires(iorDesc);
	    org.omg.IOP.TaggedComponent comp = maker.evaluate( iorDesc ) ;
	    
            if( desc.getApplication() != null)  {
                realmName = desc.getApplication().getRealm();
            }

            if(realmName == null) {
                realmName = iorDesc.getRealmName();
            }

            if(realmName == null) {
                realmName = DEFAULT_REALM;
            }
            // Create AS_Context
            AS_ContextSec asContext = createASContextSec(iorDesc, realmName);
	    
            // Create SAS_Context
            SAS_ContextSec sasContext = createSASContextSec(iorDesc);
	    
	    // update the target requires value
	    int targ_req = target_requires | asContext.target_requires
		| sasContext.target_requires;
	    
            // Convert Profile.TaggedComponent to org.omg.IOP.TaggedComponent
            mechList[i] = new CompoundSecMech((short)targ_req,
                            comp, asContext, sasContext);
	}
        return mechList;
    }

    private CompoundSecMech[] createCompoundSecMechs( 
	final List<SocketInfo> socketInfos,
	final EjbDescriptor desc ) throws IOException {

	DescriptorMaker maker = new DescriptorMaker() {
	    public org.omg.IOP.TaggedComponent evaluate( EjbIORConfigurationDescriptor desc ) {
		return createSSLInfo( socketInfos, desc, false ) ;
	    }
	} ;

	return createCompoundSecMechs( maker, desc ) ;
    }

    private CompoundSecMech[] createCompoundSecMechs( 
	final int sslPort,
	final EjbDescriptor desc ) throws IOException {

	DescriptorMaker maker = new DescriptorMaker() {
	    public org.omg.IOP.TaggedComponent evaluate( EjbIORConfigurationDescriptor desc ) {
		return createSSLInfo( sslPort, desc, false ) ;
	    }
	} ;

	return createCompoundSecMechs( maker, desc ) ;
    }

    /**
     * Create the AS layer context within a compound mechanism definition.
     */
    public AS_ContextSec createASContextSec(
			EjbIORConfigurationDescriptor iorDesc, String realmName)
        		throws IOException
    {
        AS_ContextSec asContext = null;
        int target_supports = 0;
        int target_requires = 0;
        byte[] client_authentication_mechanism = {};
        byte[] target_name = {} ;
        String authMethod  = null;
        boolean authMethodRequired = false;

        if(_logger.isLoggable(Level.FINE)){
            _logger.log(Level.FINE, "IIOP: Creating AS_Context");
        }

        // If AS_ContextSec is not required to be generated in an IOR,
        // then optimize the code by not generating and filling in fields that are
        // irrelevant.

        if (iorDesc != null) {
            authMethod = iorDesc.getAuthenticationMethod();
            authMethodRequired = iorDesc.isAuthMethodRequired();
	}

        if ((authMethod != null) && 
	    (authMethod.equalsIgnoreCase(EjbIORConfigurationDescriptor.NONE))) {

            asContext = new AS_ContextSec((short)target_supports, 
		(short)target_requires, client_authentication_mechanism, 
		target_name);

            return asContext;
	}
            
        /** Functionality for Realm Per App
         * Try to get the realm from the descriptor, else fill in default
         */
  /*      if(_realm_name == null){// realm name should be populated at this point
            if(iorDesc != null){
                _realm_name = iorDesc.getRealmName();
            }

            if(_realm_name == null){
                _realm_name = DEFAULT_REALM;
                if(_logger.isLoggable(Level.FINE)){
                    _logger.log(Level.FINE, "IIOP:AS_Context: Realm Name = null,"
                                + " setting default realm for logging in");
                }
            }
        }*/

        if(_logger.isLoggable(Level.FINE)){
            _logger.log(Level.FINE, "IIOP:AS_Context: Realm Name for login = "+
                        realmName);
        }

        if (realmName == null) {
            realmName = iorDesc.getRealmName();
        }
        if (realmName == null) {
            realmName = DEFAULT_REALM;
        }
        byte[] _realm_name_bytes = realmName.getBytes();
        
        target_name = GSSUtils.createExportedName(
                               GSSUtils.GSSUP_MECH_OID,
		               _realm_name_bytes);

        target_supports = EstablishTrustInClient.value;

        if  (authMethodRequired){
            target_requires = EstablishTrustInClient.value;
        }

        client_authentication_mechanism = GSSUtils.getMechanism();

        asContext = new AS_ContextSec((short)target_supports,
                                        (short)target_requires,
                                        client_authentication_mechanism,
                                        target_name);

        return asContext;
    }

    /**
     * Create the SAS layer context within a compound mechanism definition.
     */
    public SAS_ContextSec createSASContextSec(
		    EjbIORConfigurationDescriptor iorDesc)
        	    throws IOException
    {
        SAS_ContextSec sasContext = null;
	// target_supports = 0 means that target supports ITTAbsent
        int target_supports = 0;
        int target_requires = 0;
        ServiceConfiguration[] priv = new ServiceConfiguration[0];
        String callerPropagation = null;
        byte[][] mechanisms = {};
        
        if(_logger.isLoggable(Level.FINE)){
            _logger.log(Level.FINE, "IIOP: Creating SAS_Context");
        }


	// this shall be non-zero if target_supports is non-zero
	int supported_identity_token_type = 0;

        if (iorDesc != null) {
            callerPropagation = iorDesc.getCallerPropagation();
        }

        if ((callerPropagation != null) 
	    && (callerPropagation.equalsIgnoreCase(EjbIORConfigurationDescriptor.NONE))){
            sasContext = new SAS_ContextSec((short)target_supports,
                                            (short)target_requires,
                                            priv, mechanisms,
					    supported_identity_token_type);
            return sasContext;
	}

	target_supports = IdentityAssertion.value;

        byte[] upm = GSSUtils.getMechanism(); // Only username_password mechanism
        mechanisms = new byte[1][upm.length];
        for(int i = 0; i < upm.length; i++) {
            mechanisms[0][i] = upm[i];
        }

	// para 166 of CSIv2 spec says that the bit corresponding to the
	// ITTPrincipalName is non-zero if supported_mechanism has atleast
	// 1 element. Supported_mechanism has the value of GSSUP OID
	if (target_supports != 0){
	    supported_identity_token_type = SUPPORTED_IDENTITY_TOKEN_TYPES;
        }

        sasContext = new SAS_ContextSec((short)target_supports,
                                        (short)target_requires,
                                        priv, mechanisms,
					supported_identity_token_type);

        return sasContext;
    }

    /**
     * Get the value of target_supports for the transport layer.
     */
    public int getTargetSupports(EjbIORConfigurationDescriptor iorDesc)
    {
	if ( iorDesc == null ) {
            return 0;
        }

	int supports = 0;
	String integrity = iorDesc.getIntegrity();
	if(!integrity.equalsIgnoreCase(EjbIORConfigurationDescriptor.NONE)) {
	    supports = supports | Integrity.value;
	}

	String confidentiality = iorDesc.getConfidentiality();
	if(!confidentiality.equalsIgnoreCase(EjbIORConfigurationDescriptor.NONE)) {
	    supports = supports | Confidentiality.value;
	}

	String establishTrustInTarget = iorDesc.getEstablishTrustInTarget();
	if(!establishTrustInTarget.equalsIgnoreCase(EjbIORConfigurationDescriptor.NONE)) {
	    supports = supports | EstablishTrustInTarget.value;
	}

	String establishTrustInClient = iorDesc.getEstablishTrustInClient();
	if(!establishTrustInClient.equalsIgnoreCase(EjbIORConfigurationDescriptor.NONE)) {
	    supports = supports | EstablishTrustInClient.value;
	}

	return supports;
    }

    /**
     * Get the value of target_requires for the transport layer.
     */
    public int getTargetRequires(EjbIORConfigurationDescriptor iorDesc)
    {
	if ( iorDesc == null ) {
            return 0;
        }

	int requires = 0;
	String integrity = iorDesc.getIntegrity();
	if(integrity.equalsIgnoreCase(EjbIORConfigurationDescriptor.REQUIRED)) {
	    requires = requires | Integrity.value;
	}

	String confidentiality = iorDesc.getConfidentiality();
	if(confidentiality.equalsIgnoreCase(EjbIORConfigurationDescriptor.REQUIRED)) {
	    requires = requires | Confidentiality.value;
	}

	String establishTrustInTarget = iorDesc.getEstablishTrustInTarget();
	if(establishTrustInTarget.equalsIgnoreCase(EjbIORConfigurationDescriptor.REQUIRED)) {
	    requires = requires | EstablishTrustInTarget.value;
	}

	String establishTrustInClient = iorDesc.getEstablishTrustInClient();
	if(establishTrustInClient.equalsIgnoreCase(EjbIORConfigurationDescriptor.REQUIRED)) {
	    requires = requires | EstablishTrustInClient.value;
	}

	return requires;
    }

    private int getTargetSupportsDefault( EjbIORConfigurationDescriptor desc ) {
	int targetSupports = 0 ;
	if (desc == null)
	    targetSupports = Integrity.value | Confidentiality.value 
		| EstablishTrustInClient.value | EstablishTrustInTarget.value;
	else
	    targetSupports = getTargetSupports(desc);

	return targetSupports ;
    }

    private int getTargetRequiresDefault( EjbIORConfigurationDescriptor desc, boolean sslRequired ) {
	int targetRequires = 0 ;
	if (desc == null) {
	    if (sslRequired)
		targetRequires = Integrity.value | Confidentiality.value 
		    | EstablishTrustInClient.value;
	} else {
	    targetRequires = getTargetRequires(desc);
	}

	return targetRequires ;
    }

    private org.omg.IOP.TaggedComponent createTlsSecTransComponent( 
	int targetSupports, int targetRequires, TransportAddress[] listTa ) {

	TLS_SEC_TRANS tls_sec = new TLS_SEC_TRANS(
	    (short)targetSupports, (short)targetRequires, listTa);

 	CDROutputObject out = (CDROutputObject) orb.create_output_stream();
 	out.putEndian() ; 
 	TLS_SEC_TRANSHelper.write((org.omg.CORBA.portable.OutputStream)out, tls_sec);

	byte[] buf = out.toByteArray() ;
	
	// create new Tagged Component for SSL
	org.omg.IOP.TaggedComponent tc = new org.omg.IOP.TaggedComponent(
			TAG_TLS_SEC_TRANS.value, buf ) ;
	return tc;
    }

    private TransportAddress[] generateTransportAddresses(
	int sslPort ) {

	String hostName = Utility.getLocalAddress();
	short shortPort = Utility.intToShort(sslPort);
	TransportAddress ta = new TransportAddress(hostName, shortPort);
	TransportAddress[] listTa = new TransportAddress[] { ta } ;

	return listTa;
    }

    private TransportAddress[] generateTransportAddresses( 
	List<SocketInfo> socketInfos ) {

        TransportAddress[] listTa = new TransportAddress[socketInfos.size()];
	for(int i=0; i<socketInfos.size(); i++){
            SocketInfo socketInfo = socketInfos.get(i);
            int sslport = socketInfo.port();
            String host = socketInfo.host();
            short short_port = Utility.intToShort(sslport);
            TransportAddress ta = new TransportAddress(host, short_port);
            listTa[i] = ta;
        }
	return listTa;
    }

    /**
     * Create the SSL tagged component within a compound mechanism
     * definition.
     */
    private org.omg.IOP.TaggedComponent createSSLInfo(
	int sslport, 
	EjbIORConfigurationDescriptor iorDesc, boolean sslRequired ) {

	int targetSupports = getTargetSupportsDefault( iorDesc ) ;
	int targetRequires = getTargetRequiresDefault( iorDesc, sslRequired ) ;
	boolean mutualAuthRequired = (iorDesc != null) &&
	    ((targetRequires & EstablishTrustInClient.value) ==
	     EstablishTrustInClient.value) ;
        int ssl_port = mutualAuthRequired ? sslMutualAuthPort : sslport; 
	
        if(_logger.isLoggable(Level.FINE)){
            _logger.log(Level.FINE, "IIOP: Creating Transport Mechanism for sslport "
		+ ssl_port );
        }

        /* 
         * if both targetSupports and targetRequires are zero, then the 
         * mechanism does not support a transport_mechanism and hence
         * a TAG_NULL_TAG must be generated.
         */

        if ( (targetSupports | targetRequires) == 0  || ssl_port == -1) {
	     return NULL_TAGGED_COMPONENT ;
	}

	TransportAddress[] listTa = generateTransportAddresses( ssl_port ) ;
	return createTlsSecTransComponent( targetSupports, targetRequires, listTa ) ;
    }

    private org.omg.IOP.TaggedComponent createSSLInfo(
	List<SocketInfo> socketInfos, 
        EjbIORConfigurationDescriptor iorDesc, boolean sslRequired ) {

	int targetSupports = getTargetSupportsDefault( iorDesc ) ;
	int targetRequires = getTargetRequiresDefault( iorDesc, sslRequired ) ;
	
        if(_logger.isLoggable(Level.FINE)){
            _logger.log(Level.FINE, "IIOP: Creating Transport Mechanism for socketInfos "
		+ socketInfos );
        }

        /* 
         * if both targetSupports and targetRequires are zero, then the 
         * mechanism does not support a transport_mechanism and hence
         * a TAG_NULL_TAG must be generated.
         */

        if ( (targetSupports | targetRequires) == 0 ) {
	     return NULL_TAGGED_COMPONENT ;
	}

	TransportAddress[] listTa = generateTransportAddresses(socketInfos);
	return createTlsSecTransComponent( targetSupports, targetRequires, listTa ) ;
    }

    /**
     * This method determines if all the mechanisms defined in the
     * CSIV2 CompoundSecMechList structure require protected 
     * invocations.
     */
    public boolean allMechanismsRequireSSL(Set iorDescSet) {
	int size = iorDescSet.size();
	if(size == 0) {
	    return false;
	}

	Iterator<EjbIORConfigurationDescriptor> itr = 
	    iorDescSet.iterator();

	for(int i = 0; i < size; i++) {
	    EjbIORConfigurationDescriptor iorDesc = itr.next();
            int target_requires = getTargetRequires(iorDesc);
	    if(target_requires == 0) {
		return false;
	    }
	}

	return true;
    }

    /**
     * Get the Compound security mechanism list from the given IOR.
     * @param the IOR.
     * @return the array of compound security mechanisms.
     */
    public CompoundSecMech[] getSecurityMechanisms(IOR ior) {
	IIOPProfile prof = ior.getProfile();
	IIOPProfileTemplate ptemp = (IIOPProfileTemplate)prof.
 			    getTaggedProfileTemplate();
	Iterator<TaggedComponent> itr = ptemp.iteratorById(
	    TAG_CSI_SEC_MECH_LIST.value);

	if(!itr.hasNext()) {
            if(_logger.isLoggable(Level.FINE)){
                String msg = "IIOP:TAG_CSI_SEC_MECH_LIST tagged component not found";
                _logger.log(Level.FINE, msg);
	    }
	    return null;
	}

	TaggedComponent tcomp = itr.next();
	if(_logger.isLoggable(Level.FINE)){
	    _logger.log(Level.FINE,"Component:" + tcomp);
	}

	if (itr.hasNext()) {
	    String msg = "More than one TAG_CSI_SEC_MECH_LIST tagged " + 
			 "component found ";
            _logger.log(Level.SEVERE,"iiop.many_tagged_component");
	    throw new RuntimeException(msg);
	}

	org.omg.IOP.TaggedComponent comp = tcomp.getIOPComponent(orb);
	byte[] b = comp.component_data;
	CDRInputObject in = (CDRInputObject) new EncapsInputStream(orb, b, b.length);
	in.consumeEndian();
	CompoundSecMechList l = CompoundSecMechListHelper.read(in);
	CompoundSecMech[] list = l.mechanism_list;

	return list;
    }

    /**
     * Retrieve the SSL tagged component from the compound security
     * mechanism.
     */
    public TLS_SEC_TRANS getSSLInformation(CompoundSecMech mech){
	org.omg.IOP.TaggedComponent pcomp = mech.transport_mech; 
	TLS_SEC_TRANS ssl = getSSLComponent(pcomp);
	return ssl;
    }
	
    private TLS_SEC_TRANS getSSLComponent(org.omg.IOP.TaggedComponent comp) {

	TLS_SEC_TRANS ssl = null;        
	
        // a TAG_NULL_TAG implies that SSL is not required
        if (comp.tag == TAG_NULL_TAG.value){
            ssl = null;
        } else {
            byte[] b = comp.component_data;
            CDRInputObject in = (CDRInputObject)  new EncapsInputStream(orb, b, b.length);
            in.consumeEndian();
            ssl = TLS_SEC_TRANSHelper.read(in);
        }

	return ssl;
    }
}
