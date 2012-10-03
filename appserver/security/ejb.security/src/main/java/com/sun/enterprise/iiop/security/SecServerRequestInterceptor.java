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

/**
 * This class is a server side request interceptor for CSIV2. 
 * It is used to send and receive the service context in a 
 * a service context element in the service context list in  
 * an IIOP header.
 *
 * @author: Nithya Subramanian
 */

import com.sun.enterprise.common.iiop.security.SecurityContext;
import org.omg.CORBA.*;
import org.omg.PortableInterceptor.*;
import org.omg.IOP.*;

import java.security.cert.X509Certificate;

/* Import classes generated from CSIV2 idl files */
import com.sun.corba.ee.org.omg.CSI.*;
import com.sun.corba.ee.spi.legacy.connection.Connection;
import com.sun.corba.ee.spi.legacy.interceptor.RequestInfoExt;
import com.sun.enterprise.common.iiop.security.AnonCredential;
import com.sun.enterprise.common.iiop.security.GSSUPName;
import sun.security.util.DerInputStream;
import sun.security.util.DerValue;

import sun.security.x509.X509CertImpl;
import sun.security.x509.X500Name;
import javax.security.auth.*;  

import com.sun.enterprise.security.auth.login.common.PasswordCredential;
import com.sun.enterprise.security.auth.login.common.X509CertificateCredential;

import com.sun.enterprise.util.LocalStringManagerImpl;

import com.sun.logging.LogDomains;
import java.net.Socket;
import java.util.Hashtable;
import java.util.logging.*;
import org.glassfish.enterprise.iiop.api.GlassFishORBHelper;

/*
 * Security server request interceptor 
 */

