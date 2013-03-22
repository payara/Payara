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

package com.sun.enterprise.iiop.security;

import com.sun.enterprise.common.iiop.security.AnonCredential;
import com.sun.enterprise.common.iiop.security.GSSUPName;
import com.sun.enterprise.common.iiop.security.SecurityContext;




import com.sun.corba.ee.org.omg.CSI.*;
import com.sun.corba.ee.org.omg.CSIIOP.CompoundSecMech;
import com.sun.enterprise.security.auth.login.common.PasswordCredential;
import com.sun.enterprise.security.auth.login.common.X509CertificateCredential;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.logging.LogDomains;

import java.util.*;
import java.util.logging.Level;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.security.cert.X509Certificate;

import org.glassfish.enterprise.iiop.api.GlassFishORBHelper;
import org.omg.CORBA.*;
import org.omg.PortableInterceptor.*;
import org.omg.IOP.*;
import sun.security.util.DerOutputStream;
import sun.security.util.DerValue;
import sun.security.x509.X500Name;

/**
 * This class implements a client side security request interceptor for CSIV2.
 * It is used to send and receive the service context in a service context
 * element in the service context list in an IIOP header.
 *
 * 
 */

public class SecClientRequestInterceptor extends    org.omg.CORBA.LocalObject
                                    implements ClientRequestInterceptor   {
    
    private static java.util.logging.Logger _logger=null;
    static{
       _logger=LogDomains.getLogger(SecClientRequestInterceptor.class,LogDomains.SECURITY_LOGGER);
        }

    private static LocalStringManagerImpl localStrings =
	new LocalStringManagerImpl(SecClientRequestInterceptor.class);

    private String name;                 // name of interceptor
    
    /**
     * prname  (name + "::") is name of interceptor used for logging 
     * purposes. It is only used in the call to Logger.methodentry()
     * in this file. Its purpose is to identify the interceptor name 
     */
    private String prname; 
    private Codec  codec;                // used for marshalling
    //private ORB    orb;                  
    //private SecurityService secsvc;
    private GlassFishORBHelper orbHelper;
    private SecurityContextUtil secContextUtil;

    /** 
     *  Hard code the value of 15 for SecurityAttributeService until
     *  it is defined in IOP.idl.
     *     sc.context_id = SecurityAttributeService.value;
     */
    protected static final int SECURITY_ATTRIBUTE_SERVICE_ID = 15;
   
    public SecClientRequestInterceptor(String name, Codec codec) {
	this.name   = name;
        this.codec  = codec;
        this.prname = name + "::";
        orbHelper = Lookups.getGlassFishORBHelper();
        secContextUtil = Lookups.getSecurityContextUtil();
    }

    public String name() {
	return name;
    }

    /**
     * Retrieves a single credential from a credset for the specified class.
     * It also performs some semantic checking and logging.
     *  
     * A null is returned if semantic checking fails. 
     */
    private java.lang.Object getCred(Set credset, Class c) {

        java.lang.Object cred = null ; // return value
        String clsname = c.getName() ;
        
        /* check that there is only instance of a credential in the subject */
	    if(_logger.isLoggable(Level.FINE)) {
		_logger.log(Level.FINE,"Checking for a single instance of class in subject");
                _logger.log(Level.FINE,"    Classname = " + clsname);
	}
        if (credset.size() != 1) {
	    if(_logger.isLoggable(Level.SEVERE)) 
		_logger.log(Level.SEVERE,"iiop.multiple_credset",new java.lang.Object[]{Integer.valueOf(credset.size()),clsname});
            throw new SecurityException(
		localStrings.getLocalString("secclientreqinterceptor.inv_credlist_size",
		                            "Credential list size is not 1."));
	}

        Iterator iter = credset.iterator();
        while (iter.hasNext())
            cred = iter.next();
	    if(_logger.isLoggable(Level.FINE)) 
                _logger.log(Level.FINE,"Verified single instance of class ( " +clsname + " )");
        return cred;
    }

    /**
     *  Returns a client authentication token for the
     *  PasswordCredential in the subject.
     *  The client authentication token is cdr encoded.
     */

    private byte[] createAuthToken(java.lang.Object cred, Class cls, ORB orb, CompoundSecMech mech)
        throws Exception
    {
        byte[] gsstoken = {};      // GSS token

        if (PasswordCredential.class.isAssignableFrom(cls)) {

                _logger.log(Level.FINE,"Constructing a PasswordCredential client auth token");

            /* Generate mechanism specific GSS token for the GSSUP mechanism */
            PasswordCredential pwdcred = (PasswordCredential) cred;
	    GSSUPToken tok = GSSUPToken.getClientSideInstance(orb, codec, pwdcred, mech);
            gsstoken = tok.getGSSToken();
	}
        return gsstoken;
    }
 
    /**       
     *  create and return an identity token from the credential. 
     *  The identity token is cdr encoded.
     */
    private IdentityToken createIdToken(java.lang.Object cred, Class cls, ORB orb)
        throws Exception {

        IdentityToken idtok   = null;

        DerOutputStream dos = new DerOutputStream();
        DerValue[]  derval  = null ; // DER encoding buffer
        //byte[] cdrval ;            // CDR encoding buffer
        Any  any = orb.create_any();
        idtok = new IdentityToken();
  
        if (X500Name.class.isAssignableFrom(cls)) {
                _logger.log(Level.FINE,"Constructing an X500 DN Identity Token");
            X500Name credname = (X500Name) cred;
            credname.encode(dos);  // ASN.1 encoding
            X501DistinguishedNameHelper.insert(any, dos.toByteArray());

            /* IdentityToken with CDR encoded X501 name */
            idtok.dn(codec.encode_value(any)); 
        } else if (X509CertificateCredential.class.isAssignableFrom(cls)) {
                _logger.log(Level.FINE,"Constructing an X509 Certificate Chain Identity Token");
	    /* create a DER encoding */
            X509CertificateCredential certcred = (X509CertificateCredential) cred;
            X509Certificate[] certchain = certcred.getX509CertificateChain();
                _logger.log(Level.FINE,"Certchain length = " + certchain.length);
            derval = new DerValue[certchain.length];
            for (int i = 0; i < certchain.length ; i++)
                derval[i] = new DerValue(certchain[i].getEncoded());
            dos.putSequence(derval);
            X509CertificateChainHelper.insert(any, dos.toByteArray());

            /* IdentityToken with CDR encoded certificate chain */
            idtok.certificate_chain(codec.encode_value(any));
        } else if (AnonCredential.class.isAssignableFrom(cls)) {
                _logger.log(Level.FINE,"Constructing an Anonymous Identity Token");
            idtok.anonymous(true);

        } else if (GSSUPName.class.isAssignableFrom(cls)) {
            /* GSSAPI Exported name */
            _logger.log(Level.FINE,"Constructing a GSS Exported name Identity Token");
	    /* create a DER encoding */
            GSSUPName gssname = (GSSUPName) cred;

            byte[] expname = gssname.getExportedName();
            GSS_NT_ExportedNameHelper.insert(any, expname);

            /* IdentityToken with CDR encoded GSSUPName */
            idtok.principal_name(codec.encode_value(any));
	}
        return (idtok);
    }

    /**
     * send_request() interception point adds the security context to the
     * service context field.
     */
    public void send_request(ClientRequestInfo ri) throws ForwardRequest
    {
        /**
         * CSIV2 level 0 implementation only requires stateless clients.
         * Client context id is therefore always set to 0.
         */
        long  cContextId = 0;         // CSIV2 requires type to be long

        // XXX: Workaround for non-null connection object ri for local invocation.
        ConnectionExecutionContext.removeClientThreadID();
        /**
         * CSIV2 level 0 implementation does not require any authorization
         * tokens to be sent over the wire. So set cAuthzElem to empty.
         */
	AuthorizationElement[] cAuthzElem = {} ;

        /* Client identity token to be added to the service context field */
        IdentityToken cIdentityToken  = null;

        /* Client authentication token to be added to the service context field */
        byte[] cAuthenticationToken   = {} ;

        /* CDR encoded Security Attribute Service element */
        byte[] cdr_encoded_saselm     = null;

        java.lang.Object  cred = null ; // A single JAAS credential

	if(_logger.isLoggable(Level.FINE))
                _logger.log(Level.FINE,"++++ Entered " + prname + "send_request" + "()");
        SecurityContext secctxt = null;       // SecurityContext to be sent 
	ORB orb = orbHelper.getORB();
	org.omg.CORBA.Object effective_target = ri.effective_target();
	try{
	    secctxt = secContextUtil.getSecurityContext(effective_target);
	}catch(InvalidMechanismException ime){
               _logger.log(Level.SEVERE,"iiop.sec_context_exception",ime);
	    throw new RuntimeException(ime.getMessage());
	}catch(InvalidIdentityTokenException iite){
                _logger.log(Level.SEVERE,"iiop.runtime_exception",iite);
	    throw new RuntimeException(iite.getMessage());
	}

        /**
         * In an unprotected invocation, there is nothing to be sent to 
         * the service context field. Check for this case.
         */
        if (secctxt == null) {
            if(_logger.isLoggable(Level.FINE)){
                _logger.log(Level.FINE,"Security context is null (nothing to add to service context)");
            }
            return;
	}

	final SecurityContext sCtx = secctxt;
        /* Construct an authentication token */
        if (secctxt.authcls != null) {
            cred = AccessController.doPrivileged(new PrivilegedAction() {
                public java.lang.Object run() {
                    return getCred(sCtx.subject.getPrivateCredentials(sCtx.authcls), sCtx.authcls);
                }
            });

            try {
                
                SecurityMechanismSelector sms = Lookups.getSecurityMechanismSelector();
                ConnectionContext cc = sms.getClientConnectionContext();
                CompoundSecMech mech = cc.getMechanism();

                cAuthenticationToken = createAuthToken(cred, secctxt.authcls, orb, mech);
            } catch (Exception e) {
                _logger.log(Level.SEVERE,"iiop.createauthtoken_exception",e);
	        throw new SecurityException(
		    localStrings.getLocalString("secclientreqinterceptor.err_authtok_create",
					        "Error while constructing an authentication token."));
            }
	}
        

        /* Construct an identity token */
        if (secctxt.identcls != null) {
            cred = getCred(secctxt.subject.getPublicCredentials(secctxt.identcls),
                           secctxt.identcls);
            try {
                cIdentityToken = createIdToken(cred, secctxt.identcls, orb);
            } catch (Exception e) {
                _logger.log(Level.SEVERE,"iiop.createidtoken_exception",e);
	        throw new SecurityException(
		    localStrings.getLocalString("secclientreqinterceptor.err_idtok_create",
					        "Error while constructing an identity token."));
            }
	} else {
            if(_logger.isLoggable(Level.FINE)){
                _logger.log(Level.FINE,"Constructing an Absent Identity Token");
            }
             cIdentityToken = new IdentityToken();
             cIdentityToken.absent(true);
	}

        if(_logger.isLoggable(Level.FINE)){
            _logger.log(Level.FINE,"Creating an EstablishContext message");
        }
	EstablishContext ec = new EstablishContext(cContextId,
                                                   cAuthzElem,
    	                                           cIdentityToken,
                                                   cAuthenticationToken);

	SASContextBody sasctxbody = new SASContextBody();
	sasctxbody.establish_msg(ec);

        /* CDR encode the SASContextBody */
        Any SasAny = orb.create_any();
        SASContextBodyHelper.insert(SasAny, sasctxbody);

        try {        
	    cdr_encoded_saselm = codec.encode_value(SasAny);
        } catch (Exception e) {
                _logger.log(Level.SEVERE,"iiop.encode_exception",e);
	    throw new SecurityException(
		localStrings.getLocalString("secclientreqinterceptor.err_cdr_encode",
		                            "CDR Encoding error for a SAS context element."));
        }

        /* add SAS element to service context list*/
        ServiceContext sc  = new ServiceContext();
        sc.context_id      = SECURITY_ATTRIBUTE_SERVICE_ID;
        sc.context_data    = cdr_encoded_saselm;
        if(_logger.isLoggable(Level.FINE)){
            _logger.log(Level.FINE,"Adding EstablishContext message to service context list");
        }
        boolean no_replace = false;
        ri.add_request_service_context(sc, no_replace);
        if(_logger.isLoggable(Level.FINE)){
            _logger.log(Level.FINE,"Added EstablishContext message to service context list");
        }
    }

    public void send_poll(ClientRequestInfo ri) {
    }


    /**
     * set the reply status
     */
    private void setreplyStatus(int status, org.omg.CORBA.Object target) {
        if(_logger.isLoggable(Level.FINE)){
            _logger.log(Level.FINE,"Status to be set : " + status);
        }
        
        SecurityContextUtil.receivedReply(status, target);
        if(_logger.isLoggable(Level.FINE)){
                _logger.log(Level.FINE,"Invoked receivedReply()");
        }
    }

    /**
     * Map the reply status code to a format suitable for J2EE RI.
     * 
     * @param  repst  reply status from the service context field.
     * @return        mapped status code
     *
     */
    private int mapreplyStatus(int repst)
    {
        int status;

        if(_logger.isLoggable(Level.FINE)){
                _logger.log(Level.FINE,"Reply status to be mapped =  " + repst);
        }

        switch (repst) {

	case SUCCESSFUL.value:
        case USER_EXCEPTION.value:
            status = SecurityContextUtil.STATUS_PASSED;
            break;

        case LOCATION_FORWARD.value:
        case TRANSPORT_RETRY.value:
            status = SecurityContextUtil.STATUS_RETRY;
            break;

        case SYSTEM_EXCEPTION.value:
	    status = SecurityContextUtil.STATUS_FAILED;
            break;

	default:
            status = repst;
            /**
             * There is currently no mapping defined for any other status
             * codes. So map this is to a STATUS_FAILED. 
             */
            break;
        }
        if(_logger.isLoggable(Level.FINE)){
            _logger.log(Level.FINE,"Mapped reply status = " + status);
        }
        return status;
    }

    private void handle_null_service_context(ClientRequestInfo ri) {
        if(_logger.isLoggable(Level.FINE)){
            _logger.log(Level.FINE,"No SAS context element found in service context list");
        }
        setreplyStatus(SecurityContextUtil.STATUS_PASSED, ri.effective_target());
    }
    
    public void receive_reply(ClientRequestInfo ri)
    {
        ServiceContext sc = null;
        int status = -1;
 
	if(_logger.isLoggable(Level.FINE)){
                _logger.log(Level.FINE,"++++ Entered " + prname + "receive_reply");
        }

        /** 
         * get the service context element from the reply and decode the
         * mesage.
         */
	try {
            sc = ri.get_reply_service_context(SECURITY_ATTRIBUTE_SERVICE_ID);
            if (sc == null) {
                handle_null_service_context(ri);
                return;
            }
	} catch(org.omg.CORBA.BAD_PARAM e) {
            handle_null_service_context(ri);
            return;
	} catch(Exception ex) {
            _logger.log(Level.SEVERE,"iiop.service_context_exception",ex);
	    return;
	}

        Any a;
        try {
            a = codec.decode_value(sc.context_data, SASContextBodyHelper.type()); //decode the CDR encoding
        } catch (Exception e) {
                _logger.log(Level.SEVERE,"iiop.decode_exception",e);
	    throw new SecurityException(
		localStrings.getLocalString("secclientreqinterceptor.err_cdr_decode",
		                            "CDR Decoding error for SAS context element."));
	}

        SASContextBody sasctxbody = SASContextBodyHelper.extract(a);
        short sasdiscr = sasctxbody.discriminator();
        if(_logger.isLoggable(Level.FINE)){
            _logger.log(Level.FINE,"Received " + SvcContextUtils.getMsgname(sasdiscr) + " message");
        }

        /**
         * Verify that either a CompleteEstablishContext msg or an
         * ContextError message was received.
         */
         if(_logger.isLoggable(Level.FINE)){
            _logger.log(Level.FINE,"Verifying the SAS protocol reply message");
         }

        /* Check the discriminator value */        
                 
        if ((sasdiscr != MTCompleteEstablishContext.value) 
	           && (sasdiscr != MTContextError.value)) {
	                _logger.log(Level.SEVERE,"iiop.invalid_reply_message");
	    throw new SecurityException(
		localStrings.getLocalString("secclientreqinterceptor.err_not_cecec_msg",
		                            "Reply message not one of CompleteEstablishContext or ContextError."));
	}

        /* Map the error code */
        int st = mapreplyStatus(ri.reply_status()); 
 
        setreplyStatus(st, ri.effective_target());
    }
  
    public void receive_exception(ClientRequestInfo ri) throws ForwardRequest
    {
	if(_logger.isLoggable(Level.FINE)){
                _logger.log(Level.FINE,"++++ Entered " + prname + "receive_exception");
        }
    }
   
    public void receive_other(ClientRequestInfo ri) throws ForwardRequest
    {
    }

    public void destroy()
    {
    }
    
    protected GlassFishORBHelper getORBHelper() {
        return this.orbHelper;
    }
}
