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
 * https://github.com/payara/Payara/blob/master/LICENSE.txt
 * See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at legal/OPEN-SOURCE-LICENSE.txt.
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
// Portions Copyright 2018-2024 Payara Foundation and/or its affiliates
// Payara Foundation and/or its affiliates elects to include this software in this distribution under the GPL Version 2 license

package com.sun.enterprise.iiop.security;

import com.sun.corba.ee.org.omg.CSI.CompleteEstablishContext;
import com.sun.corba.ee.org.omg.CSI.ContextError;
import com.sun.corba.ee.org.omg.CSI.EstablishContext;
import com.sun.corba.ee.org.omg.CSI.GSS_NT_ExportedNameHelper;
import com.sun.corba.ee.org.omg.CSI.ITTAbsent;
import com.sun.corba.ee.org.omg.CSI.ITTAnonymous;
import com.sun.corba.ee.org.omg.CSI.ITTDistinguishedName;
import com.sun.corba.ee.org.omg.CSI.ITTPrincipalName;
import com.sun.corba.ee.org.omg.CSI.ITTX509CertChain;
import com.sun.corba.ee.org.omg.CSI.IdentityToken;
import com.sun.corba.ee.org.omg.CSI.MTEstablishContext;
import com.sun.corba.ee.org.omg.CSI.MTMessageInContext;
import com.sun.corba.ee.org.omg.CSI.SASContextBody;
import com.sun.corba.ee.org.omg.CSI.SASContextBodyHelper;
import com.sun.corba.ee.org.omg.CSI.X501DistinguishedNameHelper;
import com.sun.corba.ee.org.omg.CSI.X509CertificateChainHelper;
import com.sun.corba.ee.spi.legacy.connection.Connection;
import com.sun.corba.ee.spi.legacy.interceptor.RequestInfoExt;
import com.sun.enterprise.common.iiop.security.AnonCredential;
import com.sun.enterprise.common.iiop.security.GSSUPName;
import com.sun.enterprise.common.iiop.security.SecurityContext;
import com.sun.enterprise.security.auth.login.common.PasswordCredential;
import com.sun.enterprise.security.auth.login.common.X509CertificateCredential;
import com.sun.enterprise.security.auth.realm.certificate.OID;
import com.sun.enterprise.util.LocalStringManagerImpl;
import com.sun.logging.LogDomains;
import java.io.ByteArrayInputStream;
import java.net.Socket;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.Hashtable;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.security.auth.Subject;
import javax.security.auth.x500.X500Principal;
import org.glassfish.enterprise.iiop.api.GlassFishORBHelper;
import org.omg.CORBA.Any;
import org.omg.CORBA.BAD_PARAM;
import org.omg.CORBA.NO_PERMISSION;
import org.omg.CORBA.ORB;
import org.omg.IOP.Codec;
import org.omg.IOP.ServiceContext;
import org.omg.PortableInterceptor.ForwardRequest;
import org.omg.PortableInterceptor.ServerRequestInfo;
import org.omg.PortableInterceptor.ServerRequestInterceptor;

import static com.sun.enterprise.iiop.security.GSSUtils.GSSUP_MECH_OID;
import static com.sun.enterprise.iiop.security.GSSUtils.verifyMechOID;
import static com.sun.enterprise.iiop.security.SecurityContextUtil.STATUS_FAILED;
import static java.util.logging.Level.FINE;
import static java.util.logging.Level.SEVERE;

/**
 * Security server request interceptor
 */
public class SecServerRequestInterceptor extends org.omg.CORBA.LocalObject implements ServerRequestInterceptor {

    private static final long serialVersionUID = 1L;

    private static Logger logger = LogDomains.getLogger(SecServerRequestInterceptor.class, LogDomains.SECURITY_LOGGER);
    private static LocalStringManagerImpl localStrings = new LocalStringManagerImpl(SecServerRequestInterceptor.class);

    // The below cannot be InheritableThreadLocal because the counter inside
    // would be reused by the thread pool, thus it's a non-inheritable ThreadLocal
    // See PAYARA-2561
    private final ThreadLocal<Counter> counterForCalls = new ThreadLocal<Counter>() {
        @Override
        protected Counter initialValue() {
            return new Counter();
        }
    };