public class SecServerRequestInterceptor
    extends    org.omg.CORBA.LocalObject
    implements ServerRequestInterceptor
{

    private static java.util.logging.Logger _logger=null;
    static{
       _logger=LogDomains.getLogger(SecServerRequestInterceptor.class,LogDomains.SECURITY_LOGGER);
        }
    private static LocalStringManagerImpl localStrings =
        new LocalStringManagerImpl(SecServerRequestInterceptor.class);
    private InheritableThreadLocal counterForCalls = new InheritableThreadLocal();

    /** 
     *  Hard code the value of 15 for SecurityAttributeService until
     *  it is defined in IOP.idl.
     *     sc.context_id = SecurityAttributeService.value;
     */
    protected static final int SECURITY_ATTRIBUTE_SERVICE_ID = 15;
    // the major and minor codes for a invalid mechanism
    private static final int INVALID_MECHANISM_MAJOR = 2;
    private static final int INVALID_MECHANISM_MINOR = 1;

    public static final String SERVER_CONNECTION_CONTEXT = "ServerConnContext";

    /* used when inserting into service context field */
    private static final boolean  NO_REPLACE = false; 

    /* name of interceptor used for logging purposes (name + "::") */
    private String prname; 
    private String name;
    private Codec  codec;
    //private ORB orb;
    private SecurityContextUtil secContextUtil = null;
    private GlassFishORBHelper orbHelper;
    private SecurityMechanismSelector smSelector = null;
    //Not required
    //  SecurityService secsvc = null;       // Security Service
    public SecServerRequestInterceptor(String name, Codec codec) {
        this.name    = name;
        this.codec   = codec;
        this.prname  = name + "::";
        secContextUtil = Lookups.getSecurityContextUtil();
        orbHelper = Lookups.getGlassFishORBHelper();
        smSelector = Lookups.getSecurityMechanismSelector();
    }

    public String name() {
        return name;
    }

    /**
     * Create a ContextError message. This is currently designed to work only
     * for the GSSUP mechanism.
     */

    /* Create a ContexError Message */
    private SASContextBody createContextError(int status) {
        /** 
         * CSIV2 SPEC NOTE:
         * 
         * Check that CSIV2 spec does not require an error token to be sent
         * for the GSSUP mechanism.
         */

        return createContextError(1, status);
    }

    /* create a context error with the specified major and minor status
     */
    private SASContextBody createContextError(int major, int minor) {
        
        if(_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE,"Creating ContextError message: major code = "+ major+ "minor code= "+ minor);
        }
        byte error_token[] = {} ;
        ContextError ce = new ContextError(0,      /* stateless client id */
                                           major, // major
                                           minor, // minor
                                           error_token);
        SASContextBody sasctxtbody = new SASContextBody();
        sasctxtbody.error_msg(ce);
        return sasctxtbody;
    }

    /**
     * Create a CompleteEstablishContext Message. This currently works only
     * for the GSSUP mechanism.
     */
    private SASContextBody createCompleteEstablishContext(int status) {
        /** 
         * CSIV2 SPEC NOTE:
         * 
         * Check CSIV2 spec to make sure that there is no 
         * final_context_token for GSSUP mechanism
         */
        if(_logger.isLoggable(Level.FINE)){
            _logger.log(Level.FINE,"Creating CompleteEstablishContext message");
        }
        byte[] final_context_token  = {} ;
        CompleteEstablishContext cec = new CompleteEstablishContext(
                              0,      // stateless client id
                              false,  // for stateless 
                              final_context_token);
        SASContextBody sasctxtbody = new SASContextBody();
        sasctxtbody.complete_msg(cec);
        return sasctxtbody;
    }

    /**
     *  CDR encode a SAS Context body and then construct a service context
     *  element.
     */
    private ServiceContext createSvcContext(SASContextBody sasctxtbody, ORB orb) {

        ServiceContext sc = null;

        Any a = orb.create_any();
        SASContextBodyHelper.insert(a, sasctxtbody);

        byte[] cdr_encoded_saselm = {};
        try {
            cdr_encoded_saselm = codec.encode_value(a);
        } catch (Exception e) {
                _logger.log(Level.SEVERE,"iiop.encode_exception",e);
        }
        sc               = new ServiceContext();
        sc.context_id    = SECURITY_ATTRIBUTE_SERVICE_ID;
        sc.context_data  = cdr_encoded_saselm;
        return sc;

    }

    /**
     * Create an identity from an Identity Token and stores it as a
     * public credential in the JAAS subject in a security context.
     * 
     * Set the identcls field in the security context.
     * 
     */
    private void createIdCred(SecurityContext sc, IdentityToken idtok)
        throws Exception {

        byte[] derenc ; // used to hold DER encodings
        Any    any;     // Any object returned from codec.decode_value()

        switch (idtok.discriminator()) {

        case ITTAbsent.value:
            if(_logger.isLoggable(Level.FINE)){
                _logger.log(Level.FINE,"Identity token type is Absent");
            }
            sc.identcls = null;
            break;

        case ITTAnonymous.value:
            if(_logger.isLoggable(Level.FINE)){
                _logger.log(Level.FINE,"Identity token type is Anonymous");
		_logger.log(Level.FINE,"Adding AnonyCredential to subject's PublicCredentials");
            }
            sc.subject.getPublicCredentials().add(new AnonCredential());
            sc.identcls = AnonCredential.class;
            break;

        case ITTDistinguishedName.value:
            /* Construct a X500Name */

            derenc = idtok.dn();
            /* Issue 5766: Decode CDR encoding if necessary */
            if (isCDR(derenc)) {
                any = codec.decode_value(derenc, X501DistinguishedNameHelper.type());

                /* Extract CDR encoding */
                derenc = X501DistinguishedNameHelper.extract(any);
            }
            if(_logger.isLoggable(Level.FINE)){
                _logger.log(Level.FINE,"Create an X500Name object from identity token");
            }
            X500Name xname = new X500Name(derenc);
	    if(_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE,"Identity to be asserted is " + xname.toString());
		_logger.log(Level.FINE,"Adding X500Name to subject's PublicCredentials");
	    }
            sc.subject.getPublicCredentials().add(xname);
            sc.identcls = X500Name.class;
            break;
            
        case ITTX509CertChain.value:
            /*  Construct a X509CertificateChain */
            if(_logger.isLoggable(Level.FINE)){
                _logger.log(Level.FINE,"Identity token type is a X509 Certificate Chain");
            }
            derenc = idtok.certificate_chain();
            /* Issue 5766: Decode CDR encoding if necessary */
            if (isCDR(derenc)) {
                /* Decode CDR encoding */
                any = codec.decode_value(derenc, X509CertificateChainHelper.type());

                /* Extract DER encoding */
                derenc = X509CertificateChainHelper.extract(any);
            }

            DerInputStream din = new DerInputStream(derenc);

            /** 
             * Size specified for getSequence() is 1 and is just 
             * used as a guess by the method getSequence().
             */
            DerValue[] derval = din.getSequence(1);
            X509Certificate[] certchain = 
                        new X509CertImpl[derval.length];
            /**
             * X509Certificate does not have a constructor which can
             * be used to instantiate objects from DER encodings. So
             * use X509CertImpl extends X509Cerificate and also implements
             * DerEncoder interface. 
             */
            if(_logger.isLoggable(Level.FINE)){
		_logger.log(Level.FINE,"Contents of X509 Certificate chain:");
            }
            for (int i = 0; i < certchain.length; i++) {
                certchain[i] = new X509CertImpl(derval[i]);
                if(_logger.isLoggable(Level.FINE)){
                _logger.log(Level.FINE,"    " + certchain[i].getSubjectDN().getName());
                }
            }
            if(_logger.isLoggable(Level.FINE)){
                _logger.log(Level.FINE,"Creating a X509CertificateCredential object from certchain");
            }
            /**
             * The alias field in the X509CertificateCredential is currently ignored
             * by the RI. So it is set to "dummy".
             * 
             */
            X509CertificateCredential cred = 
                new X509CertificateCredential(certchain, certchain[0].getSubjectDN().getName(), "default");
            if(_logger.isLoggable(Level.FINE)){
                _logger.log(Level.FINE,"Adding X509CertificateCredential to subject's PublicCredentials");
            }
            sc.subject.getPublicCredentials().add(cred);
            sc.identcls = X509CertificateCredential.class;
            break;
 
        case ITTPrincipalName.value:
            if(_logger.isLoggable(Level.FINE)){
		_logger.log(Level.FINE,"Identity token type is GSS Exported Name");
            }
            byte[] expname = idtok.principal_name();
            /* Issue 5766: Decode CDR encoding if necessary */
            if (isCDR(expname)) {
                /* Decode CDR encoding */
                any = codec.decode_value(expname, GSS_NT_ExportedNameHelper.type());

                expname = GSS_NT_ExportedNameHelper.extract(any);
            }
            if ( ! GSSUtils.verifyMechOID(GSSUtils.GSSUP_MECH_OID, expname)) {
                throw new SecurityException(
                localStrings.getLocalString("secserverreqinterceptor.err_unknown_idassert_type",
                                            "Unknown identity assertion type."));
            }

            GSSUPName gssname = new GSSUPName(expname);

            sc.subject.getPublicCredentials().add(gssname);
            sc.identcls  = GSSUPName.class;
            _logger.log(Level.FINE,"Adding GSSUPName credential to subject");
            break;

        default:
		_logger.log(Level.SEVERE,"iiop.unknown_identity");              
            throw new SecurityException(
                localStrings.getLocalString("secserverreqinterceptor.err_unknown_idassert_type",
                                            "Unknown identity assertion type."));
        }
    }

    /**
     * Check if given byte is CDR encapsulated.
     * @param bytes an input array of byte
     * @return boolean indicates whether input is CDR
     */
    private boolean isCDR(byte[] bytes) {
        return (bytes != null && bytes.length > 0 && 
                (bytes[0] == 0x0 || bytes[0] == 0x1));
    }

    /**
     * Create an auth credential from authentication token and store
     * it as a private credential in the JAAS subject in the security 
     * context.
     *
     * Set the authcls field in the security context.
     *
     * This method currently only works for PasswordCredential tokens.
     */
    private void createAuthCred(SecurityContext sc, byte[] authtok, ORB orb) throws Exception
    {
		_logger.log(Level.FINE,"Constructing a PasswordCredential from client authentication token");
        /* create a GSSUPToken from the authentication token */
        GSSUPToken tok  = GSSUPToken.getServerSideInstance(orb, codec, authtok);

        final PasswordCredential pwdcred = tok.getPwdcred();
        final SecurityContext fsc = sc;
	    if(_logger.isLoggable(Level.FINE)) {
		_logger.log(Level.FINE,"Password credential = " + pwdcred.toString());
		_logger.log(Level.FINE,"Adding PasswordCredential to subject's PrivateCredentials");
	}
        java.security.AccessController.doPrivileged(new java.security.PrivilegedAction() {
            public java.lang.Object run() {
                fsc.subject.getPrivateCredentials().add(pwdcred);
                return null;
            }
        });
        sc = fsc;
        sc.authcls  = PasswordCredential.class;
    }     
    
    private void handle_null_service_context(ServerRequestInfo ri, ORB orb) {
        if(_logger.isLoggable(Level.FINE)){
            _logger.log(Level.FINE,"No SAS context element found in service context list for operation: " + ri.operation());
        }
	ServiceContext sc = null;
        int secStatus = secContextUtil.setSecurityContext(null, ri.object_id(),
                ri.operation(), getServerSocket());
        
        if (secStatus == SecurityContextUtil.STATUS_FAILED){
            SASContextBody sasctxbody = createContextError(INVALID_MECHANISM_MAJOR,
                    INVALID_MECHANISM_MINOR);
            sc = createSvcContext(sasctxbody, orb);
            ri.add_reply_service_context(sc, NO_REPLACE);
            if(_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE,
                        "SecServerRequestInterceptor.receive_request: NO_PERMISSION");
            }
            throw new NO_PERMISSION();
        }       
    }
     
    public void receive_request(ServerRequestInfo ri) 
         throws ForwardRequest  
    {
        SecurityContext seccontext = null;   // SecurityContext to be sent
        ServiceContext  sc = null;           // service context
        int status = 0;
        boolean  raise_no_perm = false;

        if (_logger.isLoggable(Level.FINE)) {
            _logger.log(Level.FINE, "++++ Entered " + prname + "receive_request");
        }
        
       // secsvc  = Csiv2Manager.getSecurityService();
        ORB orb = orbHelper.getORB();

        try {
            sc = ri.get_request_service_context(SECURITY_ATTRIBUTE_SERVICE_ID);
            if (sc == null) {
                handle_null_service_context(ri, orb);
                return;
            }
        } catch (org.omg.CORBA.BAD_PARAM e) {
            handle_null_service_context(ri, orb);
            return;
        }

        if(_logger.isLoggable(Level.FINE)){
		_logger.log(Level.FINE,"Received a non null SAS context element");
        }
        /* Decode the service context field */
        Any SasAny;
        try {        
            SasAny = codec.decode_value(sc.context_data, SASContextBodyHelper.type());
        } catch (Exception e) {
        _logger.log(Level.SEVERE,"iiop.decode_exception",e);
            throw new SecurityException(
                localStrings.getLocalString("secserverreqinterceptor.err_cdr_decode",
                                            "CDR Decoding error for SAS context element."));
        }

        if(_logger.isLoggable(Level.FINE)){
		_logger.log(Level.FINE,"Successfully decoded CDR encoded SAS context element.");
        }
        SASContextBody sasctxbody = SASContextBodyHelper.extract(SasAny);

        short sasdiscr = sasctxbody.discriminator();
        if(_logger.isLoggable(Level.FINE)){
		_logger.log(Level.FINE,"SAS context element is a/an " + SvcContextUtils.getMsgname(sasdiscr)+ " message");
        }
        /* Check message type received */

        /**
         *  CSIV2 SPEC NOTE:
         *
         *  Section 4.3 "TSS State Machine" , table 4-4 "TSS State Table" 
         *  shows that a MessageInContext can be received. In this case
         *  the table is somewhat unclear. But in this case a ContextError
         *  with the status code "No Context" ( specified in  
         *  section 4.5 "ContextError Values and Exceptions" must be sent back.
         *  A NO_PERMISSION exception must also be raised.
         *
         *  ISSUE: should setSecurityContext(null) be called ?
         */

        if (sasdiscr == MTMessageInContext.value) {
             sasctxbody = createContextError(SvcContextUtils.MessageInContextMinor);
             sc = createSvcContext(sasctxbody, orb);
        if(_logger.isLoggable(Level.FINE)){
		_logger.log(Level.FINE,"Adding ContextError message to service context list");
		_logger.log(Level.FINE,"SecurityContext set to null");
        }
             ri.add_reply_service_context(sc, NO_REPLACE);
             // no need to set the security context
//              secsvc.setSecurityContext(null, ri.object_id(), ri.operation());

             throw new NO_PERMISSION();
        }

        /**
         * CSIV2 SPEC NOTE:
         * 
         * CSIV2 spec does not specify the actions for any message other than
         * a MessageInContext and EstablishContext message.So for such messages,
         * this implementation simply drops the message on the floor. No
         * other message is sent back. Neither is an exception raised.
         * 
         * ISSUE: Should there be some other action ?
         */

        if (sasdiscr != MTEstablishContext.value) {
            _logger.log(Level.SEVERE,"iiop.not_establishcontext_msg");
            throw new SecurityException(
                localStrings.getLocalString("secserverreqinterceptor.err_not_ec_msg",
                                            "Received message not an EstablishContext message."));
        }
 
        EstablishContext ec = sasctxbody.establish_msg();

        seccontext = new SecurityContext();
        seccontext.subject = new Subject();
        
        try {
            if (ec.client_authentication_token.length != 0) {
                if(_logger.isLoggable(Level.FINE)){
                    _logger.log(Level.FINE,"Message contains Client Authentication Token");
                }
                createAuthCred(seccontext, ec.client_authentication_token, orb);
            }
        } catch (Exception e) {
            _logger.log(Level.SEVERE,"iiop.authentication_exception",e);
            throw new SecurityException(
                localStrings.getLocalString("secsercverreqinterceptor.err_cred_create",
                                            "Error while creating a JAAS subject credential."));


        }

        try{
            if (ec.identity_token != null) {
                if(_logger.isLoggable(Level.FINE)){
                    _logger.log(Level.FINE,"Message contains an Identity Token");
                }
                createIdCred(seccontext, ec.identity_token);
            }
        } catch (SecurityException secex){
            _logger.log(Level.SEVERE,"iiop.security_exception",secex);
            sasctxbody = createContextError(INVALID_MECHANISM_MAJOR,
                                            INVALID_MECHANISM_MINOR);
            sc = createSvcContext(sasctxbody, orb);
            ri.add_reply_service_context(sc, NO_REPLACE);
            throw new NO_PERMISSION();
        } catch (Exception e) {
            _logger.log(Level.SEVERE,"iiop.generic_exception",e);
            throw new SecurityException(
                                        localStrings.getLocalString("secsercverreqinterceptor.err_cred_create",
                                                                    "Error while creating a JAAS subject credential."));
            
        }

        if(_logger.isLoggable(Level.FINE)){
            _logger.log(Level.FINE,"Invoking setSecurityContext() to set security context");
        }
        status = secContextUtil.setSecurityContext(seccontext, ri.object_id(), ri.operation(), getServerSocket());
	if(_logger.isLoggable(Level.FINE)){
		_logger.log(Level.FINE,"setSecurityContext() returned status code " + status);
        }
        /**
         * CSIV2 SPEC NOTE:
         *
         * If ec.client_context_id is non zero, then this is a stateful
         * request. As specified in section 4.2.1, a stateless server must
         * attempt to validate the security tokens in the security context
         * field. If validation succeeds then CompleteEstablishContext message
         * is sent back. If validation fails, a ContextError must be sent back.
         */
        if (status == SecurityContextUtil.STATUS_FAILED) {
            if(_logger.isLoggable(Level.FINE)){
		_logger.log(Level.FINE,"setSecurityContext() returned STATUS_FAILED");
            }
            sasctxbody = createContextError(status);
            sc = createSvcContext(sasctxbody, orb);
            if(_logger.isLoggable(Level.FINE)){
		_logger.log(Level.FINE,"Adding ContextError message to service context list");
            }
            ri.add_reply_service_context(sc, NO_REPLACE);
            throw new NO_PERMISSION();
        }

        if(_logger.isLoggable(Level.FINE)){
		_logger.log(Level.FINE,"setSecurityContext() returned SUCCESS");
        }
        sasctxbody = createCompleteEstablishContext(status);
        sc = createSvcContext(sasctxbody, orb);
        if(_logger.isLoggable(Level.FINE)){
            _logger.log(Level.FINE,"Adding CompleteEstablisContext message to service context list");
        }
        ri.add_reply_service_context(sc, NO_REPLACE);
    }

    /* This method is keeping a track of when to unset the security context
     * Currently with the re-use of the threads made by the orb the security
     * context does not get unset. This method determines when to unset the 
     * security context
     */
    public void receive_request_service_contexts(ServerRequestInfo ri)
         throws ForwardRequest
    {
        // cannot set this in receive_request due to the PI flow control
        // semantics. e.g. if receive_req for some other PI throws an 
        // exception - the send_exception will be called that will muck
        // the stack up
        Counter cntr = (Counter)counterForCalls.get();
        if (cntr == null){
            cntr = new Counter();
            counterForCalls.set(cntr);
        } 
        if (cntr.count == 0) {
            //Not required
            //SecurityService secsvc  = Csiv2Manager.getSecurityService();
            SecurityContextUtil.unsetSecurityContext(isLocal());
        }
        cntr.increment();

        Socket s = null;
	Connection c = null;
	if (ri instanceof RequestInfoExt) {
            c = ((RequestInfoExt)ri).connection();
        }
        ServerConnectionContext scc = null;
        if (c != null) {
            s = c.getSocket();
            if(_logger.isLoggable(Level.FINE)) {
                _logger.log(Level.FINE,"RECEIVED request on connection: " + c);
                _logger.log(Level.FINE,"Socket =" + s);
            }
            scc = new ServerConnectionContext(s);
        } else {
            scc = new ServerConnectionContext();
        }
        setServerConnectionContext(scc);
    }

    public void send_reply(ServerRequestInfo ri)
    {
        unsetSecurityContext();
    }
 
    public void send_exception(ServerRequestInfo ri) 
         throws ForwardRequest
    {
        unsetSecurityContext();
    }
   
    public void send_other(ServerRequestInfo ri)
         throws ForwardRequest
    {
        unsetSecurityContext();
    }

    public void destroy()
    {
    }

    private void unsetSecurityContext() {
        try {
            Counter cntr = (Counter) counterForCalls.get();
            if (cntr == null) {      // sanity check
                cntr = new Counter(1);
            }
            cntr.decrement();
            if (cntr.count == 0) {
                SecurityContextUtil.unsetSecurityContext(isLocal());

            }
        } finally {
            ConnectionExecutionContext.removeClientThreadID();
        }
    }

    private boolean isLocal() {
        boolean local = true;
        ServerConnectionContext scc =
               getServerConnectionContext();
        if (scc != null && scc.getSocket() != null) {
            local = false;
        }
        Long clientID = ConnectionExecutionContext.readClientThreadID();
        if (clientID != null && clientID == Thread.currentThread().getId()) {
            local = true;
        }
        return local;
    }

    private Socket getServerSocket() {
        ServerConnectionContext scc = getServerConnectionContext();
        if (scc != null) {
            return scc.getSocket();
        }
        return null;
    }

    private ServerConnectionContext getServerConnectionContext() {
        Hashtable h = ConnectionExecutionContext.getContext();
        ServerConnectionContext scc =
            (ServerConnectionContext) h.get(SERVER_CONNECTION_CONTEXT);
        return scc;
    }

    public static void setServerConnectionContext(ServerConnectionContext scc) {
        Hashtable h = ConnectionExecutionContext.getContext();
        h.put(SERVER_CONNECTION_CONTEXT, scc);
    }
}

class Counter {
    
    public int count = 0;
    public Counter(int count){
        this.count = count;
    }
    public Counter(){
        count = 0;
    }
    public void setCount(int counter){
        count = counter;
    }
    public void increment(){
        count++;
    }
    public void decrement(){
        count--;
    }
    public String display(){
         return " Counter = " +count;
    }
}