    /**
     * Hard code the value of 15 for SecurityAttributeService until it is defined in IOP.idl.
     * sc.context_id = SecurityAttributeService.value;
     */
    protected static final int SECURITY_ATTRIBUTE_SERVICE_ID = 15;

    // The major and minor codes for a invalid mechanism
    private static final int INVALID_MECHANISM_MAJOR = 2;
    private static final int INVALID_MECHANISM_MINOR = 1;

    public static final String SERVER_CONNECTION_CONTEXT = "ServerConnContext";

    // Used when inserting into service context field
    private static final boolean NO_REPLACE = false;

    // Name of interceptor used for logging purposes (name + "::")
    private final String prname;
    private final String name;
    private final Codec codec;
    private final SecurityContextUtil secContextUtil;
    private final GlassFishORBHelper orbHelper;
    private final SecurityMechanismSelector smSelector;

    public SecServerRequestInterceptor(String name, Codec codec) {
        this.name = name;
        this.codec = codec;
        this.prname = name + "::";
        secContextUtil = Lookups.getSecurityContextUtil();
        orbHelper = Lookups.getGlassFishORBHelper();
        smSelector = Lookups.getSecurityMechanismSelector();
    }

    @Override
    public String name() {
        return name;
    }

    /**
     * Create a ContextError message. This is currently designed to work only for the GSSUP mechanism.
     */
    private SASContextBody createContextError(int status) {

        /**
         * CSIV2 SPEC NOTE:
         *
         * Check that CSIV2 spec does not require an error token to be sent for the GSSUP mechanism.
         */

        return createContextError(1, status);
    }

    /**
     * create a context error with the specified major and minor status
     */
    private SASContextBody createContextError(int major, int minor) {
        if (logger.isLoggable(FINE)) {
            logger.log(FINE, "Creating ContextError message: major code = {0}minor code= {1}", new Object[]{major, minor});
        }

        byte error_token[] = {};
        ContextError ce = new ContextError(0, /* stateless client id */
                major, // major
                minor, // minor
                error_token);

        SASContextBody sasctxtbody = new SASContextBody();
        sasctxtbody.error_msg(ce);

        return sasctxtbody;
    }

    /**
     * Create a CompleteEstablishContext Message. This currently works only for the GSSUP mechanism.
     */
    private SASContextBody createCompleteEstablishContext(int status) {

        /**
         * CSIV2 SPEC NOTE:
         *
         * Check CSIV2 spec to make sure that there is no final_context_token for GSSUP mechanism
         */
        if (logger.isLoggable(FINE)) {
            logger.log(FINE, "Creating CompleteEstablishContext message");
        }

        byte[] final_context_token = {};
        CompleteEstablishContext completeEstablishContext = new CompleteEstablishContext(0, // stateless client id
                false, // for stateless
                final_context_token);

        SASContextBody sasctxtbody = new SASContextBody();
        sasctxtbody.complete_msg(completeEstablishContext);

        return sasctxtbody;
    }

    /**
     * CDR encode a SAS Context body and then construct a service context element.
     */
    private ServiceContext createSvcContext(SASContextBody sasContextBody, ORB orb) {

        Any any = orb.create_any();
        SASContextBodyHelper.insert(any, sasContextBody);

        byte[] cdr_encoded_saselm = {};
        try {
            cdr_encoded_saselm = codec.encode_value(any);
        } catch (Exception e) {
            logger.log(Level.SEVERE, "iiop.encode_exception", e);
        }

        ServiceContext serviceContext = new ServiceContext();
        serviceContext.context_id = SECURITY_ATTRIBUTE_SERVICE_ID;
        serviceContext.context_data = cdr_encoded_saselm;

        return serviceContext;
    }

    /**
     * Create an identity from an Identity Token and stores it as a public credential in the JAAS
     * subject in a security context.
     *
     * Set the identcls field in the security context.
     *
     */
    private void createIdCred(SecurityContext securityContext, IdentityToken identityToken) throws Exception {

        byte[] derEncoding; // used to hold DER encodings
        Any any; // Any object returned from codec.decode_value()

        switch (identityToken.discriminator()) {

        case ITTAbsent.value:
            if (logger.isLoggable(FINE)) {
                logger.log(FINE, "Identity token type is Absent");
            }

            securityContext.identcls = null;
            break;

        case ITTAnonymous.value:
            if (logger.isLoggable(FINE)) {
                logger.log(FINE, "Identity token type is Anonymous");
                logger.log(FINE, "Adding AnonyCredential to subject's PublicCredentials");
            }

            securityContext.subject.getPublicCredentials().add(new AnonCredential());
            securityContext.identcls = AnonCredential.class;
            break;

        case ITTDistinguishedName.value:
            // Construct a X500Principal

            derEncoding = identityToken.dn();

            // Issue 5766: Decode CDR encoding if necessary
            if (isCDR(derEncoding)) {
                any = codec.decode_value(derEncoding, X501DistinguishedNameHelper.type());

                // Extract CDR encoding
                derEncoding = X501DistinguishedNameHelper.extract(any);
            }

            if (logger.isLoggable(FINE)) {
                logger.log(FINE, "Create an X500Principal object from identity token");
            }

            X500Principal xname = new X500Principal(derEncoding);

            if (logger.isLoggable(FINE)) {
                logger.log(FINE, "Identity to be asserted is {0}", xname.toString());
                logger.log(FINE, "Adding X500Principal to subject's PublicCredentials");
            }

            securityContext.subject.getPublicCredentials().add(xname);
            securityContext.identcls = X500Principal.class;
            break;

        case ITTX509CertChain.value:
            // Construct a X509CertificateChain
            if (logger.isLoggable(FINE)) {
                logger.log(FINE, "Identity token type is a X509 Certificate Chain");
            }

            derEncoding = identityToken.certificate_chain();

            // Issue 5766: Decode CDR encoding if necessary
            if (isCDR(derEncoding)) {
                // Decode CDR encoding
                any = codec.decode_value(derEncoding, X509CertificateChainHelper.type());

                // Extract DER encoding
                derEncoding = X509CertificateChainHelper.extract(any);
            }
            
            List<? extends Certificate> certificates = CertificateFactory.getInstance("X.509")
                    .generateCertPath(new ByteArrayInputStream(derEncoding))
                    .getCertificates();

            X509Certificate[] certchain = new X509Certificate[certificates.size()];

            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "Content of X509 Certificate chain:");
            }
            for (int i = 0; i < certchain.length; i++) {
                certchain[i] = (X509Certificate) certificates.get(i);
                if (logger.isLoggable(FINE)) {
                    logger.log(FINE, "    " + certchain[i].getSubjectX500Principal()
                            .getName(X500Principal.RFC2253, OID.getOIDMap()));
                }
            }
            if (logger.isLoggable(FINE)) {
                logger.log(FINE, "Creating a X509CertificateCredential object from certchain");
            }
            // The alias field in the X509CertificateCredential is currently ignored by the RI.
            // So it is set to "dummy".
            X509CertificateCredential cred = new X509CertificateCredential(certchain,
                certchain[0].getSubjectX500Principal().getName(X500Principal.RFC2253, OID.getOIDMap()), "default");
            if (logger.isLoggable(FINE)) {
                logger.log(FINE, "Adding X509CertificateCredential to subject's PublicCredentials");
            }
            securityContext.subject.getPublicCredentials().add(cred);
            securityContext.identcls = X509CertificateCredential.class;
            break;

        case ITTPrincipalName.value:
            if (logger.isLoggable(FINE)) {
                logger.log(FINE, "Identity token type is GSS Exported Name");
            }

            byte[] expname = identityToken.principal_name();

            // Issue 5766: Decode CDR encoding if necessary
            if (isCDR(expname)) {
                // Decode CDR encoding
                any = codec.decode_value(expname, GSS_NT_ExportedNameHelper.type());

                expname = GSS_NT_ExportedNameHelper.extract(any);
            }

            if (!verifyMechOID(GSSUP_MECH_OID, expname)) {
                throw new SecurityException(localStrings.getLocalString("secserverreqinterceptor.err_unknown_idassert_type",
                        "Unknown identity assertion type."));
            }

            GSSUPName gssname = new GSSUPName(expname);

            securityContext.subject.getPublicCredentials().add(gssname);
            securityContext.identcls = GSSUPName.class;

            logger.log(FINE, "Adding GSSUPName credential to subject");
            break;

        default:
            logger.log(SEVERE, "iiop.unknown_identity");
            throw new SecurityException(localStrings.getLocalString("secserverreqinterceptor.err_unknown_idassert_type",
                    "Unknown identity assertion type."));
        }
    }

    /**
     * Check if given byte is CDR encapsulated.
     *
     * @param bytes an input array of byte
     * @return boolean indicates whether input is CDR
     */
    private boolean isCDR(byte[] bytes) {
        return (bytes != null && bytes.length > 0 && (bytes[0] == 0x0 || bytes[0] == 0x1));
    }

    /**
     * Create an auth credential from authentication token and store it as a private credential in the
     * JAAS subject in the security context.
     *
     * Set the authcls field in the security context.
     *
     * This method currently only works for PasswordCredential tokens.
     */
    private void createAuthCredential(SecurityContext securityContext, byte[] authToken, ORB orb) throws Exception {
        logger.log(FINE, "Constructing a PasswordCredential from client authentication token");

        // Create a GSSUPToken from the authentication token
        PasswordCredential passwordCredential = GSSUPToken.getServerSideInstance(orb, codec, authToken).getPwdcred();

        if (logger.isLoggable(FINE)) {
            logger.log(FINE, "Password credential = " + passwordCredential.toString());
            logger.log(FINE, "Adding PasswordCredential to subject's PrivateCredentials");
        }
        
        securityContext.subject.getPrivateCredentials().add(passwordCredential);
        securityContext.authcls = PasswordCredential.class;
    }

    private void handle_null_service_context(ServerRequestInfo serverRequestInfo, ORB orb) {
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE,
                    "No SAS context element found in service context list for operation: " + serverRequestInfo.operation());
        }

        ServiceContext serviceContext = null;
        int secStatus = secContextUtil.setSecurityContext(null, serverRequestInfo.object_id(), serverRequestInfo.operation(),
                getServerSocket());

        if (secStatus == STATUS_FAILED) {
            SASContextBody sasctxbody = createContextError(INVALID_MECHANISM_MAJOR, INVALID_MECHANISM_MINOR);
            serviceContext = createSvcContext(sasctxbody, orb);
            serverRequestInfo.add_reply_service_context(serviceContext, NO_REPLACE);

            if (logger.isLoggable(Level.FINE)) {
                logger.log(Level.FINE, "SecServerRequestInterceptor.receive_request: NO_PERMISSION");
            }

            throw new NO_PERMISSION();
        }
    }

    @Override
    public void receive_request(ServerRequestInfo serverRequestInfo) throws ForwardRequest {
        SecurityContext securityContext = null; // SecurityContext to be sent
        ServiceContext serviceContext = null; // service context
        int status = 0;

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "++++ Entered " + prname + "receive_request");
        }

        ORB orb = orbHelper.getORB();

        try {
            serviceContext = serverRequestInfo.get_request_service_context(SECURITY_ATTRIBUTE_SERVICE_ID);
            if (serviceContext == null) {
                handle_null_service_context(serverRequestInfo, orb);
                return;
            }
        } catch (BAD_PARAM e) {
            handle_null_service_context(serverRequestInfo, orb);
            return;
        }

        if (logger.isLoggable(FINE)) {
            logger.log(FINE, "Received a non null SAS context element");
        }

        // Decode the service context field
        Any SasAny;
        try {
            SasAny = codec.decode_value(serviceContext.context_data, SASContextBodyHelper.type());
        } catch (Exception e) {
            logger.log(SEVERE, "iiop.decode_exception", e);
            throw new SecurityException(localStrings.getLocalString("secserverreqinterceptor.err_cdr_decode",
                    "CDR Decoding error for SAS context element."));
        }

        if (logger.isLoggable(FINE)) {
            logger.log(FINE, "Successfully decoded CDR encoded SAS context element.");
        }

        SASContextBody sasctxbody = SASContextBodyHelper.extract(SasAny);

        short sasdiscr = sasctxbody.discriminator();
        if (logger.isLoggable(FINE)) {
            logger.log(FINE, "SAS context element is a/an " + SvcContextUtils.getMsgname(sasdiscr) + " message");
        }

        // Check message type received

        /**
         * CSIV2 SPEC NOTE:
         *
         * Section 4.3 "TSS State Machine" , table 4-4 "TSS State Table" shows that a MessageInContext can
         * be received. In this case the table is somewhat unclear. But in this case a ContextError with the
         * status code "No Context" ( specified in section 4.5 "ContextError Values and Exceptions" must be
         * sent back. A NO_PERMISSION exception must also be raised.
         *
         * ISSUE: should setSecurityContext(null) be called ?
         */

        if (sasdiscr == MTMessageInContext.value) {
            sasctxbody = createContextError(SvcContextUtils.MessageInContextMinor);
            serviceContext = createSvcContext(sasctxbody, orb);

            if (logger.isLoggable(FINE)) {
                logger.log(FINE, "Adding ContextError message to service context list");
                logger.log(FINE, "SecurityContext set to null");
            }

            serverRequestInfo.add_reply_service_context(serviceContext, NO_REPLACE);

            throw new NO_PERMISSION();
        }

        /**
         * CSIV2 SPEC NOTE:
         *
         * CSIV2 spec does not specify the actions for any message other than a MessageInContext and
         * EstablishContext message.So for such messages, this implementation simply drops the message on
         * the floor. No other message is sent back. Neither is an exception raised.
         *
         * ISSUE: Should there be some other action ?
         */

        if (sasdiscr != MTEstablishContext.value) {
            logger.log(SEVERE, "iiop.not_establishcontext_msg");

            throw new SecurityException(localStrings.getLocalString("secserverreqinterceptor.err_not_ec_msg",
                    "Received message not an EstablishContext message."));
        }

        EstablishContext establishContext = sasctxbody.establish_msg();

        securityContext = new SecurityContext();
        securityContext.subject = new Subject();

        try {
            if (establishContext.client_authentication_token.length != 0) {
                if (logger.isLoggable(FINE)) {
                    logger.log(FINE, "Message contains Client Authentication Token");
                }

                createAuthCredential(securityContext, establishContext.client_authentication_token, orb);
            }
        } catch (Exception e) {
            logger.log(SEVERE, "iiop.authentication_exception", e);

            throw new SecurityException(localStrings.getLocalString("secsercverreqinterceptor.err_cred_create",
                    "Error while creating a JAAS subject credential."));
        }

        try {
            if (establishContext.identity_token != null) {
                if (logger.isLoggable(Level.FINE)) {
                    logger.log(Level.FINE, "Message contains an Identity Token");
                }
                createIdCred(securityContext, establishContext.identity_token);
            }
        } catch (SecurityException secex) {
            logger.log(SEVERE, "iiop.security_exception", secex);
            sasctxbody = createContextError(INVALID_MECHANISM_MAJOR, INVALID_MECHANISM_MINOR);
            serviceContext = createSvcContext(sasctxbody, orb);
            serverRequestInfo.add_reply_service_context(serviceContext, NO_REPLACE);

            throw new NO_PERMISSION();
        } catch (Exception e) {
            logger.log(SEVERE, "iiop.generic_exception", e);
            throw new SecurityException(localStrings.getLocalString("secsercverreqinterceptor.err_cred_create",
                    "Error while creating a JAAS subject credential."));

        }

        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "Invoking setSecurityContext() to set security context");
        }

        status = secContextUtil.setSecurityContext(securityContext, serverRequestInfo.object_id(), serverRequestInfo.operation(),
                getServerSocket());
        if (logger.isLoggable(Level.FINE)) {
            logger.log(Level.FINE, "setSecurityContext() returned status code " + status);
        }

        /**
         * CSIV2 SPEC NOTE:
         *
         * If ec.client_context_id is non zero, then this is a stateful request. As specified in section
         * 4.2.1, a stateless server must attempt to validate the security tokens in the security context
         * field. If validation succeeds then CompleteEstablishContext message is sent back. If validation
         * fails, a ContextError must be sent back.
         */
        if (status == STATUS_FAILED) {
            if (logger.isLoggable(FINE)) {
                logger.log(FINE, "setSecurityContext() returned STATUS_FAILED");
            }

            sasctxbody = createContextError(status);
            serviceContext = createSvcContext(sasctxbody, orb);
            if (logger.isLoggable(FINE)) {
                logger.log(FINE, "Adding ContextError message to service context list");
            }
            serverRequestInfo.add_reply_service_context(serviceContext, NO_REPLACE);

            throw new NO_PERMISSION();
        }

        if (logger.isLoggable(FINE)) {
            logger.log(FINE, "setSecurityContext() returned SUCCESS");
        }

        sasctxbody = createCompleteEstablishContext(status);
        serviceContext = createSvcContext(sasctxbody, orb);

        if (logger.isLoggable(FINE)) {
            logger.log(FINE, "Adding CompleteEstablisContext message to service context list");
        }

        serverRequestInfo.add_reply_service_context(serviceContext, NO_REPLACE);
    }

    /**
     * This method is keeping a track of when to unset the security context Currently with the re-use of
     * the threads made by the orb the security context does not get unset. This method determines when
     * to unset the security context
     */
    @Override
    public void receive_request_service_contexts(ServerRequestInfo serverRequestInfo) throws ForwardRequest {
        // cannot set this in receive_request due to the PI flow control
        // semantics. e.g. if receive_req for some other PI throws an
        // exception - the send_exception will be called that will muck
        // the stack up
        Counter cntr = counterForCalls.get();
        if (cntr.count == 0) {
            SecurityContextUtil.unsetSecurityContext(isLocal());
        }
        cntr.increment();

        Socket socket = null;
        Connection connection = null;
        if (serverRequestInfo instanceof RequestInfoExt) {
            connection = ((RequestInfoExt) serverRequestInfo).connection();
        }

        ServerConnectionContext serverConnectionContext = null;
        if (connection != null) {
            socket = connection.getSocket();
            if (logger.isLoggable(FINE)) {
                logger.log(FINE, "RECEIVED request on connection: " + connection);
                logger.log(FINE, "Socket =" + socket);
            }
            serverConnectionContext = new ServerConnectionContext(socket);
        } else {
            serverConnectionContext = new ServerConnectionContext();
        }

        setServerConnectionContext(serverConnectionContext);
    }

    @Override
    public void send_reply(ServerRequestInfo ri) {
        unsetSecurityContext();
    }

    @Override
    public void send_exception(ServerRequestInfo ri) throws ForwardRequest {
        unsetSecurityContext();
    }

    @Override
    public void send_other(ServerRequestInfo ri) throws ForwardRequest {
        unsetSecurityContext();
    }

    @Override
    public void destroy() {
    }

    private void unsetSecurityContext() {
        try {
            Counter cntr = counterForCalls.get();
            cntr.decrement();
            if (cntr.count <= 0) {
                cntr.count = 0;
                SecurityContextUtil.unsetSecurityContext(isLocal());

            }
        } finally {
            ConnectionExecutionContext.removeClientThreadID();
        }
    }

    private boolean isLocal() {
        boolean local = true;
        ServerConnectionContext scc = getServerConnectionContext();
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
        ServerConnectionContext scc = (ServerConnectionContext) h.get(SERVER_CONNECTION_CONTEXT);
        return scc;
    }

    public static void setServerConnectionContext(ServerConnectionContext scc) {
        Hashtable h = ConnectionExecutionContext.getContext();
        h.put(SERVER_CONNECTION_CONTEXT, scc);
    }
}

class Counter {

    public int count = 0;

    public Counter(int count) {
        this.count = count;
    }

    public Counter() {
        count = 0;
    }

    public void setCount(int counter) {
        count = counter;
    }

    public void increment() {
        count++;
    }

    public void decrement() {
        count--;
    }

    public String display() {
        return " Counter = " + count;
    }
}
